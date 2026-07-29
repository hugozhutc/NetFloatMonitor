package com.example.netfloatmonitor.network


import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress



class UdpReceiver(

    private val port:Int,

    private val onData:(String)->Unit

) {


    private var socket:DatagramSocket? = null


    @Volatile
    private var running=false


    private var thread:Thread?=null



    fun start(){


        if(running)
            return



        if(port<=0 || port>=65536){

            Log.e(
                "UdpReceiver",
                "非法端口:$port"
            )

            return

        }



        running=true



        thread = Thread {


            try{


                Log.e(
                    "UdpReceiver",
                    "UDP线程启动"
                )



                socket =
                    DatagramSocket(
                        null
                    ).apply {


                        reuseAddress=true


                        bind(
                            InetSocketAddress(
                                "0.0.0.0",
                                port
                            )
                        )


                    }



                Log.e(
                    "UdpReceiver",
                    "UDP监听成功 port=$port"
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



                    Log.e(
                        "UdpReceiver",
                        "收到UDP ${packet.address.hostAddress} len=${packet.length}"
                    )



                    if(data.isNotEmpty()){

                        onData(data)

                    }


                }


            }catch(e:Exception){


                Log.e(
                    "UdpReceiver",
                    "UDP异常",
                    e
                )


            }finally{


                stop()


            }



        }



        thread?.start()


    }







    fun stop(){


        running=false


        try{

            socket?.close()

        }catch(_:Exception){}



        socket=null


    }


}
