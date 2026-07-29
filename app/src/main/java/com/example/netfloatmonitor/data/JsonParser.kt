package com.example.netfloatmonitor.data


import org.json.JSONObject



object JsonParser {



    fun parse(json:String):LinkStatus{


        val obj =
            JSONObject(json)



        return LinkStatus(



            // =====================
            // AIR
            // =====================


            airRssi1 =
                getValue(
                    obj,
                    "rssi1_a",
                    "air_rssi1",
                    "airRssi1"
                ),


            airRssi2 =
                getValue(
                    obj,
                    "rssi2_a",
                    "air_rssi2",
                    "airRssi2"
                ),


            airSnr =
                getValue(
                    obj,
                    "snr_a",
                    "air_snr",
                    "airSnr"
                ),


            airPass =
                getValue(
                    obj,
                    "pass_a",
                    "air_pass"
                ),


            airFailed =
                getValue(
                    obj,
                    "failed_a",
                    "air_failed"
                ),


            airAnt =
                getValue(
                    obj,
                    "ant_a",
                    "air_ant"
                ),





            // =====================
            // GROUND
            // =====================


            gndRssi1 =
                getValue(
                    obj,
                    "rssi1_g",
                    "ground_rssi1",
                    "gnd_rssi1"
                ),


            gndRssi2 =
                getValue(
                    obj,
                    "rssi2_g",
                    "ground_rssi2",
                    "gnd_rssi2"
                ),


            gndSnr =
                getValue(
                    obj,
                    "snr_g",
                    "ground_snr",
                    "gnd_snr"
                ),


            gndPass =
                getValue(
                    obj,
                    "pass_g",
                    "ground_pass"
                ),


            gndFailed =
                getValue(
                    obj,
                    "failed_g",
                    "ground_failed"
                ),


            gndAnt =
                getValue(
                    obj,
                    "ant_g",
                    "ground_ant"
                ),





            // =====================
            // LINK
            // =====================


            freq =
                getValue(
                    obj,
                    "freq_tx",
                    "freq",
                    "frequency"
                ),



            mcs =
                getValue(
                    obj,
                    "mcs"
                ),



            power =
                getValue(
                    obj,
                    "power",
                    "tx_power"
                ),



            distance =
                getValue(
                    obj,
                    "distance",
                    "dist"
                ),



            txRate =
                getValue(
                    obj,
                    "ethTx",
                    "txRate"
                ),



            rxRate =
                getValue(
                    obj,
                    "ethRx",
                    "rxRate"
                ),




            // =====================
            // NOISE
            // =====================


            airNoise =
                parseNoise(
                    getValue(
                        obj,
                        "noiseFloor_a"
                    )
                ),



            gndNoise =
                parseNoise(
                    getValue(
                        obj,
                        "noiseFloor_g"
                    )
                )



        )


    }







    /**
     * 多字段匹配
     */
    private fun getValue(
        obj:JSONObject,
        vararg keys:String
    ):String{


        val names =
            obj.keys()


        while(names.hasNext()){


            val jsonKey =
                names.next()



            for(key in keys){


                if(
                    jsonKey.equals(
                        key,
                        ignoreCase = true
                    )
                ){


                    return obj
                        .opt(jsonKey)
                        ?.toString()
                        ?: "--"

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
        ){

            return emptyArray()

        }



        return value
            .split(",")
            .toTypedArray()


    }



}
