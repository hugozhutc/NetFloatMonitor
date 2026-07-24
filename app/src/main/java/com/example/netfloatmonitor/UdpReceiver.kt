package com.example.netfloatmonitor


import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread



class UdpReceiver(

    // 目标设备IP
    private val targetIp:String,

    // UDP监听端口
    private val port:Int,

    // 数据回调
    private val callback:(String)->Unit

){



    private var socket:DatagramSocket? = null


    private val running =
        AtomicBoolean(false)



    //统计

    private var packetCount = 0


    private var byteCount = 0L


    private var startTime =
        System.currentTimeMillis()





    fun start(){


        if(running.get())
            return



        running.set(true)



        thread {


            try {



                socket =
                    DatagramSocket(port)



                socket?.soTimeout =
                    1000



                val buffer =
                    ByteArray(8192)





                while(running.get()){



                    try{


                        val packet =
                            DatagramPacket(
                                buffer,
                                buffer.size
                            )



                        socket?.receive(
                            packet
                        )



                        val sourceIp =

                            packet.address
                                .hostAddress
                                ?: ""





                        /**
                         * IP过滤
                         *
                         * 空IP表示接收全部
                         */

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

                                packet.length,

                                Charsets.UTF_8

                            )





                        packetCount++


                        byteCount +=
                            packet.length





                        val now =
                            System.currentTimeMillis()



                        val duration =

                            (now-startTime)
                                .coerceAtLeast(1)





                        val kbps =

                            byteCount
                                .toDouble()
                                *
                                8
                                /
                                duration
                                *
                                1000
                                /
                                1000





                        /**
                         * 返回原始JSON
                         *
                         * 不在这里解析
                         * 交给JsonParser
                         */

                        callback(
                            data
                        )





                    }
                    catch(e:java.net.SocketTimeoutException){


                        // 超时继续监听


                    }



                }



            }

            catch(e:Exception){



                callback(

                    """
                    UDP ERROR
                    
                    ${e.message}
                    
                    """.trimIndent()

                )



            }

            finally{


                try{


                    socket?.close()


                }
                catch(_:Exception){}



            }



        }



    }







    /**
     * 获取统计信息
     */
    fun getStatistics():String{


        val time =

            (System.currentTimeMillis()
                    -
                    startTime)
                .coerceAtLeast(1)



        val rate =

            byteCount
                .toDouble()
                *
                8
                /
                time
                *
                1000
                /
                1000




        return """

PACKET:
$packetCount


DATA:
${byteCount/1024} KB


RATE:
${String.format("%.2f",rate)} kbps

""".trimIndent()



    }







    fun stop(){



        running.set(false)



        try{


            socket?.close()


        }
        catch(_:Exception){}



    }



}
