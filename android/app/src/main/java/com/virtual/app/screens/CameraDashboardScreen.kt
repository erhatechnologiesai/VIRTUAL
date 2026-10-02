package com.virtual.app.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virtual.app.navigation.Screen
import com.virtual.app.ui.theme.*
import com.virtual.camera.service.CameraForegroundService
import com.virtual.core.storage.SecureStorageManager
import com.virtual.core.telemetry.DeviceStatusMonitor

@Composable
fun CameraDashboardScreen(
    storage: SecureStorageManager,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val telemetry = remember { DeviceStatusMonitor(context) }

    var isStreaming by remember { mutableStateOf(false) }
    var isConflictBusy by remember { mutableStateOf(false) }
    var batteryLevel by remember { mutableStateOf(telemetry.getBatteryLevel()) }
    var networkType by remember { mutableStateOf(telemetry.getNetworkType()) }
    val quality by remember { mutableStateOf("720p") }

    // Register receiver to get live stream status updates from CameraForegroundService
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == CameraForegroundService.BROADCAST_STREAM_STATUS) {
                    isStreaming = intent.getBooleanExtra(CameraForegroundService.EXTRA_IS_STREAMING, false)
                    isConflictBusy = intent.getBooleanExtra(CameraForegroundService.EXTRA_IS_BUSY, false)
                }
            }
        }
        val filter = IntentFilter(CameraForegroundService.BROADCAST_STREAM_STATUS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {}
        }
    }

    fun toggleStreaming() {
        val intent = Intent(context, CameraForegroundService::class.java).apply {
            action = if (isStreaming) {
                CameraForegroundService.ACTION_STOP_STREAM
            } else {
                CameraForegroundService.ACTION_START_STREAM
            }
            putExtra(CameraForegroundService.EXTRA_RESOLUTION, quality)
            putExtra(CameraForegroundService.EXTRA_FPS, 30)
            putExtra(CameraForegroundService.EXTRA_FRONT_FACING, false)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isStreaming) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        isStreaming = !isStreaming
        if (!isStreaming) {
            isConflictBusy = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VirtualWhite)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "VIRTUAL",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = VirtualBluePrimary
                        )
                        Text(
                            text = "Camera Device: ${storage.deviceUuid ?: "VRT-CAM"}",
                            fontSize = 13.sp,
                            color = VirtualTextSecondary
                        )
                    }

                    Row {
                        IconButton(onClick = { onNavigate(Screen.PairingQr.route) }) {
                            Text("📱", fontSize = 20.sp)
                        }
                        IconButton(onClick = { onNavigate(Screen.Settings.route) }) {
                            Text("⚙", fontSize = 20.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Camera Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = VirtualBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VirtualBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Camera Status",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = VirtualTextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(12.dp),
                                shape = CircleShape,
                                color = when {
                                    isConflictBusy -> VirtualWarningAmber
                                    isStreaming -> VirtualOnlineGreen
                                    else -> VirtualOfflineRed
                                }
                            ) {}

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = when {
                                    isConflictBusy -> "Camera In Use by Another App"
                                    isStreaming -> "Streaming Live"
                                    else -> "Idle (Ready to Stream)"
                                },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isConflictBusy -> VirtualWarningAmber
                                    isStreaming -> VirtualOnlineGreen
                                    else -> VirtualTextPrimary
                                }
                            )
                        }

                        if (isConflictBusy) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = VirtualWarningAmber.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "Another application is currently using the camera.\nVIRTUAL will resume automatically when released.",
                                    fontSize = 12.sp,
                                    color = VirtualTextPrimary,
                                    modifier = Modifier.padding(10.dp),
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = VirtualBorder)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Device Telemetry
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Battery", fontSize = 12.sp, color = VirtualTextMuted)
                                Text("$batteryLevel%", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = VirtualTextPrimary)
                            }
                            Column {
                                Text("Network", fontSize = 12.sp, color = VirtualTextMuted)
                                Text(networkType, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = VirtualTextPrimary)
                            }
                            Column {
                                Text("Quality", fontSize = 12.sp, color = VirtualTextMuted)
                                Text(quality, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = VirtualTextPrimary)
                            }
                            Column {
                                Text("FPS", fontSize = 12.sp, color = VirtualTextMuted)
                                Text("30", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = VirtualTextPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Screen Off & Multitasking Guidance Note
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = VirtualBlueSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💡", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Streaming runs via Android Foreground Service. You can lock your screen or switch to text apps like WhatsApp; the camera feed remains active.",
                            fontSize = 12.sp,
                            color = VirtualBluePrimary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Bottom Streaming Action Button
            Button(
                onClick = { toggleStreaming() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isStreaming) VirtualOfflineRed else VirtualBluePrimary
                )
            ) {
                Text(
                    text = if (isStreaming) "Stop Streaming" else "Start Streaming",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VirtualWhite
                )
            }
        }
    }
}
