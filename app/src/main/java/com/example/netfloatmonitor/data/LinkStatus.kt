package com.example.netfloatmonitor.data


data class LinkStatus(


    // ==========================
    // AIR 天空端
    // ==========================

    var airRssi1: String = "--",

    var airRssi2: String = "--",

    var airSnr: String = "--",

    var airPass: String = "0",

    var airFailed: String = "0",

    var airAnt: String = "--",




    // ==========================
    // GROUND 地面端
    // ==========================

    var gndRssi1: String = "--",

    var gndRssi2: String = "--",

    var gndSnr: String = "--",

    var gndPass: String = "0",

    var gndFailed: String = "0",

    var gndAnt: String = "--",




    // ==========================
    // 公共链路参数
    // ==========================

    var freq: String = "--",

    var mcs: String = "--",

    var power: String = "--",

    var distance: String = "0",


    var txRate: String = "0",

    var rxRate: String = "0",




    // ==========================
    // 底噪
    // ==========================

    var airNoise: Array<String> = emptyArray(),

    var gndNoise: Array<String> = emptyArray(),




    // ==========================
    // 新增扩展字段
    // ==========================

    // 数据来源IP
    var sourceIp: String = "--",


    // 接收时间
    var timestamp: String = "--",


    // 数据包统计
    var packetCount: Long = 0,


    // 链路质量百分比
    var quality: Int = 0,


    // 在线状态
    var online: Boolean = false


)
