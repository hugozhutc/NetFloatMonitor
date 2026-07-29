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



    private val airPanel =
        LinkPanelView(
            context,
            "AIR"
        )


    private val gndPanel =
        LinkPanelView(
            context,
            "GROUND"
        )


    private val linkPanel =
        LinkPanelView(
            context,
            "LINK"
        )



    private val chartView =
        ChartView(context)



    private var downX=0f
    private var downY=0f



    private var expanded=true



    init {


        orientation =
            VERTICAL



        setPadding(
            15,
            10,
            15,
            10
        )



        background =
            createBackground()





        val title =
            TextView(context)



        title.text =
            "NETFLOAT MONITOR"



        title.textSize =
            18f



        title.setTextColor(
            Color.GREEN
        )



        addView(
            title,
            LayoutParams(
                MATCH_PARENT,
                50
            )
        )






        val panelRow =
            LinearLayout(context)



        panelRow.orientation =
            HORIZONTAL




        panelRow.addView(
            airPanel,
            LayoutParams(
                320,
                WRAP_CONTENT
            )
        )



        panelRow.addView(
            gndPanel,
            LayoutParams(
                320,
                WRAP_CONTENT
            )
        )



        panelRow.addView(
            linkPanel,
            LayoutParams(
                320,
                WRAP_CONTENT
            )
        )



        addView(
            panelRow
        )







        addView(
            chartView,
            LayoutParams(
                MATCH_PARENT,
                300
            )
        )







        setOnTouchListener{

                _,event ->


            when(event.action){


                MotionEvent.ACTION_DOWN->{


                    downX =
                        event.rawX


                    downY =
                        event.rawY


                    true

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

                    true

                }


                else ->
                    true

            }

        }




        setOnClickListener{


            toggle()

        }



    }









    fun updateStatus(
        status:LinkStatus
    ){



        airPanel.updateAir(
            status
        )



        gndPanel.updateGround(
            status
        )



        linkPanel.updateLink(
            status
        )



        chartView.addData(

            status.airRssi1.toFloatOrNull(),

            status.airRssi2.toFloatOrNull(),

            status.airSnr.toFloatOrNull()

        )


    }









    private fun toggle(){


        expanded =
            !expanded



        if(expanded){



            visibility =
                VISIBLE



            params.width =
                1000



            params.height =
                700



        }
        else{



            params.width =
                220



            params.height =
                80



        }



        windowManager
            .updateViewLayout(
                this,
                params
            )


    }









    private fun createBackground():
            GradientDrawable{


        return GradientDrawable()
            .apply{


                setColor(
                    Color.argb(
                        220,
                        15,
                        15,
                        15
                    )
                )


                cornerRadius =
                    25f

            }


    }



}
