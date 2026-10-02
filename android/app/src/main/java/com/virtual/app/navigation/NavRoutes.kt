package com.virtual.app.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object DeviceRole : Screen("device_role")
    object CameraPermission : Screen("camera_permission")
    object CameraSetup : Screen("camera_setup")
    object PairingQr : Screen("pairing_qr")
    object ViewerAddCamera : Screen("viewer_add_camera")
    object PairingSuccess : Screen("pairing_success")
    object CameraDashboard : Screen("camera_dashboard")
    object ViewerDashboard : Screen("viewer_dashboard")
    object LiveView : Screen("live_view/{cameraId}/{cameraName}") {
        fun createRoute(cameraId: String, cameraName: String) = "live_view/$cameraId/$cameraName"
    }
    object Settings : Screen("settings")
    object Security : Screen("security")
}
