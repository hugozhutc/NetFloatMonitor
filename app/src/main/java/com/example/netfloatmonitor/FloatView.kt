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

    private lateinit var miniIconView: CardView
    private lateinit var expandedPanelView: CardView
    private lateinit var minimizeBtn: Button
    
    // 右下角缩放块引用
    private lateinit var resizeHandle: View
    
    // 左右两端的文本显示组件
    private lateinit var skyTextView: TextView
    private lateinit var groundTextView: TextView

    init {
        orientation = VERTICAL
        initView(context)
        setupDragAndResizeListeners(context)
    }

    private fun initView(context: Context) {
        // 1. 创建折叠状态的悬浮小图标 (60dp x 60dp 绿圆)
        miniIconView = CardView(context).apply {
            radius = dp2px(context, 30f).toFloat()
            setCardBackgroundColor(Color.parseColor("#2ECC71"))
            layoutParams = LayoutParams(dp2px(context, 60f), dp2px(context, 60f))
            
            val tv = TextView(context).apply {
                text = "NET"
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                textSize = 14f
            }
            addView(tv, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            visibility = View.VISIBLE
        }

        // 2. 创建展开状态的数据面板布局
        expandedPanelView = CardView(context).apply {
            radius = dp2px(context, 8f).toFloat() // 恢复之前较小的圆角
            setCardBackgroundColor(Color.parseColor("#1A222D")) // 深蓝黑底色，接近图示背景
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            visibility = View.GONE

            val rootFrame = FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
            
            // 主体内容垂直排列
            val innerLayout = LinearLayout(context).apply {
                orientation = VERTICAL
                val p = dp2px(context, 10f)
                setPadding(p, p, p, p)
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }

            // 标题与收起按钮栏
            val titleLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val titleTv = TextView(context).apply {
                text = "NetFloatMonitor" // 还原顶部半透明背景的标题
                setTextColor(Color.parseColor("#80FFFFFF")) // 半透明白
                textSize = 14f
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            }
            minimizeBtn = Button(context).apply {
                text = "收起"
                textSize = 11f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#E74C3C"))
                layoutParams = LayoutParams(dp2px(context, 50f), dp2px(context, 26f))
            }
            titleLayout.addView(titleTv)
            titleLayout.addView(minimizeBtn)
            innerLayout.addView(titleLayout)

            // 双栏布局容器 (还原你图中的布局：左边 AIR，右边 GND)
            val dualColumnLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply {
                    topMargin = dp2px(context, 6f)
                }
            }

            // 【左侧栏：AIR 天空端】
            val skyLayout = LinearLayout(context).apply {
                orientation = VERTICAL
                layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            }
            val skyTitle = TextView(context).apply {
                text = "AIR"
                setTextColor(Color.parseColor("#00FF00")) // 还原图中的绿色高亮标题
                textSize = 16f
                paint.isFakeBoldText = true // 加粗
            }
            val skyScrollView = ScrollView(context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                isVerticalScrollBarEnabled = false // 隐藏滚动条更美观
            }
            skyTextView = TextView(context).apply {
                text = "waiting..."
                setTextColor(Color.WHITE) // 还原图中的白色属性文字
                textSize = 13f
                lineSpacingMultiplier = 1.2f // 调整行间距贴合原版
            }
            skyScrollView.addView(skyTextView)
            skyLayout.addView(skyTitle)
            skyLayout.addView(skyScrollView)

            // 【右侧栏：GND 地面端】
            val groundLayout = LinearLayout(context).apply {
                orientation = VERTICAL
                layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                    leftMargin = dp2px(context, 10f) // 左右留出间距
                }
            }
            val groundTitle = TextView(context).apply {
                text = "GND"
                setTextColor(Color.parseColor("#00FF00")) // 绿色高亮
                textSize = 16f
                paint.isFakeBoldText = true
            }
            val groundScrollView = ScrollView(context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                isVerticalScrollBarEnabled = false
            }
            groundTextView = TextView(context).apply {
                text = "waiting..."
                setTextColor(Color.WHITE)
                textSize = 13f
                lineSpacingMultiplier = 1.2f
            }
            groundScrollView.addView(groundTextView)
            groundLayout.addView(groundTitle)
            groundLayout.addView(groundScrollView)

            dualColumnLayout.addView(skyLayout)
            dualColumnLayout.addView(groundLayout)
            innerLayout.addView(dualColumnLayout)
            
            rootFrame.addView(innerLayout)

            // 右下角拉伸缩放响应块
            resizeHandle = View(context).apply {
                layoutParams = FrameLayout.LayoutParams(dp2px(context, 30f), dp2px(context, 30f)).apply {
                    gravity = Gravity.BOTTOM or Gravity.RIGHT
                }
                setBackgroundColor(Color.TRANSPARENT)
            }
            rootFrame.addView(resizeHandle)

            addView(rootFrame)
        }

        addView(miniIconView)
        addView(expandedPanelView)

        // 点击小图标 -> 展开面板
        miniIconView.setOnClickListener {
            if (!isExpanded) {
                isExpanded = true
                miniIconView.visibility = View.GONE
                expandedPanelView.visibility = View.VISIBLE
                
                params.width = dp2px(context, 360f)
                params.height = dp2px(context, 280f)
                windowManager.updateViewLayout(this, params)
            }
        }

        // 点击收起按钮 -> 折叠回小图标
        minimizeBtn.setOnClickListener {
            if (isExpanded) {
                isExpanded = false
                expandedPanelView.visibility = View.GONE
                miniIconView.visibility = View.VISIBLE
                
                params.width = WindowManager.LayoutParams.WRAP_CONTENT
                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                windowManager.updateViewLayout(this, params)
            }
        }
    }

    /**
     * 核心修复：精准智能拆分单条 JSON 报文到左右两栏
     */
    fun updateJson(data: String) {
        if (data.isBlank()) return

        val skyBuilder = StringBuilder()
        val groundBuilder = StringBuilder()

        // 将接收到的文本按行拆分，或者去掉花括号后按逗号拆分键值对
        val cleanData = data.replace("{", "").replace("}", "").replace("\"", "")
        val lines = cleanData.split(Regex("[\n,]+"))

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // 包含 _a 或 air 的行扔给左边 AIR 栏
            if (trimmed.contains("_a") || trimmed.contains("air", ignoreCase = true)) {
                skyBuilder.append(trimmed).append("\n")
            } 
            // 包含 _g 或 gnd 的行扔给右边 GND 栏
            else if (trimmed.contains("_g") || trimmed.contains("gnd", ignoreCase = true)) {
                groundBuilder.append(trimmed).append("\n")
            }
            // 如果是没有特定后缀的通用行，两边同时打印（或者你也可以自行分配）
            else {
                skyBuilder.append(trimmed).append("\n")
                groundBuilder.append(trimmed).append("\n")
            }
        }

        // 更新界面
        if (skyBuilder.isNotEmpty()) {
            skyTextView.text = skyBuilder.toString().trimEnd()
        }
        if (groundBuilder.isNotEmpty()) {
            groundTextView.text = groundBuilder.toString().trimEnd()
        }
    }

    private fun setupDragAndResizeListeners(context: Context) {
        // 拖拽位置
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
                            miniIconView.performClick()
                        }
                    }
                    true
                }
                else -> false
            }
        }
        miniIconView.setOnTouchListener(dragListener)
        expandedPanelView.setOnTouchListener(dragListener)

        // 右下角大小拉伸
        resizeHandle.setOnTouchListener { _, event ->
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
