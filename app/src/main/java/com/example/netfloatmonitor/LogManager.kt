package com.example.netfloatmonitor


import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class LogManager(

    private val context: Context

){


    private val logDir: File = File(

        context.filesDir,

        "NetFloat/log"

    )



    init {


        if(!logDir.exists()){

            logDir.mkdirs()

        }


        Log.d(

            "LogManager",

            "PATH:${logDir.absolutePath}"

        )

    }




    fun save(data:String){


        try {


            val file = File(

                logDir,

                "test.log"

            )



            file.appendText(

                """
                
====================
TIME:${getTime()}

$data

====================

""".trimIndent()
                + "\n"

            )



            Log.d(

                "LogManager",

                "WRITE OK:${file.absolutePath}"

            )


        }
        catch(e:Exception){


            Log.e(

                "LogManager",

                "WRITE ERROR:${e.message}",

                e

            )


        }


    }






    private fun getTime():String{


        return SimpleDateFormat(

            "yyyy-MM-dd HH:mm:ss.SSS",

            Locale.getDefault()

        )
        .format(Date())


    }






    fun getLogPath():String{


        return logDir.absolutePath


    }


}
