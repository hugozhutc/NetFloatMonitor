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



    private var downRawX = 0f
    private var downRawY = 0f


    private var startWidth = 0
    private var startHeight = 0


    private var resize = false


    private val resizeArea = 80



    init {


        textSize = 13f


        typeface =
            Typeface.MONOSPACE


        setTextColor(
            Color.WHITE
        )


        setBackgroundColor(
            Color.argb(
                210,
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


        isClickable = true


    }







    /**
     * 更新链路数据
     */
    fun updateStatus(
        s:LinkStatus
    ){



        post {



            text = """

╔ NetFloat Monitor

========== GROUND ==========

LDPC:
PASS ${s.passG}
FAIL ${s.failG}


RSSI:
ANT1 ${s.rssiG1}
ANT2 ${s.rssiG2}


SNR:
${s.snrG} dB


LQI:
${s.lqiG}% ${quality(s.lqiG)}


TEMP:
RF ${s.tempG} ℃



========== AIR ============


LDPC:
PASS ${s.passA}
FAIL ${s.failA}


RSSI:
ANT1 ${s.rssiA1}
ANT2 ${s.rssiA2}


SNR:
${s.snrA} dB


LQI:
${s.lqiA}% ${quality(s.lqiA)}


TEMP:
RF ${s.tempA} ℃



========== LINK ===========


MCS:
${s.mcs}


RX:
${s.rxFreq}


TX:
${s.txFreq}


POWER:
${s.power} dBm


DIST:
${s.distance} m



========== NETWORK ========


ETH RX:
${s.ethRx} kbps


ETH TX:
${s.ethTx} kbps


============================

""".trimIndent()


        }


    }








    /**
     * 链路质量判断
     */
    private fun quality(
        value:String
    ):String{


        val v =
            value.toIntOrNull()
            ?:0



        return when{


            v >= 80 ->
                "GOOD"


            v >=50 ->
                "NORMAL"


            else ->
                "BAD"


        }


    }








    /**
     * 更新错误信息
     */
    fun updateText(
        msg:String
    ){


        post{


            text =
                msg


        }


    }









    /**
     * 拖动 + 缩放
     */
    override fun onTouchEvent(
        event:MotionEvent
    ):Boolean {



        when(event.action){



            MotionEvent.ACTION_DOWN -> {



                downRawX =
                    event.rawX


                downRawY =
                    event.rawY



                startWidth =
                    width


                startHeight =
                    height



                resize =

                    event.x >
                    width-resizeArea
                    &&
                    event.y >
                    height-resizeArea



                return true

            }




            MotionEvent.ACTION_MOVE -> {



                val dx =
                    event.rawX-downRawX


                val dy =
                    event.rawY-downRawY




                if(resize){



                    params.width =

                        max(
                            300,
                            startWidth+
                            dx.toInt()
                        )


                    params.height =

                        max(
                            300,
                            startHeight+
                            dy.toInt()
                        )



                }

                else{



                    params.x +=
                        dx.toInt()


                    params.y +=
                        dy.toInt()



                    downRawX =
                        event.rawX


                    downRawY =
                        event.rawY


                }




                windowManager.updateViewLayout(

                    this,

                    params

                )



                return true


            }




            MotionEvent.ACTION_UP -> {


                resize=false


                return true


            }


        }



        return true


    }








    /**
     * 绘制缩放角标
     */
    override fun onDraw(
        canvas:Canvas
    ){


        super.onDraw(canvas)



        val paint =
            Paint()



        paint.color =
            Color.WHITE



        paint.strokeWidth =
            3f



        canvas.drawLine(

            width-45f,

            height-10f,

            width-10f,

            height-45f,

            paint

        )



        canvas.drawLine(

            width-30f,

            height-10f,

            width-10f,

            height-30f,

            paint

        )


    }


}
