package com.virtual.auth

import android.content.Context
import com.virtual.core.models.AuthResponse
import com.virtual.core.models.Device
import com.virtual.core.models.DeviceType
import com.virtual.core.network.ApiClient
import com.virtual.core.storage.SecureStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

class AuthRepository(private val context: Context) {

    private val api: com.virtual.core.network.ApiService get() = ApiClient.getService(context)
    private val storage = SecureStorageManager.getInstance(context)

    private fun sanitizeEmail(email: String): String {
        var clean = email.trim().lowercase()
        if (clean.isBlank()) clean = "user@virtual.app"
        if (!clean.contains("@")) clean = "$clean@virtual.app"
        if (clean.endsWith(".cor") || clean.endsWith(".con") || clean.endsWith(".cm")) {
            clean = clean.substringBeforeLast(".") + ".com"
        }
        return clean
    }

    suspend fun register(name: String, email: String, pass: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = sanitizeEmail(email)
            val cleanPass = pass.trim()
            val cleanName = name.trim()
            val response = api.register(mapOf("name" to cleanName, "email" to cleanEmail, "password" to cleanPass))
            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                saveAuthData(data)
                Result.success(data)
            } else {
                val errorMsg = extractErrorMessage(response, "Registration failed")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, pass: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = sanitizeEmail(email)
            val cleanPass = pass.trim()
            val response = api.login(mapOf("email" to cleanEmail, "password" to cleanPass))
            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                saveAuthData(data)
                Result.success(data)
            } else {
                val errorMsg = extractErrorMessage(response, "Invalid email or password")
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
                val errorMsg = extractErrorMessage(response, "Failed to register device")
                Result.failure(Exception(errorMsg))
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

    private fun extractErrorMessage(response: retrofit2.Response<*>, defaultMsg: String): String {
        return try {
            val errorJson = response.errorBody()?.string()
            if (!errorJson.isNullOrBlank()) {
                val jsonObject = JSONObject(errorJson)
                if (jsonObject.has("error")) {
                    jsonObject.getString("error")
                } else if (jsonObject.has("errors")) {
                    val array = jsonObject.getJSONArray("errors")
                    if (array.length() > 0) {
                        val firstObj = array.getJSONObject(0)
                        firstObj.optString("message", defaultMsg)
                    } else {
                        defaultMsg
                    }
                } else {
                    defaultMsg
                }
            } else {
                defaultMsg
            }
        } catch (e: Exception) {
            defaultMsg
        }
    }
}
