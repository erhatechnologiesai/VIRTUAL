package com.virtual.app.screens

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virtual.app.navigation.Screen
import com.virtual.app.ui.theme.*
import com.virtual.core.storage.SecureStorageManager
import com.virtual.settings.SettingsRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    storage: SecureStorageManager,
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit,
    onNavigateSecurity: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var resolution by remember { mutableStateOf("720p") }
    var fps by remember { mutableStateOf(30) }
    var audioEnabled by remember { mutableStateOf(false) }
    var autoReconnect by remember { mutableStateOf(true) }
    var isBatteryOptIgnored by remember { mutableStateOf(settingsRepository.isIgnoringBatteryOptimizations()) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val result = settingsRepository.getSettings()
            if (result.isSuccess) {
                val data = result.getOrNull()!!
                resolution = data.videoQuality
                fps = data.fps
                audioEnabled = data.audioEnabled
                autoReconnect = data.autoReconnect
            }
        }
    }

    fun saveSettings() {
        coroutineScope.launch {
            settingsRepository.updateSettings(
                videoQuality = resolution,
                fps = fps,
                audioEnabled = audioEnabled,
                autoReconnect = autoReconnect
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VirtualWhite)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Text("←", fontSize = 22.sp, color = VirtualTextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = VirtualTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Device Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VirtualBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, VirtualBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Device Identity", fontSize = 13.sp, color = VirtualTextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = storage.deviceUuid ?: "VRT-UNKNOWN",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = VirtualBluePrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Role: ${storage.deviceRole?.name ?: "UNASSIGNED"}  •  App v1.0.0",
                        fontSize = 13.sp,
                        color = VirtualTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Video Quality
            Text("Streaming Quality", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = VirtualTextPrimary)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("360p", "480p", "720p", "1080p").forEach { q ->
                    val isSelected = resolution == q
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .clickable {
                                resolution = q
                                saveSettings()
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) VirtualBluePrimary else VirtualBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) VirtualBluePrimary else VirtualBorder)
                    ) {
                        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = q,
                                color = if (isSelected) VirtualWhite else VirtualTextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Toggles
            Text("Preferences", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = VirtualTextPrimary)
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VirtualBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, VirtualBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Audio Streaming", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = VirtualTextPrimary)
                            Text("Transmit microphone audio with video", fontSize = 12.sp, color = VirtualTextSecondary)
                        }
                        Switch(
                            checked = audioEnabled,
                            onCheckedChange = {
                                audioEnabled = it
                                saveSettings()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = VirtualWhite, checkedTrackColor = VirtualBluePrimary)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = VirtualBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto Reconnect", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = VirtualTextPrimary)
                            Text("Automatically restore WebRTC on network drops", fontSize = 12.sp, color = VirtualTextSecondary)
                        }
                        Switch(
                            checked = autoReconnect,
                            onCheckedChange = {
                                autoReconnect = it
                                saveSettings()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = VirtualWhite, checkedTrackColor = VirtualBluePrimary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Battery Optimization Info & Setting
            Text("Background Reliability", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = VirtualTextPrimary)
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VirtualBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, VirtualBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isBatteryOptIgnored) "✓ Battery Optimization Ignored" else "⚠️ Battery Optimization Active",
                        fontWeight = FontWeight.Bold,
                        color = if (isBatteryOptIgnored) VirtualOnlineGreen else VirtualWarningAmber,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "To allow the camera stream to continue when your screen is off, disable Android battery optimization for VIRTUAL.",
                        fontSize = 13.sp,
                        color = VirtualTextSecondary,
                        lineHeight = 18.sp
                    )
                    if (!isBatteryOptIgnored) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val intent = settingsRepository.requestIgnoreBatteryOptimizationsIntent()
                                intent?.let { context.startActivity(it) }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VirtualBluePrimary)
                        ) {
                            Text("Disable Optimization", color = VirtualWhite, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Security Navigation
            OutlinedButton(
                onClick = onNavigateSecurity,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, VirtualBluePrimary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VirtualBluePrimary)
            ) {
                Text("Manage Security & Paired Devices", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
