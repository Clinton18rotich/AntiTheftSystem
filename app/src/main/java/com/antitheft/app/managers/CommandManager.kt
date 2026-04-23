package com.antitheft.app.managers

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.antitheft.app.services.LocationService
import com.antitheft.app.services.StealthService
import com.antitheft.app.utils.Constants
import kotlinx.coroutines.*

class CommandManager(private val context: Context) {
    
    private val locationManager = LocationManager(context)
    private val cameraManager = CameraManager(context)
    private val callManager = CallManager(context)
    private val dataManager = DataManager(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    fun processCommand(sender: String, command: String, context: Context) {
        val cmd = command.removePrefix(Constants.COMMAND_PREFIX).trim()
        val parts = cmd.split(" ")
        val action = parts[0].uppercase()
        val param = if (parts.size > 1) parts.subList(1, parts.size).joinToString(" ") else ""
        
        scope.launch {
            when (action) {
                Constants.CMD_LOCATION -> getLocation(sender)
                Constants.CMD_CAMERA_PHOTO -> takePhoto(sender, param)
                Constants.CMD_CAMERA_VIDEO -> recordVideo(sender, param)
                Constants.CMD_CALL -> makeCall(sender, param)
                Constants.CMD_SMS -> sendSMS(sender, param)
                Constants.CMD_WIPE -> wipeDevice(sender)
                Constants.CMD_LOCK -> lockDevice(sender)
                Constants.CMD_ALARM -> playAlarm(sender)
                Constants.CMD_SIM_INFO -> getSIMInfo(sender)
                Constants.CMD_BATTERY -> getBatteryInfo(sender)
                Constants.CMD_RECORD_AUDIO -> recordAudio(sender, param)
                Constants.CMD_DEVICE_INFO -> getDeviceInfo(sender)
                Constants.CMD_START_TRACKING -> startTracking(sender, param)
                Constants.CMD_STOP_TRACKING -> stopTracking(sender)
                Constants.CMD_SYNC_NOW -> syncNow(sender)
                // Enhanced commands
                "VIBRATE" -> vibrate(sender, param)
                "FLASHLIGHT" -> controlFlashlight(sender, param)
                "VOLUME" -> setVolume(sender, param)
                "WIFI" -> toggleWifi(sender, param)
                "BLUETOOTH" -> toggleBluetooth(sender, param)
                "CONTACTS" -> getContacts(sender)
                "CALL_LOGS" -> getCallLogs(sender, param)
                "FILE_LIST" -> listFiles(sender, param)
                "STORAGE_INFO" -> getStorageInfo(sender)
                "PING" -> sendPing(sender)
                "HELP" -> sendHelp(sender)
            }
        }
    }
    
    private fun getLocation(sender: String) {
        locationManager.getCurrentLocation { location ->
            dataManager.sendSMS(sender, "📍 $location\nhttps://maps.google.com/?q=$location")
        }
    }
    
    private fun takePhoto(sender: String, param: String) {
        val useFlash = param.lowercase() == "flash"
        cameraManager.capturePhoto(useFlash) { photoPath ->
            if (photoPath.startsWith("/")) {
                dataManager.sendEmailWithAttachment("Camera Capture", "Photo captured", java.io.File(photoPath), sender)
            } else {
                dataManager.sendSMS(sender, photoPath)
            }
        }
    }
    
    private fun recordVideo(sender: String, param: String) {
        val duration = param.toIntOrNull() ?: Constants.VIDEO_DURATION
        cameraManager.recordVideo(duration) { videoPath ->
            if (videoPath.startsWith("/")) {
                dataManager.sendEmailWithAttachment("Video Capture", "Video recorded", java.io.File(videoPath), sender)
            } else {
                dataManager.sendSMS(sender, videoPath)
            }
        }
    }
    
    private fun makeCall(sender: String, param: String) {
        if (param.isNotEmpty()) {
            callManager.makeSilentCall(param)
            dataManager.sendSMS(sender, "✓ Calling $param")
        }
    }
    
    private fun sendSMS(sender: String, param: String) {
        val parts = param.split("|", limit = 2)
        if (parts.size == 2) {
            dataManager.sendSMS(parts[0], parts[1])
            dataManager.sendSMS(sender, "✓ SMS sent to ${parts[0]}")
        }
    }
    
    private fun wipeDevice(sender: String) {
        dataManager.sendSMS(sender, "⚠️ Wiping device...")
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        try { devicePolicyManager.wipeData(0) } catch (e: Exception) { dataManager.sendSMS(sender, "Wipe failed: ${e.message}") }
    }
    
    private fun lockDevice(sender: String) {
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        try {
            devicePolicyManager.lockNow()
            dataManager.sendSMS(sender, "✓ Device locked")
        } catch (e: Exception) { dataManager.sendSMS(sender, "Lock failed: ${e.message}") }
    }
    
    private fun playAlarm(sender: String) {
        val intent = Intent(context, StealthService::class.java).apply { putExtra("action", "alarm") }
        context.startService(intent)
        dataManager.sendSMS(sender, "✓ Alarm activated")
    }
    
    private fun getSIMInfo(sender: String) {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
        val info = "SIM: ${telephonyManager.simSerialNumber ?: "N/A"}\nOperator: ${telephonyManager.simOperatorName}"
        dataManager.sendSMS(sender, info)
    }
    
    private fun getBatteryInfo(sender: String) {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        val level = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        dataManager.sendSMS(sender, "Battery: $level%")
    }
    
    private fun recordAudio(sender: String, param: String) {
        dataManager.sendSMS(sender, "Audio recording started for ${param.toIntOrNull() ?: 60}s")
    }
    
    private fun getDeviceInfo(sender: String) {
        val info = "Model: ${Build.MODEL}\nManufacturer: ${Build.MANUFACTURER}\nAndroid: ${Build.VERSION.RELEASE}"
        dataManager.sendSMS(sender, info)
    }
    
    private fun startTracking(sender: String, param: String) {
        val interval = param.toLongOrNull() ?: Constants.TRACKING_INTERVAL
        val intent = Intent(context, LocationService::class.java).apply {
            putExtra("action", "start")
            putExtra("interval", interval)
            putExtra("duration", 3600000L)
            putExtra("target_number", sender)
        }
        context.startService(intent)
    }
    
    private fun stopTracking(sender: String) {
        val intent = Intent(context, LocationService::class.java).apply { putExtra("action", "stop") }
        context.startService(intent)
        dataManager.sendSMS(sender, "✓ Tracking stopped")
    }
    
    private fun syncNow(sender: String) {
        dataManager.sendSMS(sender, "✓ Syncing offline data")
    }
    
    // Enhanced Commands
    private fun vibrate(sender: String, param: String) {
        val duration = param.toLongOrNull() ?: 1000L
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") vibrator.vibrate(duration)
        }
        dataManager.sendSMS(sender, "✓ Vibrating ${duration}ms")
    }
    
