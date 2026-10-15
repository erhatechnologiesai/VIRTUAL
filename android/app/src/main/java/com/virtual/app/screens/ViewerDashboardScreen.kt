package com.virtual.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virtual.app.navigation.Screen
import com.virtual.app.ui.theme.*
import com.virtual.core.models.Device
import com.virtual.core.models.DeviceStatus
import com.virtual.core.storage.SecureStorageManager
import com.virtual.viewer.ViewerRepository
import kotlinx.coroutines.launch

@Composable
fun ViewerDashboardScreen(
    viewerRepository: ViewerRepository,
    storage: SecureStorageManager,
    onNavigate: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var cameras by remember { mutableStateOf<List<Device>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshCameras() {
        isLoading = true
        coroutineScope.launch {
            val result = viewerRepository.getPairedCameras()
            isLoading = false
            if (result.isSuccess) {
                cameras = result.getOrNull() ?: emptyList()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshCameras()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumBackgroundGradient)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VIRTUAL",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = VirtualCyanGlow
                    )
                    Text(
                        text = "My Cameras",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = VirtualTextWhiteHigh
                    )
                }

                Row {
                    IconButton(
                        onClick = { onNavigate(Screen.ViewerAddCamera.route) },
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .background(VirtualGlassBg, CircleShape)
                            .border(1.dp, VirtualGlassBorder, CircleShape)
                    ) {
                        Text("➕", fontSize = 18.sp)
                    }
                    IconButton(
                        onClick = { onNavigate(Screen.Settings.route) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(VirtualGlassBg, CircleShape)
                            .border(1.dp, VirtualGlassBorder, CircleShape)
                    ) {
                        Text("⚙️", fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = VirtualCyanGlow)
                }
            } else if (cameras.isEmpty() && storage.pairedCameraId == null) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📷", fontSize = 54.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No cameras paired yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = VirtualTextWhiteHigh
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pair a Phone 1 camera device once to begin streaming.",
                            fontSize = 14.sp,
                            color = VirtualTextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        Button(
                            onClick = { onNavigate(Screen.ViewerAddCamera.route) },
                            modifier = Modifier
                                .height(50.dp)
                                .background(PremiumButtonGradient, shape = RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Text("Add Camera", color = VirtualWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            } else {
                val displayList = if (cameras.isNotEmpty()) {
                    cameras
                } else {
                    // Fallback to permanently saved pairing in local storage
                    listOf(
                        Device(
                            id = storage.pairedCameraId ?: "local_cam_1",
                            userId = storage.userId ?: "",
                            deviceUuid = "VRT-CAM-SAVED",
                            deviceName = storage.pairedCameraName ?: "Home Camera",
                            deviceType = com.virtual.core.models.DeviceType.CAMERA,
                            status = DeviceStatus.ONLINE,
                            lastSeen = "Just now"
                        )
                    )
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(displayList) { camera ->
                        val isOnline = camera.status == DeviceStatus.ONLINE || camera.status == DeviceStatus.STREAMING
                        val resolution = camera.cameraDetail?.resolution ?: "720p"

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = VirtualGlassBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VirtualGlassBorder)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📷", fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = camera.deviceName,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = VirtualTextWhiteHigh
                                            )
                                            Text(
                                                text = camera.deviceUuid,
                                                fontSize = 12.sp,
                                                color = VirtualTextMuted
                                            )
                                        }
                                    }

                                    // Online / Offline Status Badge
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isOnline) VirtualOnlineGreen.copy(alpha = 0.18f) else VirtualOfflineRed.copy(alpha = 0.18f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isOnline) VirtualOnlineGreen.copy(alpha = 0.4f) else VirtualOfflineRed.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                modifier = Modifier.size(8.dp),
                                                shape = CircleShape,
                                                color = if (isOnline) VirtualOnlineGreen else VirtualOfflineRed
                                            ) {}
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isOnline) "Online" else "Offline",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isOnline) VirtualOnlineGreen else VirtualOfflineRed
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isOnline) "Quality: $resolution" else "Last seen: recently",
                                        fontSize = 13.sp,
                                        color = VirtualTextMuted
                                    )

                                    Button(
                                        onClick = {
                                            onNavigate(Screen.LiveView.createRoute(camera.id, camera.deviceName))
                                        },
                                        modifier = Modifier.background(PremiumButtonGradient, shape = RoundedCornerShape(10.dp)),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                                    ) {
                                        Text(
                                            text = "VIEW LIVE",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = VirtualWhite
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
