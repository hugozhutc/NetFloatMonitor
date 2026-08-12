package com.example.netfloatmonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity() {

    private lateinit var editIp: EditText
    private lateinit var editPort: EditText
    private lateinit var switchFloat: Switch
    private lateinit var switchChart: Switch
    private lateinit var switchLogging: Switch
    private lateinit var startBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var clearBtn: Button
    private lateinit var tvStatusInfo: TextView
    private lateinit var logPath: TextView

    private var isServiceRunning = false

    // 接收来自 FloatService 的状态广播更新 UI 面板
    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isStopped = intent?.getBooleanExtra("IS_STOPPED", false) ?: false
            if (isStopped) {
                isServiceRunning = false
                tvStatusInfo.text = "链路状态: 待机\n当前文件: 未开启监控\n已收数据: 0 包 | 速率: 0 Hz"
                return
            }

            val totalPackets = intent?.getIntExtra("TOTAL_PACKETS", 0) ?: 0
            val hz = intent?.getIntExtra("HZ", 0) ?: 0
            isServiceRunning = true
            tvStatusInfo.text = "链路状态: 接收中\n当前文件: 实时日志记录中\n已收数据: $totalPackets 包 | 速率: ${hz} Hz"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        checkOverlayPermission()
        setupListeners()

        LocalBroadcastManager.getInstance(this).registerReceiver(
            statusReceiver,
            IntentFilter("com.example.netfloatmonitor.STATUS_UPDATE")
        )
    }

    private fun initViews() {
        editIp = findViewById(R.id.editIp)
        editPort = findViewById(R.id.editPort)
        switchFloat = findViewById(R.id.switchFloat)
        switchChart = findViewById(R.id.switchChart)
        switchLogging = findViewById(R.id.switchLogging)
        startBtn = findViewById(R.id.startBtn)
        stopBtn = findViewById(R.id.stopBtn)
        clearBtn = findViewById(R.id.clearBtn)
        tvStatusInfo = findViewById(R.id.tvStatusInfo)
        logPath = findViewById(R.id.logPath)
    }

    private fun setupListeners() {
        startBtn.setOnClickListener {
            startFloatService()
        }

        stopBtn.setOnClickListener {
            stopFloatService()
        }

        clearBtn.setOnClickListener {
            Toast.makeText(this, "缓存日志已清理", Toast.LENGTH_SHORT).show()
        }

        // 悬浮窗显示/隐藏开关
        switchFloat.setOnCheckedChangeListener { _, isChecked ->
            saveConfig("SHOW_FLOAT_WINDOW", isChecked)
            notifyConfigChanged()
        }

        // 实时图表开关
        switchChart.setOnCheckedChangeListener { _, isChecked ->
            saveConfig("SHOW_CHART", isChecked)
            notifyConfigChanged()
        }

        // CSV 日志导出开关
        switchLogging.setOnCheckedChangeListener { _, isChecked ->
            saveConfig("ENABLE_LOGGING", isChecked)
            notifyConfigChanged()
        }
    }

    private fun notifyConfigChanged() {
        val intent = Intent("com.example.netfloatmonitor.CONFIG_CHANGED")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun saveConfig(key: String, value: Boolean) {
        val sp = getSharedPreferences("net_float_config", Context.MODE_PRIVATE)
        sp.edit().putBoolean(key, value).apply()
    }

    private fun startFloatService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show()
            checkOverlayPermission()
            return
        }

        val ip = editIp.text.toString().trim()
        val portStr = editPort.text.toString().trim()
        val port = if (portStr.isNotEmpty()) portStr.toInt() else 14550

        val intent = Intent(this, FloatService::class.java).apply {
            putExtra("IP", ip)
            putExtra("PORT", port)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopFloatService() {
        val intent = Intent(this, FloatService::class.java)
        stopService(intent)
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusReceiver)
    }
}
