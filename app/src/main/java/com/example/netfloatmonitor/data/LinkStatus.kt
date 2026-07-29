package com.example.netfloatmonitor.data


data class LinkStatus(

    // =========================
    // AIR 天空端
    // =========================

    var airRssi1: String = "--",

    var airRssi2: String = "--",

    var airSnr: String = "--",

    var airPass: String = "0",

    var airFailed: String = "0",

    var airAnt: String = "--",


    // =========================
    // GROUND 地面端
    // =========================

    var gndRssi1: String = "--",

    var gndRssi2: String = "--",

    var gndSnr: String = "--",

    var gndPass: String = "0",

    var gndFailed: String = "0",

    var gndAnt: String = "--",


    // =========================
    // 公共链路参数
    // =========================

    var freq: String = "--",

    var mcs: String = "--",

    var power: String = "--",

    var distance: String = "0",


    var txRate: String = "0",

    var rxRate: String = "0",


    // =========================
    // 噪声
    // =========================

    var airNoise: Array<String> = emptyArray(),

    var gndNoise: Array<String> = emptyArray(),



    // =========================
    // V2新增状态
    // =========================

    /**
     * 链路质量百分比
     * 0-100
     */
    var linkQuality:Int = 0,


    /**
     * 丢包率 %
     */
    var lossRate:Float = 0f,


    /**
     * 数据更新时间
     */
    var updateTime:Long = System.currentTimeMillis()



){



    /**
     * RSSI转换
     *
     * 原始:
     * -65
     *
     * UI:
     * 65
     */
    fun airRssiValue():Int{

        return parseNumber(airRssi1)

    }



    fun gndRssiValue():Int{

        return parseNumber(gndRssi1)

    }



    fun airSnrValue():Int{

        return parseNumber(airSnr)

    }



    fun gndSnrValue():Int{

        return parseNumber(gndSnr)

    }



    private fun parseNumber(value:String):Int{

        return try {

            kotlin.math.abs(
                value.toFloat().toInt()
            )

        }catch(e:Exception){

            0

        }

    }




    /**
     * 自动计算链路质量
     */
    fun calculateQuality(){


        val rssi =
            airRssiValue()


        val snr =
            airSnrValue()



        var score = 0



        // RSSI评分
        score += when{

            rssi >= 80 -> 50

            rssi >= 60 -> 40

            rssi >= 40 -> 25

            else -> 10

        }



        // SNR评分

        score += when{

            snr >=30 -> 50

            snr >=20 -> 40

            snr >=10 -> 25

            else ->10

        }



        linkQuality =
            score.coerceIn(0,100)

    }


}
