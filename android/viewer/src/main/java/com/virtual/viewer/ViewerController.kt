package com.virtual.viewer

import android.content.Context
import android.util.Log
import com.virtual.core.network.ApiClient
import com.virtual.core.storage.SecureStorageManager
import com.virtual.webrtc.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.webrtc.*

interface ViewerUiCallbacks {
    fun onConnectionStateChanged(state: String)
    fun onCameraBusy(busy: Boolean, message: String)
    fun onRemoteVideoReady(track: VideoTrack)
    fun onError(error: String)
}

class ViewerController(
    private val context: Context,
    private val callbacks: ViewerUiCallbacks
) : SignalingListener, WebRtcEvents {

    private val TAG = "VirtualViewerController"
    private val storage = SecureStorageManager.getInstance(context)
    private var signalingClient: WebRtcSignalingClient? = null
    private var webRtcManager: WebRtcManager? = null

    private var targetCameraId: String? = null
    private var sessionId: String? = null
    private var isAudioMuted = false
    private var isConnected = false
    private var remoteVideoTrack: VideoTrack? = null

    private val controllerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun connectToCamera(cameraId: String) {
        targetCameraId = cameraId
        Log.i(TAG, "Initiating WebRTC connection to Camera [$cameraId]")
        callbacks.onConnectionStateChanged("CONNECTING")

        controllerScope.launch {
            // 1. Fetch dynamic STUN/TURN ICE servers from backend
            fetchIceServers()

            // 2. Initialize WebRTC Manager
            webRtcManager = WebRtcManager(context, this@ViewerController)
            webRtcManager?.createPeerConnection()

            // 3. Connect Signaling
            signalingClient = WebRtcSignalingClient(storage, this@ViewerController)
            signalingClient?.connect()

            // 4. Create WebRTC session record in backend
            try {
                val viewerId = storage.deviceId ?: ""
                val response = ApiClient.getService(context).startSession(
                    mapOf("cameraDeviceId" to cameraId, "viewerDeviceId" to viewerId)
                )
                if (response.isSuccessful && response.body()?.data != null) {
                    sessionId = response.body()!!.data!!["id"] as? String
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not register session in backend: ${e.message}")
            }
        }
    }

    private suspend fun fetchIceServers() = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.getService(context).getIceServers()
            if (response.isSuccessful && response.body()?.data != null) {
                val servers = response.body()!!.data!!.iceServers
                webRtcManager?.setIceServers(servers)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Using default STUN server: ${e.message}")
        }
    }

    fun toggleAudio(): Boolean {
        isAudioMuted = !isAudioMuted
        // Mute remote audio track
        return isAudioMuted
    }

    fun reconnect() {
        Log.i(TAG, "Manual reconnect triggered")
        val cam = targetCameraId ?: return
        disconnect()
        connectToCamera(cam)
    }

    fun disconnect() {
        Log.i(TAG, "Disconnecting ViewerController...")
        callbacks.onConnectionStateChanged("DISCONNECTED")

        // End session in backend
        sessionId?.let { sid ->
            controllerScope.launch(Dispatchers.IO) {
                try {
                    ApiClient.getService(context).endSession(sid, mapOf("disconnectReason" to "USER_DISCONNECTED"))
                } catch (e: Exception) {}
            }
        }

        signalingClient?.disconnect()
        signalingClient = null

        webRtcManager?.close()
        webRtcManager = null

        remoteVideoTrack = null
        isConnected = false
    }

    // --- SignalingListener Callbacks ---
    override fun onConnected() {
        Log.d(TAG, "Signaling connected. Creating SDP Offer to receive camera video.")
        targetCameraId?.let {
            webRtcManager?.createOffer()
        }
    }

    override fun onDisconnected() {
        callbacks.onConnectionStateChanged("DISCONNECTED")
    }

    override fun onOfferReceived(fromDeviceId: String, sdp: String, sessionId: String?) {}

    override fun onAnswerReceived(fromDeviceId: String, sdp: String, sessionId: String?) {
        Log.i(TAG, "WebRTC Answer received from Camera [$fromDeviceId]")
        val answer = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        webRtcManager?.setRemoteDescription(answer) {
            callbacks.onConnectionStateChanged("CONNECTED")
            isConnected = true
        }
    }

    override fun onIceCandidateReceived(fromDeviceId: String, sdpMid: String, sdpMLineIndex: Int, candidate: String) {
        val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidate)
        webRtcManager?.addIceCandidate(iceCandidate)
    }

    override fun onCameraBusy(busy: Boolean, message: String) {
        Log.w(TAG, "Camera conflict update: busy=$busy, message=$message")
        callbacks.onCameraBusy(busy, message)
    }

    override fun onPairingSuccess(details: JSONObject) {}

    override fun onStreamStarted(cameraId: String) {
        Log.d(TAG, "Stream active on camera $cameraId")
    }

    override fun onStreamStopped(cameraId: String, reason: String) {
        Log.i(TAG, "Camera stopped stream: $reason")
        callbacks.onConnectionStateChanged("CAMERA_OFFLINE")
    }

    // --- WebRtcEvents Callbacks ---
    override fun onIceCandidateGenerated(candidate: IceCandidate) {
        targetCameraId?.let { camId ->
            signalingClient?.sendIceCandidate(camId, candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp)
        }
    }

    override fun onLocalDescriptionGenerated(sdp: SessionDescription) {
        targetCameraId?.let { camId ->
            if (sdp.type == SessionDescription.Type.OFFER) {
                signalingClient?.sendOffer(camId, sdp.description, sessionId)
            }
        }
    }

    override fun onRemoteVideoTrackReceived(track: VideoTrack) {
        Log.i(TAG, "Remote VideoTrack received successfully! Handing to UI renderer.")
        remoteVideoTrack = track
        track.setEnabled(true)
        controllerScope.launch {
            callbacks.onRemoteVideoReady(track)
            callbacks.onConnectionStateChanged("LIVE")
        }
    }

    override fun onConnectionStateChanged(state: PeerConnection.PeerConnectionState) {
        Log.d(TAG, "PeerConnection State: $state")
        when (state) {
            PeerConnection.PeerConnectionState.CONNECTED -> {
                callbacks.onConnectionStateChanged("LIVE")
            }
            PeerConnection.PeerConnectionState.DISCONNECTED,
            PeerConnection.PeerConnectionState.FAILED -> {
                callbacks.onConnectionStateChanged("RECONNECTING")
                // Automatic reconnect
                controllerScope.launch {
                    delay(2000)
                    reconnect()
                }
            }
            else -> {}
        }
    }

    override fun onIceConnectionStateChanged(state: PeerConnection.IceConnectionState) {
        Log.d(TAG, "ICE Connection State: $state")
    }

    override fun onError(message: String) {
        callbacks.onError(message)
    }
}
