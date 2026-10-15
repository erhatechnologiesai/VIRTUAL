package com.virtual.app.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virtual.app.navigation.Screen
import com.virtual.app.ui.theme.*
import com.virtual.auth.AuthRepository
import com.virtual.core.network.ApiClient
import com.virtual.core.storage.SecureStorageManager
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(
    authRepository: AuthRepository,
    onNavigate: (String) -> Unit
) {
    DeviceRoleScreen(
        authRepository = authRepository,
        onNavigate = onNavigate
    )
}
