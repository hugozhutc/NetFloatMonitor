package com.example.netfloatmonitor


import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import kotlin.concurrent.thread



class UdpReceiver(

    private val ip:String,

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


                socket = DatagramSocket()



                // 绑定本机监听端口
                socket?.reuseAddress = true



                socket?.bind(

                    InetSocketAddress(
                        port
                    )

                )



                val buffer =
                    ByteArray(8192)



                while(running){



                    val packet =

                        DatagramPacket(

                            buffer,

                            buffer.size

                        )




                    socket?.receive(packet)



                    val data =

                        String(

                            packet.data,

                            0,

                            packet.length,

                            Charsets.UTF_8

                        )



                    // 调试输出

                    println(
                        "UDP RX:$data"
                    )



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
