package com.example.netfloatmonitor


import android.content.Context
import android.graphics.*
import android.view.*
import android.widget.TextView
import kotlin.math.max



class FloatView(

    context: Context,

    private val windowManager: WindowManager,

    private val params: WindowManager.LayoutParams

) : TextView(context) {



    private var downX = 0f
    private var downY = 0f


    private var startWidth = 0
    private var startHeight = 0


    private var resizeMode = false


    private val resizeSize = 80



    init {


        textSize = 14f


        setTextColor(Color.WHITE)


        setBackgroundColor(
            Color.argb(
                200,
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


        gravity = Gravity.START



    }





    /**
     * 更新链路状态
     */
    fun updateStatus(
        status: LinkStatus
    ){


        post {


            text = """

╔ NetFloat Monitor

GROUND

RSSI:
${status.rssiG1} / ${status.rssiG2}

SNR:
${status.snrG} dB

LQI:
${status.lqiG} %

TEMP:
${status.tempG} ℃


----------------


AIR

RSSI:
${status.rssiA1} / ${status.rssiA2}

SNR:
${status.snrA} dB

LQI:
${status.lqiA} %

TEMP:
${status.tempA} ℃


----------------


LINK

MCS:
${status.mcs}

RX:
${status.rxFreq}

TX:
${status.txFreq}

POWER:
${status.power} dBm

DIST:
${status.distance} m


""".trimIndent()


        }


    }







    /**
     * 接收异常信息
     */
    fun updateText(
        msg:String
    ){


        post{

            text = msg

        }


    }







    override fun onTouchEvent(
        event: MotionEvent
    ):Boolean {



        when(event.action){



            MotionEvent.ACTION_DOWN -> {


                downX =
                    event.rawX


                downY =
                    event.rawY



                startWidth =
                    width


                startHeight =
                    height



                resizeMode =

                    event.x >
                    width - resizeSize
                    &&
                    event.y >
                    height - resizeSize



                return true

            }



            MotionEvent.ACTION_MOVE -> {


                val dx =
                    event.rawX-downX


                val dy =
                    event.rawY-downY




                if(resizeMode){


                    params.width =
                        max(
                            300,
                            startWidth+
                            dx.toInt()
                        )


                    params.height =
                        max(
                            200,
                            startHeight+
                            dy.toInt()
                        )



                }
                else{


                    params.x +=
                        dx.toInt()


                    params.y +=
                        dy.toInt()



                    downX =
                        event.rawX


                    downY =
                        event.rawY


                }




                windowManager.updateViewLayout(
                    this,
                    params
                )


                return true

            }



            MotionEvent.ACTION_UP -> {


                resizeMode=false


                return true

            }


        }


        return true

    }






    override fun onDraw(
        canvas: Canvas
    ){


        super.onDraw(canvas)



        val paint =
            Paint()



        paint.color =
            Color.WHITE


        paint.strokeWidth =
            3f



        //右下角缩放标记

        canvas.drawLine(
            width-40f,
            height-10f,
            width-10f,
            height-40f,
            paint
        )


        canvas.drawLine(
            width-25f,
            height-10f,
            width-10f,
            height-25f,
            paint
        )



    }



}
