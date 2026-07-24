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



class FloatService : Service() {



    private lateinit var windowManager: WindowManager


    private lateinit var floatView: FloatView


    private lateinit var windowParams:
            WindowManager.LayoutParams



    private var receiver: UdpReceiver? = null



    private lateinit var logger: LogManager





    override fun onCreate() {


        super.onCreate()



        // 创建前台通知

        createNotification()



        logger =
            LogManager(this)



        windowManager =
            getSystemService(
                Context.WINDOW_SERVICE
            ) as WindowManager



        createFloatWindow()



    }





    /**
     * 创建悬浮窗
     */
    private fun createFloatWindow(){



        windowParams =
            WindowManager.LayoutParams()



        windowParams.width =
            600



        windowParams.height =
            700



        windowParams.x =
            100



        windowParams.y =
            200




        windowParams.gravity =
            Gravity.TOP or Gravity.LEFT




        windowParams.format =
            PixelFormat.TRANSLUCENT




        windowParams.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE




        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){


            windowParams.type =
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY


        }
        else{


            windowParams.type =
                WindowManager.LayoutParams.TYPE_PHONE


        }





        floatView =
            FloatView(
                this,
                windowManager,
                windowParams
            )



        windowManager.addView(
            floatView,
            windowParams
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
                14550
            )
            ?:14550




        startUdp(port)



        return START_STICKY

    }






    /**
     * 启动UDP监听
     */
    private fun startUdp(port:Int){



        receiver?.stop()



        receiver =
            UdpReceiver(
                port
            ){data->




                try {



                    // 保存原始JSON

                    logger.save(data)



                    // 显示数据


                    val display =

                        formatJson(data)



                    floatView.update(
                        display
                    )



                }
                catch(e:Exception){



                    floatView.update(
                        e.message ?: "ERROR"
                    )


                }



            }



        receiver?.start()



    }







    /**
     * JSON简单格式化
     */
    private fun formatJson(
        json:String
    ):String{


        return try{


            val obj =
                org.json.JSONObject(json)



            """
NetFloat Monitor


GROUND

RSSI:
${obj.optString("rssi1_g")}/${obj.optString("rssi2_g")}

SNR:
${obj.optString("snr_g")}

LQI:
${obj.optString("lqi_g")}%


AIR

RSSI:
${obj.optString("rssi1_a")}/${obj.optString("rssi2_a")}

SNR:
${obj.optString("snr_a")}

LQI:
${obj.optString("lqi_a")}%


LINK

MCS:
${obj.optString("mcs")}

RX:
${obj.optString("freq_rx")}

TX:
${obj.optString("freq_tx")}

POWER:
${obj.optString("power")} dBm


DIST:
${obj.optString("distance")} m

""".trimIndent()



        }
        catch(e:Exception){


            json


        }


    }









    /**
     * 前台通知
     */
    private fun createNotification(){



        val channelId =
            "netfloat_monitor"



        if(Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O){



            val channel =
                NotificationChannel(
                    channelId,
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






        val notification =

            NotificationCompat.Builder(
                this,
                channelId
            )


                .setContentTitle(
                    "NetFloat Monitor运行中"
                )


                .setContentText(
                    "UDP数据监听"
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


        }
        catch(e:Exception){



        }




        super.onDestroy()


    }







    override fun onBind(
        intent:Intent?
    ):IBinder?{


        return null


    }



}
