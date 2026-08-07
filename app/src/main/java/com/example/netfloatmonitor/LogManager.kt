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

    private val logDir = File(context.getExternalFilesDir(null), "NetFloatLogs").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    private val isRecording = AtomicBoolean(false)
    private var currentFileName: String? = null
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

            // 用于当前行实际填入的数据映射
            val rowValuesMap = mutableMapOf<String, String>()
            rowValuesMap["Timestamp"] = getTime()

            // 1. 递归或平铺解析 JSON 中的普通字段与数组字段（如底噪 channels）
            fun flattenJson(prefix: String, obj: JSONObject) {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val fullKey = if (prefix.isEmpty()) key else "$prefix.$key"
                    val value = obj.opt(key)

                    when (value) {
                        is JSONObject -> {
                            flattenJson(fullKey, value)
                        }
                        is org.json.JSONArray -> {
                            // 如果是数组（例如底噪列表），将其展开为 ch1, ch2, ch3...
                            for (i in 0 until value.length()) {
                                val itemKey = "${fullKey}_ch${i + 1}"
                                val itemVal = value.optString(i, "")
                                rowValuesMap[itemKey] = itemVal
                                if (!csvHeaders.contains(itemKey)) {
                                    csvHeaders.add(itemKey)
                                }
                            }
                        }
                        else -> {
                            val strVal = value?.toString() ?: ""
                            val cleanVal = if (strVal.contains(",")) "\"$strVal\"" else strVal
                            rowValuesMap[fullKey] = cleanVal
                            if (!csvHeaders.contains(fullKey)) {
                                csvHeaders.add(fullKey)
                            }
                        }
                    }
                }
            }

            // 执行解析
            flattenJson("", jsonObject)

            // 2. 如果是第一次写入，初始化 CSV 表头 (保证 Timestamp 永远在第一列)
            if (csvHeaders.isEmpty() || !csvHeaders.contains("Timestamp")) {
                csvHeaders.remove("Timestamp")
                csvHeaders.add(0, "Timestamp")
                
                val headerLine = csvHeaders.joinToString(separator = ",") + "\n"
                file.appendText(headerLine)
            }

            // 3. 严格按照表头顺序组装整行数据
            val rowData = ArrayList<String>(csvHeaders.size)
            for (header in csvHeaders) {
                val rawVal = rowValuesMap[header] ?: ""
                val cleanValue = if (rawVal.contains(",")) "\"$rawVal\"" else rawVal
                rowData.add(cleanValue)
            }

            val dataLine = rowData.joinToString(separator = ",") + "\n"
            file.appendText(dataLine)

        } catch (e: Exception) {
            Log.e("LogManager", "落盘写入失败: ${e.message}")
        }
    }
}
