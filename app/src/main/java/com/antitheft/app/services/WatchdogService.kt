package com.antitheft.app.services

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.*

class WatchdogService : Service() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isWatching = false
    
    override fun onCreate() {
        super.onCreate()
        isWatching = true
        serviceScope.launch {
            while (isWatching) {
                delay(10000)
                if (!isServiceRunning(StealthService::class.java)) {
                    val intent = Intent(this@WatchdogService, StealthService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
                    else startService(intent)
                }
                checkMemoryPressure()
            }
        }
    }
    
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
        return runningServices.any { it.service.className == serviceClass.name }
    }
    
    private fun checkMemoryPressure() {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        if (memoryInfo.lowMemory) System.gc()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    
    override fun onDestroy() {
        super.onDestroy()
        isWatching = false
        serviceScope.cancel()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
