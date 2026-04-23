package com.antitheft.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.antitheft.app.managers.CameraManager
import com.antitheft.app.managers.DataManager
import com.antitheft.app.managers.LocationManager
import com.antitheft.app.utils.Constants
import kotlinx.coroutines.*

class SIMChangeReceiver : BroadcastReceiver() {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.SIM_STATE_CHANGED") return
        
        val simState = intent.getStringExtra("ss")
        
        when (simState) {
            "LOADED", "ABSENT" -> {
                scope.launch { handleSIMStateChange(context) }
            }
        }
    }
    
    private suspend fun handleSIMStateChange(context: Context) {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val newSIM = telephonyManager.simSerialNumber
        val prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        val oldSIM = prefs.getString(Constants.PREF_SIM_SERIAL, null)
        
        if (oldSIM != null && oldSIM != newSIM && newSIM != null) {
            val emergencyNumber = prefs.getString(Constants.PREF_EMERGENCY_NUMBER, "")
            if (!emergencyNumber.isNullOrEmpty()) {
                val dataManager = DataManager(context)
                val simInfo = """
                    ALERT: SIM CARD CHANGED!
                    Old SIM: ${oldSIM.takeLast(4)}
                    New SIM: ${newSIM.takeLast(4)}
                    Operator: ${telephonyManager.simOperatorName}
                """.trimIndent()
                
                dataManager.sendSMS(emergencyNumber, simInfo)
                
                delay(2000)
                val locationManager = LocationManager(context)
                locationManager.getCurrentLocation { location ->
                    dataManager.sendSMS(emergencyNumber, "Current location: $location")
                }
                
                delay(3000)
                val cameraManager = CameraManager(context)
                cameraManager.capturePhoto(false) { photoPath ->
                    if (photoPath.startsWith("/")) {
                        dataManager.sendEmailWithAttachment(
                            "SIM Change Alert", "SIM card changed", java.io.File(photoPath), emergencyNumber
                        )
                    }
                }
            }
            prefs.edit().putString(Constants.PREF_SIM_SERIAL, newSIM).apply()
        } else if (oldSIM == null && newSIM != null) {
            prefs.edit().putString(Constants.PREF_SIM_SERIAL, newSIM).apply()
        }
    }
}
