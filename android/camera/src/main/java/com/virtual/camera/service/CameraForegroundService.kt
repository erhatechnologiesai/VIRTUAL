package com.virtual.camera.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.virtual.camera.conflict.CameraConflictDetector
import com.virtual.camera.conflict.CameraConflictListener
import com.virtual.core.storage.SecureStorageManager
import com.virtual.core.telemetry.DeviceStatusMonitor
import com.virtual.webrtc.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.webrtc.*

class CameraForegroundService : Service(), SignalingListener, WebRtcEvents, CameraConflictListener {

    private val TAG = "VirtualCameraService"
    private val NOTIFICATION_CHANNEL_ID = "virtual_camera_stream_channel"
    private val NOTIFICATION_ID = 1001

    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var storage: SecureStorageManager
    private lateinit var telemetryMonitor: DeviceStatusMonitor
    private var signalingClient: WebRtcSignalingClient? = null
    private var webRtcManager: WebRtcManager? = null
    private var cameraConflictDetector: CameraConflictDetector? = null
    private var videoCapturer: VideoCapturer? = null

    private var currentViewerDeviceId: String? = null
    private var currentSessionId: String? = null
    private var isStreaming = false
    private var isCameraHardwareBusy = false
    private var isFrontFacingCamera = false
    private var currentResolution = "720p"
    private var currentFps = 30

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        const val ACTION_START_STREAM = "com.virtual.camera.ACTION_START_STREAM"
        const val ACTION_STOP_STREAM = "com.virtual.camera.ACTION_STOP_STREAM"
        const val ACTION_SWITCH_CAMERA = "com.virtual.camera.ACTION_SWITCH_CAMERA"
        const val EXTRA_RESOLUTION = "extra_resolution"
        const val EXTRA_FPS = "extra_fps"
        const val EXTRA_FRONT_FACING = "extra_front_facing"

        // State notification broadcast
        const val BROADCAST_STREAM_STATUS = "com.virtual.camera.BROADCAST_STREAM_STATUS"
        const val EXTRA_IS_STREAMING = "extra_is_streaming"
        const val EXTRA_IS_BUSY = "extra_is_busy"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CameraForegroundService onCreate")
        storage = SecureStorageManager.getInstance(applicationContext)
        telemetryMonitor = DeviceStatusMonitor(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_STREAM -> {
                currentResolution = intent.getStringExtra(EXTRA_RESOLUTION) ?: "720p"
                currentFps = intent.getIntExtra(EXTRA_FPS, 30)
                isFrontFacingCamera = intent.getBooleanExtra(EXTRA_FRONT_FACING, false)
                startForegroundStream()
            }
            ACTION_STOP_STREAM -> {
                stopForegroundStream()
            }
            ACTION_SWITCH_CAMERA -> {
                switchCameraLens()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundStream() {
        Log.i(TAG, "Starting VIRTUAL camera foreground stream...")

        // 1. Promote to Foreground Service with Camera type
        val notification = buildNotification("Live Camera Active", "Streaming feed to authorized viewers")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
            } else {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // 2. Acquire Partial WakeLock for Screen-Off / Locked mode
        acquireWakeLock()

        // 3. Initialize WebRTC and Camera Capturer
        initWebRtcAndCapture()

        // 4. Initialize Signaling Client
        signalingClient = WebRtcSignalingClient(storage, this)
        signalingClient?.connect()

        // 5. Initialize Camera Conflict Detector
        cameraConflictDetector = CameraConflictDetector(this, this)
        cameraConflictDetector?.startMonitoring("0") // default back camera id

        // 6. Start telemetry reporting
        telemetryMonitor.startReporting {
            when {
                isCameraHardwareBusy -> "CONFLICT_BUSY"
                isStreaming -> "STREAMING"
                else -> "ONLINE"
            }
        }

        isStreaming = true
        broadcastState()
    }

    private fun initWebRtcAndCapture() {
        webRtcManager = WebRtcManager(this, this)
        webRtcManager?.createPeerConnection()

        videoCapturer = CameraCapturerHelper.createCameraCapturer(this, isFrontFacingCamera)
        val (width, height) = CameraCapturerHelper.getDimensionsForResolution(currentResolution)

        if (videoCapturer != null) {
            webRtcManager?.initLocalVideoTrack(videoCapturer!!, width, height, currentFps)
            webRtcManager?.initLocalAudioTrack(false) // Audio optional, default disabled
            Log.d(TAG, "Camera capturer initialized successfully")
        } else {
            Log.e(TAG, "Failed to create camera capturer")
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "virtual:CameraStreamWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // 12 hours max
            }
            Log.d(TAG, "Partial WakeLock acquired for Screen-Off streaming")
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake lock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock: ${e.message}")
        }
    }

    private fun switchCameraLens() {
        isFrontFacingCamera = !isFrontFacingCamera
        try {
            val cameraVideoCapturer = videoCapturer as? CameraVideoCapturer
            cameraVideoCapturer?.switchCamera(null)
            Log.d(TAG, "Switched camera lens facing. Front=$isFrontFacingCamera")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch camera: ${e.message}")
        }
    }

    private fun stopForegroundStream() {
        Log.i(TAG, "Stopping VIRTUAL camera stream...")

        isStreaming = false
        signalingClient?.sendStreamStopped(currentViewerDeviceId, "user_stopped")
        signalingClient?.disconnect()
        signalingClient = null

        cameraConflictDetector?.stopMonitoring()
        cameraConflictDetector = null

        telemetryMonitor.stopReporting()

        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            videoCapturer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping video capturer: ${e.message}")
        }

        webRtcManager?.close()
        webRtcManager = null

        releaseWakeLock()
        broadcastState()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // --- Signaling Callbacks ---
    override fun onConnected() {
        Log.d(TAG, "Signaling connected. Announcing ready state.")
    }

    override fun onDisconnected() {
        Log.w(TAG, "Signaling disconnected.")
    }

    override fun onOfferReceived(fromDeviceId: String, sdp: String, sessionId: String?) {
        Log.i(TAG, "Received WebRTC Offer from Viewer [$fromDeviceId]")
        currentViewerDeviceId = fromDeviceId
        currentSessionId = sessionId

        val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, sdp)
        webRtcManager?.setRemoteDescription(sessionDescription) {
            // Generate Answer
            webRtcManager?.createAnswer()
        }
    }

