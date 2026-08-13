package com.example.netfloatmonitor

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var ipEdit: EditText
    private lateinit var portEdit: EditText
    private lateinit var logPath: TextView
    private lateinit var logManager: LogManager
    private lateinit var tvStatusInfo: TextView

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            
            val isStopped = intent.getBooleanExtra("IS_STOPPED", false)
            if (isStopped) {
                tvStatusInfo.text = "链路状态: 已停止\n悬浮窗: 已关闭\n当前文件: 未开启监控\n已收数据: 0 包 | 速率: 0 Hz"
                return
            }

            val total = intent.getIntExtra("TOTAL_PACKETS", 0)
            val hz = intent.getIntExtra("HZ", 0)
            val isFloatShowing = intent.getBooleanExtra("IS_FLOAT_SHOWING", false)
            val currentFile = logManager.getCurrentFileName()

            val floatStateStr = if (isFloatShowing) "显示中" else "已隐藏"

            tvStatusInfo.text = """
                链路状态: 正在监听...
                悬浮窗状态: $floatStateStr
                当前文件: $currentFile
                已收数据: $total 包 | 速率: $hz Hz
            """.trimIndent()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logManager = LogManager(this)

        ipEdit = findViewById(R.id.editIp)
        portEdit = findViewById(R.id.editPort)
        logPath = findViewById(R.id.logPath)
        tvStatusInfo = findViewById(R.id.tvStatusInfo)

        val startBtn = findViewById<Button>(R.id.startBtn)
        val stopBtn = findViewById<Button>(R.id.stopBtn)
        val clearBtn = findViewById<Button>(R.id.clearBtn)
        val toggleFloatBtn = findViewById<Button?>(R.id.toggleFloatBtn)

        loadConfig()
        showLogPath()
        
        // 检查电池优化白名单与通知权限
        checkBatteryOptimization()
        checkNotificationPermission()

        tvStatusInfo.text = "链路状态: 待机\n悬浮窗: 未启动\n当前文件: 未开启监控\n已收数据: 0 包 | 速率: 0 Hz"

        startBtn.setOnClickListener {
            saveConfig()

            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                Toast.makeText(this, "请开启悬浮窗权限", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val port = portEdit.text.toString().toIntOrNull() ?: 16789

            logManager.startNewSession()
            val previewFile = logManager.getCurrentFileName()
            tvStatusInfo.text = """
                链路状态: 正在初始化...
                当前文件: $previewFile
                已收数据: 0 包 | 速率: 0 Hz
            """.trimIndent()

            val serviceIntent = Intent(this, FloatService::class.java).apply {
                putExtra("PORT", port)
                putExtra("IP", ipEdit.text.toString())
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            Toast.makeText(this, "UDP监听启动 端口:$port", Toast.LENGTH_SHORT).show()
        }

        stopBtn.setOnClickListener {
            stopService(Intent(this, FloatService::class.java))
            Toast.makeText(this, "监听已停止，CSV表格已封存", Toast.LENGTH_SHORT).show()
        }

        clearBtn.setOnClickListener {
            clearLog()
        }

        // 切换悬浮窗显隐（Activity 在台前时直接调用 startService 发送 Action）
        toggleFloatBtn?.setOnClickListener {
            val intent = Intent(this, FloatService::class.java).apply {
                action = FloatService.ACTION_TOGGLE_FLOAT
            }
            startService(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            statusReceiver, 
            IntentFilter("com.example.netfloatmonitor.STATUS_UPDATE")
        )
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusReceiver)
    }

    // Android 13 (API 33) 动态申请 POST_NOTIFICATIONS 权限
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    // 申请忽略电池优化白名单，防止后台被系统杀掉
    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    @SuppressLint("BatteryLife")
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    // 部分定制 ROM 忽略无对应 Activity 产生的异常
                }
            }
        }
    }

    private fun saveConfig() {
        getSharedPreferences("net_config", Context.MODE_PRIVATE)
            .edit()
            .putString("ip", ipEdit.text.toString())
            .putString("port", portEdit.text.toString())
            .apply()
    }

    private fun loadConfig() {
        val sp = getSharedPreferences("net_config", Context.MODE_PRIVATE)
        ipEdit.setText(sp.getString("ip", "192.168.144.33"))
        portEdit.setText(sp.getString("port", "16789"))
    }

    private fun showLogPath() {
        logPath.text = "日志目录:\n${logManager.getLogPath()}"
    }

    private fun clearLog() {
        val files: List<File> = logManager.getLogFiles()
        var deletedCount = 0
        
        files.forEach { file ->
            if (file.exists() && file.delete()) {
                deletedCount++
            }
        }

        Toast.makeText(
            this,
            if (deletedCount > 0) "已成功清除 $deletedCount 个历史CSV表格" else "没有需要清除的历史数据",
            Toast.LENGTH_SHORT
        ).show()
    }
}
