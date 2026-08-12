package com.example.netfloatmonitor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList

class FloatView(
    context: Context,
    private val windowManager: WindowManager,
    private val params: WindowManager.LayoutParams
) : LinearLayout(context) {

    // ==========================================
    // 尺寸常量定义 (天空/地面双竖列布局宽度 220)
    // ==========================================
    private val TEXT_COL_WIDTH = 220      // 单个文本数据列宽度
    private val CHART_COL_WIDTH = 480     // 单个图表曲线列宽度
    
    private val collapsedWidth = 220
    private val collapsedHeight = 130
    private var lastExpandedHeight = 650 

    // 核心状态控制：独立区分两张图表的收纳与展开
    private var isExpanded = true             // 悬浮窗整体展开/收起
    private var isWaveformExpanded = true     // 实时波形曲线列展开/收起
    private var isNoiseExpanded = true        // 底噪频谱曲线列展开/收起

    private var startWidth = 0
    private var startHeight = 0
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var resize = false

    // =========================================================================
    // 状态追踪变量：用于 support fail 字段变红、5秒恢复原色
    // =========================================================================
    private val lastValues = HashMap<String, String>()
    private val redTimerRunnables = HashMap<String, Runnable>()
    private val mainHandler = Handler(Looper.getMainLooper())

    // 统一底噪曲线调色板
    private val noiseCurveColors = intArrayOf(
        Color.parseColor("#E74C3C"), // ch1: 红
        Color.parseColor("#F1C40F"), // ch2: 黄
        Color.parseColor("#3498DB"), // ch3: 蓝
        Color.parseColor("#9B59B6"), // ch4: 紫
        Color.parseColor("#1ABC9C"), // ch5: 青
        Color.parseColor("#E67E22")  // ch6: 橙
    )

    // UI 容器组件
    private val topBar = LinearLayout(context)
    private val contentFrame = FrameLayout(context)
    private val contentPanel = LinearLayout(context) // 主内容水平平铺容器
    
    // 独立解耦的图表列容器
    private val waveformCol = LinearLayout(context)
    private val noiseCol = LinearLayout(context)

    // 数据列表容器 (天空/地面 2 竖列)
    private val airLayout = LinearLayout(context)
    private val gndLayout = LinearLayout(context)
    
    // 自定义 View 实例
    private val airChartView = WaveformView(context, isAir = true)
    private val gndChartView = WaveformView(context, isAir = false)
    private val airNoiseChartView = NoiseFloorChartView(context, isAir = true, noiseCurveColors)
    private val gndNoiseChartView = NoiseFloorChartView(context, isAir = false, noiseCurveColors)

    // 迷你折叠面板组件
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
    }

    // 顶部全局最小化至图标按钮
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

    // 单独控制【实时波形图】独立开关
    private val waveformToggleBtn = Button(context).apply {
        text = "Link Curve"
        textSize = 11f
        setTextColor(Color.WHITE)
        setGravity(Gravity.CENTER)
        setPadding(10, 0, 10, 0)
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#2980B9"))
            cornerRadius = 6f
        }
    }

    // 单独控制【底噪频谱图】独立开关
    private val noiseToggleBtn = Button(context).apply {
        text = "Noise Floor"
        textSize = 11f
        setTextColor(Color.WHITE)
        setGravity(Gravity.CENTER)
        setPadding(10, 0, 10, 0)
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#27AE60"))
            cornerRadius = 6f
        }
    }

    init {
        this.orientation = LinearLayout.VERTICAL
        this.setPadding(12, 8, 12, 12)

        val bg = GradientDrawable().apply {
            setColor(Color.argb(205, 15, 15, 15))
            cornerRadius = 14f
        }
        this.background = bg

        // 1. 初始化右下角迷你模式状态面板
        collapsedPanel.orientation = LinearLayout.HORIZONTAL
        collapsedPanel.gravity = Gravity.CENTER
        collapsedPanel.visibility = View.GONE
        val iconLp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        collapsedPanel.addView(airSignalIconView, iconLp)
        collapsedPanel.addView(gndSignalIconView, iconLp)
        addView(collapsedPanel)

        // 2. 初始化顶部控制状态栏
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        topBar.setPadding(0, 0, 4, 6)
        
        val btnLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 48).apply { rightMargin = 12 }
        topBar.addView(waveformToggleBtn, btnLp)
        topBar.addView(noiseToggleBtn, btnLp)
        topBar.addView(toggleBtn, LinearLayout.LayoutParams(48, 48))
        addView(topBar)

        // 3. 构建主内容区 (横向平铺架构)
        contentPanel.orientation = LinearLayout.HORIZONTAL
        airLayout.orientation = LinearLayout.VERTICAL
        gndLayout.orientation = LinearLayout.VERTICAL
        
        // 第 1 列：空中数传文本面板 (左侧)
        contentPanel.addView(createPanel("AIR", airLayout), LinearLayout.LayoutParams(TEXT_COL_WIDTH, LinearLayout.LayoutParams.MATCH_PARENT))
        
        // 第 2 列：地面数传文本面板 (右侧，2 竖列分隔)
        val gndTextLp = LinearLayout.LayoutParams(TEXT_COL_WIDTH, LinearLayout.LayoutParams.MATCH_PARENT).apply { leftMargin = 12 }
        contentPanel.addView(createPanel("GND", gndLayout), gndTextLp)

        val subChartLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { bottomMargin = 6 }
        val lastChartLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)

        // 第 3 列：实时波形网格图层
        waveformCol.orientation = LinearLayout.VERTICAL
        waveformCol.addView(airChartView, subChartLp)
        waveformCol.addView(gndChartView, lastChartLp)
        val waveColLp = LinearLayout.LayoutParams(CHART_COL_WIDTH, LinearLayout.LayoutParams.MATCH_PARENT).apply { leftMargin = 16 }
        contentPanel.addView(waveformCol, waveColLp)

        // 第 4 列：底噪频谱网格图层
        noiseCol.orientation = LinearLayout.VERTICAL
        noiseCol.addView(airNoiseChartView, subChartLp)
        noiseCol.addView(gndNoiseChartView, lastChartLp)
        val noiseColLp = LinearLayout.LayoutParams(CHART_COL_WIDTH, LinearLayout.LayoutParams.MATCH_PARENT).apply { leftMargin = 16 }
        contentPanel.addView(noiseCol, noiseColLp)
        
        // 4. 组装外层容器与边缘拖拽指示块
        contentFrame.addView(contentPanel, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        contentFrame.addView(resizeIndicator, FrameLayout.LayoutParams(18, 18).apply { 
            gravity = Gravity.BOTTOM or Gravity.END
            setMargins(0, 0, 2, 2) 
        })
        addView(contentFrame, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))

        updateWindowLayoutWidth()
        params.height = lastExpandedHeight

        // 5. 事件绑定与独立显示切换（展开时触发绘制补刷）
        waveformToggleBtn.setOnClickListener {
            isWaveformExpanded = !isWaveformExpanded
            waveformCol.visibility = if (isWaveformExpanded) View.VISIBLE else View.GONE
            waveformToggleBtn.text = "Link Curve"
            waveformToggleBtn.background = GradientDrawable().apply {
                setColor(Color.parseColor(if (isWaveformExpanded) "#2980B9" else "#7F8C8D"))
                cornerRadius = 6f
            }
            if (isWaveformExpanded) {
                airChartView.postInvalidate()
                gndChartView.postInvalidate()
            }
            updateWindowLayoutWidth()
        }

        noiseToggleBtn.setOnClickListener {
            isNoiseExpanded = !isNoiseExpanded
            noiseCol.visibility = if (isNoiseExpanded) View.VISIBLE else View.GONE
            noiseToggleBtn.text = "Noise Floor"
            noiseToggleBtn.background = GradientDrawable().apply {
                setColor(Color.parseColor(if (isNoiseExpanded) "#27AE60" else "#7F8C8D"))
                cornerRadius = 6f
            }
            if (isNoiseExpanded) {
                airNoiseChartView.postInvalidate()
                gndNoiseChartView.postInvalidate()
            }
            updateWindowLayoutWidth()
        }

        toggleBtn.setOnClickListener {
            if (isExpanded) performGlobalToggle()
        }

        setupTouchInteraction()
    }

    private fun updateWindowLayoutWidth() {
        if (!isAttachedToWindow || !isExpanded) return
        
        var dynamicWidth = TEXT_COL_WIDTH * 2 + 50
        if (isWaveformExpanded) dynamicWidth += CHART_COL_WIDTH + 16
        if (isNoiseExpanded) dynamicWidth += CHART_COL_WIDTH + 16
        
        params.width = dynamicWidth
        try {
            windowManager.updateViewLayout(this, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupTouchInteraction() {
        setOnTouchListener(object : OnTouchListener {
            private var isDragging = false

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                if (!isAttachedToWindow) return false
                
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        lastX = event.rawX
                        lastY = event.rawY
                        startWidth = width
                        startHeight = height
                        resize = isExpanded && (event.x > (width - 120)) && (event.y > (height - 120))
                        isDragging = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (isExpanded && resize) {
                            val totalDx = event.rawX - downX
                            val totalDy = event.rawY - downY
                            
                            val newWidth = (startWidth + totalDx).toInt().coerceAtLeast(350)
                            val newHeight = (startHeight + totalDy).toInt().coerceAtLeast(250)
                            
                            params.width = newWidth
                            params.height = newHeight
                            lastExpandedHeight = newHeight
                        } else {
                            val dx = event.rawX - lastX
                            val dy = event.rawY - lastY
                            
                            if (Math.abs(event.rawX - downX) > 5 || Math.abs(event.rawY - downY) > 5) {
                                isDragging = true
                            }
                            
                            params.x += dx.toInt()
                            params.y += dy.toInt()
                        }
                        
                        lastX = event.rawX
                        lastY = event.rawY
                        
                        try {
                            windowManager.updateViewLayout(this@FloatView, params)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isExpanded && !isDragging) {
                            performGlobalToggle()
                        }
                    }
                }
                return true
            }
        })
    }

    private fun performGlobalToggle() {
        if (!isAttachedToWindow) return
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
        } else {
            isExpanded = true
            collapsedPanel.visibility = View.GONE
            topBar.visibility = View.VISIBLE
            contentFrame.visibility = View.VISIBLE
            
            panelBg.setColor(Color.argb(205, 15, 15, 15))
            panelBg.cornerRadius = 14f
            this.background = panelBg
            this.setPadding(12, 8, 12, 12)
            
            updateWindowLayoutWidth()
            params.height = lastExpandedHeight

            // 恢复全局展开时，立即通知当前处于显式可见状态的图表刷新最新缓存数据
            if (isWaveformExpanded) {
                airChartView.postInvalidate()
                gndChartView.postInvalidate()
            }
            if (isNoiseExpanded) {
                airNoiseChartView.postInvalidate()
                gndNoiseChartView.postInvalidate()
            }
        }
        try {
            windowManager.updateViewLayout(this@FloatView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createPanel(title: String, containerLayout: LinearLayout): View {
        val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val titleView = TextView(context).apply {
            text = title
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#E67E22"))
            setPadding(4, 2, 4, 6)
        }
        box.addView(titleView)
        val scroll = ScrollView(context).apply { isVerticalScrollBarEnabled = false }
        scroll.addView(containerLayout)
        box.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        return box
    }

    // =========================================================================
    // 数据动态刷新与渲染解析逻辑
    // =========================================================================
    fun updateJsonDynamic(rawJson: String) {
        if (!isAttachedToWindow) return
        post {
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

                    if (key == "noiseFloor_a" || key == "noiseFloor_g") {
                        val isAir = key == "noiseFloor_a"
                        val targetLayout = if (isAir) airLayout else gndLayout
                        val targetMap = if (isAir) airTextViewMap else gndTextViewMap
                        val chart = if (isAir) airNoiseChartView else gndNoiseChartView
                        
                        chart.addNoiseData(valueStr)
                        
                        val parts = valueStr.split(",")
                        parts.forEachIndexed { index, partValue ->
                            val subKey = "${key}_ch${index + 1}"
                            val prefixLabel = if (isAir) "Air_ch" else "Gnd_ch"
                            val displayText = "$prefixLabel${index + 1} : ${partValue.trim()}"
                            val chColor = noiseCurveColors[index % noiseCurveColors.size]
                            
                            val cachedTv = targetMap[subKey]
                            if (cachedTv != null) {
                                cachedTv.text = displayText
                                cachedTv.setTextColor(chColor)
                            } else {
                                val tv = TextView(context).apply {
                                    text = displayText
                                    textSize = 10.5f
                                    setTextColor(chColor)
                                    setPadding(6, 4, 6, 4)
                                }
                                targetLayout.addView(tv)
                                targetMap[subKey] = tv
                            }
                        }
                        continue 
                    }

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
                Log.e("FloatViewError", "数据处理渲染异常: ${e.message}")
            }
        }
    }

    private fun updateOrAddTextWithColor(layout: LinearLayout, map: HashMap<String, TextView>, key: String, value: String) {
        val cachedTv = map[key]
        
        var displayColor = when {
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
            key.contains("pass", ignoreCase = true) -> Color.parseColor("#3498DB")
            else -> Color.WHITE
        }

        // 针对 LdpcFail / failed 变红，增加停止后 5秒自动恢复
        if (key.contains("fail", ignoreCase = true) || key.contains("failed", ignoreCase = true)) {
            val oldValue = lastValues[key]
            lastValues[key] = value

            if (oldValue != null && oldValue != value) {
                redTimerRunnables[key]?.let { mainHandler.removeCallbacks(it) }
                
                val resetRunnable = Runnable {
                    map[key]?.setTextColor(Color.WHITE)
                    redTimerRunnables.remove(key)
                }
                redTimerRunnables[key] = resetRunnable
                mainHandler.postDelayed(resetRunnable, 5000)
                
                displayColor = Color.parseColor("#E74C3C")
            } else {
                displayColor = if (redTimerRunnables.containsKey(key)) {
                    Color.parseColor("#E74C3C")
                } else {
                    Color.WHITE
                }
            }
        }

        val displayText = "$key : $value"
        if (cachedTv != null) {
            cachedTv.text = displayText
            cachedTv.setTextColor(displayColor)
        } else {
            val tv = TextView(context).apply {
                text = displayText
                textSize = 10.5f
                setTextColor(displayColor)
                setPadding(6, 4, 6, 4)
            }
            
            // 确保 LdpcPass 布局置于文本面板第一栏 (Index 0)
            if (key.contains("ldpcpass", ignoreCase = true) || key.contains("pass", ignoreCase = true)) {
                layout.addView(tv, 0)
            } else {
                layout.addView(tv)
            }
            map[key] = tv
        }
    }

    // ==========================================
    // 自定义 View 组件绘制逻辑（加入 isShown 截断）
    // ==========================================

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
            textSize = 15f         
            isFakeBoldText = true  
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        fun setSignalData(rssi1: Float, rssi2: Float, snrVal: Float) {
            this.r1 = rssi1
            this.r2 = rssi2
            this.snr = snrVal
            // 仅在迷你模式可见时重绘
            if (isShown) {
                postInvalidate()
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0 || h <= 0) return

            textPaint.color = if (label == "AIR") Color.parseColor("#E67E22") else Color.parseColor("#3498DB")
            textPaint.isFakeBoldText = true
            canvas.drawText(label, w / 2f, 20f, textPaint)

            val primaryRssi = if (r1 > 0 && r2 > 0) Math.min(r1, r2) else Math.max(r1, r2)
            val (bars, barColor) = when {
                primaryRssi == 0f -> 1 to Color.parseColor("#E74C3C")
                primaryRssi < 60f -> 4 to Color.parseColor("#2ECC71")
                primaryRssi < 75f -> 3 to Color.parseColor("#F1C40F")
                primaryRssi < 90f -> 2 to Color.parseColor("#E67E22")
                else -> 1 to Color.parseColor("#E74C3C")
            }

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

            val infoStr = "${r1.toInt()}/${r2.toInt()}/${snr.toInt()}"
            val finalInfo = if (primaryRssi == 0f) "DISCONN" else infoStr
            subTextPaint.color = barColor
            canvas.drawText(finalInfo, w / 2f, h - 15f, subTextPaint)
        }
    }

    private class WaveformView(context: Context, private val isAir: Boolean) : View(context) {
        private val maxDataPoints = 100
        private val yAxisWidth = 85f 

        private val rssi1List = CopyOnWriteArrayList<Float>()
        private val rssi2List = CopyOnWriteArrayList<Float>()
        private val snrList = CopyOnWriteArrayList<Float>()

        private val axisTextPaint = Paint().apply { color = Color.parseColor("#95A5A6"); textSize = 13f; isAntiAlias = true }
        private val prefixTextPaint = Paint().apply { 
            color = Color.parseColor("#ECF0F1")
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true 
        }

        private val colorRssi1 = Color.parseColor("#2980B9")
        private val colorRssi2 = Color.parseColor("#3498DB")
        private val colorSnr   = Color.parseColor("#2ECC71")

        private val paintRssi1 = Paint().apply { color = colorRssi1; strokeWidth = 3f; style = Paint.Style.STROKE; isAntiAlias = true }
        private val paintRssi2 = Paint().apply { color = colorRssi2; strokeWidth = 2f; style = Paint.Style.STROKE; isAntiAlias = true }
        private val paintSnr   = Paint().apply { color = colorSnr; strokeWidth = 2.5f; style = Paint.Style.STROKE; isAntiAlias = true }

        private val paintTextRssi1 = Paint().apply { color = colorRssi1; textSize = 14f; isAntiAlias = true }
        private val paintTextRssi2 = Paint().apply { color = colorRssi2; textSize = 14f; isAntiAlias = true }
        private val paintTextSnr   = Paint().apply { color = colorSnr; textSize = 14f; isAntiAlias = true }

        private val gridPaint = Paint().apply { color = Color.argb(30, 255, 255, 255); strokeWidth = 1f }
        private val bgPaint = Paint().apply { color = Color.argb(15, 255, 255, 255) }

        private val rssiMin = 0f
        private val rssiMax = 120f
        private val snrMin = 0f
        private val snrMax = 50f

        fun addData(r1: Float?, r2: Float?, snr: Float?) {
            rssi1List.add(r1 ?: rssi1List.lastOrNull() ?: 0f)
            rssi2List.add(r2 ?: rssi2List.lastOrNull() ?: 0f)
            snrList.add(snr ?: snrList.lastOrNull() ?: 0f)
            
            if (rssi1List.size > maxDataPoints) rssi1List.removeAt(0)
            if (rssi2List.size > maxDataPoints) rssi2List.removeAt(0)
            if (snrList.size > maxDataPoints) snrList.removeAt(0)

            // 智能截断：仅在当前视图及所有父容器处于可见（VISIBLE）状态时进行重绘操作
            if (isShown) {
                postInvalidate()
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
            canvas.drawRect(chartLeft, 0f, chartRight, h, bgPaint)
        
            val axisRssiRange = rssiMax - rssiMin
            val labelValues = floatArrayOf(110f, 90f, 70f, 50f, 30f, 0f)
            val rssiLabels = arrayOf("110", "90", "70", "50", "30", "0")
            val snrLabels  = arrayOf("45", "35", "25", "15", "8", "0")
        
            for (i in labelValues.indices) {
                val y = h * (1f - (labelValues[i] - rssiMin) / axisRssiRange)
                if (y in 0f..h) {
                    canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
                    val textY = if (i == labelValues.lastIndex) y - 6f else y + 5f
                    canvas.drawText("${rssiLabels[i]}(${snrLabels[i]})", 5f, textY, axisTextPaint)
                }
            }
        
            val prefix = if (isAir) "[AIR] " else "[GND] "
            canvas.drawText(prefix, chartLeft + 15f, 22f, prefixTextPaint)
            val startX = chartLeft + 15f + prefixTextPaint.measureText(prefix)

            val r1Text = "R1: ${rssi1List.lastOrNull()?.toInt() ?: 0}  "
            canvas.drawText(r1Text, startX, 22f, paintTextRssi1)
            val r2Text = "R2: ${rssi2List.lastOrNull()?.toInt() ?: 0}  "
            canvas.drawText(r2Text, startX + paintTextRssi1.measureText(r1Text), 22f, paintTextRssi2)
            val snrText = "SNR: ${snrList.lastOrNull()?.toInt() ?: 0}"
            canvas.drawText(snrText, startX + paintTextRssi1.measureText(r1Text) + paintTextRssi2.measureText(r2Text), 22f, paintTextSnr)

            drawNormalCurve(canvas, rssi1List, rssiMin, rssiMax, chartLeft, chartWidth, h, paintRssi1)
            drawNormalCurve(canvas, rssi2List, rssiMin, rssiMax, chartLeft, chartWidth, h, paintRssi2)
            drawNormalCurve(canvas, snrList, minVal = snrMin, maxVal = snrMax, leftOffset = chartLeft, cWidth = chartWidth, h = h, paint = paintSnr)
        }

        private fun drawNormalCurve(canvas: Canvas, list: List<Float>, minVal: Float, maxVal: Float, leftOffset: Float, cWidth: Float, h: Float, paint: Paint) {
            val size = list.size
            if (size < 2) return
            val stepX = cWidth / (maxDataPoints - 1)
            val curRange = maxVal - minVal
            for (i in 0 until size - 1) {
                val startX = leftOffset + (i * stepX)
                val endX = leftOffset + ((i + 1) * stepX)
                val valStart = list[i].coerceIn(minVal, maxVal)
                val valEnd = list[i + 1].coerceIn(minVal, maxVal)
                canvas.drawLine(startX, h * (1f - (valStart - minVal) / curRange), endX, h * (1f - (valEnd - minVal) / curRange), paint)
            }
        }
    }

    private class NoiseFloorChartView(
        context: Context, 
        private val isAir: Boolean,
        private val curveColors: IntArray
    ) : View(context) {
        private val maxDataPoints = 100
        private val yAxisWidth = 85f
        
        private val historyList = CopyOnWriteArrayList<FloatArray>()
        
        private val axisTextPaint = Paint().apply { color = Color.parseColor("#95A5A6"); textSize = 13f; isAntiAlias = true }
        private val headerTextPaint = Paint().apply { color = Color.parseColor("#E67E22"); textSize = 14f; isFakeBoldText = true; isAntiAlias = true }
        private val gridPaint = Paint().apply { color = Color.argb(30, 255, 255, 255); strokeWidth = 1f }
        private val bgPaint = Paint().apply { color = Color.argb(20, 230, 126, 34) }

        private val curvePaints = Array(curveColors.size) { i ->
            Paint().apply { color = curveColors[i]; strokeWidth = 2f; style = Paint.Style.STROKE; isAntiAlias = true }
        }

        private val legendPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
        private val legendTextPaint = Paint().apply { color = Color.parseColor("#BDC3C7"); textSize = 11f; isAntiAlias = true }

        private val noiseMin = 30f
        private val noiseMax = 120f
        private val noiseValues = floatArrayOf(120f, 105f, 90f, 75f, 60f, 45f, 30f)
        private val noiseLabels = arrayOf("120", "105", "90", "75", "60", "45", "30")

        fun addNoiseData(rawCsv: String) {
            try {
                val parts = rawCsv.split(",")
                val floatArray = FloatArray(parts.size)
                for (i in parts.indices) {
                    floatArray[i] = parts[i].trim().toFloatOrNull() ?: 0f
                }
                historyList.add(floatArray)
                if (historyList.size > maxDataPoints) historyList.removeAt(0)
                
                // 智能截断：仅在当前视图及所有父容器处于可见（VISIBLE）状态时进行重绘操作
                if (isShown) {
                    postInvalidate()
                }
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
            canvas.drawRect(chartLeft, 0f, chartRight, h, bgPaint)
        
            val axisNoiseRange = noiseMax - noiseMin
        
            for (i in noiseValues.indices) {
                val y = h * (1f - (noiseValues[i] - noiseMin) / axisNoiseRange)
                if (y in 0f..h) {
                    canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
                    val textY = if (i == noiseValues.lastIndex) y - 4f else y + 5f
                    canvas.drawText(noiseLabels[i], 20f, textY, axisTextPaint)
                }
            }
        
            val title = if (isAir) "[AIR] NOISE" else "[GND] NOISE"
            canvas.drawText(title, chartLeft + 15f, 22f, headerTextPaint)
        
            val historySize = historyList.size
            if (historySize == 0) return
            
            val currentChannels = historyList[historySize - 1].size
            val stepX = chartWidth / (maxDataPoints - 1)
            val range = noiseMax - noiseMin
        
            for (ch in 0 until currentChannels) {
                val paint = curvePaints[ch % curvePaints.size]
                
                for (i in 0 until historySize - 1) {
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
        
            var legendRightX = w - 15f
            val legendY = 22f
        
            for (ch in (currentChannels - 1) downTo 0) {
                val chColor = curveColors[ch % curveColors.size]
                val labelStr = "ch${ch + 1}"
                
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
