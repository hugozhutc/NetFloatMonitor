package com.example.netfloatmonitor


import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import kotlin.concurrent.thread



class UdpReceiver(

    private val port:Int,

    private val onData:(String)->Unit

){


    private var running = false

    private var socket:DatagramSocket? = null



    fun start(){


        if(running)
            return


        running = true



        thread {


            try {



                socket = DatagramSocket(
                    null
                )



                socket!!.reuseAddress = true



                //监听 UDP 16789

                socket!!.bind(

                    InetSocketAddress(

                        "0.0.0.0",

                        port

                    )

                )



                println(
                    "UDP LISTEN:$port"
                )



                val buffer =
                    ByteArray(8192)



                while(running){



                    val packet =

                        DatagramPacket(

                            buffer,

                            buffer.size

                        )



                    socket!!.receive(

                        packet

                    )




                    val sourceIP =

                        packet.address
                            .hostAddress



                    val data =

                        String(

                            packet.data,

                            0,

                            packet.length,

                            Charsets.UTF_8

                        )




                    println(

                        "UDP RX FROM:$sourceIP"

                    )



                    println(data)




                    onData(data)



                }



            }
            catch(e:Exception){


                println(

                    "UDP ERROR:${e.message}"

                )


            }


        }


    }







    fun stop(){


        running=false


        try{

            socket?.close()

        }
        catch(_:Exception){}



    }


}
