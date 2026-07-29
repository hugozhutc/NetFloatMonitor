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
import com.example.netfloatmonitor.data.JsonParser
import com.example.netfloatmonitor.log.LogManager
import com.example.netfloatmonitor.net.UdpReceiver
import com.example.netfloatmonitor.ui.FloatWindow

import java.util.Timer
import java.util.TimerTask



class FloatService : Service() {



    private var floatWindow: FloatWindow? = null


    private var udpReceiver: UdpReceiver? = null



    private lateinit var logManager: LogManager




    private var packetCount = 0


    private var currentHz = 0


    private var secondCount = 0



    private var timer: Timer? = null





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

            "Service Started"

        )

    }









    override fun onStartCommand(

        intent:Intent?,

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



        startUDP(port)



        startTimer()



        return START_NOT_STICKY

    }









    private fun startUDP(

        port:Int

    ){



        udpReceiver?.stop()



        udpReceiver =

            UdpReceiver(

                port

            ){ data ->



                try {



                    packetCount++

                    secondCount++



                    logManager.save(
                        data
                    )



                    val status =

                        JsonParser.parse(
                            data
                        )



                    floatWindow?.post {



                        floatWindow
                            ?.updateStatus(
                                status
                            )

                    }



                }catch(e:Exception){


                    Log.e(

                        "FloatService",

                        "JSON error ${e.message}"

                    )


                }


            }



        udpReceiver?.start()


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



        params.width =

            900



        params.height =

            WindowManager.LayoutParams.WRAP_CONTENT





        params.type =

            if(Build.VERSION.SDK_INT>=26)

                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

            else

                WindowManager.LayoutParams.TYPE_PHONE





        params.format =

            PixelFormat.TRANSLUCENT





        params.flags =

            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE





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









    private fun startTimer(){



        timer?.cancel()



        timer = Timer()



        timer?.scheduleAtFixedRate(

            object:TimerTask(){



                override fun run(){



                    currentHz =
                        secondCount



                    secondCount=0




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

            packetCount

        )


        intent.putExtra(

            "HZ",

            currentHz

        )



        sendBroadcast(intent)



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


        }catch(e:Exception){



            Log.e(

                "FloatService",

                e.message ?: ""

            )


        }





        floatWindow=null



        sendStopBroadcast()


    }









    private fun sendStopBroadcast(){


        val intent =

            Intent(

                "com.example.netfloatmonitor.STATUS_UPDATE"

            )


        intent.putExtra(

            "IS_STOPPED",

            true

        )


        sendBroadcast(intent)


    }









    override fun onBind(

        intent:Intent?

    ):IBinder? = null







    private fun createNotificationChannel(){


        if(Build.VERSION.SDK_INT>=26){


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

                "UDP监听运行中"

            )

            .setSmallIcon(

                android.R.drawable.ic_menu_info_details

            )

            .build()


    }



}
