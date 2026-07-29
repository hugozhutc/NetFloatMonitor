package com.example.netfloatmonitor.service


import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.WindowManager

import androidx.core.app.NotificationCompat

import com.example.netfloatmonitor.R
import com.example.netfloatmonitor.JsonParser
import com.example.netfloatmonitor.LogManager
import com.example.netfloatmonitor.UdpReceiver

import com.example.netfloatmonitor.ui.FloatWindow

import com.example.netfloatmonitor.data.LinkStatus



class FloatService : Service() {



    private var udpReceiver: UdpReceiver? = null


    private var floatWindow: FloatWindow? = null


    private lateinit var logManager: LogManager



    private var packetCount = 0



    override fun onCreate() {

        super.onCreate()


        logManager =
            LogManager(this)



        createNotificationChannel()


        startForeground(
            1001,
            createNotification()
        )


        Log.d(
            "FloatService",
            "服务启动"
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





        logManager.startNewSession()



        showFloatWindow()



        startUdp(port)



        return START_NOT_STICKY

    }









    private fun startUdp(
        port:Int
    ){



        udpReceiver?.stop()



        udpReceiver =
            UdpReceiver(
                port
            ){ json ->



                processData(json)


            }



        udpReceiver?.start()



    }









    private fun processData(
        json:String
    ){


        try {


            //1.保存原始数据

            logManager.save(
                json
            )




            //2.JSON转换

            val status:LinkStatus =
                JsonParser.parse(
                    json
                )





            packetCount++





            //3.刷新悬浮窗

            floatWindow?.updateStatus(
                status
            )



        }
        catch(e:Exception){


            Log.e(
                "FloatService",
                "数据处理失败:${e.message}"
            )


        }


    }









    private fun showFloatWindow(){


        if(floatWindow!=null)
            return



        val wm =
            getSystemService(
                WINDOW_SERVICE
            )
                    as WindowManager




        val params =
            WindowManager.LayoutParams()





        params.width =
            1000


        params.height =
            700



        params.x =
            50


        params.y =
            200




        params.type =
            if(Build.VERSION.SDK_INT>=26)

                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

            else

                WindowManager.LayoutParams.TYPE_PHONE




        params.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE



        params.format =
            PixelFormat.TRANSLUCENT





        floatWindow =
            FloatWindow(
                this,
                wm,
                params
            )



        wm.addView(
            floatWindow,
            params
        )


    }









    override fun onDestroy(){

        super.onDestroy()



        udpReceiver?.stop()

        udpReceiver=null




        logManager.stopSession()





        floatWindow?.let{


            try{


                val wm =
                    getSystemService(
                        WINDOW_SERVICE
                    )
                            as WindowManager


                wm.removeView(it)


            }
            catch(e:Exception){



            }


        }



        floatWindow=null



        Log.d(
            "FloatService",
            "服务停止"
        )



    }








    override fun onBind(
        intent:Intent?
    ):IBinder?=null







    private fun createNotificationChannel(){


        if(
            Build.VERSION.SDK_INT>=26
        ){


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
                "UDP链路监控运行中"
            )


            .setSmallIcon(
                android.R.drawable.ic_menu_info_details
            )


            .build()



    }


}
