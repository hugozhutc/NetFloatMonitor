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



    private var thread:Thread? = null







    fun start(){


        if(running)
            return



        running=true



        thread = Thread {


            try {



                socket = DatagramSocket(

                    null

                ).apply {


                    reuseAddress=true


                    bind(

                        InetSocketAddress(
                            port
                        )

                    )

                }




                Log.d(

                    "UdpReceiver",

                    "UDP监听启动 port=$port"

                )






                val buffer =

                    ByteArray(
                        8192
                    )





                while(running){



                    val packet =

                        DatagramPacket(

                            buffer,

                            buffer.size

                        )




                    socket?.receive(

                        packet

                    )





                    if(!running)
                        break






                    val data =

                        String(

                            packet.data,

                            0,

                            packet.length,

                            Charsets.UTF_8

                        ).trim()





                    Log.d(

                        "UdpReceiver",

                        "RX ${packet.address.hostAddress} len=${packet.length}"

                    )





                    if(data.isNotEmpty()){


                        onData(data)


                    }



                }





            }catch(e:Exception){



                if(running){


                    Log.e(

                        "UdpReceiver",

                        "UDP异常:${e.message}"

                    )


                }


            }finally{


                close()


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


        thread=null


    }









    private fun close(){


        try{


            socket?.close()


        }catch(_:Exception){}


        socket=null


    }


}
