package com.virtual.core.models

import com.google.gson.annotations.SerializedName

enum class DeviceType {
    CAMERA,
    VIEWER
}

enum class DeviceStatus {
    ONLINE,
    OFFLINE,
    STREAMING,
    CONFLICT_BUSY
}

data class Device(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("device_uuid") val deviceUuid: String,
    @SerializedName("device_name") val deviceName: String,
    @SerializedName("device_type") val deviceType: DeviceType,
    val platform: String = "ANDROID",
    @SerializedName("app_version") val appVersion: String = "1.0.0",
    val status: DeviceStatus = DeviceStatus.OFFLINE,
    @SerializedName("last_seen") val lastSeen: String? = null,
    val cameraDetail: CameraDetail? = null,
    val settings: DeviceSetting? = null
)

data class CameraDetail(
    val id: String? = null,
    @SerializedName("camera_type") val cameraType: String = "BACK",
    val resolution: String = "720p",
    val fps: Int = 30,
    @SerializedName("audio_enabled") val audioEnabled: Boolean = false,
    @SerializedName("stream_status") val streamStatus: String = "IDLE",
    @SerializedName("battery_level") val batteryLevel: Int = 100,
    @SerializedName("network_type") val networkType: String = "WIFI",
    @SerializedName("last_stream_started") val lastStreamStarted: String? = null
)

data class DeviceSetting(
    val id: String? = null,
    @SerializedName("video_quality") val videoQuality: String = "720p",
    val fps: Int = 30,
    @SerializedName("audio_enabled") val audioEnabled: Boolean = false,
    @SerializedName("auto_reconnect") val autoReconnect: Boolean = true
)

data class PairingInfo(
    val id: String,
    @SerializedName("camera_device_id") val cameraDeviceId: String,
    @SerializedName("viewer_device_id") val viewerDeviceId: String,
    @SerializedName("pairing_status") val pairingStatus: String,
    @SerializedName("paired_at") val pairedAt: String?,
    val cameraDevice: Device?,
    val viewerDevice: Device?
)

data class CreatePairingResponse(
    val pairingId: String,
    val cameraDeviceId: String,
    val cameraUuid: String,
    val pairingCode: String,
    val pairingToken: String,
    val qrData: String,
    val expiresAt: String,
    val expiresInSeconds: Int
)

data class AuthResponse(
    val user: UserInfo,
    val tokens: AuthTokens
)

data class UserInfo(
    val id: String,
    val name: String,
    val email: String
)

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: String = "1h"
)

data class IceServerConfig(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null
)

data class IceServersResponse(
    val iceServers: List<IceServerConfig>
)
