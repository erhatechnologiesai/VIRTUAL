package com.virtual.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.virtual.core.models.DeviceType
import com.virtual.core.storage.SecureStorageManager
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    storage: SecureStorageManager,
    onNavigate: (String) -> Unit
) {
    LaunchedEffect(Unit) {
        delay(1200) // Brief branded splash
        if (storage.isLoggedIn()) {
            when (storage.deviceRole) {
                DeviceType.CAMERA -> onNavigate(Screen.CameraDashboard.route)
                DeviceType.VIEWER -> {
                    if (storage.isPaired) {
                        onNavigate(Screen.ViewerDashboard.route)
                    } else {
                        onNavigate(Screen.ViewerAddCamera.route)
                    }
                }
                null -> onNavigate(Screen.DeviceRole.route)
            }
        } else {
            onNavigate(Screen.Welcome.route)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VirtualWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(88.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = VirtualBluePrimary,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "V",
                        color = VirtualWhite,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "VIRTUAL",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = VirtualBluePrimary,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Secure Remote Live Camera",
                fontSize = 15.sp,
                color = VirtualTextSecondary
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = VirtualBlueVibrant,
                strokeWidth = 3.dp
            )
        }
    }
}
