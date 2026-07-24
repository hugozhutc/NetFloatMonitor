package com.example.netfloatmonitor


import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.widget.*
import android.content.Context



class MainActivity : AppCompatActivity(){



    private lateinit var ipEdit:EditText

    private lateinit var portEdit:EditText



    override fun onCreate(
        savedInstanceState:Bundle?
    ){

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



        val startBtn =
            findViewById<Button>(
                R.id.startBtn
            )


        val stopBtn =
            findViewById<Button>(
                R.id.stopBtn
            )



        loadConfig()



        startBtn.setOnClickListener{


            saveConfig()



            // 检查悬浮窗权限

            if(!Settings.canDrawOverlays(this)){


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





            val intent =
                Intent(
                    this,
                    FloatService::class.java
                )


            intent.putExtra(
                "IP",
                ipEdit.text.toString()
            )


            intent.putExtra(
                "PORT",
                portEdit.text.toString()
                    .toInt()
            )



            startForegroundService(
                intent
            )


        }





        stopBtn.setOnClickListener{


            stopService(
                Intent(
                    this,
                    FloatService::class.java
                )
            )


        }


    }






    private fun saveConfig(){


        val sp =
            getSharedPreferences(
                "config",
                Context.MODE_PRIVATE
            )


        sp.edit()

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
                "config",
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



}
