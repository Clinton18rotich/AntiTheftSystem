package com.antitheft.app.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.antitheft.app.managers.DataManager
import com.antitheft.app.managers.LocationManager
import com.antitheft.app.utils.Constants
import kotlinx.coroutines.*

class LocationService : Service() {
    
    private lateinit var locationManager: LocationManager
    private lateinit var dataManager: DataManager
    private var targetNumber: String = ""
    private var interval: Long = Constants.TRACKING_INTERVAL
    private var duration: Long = 3600000L
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var trackingJob: Job? = null
    
    override fun onCreate() {
        super.onCreate()
        locationManager = LocationManager(this)
        dataManager = DataManager(this)
        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("")
            .setContentText("")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        startForeground(Constants.NOTIFICATION_ID + 1, notification)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            interval = it.getLongExtra("interval", Constants.TRACKING_INTERVAL)
            duration = it.getLongExtra("duration", 3600000L)
            targetNumber = it.getStringExtra("target_number") ?: ""
            when (it.getStringExtra("action")) {
                "start" -> startTracking()
                "stop" -> stopTracking()
            }
        }
        return START_STICKY
    }
    
    private fun startTracking() {
        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            var locationCount = 0
            dataManager.sendSMS(targetNumber, "Tracking started")
            locationManager.startTracking(interval) { location ->
                locationCount++
                dataManager.sendSMS(targetNumber, "#$locationCount: $location")
            }
            delay(duration)
            stopTracking()
            dataManager.sendSMS(targetNumber, "Tracking completed. Total: $locationCount")
        }
    }
    
    private fun stopTracking() {
        trackingJob?.cancel()
        locationManager.stopTracking()
        stopSelf()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        locationManager.stopTracking()
        trackingJob?.cancel()
        serviceScope.cancel()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
