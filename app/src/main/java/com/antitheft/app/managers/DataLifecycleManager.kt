package com.antitheft.app.managers

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class DataLifecycleManager(private val context: Context) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val networkMonitor = com.antitheft.app.utils.NetworkMonitor(context)
    private val offlineStorage = OfflineDataManager(context)
    
    init {
        scope.launch {
            networkMonitor.networkState.collect { state ->
                when (state) {
                    com.antitheft.app.utils.NetworkState.ONLINE -> handleOnlineTransition()
                    com.antitheft.app.utils.NetworkState.OFFLINE -> handleOfflineTransition()
                    else -> {}
                }
            }
        }
    }
    
    private suspend fun handleOnlineTransition() {
        val pending = offlineStorage.getPendingCleanupData()
        pending.forEach { offlineStorage.markAsUploaded(it.id) }
    }
    
    private suspend fun handleOfflineTransition() {
        offlineStorage.setCompressionLevel(CompressionLevel.MAXIMUM)
    }
    
    suspend fun forceSyncNow() {
        if (networkMonitor.isOnline()) handleOnlineTransition()
    }
    
    suspend fun checkAndSyncIfNeeded() {
        if (networkMonitor.isOnline() && offlineStorage.getPendingCount() > 0) handleOnlineTransition()
    }
}
