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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virtual.app.navigation.Screen
import com.virtual.app.ui.theme.*
import com.virtual.auth.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(
    authRepository: AuthRepository,
    onNavigate: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var showAuthDialog by remember { mutableStateOf(false) }
    var isRegisterMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("demo@virtualcam.io") }
    var password by remember { mutableStateOf("password123") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VirtualWhite)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = VirtualBluePrimary,
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("V", color = VirtualWhite, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "VIRTUAL",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = VirtualBluePrimary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Two-device live remote camera system",
                fontSize = 15.sp,
                color = VirtualTextSecondary
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    isRegisterMode = true
                    errorMessage = null
                    showAuthDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VirtualBluePrimary)
            ) {
                Text(
                    text = "Get Started",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VirtualWhite
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    isRegisterMode = false
                    errorMessage = null
                    showAuthDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, VirtualBluePrimary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VirtualBluePrimary)
            ) {
                Text(
                    text = "Login",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showAuthDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showAuthDialog = false },
            title = {
                Text(
                    text = if (isRegisterMode) "Create Account" else "Welcome Back",
                    fontWeight = FontWeight.Bold,
                    color = VirtualTextPrimary
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = VirtualOfflineRed,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    if (isRegisterMode) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = if (isRegisterMode) {
                                authRepository.register(name.ifBlank { "User" }, email, password)
                            } else {
                                authRepository.login(email, password)
                            }
                            isLoading = false
                            if (result.isSuccess) {
                                showAuthDialog = false
                                onNavigate(Screen.DeviceRole.route)
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Authentication failed"
                            }
                        }
                    },
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = VirtualBluePrimary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = VirtualWhite, strokeWidth = 2.dp)
                    } else {
                        Text(if (isRegisterMode) "Sign Up" else "Log In")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAuthDialog = false },
                    enabled = !isLoading
                ) {
                    Text("Cancel", color = VirtualTextSecondary)
                }
            }
        )
    }
}
