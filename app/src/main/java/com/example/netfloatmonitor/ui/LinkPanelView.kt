
package com.example.netfloatmonitor.ui


import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

import com.example.netfloatmonitor.data.LinkStatus



class LinkPanelView(
    context: Context,
    private val title:String
) : LinearLayout(context){



    private val titleView =
        TextView(context)


    private val contentView =
        TextView(context)



    init {


        orientation =
            VERTICAL



        setPadding(
            15,
            12,
            15,
            12
        )



        background =
            createBackground()



        titleView.text =
            title



        titleView.textSize =
            18f



        titleView.setTextColor(
            Color.CYAN
        )



        titleView.gravity =
            Gravity.CENTER



        addView(
            titleView
        )





        contentView.textSize =
            14f



        contentView.setTextColor(
            Color.WHITE
        )


        contentView.setPadding(
            0,
            10,
            0,
            0
        )



        addView(
            contentView
        )


    }









    fun updateAir(
        status:LinkStatus
    ){


        contentView.text =
            """
RSSI1 : ${status.airRssi1}

RSSI2 : ${status.airRssi2}

SNR   : ${status.airSnr}

PASS  : ${status.airPass}

FAIL  : ${status.airFailed}

ANT   : ${status.airAnt}
            """.trimIndent()


    }









    fun updateGround(
        status:LinkStatus
    ){


        contentView.text =
            """
RSSI1 : ${status.gndRssi1}

RSSI2 : ${status.gndRssi2}

SNR   : ${status.gndSnr}

PASS  : ${status.gndPass}

FAIL  : ${status.gndFailed}

ANT   : ${status.gndAnt}
            """.trimIndent()


    }









    fun updateLink(
        status:LinkStatus
    ){


        val qualityColor =
            when{


                status.linkQuality >=80 ->
                    "GOOD"


                status.linkQuality >=50 ->
                    "WARN"


                else ->
                    "BAD"

            }




        contentView.text =
            """
QUALITY : ${status.linkQuality}%


STATUS  : $qualityColor


FREQ    : ${status.freq}


MCS     : ${status.mcs}


POWER   : ${status.power}


DIST    : ${status.distance}


TX      : ${status.txRate}


RX      : ${status.rxRate}


LOSS    : %.2f%%
            """.trimIndent()
                .format(status.lossRate)



    }









    private fun createBackground():
            GradientDrawable{


        return GradientDrawable()
            .apply{


                setColor(
                    Color.argb(
                        120,
                        255,
                        255,
                        255
                    )
                )


                cornerRadius =
                    15f

            }

    }



}
