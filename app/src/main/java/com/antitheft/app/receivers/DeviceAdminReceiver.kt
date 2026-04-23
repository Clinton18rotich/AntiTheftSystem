package com.antitheft.app.receivers

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.antitheft.app.managers.CameraManager
import com.antitheft.app.managers.DataManager
import com.antitheft.app.utils.Constants

class DeviceAdminReceiver : DeviceAdminReceiver() {
    
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        val prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("device_admin_enabled", true).apply()
        val emergencyNumber = prefs.getString(Constants.PREF_EMERGENCY_NUMBER, "")
        if (!emergencyNumber.isNullOrEmpty()) {
            DataManager(context).sendSMS(emergencyNumber, "Device Admin activated")
        }
    }
    
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        val prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("device_admin_enabled", false).apply()
        val emergencyNumber = prefs.getString(Constants.PREF_EMERGENCY_NUMBER, "")
        if (!emergencyNumber.isNullOrEmpty()) {
            DataManager(context).sendSMS(emergencyNumber, "ALERT: Device Admin disabled!")
        }
    }
    
    override fun onPasswordFailed(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        val failedAttempts = prefs.getInt("failed_attempts", 0) + 1
        prefs.edit().putInt("failed_attempts", failedAttempts).apply()
        
        if (failedAttempts >= 3) {
            CameraManager(context).capturePhoto(false) { photoPath ->
                val emergencyNumber = prefs.getString(Constants.PREF_EMERGENCY_NUMBER, "")
                if (!emergencyNumber.isNullOrEmpty() && photoPath.startsWith("/")) {
                    DataManager(context).sendEmailWithAttachment(
                        "Failed Unlock Attempt", "Multiple failed attempts", java.io.File(photoPath), emergencyNumber
                    )
                }
            }
        }
    }
    
    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt("failed_attempts", 0).apply()
    }
}
