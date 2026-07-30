package com.network.monitor // 请根据你的项目实际 package 调整

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.util.Collections
import kotlin.math.max
import kotlin.math.min

/**
 * 核心配置：定义左侧文本色彩与右侧图表曲线颜色池的精准映射
 * 支持 2.4G(a) 和 5.8G(g) 多频点动态拆解
 */
object ColorPool {
    val noiseColors = arrayOf(
        Color.parseColor("#FF5252"), // 鲜红
        Color.parseColor("#FFD700"), // 金黄
        Color.parseColor("#00E676"), // 翠绿
        Color.parseColor("#00B0FF"), // 天蓝
        Color.parseColor("#D500F9"), // 魅紫
        Color.parseColor("#FF9100")  // 橙色
    )

    fun getColorForIndex(index: Int): Int {
        return noiseColors[index % noiseColors.size]
    }
}

/**
 * 悬浮窗主控制面板
 */
class FloatMonitorWindow(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val layoutParams = WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        gravity = Gravity.TOP or Gravity.START
        x = 100
        y = 100
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
    }

    // 主体根布局
    private val rootLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        setBackgroundColor(Color.parseColor("#AA000000")) // 70% 透明黑
        setPadding(16, 16, 16, 16)
    }

    // 左侧 Telemetry 数据面板 (可滚动)
    private val leftScrollView = ScrollView(context).apply {
        layoutParams = LinearLayout.LayoutParams(400, 500) // 固定初始宽高
        isHorizontalScrollBarEnabled = false
        isVerticalScrollBarEnabled = true
    }

    private val leftContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }

    // 右侧图表区域 (包含波形图与底噪图)
    private val rightContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(600, 500)
    }

    private val waveformView = WaveformView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
    }

    private val noiseFloorChartView = NoiseFloorChartView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
    }

    // 右下角缩放手柄
    private val resizeHandle = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(30, 30).apply {
            gravity = Gravity.END or Gravity.BOTTOM
        }
        setBackgroundColor(Color.parseColor("#55FFFFFF")) // 半透明白
    }

    // 状态管理与缓存池
    private var isCollapsed = false
    private val textViewsCache = HashMap<String, TextView>()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var initialWidth = 1000
    private var initialHeight = 500
    private var isDragging = false

    init {
        leftScrollView.addView(leftContainer)
        
        // 构建右侧组合视图
        val chartLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            addView(waveformView)
            addView(noiseFloorChartView)
        }

        rootLayout.addView(leftScrollView)
        rootLayout.addView(chartLayout)
        rootLayout.addView(resizeHandle)

        setupTouchEvents()
    }

    fun show() {
        if (rootLayout.parent == null) {
            windowManager.addView(rootLayout, layoutParams)
        }
    }

    fun dismiss() {
        if (rootLayout.parent != null) {
            windowManager.removeView(rootLayout)
        }
        textViewsCache.clear()
    }

    /**
     * 核心数据接收入口：解析 JSON 并动态分流
     */
    fun onDataReceived(jsonStr: String) {
        mainHandler.post {
            try {
                val json = JSONObject(jsonStr)
                
                // 1. 刷新右侧通用波形图 (假设字段为 rssi 或 snr)
                if (json.has("rssi")) {
                    waveformView.addValue(json.getDouble("rssi").toFloat())
                }

                // 2. 动态解析多路底噪并精准绑定色彩
                updateJsonDynamic(leftContainer, textViewsCache, json)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 动态展平 JSON 并处理底噪多频点拆解
     */
    private fun updateJsonDynamic(
        targetLayout: LinearLayout,
        targetMap: HashMap<String, TextView>,
        jsonObject: JSONObject
    ) {
        val keys = jsonObject.keys()
        
        // 防御性检查：如果容器内部组件数量异常，执行强力清空，防止折叠切换时内存泄漏或视图重叠
        if (targetLayout.childCount > 0 && targetMap.isEmpty()) {
            targetLayout.removeAllViews()
        }

        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject.get(key)

            if (value is JSONObject) {
                // 递归处理嵌套对象
                updateJsonDynamic(targetLayout, targetMap, value)
            } else {
                // 处理底噪多频点数据拆解逻辑
                if (key == "noiseFloor_a" || key == "noiseFloor_g") {
                    val rawString = value.toString() // 格式如 "[-95, -98, -92]"
                    val cleanStr = rawString.replace("[", "").replace("]", "").replace(" ", "")
                    if (TextUtils.isEmpty(cleanStr)) continue

                    val noiseValues = cleanStr.split(",")
                    val chartValues = FloatArray(noiseValues.size)

                    // 清理不再需要的旧动态组件（例如频点数量发生改变时）
                    val currentSubKeys = noiseValues.indices.map { "${key}_$it" }
                    val iterator = targetMap.entries.iterator()
                    while (iterator.hasNext()) {
                        val entry = iterator.next()
                        if (entry.key.startsWith("${key}_") && !currentSubKeys.contains(entry.key)) {
                            targetLayout.removeView(entry.value)
                            iterator.remove()
                        }
                    }

                    // 遍历渲染各个频点
                    for (i in noiseValues.indices) {
                        val subKey = "${key}_$i"
                        val noiseVal = noiseValues[i].toFloatOrNull() ?: 0f
                        chartValues[i] = noiseVal

                        val freqLabel = if (key == "noiseFloor_a") "2.4G 频点$i" else "5.8G 频点$i"
                        val displayText = "$freqLabel: ${noiseVal}dBm"
                        val channelColor = ColorPool.getColorForIndex(i)

                        val cachedTv = targetMap[subKey]
                        if (cachedTv != null) {
                            // 防御性校验：确保缓存的 View 确实还在布局中，不在则重新添加
                            if (cachedTv.parent == null) {
                                targetLayout.addView(cachedTv)
                            }
                            cachedTv.text = displayText
                            cachedTv.setTextColor(channelColor)
                        } else {
                            val tv = TextView(context).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    topMargin = 4
                                    bottomMargin = 4
                                }
                                text = displayText
                                setTextColor(channelColor)
                                textSize = 13f
                            }
                            targetLayout.addView(tv)
                            targetMap[subKey] = tv
                        }
                    }

                    // 同步将解析好的多频点数组推送到右侧底噪图表进行绘制与图例显示
                    noiseFloorChartView.updateNoiseData(key, chartValues)

                } else {
                    // 普通文本字段展示逻辑
                    val displayText = "$key: $value"
                    val cachedTv = targetMap[key]
                    if (cachedTv != null) {
                        if (cachedTv.parent == null) targetLayout.addView(cachedTv)
                        cachedTv.text = displayText
                    } else {
                        val tv = TextView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                Double.NaN.toInt() // WRAP_CONTENT
                            )
                            text = displayText
                            setTextColor(Color.WHITE)
                            textSize = 13f
                        }
                        targetLayout.addView(tv)
                        targetMap[key] = tv
                    }
                }
            }
        }
    }

    /**
     * 移动、双击折叠以及右下角双向拉伸拖拽手势交互实现
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchEvents() {
        // 双击与移动手势监听
        var lastTouchTime = 0L
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        rootLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchTime = System.currentTimeMillis()
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (dx * dx + dy * dy > 25) {
                        isDragging = true
                        layoutParams.x = initialX + dx
                        layoutParams.y = initialY + dy
                        windowManager.updateViewLayout(rootLayout, layoutParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging && (System.currentTimeMillis() - lastTouchTime < 300)) {
                        // 触发双击折叠/展开动画
                        performToggle()
                    }
                    true
                }
                else -> false
            }
        }

        // 右下角拉伸缩放手柄逻辑
        var startW = 0
        var startH = 0
        resizeHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startW = rootLayout.width
                    startH = rootLayout.height
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dw = (event.rawX - initialTouchX).toInt()
                    val dh = (event.rawY - initialTouchY).toInt()
                    
                    val targetW = max(600, startW + dw)
                    val targetH = max(300, startH + dh)

                    // 动态分配两侧的伸缩权重
                    leftScrollView.layoutParams.width = (targetW * 0.4f).toInt()
                    leftScrollView.layoutParams.height = targetH - 40
                    
                    val rightLayout = rootLayout.getChildAt(1) as? LinearLayout
                    rightLayout?.layoutParams?.width = (targetW * 0.6f).toInt()
                    rightLayout?.layoutParams?.height = targetH - 40

                    rootLayout.requestLayout()
                    true
                }
                else -> false
            }
        }
    }

    /**
     * 折叠与展开动效
     */
    private fun performToggle() {
        isCollapsed = !isCollapsed
        val startWidth = rootLayout.width
        val endWidth = if (isCollapsed) 450 else initialWidth

        ValueAnimator.ofInt(startWidth, endWidth).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val valAnim = animator.animatedValue as Int
                leftScrollView.layoutParams.width = if (isCollapsed) valAnim - 50 else (valAnim * 0.4f).toInt()
                val rightLayout = rootLayout.getChildAt(1) as? LinearLayout
                rightLayout?.visibility = if (isCollapsed) View.GONE else View.VISIBLE
                rootLayout.requestLayout()
            }
            start()
        }
        if (!isCollapsed) {
            initialWidth = rootLayout.width
            initialHeight = rootLayout.height
        }
    }
}

