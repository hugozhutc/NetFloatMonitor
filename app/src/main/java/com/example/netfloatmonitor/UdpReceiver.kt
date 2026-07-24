package com.example.netfloatmonitor


import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean



class UdpReceiver(

    private val targetIp:String,

    private val targetPort:Int,

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


        running.set(true)



        Thread{


            try{


                socket =
                    DatagramSocket(
                        targetPort
                    )



                val buffer =
                    ByteArray(4096)




                while(running.get()){


                    val packet =
                        DatagramPacket(
                            buffer,
                            buffer.size
                        )



                    socket!!.receive(
                        packet
                    )



                    val sourceIp =
                        packet.address.hostAddress




                    // IP过滤

                    if(
                        targetIp.isNotEmpty()
                        &&
                        sourceIp != targetIp
                    ){

                        continue

                    }




                    val data =

                        String(
                            packet.data,
                            0,
                            packet.length
                        )




                    packetCount++


                    byteCount +=
                        packet.length




                    val now =
                        System.currentTimeMillis()



                    val second =
                        (now-startTime)/1000.0




                    val kbps =

                        if(second>0)

                            byteCount*8/
                            second/
                            1000

                        else

                            0.0




                    val info =

"""
$data


----------------

RX:
$packetCount

Rate:
${String.format("%.2f",kbps)} kbps

SRC:
$sourceIp

""".trimIndent()




                    callback(
                        info
                    )



                }


            }
            catch(e:Exception){


                callback(
                    "UDP ERROR:${e.message}"
                )


            }



        }.start()


    }






    fun stop(){


        running.set(false)


        try{


            socket?.close()


        }
        catch(e:Exception){}



    }



}
