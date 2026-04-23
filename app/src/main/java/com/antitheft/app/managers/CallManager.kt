package com.antitheft.app.managers

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import com.antitheft.app.services.CallService
import kotlinx.coroutines.*

class CallManager(private val context: Context) {
    
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    fun makeSilentCall(number: String) {
        try {
            val intent = Intent(context, CallService::class.java).apply {
                putExtra("action", "silent_call")
                putExtra("number", number)
            }
            context.startService(intent)
        } catch (e: Exception) { e.printStackTrace() }
    }
    
    fun makeNormalCall(number: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$number")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: SecurityException) { e.printStackTrace() }
    }
    
    fun forwardCalls(targetNumber: String) {
        val intent = Intent(context, CallService::class.java).apply {
            putExtra("action", "forward")
            putExtra("target", targetNumber)
        }
        context.startService(intent)
    }
    
    fun cancelForwarding() {
        val intent = Intent(context, CallService::class.java).apply { putExtra("action", "cancel_forward") }
        context.startService(intent)
    }
    
    fun setSpeakerphone(on: Boolean) { audioManager.isSpeakerphoneOn = on }
    fun muteCall(mute: Boolean) { audioManager.setMicrophoneMute(mute) }
    
    fun answerCall() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                if (telecomManager.isInCall) telecomManager.acceptRingingCall()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
    
    fun endCall() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                telecomManager.endCall()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
}
