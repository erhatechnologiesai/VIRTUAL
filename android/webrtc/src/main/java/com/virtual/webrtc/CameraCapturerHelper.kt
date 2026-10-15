package com.virtual.webrtc

import android.content.Context
import android.util.Log
import org.webrtc.*

object CameraCapturerHelper {
    private const val TAG = "CameraCapturerHelper"

    fun createCameraCapturer(
        context: Context,
        isFrontFacing: Boolean = false,
        eventsHandler: CameraVideoCapturer.CameraEventsHandler? = null
    ): VideoCapturer? {
        val enumerator = if (Camera2Enumerator.isSupported(context)) {
            Log.d(TAG, "Using Camera2Enumerator")
            Camera2Enumerator(context)
        } else {
            Log.d(TAG, "Using Camera1Enumerator fallback")
            Camera1Enumerator(true)
        }

        val deviceNames = enumerator.deviceNames

        // 1. Try to find requested camera lens facing
        for (deviceName in deviceNames) {
            if (isFrontFacing && enumerator.isFrontFacing(deviceName)) {
                Log.d(TAG, "Found Front Camera: $deviceName")
                val capturer = enumerator.createCapturer(deviceName, eventsHandler)
                if (capturer != null) return capturer
            }
            if (!isFrontFacing && enumerator.isBackFacing(deviceName)) {
                Log.d(TAG, "Found Back Camera: $deviceName")
                val capturer = enumerator.createCapturer(deviceName, eventsHandler)
                if (capturer != null) return capturer
            }
        }

        // 2. Fallback to any available camera device
        for (deviceName in deviceNames) {
            val capturer = enumerator.createCapturer(deviceName, eventsHandler)
            if (capturer != null) {
                Log.d(TAG, "Using fallback camera: $deviceName")
                return capturer
            }
        }

        Log.e(TAG, "No camera device found on hardware")
        return null
    }

    fun getDimensionsForResolution(resolution: String): Pair<Int, Int> {
        return when (resolution.lowercase()) {
            "360p" -> Pair(640, 360)
            "480p" -> Pair(854, 480)
            "1080p" -> Pair(1920, 1080)
            else -> Pair(1280, 720) // default 720p
        }
    }
}
