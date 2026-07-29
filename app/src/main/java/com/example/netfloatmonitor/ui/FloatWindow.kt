package com.example.netfloatmonitor.ui


import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView


import com.example.netfloatmonitor.data.LinkStatus




class FloatWindow(

    context: Context,

    private val windowManager: WindowManager,

    private val params: WindowManager.LayoutParams


) : LinearLayout(context) {



    private val airPanel =
        LinkPanelView(
            context,
            "AIR"
        )


    private val gndPanel =
        LinkPanelView(
            context,
            "GND"
        )


    private val infoText =
        TextView(context)



    private var lastX = 0f

    private var lastY = 0f





    init {


        orientation = VERTICAL


        setPadding(
            15,
            15,
            15,
            15
        )


        background =
            GradientDrawable().apply {

                setColor(
                    Color.argb(
                        220,
                        15,
                        18,
                        22
                    )
                )

                cornerRadius = 20f

            }




        val title =
            TextView(context)


        title.text =
            "NetFloat Monitor"


        title.textSize =
            20f


        title.setTextColor(
            Color.CYAN
        )


        addView(
            title,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        )





        infoText.textSize = 14f

        infoText.setTextColor(
            Color.WHITE
        )


        val infoLp =
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )


        infoLp.topMargin = 10


        addView(
            infoText,
            infoLp
        )







        val linkLayout =
            LinearLayout(context)


        linkLayout.orientation =
            HORIZONTAL




        linkLayout.addView(

            airPanel,

            LayoutParams(
                420,
                LayoutParams.WRAP_CONTENT
            )

        )



        linkLayout.addView(

            gndPanel,

            LayoutParams(
                420,
                LayoutParams.WRAP_CONTENT
            )

        )



        val linkLp =
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )


        linkLp.topMargin = 10



        addView(
            linkLayout,
            linkLp
        )




        setOnTouchListener(
            MoveListener()
        )


    }









    fun updateStatus(
        status: LinkStatus
    ) {



        airPanel.update(

            status.airRssi1,

            status.airRssi2,

            status.airSnr,

            status.airPass,

            status.airFailed,

            status.airAnt

        )





        gndPanel.update(

            status.gndRssi1,

            status.gndRssi2,

            status.gndSnr,

            status.gndPass,

            status.gndFailed,

            status.gndAnt

        )





        infoText.text =

            """
IP       : ${status.sourceIp}

FREQ     : ${status.freq}

MCS      : ${status.mcs}

POWER    : ${status.power}

DIST     : ${status.distance}

TX       : ${status.txRate}

RX       : ${status.rxRate}

            """.trimIndent()


    }









    private inner class MoveListener :

        OnTouchListener {



        override fun onTouch(

            v: View?,

            event: MotionEvent?

        ): Boolean {



            when(event?.action){



                MotionEvent.ACTION_DOWN -> {


                    lastX =
                        event.rawX


                    lastY =
                        event.rawY

                    return true

                }



                MotionEvent.ACTION_MOVE -> {



                    params.x +=
                        (
                            event.rawX -
                            lastX
                        ).toInt()



                    params.y +=
                        (
                            event.rawY -
                            lastY
                        ).toInt()




                    lastX =
                        event.rawX


                    lastY =
                        event.rawY




                    windowManager.updateViewLayout(

                        this@FloatWindow,

                        params

                    )



                    return true

                }



            }


            return true

        }


    }


        
}
