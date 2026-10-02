package com.virtual.pairing

import android.content.Context
import com.virtual.core.models.CreatePairingResponse
import com.virtual.core.models.PairingInfo
import com.virtual.core.network.ApiClient
import com.virtual.core.storage.SecureStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class PairingRepository(private val context: Context) {

    private val api = ApiClient.getService(context)
    private val storage = SecureStorageManager.getInstance(context)

    suspend fun createPairingSession(): Result<CreatePairingResponse> = withContext(Dispatchers.IO) {
        try {
            val cameraId = storage.deviceId ?: return@withContext Result.failure(Exception("Device not registered"))
            val response = api.createPairing(mapOf("cameraDeviceId" to cameraId))

            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!)
            } else {
                val errorMsg = response.body()?.error ?: "Failed to generate pairing QR"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinPairingWithTokenOrCode(
        pairingToken: String? = null,
        pairingCode: String? = null
    ): Result<PairingInfo> = withContext(Dispatchers.IO) {
        try {
            val viewerId = storage.deviceId ?: return@withContext Result.failure(Exception("Device not registered"))

            val body = mutableMapOf("viewerDeviceId" to viewerId)
            if (!pairingToken.isNullOrEmpty()) {
                body["pairingToken"] = pairingToken
            }
            if (!pairingCode.isNullOrEmpty()) {
                body["pairingCode"] = pairingCode.trim().uppercase()
            }

            val response = api.joinPairing(body)
            if (response.isSuccessful && response.body()?.data != null) {
                val pairing = response.body()!!.data!
                // Save pairing permanently in local secure storage
                val cameraName = pairing.cameraDevice?.deviceName ?: "Home Camera"
                storage.savePairing(pairing.cameraDeviceId, cameraName)
                Result.success(pairing)
            } else {
                val errorMsg = response.body()?.error ?: "Pairing failed. Invalid or expired code."
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parseQrPayload(qrContent: String): Pair<String?, String?> {
        return try {
            val json = JSONObject(qrContent)
            val token = json.optString("token", null)
            val code = json.optString("code", null)
            Pair(token, code)
        } catch (e: Exception) {
            // Might be a raw token or raw code string
            if (qrContent.contains("-")) {
                Pair(null, qrContent.trim())
            } else {
                Pair(qrContent.trim(), null)
            }
        }
    }

    suspend fun getActivePairings(): Result<List<PairingInfo>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPairings()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!)
            } else {
                Result.failure(Exception(response.body()?.error ?: "Failed to fetch pairings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun revokePairing(pairingId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = api.revokePairing(pairingId)
            if (response.isSuccessful) {
                storage.clearPairing()
                Result.success(true)
            } else {
                Result.failure(Exception(response.body()?.error ?: "Failed to revoke pairing"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
