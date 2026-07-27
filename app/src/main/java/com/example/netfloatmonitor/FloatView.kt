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

        // 2. 创建展开状态的数据面板布局 (使用 FrameLayout 方便右下角层叠缩放块)
        expandedPanelView = CardView(context).apply {
            radius = dp2px(context, 12f).toFloat()
            setCardBackgroundColor(Color.parseColor("#1C1C1E"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            visibility = View.GONE

            val rootFrame = FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
            
            // 主体内容垂直排列
            val innerLayout = LinearLayout(context).apply {
                orientation = VERTICAL
                val p = dp2px(context, 12f)
                setPadding(p, p, p, p)
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }

            // 标题与收起按钮栏
            val titleLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val titleTv = TextView(context).apply {
                text = "网络监控面板"
                setTextColor(Color.WHITE)
                textSize = 16f
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            }
            minimizeBtn = Button(context).apply {
                text = "收起"
                textSize = 12f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#E74C3C"))
                layoutParams = LayoutParams(dp2px(context, 60f), dp2px(context, 30f))
            }
            titleLayout.addView(titleTv)
            titleLayout.addView(minimizeBtn)
            innerLayout.addView(titleLayout)

            // 双栏布局容器 (左边天空端，右边地面端)
            val dualColumnLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply {
                    topMargin = dp2px(context, 8f)
                }
            }

            // 【左侧栏：天空端】
            val skyLayout = LinearLayout(context).apply {
                orientation = VERTICAL
                layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            }
            val skyTitle = TextView(context).apply {
                text = "【天空端】"
                setTextColor(Color.parseColor("#3498DB"))
                textSize = 12f
            }
            val skyScrollView = ScrollView(context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            }
            skyTextView = TextView(context).apply {
                text = "等待数据..."
                setTextColor(Color.GREEN)
                textSize = 11f
            }
            skyScrollView.addView(skyTextView)
            skyLayout.addView(skyTitle)
            skyLayout.addView(skyScrollView)

            // 分界线
            val divider = View(context).apply {
                setBackgroundColor(Color.DKGRAY)
                layoutParams = LayoutParams(dp2px(context, 1f), LayoutParams.MATCH_PARENT).apply {
                    leftMargin = dp2px(context, 6f)
                    rightMargin = dp2px(context, 6f)
                }
            }

            // 【右侧栏：地面端】
            val groundLayout = LinearLayout(context).apply {
                orientation = VERTICAL
                layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            }
            val groundTitle = TextView(context).apply {
                text = "【地面端】"
                setTextColor(Color.parseColor("#E67E22"))
                textSize = 12f
            }
            val groundScrollView = ScrollView(context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            }
            groundTextView = TextView(context).apply {
                text = "等待数据..."
                setTextColor(Color.GREEN)
                textSize = 11f
            }
            groundScrollView.addView(groundTextView)
            groundLayout.addView(groundTitle)
            groundLayout.addView(groundScrollView)

            dualColumnLayout.addView(skyLayout)
            dualColumnLayout.addView(divider)
            dualColumnLayout.addView(groundLayout)
            innerLayout.addView(dualColumnLayout)
            
            rootFrame.addView(innerLayout)

            // 右下角添加专用于拖动改变大小的触摸块 (30dp x 30dp)
            val resizeHandle = View(context).apply {
                id = R.id.btn_minimize + 99 // 设定独立ID区分
                layoutParams = FrameLayout.LayoutParams(dp2px(context, 30f), dp2px(context, 30f)).apply {
                    gravity = Gravity.BOTTOM or Gravity.RIGHT
                }
                setBackgroundColor(Color.TRANSPARENT) // 透明块，不遮挡视觉
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
                params.height = dp2px(context, 300f)
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

    fun updateData(skyData: String, groundData: String) {
        if (skyData.isNotEmpty()) skyTextView.text = skyData
        if (groundData.isNotEmpty()) groundTextView.text = groundData
    }

    private fun setupDragAndResizeListeners(context: Context) {
        // 1. 拖动悬浮窗位置的监听器 (绑定在图标和面板主体)
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

        // 2. 拖动右下角改变大小的监听器
        val resizeHandle = expandedPanelView.findViewById<View>(R.id.btn_minimize + 99)
        resizeHandle?.setOnTouchListener { _, event ->
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
                    
                    // 限制最小尺寸，防止缩成 0 像素无法拉回
                    val newWidth = (initialWidth + deltaX).coerceAtLeast(dp2px(context, 240f))
                    val newHeight = (initialHeight + deltaY).coerceAtLeast(dp2px(context, 200f))
                    
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
