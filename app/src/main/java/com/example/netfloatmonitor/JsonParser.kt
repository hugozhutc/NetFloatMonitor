package com.example.netfloatmonitor

import org.json.JSONObject


object JsonParser {


    fun parse(data:String):LinkStatus{


        val json =
            JSONObject(data)


        return LinkStatus(


            rssiG1 =
            json.optString("rssi1_g"),


            rssiG2 =
            json.optString("rssi2_g"),


            snrG =
            json.optString("snr_g"),


            lqiG =
            json.optString("lqi_g"),


            tempG =
            json.optString("tempRF_g"),



            rssiA1 =
            json.optString("rssi1_a"),


            rssiA2 =
            json.optString("rssi2_a"),


            snrA =
            json.optString("snr_a"),


            lqiA =
            json.optString("lqi_a"),


            tempA =
            json.optString("tempRF_a"),



            mcs =
            json.optString("mcs"),


            rxFreq =
            json.optString("freq_rx"),


            txFreq =
            json.optString("freq_tx"),


            power =
            json.optString("power"),


            distance =
            json.optString("distance")

        )


    }

}
