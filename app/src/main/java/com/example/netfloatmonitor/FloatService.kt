package com.example.netfloatmonitor

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import android.util.Log

class FloatService : Service() {

    private var floatView: FloatView? = null
    private var receiver: UdpReceiver? = null
    private lateinit var logger: LogManager

    override fun onCreate() {
        super.onCreate()
        logger = LogManager(this)
        
        Log.d("FloatService", "Service onCreate 触发")
        logger.save("SERVICE START TEST") 
        logger.save("========== SERVICE CREATE ==========")
        
        createNotificationChannel()
        startForeground(1001, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val port = intent?.getIntExtra("PORT", 16789) ?: 16789
        showFloatWindow()
        startUdpReceive(port)
        return START_NOT_STICKY
    }

    private fun startUdpReceive(port: Int) {
        receiver?.stop()
        
        receiver = UdpReceiver(port) { data ->
            try {
                // 收到任何 UDP 数据都会实时追加进日志文件
                logger.save("UDP RECEIVE -> \n$data")
                
                // 刷新悬浮窗 UI
                floatView?.post {
                    floatView?.updateJson(data)
                }
            } catch (e: Exception) {
                Log.e("FloatService", "UI 更新或日志记录异常", e)
                logger.save("DISPLAY ERROR:\n${e.stackTraceToString()}")
            }
        }
        
        receiver?.start()
        logger.save("UDP LISTEN START PORT: $port")
    }

    private fun showFloatWindow() {
        if (floatView != null) return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams()
        
        params.width = 600
        params.height = 500
        params.type = if (Build.VERSION.SDK_INT >= 26) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        params.format = PixelFormat.TRANSLUCENT
        params.x = 50
        params.y = 200
        
        floatView = FloatView(this, wm, params)
        wm.addView(floatView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("FloatService", "Service onDestroy 触发")
        logger.save("========== SERVICE DESTROY ==========")
        
        receiver?.stop()
        receiver = null
        
        if (floatView != null) {
            try {
                val wm = getSystemService(WINDOW_SERVICE) as WindowManager
                wm.removeView(floatView)
            } catch (e: Exception) {
                Log.e("FloatService", "移除悬浮窗异常: ${e.message}")
            }
            floatView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                "net_monitor", 
                "NetFloat Monitor", 
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "net_monitor")
            .setContentTitle("NetFloat Monitor")
            .setContentText("UDP监听运行中")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()
    }
}
