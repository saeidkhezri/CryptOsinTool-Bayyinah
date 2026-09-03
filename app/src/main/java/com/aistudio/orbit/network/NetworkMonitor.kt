package com.aistudio.orbit.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

sealed class NetworkStatus {
    data class Online(
        val isWifi: Boolean = true,
        val isCellular: Boolean = false,
        val activeConnectionType: String = "WiFi / High Bandwidth"
    ) : NetworkStatus()

    object Offline : NetworkStatus()
}

/**
 * Enterprise Network Monitoring Service using ConnectivityManager.
 * Tracks system network capabilities and exposes real-time status flows.
 */
class NetworkMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    val networkStatusFlow: Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(determineNetworkStatus())
            }

            override fun onLost(network: Network) {
                trySend(determineNetworkStatus())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(determineNetworkStatus())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager?.registerNetworkCallback(request, callback)
        trySend(determineNetworkStatus())

        awaitClose {
            try {
                connectivityManager?.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
                // Ignore cleanup error if service unbinds
            }
        }
    }

    fun isCurrentlyOnline(): Boolean {
        val status = determineNetworkStatus()
        return status is NetworkStatus.Online
    }

    fun determineNetworkStatus(): NetworkStatus {
        val cm = connectivityManager ?: return NetworkStatus.Offline
        val activeNetwork = cm.activeNetwork ?: return NetworkStatus.Offline
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkStatus.Offline

        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        if (!hasInternet) return NetworkStatus.Offline

        val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val typeStr = when {
            isWifi -> "WiFi (Stable)"
            isCellular -> "Cellular Data"
            else -> "Ethernet / External"
        }

        return NetworkStatus.Online(
            isWifi = isWifi,
            isCellular = isCellular,
            activeConnectionType = typeStr
        )
    }
}
