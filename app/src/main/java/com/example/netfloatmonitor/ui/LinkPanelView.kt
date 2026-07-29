package com.example.netfloatmonitor.ui


import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
import android.widget.TextView




class LinkPanelView(

    context: Context,

    private val title:String

) : LinearLayout(context) {



    private val rssi1Text =
        TextView(context)


    private val rssi2Text =
        TextView(context)


    private val snrText =
        TextView(context)


    private val passText =
        TextView(context)


    private val failText =
        TextView(context)


    private val antText =
        TextView(context)





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
            GradientDrawable().apply {

                setColor(
                    Color.rgb(
                        35,
                        38,
                        42
                    )
                )

                cornerRadius =
                    12f

            }






        val titleView =
            TextView(context)



        titleView.text =
            title



        titleView.textSize =
            18f



        titleView.setTextColor(
            Color.GREEN
        )



        addView(
            titleView,
            createParams()
        )




        addText(rssi1Text)

        addText(rssi2Text)

        addText(snrText)

        addText(passText)

        addText(failText)

        addText(antText)



        setDefault()



    }









    private fun addText(
        view:TextView
    ){


        view.textSize =
            14f


        view.setTextColor(
            Color.WHITE
        )


        view.setPadding(
            0,
            4,
            0,
            4
        )



        addView(
            view,
            createParams()
        )


    }









    private fun createParams():

            LayoutParams {



        return LayoutParams(

            LayoutParams.MATCH_PARENT,

            LayoutParams.WRAP_CONTENT

        )

    }









    private fun setDefault(){


        rssi1Text.text =
            "RSSI1 : --"


        rssi2Text.text =
            "RSSI2 : --"


        snrText.text =
            "SNR   : --"


        passText.text =
            "PASS  : --"


        failText.text =
            "FAIL  : --"


        antText.text =
            "ANT   : --"


    }









    fun update(

        rssi1:String,

        rssi2:String,

        snr:String,

        pass:String,

        fail:String,

        ant:String

    ){



        rssi1Text.text =
            "RSSI1 : $rssi1"



        rssi2Text.text =
            "RSSI2 : $rssi2"



        snrText.text =
            "SNR   : $snr"



        passText.text =
            "PASS  : $pass"



        failText.text =
            "FAIL  : $fail"



        antText.text =
            "ANT   : $ant"



    }


}
