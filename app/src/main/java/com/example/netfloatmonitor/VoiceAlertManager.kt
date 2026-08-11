package com.example.netfloatmonitor

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class VoiceAlertManager(
    context: Context
) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext

    private var tts: TextToSpeech? = null

    private var initialized = false

    // 同一种告警30秒内只播报一次
    private val cooldownMs = 30_000L

    private val lastAlertTime =
        mutableMapOf<String, Long>()

    // 链路是否已经进入断开状态
    private var linkDisconnected = false

    init {
        tts = TextToSpeech(
            appContext,
            this
        )
    }

    override fun onInit(status: Int) {

        if (status != TextToSpeech.SUCCESS) {

            Log.e(
                "VoiceAlert",
                "TTS初始化失败，status=$status"
            )

            return
        }

        val result =
            tts?.setLanguage(Locale.CHINA)

        initialized =
            result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED

        if (!initialized) {

            Log.e(
                "VoiceAlert",
                "设备不支持中文TTS"
            )

            return
        }

        // 语速
        tts?.setSpeechRate(1.0f)

        // 音调
        tts?.setPitch(1.0f)

        Log.d(
            "VoiceAlert",
            "TTS初始化成功"
        )
    }

    /**
     * 普通告警
     *
     * key：
     * air_rssi
     * gnd_rssi
     * air_snr
     * gnd_snr
     */
    fun speakAlert(
        key: String,
        text: String
    ) {

        if (!initialized) {
            return
        }

        val now =
            System.currentTimeMillis()

        val lastTime =
            lastAlertTime[key] ?: 0L

        // 冷却时间
        if (now - lastTime < cooldownMs) {
            return
        }

        lastAlertTime[key] = now

        Log.d(
            "VoiceAlert",
            "语音播报：$text"
        )

        tts?.speak(
            text,
            TextToSpeech.QUEUE_ADD,
            null,
            "alert_$key"
        )
    }

    /**
     * 链路断开
     *
     * 只播报一次
     */
    fun onLinkDisconnected() {

        if (linkDisconnected) {
            return
        }

        linkDisconnected = true

        if (!initialized) {
            return
        }

        Log.d(
            "VoiceAlert",
            "语音播报：警告，链路已断开"
        )

        tts?.speak(
            "警告，链路已断开",
            TextToSpeech.QUEUE_ADD,
            null,
            "link_disconnected"
        )
    }

    /**
     * 链路恢复
     *
     * 只有之前断开过才播报
     */
    fun onLinkRecovered() {

        if (!linkDisconnected) {
            return
        }

        linkDisconnected = false

        if (!initialized) {
            return
        }

        Log.d(
            "VoiceAlert",
            "语音播报：链路已恢复"
        )

        tts?.speak(
            "链路已恢复",
            TextToSpeech.QUEUE_ADD,
            null,
            "link_recovered"
        )
    }

    /**
     * 重置状态
     */
    fun reset() {

        lastAlertTime.clear()

        linkDisconnected = false
    }

    /**
     * 释放TTS
     */
    fun shutdown() {

        try {

            tts?.stop()

            tts?.shutdown()

        } catch (e: Exception) {

            Log.e(
                "VoiceAlert",
                "TTS关闭异常",
                e
            )
        }

        tts = null

        initialized = false
    }
}
