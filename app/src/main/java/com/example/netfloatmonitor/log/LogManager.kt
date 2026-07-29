package com.example.netfloatmonitor.data


import android.content.Context
import android.os.Environment
import android.util.Log

import org.json.JSONObject

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.LinkedBlockingQueue



class LogManager(
    private val context:Context
) {



    private val TAG =
        "LogManager"




    // Android公共Documents目录

    private val logDir =
        File(
            Environment
                .getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS
                ),
            "NetFloatMonitor/log"
        )




    private var running=false



    private var writerThread:Thread?=null



    private var writer:BufferedWriter?=null



    private var currentFile:File?=null



    private val queue =
        LinkedBlockingQueue<String>(5000)



    private val headers =
        LinkedHashSet<String>()



    init {


        if(
            !logDir.exists()
        ){

            logDir.mkdirs()

        }


    }








    fun getLogPath():String{


        return logDir.absolutePath

    }








    fun getLogFiles():List<File>{


        return logDir
            .listFiles()
            ?.filter {

                it.extension=="csv"

            }
            ?: emptyList()


    }









    fun getCurrentFileName():String{


        return currentFile
            ?.name
            ?: "未开启监控"


    }









    fun startNewSession(){



        stopSession()



        headers.clear()



        val name =
            "NetLog_" +
                    SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
                        Locale.getDefault()
                    )
                        .format(Date())
                    +
                    ".csv"



        currentFile =
            File(
                logDir,
                name
            )



        running=true



        writerThread =
            Thread {


                writeLoop()


            }



        writerThread?.start()



        Log.d(
            TAG,
            "日志开始:$name"
        )



    }









    fun save(
        json:String
    ){


        if(
            !running
        )
            return



        if(
            json.isBlank()
        )
            return



        queue.offer(
            json
        )


    }









    private fun writeLoop(){



        try{


            writer =
                BufferedWriter(
                    FileWriter(
                        currentFile,
                        true
                    )
                )




            while(
                running ||
                queue.isNotEmpty()
            ){



                val json =
                    queue.poll()



                if(
                    json==null
                ){

                    Thread.sleep(20)

                    continue

                }



                writeJson(
                    json
                )


            }



        }
        catch(e:Exception){


            Log.e(
                TAG,
                "写日志异常:${e.message}"
            )


        }
        finally{


            try{


                writer?.flush()

                writer?.close()


            }
            catch(_:Exception){}


        }



    }









    private fun writeJson(
        json:String
    ){



        val obj =
            JSONObject(json)




        //第一次收到数据

        if(
            headers.isEmpty()
        ){


            headers.add(
                "Timestamp"
            )


            val keys =
                obj.keys()


            while(
                keys.hasNext()
            ){

                headers.add(
                    keys.next()
                )

            }



            writer?.write(
                headers.joinToString(",")
            )


            writer?.newLine()


        }





        //检查新增字段

        val keys =
            obj.keys()



        while(
            keys.hasNext()
        ){

            headers.add(
                keys.next()
            )

        }







        val row =
            ArrayList<String>()



        row.add(
            time()
        )



        headers
            .drop(1)
            .forEach {


                val value =
                    obj.optString(
                        it,
                        ""
                    )


                row.add(
                    escape(value)
                )


            }



        writer?.write(
            row.joinToString(",")
        )


        writer?.newLine()



        writer?.flush()



    }









    private fun escape(
        value:String
    ):String{


        return if(
            value.contains(",") ||
            value.contains("\"")
        ){

            "\"" +
                    value.replace(
                        "\"",
                        "\"\""
                    )
                    +
                    "\""

        }
        else
            value


    }









    private fun time():String{


        return SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS",
            Locale.getDefault()
        )
            .format(
                Date()
            )


    }









    fun stopSession(){



        running=false



        try{

            writerThread?.join(
                500
            )

        }
        catch(_:Exception){}



        writerThread=null



        writer?.close()

        writer=null



        queue.clear()



    }



}
