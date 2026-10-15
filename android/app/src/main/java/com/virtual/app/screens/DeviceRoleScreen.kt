package com.virtual.app.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virtual.app.navigation.Screen
import com.virtual.app.ui.theme.*
import com.virtual.auth.AuthRepository
import com.virtual.core.models.DeviceType
import com.virtual.core.storage.SecureStorageManager
import kotlinx.coroutines.launch

@Composable
fun DeviceRoleScreen(
    authRepository: AuthRepository,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val storage = remember { SecureStorageManager.getInstance(context) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf<DeviceType?>(null) }
    var showServerDialog by remember { mutableStateOf(false) }
    var serverUrlInput by remember { mutableStateOf(storage.customBaseUrl ?: com.virtual.core.network.ApiClient.baseUrl) }
    val scrollState = rememberScrollState()

    fun selectRole(role: DeviceType) {
        if (isLoading) return
        isLoading = true
        selectedRole = role

        coroutineScope.launch {
            storage.deviceRole = role
            authRepository.registerDevice(role)
            isLoading = false

            if (role == DeviceType.CAMERA) {
                onNavigate(Screen.CameraPermission.route)
            } else {
                onNavigate(Screen.ViewerAddCamera.route)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumBackgroundGradient)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier.size(76.dp),
                shape = RoundedCornerShape(22.dp),
                color = Color.Transparent,
                border = BorderStroke(2.dp, PremiumGoldGradient),
                shadowElevation = 12.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PremiumButtonGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text("V", color = VirtualWhite, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "VIRTUAL",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = VirtualWhite,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Select Device Role",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VirtualGoldAccent,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Choose how you want to use this phone. Connect two devices seamlessly.",
                fontSize = 14.sp,
                color = VirtualTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Dedicated Server Configuration Pill Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = VirtualGlassBg,
                border = BorderStroke(1.dp, VirtualCyanGlow.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        serverUrlInput = storage.customBaseUrl ?: com.virtual.core.network.ApiClient.baseUrl
                        showServerDialog = true
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚙️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Backend Server (Wi-Fi / 4G)", color = VirtualWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = (storage.customBaseUrl ?: com.virtual.core.network.ApiClient.baseUrl).removeSuffix("/"),
                                color = VirtualCyanGlow,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = VirtualGoldAccent.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, VirtualGoldAccent)
                    ) {
                        Text(
                            text = "Change",
                            color = VirtualGoldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Role 1: Camera Device Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isLoading) { selectRole(DeviceType.CAMERA) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedRole == DeviceType.CAMERA) VirtualCardDarkSurface else Color.White.copy(alpha = 0.08f)
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (selectedRole == DeviceType.CAMERA) VirtualGoldGlow else VirtualBorderGlow.copy(alpha = 0.3f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.5.dp, VirtualCyanGlow)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(PremiumButtonGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📷", fontSize = 26.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Camera Device",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = VirtualWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Continuously stream live remote camera video over internet.",
                            fontSize = 13.sp,
                            color = VirtualTextMuted,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Role 2: Viewer Device Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isLoading) { selectRole(DeviceType.VIEWER) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedRole == DeviceType.VIEWER) VirtualCardDarkSurface else Color.White.copy(alpha = 0.08f)
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (selectedRole == DeviceType.VIEWER) VirtualCyanGlow else VirtualBorderGlow.copy(alpha = 0.3f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.5.dp, VirtualGoldAccent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(PremiumGoldGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📱", fontSize = 26.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Viewer Device",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = VirtualWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Remotely view live video feed from your paired Camera phone.",
                            fontSize = 13.sp,
                            color = VirtualTextMuted,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = VirtualGoldAccent,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Setting up device role...",
                        color = VirtualGoldAccent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Top Bar Server Settings Button with Gold Glow Accent (Rendered AFTER Column so it is on top)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, VirtualGoldAccent),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .zIndex(50f)
                .clickable {
                    serverUrlInput = storage.customBaseUrl ?: com.virtual.core.network.ApiClient.baseUrl
                    showServerDialog = true
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚙️", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Server", color = VirtualWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text("⚙️ Backend Server Settings", fontWeight = FontWeight.Bold, color = VirtualTextPrimary) },
            text = {
                Column {
                    Text(
                        "Set backend server URL. For same Wi-Fi, tap 'Use Local Wi-Fi'.",
                        fontSize = 13.sp,
                        color = VirtualTextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = serverUrlInput,
                        onValueChange = { serverUrlInput = it },
                        label = { Text("Server URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            serverUrlInput = "http://192.168.18.20:4000/"
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Use Local Wi-Fi (192.168.18.20)", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (serverUrlInput.isNotBlank()) {
                            com.virtual.core.network.ApiClient.updateBaseUrl(serverUrlInput, context)
                            Toast.makeText(context, "Server URL updated!", Toast.LENGTH_SHORT).show()
                        }
                        showServerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VirtualBluePrimary)
                ) {
                    Text("Save", color = VirtualWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) {
                    Text("Cancel", color = VirtualTextSecondary)
                }
            }
        )
    }
}

