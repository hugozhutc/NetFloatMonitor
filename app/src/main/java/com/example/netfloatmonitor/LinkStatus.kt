package com.example.netfloatmonitor


data class LinkStatus(

    var rssiG1:String="--",
    var rssiG2:String="--",
    var snrG:String="--",
    var lqiG:String="--",
    var tempG:String="--",


    var rssiA1:String="--",
    var rssiA2:String="--",
    var snrA:String="--",
    var lqiA:String="--",
    var tempA:String="--",


    var mcs:String="--",
    var rxFreq:String="--",
    var txFreq:String="--",
    var power:String="--",
    var distance:String="--"

)
