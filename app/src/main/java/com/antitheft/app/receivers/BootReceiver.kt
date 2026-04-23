package com.antitheft.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.antitheft.app.services.StealthService
import com.antitheft.app.services.WatchdogService
import com.antitheft.app.utils.Constants

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                val prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
                val isSetupComplete = !prefs.getBoolean(Constants.PREF_FIRST_RUN, true)
                
                if (isSetupComplete) {
                    val stealthIntent = Intent(context, StealthService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(stealthIntent)
                    } else {
                        context.startService(stealthIntent)
                    }
                    
                    val watchdogIntent = Intent(context, WatchdogService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(watchdogIntent)
                    } else {
                        context.startService(watchdogIntent)
                    }
                }
            }
        }
    }
}
