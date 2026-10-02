package com.virtual.core.network

import com.virtual.core.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- Authentication ---
    @POST("api/auth/register")
    suspend fun register(@Body body: Map<String, String>): Response<ApiResponse<AuthResponse>>

    @POST("api/auth/login")
    suspend fun login(@Body body: Map<String, String>): Response<ApiResponse<AuthResponse>>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body body: Map<String, String>): Response<ApiResponse<AuthTokens>>

    @POST("api/auth/logout")
    suspend fun logout(): Response<ApiResponse<Map<String, String>>>

    // --- Devices ---
    @POST("api/devices")
    suspend fun registerDevice(@Body body: Map<String, Any>): Response<ApiResponse<Device>>

    @GET("api/devices")
    suspend fun getDevices(): Response<ApiResponse<List<Device>>>

    @GET("api/devices/{id}")
    suspend fun getDeviceById(@Path("id") id: String): Response<ApiResponse<Device>>

    @DELETE("api/devices/{id}")
    suspend fun deleteDevice(@Path("id") id: String): Response<ApiResponse<Map<String, Any>>>

    @POST("api/devices/{id}/telemetry")
    suspend fun sendTelemetry(
        @Path("id") id: String,
        @Body body: Map<String, Any>
    ): Response<ApiResponse<Map<String, Any>>>

    // --- Pairing ---
    @POST("api/pairing/create")
    suspend fun createPairing(@Body body: Map<String, String>): Response<ApiResponse<CreatePairingResponse>>

    @POST("api/pairing/join")
    suspend fun joinPairing(@Body body: Map<String, String>): Response<ApiResponse<PairingInfo>>

    @GET("api/pairing")
    suspend fun getPairings(): Response<ApiResponse<List<PairingInfo>>>

    @DELETE("api/pairing/{id}")
    suspend fun revokePairing(@Path("id") id: String): Response<ApiResponse<PairingInfo>>

    // --- Cameras & Sessions ---
    @GET("api/cameras")
    suspend fun getPairedCameras(): Response<ApiResponse<List<Device>>>

    @GET("api/cameras/{id}/status")
    suspend fun getCameraStatus(@Path("id") id: String): Response<ApiResponse<Map<String, Any>>>

    @POST("api/sessions")
    suspend fun startSession(@Body body: Map<String, String>): Response<ApiResponse<Map<String, Any>>>

    @DELETE("api/sessions/{id}")
    suspend fun endSession(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<ApiResponse<Map<String, Any>>>

    // --- Settings & WebRTC ---
    @GET("api/settings")
    suspend fun getSettings(@Query("deviceId") deviceId: String): Response<ApiResponse<DeviceSetting>>

    @PUT("api/settings")
    suspend fun updateSettings(
        @Query("deviceId") deviceId: String,
        @Body body: Map<String, Any>
    ): Response<ApiResponse<DeviceSetting>>

    @GET("api/webrtc/ice-servers")
    suspend fun getIceServers(): Response<ApiResponse<IceServersResponse>>
}

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: String?,
    val errors: List<Any>? = null
)