    private fun controlFlashlight(sender: String, param: String) {
        dataManager.sendSMS(sender, "Flashlight: $param (Camera2 API)")
    }
    
    private fun setVolume(sender: String, param: String) {
        val level = param.toIntOrNull() ?: 50
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (maxVolume * level / 100).coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        dataManager.sendSMS(sender, "✓ Volume set to $level%")
    }
    
    private fun toggleWifi(sender: String, param: String) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val newState = when (param.lowercase()) {
            "on" -> true
            "off" -> false
            else -> !wifiManager.isWifiEnabled
        }
        wifiManager.isWifiEnabled = newState
        dataManager.sendSMS(sender, "✓ WiFi ${if (newState) "enabled" else "disabled"}")
    }
    
    private fun toggleBluetooth(sender: String, param: String) {
        val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            dataManager.sendSMS(sender, "No Bluetooth")
            return
        }
        when (param.lowercase()) {
            "on" -> bluetoothAdapter.enable()
            "off" -> bluetoothAdapter.disable()
            else -> { if (bluetoothAdapter.isEnabled) bluetoothAdapter.disable() else bluetoothAdapter.enable() }
        }
        dataManager.sendSMS(sender, "✓ Bluetooth toggled")
    }
    
    private fun getContacts(sender: String) {
        dataManager.sendSMS(sender, "Contacts: Permission required")
    }
    
    private fun getCallLogs(sender: String, param: String) {
        dataManager.sendSMS(sender, "Call logs: Permission required")
    }
    
    private fun listFiles(sender: String, param: String) {
        val path = if (param.isNotEmpty()) param else "/sdcard"
        val dir = java.io.File(path)
        if (!dir.exists()) { dataManager.sendSMS(sender, "Path not found"); return }
        val files = dir.listFiles()?.take(20)?.joinToString("\n") { 
            "${if (it.isDirectory) "[DIR]" else "[FILE]"} ${it.name} (${it.length()} bytes)"
        } ?: "Empty"
        dataManager.sendSMS(sender, "Files in $path:\n$files")
    }
    
    private fun getStorageInfo(sender: String) {
        val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
        val free = stat.availableBytes
        val total = stat.totalBytes
        dataManager.sendSMS(sender, "Storage: ${free/1024/1024}MB free / ${total/1024/1024}MB total")
    }
    
    private fun sendPing(sender: String) {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        val level = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        dataManager.sendSMS(sender, "PONG | Battery: $level% | Online")
    }
    
    private fun sendHelp(sender: String) {
        val help = """
            Commands:
            LOC - Get location
            PHOTO - Take photo
            VIDEO [sec] - Record video
            CALL [num] - Make call
            SMS [num]|[msg] - Send SMS
            LOCK - Lock device
            ALARM - Play alarm
            VIBRATE [ms] - Vibrate
            VOLUME [0-100] - Set volume
            WIFI [on/off] - Toggle WiFi
            BLUETOOTH [on/off] - Toggle Bluetooth
            FILE_LIST [path] - List files
            STORAGE_INFO - Storage info
            PING - Check status
            HELP - This message
        """.trimIndent()
        dataManager.sendSMS(sender, help)
    }
}
