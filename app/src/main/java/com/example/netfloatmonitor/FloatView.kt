package com.example.netfloatmonitor


import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.TextView


class FloatView(
    context: Context
):TextView(context){



    init{


        text =
        "NetFloat Monitor\nWaiting..."



        setTextColor(
            Color.WHITE
        )


        setBackgroundColor(
            Color.argb(
                180,
                0,
                0,
                0
            )
        )


        textSize = 14f


        gravity =
            Gravity.CENTER



        setPadding(
            20,
            20,
            20,
            20
        )

    }



    fun update(data:String){


        post{


            text =
            "NetFloat Monitor\n\nRX:\n$data"

        }


    }


}
