package com.example.netfloatmonitor


import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



class LogManager(

    private val context: Context

){


    private val logDir: File



    init {


        logDir = File(

            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS
            ),

            "NetFloatMonitor/log"

        )


        if(!logDir.exists()){

            logDir.mkdirs()

        }


        Log.d(
            "LogManager",
            "LOG PATH:${logDir.absolutePath}"
        )


    }






    fun save(json:String){


        try {


            val file = File(

                logDir,

                getFileName()

            )



            val text =

                """
====================
TIME:${getTime()}
$json
====================

""".trimIndent()



            file.appendText(text)



            Log.d(

                "LogManager",

                "SAVE OK:${file.absolutePath}"

            )


        }

        catch(e:Exception){


            Log.e(

                "LogManager",

                "SAVE ERROR:${e.message}"

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
