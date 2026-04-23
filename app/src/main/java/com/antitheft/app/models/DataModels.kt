package com.antitheft.app.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocationData(
    val latitude: Double, val longitude: Double, val accuracy: Float,
    val timestamp: Long, val provider: String
) : Parcelable

@Parcelize
data class PhotoData(
    val path: String, val size: Long, val width: Int, val height: Int,
    val timestamp: Long, val hasFace: Boolean = false, val hasText: Boolean = false
) : Parcelable

@Parcelize
data class AudioData(
    val path: String, val duration: Long, val size: Long,
    val timestamp: Long, val format: String
) : Parcelable

@Parcelize
data class VideoData(
    val path: String, val duration: Long, val size: Long,
    val width: Int, val height: Int, val timestamp: Long
) : Parcelable

data class DeviceInfo(
    val model: String, val manufacturer: String, val androidVersion: String,
    val sdkVersion: Int, val batteryLevel: Int, val isRooted: Boolean,
    val availableStorage: Long, val totalStorage: Long
)
