package com.example.netfloatmonitor

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.cardview.widget.CardView

class FloatView(
    context: Context,
    private val windowManager: WindowManager,
    private val params: WindowManager.LayoutParams
) : LinearLayout(context) {

    // 状态控制变量
    private var isExpanded = false
    private var initialX = 0
    private var initialY = 0
    private var touchX = 0f
    private var touchY = 0f

    // 内部视图组件
    private lateinit var miniIconView: CardView
    private lateinit var expandedPanelView: CardView
    private lateinit var jsonTextView: TextView
    private lateinit var minimizeBtn: Button

    init {
        orientation = VERTICAL
        initView(context)
        setupDragListener()
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
            radius = dp2px(context, 12f).toFloat()
            setCardBackgroundColor(Color.parseColor("#1C1C1E"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            
            // 面板内部垂直排列
            val innerLayout = LinearLayout(context).apply {
                orientation = VERTICAL
                val p = dp2px(context, 12f)
                setPadding(p, p, p, p) // 修正：使用原生 setPadding 替代 padding
            }

            // 标题与收起按钮栏
            val titleLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val titleTv = TextView(context).apply {
                text = "网络监控面板"
                setTextColor(Color.WHITE) // 修正：使用 setTextColor
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

            // 数据滚动区域
            val scrollView = ScrollView(context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply {
                    topMargin = dp2px(context, 8f)
                }
            }
            jsonTextView = TextView(context).apply {
                text = "等待网络数据..."
                setTextColor(Color.GREEN) // 修正：使用 setTextColor
                textSize = 12f
            }
            scrollView.addView(jsonTextView)
            innerLayout.addView(scrollView)

            addView(innerLayout)
            visibility = View.GONE
        }

        // 将两个状态的视图都塞入根布局
        addView(miniIconView)
        addView(expandedPanelView)

        // 3. 点击小图标 -> 展开面板
        miniIconView.setOnClickListener {
            if (!isExpanded) {
                isExpanded = true
                miniIconView.visibility = View.GONE
                expandedPanelView.visibility = View.VISIBLE
                
                params.width = dp2px(context, 260f)
                params.height = dp2px(context, 300f)
                windowManager.updateViewLayout(this, params)
            }
        }

        // 4. 点击面板内的收起按钮 -> 折叠回小图标
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

    fun updateJson(data: String) {
        jsonTextView.text = data
    }

    private fun setupDragListener() {
        val touchListener = OnTouchListener { _, event ->
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

        miniIconView.setOnTouchListener(touchListener)
        expandedPanelView.setOnTouchListener(touchListener)
    }

    private fun dp2px(context: Context, dp: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }
}
