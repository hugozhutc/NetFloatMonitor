package com.example.netfloatmonitor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.util.LinkedList
import kotlin.concurrent.thread

@SuppressLint("ViewConstructor")
class FloatView(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val layoutParams = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        gravity = Gravity.TOP or Gravity.START
        x = 100
        y = 100
    }

    // UI 根布局与各组件
    private var rootLayout: LinearLayout? = null
    private var tvGndRssi: TextView? = null
    private var tvAirRssi: TextView? = null
    private var tvBandwidth: TextView? = null
    private var gndChartView: NoiseFloorChartView? = null
    private var airChartView: NoiseFloorChartView? = null

    // 网络套接字控制
    private var multicastSocket: MulticastSocket? = null
    @Volatile private var isRunning = false

    // 数据刷新 Handler
    private val mainHandler = Handler(Looper.getMainLooper())

    fun show() {
        if (rootLayout != null) return

        // 创建悬浮窗根布局
        rootLayout = object : LinearLayout(context) {
            private var initialX = 0
            private var initialY = 0
            private var touchX = 0f
            private var touchY = 0f

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouchEvent(event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        touchX = event.rawX
                        touchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        layoutParams.x = initialX + (event.rawX - touchX).toInt()
                        layoutParams.y = initialY + (event.rawY - touchY).toInt()
                        windowManager.updateViewLayout(this, layoutParams)
                        return true
                    }
                }
                return super.onTouchEvent(event)
            }
        }.apply {
            orientation = VERTICAL
            setBackgroundColor(Color.argb(180, 20, 20, 20)) // 半透明深色背景
            setPadding(20, 20, 20, 20)
        }

        // 状态文本行布局
        val textLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 初始化各个状态文本
        val textMargin = 15
        tvGndRssi = TextView(context).apply {
            text = "GND: -- dBm"
            setTextColor(Color.GREEN)
            textSize = 12f
        }
        tvAirRssi = TextView(context).apply {
            text = "AIR: -- dBm"
            setTextColor(Color.CYAN)
            textSize = 12f
            setPadding(textMargin, 0, 0, 0)
        }
        tvBandwidth = TextView(context).apply {
            text = "Rate: -- Mbps"
            setTextColor(Color.YELLOW)
            textSize = 12f
            setPadding(textMargin, 0, 0, 0)
        }

        textLayout.addView(tvGndRssi)
        textLayout.addView(tvAirRssi)
        textLayout.addView(tvBandwidth)
        rootLayout?.addView(textLayout)

        // 动态添加地面端与天空端图表控件
        val chartHeight = 160
        val chartMarginTop = 12

        gndChartView = NoiseFloorChartView(context, isAir = false).apply {
            layoutParams = LinearLayout.LayoutParams(520, chartHeight).apply {
                topMargin = chartMarginTop
            }
        }
        airChartView = NoiseFloorChartView(context, isAir = true).apply {
            layoutParams = LinearLayout.LayoutParams(520, chartHeight).apply {
                topMargin = chartMarginTop
            }
        }

        rootLayout?.addView(gndChartView)
        rootLayout?.addView(airChartView)

        // 挂载到 WindowManager
        windowManager.addView(rootLayout, layoutParams)

        // 启动网络监听
        startNetworkListening()
    }

    fun dismiss() {
        isRunning = false
        try {
            multicastSocket?.leaveGroup(InetAddress.getByName("224.0.0.1"))
            multicastSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        rootLayout?.let {
            windowManager.removeView(it)
            rootLayout = null
        }
    }

    private fun startNetworkListening() {
        isRunning = true
        thread(start = true) {
            val buffer = ByteArray(2048)
            try {
                // 配置组播地址与接收端口（假设为 8080，请根据实际传输端修改）
                val groupIp = "224.0.0.1" 
                val port = 8080
                
                multicastSocket = MulticastSocket(port).apply {
                    joinGroup(InetAddress.getByName(groupIp))
                }

                while (isRunning) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    multicastSocket?.receive(packet)

                    val message = String(packet.data, 0, packet.length).trim()
                    parseAndPayloadUpdate(message)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 协议解析解析函数，支持两种格式：
     * 1. 数传状态: STAT:GND_RSSI,AIR_RSSI,BANDWIDTH  (例如 "STAT:-65,-62,14.5")
     * 2. 地面噪声: GND_NOISE:ch1,ch2,ch3...         (例如 "GND_NOISE:65,70,58,82,90,44")
     * 3. 天空噪声: AIR_NOISE:ch1,ch2,ch3...         (例如 "AIR_NOISE:68,72,55,80,88,42")
     */
    private fun parseAndPayloadUpdate(rawMsg: String) {
        try {
            when {
                rawMsg.startsWith("STAT:") -> {
                    val dataPart = rawMsg.substring(5)
                    val tokens = dataPart.split(",")
                    if (tokens.size >= 3) {
                        mainHandler.post {
                            tvGndRssi?.text = "GND: ${tokens[0]} dBm"
                            tvAirRssi?.text = "AIR: ${tokens[1]} dBm"
                            tvBandwidth?.text = "Rate: ${tokens[2]} Mbps"
                        }
                    }
                }
                rawMsg.startsWith("GND_NOISE:") -> {
                    val csvData = rawMsg.substring(10)
                    mainHandler.post {
                        gndChartView?.addNoiseData(csvData)
                    }
                }
                rawMsg.startsWith("AIR_NOISE:") -> {
                    val csvData = rawMsg.substring(10)
                    mainHandler.post {
                        airChartView?.addNoiseData(csvData)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 自定义折线图组件 - 用于绘制各信道的底噪历史曲线
     * 修复了之前导致编译失败的变量命名错误
     */
    private class NoiseFloorChartView(context: Context, private val isAir: Boolean) : View(context) {
        private val maxDataPoints = 100
        private val yAxisWidth = 85f
        
        // 存储历史多信道数据的队列
        private val historyList = LinkedList<FloatArray>()
        
        // 各种画笔配置
        private val axisTextPaint = Paint().apply { color = Color.parseColor("#95A5A6"); textSize = 13f; isAntiAlias = true }
        private val headerTextPaint = Paint().apply { color = Color.parseColor("#E67E22"); textSize = 14f; isFakeBoldText = true; isAntiAlias = true }
        private val gridPaint = Paint().apply { color = Color.argb(30, 255, 255, 255); strokeWidth = 1f }
        private val bgPaint = Paint().apply { color = Color.argb(20, 230, 126, 34) } 

        // 支持多达 6 个信道的曲线颜色分配
        private val curveColors = intArrayOf(
            Color.parseColor("#E74C3C"), // 1通道: 红
            Color.parseColor("#F1C40F"), // 2通道: 黄
            Color.parseColor("#3498DB"), // 3通道: 蓝
            Color.parseColor("#9B59B6"), // 4通道: 紫
            Color.parseColor("#1ABC9C"), // 5通道: 青
            Color.parseColor("#E67E22")  // 6通道: 橙
        )
        private val curvePaints = Array(curveColors.size) { i ->
            Paint().apply { color = curveColors[i]; strokeWidth = 2f; style = Paint.Style.STROKE; isAntiAlias = true }
        }

        // 底噪可视的数值边界 (单位: dBm 或者 相对量化值值)
        private val noiseMin = 40f
        private val noiseMax = 140f

        fun addNoiseData(rawCsv: String) {
            try {
                val parts = rawCsv.split(",")
                val floatArray = FloatArray(parts.size)
                for (i in parts.indices) {
                    // 已修复：Parti -> i
                    floatArray[i] = parts[i].trim().toFloatOrNull() ?: 0f
                }
                historyList.addLast(floatArray)
                if (historyList.size > maxDataPoints) historyList.removeFirst()
                postInvalidate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0 || h <= 0) return

            val chartLeft = yAxisWidth
            val chartRight = w
            val chartWidth = chartRight - chartLeft
            
            // 绘制区域底色
            canvas.drawRect(chartLeft, 0f, chartRight, h, bgPaint)

            // 绘制水平背景刻度线与文字刻度
            val yPositions = floatArrayOf(h * 0.2f, h * 0.5f, h * 0.8f)
            val labels = arrayOf("140", "90", "40")
            for (i in yPositions.indices) {
                val y = yPositions[i]
                canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
                // 已修复：parti -> i
                canvas.drawText(labels[i], 20f, y + 5f, axisTextPaint)
            }

            // 标题
            val title = if (isAir) "[AIR NOISE]" else "[GND NOISE]"
            canvas.drawText(title, chartLeft + 15f, 22f, headerTextPaint)

            if (historyList.isEmpty()) return
            
            val currentChannels = historyList.last.size
            val stepX = chartWidth / (maxDataPoints - 1)
            val range = noiseMax - noiseMin

            // 遍历所有收到的信道独立绘制线条
            for (ch in 0 until currentChannels) {
                val paint = curvePaints[ch % curvePaints.size]
                
                for (i in 0 until historyList.size - 1) {
                    // 已修复：parti -> i
                    val startArray = historyList[i]
                    val endArray = historyList[i + 1]
                    
                    if (ch >= startArray.size || ch >= endArray.size) continue
                    
                    val startX = chartLeft + (i * stepX)
                    val endX = chartLeft + ((i + 1) * stepX)
                    
                    val valStart = startArray[ch].coerceIn(noiseMin, noiseMax)
                    val valEnd = endArray[ch].coerceIn(noiseMin, noiseMax)
                    
                    canvas.drawLine(
                        startX, h * (1f - (valStart - noiseMin) / range),
                        endX, h * (1f - (valEnd - noiseMin) / range),
                        paint
                    )
                }
            }

            // 右上方动态渲染图例 (CH1, CH2...)
            val legendPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
            val legendTextPaint = Paint().apply { color = Color.parseColor("#BDC3C7"); textSize = 11f; isAntiAlias = true }
            
            var legendRightX = w - 15f
            val legendY = 22f

            for (ch in (currentChannels - 1) downTo 0) {
                val chColor = curveColors[ch % curveColors.size]
                val labelStr = "CH${ch + 1}"
                
                val textWidth = legendTextPaint.measureText(labelStr)
                val itemWidth = textWidth + 14f
                
                legendPaint.color = chColor
                canvas.drawRect(legendRightX - itemWidth, legendY - 8f, legendRightX - itemWidth + 8f, legendY, legendPaint)
                
                canvas.drawText(labelStr, legendRightX - itemWidth + 12f, legendY, legendTextPaint)
                
                legendRightX -= (itemWidth + 14f)
            }
        }
    }
}
