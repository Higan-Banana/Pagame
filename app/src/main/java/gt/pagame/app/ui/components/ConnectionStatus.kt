package gt.pagame.app.ui.components

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Internet availability reported by Android; not a guarantee of Firebase availability. */
@Composable
fun rememberInternetAvailable(): State<Boolean> {
    val context = LocalContext.current.applicationContext
    val manager = remember(context) { context.getSystemService(ConnectivityManager::class.java) }
    val initial = remember(manager) {
        manager.getNetworkCapabilities(manager.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    }
    val connectivity = remember(manager) {
        callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    trySend(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                }
                override fun onLost(network: Network) { trySend(false) }
            }
            manager.registerDefaultNetworkCallback(callback)
            trySend(manager.getNetworkCapabilities(manager.activeNetwork)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true)
            awaitClose { manager.unregisterNetworkCallback(callback) }
        }.distinctUntilChanged()
    }
    return connectivity.collectAsState(initial = initial)
}
