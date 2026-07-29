
package com.example.netfloatmonitor.data


import android.util.Log


class DataProcessor {


    private var lastStatus =
        LinkStatus()



    /**
     * 处理收到的JSON数据
     */
    fun process(json:String):LinkStatus? {


        return try {


            val status =
                JsonParser.parse(json)



            //计算链路质量

            status.calculateQuality()



            //计算丢包率

            status.lossRate =
                calculateLossRate(status)



            status.updateTime =
                System.currentTimeMillis()



            lastStatus =
                status



            status


        }catch(e:Exception){


            Log.e(
                "DataProcessor",
                "JSON处理失败:${e.message}"
            )


            null

        }


    }





    /**
     * 获取最近一次状态
     */
    fun getLastStatus():LinkStatus{


        return lastStatus

    }





    /**
     * 丢包率计算
     *
     * failed/(pass+failed)
     */
    private fun calculateLossRate(
        status:LinkStatus
    ):Float{


        return try{


            val pass =
                status.airPass.toFloat()


            val failed =
                status.airFailed.toFloat()



            if(pass+failed<=0){

                0f

            }else{


                failed /
                (pass+failed)
                *
                100f


            }


        }catch(e:Exception){


            0f

        }


    }


}
