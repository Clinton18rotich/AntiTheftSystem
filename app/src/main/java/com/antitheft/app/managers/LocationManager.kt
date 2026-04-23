package com.antitheft.app.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*

class LocationManager(private val context: Context) {
    
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null
    
    fun getCurrentLocation(callback: (String) -> Unit) {
        if (!hasLocationPermission()) {
            callback("Location permission not granted")
            return
        }
        
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        callback("${location.latitude},${location.longitude}")
                    } else {
                        requestFreshLocation(callback)
                    }
                }
                .addOnFailureListener { callback("Failed to get location") }
        } catch (e: Exception) {
            callback("Error: ${e.message}")
        }
    }
    
    private fun requestFreshLocation(callback: (String) -> Unit) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
            numUpdates = 1
        }
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    callback("${location.latitude},${location.longitude}")
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }
        }
        
        if (hasLocationPermission()) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
        }
    }
    
    fun startTracking(interval: Long, callback: (String) -> Unit) {
        if (!hasLocationPermission()) return
        
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            this.interval = interval
            fastestInterval = interval / 2
        }
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    callback("${location.latitude},${location.longitude}")
                }
            }
        }
        
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
    }
    
    fun stopTracking() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
    }
    
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}
