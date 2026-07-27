package com.example.netfloatmonitor

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
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

    // 状态控制：当前是否处于展开状态
    private var isExpanded = true

    // 展开状态下的标准宽高
    private val expandedWidth = 640
    private val expandedHeight = 450

    // 【优化】大幅提升折叠后悬浮球的尺寸（从100增大到160），大拇指盲操极其轻松
    private val collapsedSize = 160

    private var startWidth = 0
    private var startHeight = 0
    private var downX = 0f
    private var downY = 0f
    private var resize = false

    // 顶部的控制栏容器
    private val topBar = LinearLayout(context)
    // 下方的数据面板容器
    private val contentPanel = LinearLayout(context)

    // 精巧的切换状态按钮
    private val toggleBtn = Button(context).apply {
        text = "×"
        textSize = 14f
        setTextColor(Color.WHITE)
        setGravity(Gravity.CENTER)
        val btnBg = GradientDrawable().apply {
            setColor(Color.parseColor("#C0392B")) // 初始为深红色
            cornerRadius = 6f
        }
        background = btnBg
    }

    init {
        this.setOrientation(LinearLayout.VERTICAL)
        this.setPadding(8, 6, 8, 8)

        // 180透明度黑色背景与圆角设置
        val bg = GradientDrawable()
        bg.setColor(Color.argb(180, 0, 0, 0))
        bg.cornerRadius = 10f
        this.setBackground(bg)

        // 配置顶部控制栏
        topBar.setOrientation(LinearLayout.HORIZONTAL)
        topBar.setGravity(Gravity.RIGHT or Gravity.CENTER_VERTICAL)
        topBar.setPadding(0, 0, 4, 4)
        
        // 展开状态下关闭按钮的大小
        val btnLp = LinearLayout.LayoutParams(45, 45)
        topBar.addView(toggleBtn, btnLp)
        addView(topBar)

        // 配置下方主面板
        contentPanel.setOrientation(LinearLayout.HORIZONTAL)
        airLayout.setOrientation(LinearLayout.VERTICAL)
        gndLayout.setOrientation(LinearLayout.VERTICAL)
        contentPanel.addView(createPanel("AIR", airLayout))
        contentPanel.addView(createPanel("GND", gndLayout))
        addView(contentPanel)

        // 一键切换折叠与再次展开的核心逻辑
        toggleBtn.setOnClickListener {
            val panelBg = GradientDrawable()
            
            if (isExpanded) {
                // 【执行折叠】
                isExpanded = false
                contentPanel.visibility = View.GONE // 隐藏下方数据面板
                
                // 1. 撑满折叠后的整个窗体，让整个大悬浮球都变成可点击区域
                val collapsedLp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                toggleBtn.layoutParams = collapsedLp
                
                // 2. 优化文案与样式：变成一个大号的绿色科技感圆形悬浮钮
                toggleBtn.text = "展开"
                toggleBtn.textSize = 14f
                val btnBg = GradientDrawable().apply {
                    setColor(Color.parseColor("#1ABC9C")) // 更加明亮的青绿色
                    cornerRadius = 80f // 完美正圆
                }
                toggleBtn.background = btnBg
                
                // 3. 彻底外层透明，避免死角干扰
                panelBg.setColor(Color.TRANSPARENT)
                this.setBackground(panelBg)
                this.setPadding(0, 0, 0, 0)

                // 4. 更新悬浮窗总尺寸为加大版尺寸
                params.width = collapsedSize
                params.height = collapsedSize
            } else {
                // 【执行再次展开】
                isExpanded = true
                contentPanel.visibility = View.VISIBLE
                
                // 1. 恢复右上角微型有关按钮的尺寸
                val expandedLp = LinearLayout.LayoutParams(45, 45)
                toggleBtn.layoutParams = expandedLp
                
                // 2. 恢复关闭符号与红方块样式
                toggleBtn.text = "×"
                toggleBtn.textSize = 14f
                val btnBg = GradientDrawable().apply {
                    setColor(Color.parseColor("#C0392B"))
                    cornerRadius = 6f
                }
                toggleBtn.background = btnBg
                
                // 3. 恢复 180 透明度黑色大面板背景
                panelBg.setColor(Color.argb(180, 0, 0, 0))
                panelBg.cornerRadius = 10f
                this.setBackground(panelBg)
                this.setPadding(8, 6, 8, 8)

                // 4. 恢复悬浮窗到大面板尺寸
                params.width = expandedWidth
                params.height = expandedHeight
            }
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
                        resize = isExpanded && (event.x > (width - 50))
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
