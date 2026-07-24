package com.example.netfloatmonitor


data class LinkStatus(

    // AIR天空端
    var airRssi1:String = "-0",
    var airRssi2:String = "-0",
    var airSnr:String = "0",
    var airPass:String = "0",
    var airFailed:String = "0",
    var airAnt:String = "",


    // Ground地面端
    var gndRssi1:String = "-0",
    var gndRssi2:String = "-0",
    var gndSnr:String = "0",
    var gndPass:String = "0",
    var gndFailed:String = "0",
    var gndAnt:String = "",


    // 公共链路
    var freq:String = "",
    var mcs:String = "",
    var power:String = "",
    var distance:String = "",

    var txRate:String = "",
    var rxRate:String = "",


    // 噪声

    var airNoise:Array<String> = arrayOf(),

    var gndNoise:Array<String> = arrayOf()


)
