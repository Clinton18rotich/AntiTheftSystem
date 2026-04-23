package com.antitheft.app.utils

object Constants {
    const val PREF_NAME = "system_prefs"
    const val PREF_FIRST_RUN = "first_run"
    const val PREF_EMERGENCY_NUMBER = "emergency_number"
    const val PREF_AUTH_NUMBERS = "authorized_numbers"
    const val PREF_SIM_SERIAL = "sim_serial"
    const val PREF_EMAIL = "backup_email"
    const val PREF_EMAIL_PASSWORD = "email_password"
    const val PREF_DEVICE_ADMIN = "device_admin_enabled"
    const val NOTIFICATION_CHANNEL_ID = "system_service_channel"
    const val NOTIFICATION_ID = 9999
    const val COMMAND_PREFIX = "CMD:"
    const val ENCRYPTION_PREFIX = "ENC:"
    const val ENCRYPTION_KEY = "SysTemSecur1tyKey2024!"
    const val CMD_LOCATION = "LOC"
    const val CMD_CAMERA_PHOTO = "PHOTO"
    const val CMD_CAMERA_VIDEO = "VIDEO"
    const val CMD_CALL = "CALL"
    const val CMD_SMS = "SMS"
    const val CMD_WIPE = "WIPE"
    const val CMD_LOCK = "LOCK"
    const val CMD_ALARM = "ALARM"
    const val CMD_SIM_INFO = "SIM"
    const val CMD_BATTERY = "BATT"
    const val CMD_RECORD_AUDIO = "AUDIO"
    const val CMD_SCREENSHOT = "SCREEN"
    const val CMD_DEVICE_INFO = "INFO"
    const val CMD_START_TRACKING = "TRACK_START"
    const val CMD_STOP_TRACKING = "TRACK_STOP"
    const val CMD_SYNC_NOW = "SYNC_NOW"
    const val CMD_OFFLINE_STATUS = "OFFLINE_STATUS"
    const val VIDEO_DURATION = 30
    const val LOCATION_TIMEOUT = 30000L
    const val TRACKING_INTERVAL = 60000L
    const val SYNC_RETRY_DELAY = 30000L
    const val MAX_OFFLINE_STORAGE = 500 * 1024 * 1024L
    const val MIN_FREE_SPACE = 100 * 1024 * 1024L
    const val DATA_RETENTION_DAYS = 7
}
