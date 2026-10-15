package com.virtual.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.virtual.core.models.DeviceSetting
import com.virtual.core.network.ApiClient
import com.virtual.core.storage.SecureStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingsRepository(private val context: Context) {

    private val api: com.virtual.core.network.ApiService get() = ApiClient.getService(context)
    private val storage = SecureStorageManager.getInstance(context)

    suspend fun getSettings(): Result<DeviceSetting> = withContext(Dispatchers.IO) {
        try {
            val deviceId = storage.deviceId ?: return@withContext Result.failure(Exception("Device not registered"))
            val response = api.getSettings(deviceId)
            val setting = response.body()?.data
            if (response.isSuccessful && setting != null) {
                Result.success(setting)
            } else {
                Result.failure(Exception(response.body()?.error ?: "Failed to fetch settings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSettings(
        videoQuality: String? = null,
        fps: Int? = null,
        audioEnabled: Boolean? = null,
        autoReconnect: Boolean? = null
    ): Result<DeviceSetting> = withContext(Dispatchers.IO) {
        try {
            val deviceId = storage.deviceId ?: return@withContext Result.failure(Exception("Device not registered"))
            val body = mutableMapOf<String, Any>()
            videoQuality?.let { body["videoQuality"] = it }
            fps?.let { body["fps"] = it }
            audioEnabled?.let { body["audioEnabled"] = it }
            autoReconnect?.let { body["autoReconnect"] = it }

            val response = api.updateSettings(deviceId, body)
            val setting = response.body()?.data
            if (response.isSuccessful && setting != null) {
                Result.success(setting)
            } else {
                Result.failure(Exception(response.body()?.error ?: "Failed to update settings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }
        return true
    }

    fun requestIgnoreBatteryOptimizationsIntent(): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isIgnoringBatteryOptimizations()) {
            return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        return null
    }
}
