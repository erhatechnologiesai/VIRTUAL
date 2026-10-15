package com.virtual.app.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.virtual.app.navigation.Screen
import com.virtual.app.ui.theme.*
import com.virtual.viewer.ViewerController
import com.virtual.viewer.ViewerUiCallbacks
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun LiveViewScreen(
    cameraId: String,
    cameraName: String,
    onNavigateBack: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val context = LocalContext.current

    var connectionState by remember { mutableStateOf("CONNECTING") }
    var isCameraBusy by remember { mutableStateOf(false) }
    var busyMessage by remember { mutableStateOf("") }
    var isAudioMuted by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    var remoteVideoTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var rendererRef by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    val viewerController = remember {
        ViewerController(context, object : ViewerUiCallbacks {
            override fun onConnectionStateChanged(state: String) {
                connectionState = state
            }

            override fun onCameraBusy(busy: Boolean, message: String) {
                isCameraBusy = busy
                busyMessage = message
            }

            override fun onRemoteVideoReady(track: VideoTrack) {
                remoteVideoTrack = track
                rendererRef?.let { renderer ->
                    try {
                        track.addSink(renderer)
                    } catch (e: Exception) {}
                }
            }

            override fun onError(error: String) {
                connectionState = "ERROR"
            }
        })
    }

    LaunchedEffect(cameraId) {
        viewerController.connectToCamera(cameraId)
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                rendererRef?.release()
            } catch (e: Exception) {}
            viewerController.disconnect()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. WebRTC Remote Video Surface
        AndroidView(
            factory = { ctx ->
                SurfaceViewRenderer(ctx).apply {
                    try {
                        init(viewerController.eglBase.eglBaseContext, null)
                    } catch (e: Exception) {
                        android.util.Log.e("LiveViewScreen", "SurfaceViewRenderer init error: ${e.message}")
                    }
                    setEnableHardwareScaler(true)
                    setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    rendererRef = this
                    remoteVideoTrack?.let { track ->
                        try { track.addSink(this) } catch (e: Exception) {}
                    }
                }
            },
            update = { renderer ->
                remoteVideoTrack?.let { track ->
                    try { track.addSink(renderer) } catch (e: Exception) {}
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Camera Hardware Busy Overlay (Section 18)
        if (isCameraBusy) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = VirtualWhite)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⚠️", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Camera Temporarily Unavailable",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = VirtualTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = busyMessage.ifBlank {
                                "Another application is currently using the camera hardware. VIRTUAL will retry when the camera becomes available."
                            },
                            fontSize = 14.sp,
                            color = VirtualTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = VirtualBluePrimary,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }

        // 3. Top Header Bar
        if (!isFullScreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.TopCenter)
                    .zIndex(30f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.size(24.dp)) {
                            Text("←", color = VirtualWhite, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = cameraName,
                            color = VirtualWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable { onNavigateSettings() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚙️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Settings", color = VirtualWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. Bottom Live Controls Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
                .zIndex(30f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = VirtualWhite.copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(10.dp),
                            shape = CircleShape,
                            color = if (connectionState == "LIVE") VirtualOnlineGreen else VirtualWarningAmber
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (connectionState == "LIVE") "LIVE 720p" else connectionState,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = VirtualTextPrimary
                        )
                    }

                    // Audio Toggle
                    IconButton(
                        onClick = { isAudioMuted = viewerController.toggleAudio() }
                    ) {
                        Text(if (isAudioMuted) "🔇" else "🔊", fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = VirtualBorder)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { isFullScreen = !isFullScreen },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isFullScreen) "Exit" else "⛶ Full", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onNavigateSettings,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("⚙️ Settings", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { viewerController.reconnect() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VirtualBluePrimary),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text("↻ Reconnect", fontSize = 12.sp, color = VirtualWhite)
                    }
                }
            }
        }
    }
}
