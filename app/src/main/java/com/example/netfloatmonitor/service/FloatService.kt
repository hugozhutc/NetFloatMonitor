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
import com.example.netfloatmonitor.data.LogManager
import com.example.netfloatmonitor.net.UdpReceiver
import com.example.netfloatmonitor.ui.FloatView


import java.util.Timer
import java.util.TimerTask



class FloatService : Service() {



    private val TAG =
        "FloatService"



    private var floatView:FloatView? =
        null



    private var udpReceiver:UdpReceiver? =
        null



    private lateinit var logManager:LogManager






    private var totalPackets=0



    private var packetsLastSecond=0



    private var currentHz=0



    private var statusTimer:Timer? =
        null






    override fun onCreate(){

        super.onCreate()


        logManager =
            LogManager(
                this
            )


        createNotificationChannel()


        startForeground(
            1001,
            createNotification()
        )



        Log.d(
            TAG,
            "Service创建"
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





        restartMonitor(
            port
        )



        return START_NOT_STICKY


    }









    private fun restartMonitor(
        port:Int
    ){



        stopUdp()



        totalPackets=0

        packetsLastSecond=0

        currentHz=0




        logManager.startNewSession()



        showFloatWindow()



        startUdp(
            port
        )



        startStatusTimer()



        sendStatus()



        Log.d(
            TAG,
            "UDP监听:$port"
        )



    }









    private fun startUdp(
        port:Int
    ){



        udpReceiver =
            UdpReceiver(
                port
            )
            { json, ip ->


                try{


                    totalPackets++

                    packetsLastSecond++



                    //保存原始数据

                    logManager.save(
                        json
                    )




                    floatView?.post{


                        floatView?.updateJson(
                            json
                        )


                    }





                }
                catch(e:Exception){


                    Log.e(
                        TAG,
                        "数据处理异常:${e.message}"
                    )


                }



            }




        udpReceiver?.start()



    }









    private fun startStatusTimer(){


        statusTimer?.cancel()



        statusTimer =
            Timer()



        statusTimer?.scheduleAtFixedRate(


            object:TimerTask(){


                override fun run(){



                    currentHz =
                        packetsLastSecond



                    packetsLastSecond=0




                    sendStatus()



                }



            },


            1000,


            1000



        )


    }









    private fun sendStatus(){



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
            currentHz
        )



        intent.putExtra(
            "FILE",
            logManager.getCurrentFileName()
        )




        LocalBroadcastManager
            .getInstance(this)
            .sendBroadcast(
                intent
            )


    }









    private fun showFloatWindow(){



        if(
            floatView!=null
        )
            return




        val wm =
            getSystemService(
                WINDOW_SERVICE
            )
            as WindowManager





        val params =
            WindowManager.LayoutParams()



        params.width =
            1300



        params.height =
            540




        params.type =
            if(
                Build.VERSION.SDK_INT>=26
            ){

                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

            }
            else{

                WindowManager.LayoutParams.TYPE_PHONE

            }




        params.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE





        params.format =
            PixelFormat.TRANSLUCENT



        params.x=50

        params.y=200






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









    override fun onDestroy(){


        Log.d(
            TAG,
            "Service停止"
        )



        statusTimer?.cancel()

        statusTimer=null




        stopUdp()



        logManager.stopSession()




        sendStopStatus()





        removeFloatWindow()



        super.onDestroy()


    }









    private fun stopUdp(){



        udpReceiver?.stop()


        udpReceiver=null


    }









    private fun removeFloatWindow(){



        if(
            floatView!=null
        ){


            try{


                val wm =
                    getSystemService(
                        WINDOW_SERVICE
                    )
                    as WindowManager



                wm.removeView(
                    floatView
                )


            }
            catch(e:Exception){



                Log.e(
                    TAG,
                    "移除悬浮窗失败:${e.message}"
                )


            }



            floatView=null


        }



    }









    private fun sendStopStatus(){


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
            .sendBroadcast(
                intent
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

            .setOngoing(true)

            .build()


    }



}
