package com.virtual.auth

import android.content.Context
import com.virtual.core.models.AuthResponse
import com.virtual.core.models.Device
import com.virtual.core.models.DeviceType
import com.virtual.core.network.ApiClient
import com.virtual.core.storage.SecureStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class AuthRepository(private val context: Context) {

    private val api = ApiClient.getService(context)
    private val storage = SecureStorageManager.getInstance(context)

    suspend fun register(name: String, email: String, pass: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.register(mapOf("name" to name, "email" to email, "password" to pass))
            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                saveAuthData(data)
                Result.success(data)
            } else {
                val errorMsg = response.body()?.error ?: "Registration failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, pass: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.login(mapOf("email" to email, "password" to pass))
            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                saveAuthData(data)
                Result.success(data)
            } else {
                val errorMsg = response.body()?.error ?: "Invalid email or password"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerDevice(role: DeviceType, deviceName: String? = null): Result<Device> = withContext(Dispatchers.IO) {
        try {
            val prefix = if (role == DeviceType.CAMERA) "VRT-CAM-" else "VRT-VIE-"
            val randomSuffix = UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
            val deviceUuid = storage.deviceUuid ?: "$prefix$randomSuffix"

            val body = mapOf(
                "deviceType" to role.name,
                "deviceUuid" to deviceUuid,
                "deviceName" to (deviceName ?: if (role == DeviceType.CAMERA) "Phone 1 Camera" else "Phone 2 Viewer"),
                "platform" to "ANDROID",
                "appVersion" to "1.0.0"
            )

            val response = api.registerDevice(body)
            val device = response.body()?.data
            if (response.isSuccessful && device != null) {
                storage.deviceId = device.id
                storage.deviceUuid = device.deviceUuid
                storage.deviceRole = role
                Result.success(device)
            } else {
                Result.failure(Exception(response.body()?.error ?: "Failed to register device"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        storage.clearAll()
    }

    private fun saveAuthData(authResponse: AuthResponse) {
        storage.accessToken = authResponse.tokens.accessToken
        storage.refreshToken = authResponse.tokens.refreshToken
        storage.userId = authResponse.user.id
        storage.userEmail = authResponse.user.email
        storage.userName = authResponse.user.name
    }
}
