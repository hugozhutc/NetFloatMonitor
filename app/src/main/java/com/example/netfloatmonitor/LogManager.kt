package com.example.netfloatmonitor

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class LogManager(private val context: Context) {

    // 切换至 Android/data/com.example.netfloatmonitor/files/NetFloatLogs
    // 这是外部私有目录，不需要任何动态权限，且在 Android 10+ 上完全合规
    private val logDir = File(context.getExternalFilesDir(null), "NetFloatLogs").apply {
        if (!exists()) {
            val mkdirResult = mkdirs()
            Log.d("LogManager", "创建日志文件夹: $mkdirResult, 路径: $absolutePath")
        }
    }

    private fun getFileName(): String {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return "log_${sdf.format(Date())}.txt"
    }

    private fun getTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(Date())
    }

    // ==========================================
    // 补全以下公开方法，彻底解决 MainActivity.kt 编译报错
    // ==========================================

    /**
     * 获取当前日志文件夹的绝对路径，解决 getLogPath 未定义错误
     */
    fun getLogPath(): String {
        return logDir.absolutePath
    }

    /**
     * 获取所有日志文件的列表，解决 forEach 遍历歧义与 it 无法识别错误
     */
    fun getLogFiles(): List<File> {
        return logDir.listFiles()?.filter { it.isFile && it.name.endsWith(".txt") }?.toList() ?: emptyList()
    }

    // ==========================================

    fun save(data: String) {
        if (data.isBlank()) {
            Log.w("LogManager", "警告: 尝试写入空数据，已跳过")
            return
        }

        try {
            val file = File(logDir, getFileName())
            
            // 显式确保文件已被创建
            if (!file.exists()) {
                val createResult = file.createNewFile()
                Log.d("LogManager", "成功创建新日志文件: $createResult")
            }

            val logContent = """
            ==================== TIME:${getTime()}
            $data
            ====================
            """.trimIndent() + "\n"

            file.appendText(logContent)
            Log.d("LogManager", "日志写入成功: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("LogManager", "日志写入失败: ${e.message}", e)
        }
    }
}
