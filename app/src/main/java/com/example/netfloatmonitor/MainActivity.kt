package com.example.netfloatmonitor


import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*

import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager

import com.example.netfloatmonitor.data.LogManager
import com.example.netfloatmonitor.service.FloatService

import java.io.File



class MainActivity : AppCompatActivity() {


    private lateinit var ipEdit:EditText
    private lateinit var portEdit:EditText

    private lateinit var tvStatus:TextView
    private lateinit var tvLogPath:TextView


    private lateinit var logManager:LogManager





    private val receiver =
        object:BroadcastReceiver(){


            override fun onReceive(
                context:Context?,
                intent:Intent?
            ){


                if(intent==null)
                    return



                if(
                    intent.getBooleanExtra(
                        "IS_STOPPED",
                        false
                    )
                ){


                    updateStatus(
                        "● STOPPED\n\n没有运行"
                    )

                    return

                }




                val total =
                    intent.getIntExtra(
                        "TOTAL_PACKETS",
                        0
                    )


                val hz =
                    intent.getIntExtra(
                        "HZ",
                        0
                    )



                val file =
                    intent.getStringExtra(
                        "FILE"
                    )
                    ?: "--"




                updateStatus(
                    """
                    ● ONLINE
                    
                    接收:
                    $total packets
                    
                    速率:
                    $hz Hz
                    
                    文件:
                    $file
                    
                    """.trimIndent()
                )


            }



        }









    override fun onCreate(
        savedInstanceState:Bundle?
    ){

        super.onCreate(
            savedInstanceState
        )


        setContentView(
            R.layout.activity_main
        )



        logManager =
            LogManager(
                this
            )



        ipEdit =
            findViewById(
                R.id.editIp
            )


        portEdit =
            findViewById(
                R.id.editPort
            )


        tvStatus =
            findViewById(
                R.id.tvStatusInfo
            )


        tvLogPath =
            findViewById(
                R.id.logPath
            )




        val start =
            findViewById<Button>(
                R.id.startBtn
            )


        val stop =
            findViewById<Button>(
                R.id.stopBtn
            )


        val clear =
            findViewById<Button>(
                R.id.clearBtn
            )





        loadConfig()


        tvLogPath.text =
            """
            日志目录:
            ${logManager.getLogPath()}
            """.trimIndent()



        updateStatus(
            "● READY\n\n等待启动"
        )






        start.setOnClickListener{


            if(
                !checkFloatPermission()
            )
                return@setOnClickListener




            saveConfig()



            val port =
                portEdit.text
                    .toString()
                    .toIntOrNull()
                    ?:16789





            val intent =
                Intent(
                    this,
                    FloatService::class.java
                )


            intent.putExtra(
                "PORT",
                port
            )


            intent.putExtra(
                "IP",
                ipEdit.text.toString()
            )




            if(
                Build.VERSION.SDK_INT>=26
            ){

                startForegroundService(
                    intent
                )

            }
            else{

                startService(
                    intent
                )

            }





            Toast.makeText(
                this,
                "UDP监听启动:$port",
                Toast.LENGTH_SHORT
            ).show()



        }









        stop.setOnClickListener{


            stopService(
                Intent(
                    this,
                    FloatService::class.java
                )
            )


        }








        clear.setOnClickListener{


            clearLogs()


        }



    }









    override fun onStart(){

        super.onStart()


        LocalBroadcastManager
            .getInstance(this)
            .registerReceiver(
                receiver,
                IntentFilter(
                    "com.example.netfloatmonitor.STATUS_UPDATE"
                )
            )


    }









    override fun onStop(){

        super.onStop()


        LocalBroadcastManager
            .getInstance(this)
            .unregisterReceiver(
                receiver
            )


    }









    private fun checkFloatPermission():Boolean{


        if(
            Settings.canDrawOverlays(this)
        )
            return true





        val intent =
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse(
                    "package:$packageName"
                )
            )


        startActivity(
            intent
        )


        Toast.makeText(
            this,
            "请开启悬浮窗权限",
            Toast.LENGTH_SHORT
        ).show()



        return false


    }









    private fun updateStatus(
        text:String
    ){

        tvStatus.text =
            text

    }









    private fun saveConfig(){


        getSharedPreferences(
            "net_config",
            MODE_PRIVATE
        )
            .edit()
            .putString(
                "ip",
                ipEdit.text.toString()
            )
            .putString(
                "port",
                portEdit.text.toString()
            )
            .apply()



    }









    private fun loadConfig(){


        val sp =
            getSharedPreferences(
                "net_config",
                MODE_PRIVATE
            )


        ipEdit.setText(
            sp.getString(
                "ip",
                "192.168.144.33"
            )
        )


        portEdit.setText(
            sp.getString(
                "port",
                "16789"
            )
        )


    }









    private fun clearLogs(){


        var count=0



        logManager
            .getLogFiles()
            .forEach{


                if(
                    it.delete()
                )
                    count++


            }




        Toast.makeText(
            this,
            "删除 $count 个日志文件",
            Toast.LENGTH_SHORT
        ).show()



    }



}
