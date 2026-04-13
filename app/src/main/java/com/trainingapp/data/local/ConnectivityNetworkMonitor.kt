package com.trainingapp.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Production [NetworkMonitor] backed by [ConnectivityManager.NetworkCallback].
 *
 * Wraps the callback-based Android API in a [callbackFlow] so the rest of the
 * app can treat connectivity as a plain coroutines [Flow].  The flow:
 *  - emits the **current** connectivity state immediately on first collection,
 *  - emits again on every transition (online ↔ offline),
 *  - applies [distinctUntilChanged] so duplicate emissions are suppressed,
 *  - unregisters the callback when the collector is cancelled (no leaks).
 *
 * Requires [android.Manifest.permission.ACCESS_NETWORK_STATE].
 */
class ConnectivityNetworkMonitor(context: Context) : NetworkMonitor {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override val isOnline: Flow<Boolean> = callbackFlow {

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }
            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        // Emit the current state synchronously before registering for future changes.
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        trySend(caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)

        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }

    }.distinctUntilChanged()
}
