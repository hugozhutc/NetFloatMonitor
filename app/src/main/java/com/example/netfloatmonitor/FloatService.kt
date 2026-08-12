package com.example.netfloatmonitor

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicInteger

class FloatService : Service() {

    private var floatView: FloatView? = null
    private var receiver: UdpReceiver? = null
    private lateinit var logger: LogManager

    private val totalPackets = AtomicInteger(0)
    private val packetsInLastSecond = AtomicInteger(0)
    
    private var currentHz = 0
    private var statusTimer: Timer? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    // 广播接收器：监听来自 MainActivity 的配置修改通知
    private val configReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            floatView?.postInvalidate()
        }
    }

    override fun onCreate() {
        super.onCreate()
        logger = LogManager(this)
        Log.d("FloatService", "Service onCreate 触发")
        createNotificationChannel()

        // 注册配置变动广播
        LocalBroadcastManager.getInstance(this).registerReceiver(
            configReceiver,
            IntentFilter("com.example.netfloatmonitor.CONFIG_CHANGED")
        )

        // 兼容 Android 14 前台服务类型规范
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1001, 
                createNotification(), 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
            )
        } else {
            startForeground(1001, createNotification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val port = intent?.getIntExtra("PORT", 16789) ?: 16789

        totalPackets.set(0)
        packetsInLastSecond.set(0)
        currentHz = 0
        
        logger.startNewSession()

        showFloatWindow()
        startUdpReceive(port)
        startStatusTimer()

        mainHandler.postDelayed({
            sendStatusBroadcast()
        }, 200)

        return START_NOT_STICKY
    }

    private fun startUdpReceive(port: Int) {
        receiver?.stop()

        receiver = UdpReceiver(port) { data ->
            try {
                totalPackets.incrementAndGet()
                packetsInLastSecond.incrementAndGet()

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
                currentHz = packetsInLastSecond.getAndSet(0)
                sendStatusBroadcast()
            }
        }, 1000, 1000)
    }

    private fun sendStatusBroadcast() {
        val intent = Intent("com.example.netfloatmonitor.STATUS_UPDATE").apply {
            putExtra("TOTAL_PACKETS", totalPackets.get())
            putExtra("HZ", currentHz)
        }
        LocalBroadcastManager.getInstance(this@FloatService).sendBroadcast(intent)
    }

    private fun showFloatWindow() {
        if (floatView != null) return
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams()

        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        params.format = PixelFormat.TRANSLUCENT
        params.x = 50
        params.y = 200

        floatView = FloatView(this, wm, params)
        try {
            wm.addView(floatView, params)
        } catch (e: Exception) {
            Log.e("FloatService", "添加悬浮窗失败", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        LocalBroadcastManager.getInstance(this).unregisterReceiver(configReceiver)

        statusTimer?.cancel()
        statusTimer = null

        logger.stopSession()

        val intent = Intent("com.example.netfloatmonitor.STATUS_UPDATE").apply {
            putExtra("IS_STOPPED", true)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

        receiver?.stop()
        receiver = null

        if (floatView != null) {
            try {
                val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                if (floatView?.isAttachedToWindow == true) {
                    wm.removeView(floatView)
                }
            } catch (e: Exception) {
                Log.e("FloatService", "移除悬浮窗异常: ${e.message}")
            }
            floatView = null
        }
        mainHandler.removeCallbacksAndMessages(null)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "net_monitor",
                "NetFloat Monitor",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "net_monitor")
            .setContentTitle("NetFloat Monitor")
            .setContentText("UDP 监听运行中")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
