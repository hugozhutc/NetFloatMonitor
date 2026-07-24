package com.example.netfloatmonitor


import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
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



    fun start(){


        if(running.get()){
            return
        }


        running.set(true)



        thread {


            try{


                socket =
                    DatagramSocket(
                        null
                    )


                socket!!.reuseAddress = true


                socket!!.bind(

                    InetSocketAddress(
                        port
                    )

                )



                val buffer =
                    ByteArray(8192)




                while(running.get()){


                    val packet =
                        DatagramPacket(
                            buffer,
                            buffer.size
                        )



                    socket!!.receive(
                        packet
                    )



                    val ip =
                        packet.address.hostAddress
                            ?: ""



                    if(
                        targetIp.isNotEmpty()
                        &&
                        ip != targetIp
                    ){

                        continue

                    }



                    val data =
                        String(

                            packet.data,

                            0,

                            packet.length,

                            Charsets.UTF_8

                        )



                    callback(
                        data
                    )



                }



            }
            catch(e:Exception){


                if(running.get()){


                    callback(

                        "UDP ERROR:${e.message}"

                    )


                }


            }
            finally{


                socket?.close()


            }


        }


    }





    fun stop(){


        running.set(false)


        socket?.close()


    }


}
