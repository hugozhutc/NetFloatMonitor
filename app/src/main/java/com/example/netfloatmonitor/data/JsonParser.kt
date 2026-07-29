package com.example.netfloatmonitor.data


import org.json.JSONObject



object JsonParser {



    fun parse(
        json:String
    ):LinkStatus{


        val obj =
            JSONObject(json)



        val status =
            LinkStatus()



        status.rawJson =
            json





        // =====================
        // AIR
        // =====================


        status.airRssi1 =
            find(
                obj,
                "rssi1_a",
                "air_rssi1",
                "airRssi1"
            )


        status.airRssi2 =
            find(
                obj,
                "rssi2_a",
                "air_rssi2",
                "airRssi2"
            )


        status.airSnr =
            find(
                obj,
                "snr_a",
                "air_snr",
                "airSnr"
            )


        status.airPass =
            find(
                obj,
                "pass_a",
                "air_pass",
                "airPass"
            )



        status.airFailed =
            find(
                obj,
                "failed_a",
                "air_failed",
                "airFailed"
            )



        status.airAnt =
            find(
                obj,
                "ant_a",
                "air_ant",
                "airAnt"
            )









        // =====================
        // GROUND
        // =====================


        status.gndRssi1 =
            find(
                obj,
                "rssi1_g",
                "gnd_rssi1",
                "ground_rssi1",
                "gndRssi1"
            )



        status.gndRssi2 =
            find(
                obj,
                "rssi2_g",
                "gnd_rssi2",
                "ground_rssi2",
                "gndRssi2"
            )



        status.gndSnr =
            find(
                obj,
                "snr_g",
                "gnd_snr",
                "ground_snr",
                "gndSnr"
            )



        status.gndPass =
            find(
                obj,
                "pass_g",
                "gnd_pass",
                "gndPass"
            )



        status.gndFailed =
            find(
                obj,
                "failed_g",
                "gnd_failed",
                "gndFailed"
            )


        status.gndAnt =
            find(
                obj,
                "ant_g",
                "gnd_ant",
                "gndAnt"
            )









        // =====================
        // 公共参数
        // =====================


        status.freq =
            find(
                obj,
                "freq_tx",
                "freq",
                "frequency"
            )



        status.mcs =
            find(
                obj,
                "mcs",
                "MCS"
            )



        status.power =
            find(
                obj,
                "power",
                "txPower"
            )



        status.distance =
            find(
                obj,
                "distance",
                "dist"
            )



        status.txRate =
            find(
                obj,
                "ethTx",
                "txRate",
                "tx_rate"
            )



        status.rxRate =
            find(
                obj,
                "ethRx",
                "rxRate",
                "rx_rate"
            )









        // =====================
        // 噪声
        // =====================


        status.airNoise =
            parseNoise(
                find(
                    obj,
                    "noiseFloor_a",
                    "airNoise"
                )
            )


        status.gndNoise =
            parseNoise(
                find(
                    obj,
                    "noiseFloor_g",
                    "gndNoise"
                )
            )







        // =====================
        // 计算链路质量
        // =====================


        status.linkQuality =
            calculateQuality(
                status.airSnr
            )




        status.lossRate =
            calculateLoss(
                status.airPass,
                status.airFailed
            )




        return status


    }









    private fun find(
        obj:JSONObject,
        vararg keys:String
    ):String{


        for(
            key in keys
        ){


            if(
                obj.has(key)
            ){


                return obj.optString(
                    key,
                    "--"
                )


            }



            //忽略大小写匹配

            val iterator =
                obj.keys()


            while(
                iterator.hasNext()
            ){


                val realKey =
                    iterator.next()



                if(
                    realKey.equals(
                        key,
                        true
                    )
                ){


                    return obj.optString(
                        realKey,
                        "--"
                    )

                }

            }


        }


        return "--"


    }









    private fun parseNoise(
        value:String
    ):Array<String>{


        if(
            value=="--" ||
            value.isEmpty()
        )
            return emptyArray()



        return value
            .split(",")
            .toTypedArray()

    }









    private fun calculateQuality(
        snr:String
    ):Int{


        val value =
            snr.toFloatOrNull()
                ?: return 0



        return when{


            value>=30 ->
                100


            value>=20 ->
                80


            value>=10 ->
                60


            value>=5 ->
                40


            else ->
                20


        }


    }









    private fun calculateLoss(
        pass:String,
        fail:String
    ):Float{


        val p =
            pass.toFloatOrNull()
                ?:0f



        val f =
            fail.toFloatOrNull()
                ?:0f




        if(
            p+f<=0
        )
            return 0f




        return f /
                (p+f)
                *
                100f


    }



}
