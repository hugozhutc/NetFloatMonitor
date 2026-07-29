package com.example.netfloatmonitor.service


import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

import com.example.netfloatmonitor.R
import com.example.netfloatmonitor.data.DataProcessor
import com.example.netfloatmonitor.data.LinkStatus
import com.example.netfloatmonitor.network.UdpReceiver
import com.example.netfloatmonitor.log.LogManager
import com.example.netfloatmonitor.ui.FloatWindow

import java.util.Timer
import java.util.TimerTask



class FloatService : Service(){



    private var floatWindow:FloatWindow? = null


    private var udpReceiver:UdpReceiver? = null



    private lateinit var processor:DataProcessor


    private lateinit var logManager:LogManager



    private var timer:Timer?=null



    private var totalPackets=0


    private var hz=0


    private var lastSecondPackets=0





    override fun onCreate(){

        super.onCreate()



        processor =
            DataProcessor()



        logManager =
            LogManager(this)



        createNotificationChannel()


        startForeground(
            1001,
            createNotification()
        )


        Log.d(
            "FloatService",
            "Service启动"
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
                16789
            )
            ?:16789




        logManager.startNewSession()



        showFloatWindow()



        startUdp(port)



        startStatusTimer()



        return START_NOT_STICKY

    }









    private fun startUdp(port:Int){



        udpReceiver?.stop()



        udpReceiver =
            UdpReceiver(port){json->



                totalPackets++

                lastSecondPackets++



                //保存原始数据

                logManager.save(json)




                val status =
                    processor.process(json)



                status?.let {


                    floatWindow
                        ?.updateStatus(it)



                    sendStatus(it)

                }



            }



        udpReceiver?.start()



    }









    private fun sendStatus(
        status:LinkStatus
    ){



        val intent =
            Intent(
                "com.example.netfloatmonitor.STATUS_UPDATE"
            )


        intent.putExtra(
            "TOTAL_PACKETS",
            totalPackets
        )


        intent.putExtra(
            "HZ",
            hz
        )


        intent.putExtra(
            "QUALITY",
            status.linkQuality
        )


        LocalBroadcastManager
            .getInstance(this)
            .sendBroadcast(intent)



    }









    private fun startStatusTimer(){


        timer?.cancel()


        timer =
            Timer()



        timer?.scheduleAtFixedRate(


            object:TimerTask(){


                override fun run(){


                    hz =
                        lastSecondPackets


                    lastSecondPackets=0


                }


            },


            1000,


            1000


        )

    }









    private fun showFloatWindow(){



        if(floatWindow!=null)
            return



        val wm =
            getSystemService(
                WINDOW_SERVICE
            ) as WindowManager




        val params =
            WindowManager.LayoutParams()



        params.width=1300

        params.height=600



        params.type =
            if(Build.VERSION.SDK_INT>=26)

                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

            else

                WindowManager.LayoutParams.TYPE_PHONE





        params.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE



        params.format =
            PixelFormat.TRANSLUCENT



        params.x=50

        params.y=200





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



        timer?.cancel()


        udpReceiver?.stop()


        logManager.stopSession()



        floatWindow?.let{


            try{


                val wm =
                    getSystemService(
                        WINDOW_SERVICE
                    ) as WindowManager



                wm.removeView(it)


            }catch(e:Exception){


                Log.e(
                    "FloatService",
                    e.message?:""
                )

            }


        }



        floatWindow=null



        val intent =
            Intent(
                "com.example.netfloatmonitor.STATUS_UPDATE"
            )


        intent.putExtra(
            "IS_STOPPED",
            true
        )


        LocalBroadcastManager
            .getInstance(this)
            .sendBroadcast(intent)


    }








    override fun onBind(
        intent:Intent?
    ):IBinder?=null







    private fun createNotificationChannel(){



        if(Build.VERSION.SDK_INT>=26){


            val channel =
                NotificationChannel(
                    "net_monitor",
                    "NetFloat Monitor",
                    NotificationManager.IMPORTANCE_LOW
                )



            getSystemService(
                NotificationManager::class.java
            )
                .createNotificationChannel(channel)

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
