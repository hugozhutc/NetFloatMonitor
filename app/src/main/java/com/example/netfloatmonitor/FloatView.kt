package com.example.netfloatmonitor

import android.content.Context
import android.graphics.Color
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

class FloatView(
    context: Context,
    private val windowManager: WindowManager,
    private val params: WindowManager.LayoutParams
) : LinearLayout(context) {

    private val airLayout = LinearLayout(context)
    private val gndLayout = LinearLayout(context)

    private var startWidth = 0
    private var startHeight = 0
    private var downX = 0f
    private var downY = 0f
    private var resize = false

    // 新增：小巧的隐藏/收起按钮（带微圆角深红背景，更加精致）
    private val hideBtn = Button(context).apply {
        text = "×"
        textSize = 14f
        setTextColor(Color.WHITE)
        setGravity(Gravity.CENTER)
        val btnBg = GradientDrawable().apply {
            setColor(Color.parseColor("#C0392B")) // 深红色背景
            cornerRadius = 6f
        }
        background = btnBg
    }

    init {
        // 采用外层垂直布局，方便将隐藏按钮栏和下方的数据面板完美分层
        this.setOrientation(LinearLayout.VERTICAL)
        this.setPadding(8, 6, 8, 8)

        // 180透明度黑色背景与圆角设置
        val bg = GradientDrawable()
        bg.setColor(Color.argb(180, 0, 0, 0))
        bg.cornerRadius = 10f
        this.setBackground(bg)

        // 创建顶部的微型控制栏（把隐藏按钮靠右放置）
        val topBar = LinearLayout(context).apply {
            setOrientation(LinearLayout.HORIZONTAL)
            setGravity(Gravity.RIGHT or Gravity.CENTER_VERTICAL)
            setPadding(0, 0, 4, 4)
        }
        
        // 显式设置按钮的大小（宽45，高45，在手机上刚好是一个精巧可点的微型正方形）
        val btnLp = LinearLayout.LayoutParams(45, 45)
        topBar.addView(hideBtn, btnLp)
        addView(topBar)

        // 创建下方的数据主面板容器（保持横向 AIR / GND 双栏）
        val contentPanel = LinearLayout(context)
        contentPanel.setOrientation(LinearLayout.HORIZONTAL)

        // 初始化子布局方向
        airLayout.setOrientation(LinearLayout.VERTICAL)
        gndLayout.setOrientation(LinearLayout.VERTICAL)

        // 挂载 AIR 和 GND 双面板到容器中
        contentPanel.addView(createPanel("AIR", airLayout))
        contentPanel.addView(createPanel("GND", gndLayout))
        addView(contentPanel)

        // 点击隐藏按钮的处理逻辑：直接让悬浮窗缩小为不可见的极小尺寸，或者让内容不可见
        // 这里采用最直接的方式：点击后直接隐藏整个内容或让整个 View 移除/隐藏
        hideBtn.setOnClickListener {
            // 方式：直接将悬浮窗整体宽高设为 0，实现完美隐藏
            params.width = 0
            params.height = 0
            windowManager.updateViewLayout(this@FloatView, params)
        }

        // 拖动与缩放手势处理
        setOnTouchListener(object : OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        startWidth = width
                        startHeight = height
                        resize = event.x > (width - 50)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (resize) {
                            params.width = (startWidth + event.rawX - downX).toInt().coerceAtLeast(300)
                            params.height = (startHeight + event.rawY - downY).toInt().coerceAtLeast(200)
                        } else {
                            params.x += (event.rawX - downX).toInt()
                            params.y += (event.rawY - downY).toInt()
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

        // 显式使用 setLayoutParams 规避重载冲突
        val lp = LinearLayout.LayoutParams(300, LinearLayout.LayoutParams.MATCH_PARENT)
        box.addView(scroll, lp)

        return box
    }

    /**
     * V2.0 JSON数据精准解析处理与动态渲染
     */
    fun updateJson(json: String) {
        try {
            if (json.isBlank()) return
            val obj = JSONObject(json)

            airLayout.removeAllViews()
            gndLayout.removeAllViews()

            obj.keys().forEach { key ->
                val value = obj.get(key).toString()

                when {
                    key.endsWith("_a") -> {
                        addItem(airLayout, key, value)
                    }
                    key.endsWith("_g") -> {
                        addItem(gndLayout, key, value)
                    }
                    else -> {
                        addItem(airLayout, key, value)
                    }
                }
            }
        } catch (e: Exception) {
            airLayout.removeAllViews()
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
}
