package com.example.netfloatmonitor


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity



class MainActivity : AppCompatActivity(){


override fun onCreate(savedInstanceState: Bundle?) {

super.onCreate(savedInstanceState)


setContentView(R.layout.activity_main)



val ipEdit =
findViewById<EditText>(R.id.ipEdit)


val portEdit =
findViewById<EditText>(R.id.portEdit)



val start =
findViewById<Button>(R.id.startBtn)


val stop =
findViewById<Button>(R.id.stopBtn)


val floatBtn =
findViewById<Button>(R.id.floatBtn)


val status =
findViewById<TextView>(R.id.statusText)



floatBtn.setOnClickListener{


if(!Settings.canDrawOverlays(this)){


val intent =
Intent(
Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
Uri.parse(
"package:$packageName"
)
)


startActivity(intent)


}

else{

Toast.makeText(
this,
"悬浮窗权限已开启",
Toast.LENGTH_SHORT
).show()

}


}




start.setOnClickListener{


val ip =
ipEdit.text.toString()


val port =
portEdit.text.toString()



val intent =
Intent(
this,
FloatService::class.java
)


intent.putExtra(
"IP",
ip
)


intent.putExtra(
"PORT",
port.toInt()
)



startForegroundService(intent)



status.text =
"状态：监听中"


}




stop.setOnClickListener{


stopService(
Intent(
this,
FloatService::class.java
)
)


status.text =
"状态：停止"


}



}



}
