/*
 * REON Music App - Network Connectivity Manager
 * Copyright (c) 2024 REON
 * Handles WiFi-only checks and network state monitoring
 */

package com.reon.music.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network state data class
 */
data class NetworkState(
    val isConnected: Boolean = false,
    val isWifi: Boolean = false,
    val isCellular: Boolean = false,
    val isMetered: Boolean = true
)

/**
 * Network Connectivity Manager
 * Monitors network state and provides WiFi-only checking functionality
 */
@Singleton
class NetworkConnectivityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _networkState = MutableStateFlow(getCurrentNetworkState())
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    init {
        registerNetworkCallback()
    }
    
    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _networkState.value = getCurrentNetworkState()
            }
            
            override fun onLost(network: Network) {
                _networkState.value = getCurrentNetworkState()
            }
            
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                _networkState.value = getCurrentNetworkState()
            }
        })
    }
    
    private fun getCurrentNetworkState(): NetworkState {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        
        return if (capabilities != null) {
            NetworkState(
                isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
                isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
                isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            )
        } else {
            NetworkState(isConnected = false)
        }
    }
    
    /**
     * Check if network is available for the given operation
     * @param wifiOnly If true, only returns true when on WiFi
     */
    fun isNetworkAvailable(wifiOnly: Boolean = false): Boolean {
        val state = _networkState.value
        return if (wifiOnly) {
            state.isConnected && state.isWifi
        } else {
            state.isConnected
        }
    }
    
    /**
     * Check if we should use high quality streaming
     * Returns true if on WiFi or unmetered connection
     */
    fun shouldUseHighQuality(): Boolean {
        val state = _networkState.value
        return state.isWifi || !state.isMetered
    }
    
    /**
     * Check if downloads are allowed based on settings
     */
    fun canDownload(wifiOnlySetting: Boolean): Boolean {
        return if (wifiOnlySetting) {
            _networkState.value.isWifi
        } else {
            _networkState.value.isConnected
        }
    }
}
