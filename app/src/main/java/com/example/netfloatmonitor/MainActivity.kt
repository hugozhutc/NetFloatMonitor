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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.netfloatmonitor.R

class MainActivity : AppCompatActivity() {

    private lateinit var etPort: EditText
    private lateinit var btnToggleService: Button
    private lateinit var switchChartAir: SwitchCompat
    private lateinit var switchChartGnd: SwitchCompat
    private lateinit var tvStatus: TextView

    private var isServiceRunning = false

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isStopped = intent?.getBooleanExtra("IS_STOPPED", false) ?: false
            if (isStopped) {
                isServiceRunning = false
                btnToggleService.text = "启动悬浮窗服务"
                tvStatus.text = "状态：已停止"
                return
            }

            val totalPackets = intent?.getIntExtra("TOTAL_PACKETS", 0) ?: 0
            val hz = intent?.getIntExtra("HZ", 0) ?: 0
            isServiceRunning = true
            btnToggleService.text = "停止悬浮窗服务"
            tvStatus.text = "状态：运行中 | 总包数: $totalPackets | 速率: ${hz}Hz"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 修复点 1：传入 savedInstanceState 参数
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
        etPort = findViewById(R.id.etPort)
        btnToggleService = findViewById(R.id.btnToggleService)
        switchChartAir = findViewById(R.id.switchChartAir)
        switchChartGnd = findViewById(R.id.switchChartGnd)
        tvStatus = findViewById(R.id.tvStatus)
    }

    private fun setupListeners() {
        btnToggleService.setOnClickListener {
            if (isServiceRunning) {
                stopFloatService()
            } else {
                startFloatService()
            }
        }

        switchChartAir.setOnCheckedChangeListener { _, isChecked ->
            saveConfig("SHOW_AIR_CHART", isChecked)
            notifyConfigChanged()
        }

        switchChartGnd.setOnCheckedChangeListener { _, isChecked ->
            saveConfig("SHOW_GND_CHART", isChecked)
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

        val portStr = etPort.text.toString().trim()
        val port = if (portStr.isNotEmpty()) portStr.toInt() else 16789

        val intent = Intent(this, FloatService::class.java).apply {
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
