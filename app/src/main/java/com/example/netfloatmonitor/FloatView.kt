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
    
    private val chartContainer = LinearLayout(context)
    private val airChartView = WaveformView(context, isAir = true)
    private val gndChartView = WaveformView(context, isAir = false)

    private var isExpanded = true
    private var lastExpandedWidth = 1300
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
    
    // 核心性能优化：静态缓存所有动态 Key 的 TextView，完美支持全量字段且无需 removeAllViews()
    private val airTextViewMap = HashMap<String, TextView>()
    private val gndTextViewMap = HashMap<String, TextView>()

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
        this.orientation = LinearLayout.VERTICAL
        this.setPadding(8, 6, 8, 8)

        val bg = GradientDrawable()
        bg.setColor(Color.argb(180, 0, 0, 0))
        bg.cornerRadius = 10f
        this.background = bg

        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        topBar.setPadding(0, 0, 4, 4)
        
        val btnLp = LinearLayout.LayoutParams(45, 45)
        topBar.addView(toggleBtn, btnLp)
        addView(topBar)

        contentPanel.orientation = LinearLayout.HORIZONTAL
        airLayout.orientation = LinearLayout.VERTICAL
        gndLayout.orientation = LinearLayout.VERTICAL
        
        contentPanel.addView(createPanel("AIR", airLayout))
        contentPanel.addView(createPanel("GND", gndLayout))
        
        chartContainer.orientation = LinearLayout.VERTICAL
        
        val airChartLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply {
            setMargins(0, 0, 0, 8)
        }
        val gndChartLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        
        chartContainer.addView(airChartView, airChartLp)
        chartContainer.addView(gndChartView, gndChartLp)
        
        val chartContainerLp = LinearLayout.LayoutParams(700, LinearLayout.LayoutParams.MATCH_PARENT).apply {
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
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
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
            
            toggleBtn.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            toggleBtn.text = "Link"
            toggleBtn.textSize = 14f
            toggleBtn.background = GradientDrawable().apply {
                setColor(Color.parseColor("#1ABC9C"))
                cornerRadius = 80f
            }
            
            panelBg.setColor(Color.TRANSPARENT)
            this.background = panelBg
            this.setPadding(0, 0, 0, 0)
            params.width = collapsedSize
            params.height = collapsedSize
        } else {
            isExpanded = true
            contentFrame.visibility = View.VISIBLE
            resizeIndicator.visibility = View.VISIBLE
            
            toggleBtn.layoutParams = LinearLayout.LayoutParams(45, 45)
            toggleBtn.text = "×"
            toggleBtn.textSize = 14f
            toggleBtn.background = GradientDrawable().apply {
                setColor(Color.parseColor("#C0392B"))
                cornerRadius = 6f
            }
            
            panelBg.setColor(Color.argb(180, 0, 0, 0))
            panelBg.cornerRadius = 10f
            this.background = panelBg
            this.setPadding(8, 6, 8, 8)
            params.width = lastExpandedWidth
            params.height = lastExpandedHeight
        }
        windowManager.updateViewLayout(this@FloatView, params)
    }

    private fun createPanel(title: String, containerLayout: LinearLayout): View {
        val box = LinearLayout(context)
        box.orientation = LinearLayout.VERTICAL

        val titleView = TextView(context).apply {
            text = title
            textSize = 14f
            setTextColor(Color.GREEN)
        }
        box.addView(titleView)

        val scroll = ScrollView(context)
        scroll.addView(containerLayout)
        box.addView(scroll, LinearLayout.LayoutParams(300, LinearLayout.LayoutParams.MATCH_PARENT))
        return box
    }

    /**
     * 【终极优化】全动态高保真文本流刷新函数。
     * 直接动态遍历原始 JSON 字符串的所有 Key，如果 TextView 存在则局部更新文本内容，
     * 不存在则动态创建并塞进缓存 Map 中。彻底解决字段缺失问题，同时规避 removeAllViews 导致的重绘卡顿。
     */
    fun updateJsonDynamic(rawJson: String) {
        try {
            val obj = JSONObject(rawJson)
            
            // 提取用于折线图绘制的核心信号参数（根据命名特征智能映射）
            var airR1: Float? = null
            var airR2: Float? = null
            var airSnr: Float? = null
            var gndR1: Float? = null
            var gndR2: Float? = null
            var gndSnr: Float? = null

            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val valueStr = obj.optString(key, "")

                // 根据原始规则自动识别天空端还是地面端数据分配面板
                if (key.endsWith("_a") || key.startsWith("air_")) {
                    updateOrAddText(airLayout, airTextViewMap, key, valueStr)
                    // 提取曲线数据
                    if (key.contains("rssi1")) airR1 = valueStr.toFloatOrNull()?.let { Math.abs(it) }
                    if (key.contains("rssi2")) airR2 = valueStr.toFloatOrNull()?.let { Math.abs(it) }
                    if (key.contains("snr")) airSnr = valueStr.toFloatOrNull()?.let { Math.abs(it) }
                } else if (key.endsWith("_g") || key.startsWith("gnd_")) {
                    updateOrAddText(gndLayout, gndTextViewMap, key, valueStr)
                    // 提取曲线数据
                    if (key.contains("rssi1")) gndR1 = valueStr.toFloatOrNull()?.let { Math.abs(it) }
                    if (key.contains("rssi2")) gndR2 = valueStr.toFloatOrNull()?.let { Math.abs(it) }
                    if (key.contains("snr")) gndSnr = valueStr.toFloatOrNull()?.let { Math.abs(it) }
                } else {
                    // 没有明确阵营标签的公共属性，默认丢进左侧 AIR 面板下方动态追加显示
                    updateOrAddText(airLayout, airTextViewMap, key, valueStr)
                }
            }

            // 更新波形图
            if (airR1 != null || airR2 != null || airSnr != null) {
                airChartView.addData(airR1, airR2, airSnr)
            }
            if (gndR1 != null || gndR2 != null || gndSnr != null) {
                gndChartView.addData(gndR1, gndR2, gndSnr)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateOrAddText(layout: LinearLayout, map: HashMap<String, TextView>, key: String, value: String) {
        val cachedTv = map[key]
        if (cachedTv != null) {
            cachedTv.text = "$key : $value"
        } else {
            val tv = TextView(context).apply {
                text = "$key : $value"
                textSize = 12f
                setTextColor(Color.WHITE)
                setPadding(4, 3, 4, 3)
            }
            layout.addView(tv)
            map[key] = tv
        }
    }

    private class WaveformView(context: Context, private val isAir: Boolean) : View(context) {
        private val maxDataPoints = 100
        private val yAxisWidth = 85f 

        private val rssi1List = LinkedList<Float>()
        private val rssi2List = LinkedList<Float>()
        private val snrList = LinkedList<Float>()

        private val axisTextPaint = Paint().apply {
            color = Color.parseColor("#BDC3C7")
            textSize = 16f
            isAntiAlias = true
        }

        private val prefixTextPaint = Paint().apply {
            color = Color.LTGRAY
            textSize = 18f
            isAntiAlias = true
        }

        private val colorRssi1 = Color.parseColor("#2980B9")
        private val colorRssi2 = Color.parseColor("#3498DB")
        private val colorSnr   = Color.parseColor("#2ECC71")

        private val paintRssi1 = Paint().apply {
            color = colorRssi1
            strokeWidth = 4f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        private val paintRssi2 = Paint().apply {
            color = colorRssi2
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        private val paintSnr = Paint().apply {
            color = colorSnr
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        private val paintTextRssi1 = Paint().apply {
            color = colorRssi1
            textSize = 18f
            isAntiAlias = true
        }

        private val paintTextRssi2 = Paint().apply {
            color = colorRssi2
            textSize = 18f
            isAntiAlias = true
        }

        private val paintTextSnr = Paint().apply {
            color = colorSnr
            textSize = 18f
            isAntiAlias = true
        }

        private val gridPaint = Paint().apply {
            color = Color.argb(45, 255, 255, 255)
            strokeWidth = 1f
        }

        private val bgPaint = Paint().apply {
            color = Color.argb(30, 255, 255, 255)
        }

        private val rssiMin = 0f
        private val rssiMax = 120f
        private val snrMin = 0f
        private val snrMax = 50f

        fun addData(r1: Float?, r2: Float?, snr: Float?) {
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

            canvas.drawRect(chartLeft, 0f, chartRight, h, bgPaint)

            val yPositions = floatArrayOf(h * 0.15f, h * 0.5f, h * 0.85f)
            val rssiLabels = arrayOf("120", "60", "0")
            val snrLabels = arrayOf("50", "25", "0")

            for (i in yPositions.indices) {
                val y = yPositions[i]
                canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
                val labelText = "${rssiLabels[i]}(${snrLabels[i]})"
                canvas.drawText(labelText, 5f, y + 6f, axisTextPaint)
            }

            val prefix = if (isAir) "[AIR] " else "[GND] "
            canvas.drawText(prefix, chartLeft + 15f, 25f, prefixTextPaint)
            
            val prefixWidth = prefixTextPaint.measureText(prefix)
            val startX = chartLeft + 15f + prefixWidth

            val r1Text = "R1: ${rssi1List.lastOrNull()?.toInt()}   "
            canvas.drawText(r1Text, startX, 25f, paintTextRssi1)
            
            val r1Width = paintTextRssi1.measureText(r1Text)
            val r2Text = "R2: ${rssi2List.lastOrNull()?.toInt()}   "
            canvas.drawText(r2Text, startX + r1Width, 25f, paintTextRssi2)
            
            val r2Width = paintTextRssi2.measureText(r2Text)
            val snrText = "SNR: ${snrList.lastOrNull()?.toInt()}"
            canvas.drawText(snrText, startX + r1Width + r2Width, 25f, paintTextSnr)

            drawNormalCurve(canvas, rssi1List, rssiMin, rssiMax, chartLeft, chartWidth, h, paintRssi1)
            drawNormalCurve(canvas, rssi2List, rssiMin, rssiMax, chartLeft, chartWidth, h, paintRssi2)
            drawNormalCurve(canvas, snrList, minVal = snrMin, maxVal = snrMax, leftOffset = chartLeft, cWidth = chartWidth, h = h, paint = paintSnr)
        }

        private fun drawNormalCurve(canvas: Canvas, list: List<Float>, minVal: Float, maxVal: Float, leftOffset: Float, cWidth: Float, h: Float, paint: Paint) {
            if (list.size < 2) return
            val stepX = cWidth / (maxDataPoints - 1)
            val range = maxVal - minVal

            for (i in 0 until list.size - 1) {
                val startX = leftOffset + (i * stepX)
                val endX = leftOffset + ((i + 1) * stepX)
                val valStart = list[i].coerceIn(minVal, maxVal)
                val valEnd = list[i + 1].coerceIn(minVal, maxVal)
                val startY = h * (1f - (valStart - minVal) / range)
                val endY = h * (1f - (valEnd - minVal) / range)
                canvas.drawLine(startX, startY, endX, endY, paint)
            }
        }
    }
}
