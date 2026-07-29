package com.example.netfloatmonitor


import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.netfloatmonitor.service.FloatService



class MainActivity : AppCompatActivity() {



    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)



        startMonitorService()


        finish()

    }







    private fun startMonitorService(){



        val intent = Intent(
            this,
            FloatService::class.java
        )



        // UDP监听端口

        intent.putExtra(
            "PORT",
            16789
        )





        if(android.os.Build.VERSION.SDK_INT >= 26){


            startForegroundService(intent)


        }else{


            startService(intent)


        }



    }



}
