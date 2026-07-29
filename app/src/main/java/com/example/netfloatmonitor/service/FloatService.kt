package com.example.netfloatmonitor.service


import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import android.widget.Toast

import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

import com.example.netfloatmonitor.data.DataProcessor
import com.example.netfloatmonitor.data.JsonParser
import com.example.netfloatmonitor.log.LogManager
import com.example.netfloatmonitor.network.UdpReceiver
import com.example.netfloatmonitor.ui.FloatWindow

import java.util.Timer
import java.util.TimerTask



class FloatService : Service() {



    private var floatWindow: FloatWindow? = null


    private var udpReceiver: UdpReceiver? = null


    private lateinit var logManager: LogManager



    private var totalPackets = 0L

    private var packetsSecond = 0

    private var currentHz = 0



    private var timer: Timer? = null



    override fun onCreate() {

        super.onCreate()


        Log.e(
            "FloatService",
            "Service创建"
        )


        logManager =
            LogManager(this)



        createNotificationChannel()


        startForeground(
            1001,
            createNotification()
        )


    }






    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {



        val port =
            intent?.getIntExtra(
                "PORT",
                16789
            ) ?:16789



        Log.e(
            "FloatService",
            "启动UDP端口:$port"
        )



        Toast.makeText(
            this,
            "UDP监听启动:$port",
            Toast.LENGTH_SHORT
        ).show()



        totalPackets=0

        packetsSecond=0



        logManager.startNewSession()



        showFloatWindow()



        startUdp(port)



        startTimer()



        return START_STICKY

    }









    private fun startUdp(port:Int){



        udpReceiver?.stop()



        udpReceiver =
            UdpReceiver(port){ data ->



                try {



                    totalPackets++

                    packetsSecond++



                    Log.d(
                        "UDP",
                        "收到:$data"
                    )



                    val status =
                        JsonParser.parse(data)



                    val result =
                        DataProcessor.process(status)



                    logManager.save(data)



                    floatWindow?.updateStatus(
                        result
                    )



                    sendDataState(
                        true
                    )



                }catch(e:Exception){


                    Log.e(
                        "FloatService",
                        "解析失败:${e.message}",
                        e
                    )


                    sendDataState(
                        false
                    )

                }



            }




        udpReceiver?.start()


    }









    private fun showFloatWindow(){



        if(floatWindow!=null)
            return



        try{


            val wm =
                getSystemService(
                    WINDOW_SERVICE
                ) as WindowManager




            val params =
                WindowManager.LayoutParams()



            params.width=1200

            params.height=600



            params.type =
                if(Build.VERSION.SDK_INT>=26){

                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

                }else{

                    WindowManager.LayoutParams.TYPE_PHONE

                }



            params.flags =
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE



            params.format =
                PixelFormat.TRANSLUCENT



            params.x=50

            params.y=100




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



            Toast.makeText(
                this,
                "悬浮窗创建成功",
                Toast.LENGTH_SHORT
            ).show()



        }catch(e:Exception){


            Log.e(
                "FloatService",
                "悬浮窗失败:${e.message}"
            )


        }


    }









    private fun startTimer(){


        timer?.cancel()


        timer=Timer()



        timer?.scheduleAtFixedRate(


            object:TimerTask(){


                override fun run(){


                    currentHz =
                        packetsSecond


                    packetsSecond=0



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



        LocalBroadcastManager
            .getInstance(this)
            .sendBroadcast(intent)


    }








    private fun sendDataState(
        ok:Boolean
    ){



        val intent =
            Intent(
                "com.example.netfloatmonitor.DATA_STATE"
            )


        intent.putExtra(
            "ONLINE",
            ok
        )


        LocalBroadcastManager
            .getInstance(this)
            )
            .sendBroadcast(intent)


    }









    override fun onDestroy(){


        super.onDestroy()



        timer?.cancel()


        udpReceiver?.stop()



        logManager.stopSession()



        try{


            val wm =
                getSystemService(
                    WINDOW_SERVICE
                ) as WindowManager



            floatWindow?.let{


                wm.removeView(it)

            }


        }catch(_:Exception){}



        floatWindow=null



    }







    override fun onBind(intent:Intent?):IBinder?{

        return null

    }







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
                "UDP监听运行中"
            )

            .setSmallIcon(
                android.R.drawable.ic_menu_info_details
            )

            .setOngoing(true)

            .build()


    }


}
