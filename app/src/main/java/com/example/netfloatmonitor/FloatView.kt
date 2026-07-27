package com.example.netfloatmonitor

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
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

    init {
        // 显式方法调用，彻底解决 Val cannot be reassigned 编译冲突
        this.setOrientation(LinearLayout.HORIZONTAL)
        this.setPadding(8, 8, 8, 8)

        // 180透明度黑色背景与圆角设置
        val bg = GradientDrawable()
        bg.setColor(Color.argb(180, 0, 0, 0))
        bg.cornerRadius = 10f
        this.setBackground(bg)

        // 初始化子布局方向
        airLayout.setOrientation(LinearLayout.VERTICAL)
        gndLayout.setOrientation(LinearLayout.VERTICAL)

        // 挂载 AIR 和 GND 双面板
        addView(createPanel("AIR", airLayout))
        addView(createPanel("GND", gndLayout))

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
        // 显式显式显式调用，防止 layoutParams 隐式命名冲突
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
                        // 缺省时默认归属天空端
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
