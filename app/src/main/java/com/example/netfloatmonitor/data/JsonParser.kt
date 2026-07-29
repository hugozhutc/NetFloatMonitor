package com.example.netfloatmonitor.data


import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object JsonParser {


    fun parse(
        json: String
    ): LinkStatus {


        val status = LinkStatus()



        try {


            val obj =
                JSONObject(json)



            // =========================
            // AIR
            // =========================

            status.airRssi1 =
                getValue(
                    obj,
                    "rssi1_a",
                    "air_rssi1"
                )


            status.airRssi2 =
                getValue(
                    obj,
                    "rssi2_a",
                    "air_rssi2"
                )


            status.airSnr =
                getValue(
                    obj,
                    "snr_a",
                    "air_snr"
                )


            status.airPass =
                getValue(
                    obj,
                    "pass_a",
                    "air_pass"
                )


            status.airFailed =
                getValue(
                    obj,
                    "failed_a",
                    "air_failed"
                )


            status.airAnt =
                getValue(
                    obj,
                    "ant_a",
                    "air_ant"
                )






            // =========================
            // GND
            // =========================


            status.gndRssi1 =
                getValue(
                    obj,
                    "rssi1_g",
                    "gnd_rssi1"
                )


            status.gndRssi2 =
                getValue(
                    obj,
                    "rssi2_g",
                    "gnd_rssi2"
                )


            status.gndSnr =
                getValue(
                    obj,
                    "snr_g",
                    "gnd_snr"
                )


            status.gndPass =
                getValue(
                    obj,
                    "pass_g",
                    "gnd_pass"
                )


            status.gndFailed =
                getValue(
                    obj,
                    "failed_g",
                    "gnd_failed"
                )


            status.gndAnt =
                getValue(
                    obj,
                    "ant_g",
                    "gnd_ant"
                )








            // =========================
            // 公共参数
            // =========================


            status.freq =
                getValue(
                    obj,
                    "freq_tx",
                    "freq",
                    "frequency"
                )



            status.mcs =
                getValue(
                    obj,
                    "mcs",
                    "rate"
                )



            status.power =
                getValue(
                    obj,
                    "power",
                    "txPower"
                )



            status.distance =
                getValue(
                    obj,
                    "distance",
                    "dist"
                )



            status.txRate =
                getValue(
                    obj,
                    "ethTx",
                    "txRate"
                )



            status.rxRate =
                getValue(
                    obj,
                    "ethRx",
                    "rxRate"
                )







            // =========================
            // 噪声
            // =========================


            status.airNoise =
                parseNoise(
                    getValue(
                        obj,
                        "noiseFloor_a"
                    )
                )



            status.gndNoise =
                parseNoise(
                    getValue(
                        obj,
                        "noiseFloor_g"
                    )
                )








            // =========================
            // 扩展字段
            // =========================


            status.timestamp =
                now()



            status.online =
                true



            status.quality =
                calculateQuality(
                    status
                )



        } catch(e:Exception){


            status.online=false


        }



        return status


    }









    private fun getValue(

        obj:JSONObject,

        vararg keys:String

    ):String {


        for(key in keys){


            if(obj.has(key)){


                return obj.optString(

                    key,

                    "--"

                )


            }


        }


        return "--"

    }









    private fun parseNoise(

        value:String

    ):Array<String>{



        if(

            value.isBlank()

            ||

            value=="--"

        ){

            return emptyArray()

        }



        return value.split(",")

            .toTypedArray()


    }









    private fun calculateQuality(

        status:LinkStatus

    ):Int{


        val snr =

            status.airSnr

                .toFloatOrNull()

                ?:0f



        return when{


            snr >= 30 -> 100


            snr >= 20 -> 80


            snr >= 10 -> 60


            snr > 0 -> 40


            else -> 0


        }


    }









    private fun now():String{


        return SimpleDateFormat(

            "HH:mm:ss.SSS",

            Locale.getDefault()

        )

            .format(

                Date()

            )


    }


}
