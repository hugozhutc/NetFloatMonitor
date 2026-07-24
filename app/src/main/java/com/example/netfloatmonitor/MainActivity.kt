package com.example.netfloatmonitor


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity



class MainActivity : AppCompatActivity(){



    private lateinit var ipEdit:EditText

    private lateinit var portEdit:EditText

    private lateinit var logPath:TextView





    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)


        setContentView(
            R.layout.activity_main
        )



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





        startBtn.setOnClickListener{


            saveConfig()



            //检查悬浮窗权限

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


                return@setOnClickListener

            }





            val serviceIntent =

                Intent(
                    this,
                    FloatService::class.java
                )



            serviceIntent.putExtra(
                "IP",
                ipEdit.text.toString()
            )



            serviceIntent.putExtra(
                "PORT",
                portEdit.text.toString()
                    .toInt()
            )



            startForegroundService(
                serviceIntent
            )


            Toast.makeText(
                this,
                "监听启动",
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
                "已停止",
                Toast.LENGTH_SHORT
            ).show()


        }







        clearBtn.setOnClickListener{


            clearLog()


        }




        showLogPath()



    }








    /**
     * 保存配置
     */
    private fun saveConfig(){


        getSharedPreferences(
            "net_config",
            Context.MODE_PRIVATE
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








    /**
     * 读取配置
     */
    private fun loadConfig(){


        val sp =

            getSharedPreferences(
                "net_config",
                Context.MODE_PRIVATE
            )



        ipEdit.setText(

            sp.getString(
                "ip",
                "192.168.1.100"
            )

        )



        portEdit.setText(

            sp.getString(
                "port",
                "14550"
            )

        )


    }









    private fun showLogPath(){


        val path =

            LogManager(this)
                .getLogPath()



        logPath.text =

            "日志目录:\n$path"



    }








    /**
     * 删除日志
     */
    private fun clearLog(){


        val dir =

            java.io.File(

                LogManager(this)
                    .getLogPath()

            )



        dir.listFiles()?.forEach {


            it.delete()

        }



        Toast.makeText(

            this,

            "日志已清除",

            Toast.LENGTH_SHORT

        ).show()


    }



}
