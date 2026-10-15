package com.virtual.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.virtual.core.models.DeviceType

class SecureStorageManager private constructor(context: Context) {

    private val sharedPreferences: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPreferences = try {
            EncryptedSharedPreferences.create(
                context,
                PREF_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback for older test environments
            context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val PREF_FILE_NAME = "virtual_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_UUID = "device_uuid"
        private const val KEY_DEVICE_ROLE = "device_role"
        private const val KEY_PAIRED_CAMERA_ID = "paired_camera_id"
        private const val KEY_PAIRED_CAMERA_NAME = "paired_camera_name"
        private const val KEY_IS_PAIRED = "is_paired"
        private const val KEY_CUSTOM_BASE_URL = "custom_base_url"

        @Volatile
        private var instance: SecureStorageManager? = null

        fun getInstance(context: Context): SecureStorageManager {
            return instance ?: synchronized(this) {
                instance ?: SecureStorageManager(context.applicationContext).also { instance = it }
            }
        }
    }

    var customBaseUrl: String?
        get() = sharedPreferences.getString(KEY_CUSTOM_BASE_URL, null)
        set(value) = sharedPreferences.edit().putString(KEY_CUSTOM_BASE_URL, value).apply()

    var accessToken: String?
        get() = sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
        set(value) = sharedPreferences.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var refreshToken: String?
        get() = sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
        set(value) = sharedPreferences.edit().putString(KEY_REFRESH_TOKEN, value).apply()

    var userId: String?
        get() = sharedPreferences.getString(KEY_USER_ID, null)
        set(value) = sharedPreferences.edit().putString(KEY_USER_ID, value).apply()

    var userEmail: String?
        get() = sharedPreferences.getString(KEY_USER_EMAIL, null)
        set(value) = sharedPreferences.edit().putString(KEY_USER_EMAIL, value).apply()

    var userName: String?
        get() = sharedPreferences.getString(KEY_USER_NAME, null)
        set(value) = sharedPreferences.edit().putString(KEY_USER_NAME, value).apply()

    var deviceId: String?
        get() = sharedPreferences.getString(KEY_DEVICE_ID, null)
        set(value) = sharedPreferences.edit().putString(KEY_DEVICE_ID, value).apply()

    var deviceUuid: String?
        get() = sharedPreferences.getString(KEY_DEVICE_UUID, null)
        set(value) = sharedPreferences.edit().putString(KEY_DEVICE_UUID, value).apply()

    var deviceRole: DeviceType?
        get() {
            val roleStr = sharedPreferences.getString(KEY_DEVICE_ROLE, null) ?: return null
            return try {
                DeviceType.valueOf(roleStr)
            } catch (e: Exception) {
                null
            }
        }
        set(value) = sharedPreferences.edit().putString(KEY_DEVICE_ROLE, value?.name).apply()

    var isPaired: Boolean
        get() = sharedPreferences.getBoolean(KEY_IS_PAIRED, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_IS_PAIRED, value).apply()

    var pairedCameraId: String?
        get() = sharedPreferences.getString(KEY_PAIRED_CAMERA_ID, null)
        set(value) = sharedPreferences.edit().putString(KEY_PAIRED_CAMERA_ID, value).apply()

    var pairedCameraName: String?
        get() = sharedPreferences.getString(KEY_PAIRED_CAMERA_NAME, null)
        set(value) = sharedPreferences.edit().putString(KEY_PAIRED_CAMERA_NAME, value).apply()

    fun isLoggedIn(): Boolean = !accessToken.isNullOrEmpty()

    fun savePairing(cameraId: String, cameraName: String) {
        sharedPreferences.edit()
            .putBoolean(KEY_IS_PAIRED, true)
            .putString(KEY_PAIRED_CAMERA_ID, cameraId)
            .putString(KEY_PAIRED_CAMERA_NAME, cameraName)
            .apply()
    }

    fun clearPairing() {
        sharedPreferences.edit()
            .putBoolean(KEY_IS_PAIRED, false)
            .remove(KEY_PAIRED_CAMERA_ID)
            .remove(KEY_PAIRED_CAMERA_NAME)
            .apply()
    }

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
}
