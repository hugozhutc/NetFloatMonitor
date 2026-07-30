package com.example.netfloatmonitor

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.util.Log
import java.util.Timer
import java.util.TimerTask

class FloatService : Service() {

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
        
        return START_NOT_STICKY
    }

    private fun startUdpReceive(port: Int) {
        receiver?.stop()
        
        receiver = UdpReceiver(port) { data ->
            try {
                totalPackets++
                packetsInLastSecond++

                // 1. 投递到异步落盘缓冲队列，耗时接近 0ms，彻底避开网卡线程卡顿
                logger.save(data)
                
                // 2. 在子线程中直接完成高性能解析，减轻主线程压力
                val parsedStatus = JsonParser.parse(data)
                
                // 3. 将解析后的强类型实体单向派发给 UI 面板静态复用刷新
                mainHandler.post {
                    floatView?.updateData(parsedStatus)
                }
            } catch (e: Exception) {
                Log.e("FloatService", "网络数据流高速分发路由异常", e)
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
        }
        LocalBroadcastManager.getInstance(this@FloatService).sendBroadcast(intent)
    }

    private fun showFloatWindow() {
        if (floatView != null) return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams()
        
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
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
                val wm = getSystemService(WINDOW_SERVICE) as WindowManager
                wm.removeView(floatView)
            } catch (e: Exception) {
                Log.e("FloatService", "移除悬浮窗异常: ${e.message}")
            }
            floatView = null
        }
        mainHandler.removeCallbacksAndMessages(null)
    }

    override fun onBind(intent: Intent?): IBinder? = null

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
