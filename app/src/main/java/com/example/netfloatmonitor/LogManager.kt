package com.example.netfloatmonitor

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

class LogManager(private val context: Context) {

    companion object {
        private const val MAX_LOG_FILES = 100 // 最大允许保留的日志文件数量
    }

    private val logDir = File(context.getExternalFilesDir(null), "NetFloatLogs").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    private val isRecording = AtomicBoolean(false)
    private var currentFileName: String? = null
    // 保存表头结构，确保每一行的列顺序严格一致
    private val csvHeaders = mutableListOf<String>()
    
    // 线程安全高频并发数据流缓冲队列
    private val dataQueue = LinkedBlockingQueue<String>()
    private var consumerThread: Thread? = null

    private fun generateNewFileName(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "log_${sdf.format(Date())}.csv"
    }

    private fun getTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getLogPath(): String = logDir.absolutePath

    fun getLogFiles(): List<File> {
        return logDir.listFiles()?.filter { it.isFile && it.name.endsWith(".csv") }?.toList() ?: emptyList()
    }

    fun getCurrentFileName(): String {
        return currentFileName ?: "未开启监控"
    }

    fun startNewSession() {
        if (isRecording.get()) {
            stopSession()
        }

        // 每次创建新会话前，自动检查并清理超出上限的最旧日志
        cleanOldLogs()

        csvHeaders.clear()
        dataQueue.clear()
        
        currentFileName = generateNewFileName()
        isRecording.set(true)
        
        // 启动专属异步消费者线程，完全脱离网卡网络线程
        consumerThread = Thread({
            Log.d("LogManager", ">>> 异步日志消费线程启动成功")
            while (isRecording.get() || dataQueue.isNotEmpty()) {
                try {
                    val data = dataQueue.poll(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                    if (data != null) {
                        processAndWrite(data)
                    }
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.e("LogManager", "异步处理日志异常: ${e.message}")
                }
            }
            Log.d("LogManager", ">>> 异步日志消费线程安全退出")
        }, "NetLogConsumer-Thread").apply {
            priority = Thread.MIN_PRIORITY // 低优先级，优先保证网络收包与主线程UI
            start()
        }
        
        Log.d("LogManager", ">>> 新CSV会话开启: $currentFileName")
    }

    fun stopSession() {
        if (!isRecording.get()) return
        isRecording.set(false)
        consumerThread?.interrupt()
        consumerThread = null
        currentFileName = null
        csvHeaders.clear()
        Log.d("LogManager", ">>> CSV会话已关闭，触发消费者线程退出信号")
    }

    fun save(jsonData: String) {
        if (!isRecording.get() || currentFileName == null || jsonData.isBlank()) return
        // 仅仅做高响应入队，网络接收线程无感，绝不发生磁盘阻塞
        dataQueue.offer(jsonData)
    }

    private fun processAndWrite(jsonData: String) {
        val name = currentFileName ?: return
        try {
            val jsonObject = JSONObject(jsonData)
            val file = File(logDir, name)

            val rowValuesMap = mutableMapOf<String, String>()
            rowValuesMap["Timestamp"] = getTime()

            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val rawValue = jsonObject.optString(key, "")

                // 特别处理底噪这类逗号分隔的多通道数据（如 "88,88,70,95,96,72"）
                if (key.startsWith("noiseFloor")) {
                    val parts = rawValue.split(",")
                    for (i in parts.indices) {
                        val subKey = "${key}_ch${i + 1}"
                        val subVal = parts[i].trim()
                        rowValuesMap[subKey] = subVal
                        // 如果是第一次遇到这个通道，将其加入表头
                        if (!csvHeaders.contains(subKey)) {
                            csvHeaders.add(subKey)
                        }
                    }
                } else {
                    // 其他常规字段，直接存入 Map（绝对不要在这里进行双引号转义）
                    rowValuesMap[key] = rawValue
                    if (!csvHeaders.contains(key)) {
                        csvHeaders.add(key)
                    }
                }
            }

            // 确保 Timestamp 永远在第一列（初始化表头）
            if (csvHeaders.isEmpty() || !csvHeaders.contains("Timestamp")) {
                csvHeaders.remove("Timestamp")
                csvHeaders.add(0, "Timestamp")
                
                // 将表头落盘
                val headerLine = csvHeaders.joinToString(separator = ",") + "\n"
                file.appendText(headerLine)
            }

            // 严格按照表头顺序组装整行数据，确保无论如何不会发生错位
            val rowData = ArrayList<String>(csvHeaders.size)
            for (header in csvHeaders) {
                // 如果当前 JSON 缺了某个字段，留空占位，保证列数一致
                val rawVal = rowValuesMap[header] ?: ""
                
                // 【唯一的 CSV 安全转义处理】
                // 只有包含逗号的字符串（如 "12,22"）才会被加上双引号变为 "\"12,22\""
                val cleanValue = if (rawVal.contains(",")) {
                    "\"$rawVal\""
                } else {
                    rawVal
                }
                rowData.add(cleanValue)
            }

            // 用逗号拼接整行数据并落盘
            val dataLine = rowData.joinToString(separator = ",") + "\n"
            file.appendText(dataLine)

        } catch (e: Exception) {
            Log.e("LogManager", "落盘写入失败: ${e.message}")
        }
    }

    /**
     * 检查并自动清理超过数量上限的最旧日志文件（基于 FIFO 策略）
     */
    private fun cleanOldLogs() {
        try {
            val files = getLogFiles()
            if (files.size >= MAX_LOG_FILES) {
                // 按文件最后修改时间升序排列（最旧的在前面）
                val sortedFiles = files.sortedBy { it.lastModified() }
                
                // 计算需要清理的文件数，预留空间给即将产生的新会话
                val deleteCount = files.size - MAX_LOG_FILES + 1
                for (i in 0 until deleteCount) {
                    if (i < sortedFiles.size) {
                        val fileToDelete = sortedFiles[i]
                        if (fileToDelete.delete()) {
                            Log.d("LogManager", ">>> 自动清理超限旧日志: ${fileToDelete.name}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LogManager", "自动清理日志异常: ${e.message}")
        }
    }
}
