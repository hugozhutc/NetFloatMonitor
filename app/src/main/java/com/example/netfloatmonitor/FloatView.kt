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
    // 延迟初始化组件，内部绝无任何二次赋值
    // ==========================================
    
    private val miniIconViewRef: CardView by lazy {
        CardView(context).apply {
            radius = dp2px(context, 30f).toFloat()
            setCardBackgroundColor(Color.parseColor("#2ECC71"))
            layoutParams = LayoutParams(dp2px(context, 60f), dp2px(context, 60f))
            visibility = View.VISIBLE
            
            val tv = TextView(context).apply {
                text = "NET"
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                textSize = 14f
            }
            addView(tv, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }
    }

    private val minimizeBtnRef: Button by lazy {
        Button(context).apply {
            text = "收起"
            textSize = 11f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#E74C3C"))
            layoutParams = LayoutParams(dp2px(context, 50f), dp2px(context, 26f))
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
            layoutParams = FrameLayout.LayoutParams(dp2px(context, 30f), dp2px(context, 30f)).apply {
                gravity = Gravity.BOTTOM or Gravity.RIGHT
            }
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private val expandedPanelViewRef: CardView by lazy {
        CardView(context).apply {
            radius = dp2px(context, 8f).toFloat()
            setCardBackgroundColor(Color.parseColor("#1A222D"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            visibility = View.GONE

            val rootFrame = FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
            
            val innerLayout = LinearLayout(context).apply {
                this.orientation = LinearLayout.VERTICAL  // 显式声明，防作用域污染
                val p = dp2px(context, 10f)
                setPadding(p, p, p, p)
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }

            // 标题与收起按钮栏
            val titleLayout = LinearLayout(context).apply {
                this.orientation = LinearLayout.HORIZONTAL  // 显式声明
                gravity = Gravity.CENTER_VERTICAL
            }
            
            val titleTv = TextView(context).apply {
                text = "NetFloatMonitor"
                setTextColor(Color.parseColor("#80FFFFFF"))
                textSize = 14f
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            }
            
            titleLayout.addView(titleTv)
            titleLayout.addView(minimizeBtnRef)
            innerLayout.addView(titleLayout)

            // 双栏布局容器 (左边 AIR，右边 GND)
            val dualColumnLayout = LinearLayout(context).apply {
                this.orientation = LinearLayout.HORIZONTAL  // 显式声明
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply {
                    topMargin = dp2px(context, 6f)
                }
            }

            // 【左侧栏：AIR 天空端】
            val skyLayout = LinearLayout(context).apply {
                this.orientation = LinearLayout.VERTICAL  // 显式声明
                layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            }
            
            val skyTitle = TextView(context).apply {
                text = "AIR"
                setTextColor(Color.parseColor("#00FF00"))
                textSize = 16f
                paint.isFakeBoldText = true
            }
            
            val skyScrollView = ScrollView(context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                isVerticalScrollBarEnabled = false
            }
            
            skyScrollView.addView(skyTextViewRef)
            skyLayout.addView(skyTitle)
            skyLayout.addView(skyScrollView)

            // 【右侧栏：GND 地面端】
            val groundLayout = LinearLayout(context).apply {
                this.orientation = LinearLayout.VERTICAL  // 显式声明
                layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                    leftMargin = dp2px(context, 10f)
                }
            }
            
            val groundTitle = TextView(context).apply {
                text = "GND"
                setTextColor(Color.parseColor("#00FF00"))
                textSize = 16f
                paint.isFakeBoldText = true
            }
            
            val groundScrollView = ScrollView(context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                isVerticalScrollBarEnabled = false
            }
            
            groundScrollView.addView(groundTextViewRef)
            groundLayout.addView(groundTitle)
            groundLayout.addView(groundScrollView)

            dualColumnLayout.addView(skyLayout)
            dualColumnLayout.addView(groundLayout)
            innerLayout.addView(dualColumnLayout)
            
            rootFrame.addView(innerLayout)
            rootFrame.addView(resizeHandleRef)
            addView(rootFrame)
        }
    }

    init {
        this.orientation = LinearLayout.VERTICAL  // 显式声明
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
