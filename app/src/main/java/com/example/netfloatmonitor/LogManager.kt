package com.example.netfloatmonitor


import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*



class LogManager(
    private val context: Context
){


    private val logDir:File



    init{


        logDir =
            File(
                context.getExternalFilesDir(null),
                "log"
            )


        if(!logDir.exists()){

            logDir.mkdirs()

        }


    }



    fun save(data:String){


        try{


            val time =
                SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss.SSS",
                    Locale.getDefault()
                )
                .format(Date())



            val file =
                File(
                    logDir,
                    "monitor_${date()}.txt"
                )



            file.appendText(

                """
                
                $time
                
                RX:
                $data
                
                ----------------
                
                """.trimIndent()

            )


        }catch(e:Exception){


        }


    }



    private fun date():String{


        return SimpleDateFormat(
            "yyyyMMdd",
            Locale.getDefault()
        )
        .format(Date())


    }


}
