package com.example.netfloatmonitor


import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.*
import android.widget.TextView
import android.widget.LinearLayout
import android.graphics.drawable.GradientDrawable



class FloatView(

    context: Context,

    private val windowManager: WindowManager,

    private val params: WindowManager.LayoutParams


) : LinearLayout(context) {



    private val textView: TextView



    private var lastX = 0f
    private var lastY = 0f

    private var startWidth = 0
    private var startHeight = 0



    init {


        orientation = VERTICAL


        setPadding(
            8,
            6,
            8,
            6
        )


        val bg = GradientDrawable()

        bg.setColor(
            Color.argb(
                170,
                0,
                0,
                0
            )
        )

        bg.cornerRadius = 8f


        background = bg




        textView = TextView(context)



        textView.setTextColor(
            Color.WHITE
        )


        textView.textSize = 11f



        textView.setLineSpacing(
            0f,
            0.85f
        )



        addView(

            textView,

            LayoutParams(

                360,

                500

            )

        )



        //拖动悬浮窗

        setOnTouchListener(

            object: OnTouchListener {



                var downX=0f
                var downY=0f

                var mode = 0



                override fun onTouch(

                    v:View?,

                    event:MotionEvent

                ):Boolean {



                    when(event.action){


                        MotionEvent.ACTION_DOWN->{


                            downX =
                                event.rawX


                            downY =
                                event.rawY



                            mode =

                                if(

                                    event.x >

                                    width-50

                                )

                                    1

                                else

                                    0



                            lastX =
                                event.rawX


                            lastY =
                                event.rawY



                            startWidth =
                                width


                            startHeight =
                                height



                        }



                        MotionEvent.ACTION_MOVE->{



                            if(mode==0){


                                params.x +=

                                    (
                                        event.rawX-downX
                                    ).toInt()



                                params.y +=

                                    (
                                        event.rawY-downY
                                    ).toInt()



                                windowManager.updateViewLayout(

                                    this@FloatView,

                                    params

                                )


                                downX =
                                    event.rawX

                                downY =
                                    event.rawY



                            }
                            else{


                                val w =

                                    startWidth +

                                    (
                                        event.rawX-lastX
                                    ).toInt()



                                val h =

                                    startHeight +

                                    (
                                        event.rawY-lastY
                                    ).toInt()



                                params.width =
                                    w.coerceAtLeast(200)


                                params.height =
                                    h.coerceAtLeast(250)



                                windowManager.updateViewLayout(

                                    this@FloatView,

                                    params

                                )


                            }



                        }



                    }


                    return true

                }



            }


        )


    }





    fun updateStatus(

        status: LinkStatus

    ){



        val airNoise =
            formatNoise(
                status.airNoise
            )


        val gndNoise =
            formatNoise(
                status.gndNoise
            )




        textView.text = """


          AIR                 GND


RSSI1  ${status.airRssi1}       RSSI1  ${status.gndRssi1}

RSSI2  ${status.airRssi2}       RSSI2  ${status.gndRssi2}


SNR    ${status.airSnr}dB       SNR    ${status.gndSnr}dB


PASS   ${status.airPass}        PASS   ${status.gndPass}


FAIL   ${status.airFailed}      FAIL   ${status.gndFailed}


ANT    ${status.airAnt}         ANT    ${status.gndAnt}



FREQ   ${status.freq}


MCS    ${status.mcs}


POWER  ${status.power}


DIST   ${status.distance} m


RATE   TX:${status.txRate}

       RX:${status.rxRate}



$airNoise


$gndNoise


        """.trimIndent()



    }




    private fun formatNoise(

        data:Array<String>

    ):String{


        if(data.isEmpty())

            return ""



        val sb =
            StringBuilder()



        data.forEachIndexed{

                index,
                value ->



            val freq =

                2412 +

                index*8



            sb.append(

                "[ ] $freq $value\n"

            )


        }



        return sb.toString()

    }



}
