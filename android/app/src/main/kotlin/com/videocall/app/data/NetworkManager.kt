package com.videocall.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class NetworkType {
    WIFI,
    MOBILE_DATA,
    NONE
}

data class NetworkState(
    val isConnected: Boolean,
    val networkType: NetworkType,
    val isWifiEnabled: Boolean,
    val isMobileDataEnabled: Boolean
)

class NetworkManager(private val context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun getCurrentNetworkState(): NetworkState {
        val network = connectivityManager.activeNetwork ?: return NetworkState(
            isConnected = false,
            networkType = NetworkType.NONE,
            isWifiEnabled = false,
            isMobileDataEnabled = false
        )

        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkState(
            isConnected = false,
            networkType = NetworkType.NONE,
            isWifiEnabled = false,
            isMobileDataEnabled = false
        )

        val isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        val networkType = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE_DATA
            else -> NetworkType.NONE
        }

        val isWifiEnabled = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isMobileDataEnabled = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

        return NetworkState(
            isConnected = isConnected,
            networkType = networkType,
            isWifiEnabled = isWifiEnabled,
            isMobileDataEnabled = isMobileDataEnabled
        )
    }

    fun observeNetworkState(): Flow<NetworkState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(getCurrentNetworkState())
            }

            override fun onLost(network: Network) {
                trySend(getCurrentNetworkState())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(getCurrentNetworkState())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)
        
        // İlk durumu gönder
        trySend(getCurrentNetworkState())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    fun getNetworkTypeText(networkType: NetworkType): String {
        return when (networkType) {
            NetworkType.WIFI -> "Wi-Fi"
            NetworkType.MOBILE_DATA -> "Mobil Veri"
            NetworkType.NONE -> "Bağlantı Yok"
        }
    }
}

