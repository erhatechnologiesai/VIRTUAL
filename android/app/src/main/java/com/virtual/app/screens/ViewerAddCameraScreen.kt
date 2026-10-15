package com.virtual.app.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virtual.app.navigation.Screen
import com.virtual.app.ui.theme.*
import com.virtual.pairing.PairingRepository
import kotlinx.coroutines.launch

@Composable
fun ViewerAddCameraScreen(
    pairingRepository: PairingRepository,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pairingCodeInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showScanDialog by remember { mutableStateOf(false) }
    var qrPayloadInput by remember { mutableStateOf("") }

    fun submitPairing(code: String?, token: String? = null) {
        if (code.isNullOrBlank() && token.isNullOrBlank()) return
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val result = pairingRepository.joinPairingWithTokenOrCode(pairingToken = token, pairingCode = code?.trim())
            isLoading = false
            Toast.makeText(context, "Pairing Successful!", Toast.LENGTH_SHORT).show()
            onNavigate(Screen.PairingSuccess.route)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumBackgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(76.dp),
                shape = RoundedCornerShape(20.dp),
                color = VirtualGlassBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, VirtualGlassBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("📷", fontSize = 36.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Add Camera",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = VirtualTextWhiteHigh
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Connect this Viewer to your Camera Device. Pairing is saved permanently.",
                fontSize = 14.sp,
                color = VirtualTextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Scan QR Button
            OutlinedButton(
                onClick = {
                    showScanDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, VirtualCyanGlow),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VirtualCyanGlow)
            ) {
                Text("📷 Scan QR Code", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = VirtualGlassBorder)
                Text(
                    text = "  or enter 6-digit code  ",
                    fontSize = 13.sp,
                    color = VirtualTextMuted
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = VirtualGlassBorder)
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = VirtualOfflineRed,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            OutlinedTextField(
                value = pairingCodeInput,
                onValueChange = {
                    if (it.length <= 10) pairingCodeInput = it.uppercase()
                },
                label = { Text("Pairing Code (e.g. ABC-123)", color = VirtualTextMuted) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = VirtualTextWhiteHigh,
                    unfocusedTextColor = VirtualTextWhiteHigh,
                    focusedBorderColor = VirtualCyanGlow,
                    unfocusedBorderColor = VirtualGlassBorder,
                    focusedLabelColor = VirtualCyanGlow
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = VirtualTextWhiteHigh
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { submitPairing(pairingCodeInput) },
                enabled = !isLoading && pairingCodeInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(
                        if (!isLoading && pairingCodeInput.isNotBlank()) PremiumButtonGradient else Brush.horizontalGradient(
                            listOf(VirtualGlassBg, VirtualGlassBg)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = VirtualWhite, strokeWidth = 2.dp)
                } else {
                    Text("Pair Device", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = VirtualWhite)
                }
            }
        }
    }

    if (showScanDialog) {
        AlertDialog(
            onDismissRequest = { showScanDialog = false },
            containerColor = VirtualGlassBg,
            title = {
                Text("Scan / Input QR Payload", fontWeight = FontWeight.Bold, color = VirtualTextWhiteHigh)
            },
            text = {
                Column {
                    Text(
                        "Paste or enter the QR Code payload text from Phone 1:",
                        fontSize = 14.sp,
                        color = VirtualTextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = qrPayloadInput,
                        onValueChange = { qrPayloadInput = it },
                        label = { Text("QR Code Payload / Code", color = VirtualTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = VirtualTextWhiteHigh,
                            unfocusedTextColor = VirtualTextWhiteHigh,
                            focusedBorderColor = VirtualCyanGlow,
                            unfocusedBorderColor = VirtualGlassBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showScanDialog = false
                        coroutineScope.launch {
                            val (token, code) = pairingRepository.parseQrPayload(qrPayloadInput)
                            submitPairing(code = code, token = token)
                        }
                    },
                    enabled = qrPayloadInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = VirtualCyanGlow)
                ) {
                    Text("Pair Now", color = VirtualWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showScanDialog = false }) {
                    Text("Cancel", color = VirtualTextMuted)
                }
            }
        )
    }
}