/**
 * 通用网格波形历史图表 (基类视图)
 */
open class BaseChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    protected val gridPaint = Paint(Paint.ANTI_ALIAS_ALIAS_FLAG).apply {
        color = Color.parseColor("#22FFFFFF") // 细微白网格线
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    protected val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = 24f
    }

    protected val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    protected var chartLeft = 0f
    protected var chartTop = 0f
    protected var chartRight = 0f
    protected var chartBottom = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        chartLeft = paddingLeft + 20f
        chartTop = paddingTop + 45f // 为顶部标题和动态图理解析腾出安全空间
        chartRight = w - paddingRight - 20f
        chartBottom = h - paddingBottom - 20f
    }

    /**
     * 绘制标准背景网格与外边框 (加入抗锯齿与精准像素校准)
     */
    protected fun drawBackgroundGrid(canvas: Canvas) {
        canvas.drawRect(chartLeft, chartTop, chartRight, chartBottom, borderPaint)
        
        // 纵向网格线线数
        val cols = 6
        val widthStep = (chartRight - chartLeft) / cols
        for (i in 1 until cols) {
            val x = chartLeft + i * widthStep
            canvas.drawLine(x, chartTop, x, chartBottom, gridPaint)
        }

        // 横向网格线线数
        val rows = 4
        val heightStep = (chartBottom - chartTop) / rows
        for (i in 1 until rows) {
            // 向下取整像素对齐，防止高分屏上线条虚化
            val y = Math.floor((chartTop + i * heightStep).toDouble()).toFloat()
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
        }
    }
}

