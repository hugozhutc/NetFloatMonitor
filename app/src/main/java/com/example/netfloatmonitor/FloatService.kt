package com.example.netfloatmonitor


import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat




class FloatService : Service(){



    private lateinit var wm:WindowManager


    private lateinit var floatView:FloatView


    private lateinit var params:
            WindowManager.LayoutParams



    private var receiver:
            UdpReceiver? = null



    private lateinit var logger:
            LogManager






    override fun onCreate(){


        super.onCreate()



        createNotification()



        logger =
            LogManager(this)



        wm =
            getSystemService(
                Context.WINDOW_SERVICE
            ) as WindowManager



        createFloat()



    }






    private fun createFloat(){



        params =
            WindowManager.LayoutParams()



        params.width =
            600



        params.height =
            800



        params.x =
            100


        params.y =
            200



        params.gravity =
            Gravity.TOP or Gravity.LEFT



        params.format =
            PixelFormat.TRANSLUCENT



        params.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE




        params.type =

            if(Build.VERSION.SDK_INT >= 26)

                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

            else

                WindowManager.LayoutParams.TYPE_PHONE







        floatView =
            FloatView(
                this,
                wm,
                params
            )



        wm.addView(
            floatView,
            params
        )


    }








    override fun onStartCommand(
        intent:Intent?,
        flags:Int,
        startId:Int
    ):Int {



        val ip =

            intent?.getStringExtra(
                "IP"
            )
            ?: ""



        val port =

            intent?.getIntExtra(
                "PORT",
                14550
            )
            ?:14550




        startReceive(
            ip,
            port
        )



        return START_STICKY

    }







    private fun startReceive(

        ip:String,

        port:Int

    ){



        receiver?.stop()



        receiver =

            UdpReceiver(

                ip,

                port

            ){data->




                try{


                    logger.save(data)



                    val status =

                        JsonParser.parse(
                            data
                        )



                    floatView.updateStatus(
                        status
                    )



                }

                catch(e:Exception){



                    floatView.updateText(
                        data
                    )


                }



            }





        receiver?.start()


    }







    private fun createNotification(){



        val channelId =
            "netfloat"



        if(Build.VERSION.SDK_INT >=26){


            val channel =

                NotificationChannel(

                    channelId,

                    "NetFloat Monitor",

                    NotificationManager.IMPORTANCE_LOW

                )


            getSystemService(
                NotificationManager::class.java
            )
            .createNotificationChannel(
                channel
            )


        }




        val notification =

            NotificationCompat.Builder(
                this,
                channelId
            )

            .setContentTitle(
                "NetFloat运行中"
            )

            .setContentText(
                "UDP Link Monitor"
            )

            .setSmallIcon(
                android.R.drawable.ic_menu_info_details
            )

            .build()



        startForeground(
            1,
            notification
        )


    }








    override fun onDestroy(){



        receiver?.stop()



        try{

            wm.removeView(
                floatView
            )

        }catch(e:Exception){}




        super.onDestroy()


    }






    override fun onBind(
        intent:Intent?
    ):IBinder?{


        return null

    }


}
