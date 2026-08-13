package com.example.netfloatmonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class RestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("RestartReceiver", "接收到唤醒信号: ${intent?.action}")
        try {
            val serviceIntent = Intent(context, FloatService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("RestartReceiver", "后台唤醒服务受限: ${e.message}")
        }
    }
}
