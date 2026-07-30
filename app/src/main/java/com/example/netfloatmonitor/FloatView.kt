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
    private val collapsedSize = 160

    private var startWidth = 0
    private var startHeight = 0
    private var downX = 0f
    private var downY = 0f
    private var resize = false

    private val topBar = LinearLayout(context)
    private val contentFrame = FrameLayout(context)
    private val contentPanel = LinearLayout(context)
    
    // 缓存 TextView 节点，实现全动态字段的高性能局部更新
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
        this.setPadding(12, 8, 12, 12)

        val bg = GradientDrawable()
        bg.setColor(Color.argb(205, 15, 15, 15)) // 暗色半透明背景，保障强光下的文字可见度
        bg.cornerRadius = 14f
        this.background = bg

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
        
        val airChartLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply {
            setMargins(0, 0, 0, 10)
        }
        val gndChartLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        
        chartContainer.addView(airChartView, airChartLp)
        chartContainer.addView(gndChartView, gndChartLp)
        
        val chartContainerLp = LinearLayout.LayoutParams(720, LinearLayout.LayoutParams.MATCH_PARENT).apply {
            setMargins(16, 0, 4, 0)
        }
        contentPanel.addView(chartContainer, chartContainerLp)
        
        contentFrame.addView(contentPanel, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        
        val indicatorLp = FrameLayout.LayoutParams(18, 18).apply {
            gravity = Gravity.BOTTOM or Gravity.RIGHT
            setMargins(0, 0, 2, 2)
        }
        contentFrame.addView(resizeIndicator, indicatorLp)
        
        val frameLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        addView(contentFrame, frameLp)

        // 折叠微型状态（悬浮球）的移动与吸附
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
                        if (Math.abs(dx) > 12 || Math.abs(dy) > 12) {
                            isDragging = true
                        }
                        if (isDragging) {
                            params.x += (event.rawX - downX).toInt()
                            params.y += (event.rawY - downY).toInt()
                            downX = event.rawX
                            downY = event.rawY
                            
                            val maxAllowableY = getScreenHeight() - getNavigationBarHeight() - height
                            if (params.y > maxAllowableY) params.y = maxAllowableY
                            if (params.y < 0) params.y = 0
                            
                            windowManager.updateViewLayout(this@FloatView, params)
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isDragging) {
                            animateToEdge()
                        } else {
                            performToggle()
                        }
                    }
                }
                return true
            }
        })

        toggleBtn.setOnClickListener {
            if (isExpanded) performToggle()
        }

        // 展开状态的移动与缩放
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
                            val newWidth = (startWidth + event.rawX - downX).toInt().coerceAtLeast(600)
                            var newHeight = (startHeight + event.rawY - downY).toInt().coerceAtLeast(260)
                            
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
                            if (targetY < 0) targetY = 0
                            
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

    private fun getScreenWidth(): Int = context.resources.displayMetrics.widthPixels
    private fun getScreenHeight(): Int = context.resources.displayMetrics.heightPixels

    private fun animateToEdge() {
        val screenWidth = getScreenWidth()
        val targetX = if (params.x + collapsedSize / 2 < screenWidth / 2) 0 else screenWidth - collapsedSize
        
        val animator = ValueAnimator.ofInt(params.x, targetX).apply {
            duration = 350
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                params.x = animation.animatedValue as Int
                try {
                    windowManager.updateViewLayout(this@FloatView, params)
                } catch (e: Exception) {
                    // 防御组件异步解绑时重绘造成的崩溃
                }
            }
        }
        animator.start()
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
            toggleBtn.textSize = 13f
            toggleBtn.background = GradientDrawable().apply {
                setColor(Color.parseColor("#1ABC9C"))
                cornerRadius = 80f
            }
            
            panelBg.setColor(Color.TRANSPARENT)
            this.background = panelBg
            this.setPadding(0, 0, 0, 0)
            params.width = collapsedSize
            params.height = collapsedSize
            
            animateToEdge()
        } else {
            isExpanded = true
            contentFrame.visibility = View.VISIBLE
            resizeIndicator.visibility = View.VISIBLE
            
            toggleBtn.layoutParams = LinearLayout.LayoutParams(48, 48)
            toggleBtn.text = "×"
            toggleBtn.textSize = 14f
            toggleBtn.background = GradientDrawable().apply {
                setColor(Color.parseColor("#C0392B"))
                cornerRadius = 6f
            }
            
            panelBg.setColor(Color.argb(205, 15, 15, 15))
            panelBg.cornerRadius = 14f
            this.background = panelBg
            this.setPadding(12, 8, 12, 12)
            params.width = lastExpandedWidth
            params.height = lastExpandedHeight
            windowManager.updateViewLayout(this@FloatView, params)
        }
    }

    private fun createPanel(title: String, containerLayout: LinearLayout): View {
        val box = LinearLayout(context)
        box.orientation = LinearLayout.VERTICAL

        val titleView = TextView(context).apply {
            text = title
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD //  正确加粗方式
            setTextColor(Color.parseColor("#E67E22"))
            setPadding(4, 2, 4, 6)
        }
        box.addView(titleView)

        val scroll = ScrollView(context)
        scroll.setVerticalScrollBarEnabled(false)
        scroll.addView(containerLayout)
        box.addView(scroll, LinearLayout.LayoutParams(310, LinearLayout.LayoutParams.MATCH_PARENT))
        return box
    }

    /**
     * 全动态高保真字段解析分发（动态遍历，无缺失更新）
     */
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
                    // 纯正值提取，不处理负号，保留原汁原味
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

            // 数据推进波形图绘制区
            if (airR1 != null || airR2 != null || airSnr != null) airChartView.addData(airR1, airR2, airSnr)
            if (gndR1 != null || gndR2 != null || gndSnr != null) gndChartView.addData(gndR1, gndR2, gndSnr)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 【正值反向映射】状态机着色逻辑：数值越小（越接近底部），代表信号质量越好（越绿）
     */
    private fun updateOrAddTextWithColor(layout: LinearLayout, map: HashMap<String, TextView>, key: String, value: String) {
        val cachedTv = map[key]
        
        val displayColor = when {
            key.contains("rssi", ignoreCase = true) -> {
                val rssiVal = value.toFloatOrNull() ?: 0f
                when {
                    rssiVal == 0f -> Color.parseColor("#E74C3C")       // 0 代表异常断连或未吐数（红色）
                    rssiVal < 60f -> Color.parseColor("#2ECC71")       // 数值小代表信号极强（亮绿）
                    rssiVal < 75f -> Color.parseColor("#F1C40F")       // 中等衰减（黄色）
                    rssiVal < 90f -> Color.parseColor("#E67E22")       // 衰减偏大警告（橙色）
                    else -> Color.parseColor("#E74C3C")                // 数值过大，信号奄奄一息（暗红）
                }
            }
            key.contains("snr", ignoreCase = true) -> {
                // SNR（信噪比）天生是正数且越大越好，维持其标准映射
                val snrVal = value.toFloatOrNull() ?: 0f
                when {
                    snrVal < 8f -> Color.parseColor("#E74C3C")   // 噪声过大（红）
                    snrVal < 18f -> Color.parseColor("#F1C40F")  // 噪声中等（黄）
                    else -> Color.parseColor("#2ECC71")          // 优良（绿）
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
     * 正值直觉型波形绘制容器
     */
    private class WaveformView(context: Context, private val isAir: Boolean) : View(context) {
        private val maxDataPoints = 100
        private val yAxisWidth = 85f 

        private val rssi1List = LinkedList<Float>()
        private val rssi2List = LinkedList<Float>()
        private val snrList = LinkedList<Float>()

        private val axisTextPaint = Paint().apply {
            color = Color.parseColor("#95A5A6")
            textSize = 15f
            isAntiAlias = true
        }

        private val prefixTextPaint = Paint().apply {
            color = Color.parseColor("#ECF0F1")
            textSize = 17f
            isFakeBoldText = true //  正确写法：使用 Paint 的伪粗体属性
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
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        private val paintSnr = Paint().apply {
            color = colorSnr
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        private val paintTextRssi1 = Paint().apply { color = colorRssi1; textSize = 17f; isAntiAlias = true }
        private val paintTextRssi2 = Paint().apply { color = colorRssi2; textSize = 17f; isAntiAlias = true }
        private val paintTextSnr   = Paint().apply { color = colorSnr; textSize = 17f; isAntiAlias = true }

        private val gridPaint = Paint().apply {
            color = Color.argb(35, 255, 255, 255)
            strokeWidth = 1f
        }

        private val bgPaint = Paint().apply {
            color = Color.argb(20, 255, 255, 255)
        }

        // 正值绘图空间区间配置：0 代表极优（坐标最底部），120 代表极差（坐标最顶部）
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
            // 刻度显示：最上方是 120，最下方是 0
            val rssiLabels = arrayOf("120", "60", "0")
            val snrLabels = arrayOf("50", "25", "0")

            for (i in yPositions.indices) {
                val y = yPositions[i]
                canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
                val labelText = "${rssiLabels[i]}(${snrLabels[i]})"
                canvas.drawText(labelText, 5f, y + 5f, axisTextPaint)
            }

            val prefix = if (isAir) "[AIR] " else "[GND] "
            canvas.drawText(prefix, chartLeft + 15f, 26f, prefixTextPaint)
            
            val prefixWidth = prefixTextPaint.measureText(prefix)
            val startX = chartLeft + 15f + prefixWidth

            val r1Text = "R1: ${rssi1List.lastOrNull()?.toInt() ?: 0}  "
            canvas.drawText(r1Text, startX, 26f, paintTextRssi1)
            
            val r1Width = paintTextRssi1.measureText(r1Text)
            val r2Text = "R2: ${rssi2List.lastOrNull()?.toInt() ?: 0}  "
            canvas.drawText(r2Text, startX + r1Width, 26f, paintTextRssi2)
            
            val r2Width = paintTextRssi2.measureText(r2Text)
            val snrText = "SNR: ${snrList.lastOrNull()?.toInt() ?: 0}"
            canvas.drawText(snrText, startX + r1Width + r2Width, 26f, paintTextSnr)

            // 依据“数值小在图表底部，数值大冲高”的特征曲线渲染
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
                // 1f - (...) 的经典映射：数值越大，Y 坐标越小（越靠顶部）；数值越小，Y 坐标越大（越靠底部）
                val startY = h * (1f - (valStart - minVal) / range)
                val endY = h * (1f - (valEnd - minVal) / range)
                canvas.drawLine(startX, startY, endX, endY, paint)
            }
        }
    }
}
