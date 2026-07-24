package com.example.netfloatmonitor


import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



class LogManager(

    private val context:Context

){



    private val logDir:File



    init{


        logDir = File(

            context.getExternalFilesDir(null),

            "NetFloat/log"

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


            val file =

                File(

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



        }


    }








    /**
     * 当前日志文件
     */
    private fun getFileName():String{


        return SimpleDateFormat(

            "yyyyMMdd"

            + "_link.jsonl",

            Locale.getDefault()

        )

        .format(
            Date()
        )


    }






    private fun getTime():String{


        return SimpleDateFormat(

            "yyyy-MM-dd HH:mm:ss.SSS",

            Locale.getDefault()

        )

        .format(
            Date()
        )


    }






    /**
     * 获取日志目录
     */
    fun getLogPath():String{


        return logDir.absolutePath


    }



}
