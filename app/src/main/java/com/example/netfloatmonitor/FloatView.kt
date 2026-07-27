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
    
    // 右侧自定义曲线图实例
    private val chartView = WaveformView(context)

    private var isExpanded = true
    // 增加到 6 条曲线后，建议展开宽度保持 960 或更大，方便并排容纳三栏
    private var lastExpandedWidth = 1000
    private var lastExpandedHeight = 480
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
        
        contentPanel.addView(createPanel("AIR", airLayout))
        contentPanel.addView(createPanel("GND", gndLayout))
        
        // 右侧追加曲线图面板 (指定 360dp 宽度让 6 条线的图例显示更充裕)
        val chartContainerLp = LinearLayout.LayoutParams(360, LinearLayout.LayoutParams.MATCH_PARENT).apply {
            setMargins(12, 0, 4, 0)
        }
        contentPanel.addView(chartView, chartContainerLp)
        
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

            // 定义 6 个波形变量
            var airRssi1: Float? = null
            var airRssi2: Float? = null
            var airSnr: Float? = null
            
            var gndRssi1: Float? = null
            var gndRssi2: Float? = null
            var gndSnr: Float? = null

            obj.keys().forEach { key ->
                val valueStr = obj.get(key).toString()
                val lowerKey = key.lowercase()
                val numValue = valueStr.toFloatOrNull()

                if (numValue != null) {
                    when {
                        // 天空端数据解析 (_a 后缀)
                        lowerKey.endsWith("_a") -> {
                            when {
                                lowerKey.contains("rssi1") -> airRssi1 = numValue
                                lowerKey.contains("rssi2") -> airRssi2 = numValue
                                lowerKey.contains("rssi") && airRssi1 == null -> airRssi1 = numValue // 兼容只有单线 key 叫 rssi_a 的情况
                                lowerKey.contains("snr") -> airSnr = numValue
                            }
                        }
                        // 地面端数据解析 (_g 后缀)
                        lowerKey.endsWith("_g") -> {
                            when {
                                lowerKey.contains("rssi1") -> gndRssi1 = numValue
                                lowerKey.contains("rssi2") -> gndRssi2 = numValue
                                lowerKey.contains("rssi") && gndRssi1 == null -> gndRssi1 = numValue
                                lowerKey.contains("snr") -> gndSnr = numValue
                            }
                        }
                        // 没有带后缀的通用键名提取兜底
                        lowerKey.contains("air_rssi1") -> airRssi1 = numValue
                        lowerKey.contains("air_rssi2") -> airRssi2 = numValue
                        lowerKey.contains("air_snr") -> airSnr = numValue
                        lowerKey.contains("gnd_rssi1") -> gndRssi1 = numValue
                        lowerKey.contains("gnd_rssi2") -> gndRssi2 = numValue
                        lowerKey.contains("gnd_snr") -> gndSnr = numValue
                    }
                }

                // 渲染左侧面板文本不变
                when {
                    key.endsWith("_g") -> addItem(gndLayout, key, valueStr)
                    key.endsWith("_a") -> addItem(airLayout, key, valueStr)
                    else -> addItem(airLayout, key, valueStr)
                }
            }

            // 把这 6 条曲线的最新动态值送入图形引擎
            chartView.addData(airRssi1, airRssi2, airSnr, gndRssi1, gndRssi2, gndSnr)

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
     * 【升级版】纯 Canvas 实时 6 曲线波形图组件
     */
    private class WaveformView(context: Context) : View(context) {
        private val maxDataPoints = 40 // 数据宽度

        // 6 个高性能队列
        private val airRssi1List = LinkedList<Float>()
        private val airRssi2List = LinkedList<Float>()
        private val airSnrList = LinkedList<Float>()
        
        private val gndRssi1List = LinkedList<Float>()
        private val gndRssi2List = LinkedList<Float>()
        private val gndSnrList = LinkedList<Float>()

        private val textPaint = Paint().apply {
            color = Color.LTGRAY
            textSize = 20f
            isAntiAlias = true
        }

        // 天空 RSSI1 (深蓝) & RSSI2 (淡蓝)
        private val paintAirRssi1 = Paint().apply { color = Color.parseColor("#2980B9"); strokeWidth = 4f; style = Paint.Style.STROKE; isAntiAlias = true }
        private val paintAirRssi2 = Paint().apply { color = Color.parseColor("#3498DB"); strokeWidth = 3f; style = Paint.Style.STROKE; isAntiAlias = true }
        // 天空 SNR (明绿)
        private val paintAirSnr = Paint().apply { color = Color.parseColor("#2ECC71"); strokeWidth = 3f; style = Paint.Style.STROKE; isAntiAlias = true }

        // 地面 RSSI1 (深红/暗橙) & RSSI2 (亮黄/浅橙)
        private val paintGndRssi1 = Paint().apply { color = Color.parseColor("#D35400"); strokeWidth = 4f; style = Paint.Style.STROKE; isAntiAlias = true }
        private val paintGndRssi2 = Paint().apply { color = Color.parseColor("#E67E22"); strokeWidth = 3f; style = Paint.Style.STROKE; isAntiAlias = true }
        // 地面 SNR (紫色或青色，这里选紫色区分天空绿)
        private val paintGndSnr = Paint().apply { color = Color.parseColor("#9B59B6"); strokeWidth = 3f; style = Paint.Style.STROKE; isAntiAlias = true }

        private val gridPaint = Paint().apply {
            color = Color.argb(45, 255, 255, 255)
            strokeWidth = 1.5f
        }

        fun addData(aR1: Float?, aR2: Float?, aSnr: Float?, gR1: Float?, gR2: Float?, gSnr: Float?) {
            // 维持上一帧连续性防断流
            airRssi1List.addLast(aR1 ?: airRssi1List.lastOrNull() ?: -100f)
            airRssi2List.addLast(aR2 ?: airRssi2List.lastOrNull() ?: -100f)
            airSnrList.addLast(aSnr ?: airSnrList.lastOrNull() ?: 0f)
            
            gndRssi1List.addLast(gR1 ?: gndRssi1List.lastOrNull() ?: -100f)
            gndRssi2List.addLast(gR2 ?: gndRssi2List.lastOrNull() ?: -100f)
            gndSnrList.addLast(gSnr ?: gndSnrList.lastOrNull() ?: 0f)

            // 超长截断
            if (airRssi1List.size > maxDataPoints) airRssi1List.removeFirst()
            if (airRssi2List.size > maxDataPoints) airRssi2List.removeFirst()
            if (airSnrList.size > maxDataPoints) airSnrList.removeFirst()
            if (gndRssi1List.size > maxDataPoints) gndRssi1List.removeFirst()
            if (gndRssi2List.size > maxDataPoints) gndRssi2List.removeFirst()
            if (gndSnrList.size > maxDataPoints) gndSnrList.removeFirst()

            postInvalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0 || h <= 0) return

            // 1. 背景标线
            canvas.drawLine(0f, h * 0.25f, w, h * 0.25f, gridPaint)
            canvas.drawLine(0f, h * 0.5f, w, h * 0.5f, gridPaint)
            canvas.drawLine(0f, h * 0.75f, w, h * 0.75f, gridPaint)

            // 2. 绘制 6 条线的实时文字状态看板 (分左右两列排布，防重叠)
            canvas.drawText("A_R1: ${airRssi1List.lastOrNull()?.toInt()}", 10f, 25f, textPaint)
            canvas.drawText("A_R2: ${airRssi2List.lastOrNull()?.toInt()}", 130f, 25f, textPaint)
            canvas.drawText("A_SNR: ${airSnrList.lastOrNull()?.toInt()}", 250f, 25f, textPaint)

            canvas.drawText("G_R1: ${gndRssi1List.lastOrNull()?.toInt()}", 10f, 55f, textPaint)
            canvas.drawText("G_R2: ${gndRssi2List.lastOrNull()?.toInt()}", 130f, 55f, textPaint)
            canvas.drawText("G_SNR: ${gndSnrList.lastOrNull()?.toInt()}", 250f, 55f, textPaint)

            // 3. 实时绘制 6 条归一化曲线 (RSSI 映射量程 -120 到 -20，SNR 映射量程 -10 到 40)
            drawCurve(canvas, airRssi1List, -120f, -20f, w, h, paintAirRssi1)
            drawCurve(canvas, airRssi2List, -120f, -20f, w, h, paintAirRssi2)
            drawCurve(canvas, airSnrList, -10f, 40f, w, h, paintAirSnr)
            
            drawCurve(canvas, gndRssi1List, -120f, -20f, w, h, paintGndRssi1)
            drawCurve(canvas, gndRssi2List, -120f, -20f, w, h, paintGndRssi2)
            drawCurve(canvas, gndSnrList, -10f, 40f, w, h, paintGndSnr)
        }

        private fun drawCurve(canvas: Canvas, list: List<Float>, minVal: Float, maxVal: Float, w: Float, h: Float, paint: Paint) {
            if (list.size < 2) return
            val stepX = w / (maxDataPoints - 1)
            val range = maxVal - minVal

            for (i in 0 until list.size - 1) {
                val startX = i * stepX
                val endX = (i + 1) * stepX

                val valStart = list[i].coerceIn(minVal, maxVal)
                val valEnd = list[i + 1].coerceIn(minVal, maxVal)

                val startY = h * (1f - (valStart - minVal) / range)
                val endY = h * (1f - (valEnd - minVal) / range)

                canvas.drawLine(startX, startY, endX, endY, paint)
            }
        }
    }
}
