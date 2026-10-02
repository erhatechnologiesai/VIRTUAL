package com.virtual.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val coroutineScope = rememberCoroutineScope()
    var pairingCodeInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun submitPairing(code: String) {
        if (code.isBlank()) return
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val result = pairingRepository.joinPairingWithTokenOrCode(pairingCode = code.trim())
            isLoading = false
            if (result.isSuccess) {
                onNavigate(Screen.PairingSuccess.route)
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Pairing failed. Please check the code."
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
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(16.dp),
                color = VirtualBlueSubtle
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("📷", fontSize = 32.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Add Camera",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = VirtualTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Connect this Viewer to your Camera Device. Pairing is saved permanently.",
                fontSize = 14.sp,
                color = VirtualTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Scan QR Button
            OutlinedButton(
                onClick = {
                    // Pre-fill or scan
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, VirtualBluePrimary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VirtualBluePrimary)
            ) {
                Text("Scan QR Code", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = VirtualBorder)
                Text(
                    text = "  or enter code  ",
                    fontSize = 13.sp,
                    color = VirtualTextMuted
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = VirtualBorder)
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = VirtualOfflineRed,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            OutlinedTextField(
                value = pairingCodeInput,
                onValueChange = {
                    if (it.length <= 10) pairingCodeInput = it.uppercase()
                },
                label = { Text("Pairing Code (e.g. ABC-123)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { submitPairing(pairingCodeInput) },
                enabled = !isLoading && pairingCodeInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VirtualBluePrimary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = VirtualWhite, strokeWidth = 2.dp)
                } else {
                    Text("Pair Device", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = VirtualWhite)
                }
            }
        }
    }
}
