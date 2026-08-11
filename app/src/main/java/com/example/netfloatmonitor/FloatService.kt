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

    // ==============================
    // 语音提醒
    // ==============================

    private lateinit var voiceAlert: VoiceAlertManager

    private var voiceEnabled = true

    // RSSI / SNR 告警阈值
    private val rssiThreshold = 90f
    private val snrThreshold = 8f

    // 连续多久没有收到UDP数据认为链路断开
    private val linkTimeoutMs = 3000L

    private var lastPacketTime = 0L

    private var linkLost = false

    // 防止每一个数据包都重复进行过多判断
    private var lastVoiceCheckTime = 0L

    // ==============================
    // 原有状态
    // ==============================

    private var totalPackets = 0
    private var packetsInLastSecond = 0
    private var currentHz = 0
    private var statusTimer: Timer? = null

    private val mainHandler =
        Handler(Looper.getMainLooper())

    override fun onCreate() {

        super.onCreate()

        logger = LogManager(this)

        // 初始化语音
        voiceAlert = VoiceAlertManager(this)

        Log.d(
            "FloatService",
            "Service onCreate 触发"
        )

        createNotificationChannel()

        startForeground(
            1001,
            createNotification()
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        val port =
            intent?.getIntExtra(
                "PORT",
                16789
            ) ?: 16789

        totalPackets = 0
        packetsInLastSecond = 0
        currentHz = 0

        lastPacketTime =
            System.currentTimeMillis()

        linkLost = false

        voiceAlert.reset()

        logger.startNewSession()

        showFloatWindow()

        startUdpReceive(port)

        startStatusTimer()

        mainHandler.postDelayed(
            {
                sendStatusBroadcast()
            },
            200
        )

        return START_NOT_STICKY
    }

    private fun startUdpReceive(
        port: Int
    ) {

        receiver?.stop()

        receiver = UdpReceiver(port) { data ->

            try {

                totalPackets++
                packetsInLastSecond++

                // 收到有效UDP数据
                lastPacketTime =
                    System.currentTimeMillis()

                // 如果之前处于断开状态，现在恢复
                if (linkLost) {

                    linkLost = false

                    mainHandler.post {

                        voiceAlert.onLinkRecovered()

                    }
                }

                // 保存日志
                logger.save(data)

                // 语音告警判断
                checkVoiceAlert(data)

                // 更新悬浮窗
                mainHandler.post {

                    floatView?.updateJsonDynamic(data)

                }

            } catch (e: Exception) {

                Log.e(
                    "FloatService",
                    "网络数据流分发路由异常",
                    e
                )
            }
        }

        receiver?.start()
    }

    /**
     * 语音告警判断
     */
    private fun checkVoiceAlert(
        json: String
    ) {

        // 控制检查频率
        val now =
            System.currentTimeMillis()

        if (now - lastVoiceCheckTime < 500) {
            return
        }

        lastVoiceCheckTime = now

        try {

            val status =
                JsonParser.parse(json)

            // =========================
            // AIR RSSI
            // =========================

            val airRssi1 =
                status.airRssi1.toFloatOrNull()

            val airRssi2 =
                status.airRssi2.toFloatOrNull()

            if (
                (airRssi1 != null &&
                 airRssi1 >= rssiThreshold) ||

                (airRssi2 != null &&
                 airRssi2 >= rssiThreshold)
            ) {

                if (voiceEnabled) {

                    mainHandler.post {

                        voiceAlert.speakAlert(
                            "air_rssi",
                            "警告，空中链路RSSI过低"
                        )
                    }
                }
            }

            // =========================
            // GND RSSI
            // =========================

            val gndRssi1 =
                status.gndRssi1.toFloatOrNull()

            val gndRssi2 =
                status.gndRssi2.toFloatOrNull()

            if (
                (gndRssi1 != null &&
                 gndRssi1 >= rssiThreshold) ||

                (gndRssi2 != null &&
                 gndRssi2 >= rssiThreshold)
            ) {

                if (voiceEnabled) {

                    mainHandler.post {

                        voiceAlert.speakAlert(
                            "gnd_rssi",
                            "警告，地面链路RSSI过低"
                        )
                    }
                }
            }

            // =========================
            // AIR SNR
            // =========================

            val airSnr =
                status.airSnr.toFloatOrNull()

            if (
                airSnr != null &&
                airSnr < snrThreshold
            ) {

                if (voiceEnabled) {

                    mainHandler.post {

                        voiceAlert.speakAlert(
                            "air_snr",
                            "警告，空中链路信噪比过低"
                        )
                    }
                }
            }

            // =========================
            // GND SNR
            // =========================

            val gndSnr =
                status.gndSnr.toFloatOrNull()

            if (
                gndSnr != null &&
                gndSnr < snrThreshold
            ) {

                if (voiceEnabled) {

                    mainHandler.post {

                        voiceAlert.speakAlert(
                            "gnd_snr",
                            "警告，地面链路信噪比过低"
                        )
                    }
                }
            }

        } catch (e: Exception) {

            Log.e(
                "VoiceAlert",
                "语音告警解析失败",
                e
            )
        }
    }

    /**
     * 检查链路是否超时
     */
    private fun checkLinkTimeout() {

        if (lastPacketTime <= 0) {
            return
        }

        val now =
            System.currentTimeMillis()

        val elapsed =
            now - lastPacketTime

        if (
            elapsed >= linkTimeoutMs &&
            !linkLost
        ) {

            linkLost = true

            if (voiceEnabled) {

                voiceAlert.onLinkDisconnected()
            }

            Log.w(
                "FloatService",
                "UDP数据超过${linkTimeoutMs}ms未收到，判定链路断开"
            )
        }
    }

    private fun startStatusTimer() {

        statusTimer?.cancel()

        statusTimer = Timer()

        statusTimer?.scheduleAtFixedRate(

            object : TimerTask() {

                override fun run() {

                    currentHz =
                        packetsInLastSecond

                    packetsInLastSecond = 0

                    // 检查链路超时
                    checkLinkTimeout()

                    sendStatusBroadcast()
                }
            },

            1000,
            1000
        )
    }

    private fun sendStatusBroadcast() {

        val intent =
            Intent(
                "com.example.netfloatmonitor.STATUS_UPDATE"
            ).apply {

                putExtra(
                    "TOTAL_PACKETS",
                    totalPackets
                )

                putExtra(
                    "HZ",
                    currentHz
                )

                putExtra(
                    "LINK_LOST",
                    linkLost
                )
            }

        LocalBroadcastManager
            .getInstance(this@FloatService)
            .sendBroadcast(intent)
    }

    private fun showFloatWindow() {

        if (floatView != null) return

        val wm =
            getSystemService(
                WINDOW_SERVICE
            ) as WindowManager

        val params =
            WindowManager.LayoutParams()

        params.width =
            WindowManager.LayoutParams.WRAP_CONTENT

        params.height =
            WindowManager.LayoutParams.WRAP_CONTENT

        params.type =
            if (Build.VERSION.SDK_INT >= 26) {

                WindowManager.LayoutParams
                    .TYPE_APPLICATION_OVERLAY

            } else {

                WindowManager.LayoutParams
                    .TYPE_PHONE
            }

        params.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

        params.format =
            PixelFormat.TRANSLUCENT

        params.x = 50
        params.y = 200

        floatView =
            FloatView(
                this,
                wm,
                params
            )

        wm.addView(
            floatView,
            params
        )
    }

    override fun onDestroy() {

        super.onDestroy()

        statusTimer?.cancel()
        statusTimer = null

        logger.stopSession()

        // 停止语音
        try {

            voiceAlert.shutdown()

        } catch (e: Exception) {

            Log.e(
                "FloatService",
                "关闭语音模块异常",
                e
            )
        }

        val intent =
            Intent(
                "com.example.netfloatmonitor.STATUS_UPDATE"
            ).apply {

                putExtra(
                    "IS_STOPPED",
                    true
                )
            }

        LocalBroadcastManager
            .getInstance(this)
            .sendBroadcast(intent)

        receiver?.stop()

        receiver = null

        if (floatView != null) {

            try {

                val wm =
                    getSystemService(
                        WINDOW_SERVICE
                    ) as WindowManager

                wm.removeView(floatView)

            } catch (e: Exception) {

                Log.e(
                    "FloatService",
                    "移除悬浮窗异常: ${e.message}"
                )
            }

            floatView = null
        }

        mainHandler
            .removeCallbacksAndMessages(null)
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? = null

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            val channel =
                NotificationChannel(
                    "net_monitor",
                    "NetFloat Monitor",
                    NotificationManager
                        .IMPORTANCE_LOW
                )

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }

    private fun createNotification(): Notification {

        return NotificationCompat
            .Builder(
                this,
                "net_monitor"
            )
            .setContentTitle(
                "NetFloat Monitor"
            )
            .setContentText(
                "UDP监听运行中"
            )
            .setSmallIcon(
                android.R.drawable
                    .ic_menu_info_details
            )
            .build()
    }
}
