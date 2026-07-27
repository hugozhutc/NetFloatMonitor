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

    // ==========================================
    // 终极修复：使用显式 receiver (this.layoutParams) 彻底解决作用域穿透赋值冲突
    // ==========================================
    
    private val miniIconViewRef: CardView by lazy { createMiniIconView(context) }
    private val expandedPanelViewRef: CardView by lazy { createExpandedPanelView(context) }
    
    private val minimizeBtnRef: Button by lazy {
        Button(context).apply {
            text = "收起"
            textSize = 11f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#E74C3C"))
            this.layoutParams = LinearLayout.LayoutParams(dp2px(context, 50f), dp2px(context, 26f)) // 显式指定 this
        }
    }

    private val skyTextViewRef: TextView by lazy {
        TextView(context).apply {
            text = "waiting..."
            setTextColor(Color.WHITE)
            textSize = 13f
            lineSpacingMultiplier = 1.2f
        }
    }

    private val groundTextViewRef: TextView by lazy {
        TextView(context).apply {
            text = "waiting..."
            setTextColor(Color.WHITE)
            textSize = 13f
            lineSpacingMultiplier = 1.2f
        }
    }

    private val resizeHandleRef: View by lazy {
        View(context).apply {
            val lp = FrameLayout.LayoutParams(dp2px(context, 30f), dp2px(context, 30f))
            lp.gravity = Gravity.BOTTOM or Gravity.RIGHT
            this.layoutParams = lp // 显式指定 this
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun createMiniIconView(ctx: Context): CardView {
        val card = CardView(ctx)
        card.radius = dp2px(ctx, 30f).toFloat()
        card.setCardBackgroundColor(Color.parseColor("#2ECC71"))
        card.layoutParams = LinearLayout.LayoutParams(dp2px(ctx, 60f), dp2px(ctx, 60f))
        card.visibility = View.VISIBLE
        
        val tv = TextView(ctx)
        tv.text = "NET"
        tv.setTextColor(Color.WHITE)
        tv.gravity = Gravity.CENTER
        tv.textSize = 14f
        
        card.addView(tv, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        return card
    }

    private fun createExpandedPanelView(ctx: Context): CardView {
        val panel = CardView(ctx)
        panel.radius = dp2px(ctx, 8f).toFloat()
        panel.setCardBackgroundColor(Color.parseColor("#1A222D"))
        panel.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        panel.visibility = View.GONE

        val rootFrame = FrameLayout(ctx)
        rootFrame.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        
        val innerLayout = LinearLayout(ctx)
        innerLayout.orientation = LinearLayout.VERTICAL
        val p = dp2px(ctx, 10f)
        innerLayout.setPadding(p, p, p, p)
        innerLayout.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

        // 标题与收起按钮栏
        val titleLayout = LinearLayout(ctx)
        titleLayout.orientation = LinearLayout.HORIZONTAL
        titleLayout.gravity = Gravity.CENTER_VERTICAL
        
        val titleTv = TextView(ctx)
        titleTv.text = "NetFloatMonitor"
        titleTv.setTextColor(Color.parseColor("#80FFFFFF"))
        titleTv.textSize = 14f
        titleTv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        
        titleLayout.addView(titleTv)
        titleLayout.addView(minimizeBtnRef)
        innerLayout.addView(titleLayout)

        // 双栏布局容器 (左边 AIR，右边 GND)
        val dualColumnLayout = LinearLayout(ctx)
        dualColumnLayout.orientation = LinearLayout.HORIZONTAL
        val dualLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        dualLp.topMargin = dp2px(ctx, 6f)
        dualColumnLayout.layoutParams = dualLp

        // 【左侧栏：AIR 天空端】
        val skyLayout = LinearLayout(ctx)
        skyLayout.orientation = LinearLayout.VERTICAL
        skyLayout.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        
        val skyTitle = TextView(ctx)
        skyTitle.text = "AIR"
        skyTitle.setTextColor(Color.parseColor("#00FF00"))
        skyTitle.textSize = 16f
        skyTitle.paint.isFakeBoldText = true
        
        val skyScrollView = ScrollView(ctx)
        skyScrollView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        skyScrollView.isVerticalScrollBarEnabled = false
        
        skyScrollView.addView(skyTextViewRef)
        skyLayout.addView(skyTitle)
        skyLayout.addView(skyScrollView)

        // 【右侧栏：GND 地面端】
        val groundLayout = LinearLayout(ctx)
        groundLayout.orientation = LinearLayout.VERTICAL
        val groundLp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        groundLp.leftMargin = dp2px(ctx, 10f)
        groundLayout.layoutParams = groundLp
        
        val groundTitle = TextView(ctx)
        groundTitle.text = "GND"
        groundTitle.setTextColor(Color.parseColor("#00FF00"))
        groundTitle.textSize = 16f
        groundTitle.paint.isFakeBoldText = true
        
        val groundScrollView = ScrollView(ctx)
        groundScrollView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        groundScrollView.isVerticalScrollBarEnabled = false
        
        groundScrollView.addView(groundTextViewRef)
        groundLayout.addView(groundTitle)
        groundLayout.addView(groundScrollView)

        dualColumnLayout.addView(skyLayout)
        dualColumnLayout.addView(groundLayout)
        innerLayout.addView(dualColumnLayout)
        
        rootFrame.addView(innerLayout)
        rootFrame.addView(resizeHandleRef)
        panel.addView(rootFrame)
        
        return panel
    }

    init {
        this.orientation = LinearLayout.VERTICAL
        initView()
        setupDragAndResizeListeners(context)
    }

    private fun initView() {
        addView(miniIconViewRef)
        addView(expandedPanelViewRef)

        // 点击小图标 -> 展开面板
        miniIconViewRef.setOnClickListener {
            if (!isExpanded) {
                isExpanded = true
                miniIconViewRef.visibility = View.GONE
                expandedPanelViewRef.visibility = View.VISIBLE
                
                params.width = dp2px(context, 360f)
                params.height = dp2px(context, 280f)
                windowManager.updateViewLayout(this, params)
            }
        }

        // 点击收起按钮 -> 折叠回小图标
        minimizeBtnRef.setOnClickListener {
            if (isExpanded) {
                isExpanded = false
                expandedPanelViewRef.visibility = View.GONE
                miniIconViewRef.visibility = View.VISIBLE
                
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
