package com.virtual.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.virtual.core.models.PairingInfo
import com.virtual.core.storage.SecureStorageManager
import com.virtual.pairing.PairingRepository
import kotlinx.coroutines.launch

@Composable
fun SecurityScreen(
    storage: SecureStorageManager,
    pairingRepository: PairingRepository,
    authRepository: AuthRepository,
    onNavigateBack: () -> Unit,
    onNavigateWelcome: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var pairings by remember { mutableStateOf<List<PairingInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshPairings() {
        isLoading = true
        coroutineScope.launch {
            val result = pairingRepository.getActivePairings()
            isLoading = false
            if (result.isSuccess) {
                pairings = result.getOrNull() ?: emptyList()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshPairings()
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 22.sp, color = VirtualTextPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Security & Devices",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = VirtualTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Paired Devices",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VirtualTextPrimary
                )
                Text(
                    text = "Devices with authorized permanent streaming access",
                    fontSize = 13.sp,
                    color = VirtualTextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    CircularProgressIndicator(color = VirtualBluePrimary, modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (pairings.isEmpty() && storage.pairedCameraId == null) {
                    Text(
                        text = "No actively paired devices.",
                        fontSize = 14.sp,
                        color = VirtualTextMuted,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        if (pairings.isNotEmpty()) {
                            items(pairings) { p ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = VirtualBackground),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, VirtualBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = p.cameraDevice?.deviceName ?: "Paired Device",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = VirtualTextPrimary
                                            )
                                            Text(
                                                text = "Paired: ${p.pairedAt?.take(10) ?: "Active"}",
                                                fontSize = 12.sp,
                                                color = VirtualTextSecondary
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    pairingRepository.revokePairing(p.id)
                                                    refreshPairings()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = VirtualOfflineRed),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Revoke", fontSize = 12.sp, color = VirtualWhite)
                                        }
                                    }
                                }
                            }
                        } else if (storage.pairedCameraId != null) {
                            // Fallback saved pairing
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = VirtualBackground),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, VirtualBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = storage.pairedCameraName ?: "Paired Camera",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = VirtualTextPrimary
                                            )
                                            Text(
                                                text = "Permanent pairing active",
                                                fontSize = 12.sp,
                                                color = VirtualOnlineGreen
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                storage.clearPairing()
                                                refreshPairings()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = VirtualOfflineRed),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Remove", fontSize = 12.sp, color = VirtualWhite)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Logout Action
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        authRepository.logout()
                        onNavigateWelcome()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, VirtualOfflineRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VirtualOfflineRed)
                ) {
                    Text("Logout & Clear Device", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
