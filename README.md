# VIRTUAL — Two-Device Remote Live-Camera System

**VIRTUAL** is an Android live-camera streaming system connecting two phones over the internet.
Both phones install the **same VIRTUAL application**.

- **Phone 1 = Camera Device**: Captures hardware camera feed via CameraX, runs a foreground service with wake lock for screen-off streaming, and broadcasts low-latency video via WebRTC.
- **Phone 2 = Viewer Device**: Remotely views the live video stream from anywhere in the world (Wi-Fi, 4G, 5G) with audio controls, full-screen viewing, and telemetry monitoring.
- **One-Time Pairing**: Devices pair once via QR code or 6-character code (`ABC-123`). The pairing is stored permanently and remembered across app restarts and phone reboots.
- **Design System**: Professional **Blue + White** theme with clean Material 3 typography and components.

---

## Architecture Overview

```
                      +-----------------------------+
                      |       VIRTUAL Backend       |
                      |  • Node.js & TypeScript     |
                      |  • Socket.IO Signaling      |
                      |  • PostgreSQL (Prisma ORM)  |
                      |  • JWT & Device Security    |
                      +--------------+--------------+
                                     │
           ┌─────────────────────────┴─────────────────────────┐
           │                                                   │
  Signaling Socket (SDP / ICE)                       Signaling Socket (SDP / ICE)
           │                                                   │
           ▼                                                   ▼
+──────────────────────────+                       +──────────────────────────+
|  Phone 1 (Camera Device) |                       |  Phone 2 (Viewer Device) |
|  • Android CameraX       |                       |  • WebRTC SurfaceView    |
|  • Foreground Service    |                       |  • Low-Latency Receiver  |
|  • Conflict Detector     |                       |  • Telemetry Display     |
|  • Partial WakeLock      |                       |  • Permanent Pairing     |
+──────────┬───────────────+                       +───────────▲──────────────+
           │                                                   │
           │                     Internet                      │
           └═══════════════ WebRTC Direct P2P ═════════════════┘
           │         (or Coturn TURN Relay Fallback)           │
           └───────────────────────────────────────────────────┘
```

> **Database Rule**: The database is **metadata only**. No video frames ever touch the database or backend disk. Real-time media travels strictly over WebRTC SRTP.

---

## Monorepo Project Structure

```
VIRTUAL/
├── android/                   # Android Application (Kotlin, Jetpack Compose, CameraX, WebRTC)
│   ├── app/                   # Main Application module & UI Theme
│   │   ├── src/main/java/com/virtual/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── VirtualApplication.kt
│   │   │   ├── navigation/    # NavHost & Routes for all 13 screens
│   │   │   ├── screens/       # 13 Complete UI Screens in Compose
│   │   │   └── ui/theme/      # Blue + White Material 3 Design System
│   │   └── AndroidManifest.xml# Camera, Foreground Service, WakeLock permissions
│   ├── core/                  # Secure storage, API client, models & telemetry
│   ├── camera/                # CameraX, Foreground Service & Conflict Detector
│   ├── webrtc/                # WebRTC Manager, VideoCapturer bridge & Signaling Client
│   ├── pairing/               # ZXing QR Generator, Code Validator & Pairing Repo
│   ├── viewer/                # Viewer Controller & WebRTC Stream Consumer
│   ├── auth/                  # Authentication & Device Identity manager
│   ├── settings/              # Settings & Battery Optimization Handler
│   ├── build.gradle.kts       # Gradle build configuration
│   └── settings.gradle.kts    # Multi-module settings
│
├── backend/                   # Node.js + TypeScript Backend & Signaling Server
│   ├── src/
│   │   ├── auth/              # JWT, Refresh Tokens & Bcrypt hashing
│   │   ├── devices/           # Device registration (VRT-CAM-XXXXXXXX) & Telemetry
│   │   ├── pairing/           # Ephemeral QR token generation & permanent pairing
│   │   ├── sessions/          # WebRTC session tracking & paired camera listing
│   │   ├── signaling/         # Socket.IO WebRTC Signaling Server
│   │   ├── notifications/     # Firebase Cloud Messaging service
│   │   └── database/          # Prisma Client & PostgreSQL connection
│   ├── prisma/
│   │   └── schema.prisma      # PostgreSQL Database Schema
│   └── tests/                 # Unit & Integration Tests (Jest)
│
├── infrastructure/            # Production & Local Infrastructure
│   ├── aws/                   # ECS Fargate task definition & AWS architecture
│   ├── docker/                # Multi-stage Dockerfile & docker-compose.yml
│   └── coturn/                # Coturn TURN/STUN configuration (turnserver.conf)
│
├── docs/                      # Comprehensive Documentation
│   ├── architecture.md        # Technical architecture & WebRTC sequence diagrams
│   ├── database.md            # PostgreSQL ER diagrams & pairing security
│   ├── api.md                 # REST API & WebSocket events specification
│   ├── deployment.md          # Local docker-compose & AWS production guide
│   └── security.md            # Threat model, token lifecycle & privacy rules
└── README.md
```

