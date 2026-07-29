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


import com.example.netfloatmonitor.data.JsonParser
import com.example.netfloatmonitor.data.LinkStatus
import com.example.netfloatmonitor.data.LogManager
import com.example.netfloatmonitor.net.UdpReceiver
import com.example.netfloatmonitor.ui.FloatWindow



class FloatService : Service() {


    private val TAG = "FloatService"



    private var udpReceiver:UdpReceiver? = null


    private var floatView:FloatView? = null



    private lateinit var logManager:LogManager



    private var currentStatus =
        LinkStatus()



    private var totalPackets = 0


    private var packetsLastSecond = 0


    private var currentHz = 0



    private var statusTimer:java.util.Timer? = null






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
            TAG,
            "FloatService created"
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




        startMonitor(
            port
        )



        return START_NOT_STICKY

    }









    private fun startMonitor(
        port:Int
    ){



        stopReceiver()



        totalPackets=0

        packetsLastSecond=0

        currentHz=0



        logManager.startNewSession()



        showFloatWindow()



        startUdp(
            port
        )


        startStatistics()



        sendStatus()



        Log.d(
            TAG,
            "Listen UDP:$port"
        )

    }









    private fun startUdp(
        port:Int
    ){



        udpReceiver =
            UdpReceiver(
                port
            )
            { json,ip ->



                try {



                    totalPackets++

                    packetsLastSecond++




                    // 保存原始JSON

                    logManager.save(
                        json
                    )





                    // JSON解析

                    currentStatus =
                        JsonParser.parse(
                            json
                        )




                    currentStatus.sourceIp =
                        ip





                    // 更新悬浮窗

                    floatView?.post {


                        floatView?.updateStatus(
                            currentStatus
                        )


                    }





                    //发送状态

                    sendStatus()



                }
                catch(e:Exception){


                    Log.e(
                        TAG,
                        "parse error:${e.message}"
                    )


                }


            }



        udpReceiver?.start()


    }









    private fun startStatistics(){



        statusTimer?.cancel()



        statusTimer =
            java.util.Timer()



        statusTimer?.scheduleAtFixedRate(



            object:
                java.util.TimerTask(){


                override fun run(){



                    currentHz =
                        packetsLastSecond



                    packetsLastSecond=0



                    sendStatistics()



                }


            },



            1000,

            1000


        )



    }









    private fun sendStatistics(){


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









    private fun sendStatus(){


        val intent =
            Intent(
                "com.example.netfloatmonitor.LINK_UPDATE"
            )


        intent.putExtra(
            "AIR_RSSI1",
            currentStatus.airRssi1
        )


        intent.putExtra(
            "AIR_SNR",
            currentStatus.airSnr
        )


        intent.putExtra(
            "GND_RSSI1",
            currentStatus.gndRssi1
        )


        intent.putExtra(
            "GND_SNR",
            currentStatus.gndSnr
        )


        intent.putExtra(
            "FREQ",
            currentStatus.freq
        )


        LocalBroadcastManager
            .getInstance(this)
            .sendBroadcast(
                intent
            )



    }









    private fun showFloatWindow(){



        if(floatView!=null)
            return




        val wm =
            getSystemService(
                WINDOW_SERVICE
            )
            as WindowManager





        val params =
            WindowManager.LayoutParams()



        params.width = 1300


        params.height = 540




        params.type =
            if(
                Build.VERSION.SDK_INT >=26
            )

                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

            else

                WindowManager.LayoutParams.TYPE_PHONE





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









    private fun stopReceiver(){


        udpReceiver?.stop()


        udpReceiver=null


    }









    override fun onDestroy(){


        statusTimer?.cancel()


        statusTimer=null



        stopReceiver()



        logManager.stopSession()




        if(floatView!=null){


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
            catch(_:Exception){}



            floatView=null


        }





        val stopIntent =
            Intent(
                "com.example.netfloatmonitor.STATUS_UPDATE"
            )


        stopIntent.putExtra(
            "IS_STOPPED",
            true
        )


        LocalBroadcastManager
            .getInstance(this)
            .sendBroadcast(
                stopIntent
            )



        super.onDestroy()

    }









    override fun onBind(
        intent:Intent?
    ):IBinder? {


        return null

    }









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



            getSystemService(
                NotificationManager::class.java
            )
                .createNotificationChannel(
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
                "UDP Link Monitoring"
            )

            .setSmallIcon(
                android.R.drawable.ic_menu_info_details
            )

            .setOngoing(true)

            .build()


    }


}
