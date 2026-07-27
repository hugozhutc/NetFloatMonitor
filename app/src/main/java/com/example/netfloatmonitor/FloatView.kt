package com.example.netfloatmonitor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.util.LinkedList

class FloatView(
    context: Context,
    private val windowManager: WindowManager,
    private val params: WindowManager.LayoutParams
) : LinearLayout(context) {

    private val airLayout = LinearLayout(context)
    private val gndLayout = LinearLayout(context)
    
    // 右侧第三栏：垂直容器，用于上下堆叠放两个图表
    private val chartContainer = LinearLayout(context)
    private val airChartView = WaveformView(context, isAir = true)
    private val gndChartView = WaveformView(context, isAir = false)

    private var isExpanded = true
    // 调整展开后的默认宽高，给予上下双图表和Y轴刻度更充裕的展示空间
    private var lastExpandedWidth = 1040
    private var lastExpandedHeight = 540
    private val collapsedSize = 160

    private var startWidth = 0
    private var startHeight = 0
    private var downX = 0f
    private var downY = 0f
    private var resize = false

    private val topBar = LinearLayout(context)
    private val contentFrame = FrameLayout(context)
    private val contentPanel = LinearLayout(context)
    
    private val resizeIndicator = View(context).apply {
        val triangleBg = GradientDrawable().apply {
            setColor(Color.parseColor("#3498DB"))
            cornerRadius = 4f
        }
        background = triangleBg
        visibility = View.VISIBLE
    }

    private val toggleBtn = Button(context).apply {
        text = "×"
        textSize = 14f
        setTextColor(Color.WHITE)
        setGravity(Gravity.CENTER)
        val btnBg = GradientDrawable().apply {
            setColor(Color.parseColor("#C0392B"))
            cornerRadius = 6f
        }
        background = btnBg
    }

    init {
        this.setOrientation(LinearLayout.VERTICAL)
        this.setPadding(8, 6, 8, 8)

        val bg = GradientDrawable()
        bg.setColor(Color.argb(180, 0, 0, 0))
        bg.cornerRadius = 10f
        this.setBackground(bg)

        topBar.setOrientation(LinearLayout.HORIZONTAL)
        topBar.setGravity(Gravity.RIGHT or Gravity.CENTER_VERTICAL)
        topBar.setPadding(0, 0, 4, 4)
        
        val btnLp = LinearLayout.LayoutParams(45, 45)
        topBar.addView(toggleBtn, btnLp)
        addView(topBar)

        contentPanel.setOrientation(LinearLayout.HORIZONTAL)
        airLayout.setOrientation(LinearLayout.VERTICAL)
        gndLayout.setOrientation(LinearLayout.VERTICAL)
        
        // 1. 左侧和中间的数据文本面板
        contentPanel.addView(createPanel("AIR", airLayout))
        contentPanel.addView(createPanel("GND", gndLayout))
        
        // 2. 配置右侧第三栏容器（上下平分摆放两个折线图）
        chartContainer.setOrientation(LinearLayout.VERTICAL)
        
        // 天空端图表占用 0.5 权重，带 8dp 下边距分隔
        val airChartLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply {
            setMargins(0, 0, 0, 8)
        }
        // 地面端图表占用 0.5 权重
        val gndChartLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        
        chartContainer.addView(airChartView, airChartLp)
        chartContainer.addView(gndChartView, gndChartLp)
        
        // 将整个右侧第三栏加进主面板，分配 380dp 宽度给Y轴刻度和曲线留下充裕空间
        val chartContainerLp = LinearLayout.LayoutParams(380, LinearLayout.LayoutParams.MATCH_PARENT).apply {
            setMargins(12, 0, 4, 0)
        }
        contentPanel.addView(chartContainer, chartContainerLp)
        
        contentFrame.addView(contentPanel, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        
        val indicatorLp = FrameLayout.LayoutParams(15, 15).apply {
            gravity = Gravity.BOTTOM or Gravity.RIGHT
            setMargins(0, 0, 4, 4)
        }
        contentFrame.addView(resizeIndicator, indicatorLp)
        
        val frameLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        addView(contentFrame, frameLp)

        toggleBtn.setOnTouchListener(object : OnTouchListener {
            private var btnDownX = 0f
            private var btnDownY = 0f
            private var isDragging = false

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                if (isExpanded) return false

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        btnDownX = event.rawX
                        btnDownY = event.rawY
                        isDragging = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - btnDownX
                        val dy = event.rawY - btnDownY
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isDragging = true
                        }
                        if (isDragging) {
                            params.x += (event.rawX - downX).toInt()
                            params.y += (event.rawY - downY).toInt()
                            downX = event.rawX
                            downY = event.rawY
                            
                            val maxAllowableY = getScreenHeight() - getNavigationBarHeight() - height
                            if (params.y > maxAllowableY) {
                                params.y = maxAllowableY
                            }

                            windowManager.updateViewLayout(this@FloatView, params)
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) performToggle()
                    }
                }
                return true
            }
        })

        toggleBtn.setOnClickListener {
            if (isExpanded) performToggle()
        }

        setOnTouchListener(object : OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        startWidth = width
                        startHeight = height
                        resize = isExpanded && (event.x > (width - 120)) && (event.y > (height - 120))
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val location = IntArray(2)
                        this@FloatView.getLocationOnScreen(location)
                        val absoluteY = location[1]

                        val navBarHeight = getNavigationBarHeight()
                        val usableScreenHeight = getScreenHeight() - navBarHeight

                        if (resize) {
                            val newWidth = (startWidth + event.rawX - downX).toInt().coerceAtLeast(500)
                            var newHeight = (startHeight + event.rawY - downY).toInt().coerceAtLeast(200)
                            
                            if (absoluteY + newHeight > usableScreenHeight) {
                                newHeight = usableScreenHeight - absoluteY
                            }

                            params.width = newWidth
                            params.height = newHeight
                            
                            lastExpandedWidth = newWidth
                            lastExpandedHeight = newHeight
                        } else {
                            params.x += (event.rawX - downX).toInt()
                            var targetY = params.y + (event.rawY - downY).toInt()
                            
                            if (targetY + height > usableScreenHeight) {
                                targetY = usableScreenHeight - height
                            }
                            
                            params.y = targetY
                            downX = event.rawX
                            downY = event.rawY
                        }
                        windowManager.updateViewLayout(this@FloatView, params)
                    }
                }
                return true
            }
        })
    }

    private fun getNavigationBarHeight(): Int {
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    private fun getScreenHeight(): Int {
        return context.resources.displayMetrics.heightPixels
    }

    private fun performToggle() {
        val panelBg = GradientDrawable()
        
        if (isExpanded) {
            isExpanded = false
            contentFrame.visibility = View.GONE
            resizeIndicator.visibility = View.GONE
            
            val collapsedLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            toggleBtn.layoutParams = collapsedLp
            
            toggleBtn.text = "展开"
            toggleBtn.textSize = 14f
            val btnBg = GradientDrawable().apply {
                setColor(Color.parseColor("#1ABC9C"))
                cornerRadius = 80f
            }
            toggleBtn.background = btnBg
            
            panelBg.setColor(Color.TRANSPARENT)
            this.setBackground(panelBg)
            this.setPadding(0, 0, 0, 0)

            params.width = collapsedSize
            params.height = collapsedSize
        } else {
            isExpanded = true
            contentFrame.visibility = View.VISIBLE
            resizeIndicator.visibility = View.VISIBLE
            
            val expandedLp = LinearLayout.LayoutParams(45, 45)
            toggleBtn.layoutParams = expandedLp
            
            toggleBtn.text = "×"
            toggleBtn.textSize = 14f
            val btnBg = GradientDrawable().apply {
                setColor(Color.parseColor("#C0392B"))
                cornerRadius = 6f
            }
            toggleBtn.background = btnBg
            
            panelBg.setColor(Color.argb(180, 0, 0, 0))
            panelBg.cornerRadius = 10f
            this.setBackground(panelBg)
            this.setPadding(8, 6, 8, 8)

            params.width = lastExpandedWidth
            params.height = lastExpandedHeight
        }
        windowManager.updateViewLayout(this@FloatView, params)
    }

    private fun createPanel(title: String, containerLayout: LinearLayout): View {
        val box = LinearLayout(context)
        box.setOrientation(LinearLayout.VERTICAL)

        val titleView = TextView(context)
        titleView.text = title
        titleView.textSize = 14f
        titleView.setTextColor(Color.GREEN)
        box.addView(titleView)

        val scroll = ScrollView(context)
        scroll.addView(containerLayout)

        val lp = LinearLayout.LayoutParams(300, LinearLayout.LayoutParams.MATCH_PARENT)
        box.addView(scroll, lp)

        return box
    }

    fun updateJson(json: String) {
        try {
            if (json.isBlank()) return
            val obj = JSONObject(json)

            airLayout.removeAllViews()
            gndLayout.removeAllViews()

            var airRssi1: Float? = null
            var airRssi2: Float? = null
            var airSnr: Float? = null
            
            var gndRssi1: Float? = null
            var gndRssi2: Float? = null
            var gndSnr: Float? = null

            obj.keys().forEach { key ->
                val valueStr = obj.get(key).toString()
                val lowerKey = key.lowercase()
                // 防御性转化为绝对正数值
                val numValue = valueStr.toFloatOrNull()?.let { Math.abs(it) }

                if (numValue != null) {
                    when {
                        lowerKey.endsWith("_a") -> {
                            when {
                                lowerKey.contains("rssi1") -> airRssi1 = numValue
                                lowerKey.contains("rssi2") -> airRssi2 = numValue
                                lowerKey.contains("rssi") && airRssi1 == null -> airRssi1 = numValue
                                lowerKey.contains("snr") -> airSnr = numValue
                            }
                        }
                        lowerKey.endsWith("_g") -> {
                            when {
                                lowerKey.contains("rssi1") -> gndRssi1 = numValue
                                lowerKey.contains("rssi2") -> gndRssi2 = numValue
                                lowerKey.contains("rssi") && gndRssi1 == null -> gndRssi1 = numValue
                                lowerKey.contains("snr") -> gndSnr = numValue
                            }
                        }
                        lowerKey.contains("air_rssi1") -> airRssi1 = numValue
                        lowerKey.contains("air_rssi2") -> airRssi2 = numValue
                        lowerKey.contains("air_snr") -> airSnr = numValue
                        lowerKey.contains("gnd_rssi1") -> gndRssi1 = numValue
                        lowerKey.contains("gnd_rssi2") -> gndRssi2 = numValue
                        lowerKey.contains("gnd_snr") -> gndSnr = numValue
                    }
                }

                when {
                    key.endsWith("_g") -> addItem(gndLayout, key, valueStr)
                    key.endsWith("_a") -> addItem(airLayout, key, valueStr)
                    else -> addItem(airLayout, key, valueStr)
                }
            }

            airChartView.addData(airRssi1, airRssi2, airSnr)
            gndChartView.addData(gndRssi1, gndRssi2, gndSnr)

        } catch (e: Exception) {
            airLayout.removeAllViews()
            gndLayout.removeAllViews()
            addItem(airLayout, "JSON_ERROR", e.message ?: "Unknown Error")
        }
    }

    private fun addItem(layout: LinearLayout, key: String, value: String) {
        val tv = TextView(context)
        tv.text = "$key : $value"
        tv.textSize = 12f
        tv.setTextColor(Color.WHITE)
        tv.setPadding(4, 3, 4, 3)
        layout.addView(tv)
    }

    /**
     * 内部波形图绘制组件
     * 适配：全正数正向映射（0在最底部，数值变大曲线往上攀升）
     */
    private class WaveformView(context: Context, private val isAir: Boolean) : View(context) {
        private val maxDataPoints = 40
        private val yAxisWidth = 85f // 左侧预留给Y轴刻度标签的像素宽度

        private val rssi1List = LinkedList<Float>()
        private val rssi2List = LinkedList<Float>()
        private val snrList = LinkedList<Float>()

        private val axisTextPaint = Paint().apply {
            color = Color.parseColor("#BDC3C7")
            textSize = 16f
            isAntiAlias = true
        }

        private val textPaint = Paint().apply {
            color = Color.LTGRAY
            textSize = 18f
            isAntiAlias = true
        }

        private val paintRssi1 = Paint().apply {
            color = if (isAir) Color.parseColor("#2980B9") else Color.parseColor("#D35400")
            strokeWidth = 4f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        private val paintRssi2 = Paint().apply {
            color = if (isAir) Color.parseColor("#3498DB") else Color.parseColor("#E67E22")
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        private val paintSnr = Paint().apply {
            color = if (isAir) Color.parseColor("#2ECC71") else Color.parseColor("#9B59B6")
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        private val gridPaint = Paint().apply {
            color = Color.argb(45, 255, 255, 255)
            strokeWidth = 1f
        }

        private val bgPaint = Paint().apply {
            color = Color.argb(30, 255, 255, 255)
        }

        // --- 重新划定量程：从 0 开始 ---
        // RSSI 区间：0 到 120（0映射在底部，120映射在顶部）
        private val rssiMin = 0f
        private val rssiMax = 120f
        
        // SNR 区间：0 到 50（0映射在底部，50映射在顶部）
        private val snrMin = 0f
        private val snrMax = 50f

        fun addData(r1: Float?, r2: Float?, snr: Float?) {
            // 兜底值设定为 0（即默认从最底部刷新）
            rssi1List.addLast(r1 ?: rssi1List.lastOrNull() ?: 0f)
            rssi2List.addLast(r2 ?: rssi2List.lastOrNull() ?: 0f)
            snrList.addLast(snr ?: snrList.lastOrNull() ?: 0f)

            if (rssi1List.size > maxDataPoints) rssi1List.removeFirst()
            if (rssi2List.size > maxDataPoints) rssi2List.removeFirst()
            if (snrList.size > maxDataPoints) snrList.removeFirst()

            postInvalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0 || h <= 0) return

            val chartLeft = yAxisWidth
            val chartRight = w
            val chartWidth = chartRight - chartLeft

            // 1. 绘制独立的图表波形限制背景区域
            canvas.drawRect(chartLeft, 0f, chartRight, h, bgPaint)

            // 2. 绘制三条水平参考线及对应的 Y 轴刻度文字
            val yPositions = floatArrayOf(h * 0.15f, h * 0.5f, h * 0.85f)
            // 刻度排列调整为正向：最上面是最大值，最下面是最基础的 0
            val rssiLabels = arrayOf("120", "60", "0")
            val snrLabels = arrayOf("50", "25", "0")

            for (i in yPositions.indices) {
                val y = yPositions[i]
                // 绘制网格横线
                canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
                
                // 输出双边复合刻度，格式如 "120(50)", "60(25)", "0(0)"
                val labelText = "${rssiLabels[i]}(${snrLabels[i]})"
                canvas.drawText(labelText, 5f, y + 6f, axisTextPaint)
            }

            // 3. 绘制顶部实时数字看板输出
            val prefix = if (isAir) "AIR" else "GND"
            canvas.drawText("[$prefix] R1: ${rssi1List.lastOrNull()?.toInt()}", chartLeft + 15f, 25f, textPaint)
            canvas.drawText("R2: ${rssi2List.lastOrNull()?.toInt()}", chartLeft + 140f, 25f, textPaint)
            canvas.drawText("SNR: ${snrList.lastOrNull()?.toInt()}", chartLeft + 240f, 25f, textPaint)

            // 4. 执行常规正向比例函数渲染折线
            // 0 在最底部，数值越大曲线位置越高
            drawNormalCurve(canvas, rssi1List, rssiMin, rssiMax, chartLeft, chartWidth, h, paintRssi1)
            drawNormalCurve(canvas, rssi2List, rssiMin, rssiMax, chartLeft, chartWidth, h, paintRssi2)
            drawNormalCurve(canvas, snrList, snrMin, snrMax, chartLeft, chartWidth, h, paintSnr)
        }

        /**
         * 正向折线坐标映射逻辑
         * 作用：输入数值越大，算出的坐标 Y 越接近 0（即在 Android View 体系的最上方，视觉上的高处）
         */
        private fun drawNormalCurve(canvas: Canvas, list: List<Float>, minVal: Float, maxVal: Float, leftOffset: Float, cWidth: Float, h: Float, paint: Paint) {
            if (list.size < 2) return
            val stepX = cWidth / (maxDataPoints - 1)
            val range = maxVal - minVal

            for (i in 0 until list.size - 1) {
                val startX = leftOffset + (i * stepX)
                val endX = leftOffset + ((i + 1) * stepX)

                val valStart = list[i].coerceIn(minVal, maxVal)
                val valEnd = list[i + 1].coerceIn(minVal, maxVal)

                // 核心标准公式：h * (1f - 比例)，确保 0 值落在底部，大值爬升到顶部
                val startY = h * (1f - (valStart - minVal) / range)
                val endY = h * (1f - (valEnd - minVal) / range)

                canvas.drawLine(startX, startY, endX, endY, paint)
            }
        }
    }
}