    override fun onAnswerReceived(fromDeviceId: String, sdp: String, sessionId: String?) {
        Log.i(TAG, "Received WebRTC Answer from Viewer [$fromDeviceId]")
        val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        webRtcManager?.setRemoteDescription(sessionDescription)
    }

    override fun onIceCandidateReceived(fromDeviceId: String, sdpMid: String, sdpMLineIndex: Int, candidate: String) {
        val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidate)
        webRtcManager?.addIceCandidate(iceCandidate)
    }

    override fun onCameraBusy(busy: Boolean, message: String) {}

    override fun onPairingSuccess(details: JSONObject) {
        Log.i(TAG, "Pairing confirmed by signaling: $details")
    }

    override fun onStreamStarted(cameraId: String) {}

    override fun onStreamStopped(cameraId: String, reason: String) {
        Log.i(TAG, "Stream stopped by remote peer: $reason")
    }

    // --- WebRTC Events ---
    override fun onIceCandidateGenerated(candidate: IceCandidate) {
        currentViewerDeviceId?.let { viewerId ->
            signalingClient?.sendIceCandidate(viewerId, candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp)
        }
    }

    override fun onLocalDescriptionGenerated(sdp: SessionDescription) {
        currentViewerDeviceId?.let { viewerId ->
            if (sdp.type == SessionDescription.Type.ANSWER) {
                signalingClient?.sendAnswer(viewerId, sdp.description, currentSessionId)
                signalingClient?.sendStreamStarted(viewerId, currentResolution, currentFps)
            } else if (sdp.type == SessionDescription.Type.OFFER) {
                signalingClient?.sendOffer(viewerId, sdp.description, currentSessionId)
            }
        }
    }

    override fun onRemoteVideoTrackReceived(track: VideoTrack) {}

    override fun onConnectionStateChanged(state: PeerConnection.PeerConnectionState) {
        Log.d(TAG, "PeerConnection State: $state")
    }

    override fun onIceConnectionStateChanged(state: PeerConnection.IceConnectionState) {
        Log.d(TAG, "ICE Connection State: $state")
    }

    override fun onError(message: String) {
        Log.e(TAG, "WebRTC Error: $message")
    }

    // --- Camera Conflict Listener (Section 18) ---
    override fun onCameraConflictDetected(cameraId: String) {
        Log.w(TAG, "Camera conflict detected! Third-party app acquired camera.")
        isCameraHardwareBusy = true
        signalingClient?.sendCameraBusy(true, "Third-party camera app", currentViewerDeviceId)

        updateNotification("Camera In Use by Another App", "VIRTUAL will resume automatically when released.")
        broadcastState()
    }

    override fun onCameraAvailableAgain(cameraId: String) {
        Log.i(TAG, "Camera conflict resolved. Hardware available again. Resuming stream.")
        isCameraHardwareBusy = false
        signalingClient?.sendCameraBusy(false, null, currentViewerDeviceId)

        // Resume video capture
        try {
            val (width, height) = CameraCapturerHelper.getDimensionsForResolution(currentResolution)
            videoCapturer?.startCapture(width, height, currentFps)
            signalingClient?.sendStreamStarted(currentViewerDeviceId, currentResolution, currentFps)
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming capture: ${e.message}")
        }

        updateNotification("Live Camera Active", "Streaming feed to authorized viewers")
        broadcastState()
    }

    private fun broadcastState() {
        val intent = Intent(BROADCAST_STREAM_STATUS).apply {
            putExtra(EXTRA_IS_STREAMING, isStreaming)
            putExtra(EXTRA_IS_BUSY, isCameraHardwareBusy)
        }
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "VIRTUAL Camera Stream Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live streaming status for VIRTUAL camera device"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val stopIntent = Intent(this, CameraForegroundService::class.java).apply {
            action = ACTION_STOP_STREAM
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop Streaming", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(title: String, text: String) {
        val notification = buildNotification(title, text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForegroundStream()
        serviceScope.cancel()
        Log.d(TAG, "CameraForegroundService onDestroy")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
