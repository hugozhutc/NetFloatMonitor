package com.example.netfloatmonitor


data class LinkStatus(


    // ground

    var passG:String="0",
    var failG:String="0",

    var rssiG1:String="--",
    var rssiG2:String="--",

    var snrG:String="--",

    var lqiG:String="--",

    var tempG:String="--",



    // air

    var passA:String="0",
    var failA:String="0",

    var rssiA1:String="--",
    var rssiA2:String="--",

    var snrA:String="--",

    var lqiA:String="--",

    var tempA:String="--",



    // link

    var mcs:String="--",

    var rxFreq:String="--",

    var txFreq:String="--",

    var power:String="--",

    var distance:String="--",



    var ethRx:String="0",

    var ethTx:String="0"

)
