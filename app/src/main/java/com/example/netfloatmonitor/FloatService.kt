package com.example.netfloatmonitor

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.Timer
import java.util.TimerTask

class FloatService : Service() {

    companion object {
        const val ACTION_SHOW_FLOAT = "com.example.netfloatmonitor.ACTION_SHOW"
        const val ACTION_HIDE_FLOAT = "com.example.netfloatmonitor.ACTION_HIDE"
        const val ACTION_TOGGLE_FLOAT = "com.example.netfloatmonitor.ACTION_TOGGLE"
    }

    private var floatView: FloatView? = null
    private var receiver: UdpReceiver? = null
    private lateinit var logger: LogManager

    private var totalPackets = 0
    private var packetsInLastSecond = 0
    private var currentHz = 0
    private var statusTimer: Timer? = null
    
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        logger = LogManager(this)
        Log.d("FloatService", "Service onCreate 触发")
        createNotificationChannel()
        startForeground(1001, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        
        when (action) {
            ACTION_HIDE_FLOAT -> {
                hideFloatWindow()
                return START_STICKY
            }
            ACTION_SHOW_FLOAT -> {
                showFloatWindow()
                return START_STICKY
            }
            ACTION_TOGGLE_FLOAT -> {
                if (floatView == null) showFloatWindow() else hideFloatWindow()
                return START_STICKY
            }
        }

        // 默认启动流程
        val port = intent?.getIntExtra("PORT", 16789) ?: 16789
        totalPackets = 0
        currentHz = 0
        logger.startNewSession()
        
        showFloatWindow()
        startUdpReceive(port)
        startStatusTimer()

        mainHandler.postDelayed({
            sendStatusBroadcast()
        }, 200)
        
        return START_STICKY
    }

    private fun startUdpReceive(port: Int) {
        receiver?.stop()
        
        receiver = UdpReceiver(port) { data ->
            try {
                totalPackets++
                packetsInLastSecond++

                logger.save(data)
                
                mainHandler.post {
                    floatView?.updateJsonDynamic(data)
                }
            } catch (e: Exception) {
                Log.e("FloatService", "网络数据流分发路由异常", e)
            }
        }
        receiver?.start()
    }

    private fun startStatusTimer() {
        statusTimer?.cancel()
        statusTimer = Timer()
        statusTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                currentHz = packetsInLastSecond
                packetsInLastSecond = 0
                sendStatusBroadcast()
            }
        }, 1000, 1000)
    }

    private fun sendStatusBroadcast() {
        val intent = Intent("com.example.netfloatmonitor.STATUS_UPDATE").apply {
            putExtra("TOTAL_PACKETS", totalPackets)
            putExtra("HZ", currentHz)
            putExtra("IS_FLOAT_SHOWING", floatView != null) // 广播悬浮窗当前状态
        }
        LocalBroadcastManager.getInstance(this@FloatService).sendBroadcast(intent)
    }

    private fun showFloatWindow() {
        if (floatView != null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w("FloatService", "未授予悬浮窗权限，跳过悬浮窗显示")
            return
        }

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            format = PixelFormat.TRANSLUCENT
            x = 50
            y = 200
        }
        
        try {
            floatView = FloatView(this, wm, params)
            wm.addView(floatView, params)
            sendStatusBroadcast()
        } catch (e: Exception) {
            Log.e("FloatService", "添加悬浮窗异常: ${e.message}", e)
        }
    }

    // 隐藏/移除悬浮窗
    private fun hideFloatWindow() {
        if (floatView != null) {
            try {
                val wm = getSystemService(WINDOW_SERVICE) as WindowManager
                wm.removeView(floatView)
            } catch (e: Exception) {
                Log.e("FloatService", "移除悬浮窗异常: ${e.message}")
            }
            floatView = null
            sendStatusBroadcast()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        statusTimer?.cancel()
        statusTimer = null
        
        logger.stopSession()
        
        val intent = Intent("com.example.netfloatmonitor.STATUS_UPDATE").apply {
            putExtra("IS_STOPPED", true)
            putExtra("IS_FLOAT_SHOWING", false)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

        receiver?.stop()
        receiver = null
        
        hideFloatWindow()
        mainHandler.removeCallbacksAndMessages(null)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "net_monitor", 
                "NetFloat Monitor", 
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "net_monitor")
            .setContentTitle("NetFloat Monitor")
            .setContentText("UDP监听运行中")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }
}
