package com.example.netfloatmonitor


import org.json.JSONObject



object JsonParser {


    fun parse(json:String):LinkStatus{


        val obj =
            JSONObject(json)



        return LinkStatus(


            airRssi1 =
                obj.optString(
                    "rssi1_a"
                ),


            airRssi2 =
                obj.optString(
                    "rssi2_a"
                ),


            airSnr =
                obj.optString(
                    "snr_a"
                ),


            airPass =
                obj.optString(
                    "pass_a"
                ),


            airFailed =
                obj.optString(
                    "failed_a"
                ),


            airAnt =
                obj.optString(
                    "ant_a"
                ),



            gndRssi1 =
                obj.optString(
                    "rssi1_g"
                ),


            gndRssi2 =
                obj.optString(
                    "rssi2_g"
                ),


            gndSnr =
                obj.optString(
                    "snr_g"
                ),


            gndPass =
                obj.optString(
                    "pass_g"
                ),


            gndFailed =
                obj.optString(
                    "failed_g"
                ),


            gndAnt =
                obj.optString(
                    "ant_g"
                ),



            freq =
                obj.optString(
                    "freq_tx"
                ),



            mcs =
                obj.optString(
                    "mcs"
                ),



            power =
                obj.optString(
                    "power"
                ),



            distance =
                obj.optString(
                    "distance"
                ),



            txRate =
                obj.optString(
                    "ethTx"
                ),


            rxRate =
                obj.optString(
                    "ethRx"
                ),



            airNoise =
                obj.optString(
                    "noiseFloor_a"
                )
                .split(",")
                .toTypedArray(),



            gndNoise =
                obj.optString(
                    "noiseFloor_g"
                )
                .split(",")
                .toTypedArray()


        )

    }

}