/**
 * 视图 1：通用信号强度历史走势波形图 (单线条)
 */
class WaveformView(context: Context) : BaseChartView(context) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val dataPoints = Collections.synchronizedList(ArrayList<Float>())
    private val maxDataCount = 50

    fun addValue(value: Float) {
        dataPoints.add(value)
        if (dataPoints.size > maxDataCount) {
            dataPoints.removeAt(0)
        }
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBackgroundGrid(canvas)
        
        canvas.drawText("[SIGNAL RSSI TIMELINE]", chartLeft + 10f, chartTop - 15f, textPaint)

        if (dataPoints.isEmpty()) return

        val minVal = -120f
        val maxVal = -30f
        val valRange = maxVal - minVal
        val chartHeight = chartBottom - chartTop
        val chartWidth = chartRight - chartLeft
        val stepX = chartWidth / (maxDataCount - 1)

        var lastX = 0f
        var lastY = 0f

        synchronized(dataPoints) {
            for (i in dataPoints.indices) {
                val rawVal = dataPoints[i]
                val clampedVal = min(maxVal, max(minVal, rawVal))
                val ratio = (clampedVal - minVal) / valRange
                
                val currentX = chartLeft + i * stepX
                val currentY = chartBottom - (ratio * chartHeight)

                if (i > 0) {
                    canvas.drawLine(lastX, lastY, currentX, currentY, linePaint)
                }
                lastX = currentX
                lastY = currentY
            }
        }
    }
}

/**
 * 视图 2：多路无线底噪柱状图 (精准映射左侧色彩池，集成智能防重叠换行图例)
 */
