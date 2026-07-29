package com.example.netfloatmonitor.ui


import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*


import com.example.netfloatmonitor.data.LinkStatus



class FloatWindow(
    context: Context,
    private val windowManager: WindowManager,
    private val params: WindowManager.LayoutParams

) : LinearLayout(context) {



    private val airText =
        TextView(context)


    private val gndText =
        TextView(context)


    private val linkText =
        TextView(context)



    private val infoText =
        TextView(context)



    private var downX=0f
    private var downY=0f



    private var expanded=true



    private val root =
        this



    init {


        orientation =
            VERTICAL



        setPadding(
            12,
            10,
            12,
            10
        )



        background =
            createBackground()



        val title =
            TextView(context)



        title.text =
            "NETFLOAT MONITOR"



        title.textSize=16f


        title.setTextColor(
            Color.GREEN
        )


        title.setPadding(
            0,
            0,
            0,
            8
        )


        addView(title)





        val row =
            LinearLayout(context)


        row.orientation =
            HORIZONTAL




        addCard(
            row,
            "AIR",
            airText
        )


        addCard(
            row,
            "GROUND",
            gndText
        )


        addCard(
            row,
            "LINK",
            linkText
        )



        addView(row)





        infoText.textSize=14f


        infoText.setTextColor(
            Color.WHITE
        )


        infoText.setPadding(
            0,
            12,
            0,
            0
        )



        addView(infoText)




        setOnTouchListener {


                _,event ->


            when(event.action){


                MotionEvent.ACTION_DOWN->{


                    downX =
                        event.rawX


                    downY =
                        event.rawY


                }



                MotionEvent.ACTION_MOVE->{


                    params.x +=
                        (
                         event.rawX-downX
                        ).toInt()



                    params.y +=
                        (
                         event.rawY-downY
                        ).toInt()



                    downX =
                        event.rawX


                    downY =
                        event.rawY



                    windowManager
                        .updateViewLayout(
                            this,
                            params
                        )


                }



            }


            true

        }



        setOnClickListener{


            toggle()

        }


    }









    private fun addCard(
        parent:LinearLayout,
        title:String,
        textView:TextView
    ){


        val box =
            LinearLayout(context)



        box.orientation =
            VERTICAL



        box.setPadding(
            15,
            8,
            15,
            8
        )



        val titleView =
            TextView(context)



        titleView.text =
            title


        titleView.textSize=15f


        titleView.setTextColor(
            Color.CYAN
        )



        box.addView(titleView)



        textView.textSize=14f


        textView.setTextColor(
            Color.WHITE
        )



        box.addView(
            textView
        )



        val lp =
            LayoutParams(
                320,
                LayoutParams.WRAP_CONTENT
            )


        lp.setMargins(
            5,
            0,
            5,
            0
        )



        parent.addView(
            box,
            lp
        )


    }









    fun updateStatus(
        status:LinkStatus
    ){


        post {



            airText.text =
                """
RSSI1 ${status.airRssi1}
RSSI2 ${status.airRssi2}
SNR  ${status.airSnr}
PASS ${status.airPass}
ANT  ${status.airAnt}
                """.trimIndent()




            gndText.text =
                """
RSSI1 ${status.gndRssi1}
RSSI2 ${status.gndRssi2}
SNR  ${status.gndSnr}
PASS ${status.gndPass}
ANT  ${status.gndAnt}
                """.trimIndent()




            linkText.text =
                """
质量 ${status.linkQuality}%

MCS ${status.mcs}

${status.freq} MHz
                """.trimIndent()





            infoText.text =
                """
DISTANCE : ${status.distance} m

POWER : ${status.power}

TX : ${status.txRate}

RX : ${status.rxRate}

LOSS : %.2f %%
                """.trimIndent()
                    .format(status.lossRate)



        }


    }









    private fun toggle(){


        expanded=!expanded



        if(expanded){


            params.width=1300

            params.height=600



            visibility=
                VISIBLE


        }else{


            params.width=180

            params.height=80



        }



        windowManager
            .updateViewLayout(
                root,
                params
            )

    }









    private fun createBackground():
            GradientDrawable{


        return GradientDrawable()
            .apply{


                setColor(
                    Color.argb(
                        210,
                        10,
                        10,
                        10
                    )
                )


                cornerRadius =
                    20f

            }

    }


}
