package com.virtual.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

@Composable
fun CameraSetupScreen(
    onNavigate: (String) -> Unit
) {
    var isFrontCamera by remember { mutableStateOf(false) }
    var selectedQuality by remember { mutableStateOf("720p") }
    val qualities = listOf("360p", "480p", "720p", "1080p")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VirtualWhite)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Setup",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = VirtualTextPrimary
                )
                Text(
                    text = "Configure initial camera and streaming settings",
                    fontSize = 14.sp,
                    color = VirtualTextSecondary
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Lens Facing Option
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = VirtualBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VirtualBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Camera Lens",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VirtualTextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { isFrontCamera = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isFrontCamera) VirtualBluePrimary else VirtualBackground,
                                    contentColor = if (!isFrontCamera) VirtualWhite else VirtualTextPrimary
                                )
                            ) {
                                Text("Rear Camera")
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = { isFrontCamera = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFrontCamera) VirtualBluePrimary else VirtualBackground,
                                    contentColor = if (isFrontCamera) VirtualWhite else VirtualTextPrimary
                                )
                            ) {
                                Text("Front Camera")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Quality Selector
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = VirtualBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VirtualBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Default Video Quality",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VirtualTextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            qualities.forEach { q ->
                                val isSelected = selectedQuality == q
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                        .clickable { selectedQuality = q },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) VirtualBluePrimary else VirtualWhite,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) VirtualBluePrimary else VirtualBorder
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = q,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) VirtualWhite else VirtualTextPrimary,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom CTA
            Button(
                onClick = {
                    onNavigate(Screen.PairingQr.route)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VirtualBluePrimary)
            ) {
                Text(
                    text = "Generate Pairing QR",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VirtualWhite
                )
            }
        }
    }
}
