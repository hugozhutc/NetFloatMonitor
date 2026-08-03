package com.example.netfloatmonitor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject

@SuppressLint("ViewConstructor")
class FloatView(
    context: Context,
    private val windowManager: WindowManager,
    private val windowParams: WindowManager.LayoutParams
) : FrameLayout(context) {

    // 布局尺寸常量 (可根据实际 7 寸屏的 DPI 微调)
    private val LEFT_PANEL_WIDTH = dpToPx(320)
    private val RIGHT_CHARTS_WIDTH = dpToPx(550)
    
    // 状态标记
    private var isExpanded = true

    // UI 容器声明
    private lateinit var mainContainer: LinearLayout
    private lateinit var leftPanel: LinearLayout
    private lateinit var rightChartsContainer: LinearLayout
    private lateinit var headerLayout: LinearLayout

    // 文本数据组件
    private lateinit var airTextView: TextView
    private lateinit var gndTextView: TextView

    // 图表组件 (假设你的 WaveformView 和 SpectrumView 已经实现)
    // 这里用 View 占位，你替换为你自己的自定义 View 实例即可
    private lateinit var waveformView: View 
    private lateinit var spectrumView: View

    // 拖拽相关坐标
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    init {
        initUI()
        setupDragListener()
        
        // 初始宽度设定
        windowParams.width = LEFT_PANEL_WIDTH + RIGHT_CHARTS_WIDTH
        windowManager.updateViewLayout(this, windowParams)
    }

    private fun initUI() {
        // 1. 根容器 (水平排列)
        mainContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // 半透明暗色背景，带有圆角
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#D9000000")) // 85% 黑色
                cornerRadius = 16f
                setStroke(2, Color.parseColor("#444444"))
            }
        }

        // 2. 左侧常驻遥测面板
        leftPanel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LEFT_PANEL_WIDTH, ViewGroup.LayoutParams.MATCH_PARENT)
            setPadding(20, 20, 20, 20)
        }

        // 2.1 顶部 Header (包含标题、折叠按钮、关闭按钮)
        headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(0, 0, 0, 20)
        }

        val titleText = TextView(context).apply {
            text = "链路遥测 (拖拽)"
            setTextColor(Color.LTGRAY)
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnToggle = Button(context).apply {
            text = "📊 收起"
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#007ACC"))
            layoutParams = LinearLayout.LayoutParams(dpToPx(70), dpToPx(35)).apply { marginEnd = 10 }
            setPadding(0,0,0,0)
            setOnClickListener { toggleDrawer(this) }
        }

        val btnClose = Button(context).apply {
            text = "×"
            textSize = 14f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#CC0000"))
            layoutParams = LinearLayout.LayoutParams(dpToPx(35), dpToPx(35))
            setPadding(0,0,0,0)
            setOnClickListener { 
                // 停止服务或移除视图的逻辑
                windowManager.removeView(this@FloatView) 
            }
        }

        headerLayout.addView(titleText)
        headerLayout.addView(btnToggle)
        headerLayout.addView(btnClose)

        // 2.2 AIR 和 GND 的数据文本显示区
        airTextView = createDataTextView("[ AIR 天空端 ]\n等待数据...", "#00FFCC")
        gndTextView = createDataTextView("[ GND 地面端 ]\n等待数据...", "#FFCC00")

        leftPanel.addView(headerLayout)
        leftPanel.addView(airTextView)
        leftPanel.addView(gndTextView)

        // 3. 右侧图表区 (垂直排列：折线图占一半，频谱图占一半)
        rightChartsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(RIGHT_CHARTS_WIDTH, dpToPx(400)) // 整体高度设为400dp
            setPadding(10, 20, 20, 20)
        }

        // TODO: 这里替换为你自己写的 WaveformView 实例
        waveformView = View(context).apply {
            setBackgroundColor(Color.parseColor("#111111"))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                bottomMargin = dpToPx(10)
            }
        }

        // TODO: 这里替换为你自己写的 SpectrumView 实例
        spectrumView = View(context).apply {
            setBackgroundColor(Color.parseColor("#111111"))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        rightChartsContainer.addView(waveformView)
        rightChartsContainer.addView(spectrumView)

        // 4. 组装
        mainContainer.addView(leftPanel)
        mainContainer.addView(rightChartsContainer)
        addView(mainContainer)
    }

    private fun createDataTextView(defaultText: String, borderColor: String): TextView {
        return TextView(context).apply {
            text = defaultText
            setTextColor(Color.WHITE)
            textSize = 14f
            setLineSpacing(4f, 1.2f)
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                bottomMargin = dpToPx(10)
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1AFFFFFF"))
                cornerRadius = 8f
                setStroke(3, Color.parseColor(borderColor)) // 左侧色带区分 AIR/GND
            }
        }
    }

    // --- 核心折叠逻辑 ---
    private fun toggleDrawer(btn: Button) {
        isExpanded = !isExpanded
        if (isExpanded) {
            btn.text = "📊 收起"
            rightChartsContainer.visibility = View.VISIBLE
            windowParams.width = LEFT_PANEL_WIDTH + RIGHT_CHARTS_WIDTH
        } else {
            btn.text = "📊 展开"
            rightChartsContainer.visibility = View.GONE
            windowParams.width = LEFT_PANEL_WIDTH
        }
        
        try {
            windowManager.updateViewLayout(this, windowParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- 窗口拖拽逻辑 ---
    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragListener() {
        // 绑定拖拽事件到 headerLayout，避免误触影响图表或文本的滑动
        headerLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = windowParams.x
                    initialY = windowParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    windowParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    windowParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(this, windowParams)
                    true
                }
                else -> false
            }
        }
    }

    // --- 数据更新入口 ---
    // UDP 服务端收到 JSON 后直接回调这个方法
    fun updateJsonDynamic(rawJson: String) {
        post {
            try {
                val json = JSONObject(rawJson)
                
                // 1. 更新左侧常驻文本
                val airRssi1 = json.optString("air_rssi1", "--")
                val airRssi2 = json.optString("air_rssi2", "--")
                val airSnr = json.optString("air_snr", "--")
                val txFailed = json.optString("tx_failed_a", "0")
                
                airTextView.text = "[ AIR 天空端 ]\nRSSI 1: ${airRssi1} dBm\nRSSI 2: ${airRssi2} dBm\nSNR: ${airSnr} dB\nTx Failed: $txFailed"

                val gndRssi1 = json.optString("gnd_rssi1", "--")
                val gndRssi2 = json.optString("gnd_rssi2", "--")
                val gndSnr = json.optString("gnd_snr", "--")
                val passRate = json.optString("pass_rate_g", "--")

                gndTextView.text = "[ GND 地面端 ]\nRSSI 1: ${gndRssi1} dBm\nRSSI 2: ${gndRssi2} dBm\nSNR: ${gndSnr} dB\nPass Rate: $passRate"

                // 2. 更新右侧图表 (你需要强转回你的实际自定义 View 类型并调用它的更新方法)
                // (waveformView as WaveformView).updateData(...)
                // (spectrumView as SpectrumView).updateData(...)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 工具方法：dp 转 px
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
