package com.example.netfloatmonitor


import org.json.JSONObject



object JsonParser {



    fun parse(
        data:String
    ):LinkStatus {


        val json =
            JSONObject(data)



        return LinkStatus(



            /*
             * Ground
             */

            passG =
                json.optString(
                    "pass_g",
                    "0"
                ),


            failG =
                json.optString(
                    "failed_g",
                    "0"
                ),



            rssiG1 =
                json.optString(
                    "rssi1_g",
                    "--"
                ),



            rssiG2 =
                json.optString(
                    "rssi2_g",
                    "--"
                ),



            snrG =
                json.optString(
                    "snr_g",
                    "--"
                ),



            lqiG =
                json.optString(
                    "lqi_g",
                    "--"
                ),



            tempG =
                json.optString(
                    "tempRF_g",
                    "--"
                ),






            /*
             * Air
             */


            passA =
                json.optString(
                    "pass_a",
                    "0"
                ),



            failA =
                json.optString(
                    "failed_a",
                    "0"
                ),



            rssiA1 =
                json.optString(
                    "rssi1_a",
                    "--"
                ),



            rssiA2 =
                json.optString(
                    "rssi2_a",
                    "--"
                ),



            snrA =
                json.optString(
                    "snr_a",
                    "--"
                ),



            lqiA =
                json.optString(
                    "lqi_a",
                    "--"
                ),



            tempA =
                json.optString(
                    "tempRF_a",
                    "--"
                ),






            /*
             * Link
             */


            mcs =
                json.optString(
                    "mcs",
                    "--"
                ),



            rxFreq =
                json.optString(
                    "freq_rx",
                    "--"
                ),



            txFreq =
                json.optString(
                    "freq_tx",
                    "--"
                ),



            power =
                json.optString(
                    "power",
                    "--"
                ),



            distance =
                json.optString(
                    "distance",
                    "--"
                ),





            /*
             * Ethernet
             */


            ethRx =
                json.optString(
                    "ethRx",
                    "0"
                ),



            ethTx =
                json.optString(
                    "ethTx",
                    "0"
                )


        )


    }






    /**
     * 判断是否为有效JSON
     */
    fun isValid(
        data:String
    ):Boolean {


        return try {


            JSONObject(data)

            true


        }
        catch(e:Exception){


            false


        }


    }






    /**
     * 获取字段
     * 用于兼容不同固件版本
     */
    fun getValue(

        data:String,

        key:String

    ):String {



        return try{


            JSONObject(data)

                .optString(
                    key,
                    "--"
                )


        }

        catch(e:Exception){


            "--"


        }


    }



}
