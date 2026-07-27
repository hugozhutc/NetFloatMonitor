package com.example.netfloatmonitor

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.cardview.widget.CardView

class FloatView(
    context: Context,
    private val windowManager: WindowManager,
    private val params: WindowManager.LayoutParams
) : LinearLayout(context) {

    private var isExpanded = false
    
    // 移动位置相关变量
    private var initialX = 0
    private var initialY = 0
    private var touchX = 0f
    private var touchY = 0f

    // 缩放大小相关变量
    private var initialWidth = 0
    private var initialHeight = 0
    private var resizeTouchX = 0f
    private var resizeTouchY = 0f

    // 核心修复：修改所有变量命名，加 Ref 后缀，彻底解决系统潜在的同名 val 冲突
    private lateinit var miniIconViewRef: CardView
    private lateinit var expandedPanelViewRef: CardView
    private lateinit var minimizeBtnRef: Button
    private lateinit var resizeHandleRef: View
    
    // 左右两端的文本显示组件
    private lateinit var skyTextViewRef: TextView
    private lateinit var groundTextViewRef: TextView

    init {
        orientation = VERTICAL
        initView(context)
        setupDragAndResizeListeners(context)
    }

    private fun initView(context: Context) {
        // 1. 创建折叠状态的悬浮小图标 (60dp x 60dp 绿圆)
        val miniIcon = CardView(context)
        miniIcon.radius = dp2px(context, 30f).toFloat()
        miniIcon.setCardBackgroundColor(Color.parseColor("#2ECC71"))
        miniIcon.layoutParams = LayoutParams(dp2px(context, 60f), dp2px(context, 60f))
        
        val tv = TextView(context)
        tv.text = "NET"
        tv.setTextColor(Color.WHITE)
        tv.gravity = Gravity.CENTER
        tv.textSize = 14f
        miniIcon.addView(tv, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        miniIcon.visibility = View.VISIBLE
        this.miniIconViewRef = miniIcon

        // 2. 初始化核心子控件
        val btn = Button(context)
        btn.text = "收起"
        btn.textSize = 11f
        btn.setTextColor(Color.WHITE)
        btn.setBackgroundColor(Color.parseColor("#E74C3C"))
        btn.layoutParams = LayoutParams(dp2px(context, 50f), dp2px(context, 26f))
        this.minimizeBtnRef = btn

        val skyTv = TextView(context)
        skyTv.text = "waiting..."
        skyTv.setTextColor(Color.WHITE)
        skyTv.textSize = 13f
        skyTv.lineSpacingMultiplier = 1.2f
        this.skyTextViewRef = skyTv

        val groundTv = TextView(context)
        groundTv.text = "waiting..."
        groundTv.setTextColor(Color.WHITE)
        groundTv.textSize = 13f
        groundTv.lineSpacingMultiplier = 1.2f
        this.groundTextViewRef = groundTv

        val handle = View(context)
        handle.layoutParams = FrameLayout.LayoutParams(dp2px(context, 30f), dp2px(context, 30f)).apply {
            gravity = Gravity.BOTTOM or Gravity.RIGHT
        }
        handle.setBackgroundColor(Color.TRANSPARENT)
        this.resizeHandleRef = handle

        // 3. 创建展开状态的数据面板布局
        val panel = CardView(context)
        panel.radius = dp2px(context, 8f).toFloat()
        panel.setCardBackgroundColor(Color.parseColor("#1A222D"))
        panel.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        panel.visibility = View.GONE

        val rootFrame = FrameLayout(context)
        rootFrame.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        
        val innerLayout = LinearLayout(context)
        innerLayout.orientation = VERTICAL
        val p = dp2px(context, 10f)
        innerLayout.setPadding(p, p, p, p)
        innerLayout.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

        // 标题与收起按钮栏
        val titleLayout = LinearLayout(context)
        titleLayout.orientation = HORIZONTAL
        titleLayout.gravity = Gravity.CENTER_VERTICAL
        
        val titleTv = TextView(context)
        titleTv.text = "NetFloatMonitor"
        titleTv.setTextColor(Color.parseColor("#80FFFFFF"))
        titleTv.textSize = 14f
        titleTv.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        
        titleLayout.addView(titleTv)
        titleLayout.addView(this.minimizeBtnRef)
        innerLayout.addView(titleLayout)

        // 双栏布局容器 (左边 AIR，右边 GND)
        val dualColumnLayout = LinearLayout(context)
        dualColumnLayout.orientation = HORIZONTAL
        dualColumnLayout.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply {
            topMargin = dp2px(context, 6f)
        }

        // 【左侧栏：AIR 天空端】
        val skyLayout = LinearLayout(context)
        skyLayout.orientation = VERTICAL
        skyLayout.layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
        
        val skyTitle = TextView(context)
        skyTitle.text = "AIR"
        skyTitle.setTextColor(Color.parseColor("#00FF00"))
        skyTitle.textSize = 16f
        skyTitle.paint.isFakeBoldText = true
        
        val skyScrollView = ScrollView(context)
        skyScrollView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        skyScrollView.isVerticalScrollBarEnabled = false
        
        skyScrollView.addView(this.skyTextViewRef)
        skyLayout.addView(skyTitle)
        skyLayout.addView(skyScrollView)

        // 【右侧栏：GND 地面端】
        val groundLayout = LinearLayout(context)
        groundLayout.orientation = VERTICAL
        groundLayout.layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
            leftMargin = dp2px(context, 10f)
        }
        
        val groundTitle = TextView(context)
        groundTitle.text = "GND"
        groundTitle.setTextColor(Color.parseColor("#00FF00"))
        groundTitle.textSize = 16f
        groundTitle.paint.isFakeBoldText = true
        
        val groundScrollView = ScrollView(context)
        groundScrollView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        groundScrollView.isVerticalScrollBarEnabled = false
        
        groundScrollView.addView(this.groundTextViewRef)
        groundLayout.addView(groundTitle)
        groundLayout.addView(groundScrollView)

        dualColumnLayout.addView(skyLayout)
        dualColumnLayout.addView(groundLayout)
        innerLayout.addView(dualColumnLayout)
        
        rootFrame.addView(innerLayout)
        rootFrame.addView(this.resizeHandleRef)
        panel.addView(rootFrame)
        
        this.expandedPanelViewRef = panel

        addView(this.miniIconViewRef)
        addView(this.expandedPanelViewRef)

        // 点击小图标 -> 展开面板
        this.miniIconViewRef.setOnClickListener {
            if (!isExpanded) {
                isExpanded = true
                this.miniIconViewRef.visibility = View.GONE
                this.expandedPanelViewRef.visibility = View.VISIBLE
                
                params.width = dp2px(context, 360f)
                params.height = dp2px(context, 280f)
                windowManager.updateViewLayout(this, params)
            }
        }

        // 点击收起按钮 -> 折叠回小图标
        this.minimizeBtnRef.setOnClickListener {
            if (isExpanded) {
                isExpanded = false
                this.expandedPanelViewRef.visibility = View.GONE
                this.miniIconViewRef.visibility = View.VISIBLE
                
                params.width = WindowManager.LayoutParams.WRAP_CONTENT
                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                windowManager.updateViewLayout(this, params)
            }
        }
    }

    /**
     * 精准智能拆分单条 JSON 报文到左右两栏
     */
    fun updateJson(data: String) {
        if (data.isBlank()) return

        val skyBuilder = StringBuilder()
        val groundBuilder = StringBuilder()

        val cleanData = data.replace("{", "").replace("}", "").replace("\"", "")
        val lines = cleanData.split(Regex("[\n,]+"))

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.contains("_a") || trimmed.contains("air", ignoreCase = true)) {
                skyBuilder.append(trimmed).append("\n")
            } 
            else if (trimmed.contains("_g") || trimmed.contains("gnd", ignoreCase = true)) {
                groundBuilder.append(trimmed).append("\n")
            }
            else {
                skyBuilder.append(trimmed).append("\n")
                groundBuilder.append(trimmed).append("\n")
            }
        }

        if (skyBuilder.isNotEmpty()) {
            skyTextViewRef.text = skyBuilder.toString().trimEnd()
        }
        if (groundBuilder.isNotEmpty()) {
            groundTextViewRef.text = groundBuilder.toString().trimEnd()
        }
    }

    private fun setupDragAndResizeListeners(context: Context) {
        val dragListener = OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(this, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaX = Math.abs(event.rawX - touchX)
                    val deltaY = Math.abs(event.rawY - touchY)
                    if (deltaX < 10 && deltaY < 10) {
                        if (!isExpanded) {
                            miniIconViewRef.performClick()
                        }
                    }
                    true
                }
                else -> false
            }
        }
        miniIconViewRef.setOnTouchListener(dragListener)
        expandedPanelViewRef.setOnTouchListener(dragListener)

        resizeHandleRef.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialWidth = params.width
                    initialHeight = params.height
                    resizeTouchX = event.rawX
                    resizeTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - resizeTouchX).toInt()
                    val deltaY = (event.rawY - resizeTouchY).toInt()
                    
                    val newWidth = (initialWidth + deltaX).coerceAtLeast(dp2px(context, 260f))
                    val newHeight = (initialHeight + deltaY).coerceAtLeast(dp2px(context, 180f))
                    
                    params.width = newWidth
                    params.height = newHeight
                    windowManager.updateViewLayout(this, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun dp2px(context: Context, dp: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }
}
