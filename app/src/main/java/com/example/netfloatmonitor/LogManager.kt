package com.example.netfloatmonitor

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class LogManager(private val context: Context) {

    // 切换至 App 内部私有存储目录，100% 具备读写权限
    private val logDir = File(context.filesDir, "NetFloatLogs").apply {
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
