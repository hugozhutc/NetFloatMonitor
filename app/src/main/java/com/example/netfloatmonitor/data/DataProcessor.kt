package com.example.netfloatmonitor.data


import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



object DataProcessor {



    private var totalPacket = 0L


    private var failedPacket = 0L





    fun process(

        status: LinkStatus

    ): LinkStatus {



        totalPacket++



        updateTime(status)



        status.quality =

            calculateQuality(
                status
            )



        status.online =

            checkOnline(
                status
            )



        return status

    }









    private fun calculateQuality(

        status: LinkStatus

    ):Int {



        val snr =

            getBestSnr(
                status
            )



        val rssi =

            getBestRssi(
                status
            )





        var score = 0




        // SNR评分

        score += when {


            snr >= 30 -> 60


            snr >= 20 -> 45


            snr >= 10 -> 30


            snr > 0 -> 15


            else -> 0


        }





        // RSSI评分

        score += when {


            rssi >= -50 -> 40


            rssi >= -70 -> 30


            rssi >= -85 -> 20


            rssi > -100 -> 10


            else -> 0


        }




        return score.coerceIn(
            0,
            100
        )


    }









    private fun getBestSnr(

        status:LinkStatus

    ):Float {



        return listOf(

            status.airSnr,

            status.gndSnr

        )

            .mapNotNull {

                it.toFloatOrNull()

            }

            .maxOrNull()
            ?:0f


    }









    private fun getBestRssi(

        status:LinkStatus

    ):Float {



        return listOf(

            status.airRssi1,

            status.airRssi2,

            status.gndRssi1,

            status.gndRssi2

        )

            .mapNotNull {

                it.toFloatOrNull()

            }

            .maxOrNull()
            ?: -120f


    }









    private fun checkOnline(

        status:LinkStatus

    ):Boolean {



        return (

            status.airRssi1 != "--"

            ||

            status.gndRssi1 != "--"

        )


    }









    private fun updateTime(

        status:LinkStatus

    ){



        status.timestamp =

            SimpleDateFormat(

                "HH:mm:ss.SSS",

                Locale.getDefault()

            )

                .format(
                    Date()
                )


    }









    fun getPacketCount():Long{


        return totalPacket


    }









    fun getLossRate():Float{



        if(totalPacket==0L)

            return 0f



        return (

            failedPacket.toFloat()

            /

            totalPacket.toFloat()

        ) * 100f


    }









    fun reset(){


        totalPacket=0


        failedPacket=0


    }


}
