package com.example.netfloatmonitor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.util.LinkedList
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

@SuppressLint("ViewConstructor")
class FloatView(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    // 悬浮窗布局参数配置
    private val layoutParams = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or 
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        gravity = Gravity.TOP or Gravity.START
        x = 100
        y = 200
    }

    // 核心 UI 节点
    private var rootLayout: LinearLayout? = null
    private var tvGndRssi: TextView? = null
    private var tvAirRssi: TextView? = null
    private var tvBandwidth: TextView? = null
    private var gndChartView: NoiseFloorChartView? = null
    private var airChartView: NoiseFloorChartView? = null

    // 网络通信与多机状态控制核心
    private var multicastSocket: MulticastSocket? = null
    @Volatile private var isRunning = false
    private val mainHandler = Handler(Looper.getMainLooper())

    // 屏幕边界物理参数缓存（用于吸附算法）
    private val displayMetrics = DisplayMetrics()
    private var screenWidth = 1080
    private var screenHeight = 2400
    private var statusBarHeight = 72

    init {
        updateScreenDimensions()
        statusBarHeight = getSystemStatusBarHeight()
    }

    private fun updateScreenDimensions() {
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
    }

    private fun getSystemStatusBarHeight(): Int {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 75
    }

    /**
     * 构建并构建显示全局网络监控悬浮窗
     */
    fun show() {
        if (rootLayout != null) return
        updateScreenDimensions()

        // 实例化带有精密物理吸附逻辑与触控跟踪的根视图
        rootLayout = object : LinearLayout(context) {
            private var initialX = 0
            private var initialY = 0
            private var touchX = 0f
            private var touchY = 0f
            private var isDragging = false

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouchEvent(event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        touchX = event.rawX
                        touchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - touchX
                        val deltaY = event.rawY - touchY
                        if (!isDragging && (deltaX * deltaX + deltaY * deltaY > 25)) {
                            isDragging = true
                        }
                        if (isDragging) {
                            layoutParams.x = initialX + deltaX.toInt()
                            layoutParams.y = initialY + deltaY.toInt()
                            
                            // 限制悬浮窗绝对不能飞出物理屏幕边界
                            layoutParams.x = max(0, min(layoutParams.x, screenWidth - width))
                            layoutParams.y = max(statusBarHeight, min(layoutParams.y, screenHeight - height))
                            
                            windowManager.updateViewLayout(this, layoutParams)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (isDragging) {
                            // 边缘智能吸附弹性算法逻辑
                            val viewWidth = width
                            val currentCenterX = layoutParams.x + viewWidth / 2
                            val targetX = if (currentCenterX < screenWidth / 2) {
                                10 // 弹性贴左墙
                            } else {
                                screenWidth - viewWidth - 10 // 弹性贴右墙
                            }
                            
                            // 启动平滑吸附过渡线程
                            smoothAnimateToX(targetX)
                        }
                        return true
                    }
                }
                return super.onTouchEvent(event)
            }

            private fun smoothAnimateToX(targetX: Int) {
                val startX = layoutParams.x
                val steps = 10
                val delta = (targetX - startX) / steps
                var currentStep = 0
                
                val ticker = object : Runnable {
                    override fun run() {
                        if (currentStep < steps && rootLayout != null) {
                            layoutParams.x += delta
                            if (currentStep == steps - 1) {
                                layoutParams.x = targetX // 最后一帧强制校准
                            }
                            try {
                                windowManager.updateViewLayout(this@object, layoutParams)
                            } catch (e: Exception) { /* 规避Window解绑异常 */ }
                            currentStep++
                            mainHandler.postDelayed(this, 16)
                        }
                    }
                }
                mainHandler.post(ticker)
            }
        }.apply {
            orientation = VERTICAL
            setBackgroundColor(Color.argb(205, 15, 17, 20)) // 军工级半透明磨砂质感底色
            setPadding(24, 20, 24, 24)
            // 设施圆角和细边框
            val drawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.argb(210, 18, 22, 28))
                cornerRadius = 16f
                setStroke(2, Color.parseColor("#34495E"))
            }
            background = drawable
        }

        // 数据状态第一行布局 (水平排列)
        val textLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_VERTICAL
        }

        // 初始化各个网桥监控状态核心文本
        tvGndRssi = TextView(context).apply {
            text = "GND: -- dBm"
            setTextColor(Color.parseColor("#2ECC71")) // 翡翠绿
            textSize = 12.5f
            isFakeBoldText = true
        }
        tvAirRssi = TextView(context).apply {
            text = "AIR: -- dBm"
            setTextColor(Color.parseColor("#3498DB")) // 海洋蓝
            textSize = 12.5f
            isFakeBoldText = true
            setPadding(24, 0, 0, 0)
        }
        tvBandwidth = TextView(context).apply {
            text = "Rate: -- Mbps"
            setTextColor(Color.parseColor("#F1C40F")) // 明黄
            textSize = 12.5f
            isFakeBoldText = true
            setPadding(24, 0, 0, 0)
        }

        textLayout.addView(tvGndRssi)
        textLayout.addView(tvAirRssi)
        textLayout.addView(tvBandwidth)
        rootLayout?.addView(textLayout)

        // 高级多通道频谱底噪渲染双视图组件初始化
        val chartWidthPx = 560
        val chartHeightPx = 175
        
        gndChartView = NoiseFloorChartView(context, isAir = false).apply {
            layoutParams = LinearLayout.LayoutParams(chartWidthPx, chartHeightPx).apply {
                topMargin = 16
            }
        }
        airChartView = NoiseFloorChartView(context, isAir = true).apply {
            layoutParams = LinearLayout.LayoutParams(chartWidthPx, chartHeightPx).apply {
                topMargin = 14
            }
        }

        rootLayout?.addView(gndChartView)
        rootLayout?.addView(airChartView)

        // 挂载至系统顶级总线画面
        try {
            windowManager.addView(rootLayout, layoutParams)
            startNetworkListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 注销悬浮窗解绑并彻底释放网络套接字句柄
     */
    fun dismiss() {
        isRunning = false
        try {
            multicastSocket?.let { socket ->
                socket.leaveGroup(InetAddress.getByName("224.0.0.1"))
                socket.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        multicastSocket = null
        
        rootLayout?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            rootLayout = null
        }
    }

    /**
     * 独立线程的底层 UDP 组播并发异步监听引擎
     */
    private fun startNetworkListening() {
        if (!isRunning) {
            isRunning = true
            thread(name = "UAV-NetMonitor-Thread", start = true) {
                val buffer = ByteArray(4096)
                while (isRunning) {
                    try {
                        if (multicastSocket == null) {
                            val groupIp = "224.0.0.1"
                            val port = 8080
                            
                            multicastSocket = MulticastSocket(port).apply {
                                reuseAddress = true
                                // 强制绑定环回及无线物理网卡支持，避免部分真机路由不到组播
                                try {
                                    setNetworkInterface(NetworkInterface.getByInetAddress(InetAddress.getLocalHost()))
                                } catch (e: Exception) { }
                                joinGroup(InetAddress.getByName(groupIp))
                            }
                        }

                        val packet = DatagramPacket(buffer, buffer.size)
                        multicastSocket?.receive(packet)

                        if (!isRunning) break

                        val rawMessage = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
                        if (rawMessage.isNotEmpty()) {
                            parseAndPayloadUpdate(rawMessage)
                        }
                    } catch (e: Exception) {
                        // 异常时自动重置套接字以防死锁断连
                        try { multicastSocket?.close() } catch (ex: Exception){}
                        multicastSocket = null
                        Thread.sleep(1500) // 延迟重建防熔断
                    }
                }
            }
        }
    }

    /**
     * 军工网桥自定义链路层遥测通信协议高速分发分拣器
     * 格式示例：
     *  - STAT:-71,-68,28.45
     *  - GND_NOISE:55,60,72,81,95,46
     *  - AIR_NOISE:58,62,69,78,92,42
     */
    private fun parseAndPayloadUpdate(rawMsg: String) {
        try {
            val delimiterIndex = rawMsg.indexOf(":")
            if (delimiterIndex == -1) return
            
            val header = rawMsg.substring(0, delimiterIndex)
            val payload = rawMsg.substring(delimiterIndex + 1)

            when (header) {
                "STAT" -> {
                    val tokens = payload.split(",")
                    if (tokens.size >= 3) {
                        mainHandler.post {
                            tvGndRssi?.text = "GND: ${tokens[0]} dBm"
                            tvAirRssi?.text = "AIR: ${tokens[1]} dBm"
                            tvBandwidth?.text = "Rate: ${tokens[2]} Mbps"
                        }
                    }
                }
                "GND_NOISE" -> {
                    mainHandler.post {
                        gndChartView?.addNoiseData(payload)
                    }
                }
                "AIR_NOISE" -> {
                    mainHandler.post {
                        airChartView?.addNoiseData(payload)
                    }
                }
            }
        } catch (e: Exception) {
            // 防御外部黑产/畸形数据报文轰炸导致挂起
        }
    }

    /**
     * 自定义高级频谱图表组件 - 支持无内存抖动的历史多信道数据平滑瀑布渲染
     * 彻底修复了原版报错的 i 变量打错的逻辑灾难
     */
    private class NoiseFloorChartView(context: Context, private val isAir: Boolean) : View(context) {
        private val maxDataPoints = 80 // 横向最大时间窗口点数
        private val yAxisWidth = 70f
        
        // 基于链表的环形历史缓冲区
        private val historyList = LinkedList<FloatArray>()
        
        // 核心底图与文本画笔
        private val axisTextPaint = Paint().apply { color = Color.parseColor("#7F8C8D"); textSize = 14f; isAntiAlias = true }
        private val headerTextPaint = Paint().apply { color = if(isAir) Color.parseColor("#3498DB") else Color.parseColor("#2ECC71"); textSize = 15f; isFakeBoldText = true; isAntiAlias = true }
        private val gridPaint = Paint().apply { color = Color.argb(22, 255, 255, 255); strokeWidth = 1f }
        private val bgPaint = Paint().apply { color = Color.argb(12, 255, 255, 255) } 

        // 6通道硬件专用调色盘配色方案
        private val curveColors = intArrayOf(
            Color.parseColor("#E74C3C"), // CH1: 极光红
            Color.parseColor("#F1C40F"), // CH2: 琥珀黄
            Color.parseColor("#3498DB"), // CH3: 蔚蓝
            Color.parseColor("#9B59B6"), // CH4: 熏衣紫
            Color.parseColor("#1ABC9C"), // CH5: 翡翠青
            Color.parseColor("#E67E22")  // CH6: 活力橙
        )
        
        private val curvePaints = Array(curveColors.size) { i ->
            Paint().apply { 
                color = curveColors[i]
                strokeWidth = 2.2f 
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND // 圆滑折线拐点
                isAntiAlias = true 
            }
        }

        // 底噪可视测量动态物理门限区间 (dBm范围)
        private val noiseMin = 40f
        private val noiseMax = 140f

        fun addNoiseData(rawCsv: String) {
            try {
                val parts = rawCsv.split(",")
                val floatArray = FloatArray(parts.size)
                for (i in parts.indices) {
                    // 已全部修正：Parti -> 正确的循环因子 i
                    floatArray[i] = parts[i].trim().toFloatOrNull() ?: noiseMin
                }
                historyList.addLast(floatArray)
                if (historyList.size > maxDataPoints) {
                    historyList.removeFirst()
                }
                postInvalidate() // 调度触发硬件加速重绘画布
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
            
            // 1. 铺设数据绘图隔离背景区
            canvas.drawRect(chartLeft, 0f, chartRight, h, bgPaint)

            // 2. 计算并刻画 40、90、140 三级底噪纵向阶梯标尺与横向网格线
            val yPositions = floatArrayOf(h * 0.15f, h * 0.5f, h * 0.85f)
            val labels = arrayOf("140", "90", "40")
            for (i in yPositions.indices) {
                val y = yPositions[i]
                canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
                // 已全部修正：parti -> 正确的循环因子 i
                canvas.drawText(labels[i], 12f, y + 6f, axisTextPaint)
            }

            // 3. 图表左上角标识头绘制
            val title = if (isAir) "▼ AIR SENSOR SPECTRAL" else "▲ GND STATION SPECTRAL"
            canvas.drawText(title, chartLeft + 15f, 26f, headerTextPaint)

            if (historyList.isEmpty()) return
            
            val currentChannels = historyList.last.size
            val stepX = chartWidth / (maxDataPoints - 1)
            val range = noiseMax - noiseMin

            // 4. 核心绘制循环：按信道升序高维解包，将点集编译进矩阵进行画布连线
            for (ch in 0 until currentChannels) {
                val paint = curvePaints[ch % curvePaints.size]
                
                for (i in 0 until historyList.size - 1) {
                    // 已全部修正：parti -> 正确的循环因子 i
                    val startArray = historyList[i]
                    val endArray = historyList[i + 1]
                    
                    // 防御突发性的信道数量变更抖动，规避数组越界
                    if (ch >= startArray.size || ch >= endArray.size) continue
                    
                    val startX = chartLeft + (i * stepX)
                    val endX = chartLeft + ((i + 1) * stepX)
                    
                    val valStart = startArray[ch].coerceIn(noiseMin, noiseMax)
                    val valEnd = endArray[ch].coerceIn(noiseMin, noiseMax)
                    
                    // 将数值归一化投影映射成高精度的 Y 轴像素坐标
                    val startY = h * (1f - (valStart - noiseMin) / range)
                    val endY = h * (1f - (valEnd - noiseMin) / range)
                    
                    canvas.drawLine(startX, startY, endX, endY, paint)
                }
            }

            // 5. 动态图例右上角逆向溢出浮动对齐渲染 (CH1...CHn)
            val legendPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
            val legendTextPaint = Paint().apply { color = Color.parseColor("#BDC3C7"); textSize = 11.5f; isAntiAlias = true }
            
            var legendRightX = w - 20f
            val legendY = 24f

            for (ch in (currentChannels - 1) downTo 0) {
                val chColor = curveColors[ch % curveColors.size]
                val labelStr = "CH${ch + 1}"
                
                val textWidth = legendTextPaint.measureText(labelStr)
                val itemWidth = textWidth + 16f
                
                legendPaint.color = chColor
                // 画圆形或者方块指示标
                canvas.drawRoundRect(
                    legendRightX - itemWidth, legendY - 9f, 
                    legendRightX - itemWidth + 8f, legendY - 1f, 
                    2f, 2f, legendPaint
                )
                
                canvas.drawText(labelStr, legendRightX - itemWidth + 14f, legendY, legendTextPaint)
                legendRightX -= (itemWidth + 18f)
            }
        }
    }
}
