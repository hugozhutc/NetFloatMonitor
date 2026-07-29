package com.example.netfloatmonitor.ui


import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import java.util.LinkedList



class ChartView(

    context: Context

) : View(context) {



    private val maxPoints = 100



    private val rssi1List =
        LinkedList<Float>()


    private val rssi2List =
        LinkedList<Float>()


    private val snrList =
        LinkedList<Float>()






    private val gridPaint =
        Paint().apply {

            color =
                Color.argb(
                    60,
                    255,
                    255,
                    255
                )

            strokeWidth = 1f

        }






    private val rssi1Paint =
        Paint().apply {

            color = Color.CYAN

            strokeWidth = 3f

            style =
                Paint.Style.STROKE

            isAntiAlias = true

        }






    private val rssi2Paint =
        Paint().apply {

            color = Color.BLUE

            strokeWidth = 3f

            style =
                Paint.Style.STROKE

            isAntiAlias = true

        }






    private val snrPaint =
        Paint().apply {

            color = Color.GREEN

            strokeWidth = 3f

            style =
                Paint.Style.STROKE

            isAntiAlias = true

        }






    private val textPaint =
        Paint().apply {

            color = Color.WHITE

            textSize = 28f

            isAntiAlias = true

        }









    fun addData(

        rssi1: Float,

        rssi2: Float,

        snr: Float

    ){


        addValue(
            rssi1List,
            rssi1
        )


        addValue(
            rssi2List,
            rssi2
        )


        addValue(
            snrList,
            snr
        )


        postInvalidate()

    }









    private fun addValue(

        list: LinkedList<Float>,

        value: Float

    ){


        list.add(value)


        if(list.size > maxPoints){

            list.removeFirst()

        }


    }









    override fun onDraw(

        canvas: Canvas

    ){


        super.onDraw(canvas)



        val w =
            width.toFloat()



        val h =
            height.toFloat()




        if(w <= 0 || h <= 0)

            return






        canvas.drawColor(
            Color.TRANSPARENT
        )







        // 网格

        for(i in 1..4){


            val y =
                h * i / 5f



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

            rssi1List,

            rssi1Paint,

            120f

        )





        drawCurve(

            canvas,

            rssi2List,

            rssi2Paint,

            120f

        )





        drawCurve(

            canvas,

            snrList,

            snrPaint,

            50f

        )








        canvas.drawText(

            "RSSI1",

            10f,

            35f,

            textPaint

        )





        canvas.drawText(

            "RSSI2",

            150f,

            35f,

            textPaint

        )





        canvas.drawText(

            "SNR",

            290f,

            35f,

            textPaint

        )


    }












    private fun drawCurve(

        canvas: Canvas,

        list: List<Float>,

        paint: Paint,

        maxValue: Float

    ){



        if(list.size < 2)

            return






        val step =

            width.toFloat() /
                    (maxPoints - 1)







        for(i in 0 until list.size - 1){






            val x1 =

                i * step






            val x2 =

                (i + 1) * step








            val y1 =

                height.toFloat() -

                        (

                                list[i]
                                        /
                                        maxValue
                                        *
                                        height.toFloat()

                                )








            val y2 =

                height.toFloat() -

                        (

                                list[i + 1]
                                        /
                                        maxValue
                                        *
                                        height.toFloat()

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
