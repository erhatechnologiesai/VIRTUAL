package com.virtual.webrtc

import android.util.Log
import com.virtual.core.network.ApiClient
import com.virtual.core.storage.SecureStorageManager
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.*
import org.json.JSONObject

interface SignalingListener {
    fun onConnected()
    fun onDisconnected()
    fun onOfferReceived(fromDeviceId: String, sdp: String, sessionId: String?)
    fun onAnswerReceived(fromDeviceId: String, sdp: String, sessionId: String?)
    fun onIceCandidateReceived(fromDeviceId: String, sdpMid: String, sdpMLineIndex: Int, candidate: String)
    fun onCameraBusy(busy: Boolean, message: String)
    fun onPairingSuccess(details: JSONObject)
    fun onStreamStarted(cameraId: String)
    fun onStreamStopped(cameraId: String, reason: String)
}

class WebRtcSignalingClient(
    private val storage: SecureStorageManager,
    private val listener: SignalingListener
) {
    private val TAG = "VirtualSignaling"
    private var socket: Socket? = null
    private var isManualDisconnect = false
    private var reconnectAttempt = 0
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun connect() {
        val token = storage.accessToken ?: return
        val deviceId = storage.deviceId ?: return

        isManualDisconnect = false

        try {
            val options = IO.Options().apply {
                auth = mapOf(
                    "token" to token,
                    "deviceId" to deviceId
                )
                reconnection = true
                reconnectionAttempts = 10
                reconnectionDelay = 1000
                reconnectionDelayMax = 15000
                timeout = 20000
            }

            // Derive socket server URL from ApiClient baseUrl
            val serverUri = ApiClient.baseUrl.removeSuffix("/")
            socket = IO.socket(serverUri, options)

            setupSocketListeners()
            socket?.connect()
            Log.d(TAG, "Connecting to signaling server at $serverUri for device $deviceId")
        } catch (e: Exception) {
            Log.e(TAG, "Socket connection error: ${e.message}", e)
            scheduleReconnect()
        }
    }

    private fun setupSocketListeners() {
        socket?.apply {
            on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Signaling connected successfully")
                reconnectAttempt = 0
                listener.onConnected()
            }

            on(Socket.EVENT_DISCONNECT) {
                Log.w(TAG, "Signaling disconnected")
                listener.onDisconnected()
                if (!isManualDisconnect) {
                    scheduleReconnect()
                }
            }

            on(Socket.EVENT_CONNECT_ERROR) { args ->
                val error = args.firstOrNull()?.toString() ?: "Unknown error"
                Log.e(TAG, "Signaling connection error: $error")
                listener.onDisconnected()
                scheduleReconnect()
            }

            on("webrtc_offer") { args ->
                try {
                    val data = args[0] as JSONObject
                    val fromDeviceId = data.getString("fromDeviceId")
                    val sdpObj = data.getJSONObject("sdp")
                    val sdp = sdpObj.getString("description")
                    val sessionId = data.optString("sessionId", null)
                    listener.onOfferReceived(fromDeviceId, sdp, sessionId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing webrtc_offer: ${e.message}")
                }
            }

            on("webrtc_answer") { args ->
                try {
                    val data = args[0] as JSONObject
                    val fromDeviceId = data.getString("fromDeviceId")
                    val sdpObj = data.getJSONObject("sdp")
                    val sdp = sdpObj.getString("description")
                    val sessionId = data.optString("sessionId", null)
                    listener.onAnswerReceived(fromDeviceId, sdp, sessionId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing webrtc_answer: ${e.message}")
                }
            }

            on("ice_candidate") { args ->
                try {
                    val data = args[0] as JSONObject
                    val fromDeviceId = data.getString("fromDeviceId")
                    val candObj = data.getJSONObject("candidate")
                    val sdpMid = candObj.getString("sdpMid")
                    val sdpMLineIndex = candObj.getInt("sdpMLineIndex")
                    val candidate = candObj.getString("candidate")
                    listener.onIceCandidateReceived(fromDeviceId, sdpMid, sdpMLineIndex, candidate)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing ice_candidate: ${e.message}")
                }
            }

            on("camera_busy") { args ->
                try {
                    val data = args[0] as JSONObject
                    val busy = data.getBoolean("busy")
                    val message = data.getString("message")
                    listener.onCameraBusy(busy, message)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing camera_busy: ${e.message}")
                }
            }

            on("pairing_success") { args ->
                try {
                    val data = args[0] as JSONObject
                    listener.onPairingSuccess(data)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing pairing_success: ${e.message}")
                }
            }

            on("stream_started") { args ->
                try {
                    val data = args[0] as JSONObject
                    val cameraId = data.getString("cameraId")
                    listener.onStreamStarted(cameraId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing stream_started: ${e.message}")
                }
            }

            on("stream_stopped") { args ->
                try {
                    val data = args[0] as JSONObject
                    val cameraId = data.getString("cameraId")
                    val reason = data.optString("reason", "NORMAL")
                    listener.onStreamStopped(cameraId, reason)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing stream_stopped: ${e.message}")
                }
            }
        }
    }

    fun sendOffer(targetDeviceId: String, sdp: String, sessionId: String? = null) {
        val payload = JSONObject().apply {
            put("targetDeviceId", targetDeviceId)
            put("sessionId", sessionId)
            put("sdp", JSONObject().apply {
                put("type", "offer")
                put("description", sdp)
            })
        }
        socket?.emit("webrtc_offer", payload)
        Log.d(TAG, "Sent WebRTC Offer to $targetDeviceId")
    }

    fun sendAnswer(targetDeviceId: String, sdp: String, sessionId: String? = null) {
        val payload = JSONObject().apply {
            put("targetDeviceId", targetDeviceId)
            put("sessionId", sessionId)
            put("sdp", JSONObject().apply {
                put("type", "answer")
                put("description", sdp)
            })
        }
        socket?.emit("webrtc_answer", payload)
        Log.d(TAG, "Sent WebRTC Answer to $targetDeviceId")
    }

    fun sendIceCandidate(targetDeviceId: String, sdpMid: String, sdpMLineIndex: Int, candidate: String) {
        val payload = JSONObject().apply {
            put("targetDeviceId", targetDeviceId)
            put("candidate", JSONObject().apply {
                put("sdpMid", sdpMid)
                put("sdpMLineIndex", sdpMLineIndex)
                put("candidate", candidate)
            })
        }
        socket?.emit("ice_candidate", payload)
    }

    fun sendCameraBusy(busy: Boolean, conflictingApp: String? = null, targetDeviceId: String? = null) {
        val payload = JSONObject().apply {
            put("busy", busy)
            if (conflictingApp != null) put("conflictingApp", conflictingApp)
            if (targetDeviceId != null) put("targetDeviceId", targetDeviceId)
        }
        socket?.emit("camera_busy", payload)
        Log.w(TAG, "Sent camera_busy notification (busy=$busy)")
    }

    fun sendStreamStarted(targetDeviceId: String?, resolution: String = "720p", fps: Int = 30) {
        val payload = JSONObject().apply {
            if (targetDeviceId != null) put("targetDeviceId", targetDeviceId)
            put("resolution", resolution)
            put("fps", fps)
        }
        socket?.emit("stream_started", payload)
        Log.d(TAG, "Sent stream_started notification")
    }

    fun sendStreamStopped(targetDeviceId: String?, reason: String = "user_action") {
        val payload = JSONObject().apply {
            if (targetDeviceId != null) put("targetDeviceId", targetDeviceId)
            put("reason", reason)
        }
        socket?.emit("stream_stopped", payload)
        Log.d(TAG, "Sent stream_stopped notification")
    }

    private fun scheduleReconnect() {
        if (isManualDisconnect) return

        reconnectAttempt++
        // Exponential backoff: 1s, 2s, 4s, 8s, up to 30s
        val delayMs = (Math.pow(2.0, (reconnectAttempt - 1).toDouble()) * 1000).toLong().coerceAtMost(30000L)

        Log.d(TAG, "Scheduling signaling reconnect in ${delayMs / 1000}s (Attempt $reconnectAttempt)")
        coroutineScope.launch {
            delay(delayMs)
            if (!isManualDisconnect && (socket == null || !socket!!.connected())) {
                connect()
            }
        }
    }

    fun disconnect() {
        isManualDisconnect = true
        socket?.disconnect()
        socket = null
        Log.d(TAG, "Signaling manually disconnected")
    }
}
