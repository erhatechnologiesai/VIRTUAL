package com.virtual.viewer

import android.content.Context
import com.virtual.core.models.Device
import com.virtual.core.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ViewerRepository(private val context: Context) {

    private val api = ApiClient.getService(context)

    suspend fun getPairedCameras(): Result<List<Device>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPairedCameras()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!)
            } else {
                Result.failure(Exception(response.body()?.error ?: "Failed to load cameras"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCameraStatus(cameraId: String): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getCameraStatus(cameraId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!)
            } else {
                Result.failure(Exception(response.body()?.error ?: "Failed to load status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
