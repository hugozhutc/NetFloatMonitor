package com.example.netfloatmonitor


import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread



class UdpReceiver(

    private val targetIp:String,

    private val port:Int,

    private val callback:(String)->Unit

){


    private var socket:DatagramSocket? = null


    private val running =
        AtomicBoolean(false)



    private var packetCount = 0

    private var byteCount = 0L


    private var startTime =
        System.currentTimeMillis()





    fun start(){


        if(running.get())
            return



        running.set(true)



        thread {


            try{


                socket =
                    DatagramSocket(port)



                val buffer =
                    ByteArray(8192)



                while(running.get()){


                    val packet =
                        DatagramPacket(
                            buffer,
                            buffer.size
                        )


                    socket!!.receive(packet)



                    val sourceIp =
                        packet.address.hostAddress ?: ""



                    // IP过滤

                    if(
                        targetIp.isNotEmpty()
                        &&
                        sourceIp != targetIp
                    ){
                        continue
                    }




                    val data = String(

                        packet.data,

                        0,

                        packet.length,

                        Charsets.UTF_8

                    )



                    packetCount++

                    byteCount += packet.length



                    callback(data)



                }


            }
            catch(e:Exception){


                callback(
                    "UDP ERROR: ${e.message}"
                )


            }
            finally{


                socket?.close()


            }


        }


    }






    fun getStatistics():String{


        val time =

            System.currentTimeMillis()
            -
            startTime



        val rate =

            if(time>0)

                byteCount.toDouble()
                *
                8
                /
                time
                *
                1000

            else

                0.0




        return "PACKET:$packetCount\n" +
                "DATA:${byteCount/1024} KB\n" +
                "RATE:${String.format("%.2f",rate)} kbps"


    }






    fun stop(){


        running.set(false)


        try{

            socket?.close()

        }
        catch(e:Exception){


        }


    }


}
