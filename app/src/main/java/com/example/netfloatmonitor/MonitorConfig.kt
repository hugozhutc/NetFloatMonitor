package com.example.netfloatmonitor

import android.content.Context

object MonitorConfig {
    private const val PREF_NAME = "net_float_monitor_prefs"

    var isFloatEnabled: Boolean = true      // 悬浮窗总开关
    var isChartEnabled: Boolean = true      // 实时曲线图总开关
    var isLoggingEnabled: Boolean = false   // CSV 日志导出总开关

    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        isFloatEnabled = sp.getBoolean("isFloatEnabled", true)
        isChartEnabled = sp.getBoolean("isChartEnabled", true)
        isLoggingEnabled = sp.getBoolean("isLoggingEnabled", false)
    }

    fun save(context: Context) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().apply {
            putBoolean("isFloatEnabled", isFloatEnabled)
            putBoolean("isChartEnabled", isChartEnabled)
            putBoolean("isLoggingEnabled", isLoggingEnabled)
            apply()
        }
    }
}
