package com.antitheft.app.services

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.antitheft.app.R
import com.antitheft.app.managers.CameraManager
import com.antitheft.app.managers.DataManager
import com.antitheft.app.managers.LocationManager
import com.antitheft.app.utils.Constants
import kotlinx.coroutines.*

class StealthService : Service() {
    
    private lateinit var powerManager: PowerManager
    private lateinit var wakeLock: PowerManager.WakeLock
    private var mediaPlayer: MediaPlayer? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isAlarmActive = false
    
    override fun onCreate() {
        super.onCreate()
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        startForegroundService()
        acquireWakeLock()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("action")?.let { action ->
            when (action) {
                "alarm" -> startAlarm()
                "stop_alarm" -> stopAlarm()
                "photo" -> capturePhoto(intent.getStringExtra("sender") ?: "")
                "location" -> getLocation(intent.getStringExtra("sender") ?: "")
            }
        }
        return START_STICKY
    }
    
    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("")
            .setContentText("")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
        startForeground(Constants.NOTIFICATION_ID, notification)
    }
    
    private fun acquireWakeLock() {
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AntiTheft::WakeLock")
        wakeLock.acquire(10 * 60 * 1000L)
    }
    
    private fun startAlarm() {
        if (isAlarmActive) return
        serviceScope.launch {
            try {
                mediaPlayer = MediaPlayer.create(this@StealthService, R.raw.alarm_sound)
                mediaPlayer?.isLooping = true
                mediaPlayer?.setVolume(1.0f, 1.0f)
                mediaPlayer?.start()
                isAlarmActive = true
                showAlarmNotification()
                delay(60000)
                stopAlarm()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
    
    private fun stopAlarm() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isAlarmActive = false
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.cancel(1001)
        } catch (e: Exception) { e.printStackTrace() }
    }
    
    private fun showAlarmNotification() {
        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Security Alert")
            .setContentText("Alarm is active")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(1001, notification)
    }
    
    private fun capturePhoto(sender: String) {
        serviceScope.launch {
            val cameraManager = CameraManager(this@StealthService)
            cameraManager.capturePhoto(false) { photoPath ->
                if (photoPath.startsWith("/")) {
                    val dataManager = DataManager(this@StealthService)
                    dataManager.sendEmailWithAttachment("Security Photo", "Photo captured", java.io.File(photoPath), sender)
                }
            }
        }
    }
    
    private fun getLocation(sender: String) {
        serviceScope.launch {
            val locationManager = LocationManager(this@StealthService)
            locationManager.getCurrentLocation { location ->
                val dataManager = DataManager(this@StealthService)
                dataManager.sendSMS(sender, "Location: $location")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
        if (wakeLock.isHeld) wakeLock.release()
        serviceScope.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, StealthService::class.java))
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
