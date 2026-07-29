
package com.example.netfloatmonitor.ui


import android.content.Context
import android.graphics.*
import android.view.View
import java.util.LinkedList



class ChartView(
    context: Context
) : View(context) {



    private val maxPoints = 100



    private val rssi1 =
        LinkedList<Float>()


    private val rssi2 =
        LinkedList<Float>()


    private val snr =
        LinkedList<Float>()





    private val gridPaint =
        Paint().apply {


            color =
                Color.argb(
                    50,
                    255,
                    255,
                    255
                )


            strokeWidth=1f

        }




    private val rssi1Paint =
        Paint().apply {


            color =
                Color.rgb(
                    52,
                    152,
                    219
                )


            strokeWidth=3f


            style =
                Paint.Style.STROKE


            isAntiAlias=true


        }



    private val rssi2Paint =
        Paint().apply {


            color =
                Color.rgb(
                    46,
                    204,
                    113
                )


            strokeWidth=3f


            style =
                Paint.Style.STROKE


            isAntiAlias=true


        }




    private val snrPaint =
        Paint().apply {


            color =
                Color.rgb(
                    241,
                    196,
                    15
                )


            strokeWidth=3f


            style =
                Paint.Style.STROKE


            isAntiAlias=true

        }







    private val textPaint =
        Paint().apply {


            color =
                Color.WHITE


            textSize=26f


            isAntiAlias=true


        }






    fun addData(
        r1:Float?,
        r2:Float?,
        s:Float?
    ){



        addPoint(
            rssi1,
            r1
        )


        addPoint(
            rssi2,
            r2
        )


        addPoint(
            snr,
            s
        )



        invalidate()


    }







    private fun addPoint(
        list:LinkedList<Float>,
        value:Float?
    ){


        list.addLast(
            value
                ?: list.lastOrNull()
                ?:0f
        )


        if(
            list.size>maxPoints
        ){

            list.removeFirst()

        }


    }








    override fun onDraw(
        canvas:Canvas
    ){


        super.onDraw(canvas)



        val w =
            width.toFloat()


        val h =
            height.toFloat()



        if(
            w<=0 ||
            h<=0
        )
            return







        //背景

        canvas.drawColor(
            Color.TRANSPARENT
        )




        //网格

        for(i in 1..4){


            val y =
                h*i/5f



            canvas.drawLine(
                0f,
                y,
                w,
                y,
                gridPaint
            )

        }







        drawCurve(
            canvas,
            rssi1,
            rssi1Paint,
            120f
        )


        drawCurve(
            canvas,
            rssi2,
            rssi2Paint,
            120f
        )



        drawCurve(
            canvas,
            snr,
            snrPaint,
            50f
        )







        canvas.drawText(
            "RSSI1",
            10f,
            30f,
            rssi1Paint
        )


        canvas.drawText(
            "RSSI2",
            120f,
            30f,
            rssi2Paint
        )


        canvas.drawText(
            "SNR",
            230f,
            30f,
            snrPaint
        )

    }









    private fun drawCurve(
        canvas:Canvas,
        data:List<Float>,
        paint:Paint,
        max:Float
    ){



        if(
            data.size<2
        )
            return




        val step =
            width.toFloat()
            /
            (maxPoints-1)



        for(
            i in 0 until data.size-1
        ){


            val x1 =
                i*step


            val x2 =
                (i+1)*step





            val y1 =
                height -
                (
                 data[i]
                 /
                 max
                 *
                 height
                )



            val y2 =
                height -
                (
                 data[i+1]
                 /
                 max
                 *
                 height
                )



            canvas.drawLine(
                x1,
                y1,
                x2,
                y2,
                paint
            )

        }


    }


}
