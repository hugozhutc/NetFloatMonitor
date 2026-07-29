package com.example.netfloatmonitor.log


import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale




class LogManager(

    private val context: Context

) {



    private val logDir:File =

        File(

            context.getExternalFilesDir(null),

            "NetFloatLogs"

        ).apply {


            if(!exists()){

                mkdirs()

            }

        }





    private var currentFile:File? = null



    private var headers =

        mutableListOf<String>()



    private var recording=false





    private val maxFileSize =

        100 * 1024 * 1024







    fun getLogPath():String{


        return logDir.absolutePath


    }









    fun getLogFiles():List<File>{


        return logDir.listFiles()

            ?.filter {

                it.extension=="csv"

            }

            ?: emptyList()


    }









    fun getCurrentFileName():String{


        return currentFile?.name
            ?: "未开启监控"


    }









    @Synchronized

    fun startNewSession(){



        stopSession()



        headers.clear()



        currentFile =

            createNewFile()



        recording=true




        Log.d(

            "LogManager",

            "开始记录:${currentFile?.name}"

        )


    }









    @Synchronized

    fun stopSession(){



        recording=false



        headers.clear()



        currentFile=null



    }









    @Synchronized

    fun save(json:String){



        if(!recording)
            return



        if(json.isBlank())
            return





        try{


            val obj =

                JSONObject(json)





            checkFileSize()





            if(headers.isEmpty()){


                buildHeader(obj)


            }





            val row =

                mutableListOf<String>()





            row.add(

                getTime()

            )





            headers.drop(1).forEach{


                key ->


                row.add(

                    csvEscape(

                        obj.optString(

                            key,

                            ""

                        )

                    )

                )


            }





            appendLine(

                row.joinToString(",")

            )





        }catch(e:Exception){


            Log.e(

                "LogManager",

                "CSV保存失败:${e.message}"

            )


        }



    }









    private fun buildHeader(

        obj:JSONObject

    ){



        headers.clear()



        headers.add(

            "Timestamp"

        )



        val keys =

            obj.keys()



        while(keys.hasNext()){


            headers.add(

                keys.next()

            )


        }





        appendLine(

            headers.joinToString(",")

        )


    }









    private fun appendLine(

        text:String

    ){



        currentFile?.let{


            FileWriter(

                it,

                true

            ).use { writer ->


                writer.append(

                    text

                )

                writer.append(

                    "\n"

                )


            }


        }


    }









    private fun createNewFile():File{


        val sdf =

            SimpleDateFormat(

                "yyyyMMdd_HHmmss",

                Locale.getDefault()

            )



        return File(

            logDir,

            "NetFloat_${sdf.format(Date())}.csv"

        )


    }









    private fun checkFileSize(){



        currentFile?.let{



            if(

                it.exists()

                &&

                it.length()>maxFileSize

            ){



                currentFile =

                    createNewFile()



                headers.clear()



            }


        }


    }









    private fun csvEscape(

        value:String

    ):String{


        if(

            value.contains(",")

            ||

            value.contains("\"")

            ||

            value.contains("\n")

        ){


            return "\"" +

                    value.replace(

                        "\"",

                        "\"\""

                    )

                    +

                    "\""


        }



        return value


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


}
