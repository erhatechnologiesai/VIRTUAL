package com.virtual.camera.camerax

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

class CameraXPreviewManager(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null

    fun startPreview(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        isFrontFacing: Boolean = false,
        onCameraBound: () -> Unit = {}
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val cameraSelector = if (isFrontFacing) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                onCameraBound()
            } catch (exc: Exception) {
                // Camera binding failed
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopPreview() {
        cameraProvider?.unbindAll()
    }
}
