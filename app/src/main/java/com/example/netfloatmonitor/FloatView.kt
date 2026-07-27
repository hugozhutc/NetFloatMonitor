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

    // 状态控制：当前是否处于展开状态
    private var isExpanded = true

    // 展开状态下的标准宽高
    private val expandedWidth = 640
    private val expandedHeight = 450

    // 大号悬浮球尺寸
    private val collapsedSize = 160

    private var startWidth = 0
    private var startHeight = 0
    private var downX = 0f
    private var downY = 0f
    private var resize = false

    // 顶部的控制栏容器
    private val topBar = LinearLayout(context)
    
    // 下方的数据面板层（改用 FrameLayout 包裹，以便在右下角叠放一个缩放角标提示）
    private val contentFrame = FrameLayout(context)
    private val contentPanel = LinearLayout(context)
    
    // 【优化】右下角微型视觉缩放提示块（只在展开状态下可见）
    private val resizeIndicator = View(context).apply {
        val triangleBg = GradientDrawable().apply {
            setColor(Color.parseColor("#3498DB")) // 明亮的浅蓝色提示块
            cornerRadius = 4f
        }
        background = triangleBg
        visibility = View.VISIBLE
    }

    // 核心切换状态按钮
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

        // 180透明度黑色背景与圆角设置
        val bg = GradientDrawable()
        bg.setColor(Color.argb(180, 0, 0, 0))
        bg.cornerRadius = 10f
        this.setBackground(bg)

        // 配置顶部控制栏
        topBar.setOrientation(LinearLayout.HORIZONTAL)
        topBar.setGravity(Gravity.RIGHT or Gravity.CENTER_VERTICAL)
        topBar.setPadding(0, 0, 4, 4)
        
        val btnLp = LinearLayout.LayoutParams(45, 45)
        topBar.addView(toggleBtn, btnLp)
        addView(topBar)

        // 配置下方主面板容器
        contentPanel.setOrientation(LinearLayout.HORIZONTAL)
        airLayout.setOrientation(LinearLayout.VERTICAL)
        gndLayout.setOrientation(LinearLayout.VERTICAL)
        contentPanel.addView(createPanel("AIR", airLayout))
        contentPanel.addView(createPanel("GND", gndLayout))
        
        // 将主面板装入 FrameLayout
        contentFrame.addView(contentPanel, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        
        // 把缩放视觉角标固定在右下角（大小 15x15）
        val indicatorLp = FrameLayout.LayoutParams(15, 15).apply {
            gravity = Gravity.BOTTOM or Gravity.RIGHT
            setMargins(0, 0, 4, 4)
        }
        contentFrame.addView(resizeIndicator, indicatorLp)
        
        // 挂载混合层到最外层
        val frameLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        addView(contentFrame, frameLp)

        // 接管大球状态下的拖动与点击手势
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

        // 展开状态下点击红叉的响应
        toggleBtn.setOnClickListener {
            if (isExpanded) performToggle()
        }

        // 整体面板的手势处理（重点优化右下角盲操体验）
        setOnTouchListener(object : OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        startWidth = width
                        startHeight = height
                        
                        // 【优化】右下角触发判定范围大幅扩容：从 50 像素暴力提升到 120 像素，闭着眼睛都能抠住
                        resize = isExpanded && (event.x > (width - 120)) && (event.y > (height - 120))
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

            params.width = expandedWidth
            params.height = expandedHeight
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

    /**
     * V2.0 JSON数据精准解析处理与【动态均衡分流渲染】
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
                    // 1. 明确属于地面端的数据，依旧死死固定在右侧栏
                    key.endsWith("_g") -> {
                        addItem(gndLayout, key, value)
                    }
                    
                    // 2. 属于天空端（_a）或者缺省未指定的数据，启用智能行数均衡机制
                    else -> {
                        // 【核心改动】如果左侧栏（AIR）当前挂载的项比右侧栏（GND）多，
                        // 为了防止左侧拉得太长，自动把当前这条数据分流移到右侧栏（GND）显示
                        if (airLayout.childCount > gndLayout.childCount) {
                            addItem(gndLayout, key, value)
                        } else {
                            addItem(airLayout, key, value)
                        }
                    }
                }
            }
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
}
