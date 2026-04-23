package com.antitheft.app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor(private val context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _networkState = MutableStateFlow(NetworkState.OFFLINE)
    val networkState = _networkState.asStateFlow()
    private val _isOnline = MutableStateFlow(false)
    val isOnline = _isOnline.asStateFlow()

    init {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _networkState.value = NetworkState.ONLINE
                _isOnline.value = true
            }
            override fun onLost(network: Network) {
                _networkState.value = NetworkState.OFFLINE
                _isOnline.value = false
            }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val state = when {
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> {
                        if (capabilities.linkDownstreamBandwidthKbps < 100) NetworkState.UNSTABLE
                        else NetworkState.ONLINE
                    }
                    else -> NetworkState.OFFLINE
                }
                _networkState.value = state
                _isOnline.value = state == NetworkState.ONLINE
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    fun isOnline(): Boolean = _isOnline.value
    fun getCurrentType(): NetworkType {
        val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR_4G
            else -> NetworkType.OTHER
        }
    }

    companion object {
        fun isOnline(context: Context): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }
}

enum class NetworkState { ONLINE, OFFLINE, UNSTABLE }
enum class NetworkType { WIFI, CELLULAR_5G, CELLULAR_4G, CELLULAR_3G, NONE, OTHER }
