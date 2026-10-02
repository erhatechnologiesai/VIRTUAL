package com.virtual.app.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.virtual.app.navigation.Screen
import com.virtual.app.ui.theme.*

@Composable
fun CameraPermissionScreen(
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    var isGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isDeniedPermanently by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isGranted = granted
        if (granted) {
            onNavigate(Screen.CameraSetup.route)
        } else {
            isDeniedPermanently = true
        }
    }

    LaunchedEffect(isGranted) {
        if (isGranted) {
            onNavigate(Screen.CameraSetup.route)
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
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(16.dp),
                color = VirtualBlueSubtle
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🛡️", fontSize = 32.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Camera Permission",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = VirtualTextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "VIRTUAL requires Camera access to stream live video directly to your authorized viewer device.\n\nVideo travels directly via WebRTC peer-to-peer or secure TURN relay and is never stored on servers.",
                fontSize = 15.sp,
                color = VirtualTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            if (!isDeniedPermanently) {
                Button(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VirtualBluePrimary)
                ) {
                    Text(
                        text = "Allow Camera Access",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VirtualWhite
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Camera permission was denied. Please enable Camera in Android Settings to continue.",
                        fontSize = 14.sp,
                        color = VirtualOfflineRed,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VirtualBluePrimary)
                    ) {
                        Text("Open Android Settings", color = VirtualWhite, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
