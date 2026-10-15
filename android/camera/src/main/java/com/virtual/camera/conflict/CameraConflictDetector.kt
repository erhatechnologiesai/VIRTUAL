package com.virtual.camera.conflict

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Log

interface CameraConflictListener {
    fun onCameraConflictDetected(cameraId: String)
    fun onCameraAvailableAgain(cameraId: String)
}

class CameraConflictDetector(
    private val context: Context,
    private val listener: CameraConflictListener
) {
    private val TAG = "CameraConflictDetector"
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isStreamingActive = false
    private var activeCameraId: String? = null

    @Volatile var isSelfCapturing: Boolean = false

    private val availabilityCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            super.onCameraAvailable(cameraId)
            Log.d(TAG, "Hardware Camera available: $cameraId (isSelfCapturing=$isSelfCapturing)")
            if (isStreamingActive && cameraId == activeCameraId) {
                mainHandler.post {
                    listener.onCameraAvailableAgain(cameraId)
                }
            }
        }

        override fun onCameraUnavailable(cameraId: String) {
            super.onCameraUnavailable(cameraId)
            Log.d(TAG, "Hardware Camera unavailable callback: $cameraId (isSelfCapturing=$isSelfCapturing)")
            // When VIRTUAL itself opens the camera, Android OS marks it unavailable to third-party apps.
            // This is expected and must NOT be treated as a conflict.
            if (isStreamingActive && cameraId == activeCameraId && !isSelfCapturing) {
                Log.w(TAG, "External conflict: Camera $cameraId acquired by third-party app!")
                mainHandler.post {
                    listener.onCameraConflictDetected(cameraId)
                }
            }
        }
    }

    fun startMonitoring(cameraId: String) {
        activeCameraId = cameraId
        isStreamingActive = true
        try {
            cameraManager.registerAvailabilityCallback(availabilityCallback, mainHandler)
            Log.d(TAG, "Camera conflict monitoring started for camera $cameraId")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering camera availability callback: ${e.message}")
        }
    }

    fun stopMonitoring() {
        isStreamingActive = false
        try {
            cameraManager.unregisterAvailabilityCallback(availabilityCallback)
            Log.d(TAG, "Camera conflict monitoring stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering camera availability callback: ${e.message}")
        }
    }
}
