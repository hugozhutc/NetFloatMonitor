package com.example.netfloatmonitor.service


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.WindowManager

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
            "========== Service CREATE =========="
        )


        logManager = LogManager(this)



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



        // 默认UDP端口

        var port = 16789




        try {


            val p = intent?.getIntExtra(

                "PORT",

                16789

            )



            if(

                p != null &&

                p > 0 &&

                p < 65536

            ){

                port = p

            }



        }catch(e:Exception){


            Log.e(

                "FloatService",

                "读取端口异常:${e.message}"

            )


        }






        Log.e(

            "FloatService",

            "启动UDP监听 port=$port"

        )





        totalPackets = 0

        packetsSecond = 0






        logManager.startNewSession()





        showFloatWindow()



        startUdp(port)



        startTimer()





        return START_STICKY


    }









    private fun startUdp(port:Int){



        udpReceiver?.stop()





        udpReceiver = UdpReceiver(

            port

        ){ data ->




            try {



                totalPackets++

                packetsSecond++





                Log.e(

                    "FloatService",

                    "收到数据长度=${data.length}"

                )





                // JSON解析

                val status = JsonParser.parse(

                    data

                )






                // 数据处理

                val result = DataProcessor.process(

                    status

                )







                // 保存日志

                logManager.save(

                    data

                )







                // 更新悬浮窗

                floatWindow?.updateStatus(

                    result

                )




            }catch(e:Exception){



                Log.e(

                    "FloatService",

                    "数据处理异常:${e.message}",

                    e

                )


            }



        }





        udpReceiver?.start()


    }









    private fun showFloatWindow(){



        if(floatWindow != null)

            return





        try {



            val wm =

                getSystemService(

                    WINDOW_SERVICE

                ) as WindowManager






            val params =

                WindowManager.LayoutParams()






            params.width = 1200


            params.height = 600






            params.type =

                if(Build.VERSION.SDK_INT >= 26){


                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY


                }else{


                    WindowManager.LayoutParams.TYPE_PHONE


                }







            params.flags =

                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE







            params.format =

                PixelFormat.TRANSLUCENT






            params.x = 50


            params.y = 100






            floatWindow = FloatWindow(

                this,

                wm,

                params

            )






            wm.addView(

                floatWindow,

                params

            )





            Log.e(

                "FloatService",

                "悬浮窗创建成功"

            )



        }catch(e:Exception){



            Log.e(

                "FloatService",

                "悬浮窗创建失败:${e.message}",

                e

            )


        }



    }









    private fun startTimer(){



        timer?.cancel()



        timer = Timer()






        timer?.scheduleAtFixedRate(


            object : TimerTask(){



                override fun run(){



                    currentHz = packetsSecond



                    packetsSecond = 0



                    sendStatus()



                }



            },


            1000,


            1000


        )



    }









    private fun sendStatus(){



        val intent = Intent(

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









    override fun onDestroy(){



        Log.e(

            "FloatService",

            "========== Service DESTROY =========="

        )



        timer?.cancel()


        timer = null





        udpReceiver?.stop()


        udpReceiver = null





        logManager.stopSession()





        try{


            if(floatWindow != null){



                val wm =

                    getSystemService(

                        WINDOW_SERVICE

                    ) as WindowManager




                wm.removeView(

                    floatWindow

                )



                floatWindow=null



            }



        }catch(e:Exception){



            Log.e(

                "FloatService",

                "关闭悬浮窗异常:${e.message}"

            )


        }




        super.onDestroy()


    }









    override fun onBind(intent:Intent?):IBinder?{


        return null


    }









    private fun createNotificationChannel(){



        if(Build.VERSION.SDK_INT >= 26){



            val channel = NotificationChannel(

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

                "UDP监听运行中"

            )


            .setSmallIcon(

                android.R.drawable.ic_menu_info_details

            )


            .build()



    }



}
