package com.virtual.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.virtual.app.navigation.VirtualNavGraph
import com.virtual.app.ui.theme.VirtualTheme
import com.virtual.app.ui.theme.VirtualWhite
import com.virtual.auth.AuthRepository
import com.virtual.core.storage.SecureStorageManager
import com.virtual.pairing.PairingRepository
import com.virtual.settings.SettingsRepository
import com.virtual.viewer.ViewerRepository

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val storage = SecureStorageManager.getInstance(applicationContext)
        val authRepository = AuthRepository(applicationContext)
        val pairingRepository = PairingRepository(applicationContext)
        val viewerRepository = ViewerRepository(applicationContext)
        val settingsRepository = SettingsRepository(applicationContext)

        setContent {
            VirtualTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VirtualWhite
                ) {
                    val navController = rememberNavController()
                    VirtualNavGraph(
                        navController = navController,
                        storage = storage,
                        authRepository = authRepository,
                        pairingRepository = pairingRepository,
                        viewerRepository = viewerRepository,
                        settingsRepository = settingsRepository
                    )
                }
            }
        }
    }
}
