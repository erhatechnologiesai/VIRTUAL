package com.virtual.app.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            .background(PremiumBackgroundGradient)
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
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .background(VirtualGlassBg, CircleShape)
                        .border(1.dp, VirtualGlassBorder, CircleShape)
                ) {
                    Text("←", fontSize = 22.sp, color = VirtualTextWhiteHigh)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Settings",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = VirtualTextWhiteHigh
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Device Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VirtualGlassBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, VirtualGlassBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("DEVICE IDENTITY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VirtualGoldAccent, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = storage.deviceUuid ?: "VRT-UNKNOWN",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = VirtualCyanGlow
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Role: ${storage.deviceRole?.name ?: "UNASSIGNED"}  •  App v1.0.0",
                        fontSize = 13.sp,
                        color = VirtualTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Video Quality
            Text("Streaming Quality", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VirtualTextWhiteHigh)
            Spacer(modifier = Modifier.height(12.dp))

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
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) VirtualCyanGlow else VirtualGlassBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) VirtualCyanGlow else VirtualGlassBorder)
                    ) {
                        Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = q,
                                color = if (isSelected) VirtualWhite else VirtualTextWhiteHigh,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Toggles
            Text("Preferences", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VirtualTextWhiteHigh)
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VirtualGlassBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, VirtualGlassBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Audio Streaming", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VirtualTextWhiteHigh)
                            Text("Transmit microphone audio with video", fontSize = 12.sp, color = VirtualTextMuted)
                        }
                        Switch(
                            checked = audioEnabled,
                            onCheckedChange = {
                                audioEnabled = it
                                saveSettings()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = VirtualWhite, checkedTrackColor = VirtualCyanGlow)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = VirtualGlassBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto Reconnect", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VirtualTextWhiteHigh)
                            Text("Automatically restore WebRTC on network drops", fontSize = 12.sp, color = VirtualTextMuted)
                        }
                        Switch(
                            checked = autoReconnect,
                            onCheckedChange = {
                                autoReconnect = it
                                saveSettings()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = VirtualWhite, checkedTrackColor = VirtualCyanGlow)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Battery Optimization Info & Setting
            Text("Background Reliability", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VirtualTextWhiteHigh)
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VirtualGlassBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, VirtualGlassBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
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
                        color = VirtualTextMuted,
                        lineHeight = 18.sp
                    )
                    if (!isBatteryOptIgnored) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                val intent = settingsRepository.requestIgnoreBatteryOptimizationsIntent()
                                intent?.let { context.startActivity(it) }
                            },
                            modifier = Modifier.background(PremiumButtonGradient, shape = RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Text("Disable Optimization", color = VirtualWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Backend Server & Tunnel Settings
            Text("Backend Server & Signaling", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VirtualTextWhiteHigh)
            Spacer(modifier = Modifier.height(12.dp))

            var serverUrlText by remember { mutableStateOf(storage.customBaseUrl ?: com.virtual.core.network.ApiClient.baseUrl) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VirtualGlassBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, VirtualGlassBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Server Endpoint",
                        fontWeight = FontWeight.Bold,
                        color = VirtualTextWhiteHigh,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Enter your Cloudflare tunnel HTTPS URL (for 4G/remote) or local Wi-Fi IP (for same router).",
                        fontSize = 12.sp,
                        color = VirtualTextMuted,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = serverUrlText,
                        onValueChange = { serverUrlText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VirtualCyanGlow,
                            unfocusedBorderColor = VirtualGlassBorder,
                            focusedTextColor = VirtualTextWhiteHigh,
                            unfocusedTextColor = VirtualTextWhiteHigh
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (serverUrlText.isNotBlank()) {
                                    com.virtual.core.network.ApiClient.updateBaseUrl(serverUrlText, context)
                                    android.widget.Toast.makeText(context, "Server URL updated!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f).background(PremiumButtonGradient, shape = RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Text("Save URL", color = VirtualWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                serverUrlText = "http://192.168.18.20:4000/"
                                com.virtual.core.network.ApiClient.updateBaseUrl(serverUrlText, context)
                                android.widget.Toast.makeText(context, "Reset to Local Wi-Fi IP!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VirtualGlassBorder)
                        ) {
                            Text("Local Wi-Fi", color = VirtualCyanGlow, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Security Navigation
            OutlinedButton(
                onClick = onNavigateSecurity,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, VirtualCyanGlow),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VirtualCyanGlow)
            ) {
                Text("Manage Security & Paired Devices", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
