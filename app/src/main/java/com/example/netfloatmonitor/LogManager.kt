package com.example.netfloatmonitor


import android.content.Context
import android.os.Environment
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

            Environment.getExternalStorageDirectory(),

            "NetFloatMonitor/log"

        )



        if(!logDir.exists()){

            logDir.mkdirs()

        }


    }







    /**
     * 保存JSON数据
     */
    fun save(json:String){


        try{


            val file = File(

                logDir,

                getFileName()

            )





            val record =

                """
{
"time":"${getTime()}",
"data":$json
}

""".trimIndent()





            file.appendText(

                record + "\n"

            )



        }

        catch(e:Exception){


            e.printStackTrace()


        }


    }









    private fun getFileName():String{


        return SimpleDateFormat(

            "yyyyMMdd'_link.jsonl'",

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
