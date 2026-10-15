package com.virtual.webrtc

import android.content.Context
import android.util.Log
import com.virtual.core.models.IceServerConfig
import org.webrtc.*
import java.nio.ByteBuffer

interface WebRtcEvents {
    fun onIceCandidateGenerated(candidate: IceCandidate)
    fun onLocalDescriptionGenerated(sdp: SessionDescription)
    fun onRemoteVideoTrackReceived(track: VideoTrack)
    fun onConnectionStateChanged(state: PeerConnection.PeerConnectionState)
    fun onIceConnectionStateChanged(state: PeerConnection.IceConnectionState)
    fun onError(message: String)
}

class WebRtcManager(
    private val context: Context,
    private val events: WebRtcEvents,
    customEglBase: EglBase? = null
) {
    private val TAG = "VirtualWebRTC"

    val eglBase: EglBase = customEglBase ?: EglBase.create()
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private val queuedCandidates = mutableListOf<IceCandidate>()

    private var localVideoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    private var iceServers: List<PeerConnection.IceServer> = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun.cloudflare.com:3478").createIceServer()
    )

    init {
        initPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(
            eglBase.eglBaseContext,
            true, // enableIntelVp8Encoder
            true  // enableH264HighProfile
        )
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(defaultVideoEncoderFactory)
            .setVideoDecoderFactory(defaultVideoDecoderFactory)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()

        Log.d(TAG, "WebRTC PeerConnectionFactory initialized with hardware acceleration")
    }

    fun setIceServers(servers: List<IceServerConfig>) {
        val list = mutableListOf<PeerConnection.IceServer>()
        for (server in servers) {
            for (url in server.urls) {
                val builder = PeerConnection.IceServer.builder(url)
                if (!server.username.isNullOrEmpty() && !server.credential.isNullOrEmpty()) {
                    builder.setUsername(server.username)
                    builder.setPassword(server.credential)
                }
                list.add(builder.createIceServer())
            }
        }
        if (list.isNotEmpty()) {
            this.iceServers = list
            Log.d(TAG, "Updated ICE servers with ${list.size} server(s)")
        }
    }

    fun createPeerConnection(): PeerConnection? {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            keyType = PeerConnection.KeyType.ECDSA
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    Log.d(TAG, "Local ICE candidate discovered: ${it.sdp}")
                    events.onIceCandidateGenerated(it)
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}

            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(TAG, "Signaling state changed: $state")
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE Connection state changed: $state")
                state?.let { events.onIceConnectionStateChanged(it) }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                Log.d(TAG, "PeerConnection state changed: $newState")
                newState?.let { events.onConnectionStateChanged(it) }
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "ICE Gathering state: $state")
            }

            override fun onAddStream(stream: MediaStream?) {}

            override fun onRemoveStream(stream: MediaStream?) {}

            override fun onDataChannel(dataChannel: DataChannel?) {}

            override fun onRenegotiationNeeded() {
                Log.d(TAG, "Renegotiation needed")
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                Log.d(TAG, "WebRTC onAddTrack received: ${receiver?.track()?.kind()}")
                val track = receiver?.track()
                if (track is VideoTrack) {
                    events.onRemoteVideoTrackReceived(track)
                }
            }
        })

        return peerConnection
    }

    fun initLocalVideoTrack(capturer: VideoCapturer, width: Int = 1280, height: Int = 720, fps: Int = 30): VideoTrack? {
        val surfaceTextureHelper = SurfaceTextureHelper.create("VirtualCamCapturerThread", eglBase.eglBaseContext)
        localVideoSource = peerConnectionFactory?.createVideoSource(capturer.isScreencast)
        capturer.initialize(surfaceTextureHelper, context, localVideoSource?.capturerObserver)
        capturer.startCapture(width, height, fps)

        localVideoTrack = peerConnectionFactory?.createVideoTrack("VIRTUAL_VIDEO_TRACK", localVideoSource)
        localVideoTrack?.setEnabled(true)

        // Attach local track to peer connection
        localVideoTrack?.let {
            peerConnection?.addTrack(it, listOf("VIRTUAL_STREAM"))
        }

        Log.d(TAG, "Local Video Track created: ${width}x${height} @ ${fps}fps")
        return localVideoTrack
    }

    fun initLocalAudioTrack(enabled: Boolean = true): AudioTrack? {
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        }

        localAudioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack("VIRTUAL_AUDIO_TRACK", localAudioSource)
        localAudioTrack?.setEnabled(enabled)

        localAudioTrack?.let {
            peerConnection?.addTrack(it, listOf("VIRTUAL_STREAM"))
        }

        Log.d(TAG, "Local Audio Track created (enabled=$enabled)")
        return localAudioTrack
    }

    fun setAudioEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun createOffer(mediaConstraints: MediaConstraints = MediaConstraints()) {
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))

        peerConnection?.createOffer(object : SdpObserverAdapter("createOffer") {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let { sdp ->
                    peerConnection?.setLocalDescription(object : SdpObserverAdapter("setLocalDescription") {
                        override fun onSetSuccess() {
                            events.onLocalDescriptionGenerated(sdp)
                        }
                    }, sdp)
                }
            }
        }, mediaConstraints)
    }

    fun createAnswer(mediaConstraints: MediaConstraints = MediaConstraints()) {
        peerConnection?.createAnswer(object : SdpObserverAdapter("createAnswer") {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let { sdp ->
                    peerConnection?.setLocalDescription(object : SdpObserverAdapter("setLocalDescription") {
                        override fun onSetSuccess() {
                            events.onLocalDescriptionGenerated(sdp)
                        }
                    }, sdp)
                }
            }
        }, mediaConstraints)
    }

    fun resetPeerConnectionForNewSession(): PeerConnection? {
        try {
            Log.d(TAG, "Resetting PeerConnection for new session / reconnect")
            peerConnection?.dispose()
            peerConnection = null
            synchronized(queuedCandidates) {
                queuedCandidates.clear()
            }
            createPeerConnection()
            // Re-attach existing video & audio tracks to new peer connection
            localVideoTrack?.let {
                peerConnection?.addTrack(it, listOf("VIRTUAL_STREAM"))
                Log.d(TAG, "Re-attached local video track to fresh peer connection")
            }
            localAudioTrack?.let {
                peerConnection?.addTrack(it, listOf("VIRTUAL_STREAM"))
                Log.d(TAG, "Re-attached local audio track to fresh peer connection")
            }
            return peerConnection
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting PeerConnection: ${e.message}")
            return null
        }
    }

    fun setRemoteDescription(sdp: SessionDescription, onComplete: () -> Unit = {}, onError: (String) -> Unit = {}) {
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter("setRemoteDescription") {
            override fun onSetSuccess() {
                Log.d(TAG, "Remote description set successfully: ${sdp.type}")
                synchronized(queuedCandidates) {
                    if (queuedCandidates.isNotEmpty()) {
                        Log.d(TAG, "Draining ${queuedCandidates.size} queued ICE candidates")
                        for (cand in queuedCandidates) {
                            peerConnection?.addIceCandidate(cand)
                        }
                        queuedCandidates.clear()
                    }
                }
                onComplete()
            }

            override fun onSetFailure(error: String?) {
                Log.e(TAG, "setRemoteDescription FAILED: $error")
                onError(error ?: "Unknown error setting remote description")
            }
        }, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        if (peerConnection?.remoteDescription != null) {
            peerConnection?.addIceCandidate(candidate)
        } else {
            synchronized(queuedCandidates) {
                queuedCandidates.add(candidate)
            }
            Log.d(TAG, "Queued ICE candidate waiting for remote description: ${candidate.sdpMid}")
        }
    }

    fun close() {
        try {
            localVideoTrack?.setEnabled(false)
            localAudioTrack?.setEnabled(false)
            peerConnection?.close()
            peerConnection = null
            localVideoSource?.dispose()
            localVideoSource = null
            localAudioSource?.dispose()
            localAudioSource = null
            Log.d(TAG, "WebRTC resources released successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing WebRTC: ${e.message}")
        }
    }
}

open class SdpObserverAdapter(private val tag: String) : SdpObserver {
    override fun onCreateSuccess(desc: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {
        Log.e("SdpObserverAdapter", "$tag onCreateFailure: $error")
    }
    override fun onSetFailure(error: String?) {
        Log.e("SdpObserverAdapter", "$tag onSetFailure: $error")
    }
}
