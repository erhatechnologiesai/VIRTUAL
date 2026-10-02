package com.virtual.core.telemetry

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import com.virtual.core.network.ApiClient
import com.virtual.core.storage.SecureStorageManager
import kotlinx.coroutines.*

class DeviceStatusMonitor(private val context: Context) {

    private val storage = SecureStorageManager.getInstance(context)
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startReporting(streamStatusProvider: () -> String) {
        stopReporting()
        job = scope.launch {
            while (isActive) {
                try {
                    val deviceId = storage.deviceId
                    if (deviceId != null && storage.isLoggedIn()) {
                        val battery = getBatteryLevel()
                        val network = getNetworkType()
                        val status = streamStatusProvider()

                        val telemetryData = mapOf(
                            "batteryLevel" to battery,
                            "networkType" to network,
                            "streamStatus" to status,
                            "appVersion" to "1.0.0"
                        )

                        val service = ApiClient.getService(context)
                        service.sendTelemetry(deviceId, telemetryData)
                    }
                } catch (e: Exception) {
                    // Suppress and retry next interval
                }
                delay(15000) // 15 seconds
            }
        }
    }

    fun stopReporting() {
        job?.cancel()
        job = null
    }

    fun getBatteryLevel(): Int {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return 100

        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

        return if (level >= 0 && scale > 0) {
            ((level.toFloat() / scale.toFloat()) * 100).toInt()
        } else {
            100
        }
    }

    fun getNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "UNKNOWN"
        val network = cm.activeNetwork ?: return "NONE"
        val capabilities = cm.getNetworkCapabilities(network) ?: return "NONE"

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                // High bandwidth or 5G capability check
                if (capabilities.linkDownstreamBandwidthKbps > 20000) "5G" else "4G"
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "OTHER"
        }
    }
}
