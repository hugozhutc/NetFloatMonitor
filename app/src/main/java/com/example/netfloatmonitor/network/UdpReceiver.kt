package com.example.netfloatmonitor.network


import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress



class UdpReceiver(

    private val port: Int,

    private val onData: (String) -> Unit

) {


    private var socket: DatagramSocket? = null


    @Volatile
    private var running = false


    private var thread: Thread? = null




    fun start() {


        if (running) {

            Log.e(
                "UdpReceiver",
                "UDP已经运行"
            )

            return
        }



        running = true



        thread = Thread {


            Log.e(
                "UdpReceiver",
                "UDP线程启动"
            )



            try {


                socket = DatagramSocket(
                    null
                ).apply {


                    reuseAddress = true



                    bind(

                        InetSocketAddress(
                            "0.0.0.0",
                            port
                        )

                    )



                    Log.e(

                        "UdpReceiver",

                        "UDP绑定成功 port=$localPort address=$localAddress"

                    )


                }




                val buffer = ByteArray(8192)



                while (running) {



                    val packet = DatagramPacket(

                        buffer,

                        buffer.size

                    )



                    socket?.receive(
                        packet
                    )



                    if (!running) {

                        break

                    }





                    val data = String(

                        packet.data,

                        0,

                        packet.length,

                        Charsets.UTF_8

                    ).trim()





                    Log.e(

                        "UdpReceiver",

                        "收到UDP ${packet.address.hostAddress}:${packet.port} len=${packet.length}"

                    )





                    if (data.isNotEmpty()) {


                        onData(data)


                    }


                }





            } catch (e: Exception) {


                Log.e(

                    "UdpReceiver",

                    "UDP异常",

                    e

                )



            } finally {


                Log.e(

                    "UdpReceiver",

                    "UDP线程退出"

                )


                close()


            }



        }




        thread?.name = "NetFloat-UDP"


        thread?.start()


    }







    fun stop() {


        Log.e(

            "UdpReceiver",

            "停止UDP"

        )



        running = false



        try {

            socket?.close()

        } catch (_: Exception) {



        }



        socket = null



        thread = null


    }







    private fun close() {


        try {

            socket?.close()

        } catch (_: Exception) {



        }


        socket = null


    }


}
