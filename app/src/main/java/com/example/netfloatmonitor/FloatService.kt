package com.example.netfloatmonitor


import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import androidx.core.app.NotificationCompat



class FloatService:Service(){



    private lateinit var windowManager:WindowManager

    private lateinit var floatView:FloatView


    private var receiver:UdpReceiver?=null



    override fun onCreate(){


        super.onCreate()



        startNotification()



        windowManager =
            getSystemService(
                Context.WINDOW_SERVICE
            ) as WindowManager



        floatView =
            FloatView(this)



        val params =
            WindowManager.LayoutParams()



        params.width =
            450


        params.height =
            300



        params.type =
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY



        params.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE



        params.format =
            PixelFormat.TRANSLUCENT



        params.gravity =
            Gravity.TOP or Gravity.LEFT



        params.x=100

        params.y=200



        windowManager.addView(
            floatView,
            params
        )



    }




    override fun onStartCommand(
        intent:Intent?,
        flags:Int,
        startId:Int
    ):Int{



        val port =
            intent?.getIntExtra(
                "PORT",
                14550
            ) ?:14550




        receiver =
            UdpReceiver(port){data->


                floatView.update(data)


            }



        receiver?.start()



        return START_STICKY

    }




    private fun startNotification(){


        val channel =
            NotificationChannel(
                "monitor",
                "NetFloat",
                NotificationManager.IMPORTANCE_LOW
            )



        val manager =
            getSystemService(
                NotificationManager::class.java
            )



        manager.createNotificationChannel(
            channel
        )



        val notification =
            NotificationCompat.Builder(
                this,
                "monitor"
            )
            .setContentTitle(
                "NetFloat运行中"
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

            windowManager.removeView(
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