---

## The 13 Complete Android Screens

| # | Screen | Description |
|---|---|---|
| 1 | **Splash** | VIRTUAL logo animation with automatic session check and persistent routing |
| 2 | **Welcome** | Get Started & Login buttons with registration/login modal |
| 3 | **Device Role** | Choose "Camera Device" (Phone 1) or "Viewer Device" (Phone 2) |
| 4 | **Camera Permission** | Clear permission rationale with settings fallback if permanently denied |
| 5 | **Camera Setup** | Lens facing (Rear/Front) and resolution selector (360p, 480p, 720p, 1080p) |
| 6 | **Pairing QR** | Stylized Blue+White QR code, human pairing code (`ABC-123`), and live socket listener |
| 7 | **Viewer Add Camera** | Scan QR button and manual 6-character code text field |
| 8 | **Pairing Success** | Confirmation badge: "✓ Camera Connected" and permanent pairing notification |
| 9 | **Camera Dashboard** | Live streaming status, battery, network, quality, and [ Stop Streaming ] button |
| 10 | **Viewer Dashboard** | "My Cameras" list with online/offline indicators, last seen, and [ VIEW LIVE ] |
| 11 | **Live View** | Full WebRTC video surface, Live badge, audio toggle, full screen, and reconnect |
| 12 | **Settings** | Video quality, FPS, audio toggle, auto-reconnect, and battery optimization |
| 13 | **Security** | Paired devices list, Revoke Pairing button, Remove Device, and Logout |

---

## Screen-Off Streaming & Background Reliability

1. **Foreground Service with Camera Type**:
   Phone 1 launches an Android Foreground Service with `android:foregroundServiceType="camera"`. It displays a persistent notification with a direct "Stop Streaming" button.
2. **Partial WakeLock**:
   Acquires `PowerManager.PARTIAL_WAKE_LOCK` ensuring the CPU and network remain active when the screen locks.
3. **Camera Conflict Detection (`CameraManager.AvailabilityCallback`)**:
   - Non-camera apps (WhatsApp text, Instagram feed, TikTok feed, browser): The stream continues normally.
   - Camera apps (WhatsApp video call, Instagram camera, TikTok recording): VIRTUAL detects hardware contention gracefully, sets status to `CONFLICT_BUSY`, notifies Phone 2 ("Camera temporarily in use by another app"), and automatically re-acquires capture as soon as the other app finishes.

---

## Quick Start: Running Locally

### 1. Start Database & Coturn TURN Relay
```bash
cd infrastructure/docker
docker-compose up -d postgres coturn
```

### 2. Setup & Run Backend
```bash
cd backend
npm install
npx prisma generate
npx prisma migrate dev --name init
npm run dev
```
The server will start at `http://localhost:4000`.

### 3. Run Backend Unit Tests
```bash
cd backend
npm test
```

### 4. Build Android Application
Open the `android/` directory in Android Studio and run on Phone 1 and Phone 2 (or two emulators).
- Configure the backend URL in `ApiClient.baseUrl` (defaults to `http://10.0.2.2:4000/` for local emulator testing, or your computer's LAN IP e.g. `http://192.168.1.50:4000/` for real physical devices).

---

## 17-Step MVP Verification Checklist

- [x] 1. Install VIRTUAL on Phone 1
- [x] 2. Install VIRTUAL on Phone 2
- [x] 3. Select Phone 1 as Camera Device
- [x] 4. Select Phone 2 as Viewer Device
- [x] 5. Grant camera permission on Phone 1
- [x] 6. Pair using QR code or 6-character code (`ABC-123`)
- [x] 7. Start streaming on Phone 1
- [x] 8. View Phone 1 live camera on Phone 2
- [x] 9. Put both phones on different networks (e.g. Wi-Fi ↔ 4G/5G)
- [x] 10. Verify live video works via STUN / Coturn TURN relay
- [x] 11. Turn Phone 1 screen off
- [x] 12. Verify stream continues via Foreground Service & WakeLock
- [x] 13. Use normal non-camera applications on Phone 1 (WhatsApp chat, browser)
- [x] 14. Verify VIRTUAL multitasking preserves the stream
- [x] 15. Close and reopen VIRTUAL
- [x] 16. Verify pairing remains saved permanently in `EncryptedSharedPreferences`
- [x] 17. Verify QR pairing is NOT required again
