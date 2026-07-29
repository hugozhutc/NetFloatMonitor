package com.example.netfloatmonitor.net


import android.util.Log

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException



class UdpReceiver(
    private val port:Int,
    private val onData:(String, String)->Unit
) {



    private val TAG =
        "UdpReceiver"



    private var socket:DatagramSocket? =
        null



    private var thread:Thread? =
        null



    @Volatile
    private var running=false



    private var packetCount=0L







    fun start(){


        if(running)
            return



        running=true



        thread =
            Thread{


                receiveLoop()


            }



        thread?.start()



    }









    private fun receiveLoop(){


        try{


            socket =
                DatagramSocket(
                    port
                )



            // 增大UDP接收缓存

            socket?.receiveBufferSize =
                1024*1024





            Log.d(
                TAG,
                "UDP监听启动:$port"
            )





            val buffer =
                ByteArray(
                    65535
                )





            while(
                running
            ){



                try{


                    val packet =
                        DatagramPacket(
                            buffer,
                            buffer.size
                        )



                    socket?.receive(
                        packet
                    )



                    val data =
                        String(
                            packet.data,
                            0,
                            packet.length,
                            Charsets.UTF_8
                        )
                            .trim()





                    if(
                        data.isEmpty()
                    )
                        continue





                    packetCount++





                    val ip =
                        packet.address
                            ?.hostAddress
                            ?: "unknown"




                    onData(
                        data,
                        ip
                    )



                }
                catch(e:SocketException){


                    if(running){


                        Log.e(
                            TAG,
                            "Socket异常:${e.message}"
                        )

                    }


                    break


                }


            }



        }
        catch(e:Exception){


            Log.e(
                TAG,
                "UDP启动失败:${e.message}"
            )


        }
        finally{


            closeSocket()


        }



    }









    fun stop(){


        running=false



        closeSocket()



        try{


            thread?.join(
                300
            )


        }
        catch(_:Exception){}



        thread=null



    }









    private fun closeSocket(){



        try{


            socket?.close()



        }
        catch(_:Exception){}



        socket=null



    }









    fun getPacketCount():Long{


        return packetCount


    }



}
