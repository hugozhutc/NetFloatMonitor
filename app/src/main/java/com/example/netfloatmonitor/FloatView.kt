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

    // 用于记忆上一次展开状态下的实时宽高（初始为标准宽高）
    private var lastExpandedWidth = 640
    private var lastExpandedHeight = 450

    // 大号悬浮球尺寸
    private val collapsedSize = 160

    private var startWidth = 0
    private var startHeight = 0
    private var downX = 0f
    private var downY = 0f
    private var resize = false

    // 顶部的控制栏容器
    private val topBar = LinearLayout(context)
    
    // 下方的数据面板层
    private val contentFrame = FrameLayout(context)
    private val contentPanel = LinearLayout(context)
    
    // 右下角微型视觉缩放提示块
    private val resizeIndicator = View(context).apply {
        val triangleBg = GradientDrawable().apply {
            setColor(Color.parseColor("#3498DB"))
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
        // 增加底部的 Padding（从 8 提高到 24），确保即使紧贴小白条，内容也不会被裁剪
        this.setPadding(8, 6, 8, 24)

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
        
        // 把缩放视觉角标固定在右下角（稍微往上抬一点，避开小白条极限边缘）
        val indicatorLp = FrameLayout.LayoutParams(15, 15).apply {
            gravity = Gravity.BOTTOM or Gravity.RIGHT
            setMargins(0, 0, 4, 16)
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
                            
                            // 大球移动时防触底安全过滤
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

        // 展开状态下点击红叉的响应
        toggleBtn.setOnClickListener {
            if (isExpanded) performToggle()
        }

        // 整体面板的手势处理
        setOnTouchListener(object : OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        startWidth = width
                        startHeight = height
                        // 将拉伸判定的热区稍微向上扩充，防止在小白条处按不到
                        resize = isExpanded && (event.x > (width - 150)) && (event.y > (height - 150))
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // 获取当前窗口在屏幕上的绝对物理位置坐标
                        val location = IntArray(2)
                        this@FloatView.getLocationOnScreen(location)
                        val absoluteY = location[1]

                        val navBarHeight = getNavigationBarHeight()
                        val usableScreenHeight = getScreenHeight() - navBarHeight

                        if (resize) {
                            // 计算缩放后的新尺寸
                            val newWidth = (startWidth + event.rawX - downX).toInt().coerceAtLeast(300)
                            var newHeight = (startHeight + event.rawY - downY).toInt().coerceAtLeast(200)
                            
                            // 用屏幕绝对坐标判定是否触底，规避 params.y 虚拟偏移误差
                            if (absoluteY + newHeight > usableScreenHeight) {
                                newHeight = usableScreenHeight - absoluteY
                            }

                            params.width = newWidth
                            params.height = newHeight
                            
                            // 实时更新并记忆最新的展开宽度和高度
                            lastExpandedWidth = newWidth
                            lastExpandedHeight = newHeight
                        } else {
                            params.x += (event.rawX - downX).toInt()
                            var targetY = params.y + (event.rawY - downY).toInt()
                            
                            // 移动位置时，同样通过绝对坐标限制，防止底边越过导航小白条
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

    /**
     * 获取系统导航栏（小白条）的实时高度
     */
    private fun getNavigationBarHeight(): Int {
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    /**
     * 获取屏幕总高度
     */
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
            // 恢复展开状态的底部留白安全间距
            this.setPadding(8, 6, 8, 24)

            // 恢复到上次记忆的宽高
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

            obj.keys().forEach { key ->
                val value = obj.get(key).toString()

                when {
                    key.endsWith("_g") -> addItem(gndLayout, key, value)
                    key.endsWith("_a") -> addItem(airLayout, key, value)
                    else -> addItem(airLayout, key, value)
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