class NoiseFloorChartView(context: Context) : BaseChartView(context) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val legendIndicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // 缓存最新解析获得的两条主频段多路数据
    private var currentDataA = FloatArray(0)
    private var currentDataG = FloatArray(0)

    fun updateNoiseData(bandKey: String, values: FloatArray) {
        if (bandKey == "noiseFloor_a") {
            currentDataA = values
        } else if (bandKey == "noiseFloor_g") {
            currentDataG = values
        }
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBackgroundGrid(canvas)

        val mainTitle = "[AIR NOISE FLOORS]"
        canvas.drawText(mainTitle, chartLeft + 10f, chartTop - 18f, textPaint)

        // 动态计算主标题文字宽度，建立图例绘制的绝对左边界防御线
        val titleWidth = textPaint.measureText(mainTitle)
        val legendLeftBarrier = chartLeft + 10f + titleWidth + 30f

        // 合并数据源进行柱状图渲染与动态彩色图例解算
        val totalBarsCount = currentDataA.size + currentDataG.size
        if (totalBarsCount == 0) return

        val minVal = -120f
        val maxVal = -70f
        val valRange = maxVal - minVal
        val chartHeight = chartBottom - chartTop
        val chartWidth = chartRight - chartLeft

        val barSpacing = 16f
        val totalSpacing = barSpacing * (totalBarsCount + 1)
        val singleBarWidth = (chartWidth - totalSpacing) / totalBarsCount

        // ----------------- 核心加固：智能防重叠、支持自动换行的图例绘制算法 -----------------
        var currentLegendX = chartRight - 10f // 从右侧边界开始向左逆向排布
        var currentLegendY = chartTop - 18f    // 初始 Y 轴高度与标题对齐
        val legendRowHeight = 28f              // 换行后的行高增量

        // 统一提取图例渲染 lambda 表达式
        val drawLegendItem: (String, Int) -> Unit = { label, index ->
            val indicatorColor = ColorPool.getColorForIndex(index)
            legendIndicatorPaint.color = indicatorColor
            
            val itemTextWidth = textPaint.measureText(label)
            val totalItemWidth = 20f + 6f + itemTextWidth // [彩色方块] + 间距 + 文本宽度
            
            // 边界检查：如果当前行向左排布会侵入左侧主标题的领地，执行自动换行
            if (currentLegendX - totalItemWidth < legendLeftBarrier) {
                currentLegendX = chartRight - 10f // X 坐标重置回右侧起点
                currentLegendY += legendRowHeight // Y 坐标向下递增一行
                
                // 纵向极端越界保护：如果换行太多侵入了图表网格内部，则停止后续绘制，防止视觉灾难
                if (currentLegendY > chartBottom) {
                    return@drawLegendItem
                }
            }

            // 执行图例色块与文本的绘制
            currentLegendX -= totalItemWidth
            val rectF = RectF(currentLegendX, currentLegendY - 16f, currentLegendX + 18f, currentLegendY + 2f)
            canvas.drawRect(rectF, legendIndicatorPaint)
            canvas.drawText(label, currentLegendX + 24f, currentLegendY, textPaint)
            
            // 留出图例项之间的横向小间距
            currentLegendX -= 14f
        }

        // 依次轮询绘制 2.4G(a) 与 5.8G(g) 的动态彩色图例
        for (i in currentDataA.indices) {
            drawLegendItem("2.4G-$i", i)
        }
        for (i in currentDataG.indices) {
            drawLegendItem("5.8G-$i", i)
        }
        // ---------------------------------------------------------------------------------

        // 接下来进行下方柱状图柱体的渲染
        var barIndex = 0

        // 绘制 2.4G 频段柱体
        for (i in currentDataA.indices) {
            val noiseVal = currentDataA[i]
            drawSingleBar(canvas, noiseVal, minVal, maxVal, valRange, chartHeight, barIndex, singleBarWidth, barSpacing, i)
            barIndex++
        }

        // 绘制 5.8G 频段柱体
        for (i in currentDataG.indices) {
            val noiseVal = currentDataG[i]
            drawSingleBar(canvas, noiseVal, minVal, maxVal, valRange, chartHeight, barIndex, singleBarWidth, barSpacing, i)
            barIndex++
        }
    }

    /**
     * 精准绘制单个柱状图柱体，颜色与左侧面板及上方图例完美契合
     */
    private fun drawSingleBar(
        canvas: Canvas, value: Float, minVal: Float, maxVal: Float, valRange: Float,
        chartHeight: Float, barIndex: Int, barWidth: Float, spacing: Float, poolIndex: Int
    ) {
        val clampedVal = min(maxVal, max(minVal, value))
        val ratio = (clampedVal - minVal) / valRange
        
        val left = chartLeft + spacing + barIndex * (barWidth + spacing)
        val right = left + barWidth
        val top = chartBottom - (ratio * chartHeight)
        val bottom = chartBottom

        // 从颜色池精准提取对应的颜色
        barPaint.color = ColorPool.getColorForIndex(poolIndex)
        canvas.drawRect(left, top, right, bottom, barPaint)
    }
}
