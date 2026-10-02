package com.virtual.app.screens

import androidx.compose.foundation.background
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
import com.virtual.core.storage.SecureStorageManager

@Composable
fun PairingSuccessScreen(
    storage: SecureStorageManager,
    onNavigate: (String) -> Unit
) {
    val cameraName = storage.pairedCameraName ?: "Home Camera"

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
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = VirtualOnlineGreen.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "✓",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = VirtualOnlineGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Camera Connected",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = VirtualTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = cameraName,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = VirtualBluePrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Authorized permanent pairing established.\nYou can view this camera anytime without scanning QR again.",
                fontSize = 14.sp,
                color = VirtualTextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    onNavigate(Screen.ViewerDashboard.route)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VirtualBluePrimary)
            ) {
                Text(
                    text = "Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VirtualWhite
                )
            }
        }
    }
}
