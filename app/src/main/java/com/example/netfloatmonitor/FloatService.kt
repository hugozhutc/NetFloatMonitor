package com.example.netfloatmonitor


import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.os.Build
import android.view.WindowManager
import androidx.core.app.NotificationCompat



class FloatService : Service() {



    private var floatView: FloatView? = null

    private var receiver: UdpReceiver? = null


    private lateinit var logger: LogManager





    override fun onCreate() {

        super.onCreate()


        logger =
            LogManager(this)



        createNotificationChannel()



        startForeground(
            1001,
            createNotification()
        )

    }







    override fun onStartCommand(

        intent: Intent?,

        flags:Int,

        startId:Int

    ):Int {



        val port =

            intent?.getIntExtra(

                "PORT",

                16789

            )
            ?:16789




        showFloatWindow()



        startUdpReceive(

            port

        )



        return START_STICKY

    }








    private fun startUdpReceive(

        port:Int

    ){



        receiver?.stop()



        receiver =

            UdpReceiver(

                port

            ){ data ->



                try {



                    //保存原始JSON

                    logger.save(

                        data

                    )





                    //解析JSON

                    val status =

                        JsonParser.parse(

                            data

                        )





                    //刷新悬浮窗

                    floatView?.updateStatus(

                        status

                    )



                }

                catch(e:Exception){

    logger.save(
        "JSON ERROR:${e.message}"
    )

}



            }





        receiver?.start()



    }










    private fun showFloatWindow(){



        if(floatView != null)

            return



        val wm =

    getSystemService(
        WINDOW_SERVICE
    ) as WindowManager



val params =

    WindowManager.LayoutParams()



params.width =

    WindowManager.LayoutParams.WRAP_CONTENT


params.height =

    WindowManager.LayoutParams.WRAP_CONTENT



params.type =

    if(Build.VERSION.SDK_INT >= 26)

        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

    else

        WindowManager.LayoutParams.TYPE_PHONE



params.flags =

    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE



params.format =

    PixelFormat.TRANSLUCENT




floatView =

    FloatView(

        this,

        wm,

        params

    )




         as WindowManager





        val params =

            WindowManager.LayoutParams()



        params.width =

            WindowManager.LayoutParams.WRAP_CONTENT



        params.height =

            WindowManager.LayoutParams.WRAP_CONTENT





        params.type =

            if(Build.VERSION.SDK_INT >= 26)

                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

            else

                WindowManager.LayoutParams.TYPE_PHONE





        params.flags =

            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE





        params.format =

            PixelFormat.TRANSLUCENT





        params.x = 50

        params.y = 200





        wm.addView(

            floatView,

            params

        )



    }









    override fun onDestroy(){


        super.onDestroy()



        receiver?.stop()



        if(floatView != null){


            val wm =

                getSystemService(

                    WINDOW_SERVICE

                ) as WindowManager



            wm.removeView(

                floatView

            )



            floatView=null


        }


    }









    override fun onBind(

        intent:Intent?

    ):IBinder? {


        return null

    }









    private fun createNotificationChannel(){



        if(Build.VERSION.SDK_INT >=26){


            val channel =

                NotificationChannel(

                    "net_monitor",

                    "NetFloat Monitor",

                    NotificationManager.IMPORTANCE_LOW

                )



            val manager =

                getSystemService(

                    NotificationManager::class.java

                )


            manager.createNotificationChannel(

                channel

            )


        }


    }









    private fun createNotification():Notification{


        return NotificationCompat.Builder(

            this,

            "net_monitor"

        )

            .setContentTitle(

                "NetFloat Monitor"

            )

            .setContentText(

                "UDP 16789监听中"

            )

            .setSmallIcon(

                android.R.drawable.ic_menu_info_details

            )

            .build()


    }




}
