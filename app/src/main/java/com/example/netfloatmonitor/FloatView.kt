package com.example.netfloatmonitor


import android.content.Context
import android.graphics.*
import android.view.*
import android.widget.TextView
import kotlin.math.max



class FloatView(
    private val ctx: Context,
    private val windowManager: WindowManager,
    private val params: WindowManager.LayoutParams
) : TextView(ctx) {



    private var lastX = 0f
    private var lastY = 0f


    private var startWidth = 0
    private var startHeight = 0


    private var resizing = false



    private val resizeArea = 60



    init {


        text =
            "NetFloat Monitor\nWaiting..."


        textSize = 14f


        setTextColor(Color.WHITE)


        setBackgroundColor(
            Color.argb(
                190,
                0,
                0,
                0
            )
        )


        setPadding(
            20,
            20,
            20,
            20
        )


        setLayerType(
            View.LAYER_TYPE_SOFTWARE,
            null
        )


    }





    /**
     * 更新显示内容
     */
    fun update(json:String){


        post {


            text =
                json


        }

    }





    override fun onTouchEvent(event: MotionEvent):Boolean {



        when(event.action){



            MotionEvent.ACTION_DOWN -> {


                lastX =
                    event.rawX


                lastY =
                    event.rawY



                startWidth =
                    width


                startHeight =
                    height



                // 判断是否进入缩放区域

                resizing =
                    event.x >
                    width - resizeArea &&
                    event.y >
                    height - resizeArea



                return true

            }




            MotionEvent.ACTION_MOVE -> {



                val dx =
                    event.rawX-lastX


                val dy =
                    event.rawY-lastY



                if(resizing){


                    // 调整大小


                    params.width =
                        max(
                            250,
                            (startWidth+dx).toInt()
                        )


                    params.height =
                        max(
                            150,
                            (startHeight+dy).toInt()
                        )


                }

                else{


                    // 移动窗口


                    params.x +=
                        dx.toInt()


                    params.y +=
                        dy.toInt()



                    lastX =
                        event.rawX


                    lastY =
                        event.rawY

                }



                windowManager.updateViewLayout(
                    this,
                    params
                )


                return true

            }




            MotionEvent.ACTION_UP -> {


                resizing=false


                return true

            }


        }


        return true

    }





    override fun onDraw(canvas:Canvas){


        super.onDraw(canvas)



        // 绘制右下角缩放提示

        val paint =
            Paint()



        paint.color =
            Color.WHITE


        paint.strokeWidth =
            3f



        canvas.drawLine(
            width-30f,
            height-10f,
            width-10f,
            height-30f,
            paint
        )


        canvas.drawLine(
            width-20f,
            height-10f,
            width-10f,
            height-20f,
            paint
        )

    }


}
