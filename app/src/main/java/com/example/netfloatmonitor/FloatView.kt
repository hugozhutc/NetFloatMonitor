package com.example.netfloatmonitor


import android.content.Context
import android.graphics.Color
import android.view.*
import android.widget.*
import android.graphics.drawable.GradientDrawable
import org.json.JSONObject



class FloatView(

    context: Context,

    private val windowManager: WindowManager,

    private val params: WindowManager.LayoutParams

) : LinearLayout(context) {



    private val airLayout = LinearLayout(context)

    private val gndLayout = LinearLayout(context)



    private var startWidth = 0

    private var startHeight = 0

    private var downX = 0f

    private var downY = 0f

    private var resize = false





    init {


        orientation = HORIZONTAL


        setPadding(
            8,
            8,
            8,
            8
        )



        val bg = GradientDrawable()


        bg.setColor(

            Color.argb(

                180,

                0,

                0,

                0

            )

        )


        bg.cornerRadius = 10f



        background = bg




        airLayout.orientation =
            VERTICAL



        gndLayout.orientation =
            VERTICAL






        addView(

            createPanel(

                "AIR",

                airLayout

            )

        )



        addView(

            createPanel(

                "GND",

                gndLayout

            )

        )






        setOnTouchListener(

            object : OnTouchListener {



                override fun onTouch(

                    v: View?,

                    event: MotionEvent

                ): Boolean {



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



                            resize =

                                event.x >

                                width - 50



                        }



                        MotionEvent.ACTION_MOVE -> {



                            if(resize){



                                params.width =

                                    (

                                    startWidth +

                                    event.rawX -

                                    downX

                                    ).toInt()
                                    .coerceAtLeast(300)




                                params.height =

                                    (

                                    startHeight +

                                    event.rawY -

                                    downY

                                    ).toInt()
                                    .coerceAtLeast(200)



                            }
                            else {



                                params.x +=

                                    (

                                    event.rawX -

                                    downX

                                    ).toInt()



                                params.y +=

                                    (

                                    event.rawY -

                                    downY

                                    ).toInt()



                                downX =
                                    event.rawX


                                downY =
                                    event.rawY


                            }



                            windowManager.updateViewLayout(

                                this@FloatView,

                                params

                            )


                        }


                    }



                    return true

                }



            }


        )



    }









    private fun createPanel(

        title:String,

        layout:LinearLayout

    ):View {



        val box =

            LinearLayout(context)



        box.orientation =

            VERTICAL





        val titleView =

            TextView(context)



        titleView.text =

            title



        titleView.textSize =

            14f



        titleView.setTextColor(

            Color.GREEN

        )



        box.addView(

            titleView

        )







        val scroll =

            ScrollView(context)



        scroll.addView(

            layout

        )





        box.addView(

            scroll,

            LinearLayout.LayoutParams(

                300,

                LinearLayout.LayoutParams.MATCH_PARENT

            )

        )




        return box

    }









    /**
     * V2.0 JSON显示接口
     */
    fun updateJson(

        json:String

    ){



        try {



            val obj =

                JSONObject(json)




            airLayout.removeAllViews()

            gndLayout.removeAllViews()





            obj.keys().forEach {



                val key = it


                val value =

                    obj.get(key)
                        .toString()





                when {



                    key.endsWith("_a") -> {


                        addItem(

                            airLayout,

                            key,

                            value

                        )

                    }




                    key.endsWith("_g") -> {


                        addItem(

                            gndLayout,

                            key,

                            value

                        )

                    }




                    else -> {



                        addItem(

                            airLayout,

                            key,

                            value

                        )


                    }



                }



            }





        }

        catch(e:Exception){



            addItem(

                airLayout,

                "JSON_ERROR",

                e.message ?: ""

            )


        }



    }









    private fun addItem(

        layout:LinearLayout,

        key:String,

        value:String

    ){



        val tv =

            TextView(context)




        tv.text =

            "$key : $value"





        tv.textSize =

            12f



        tv.setTextColor(

            Color.WHITE

        )



        tv.setPadding(

            4,

            3,

            4,

            3

        )




        layout.addView(

            tv

        )


    }



}
