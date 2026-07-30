package com.example.netfloatmonitor

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
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
    private var lastExpandedWidth = 1350
    private var lastExpandedHeight = 520
    
    // 收纳看板物理尺寸（适配信号栏+三参数文本）
    private val collapsedWidth = 220
    private val collapsedHeight = 130
    // 隐藏半角后，留在屏幕内的可点区域宽度
    private val visibleEdgeWidth = 85 

    private var startWidth = 0
    private var startHeight = 0
    private var downX = 0f
    private var downY = 0f
    private var resize = false

    private val topBar = LinearLayout(context)
    private val contentFrame = FrameLayout(context)
    private val contentPanel = LinearLayout(context)
    
    // 收纳态：左右并排的双路信号栏看板
    private val collapsedPanel = LinearLayout(context)
    private val airSignalIconView = SignalIconView(context, "AIR")
    private val gndSignalIconView = SignalIconView(context, "GND")
    
    private val airTextViewMap = HashMap<String, TextView>()
    private val gndTextViewMap = HashMap<String, TextView>()

    private val resizeIndicator = View(context).apply {
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#3498DB"))
            cornerRadius = 4f
        }
        visibility = View.VISIBLE
    }

    private val toggleBtn = Button(context).apply {
        text = "×"
        textSize = 14f
        setTextColor(Color.WHITE)
        setGravity(Gravity.CENTER)
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#C0392B"))
            cornerRadius = 6f
        }
    }

    init {
        this.orientation = LinearLayout.VERTICAL
        this.setPadding(12, 8, 12, 12)

        val bg = GradientDrawable()
        bg.setColor(Color.argb(205, 15, 15, 15))
        bg.cornerRadius = 14f
        this.background = bg

        // 组装收纳态小看板布局
        collapsedPanel.orientation = LinearLayout.HORIZONTAL
        collapsedPanel.gravity = Gravity.CENTER
        collapsedPanel.visibility = View.GONE
        
        val iconLp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        collapsedPanel.addView(airSignalIconView, iconLp)
        collapsedPanel.addView(gndSignalIconView, iconLp)
        addView(collapsedPanel)

        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        topBar.setPadding(0, 0, 4, 6)
        
        val btnLp = LinearLayout.LayoutParams(48, 48)
        topBar.addView(toggleBtn, btnLp)
        addView(topBar)

        contentPanel.orientation = LinearLayout.HORIZONTAL
        airLayout.orientation = LinearLayout.VERTICAL
        gndLayout.orientation = LinearLayout.VERTICAL
        
        contentPanel.addView(createPanel("AIR TELEMETRY", airLayout))
        contentPanel.addView(createPanel("GND TELEMETRY", gndLayout))
        
        chartContainer.orientation = LinearLayout.VERTICAL
        val airChartLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { setMargins(0, 0, 0, 10) }
        val gndChartLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        chartContainer.addView(airChartView, airChartLp)
        chartContainer.addView(gndChartView, gndChartLp)
        
        val chartContainerLp = LinearLayout.LayoutParams(720, LinearLayout.LayoutParams.MATCH_PARENT).apply { setMargins(16, 0, 4, 0) }
        contentPanel.addView(chartContainer, chartContainerLp)
        
        contentFrame.addView(contentPanel, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        contentFrame.addView(resizeIndicator, FrameLayout.LayoutParams(18, 18).apply { gravity = Gravity.BOTTOM or Gravity.RIGHT; setMargins(0, 0, 2, 2) })
        addView(contentFrame, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))

        toggleBtn.setOnClickListener {
            if (isExpanded) performToggle()
        }

        // 恢复原始最纯粹的拖动实现：全屏自由拖动，不做物理死边界卡死
        setOnTouchListener(object : OnTouchListener {
            private var isDragging = false

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        startWidth = width
                        startHeight = height
                        // 处于展开态时，右下角120px区域支持拉伸大小
                        resize = isExpanded && (event.x > (width - 120)) && (event.y > (height - 120))
                        isDragging = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - downX
                        val dy = event.rawY - downY
                        
                        if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                            isDragging = true
                        }

                        if (isExpanded && resize) {
                            // 展开态拉伸逻辑
                            val newWidth = (startWidth + dx).toInt().coerceAtLeast(600)
                            val newHeight = (startHeight + dy).toInt().coerceAtLeast(260)
                            params.width = newWidth
                            params.height = newHeight
                            lastExpandedWidth = newWidth
                            lastExpandedHeight = newHeight
                        } else {
                            // 【彻底恢复原始手感】：直接根据手指偏移增量更新坐标，绝不强制卡死边界
                            params.x += dx.toInt()
                            params.y += dy.toInt()
                            
                            downX = event.rawX
                            downY = event.rawY
                        }
                        windowManager.updateViewLayout(this@FloatView, params)
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isExpanded && !isDragging) {
                            // 收纳态下点击：直接恢复展开
                            performToggle()
                        } else if (!isExpanded && isDragging) {
                            // 收纳态拖动结束：顺滑停留在最后释放的地方，支持靠边自动隐藏半边
                            animateToEdgeAndHideHalf()
                        }
                    }
                }
                return true
            }
        })
    }

    private fun getScreenWidth(): Int = context.resources.displayMetrics.widthPixels

    // 收纳时顺滑贴近最靠近的那一侧边缘
    private fun animateToEdgeAndHideHalf() {
        val screenWidth = getScreenWidth()
        
        // 判定离哪边近就吸附哪边
        val targetX = if (params.x + collapsedWidth / 2 < screenWidth / 2) {
            -(collapsedWidth - visibleEdgeWidth)
        } else {
            screenWidth - visibleEdgeWidth
        }

        ValueAnimator.ofInt(params.x, targetX).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                params.x = animation.animatedValue as Int
                if (isAttachedToWindow) {
                    windowManager.updateViewLayout(this@FloatView, params)
                }
            }
            start()
        }
    }

    private fun performToggle() {
        val panelBg = GradientDrawable()
        if (isExpanded) {
            isExpanded = false
            topBar.visibility = View.GONE
            contentFrame.visibility = View.GONE
            collapsedPanel.visibility = View.VISIBLE
            
            panelBg.setColor(Color.argb(220, 20, 20, 20))
            panelBg.cornerRadius = 16f
            this.background = panelBg
            this.setPadding(6, 8, 6, 6)
            
            params.width = collapsedWidth
            params.height = collapsedHeight
            windowManager.updateViewLayout(this@FloatView, params)
            animateToEdgeAndHideHalf()
        } else {
            isExpanded = true
            collapsedPanel.visibility = View.GONE
            topBar.visibility = View.VISIBLE
            contentFrame.visibility = View.VISIBLE
            
            panelBg.setColor(Color.argb(205, 15, 15, 15))
            panelBg.cornerRadius = 14f
            this.background = panelBg
            this.setPadding(12, 8, 12, 12)
            
            params.width = lastExpandedWidth
            params.height = lastExpandedHeight
            
            // 展开时稍微保证不要完全消失在视线外
            val screenWidth = getScreenWidth()
            if (params.x < 0) params.x = 10
            if (params.x + lastExpandedWidth > screenWidth) {
                params.x = screenWidth - lastExpandedWidth - 10
            }
            windowManager.updateViewLayout(this@FloatView, params)
        }
    }

    private fun createPanel(title: String, containerLayout: LinearLayout): View {
        val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val titleView = TextView(context).apply {
            text = title
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#E67E22"))
            setPadding(4, 2, 4, 6)
        }
        box.addView(titleView)
        val scroll = ScrollView(context).apply { setVerticalScrollBarEnabled(false) }
        scroll.addView(containerLayout)
        box.addView(scroll, LinearLayout.LayoutParams(310, LinearLayout.LayoutParams.MATCH_PARENT))
        return box
    }

    fun updateJsonDynamic(rawJson: String) {
        try {
            val obj = JSONObject(rawJson)
            
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

                if (key.endsWith("_a") || key.startsWith("air_")) {
                    updateOrAddTextWithColor(airLayout, airTextViewMap, key, valueStr)
                    if (key.contains("rssi1")) airR1 = valueStr.toFloatOrNull()
                    if (key.contains("rssi2")) airR2 = valueStr.toFloatOrNull()
                    if (key.contains("snr")) airSnr = valueStr.toFloatOrNull()
                } else if (key.endsWith("_g") || key.startsWith("gnd_")) {
                    updateOrAddTextWithColor(gndLayout, gndTextViewMap, key, valueStr)
                    if (key.contains("rssi1")) gndR1 = valueStr.toFloatOrNull()
                    if (key.contains("rssi2")) gndR2 = valueStr.toFloatOrNull()
                    if (key.contains("snr")) gndSnr = valueStr.toFloatOrNull()
                } else {
                    updateOrAddTextWithColor(airLayout, airTextViewMap, key, valueStr)
                }
            }

            airSignalIconView.setSignalData(airR1 ?: 0f, airR2 ?: 0f, airSnr ?: 0f)
            gndSignalIconView.setSignalData(gndR1 ?: 0f, gndR2 ?: 0f, gndSnr ?: 0f)

            if (airR1 != null || airR2 != null || airSnr != null) airChartView.addData(airR1, airR2, airSnr)
            if (gndR1 != null || gndR2 != null || gndSnr != null) gndChartView.addData(gndR1, gndR2, gndSnr)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateOrAddTextWithColor(layout: LinearLayout, map: HashMap<String, TextView>, key: String, value: String) {
        val cachedTv = map[key]
        val displayColor = when {
            key.contains("rssi", ignoreCase = true) -> {
                val rssiVal = value.toFloatOrNull() ?: 0f
                when {
                    rssiVal == 0f -> Color.parseColor("#E74C3C")
                    rssiVal < 60f -> Color.parseColor("#2ECC71")
                    rssiVal < 75f -> Color.parseColor("#F1C40F")
                    rssiVal < 90f -> Color.parseColor("#E67E22")
                    else -> Color.parseColor("#E74C3C")
                }
            }
            key.contains("snr", ignoreCase = true) -> {
                val snrVal = value.toFloatOrNull() ?: 0f
                when {
                    snrVal < 8f -> Color.parseColor("#E74C3C")
                    snrVal < 18f -> Color.parseColor("#F1C40F")
                    else -> Color.parseColor("#2ECC71")
                }
            }
            key.contains("failed", ignoreCase = true) -> {
                val failedCount = value.toIntOrNull() ?: 0
                if (failedCount > 0) Color.parseColor("#E74C3C") else Color.WHITE
            }
            key.contains("pass", ignoreCase = true) -> Color.parseColor("#3498DB")
            else -> Color.WHITE
        }

        val displayText = "$key : $value"
        if (cachedTv != null) {
            cachedTv.text = displayText
            cachedTv.setTextColor(displayColor)
        } else {
            val tv = TextView(context).apply {
                text = displayText
                textSize = 12f
                setTextColor(displayColor)
                setPadding(6, 4, 6, 4)
            }
            layout.addView(tv)
            map[key] = tv
        }
    }

    /**
     * 已调优：精简小巧版信号格图标 + 底部特意放大加粗的详细参数文本
     */
    private class SignalIconView(context: Context, private val label: String) : View(context) {
        private var r1 = 0f
        private var r2 = 0f
        private var snr = 0f

        private val paint = Paint().apply { isAntiAlias = true }
        private val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 14f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        private val subTextPaint = Paint().apply {
            color = Color.parseColor("#BDC3C7")
            textSize = 15f         // 底部字大一点
            isFakeBoldText = true  // 文本加粗
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        fun setSignalData(rssi1: Float, rssi2: Float, snrVal: Float) {
            this.r1 = rssi1
            this.r2 = rssi2
            this.snr = snrVal
            postInvalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0 || h <= 0) return

            // 1. 顶部标头
            textPaint.color = if (label == "AIR") Color.parseColor("#E67E22") else Color.parseColor("#3498DB")
            textPaint.isFakeBoldText = true
            canvas.drawText(label, w / 2f, 20f, textPaint)

            // 2. 状态判定
            val primaryRssi = if (r1 > 0 && r2 > 0) Math.min(r1, r2) else Math.max(r1, r2)
            val (bars, barColor) = when {
                primaryRssi == 0f -> 1 to Color.parseColor("#E74C3C")
                primaryRssi < 60f -> 4 to Color.parseColor("#2ECC71")
                primaryRssi < 75f -> 3 to Color.parseColor("#F1C40F")
                primaryRssi < 90f -> 2 to Color.parseColor("#E67E22")
                else -> 1 to Color.parseColor("#E74C3C")
            }

            // 3. 绘制图标调小后的手机阶梯格
            val barCount = 4
            val barSpacing = 4f
            val totalSpacing = barSpacing * (barCount - 1)
            val barWidth = 6f
            val startX = (w - (barWidth * barCount + totalSpacing)) / 2f
            val baseLineY = h - 45f

            for (i in 0 until barCount) {
                val x = startX + i * (barWidth + barSpacing)
                val barHeight = 8f + i * 5f
                val top = baseLineY - barHeight
                
                if (i < bars) {
                    paint.color = barColor
                    paint.style = Paint.Style.FILL
                } else {
                    paint.color = Color.argb(55, 255, 255, 255)
                    paint.style = Paint.Style.FILL
                }
                canvas.drawRect(x, top, x + barWidth, baseLineY, paint)
            }

            // 4. 绘制放大加粗的底部字体
            val infoStr = "${r1.toInt()}/${r2.toInt()}/${snr.toInt()}"
            val finalInfo = if (primaryRssi == 0f) "DISCONN" else infoStr
            subTextPaint.color = barColor
            canvas.drawText(finalInfo, w / 2f, h - 15f, subTextPaint)
        }
    }

    /**
     * 历史波形图（展开态）
     */
    private class WaveformView(context: Context, private val isAir: Boolean) : View(context) {
        private val maxDataPoints = 100
        private val yAxisWidth = 85f 

        private val rssi1List = LinkedList<Float>()
        private val rssi2List = LinkedList<Float>()
        private val snrList = LinkedList<Float>()

        private val axisTextPaint = Paint().apply { color = Color.parseColor("#95A5A6"); textSize = 15f; isAntiAlias = true }
        private val prefixTextPaint = Paint().apply { 
            color = Color.parseColor("#ECF0F1")
            textSize = 17f
            isFakeBoldText = true
            isAntiAlias = true 
        }

        private val colorRssi1 = Color.parseColor("#2980B9")
        private val colorRssi2 = Color.parseColor("#3498DB")
        private val colorSnr   = Color.parseColor("#2ECC71")

        private val paintRssi1 = Paint().apply { color = colorRssi1; strokeWidth = 4f; style = Paint.Style.STROKE; isAntiAlias = true }
        private val paintRssi2 = Paint().apply { color = colorRssi2; strokeWidth = 2.5f; style = Paint.Style.STROKE; isAntiAlias = true }
        private val paintSnr   = Paint().apply { color = colorSnr; strokeWidth = 3f; style = Paint.Style.STROKE; isAntiAlias = true }

        private val paintTextRssi1 = Paint().apply { color = colorRssi1; textSize = 17f; isAntiAlias = true }
        private val paintTextRssi2 = Paint().apply { color = colorRssi2; textSize = 17f; isAntiAlias = true }
        private val paintTextSnr   = Paint().apply { color = colorSnr; textSize = 17f; isAntiAlias = true }

        private val gridPaint = Paint().apply { color = Color.argb(35, 255, 255, 255); strokeWidth = 1f }
        private val bgPaint = Paint().apply { color = Color.argb(20, 255, 255, 255) }

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
                canvas.drawText("${rssiLabels[i]}(${snrLabels[i]})", 5f, y + 5f, axisTextPaint)
            }

            val prefix = if (isAir) "[AIR] " else "[GND] "
            canvas.drawText(prefix, chartLeft + 15f, 26f, prefixTextPaint)
            val startX = chartLeft + 15f + prefixTextPaint.measureText(prefix)

            val r1Text = "R1: ${rssi1List.lastOrNull()?.toInt() ?: 0}  "
            canvas.drawText(r1Text, startX, 26f, paintTextRssi1)
            val r2Text = "R2: ${rssi2List.lastOrNull()?.toInt() ?: 0}  "
            canvas.drawText(r2Text, startX + paintTextRssi1.measureText(r1Text), 26f, paintTextRssi2)
            val snrText = "SNR: ${snrList.lastOrNull()?.toInt() ?: 0}"
            canvas.drawText(snrText, startX + paintTextRssi1.measureText(r1Text) + paintTextRssi2.measureText(r2Text), 26f, paintTextSnr)

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
                canvas.drawLine(startX, h * (1f - (valStart - minVal) / range), endX, h * (1f - (valEnd - minVal) / range), paint)
            }
        }
    }
}
