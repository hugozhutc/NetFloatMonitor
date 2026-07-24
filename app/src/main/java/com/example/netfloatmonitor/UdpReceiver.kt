package com.example.netfloatmonitor


import java.net.DatagramPacket
import java.net.DatagramSocket


class UdpReceiver(
    private val port:Int,
    private val callback:(String)->Unit
){


    private var running = false


    fun start(){

        running = true


        Thread {


            try {


                val socket =
                    DatagramSocket(port)


                val buffer =
                    ByteArray(4096)



                while(running){


                    val packet =
                        DatagramPacket(
                            buffer,
                            buffer.size
                        )


                    socket.receive(packet)



                    val data =
                        packet.data.copyOf(
                            packet.length
                        )


                    val hex =
                        data.joinToString(" "){

                            String.format(
                                "%02X",
                                it
                            )

                        }



                    callback(hex)

                }


                socket.close()


            }catch(e:Exception){


                callback(
                    "ERROR:${e.message}"
                )


            }


        }.start()

    }



    fun stop(){

        running=false

    }


}
