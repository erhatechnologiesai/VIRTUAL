package com.virtual.app.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun PairingQrScreen(
    pairingRepository: PairingRepository,
    storage: SecureStorageManager,
    onNavigate: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pairingCode by remember { mutableStateOf("...") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
            if (result.isSuccess) {
                val data = result.getOrNull()!!
                pairingCode = data.pairingCode
                qrBitmap = QrCodeGenerator.generateQrBitmap(data.qrData, 512)
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to generate pairing QR"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VirtualWhite)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Pair Your Viewer",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = VirtualTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Scan this QR code from Phone 2 (Viewer Device) to complete one-time pairing.",
                fontSize = 14.sp,
                color = VirtualTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                modifier = Modifier
                    .size(260.dp)
                    .border(2.dp, VirtualBlueLight, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = VirtualWhite,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        CircularProgressIndicator(color = VirtualBluePrimary)
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
                color = VirtualTextSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = VirtualBlueSubtle,
                border = androidx.compose.foundation.BorderStroke(1.dp, VirtualBlueLight)
            ) {
                Text(
                    text = pairingCode,
                    fontSize = 26.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = VirtualBluePrimary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = VirtualBluePrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Waiting for viewer...",
                    fontSize = 14.sp,
                    color = VirtualTextSecondary
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
                    color = VirtualBluePrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
