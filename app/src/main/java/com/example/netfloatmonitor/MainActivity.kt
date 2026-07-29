package com.example.netfloatmonitor


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.netfloatmonitor.log.LogManager
import com.example.netfloatmonitor.service.FloatService
import java.io.File



class MainActivity : AppCompatActivity() {


    private lateinit var ipEdit: EditText

    private lateinit var portEdit: EditText

    private lateinit var logPath: TextView

    private lateinit var tvStatusInfo: TextView


    private lateinit var logManager: LogManager




    private val statusReceiver =
        object : BroadcastReceiver(){


            override fun onReceive(

                context: Context?,

                intent: Intent?

            ) {


                if(intent==null)

                    return



                if(
                    intent.getBooleanExtra(
                        "IS_STOPPED",
                        false
                    )
                ){


                    tvStatusInfo.text =
                        """
                        状态: 已停止
                        
                        文件:
                        未开启
                        
                        数据:
                        0 packet
                        """.trimIndent()


                    return

                }



                val total =

                    intent.getLongExtra(
                        "TOTAL_PACKETS",
                        0
                    )


                val hz =

                    intent.getIntExtra(
                        "HZ",
                        0
                    )



                tvStatusInfo.text =

                    """
                    状态: 运行中
                    
                    文件:
                    ${logManager.getCurrentFileName()}
                    
                    数据:
                    $total packet
                    
                    速率:
                    $hz Hz
                    
                    """.trimIndent()


            }


        }





    override fun onCreate(

        savedInstanceState: Bundle?

    ){

        super.onCreate(savedInstanceState)


        setContentView(
            R.layout.activity_main
        )



        logManager =
            LogManager(this)




        ipEdit =
            findViewById(
                R.id.editIp
            )


        portEdit =
            findViewById(
                R.id.editPort
            )


        logPath =
            findViewById(
                R.id.logPath
            )


        tvStatusInfo =
            findViewById(
                R.id.tvStatusInfo
            )




        val startBtn =
            findViewById<Button>(
                R.id.startBtn
            )


        val stopBtn =
            findViewById<Button>(
                R.id.stopBtn
            )


        val clearBtn =
            findViewById<Button>(
                R.id.clearBtn
            )





        loadConfig()


        logPath.text =
            """
            日志目录:
            ${logManager.getLogPath()}
            """.trimIndent()



        tvStatusInfo.text =
            """
            状态: 待机
            
            等待UDP数据...
            """.trimIndent()





        startBtn.setOnClickListener{


            if(
                !Settings.canDrawOverlays(this)
            ){


                val intent =
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse(
                            "package:$packageName"
                        )
                    )


                startActivity(intent)


                Toast.makeText(
                    this,
                    "请开启悬浮窗权限",
                    Toast.LENGTH_SHORT
                ).show()


                return@setOnClickListener

            }





            saveConfig()



            val port =

                portEdit.text
                    .toString()
                    .toIntOrNull()
                    ?:16789





            val serviceIntent =

                Intent(
                    this,
                    FloatService::class.java
                ).apply{


                    putExtra(
                        "PORT",
                        port
                    )


                    putExtra(
                        "IP",
                        ipEdit.text.toString()
                    )


                }





            if(
                Build.VERSION.SDK_INT >=26
            ){

                startForegroundService(
                    serviceIntent
                )

            }else{


                startService(
                    serviceIntent
                )

            }




            Toast.makeText(
                this,
                "UDP监听启动:$port",
                Toast.LENGTH_SHORT
            ).show()


        }





        stopBtn.setOnClickListener{


            stopService(
                Intent(
                    this,
                    FloatService::class.java
                )
            )


            Toast.makeText(
                this,
                "监听停止",
                Toast.LENGTH_SHORT
            ).show()


        }






        clearBtn.setOnClickListener{


            clearLog()


        }



    }









    override fun onStart(){

        super.onStart()


        LocalBroadcastManager
            .getInstance(this)
            .registerReceiver(

                statusReceiver,

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
                statusReceiver
            )

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








    private fun clearLog(){


        val files:List<File> =
            logManager.getLogFiles()


        var count=0



        files.forEach{


            if(it.delete())

                count++

        }



        Toast.makeText(

            this,

            "删除 $count 个日志",

            Toast.LENGTH_SHORT

        ).show()


    }



}
