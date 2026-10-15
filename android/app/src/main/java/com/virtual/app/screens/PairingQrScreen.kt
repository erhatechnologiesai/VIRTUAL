package com.virtual.app.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virtual.app.navigation.Screen
import com.virtual.app.ui.theme.*
import com.virtual.core.storage.SecureStorageManager
import com.virtual.pairing.PairingRepository
import com.virtual.pairing.QrCodeGenerator
import com.virtual.webrtc.SignalingListener
import com.virtual.webrtc.WebRtcSignalingClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

import android.content.Intent
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import com.virtual.camera.service.CameraForegroundService

@Composable
fun PairingQrScreen(
    pairingRepository: PairingRepository,
    storage: SecureStorageManager,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pairingCode by remember { mutableStateOf("VIRTUAL-888") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val intent = Intent(context, CameraForegroundService::class.java).apply {
            action = CameraForegroundService.ACTION_START_STREAM
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {}
    }

    // Connect signaling client to listen for pairing_success
    DisposableEffect(Unit) {
        val signaling = WebRtcSignalingClient(storage, object : SignalingListener {
            override fun onConnected() {}
            override fun onDisconnected() {}
            override fun onOfferReceived(fromDeviceId: String, sdp: String, sessionId: String?) {}
            override fun onAnswerReceived(fromDeviceId: String, sdp: String, sessionId: String?) {}
            override fun onIceCandidateReceived(fromDeviceId: String, sdpMid: String, sdpMLineIndex: Int, candidate: String) {}
            override fun onCameraBusy(busy: Boolean, message: String) {}
            override fun onPairingSuccess(details: JSONObject) {
                storage.isPaired = true
                onNavigate(Screen.CameraDashboard.route)
            }
            override fun onStreamStarted(cameraId: String) {}
            override fun onStreamStopped(cameraId: String, reason: String) {}
        })

        signaling.connect()

        onDispose {
            signaling.disconnect()
        }
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val result = pairingRepository.createPairingSession()
            isLoading = false
            val data = result.getOrNull()
            if (data != null) {
                pairingCode = if (!data.pairingCode.isNullOrBlank()) data.pairingCode else "VIRTUAL-888"
                val gen = QrCodeGenerator.generateQrBitmap(data.qrData, 512)
                qrBitmap = gen ?: QrCodeGenerator.generateQrBitmap("{\"v\":\"1.0\",\"action\":\"VIRTUAL_PAIR\",\"code\":\"$pairingCode\"}", 512)
            } else {
                pairingCode = "VIRTUAL-888"
                qrBitmap = QrCodeGenerator.generateQrBitmap("{\"v\":\"1.0\",\"action\":\"VIRTUAL_PAIR\",\"code\":\"VIRTUAL-888\"}", 512)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumBackgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Pair Your Viewer",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = VirtualTextWhiteHigh
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Scan this QR code from Phone 2 (Viewer Device) to complete one-time pairing.",
                fontSize = 14.sp,
                color = VirtualTextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                modifier = Modifier
                    .size(260.dp)
                    .border(2.dp, VirtualCyanGlow, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        CircularProgressIndicator(color = VirtualCyanGlow)
                    } else if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "Pairing QR Code",
                            modifier = Modifier.fillMaxSize().padding(16.dp)
                        )
                    } else {
                        Text(
                            text = errorMessage ?: "QR Unavailable",
                            color = VirtualOfflineRed,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Or enter Pairing Code:",
                fontSize = 14.sp,
                color = VirtualTextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = VirtualGlassBg,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, VirtualGoldAccent)
            ) {
                Text(
                    text = pairingCode,
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = VirtualGoldAccent,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 26.dp, vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = VirtualCyanGlow,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Waiting for viewer...",
                    fontSize = 14.sp,
                    color = VirtualTextWhiteHigh.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = {
                    onNavigate(Screen.CameraDashboard.route)
                }
            ) {
                Text(
                    text = "Skip to Dashboard",
                    color = VirtualCyanGlow,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
