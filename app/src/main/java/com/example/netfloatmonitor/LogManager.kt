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

        context.getExternalFilesDir(null),

        "NetFloat/log"

    )



    init {


        if(!logDir.exists()){

            val result = logDir.mkdirs()

            Log.d(
                "LogManager",
                "mkdir:$result"
            )

        }


        Log.d(
            "LogManager",
            "LOG PATH:${logDir.absolutePath}"
        )


    }





    fun save(data:String){


        try {


            val file = File(

                logDir,

                getFileName()

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

                "SAVE OK:${file.absolutePath}"

            )


        }
        catch(e:Exception){


            Log.e(

                "LogManager",

                "SAVE ERROR:${e.message}",

                e

            )


        }


    }






    private fun getFileName():String{


        return SimpleDateFormat(

            "yyyyMMdd_link.log",

            Locale.getDefault()

        )
        .format(Date())


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
