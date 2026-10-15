package com.virtual.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.virtual.app.screens.*
import com.virtual.auth.AuthRepository
import com.virtual.core.storage.SecureStorageManager
import com.virtual.pairing.PairingRepository
import com.virtual.settings.SettingsRepository
import com.virtual.viewer.ViewerRepository

@Composable
fun VirtualNavGraph(
    navController: NavHostController,
    storage: SecureStorageManager,
    authRepository: AuthRepository,
    pairingRepository: PairingRepository,
    viewerRepository: ViewerRepository,
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.DeviceRole.route,
        modifier = modifier
    ) {
        // Screen 1: Splash
        composable(Screen.Splash.route) {
            SplashScreen(
                storage = storage,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Screen 2: Welcome (Redirected directly to DeviceRoleScreen)
        composable(Screen.Welcome.route) {
            DeviceRoleScreen(
                authRepository = authRepository,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        // Screen 3: Device Role
        composable(Screen.DeviceRole.route) {
            DeviceRoleScreen(
                authRepository = authRepository,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        // Screen 4: Camera Permission
        composable(Screen.CameraPermission.route) {
            CameraPermissionScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.CameraPermission.route) { inclusive = true }
                    }
                }
            )
        }

        // Screen 5: Camera Setup
        composable(Screen.CameraSetup.route) {
            CameraSetupScreen(
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        // Screen 6: Pairing QR
        composable(Screen.PairingQr.route) {
            PairingQrScreen(
                pairingRepository = pairingRepository,
                storage = storage,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.PairingQr.route) { inclusive = true }
                    }
                }
            )
        }

        // Screen 7: Viewer Add Camera
        composable(Screen.ViewerAddCamera.route) {
            ViewerAddCameraScreen(
                pairingRepository = pairingRepository,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.ViewerAddCamera.route) { inclusive = true }
                    }
                }
            )
        }

        // Screen 8: Pairing Success
        composable(Screen.PairingSuccess.route) {
            PairingSuccessScreen(
                storage = storage,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.PairingSuccess.route) { inclusive = true }
                    }
                }
            )
        }

        // Screen 9: Camera Dashboard
        composable(Screen.CameraDashboard.route) {
            CameraDashboardScreen(
                storage = storage,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        // Screen 10: Viewer Dashboard
        composable(Screen.ViewerDashboard.route) {
            ViewerDashboardScreen(
                viewerRepository = viewerRepository,
                storage = storage,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        // Screen 11: Live View
        composable(
            route = Screen.LiveView.route,
            arguments = listOf(
                navArgument("cameraId") { type = NavType.StringType },
                navArgument("cameraName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val cameraId = backStackEntry.arguments?.getString("cameraId") ?: ""
            val cameraName = backStackEntry.arguments?.getString("cameraName") ?: "Camera"
            LiveViewScreen(
                cameraId = cameraId,
                cameraName = cameraName,
                onNavigateBack = { navController.popBackStack() },
                onNavigateSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        // Screen 12: Settings
        composable(Screen.Settings.route) {
            SettingsScreen(
                storage = storage,
                settingsRepository = settingsRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateSecurity = { navController.navigate(Screen.Security.route) }
            )
        }

        // Screen 13: Security
        composable(Screen.Security.route) {
            SecurityScreen(
                storage = storage,
                pairingRepository = pairingRepository,
                authRepository = authRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateWelcome = {
                    navController.navigate(Screen.DeviceRole.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
