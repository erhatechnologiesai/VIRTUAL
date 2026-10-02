package com.virtual.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virtual.app.navigation.Screen
import com.virtual.app.ui.theme.*
import com.virtual.auth.AuthRepository
import com.virtual.core.models.DeviceType
import kotlinx.coroutines.launch

@Composable
fun DeviceRoleScreen(
    authRepository: AuthRepository,
    onNavigate: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    fun selectRole(role: DeviceType) {
        isLoading = true
        coroutineScope.launch {
            val result = authRepository.registerDevice(role)
            isLoading = false
            if (result.isSuccess) {
                if (role == DeviceType.CAMERA) {
                    onNavigate(Screen.CameraPermission.route)
                } else {
                    onNavigate(Screen.ViewerAddCamera.route)
                }
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
                text = "VIRTUAL",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = VirtualBluePrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "How do you want to use this phone?",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = VirtualTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Choose the role for this device. Both phones run the same VIRTUAL application.",
                fontSize = 14.sp,
                color = VirtualTextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Role 1: Camera Device
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isLoading) { selectRole(DeviceType.CAMERA) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VirtualBlueSubtle),
                border = BorderStroke(1.5.dp, VirtualBlueLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = VirtualBluePrimary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("📷", fontSize = 24.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Camera Device",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = VirtualBluePrimary
                        )
                        Text(
                            text = "Continuously stream this phone's camera live over the internet.",
                            fontSize = 13.sp,
                            color = VirtualTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Role 2: Viewer Device
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isLoading) { selectRole(DeviceType.VIEWER) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VirtualWhite),
                border = BorderStroke(1.5.dp, VirtualBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = VirtualTextPrimary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("📱", fontSize = 24.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Viewer Device",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = VirtualTextPrimary
                        )
                        Text(
                            text = "Remotely view the live camera feed from your paired phone.",
                            fontSize = 13.sp,
                            color = VirtualTextSecondary
                        )
                    }
                }
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator(color = VirtualBluePrimary)
            }
        }
    }
}
