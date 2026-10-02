# VIRTUAL — REST API & WebRTC Signaling Specification

## 1. Authentication Endpoints

### `POST /api/auth/register`
Creates a new user account.
- **Request Body**:
  ```json
  {
    "name": "Jane Doe",
    "email": "jane@example.com",
    "password": "SecurePassword123"
  }
  ```
- **Response `201 Created`**:
  ```json
  {
    "success": true,
    "data": {
      "user": { "id": "uuid", "name": "Jane Doe", "email": "jane@example.com" },
      "tokens": {
        "accessToken": "eyJhbGci...",
        "refreshToken": "eyJhbGci...",
        "tokenType": "Bearer",
        "expiresIn": "1h"
      }
    }
  }
  ```

### `POST /api/auth/login`
Authenticates existing credentials.
- **Request Body**:
  ```json
  { "email": "jane@example.com", "password": "SecurePassword123" }
  ```
- **Response `200 OK`**:
  ```json
  {
    "success": true,
    "data": {
      "user": { "id": "uuid", "name": "Jane Doe", "email": "jane@example.com" },
      "tokens": { "accessToken": "eyJ...", "refreshToken": "eyJ..." }
    }
  }
  ```

---

## 2. Device Identity Endpoints

### `POST /api/devices`
Registers or updates a device instance.
- **Headers**: `Authorization: Bearer <accessToken>`
- **Request Body**:
  ```json
  {
    "deviceType": "CAMERA",
    "deviceUuid": "VRT-CAM-984F2A10",
    "deviceName": "Living Room Camera",
    "platform": "ANDROID",
    "appVersion": "1.0.0"
  }
  ```

### `GET /api/devices`
Returns all devices associated with the authenticated account.

### `POST /api/devices/:id/telemetry`
Reports periodic device status (battery, network type, stream state).
- **Request Body**:
  ```json
  {
    "batteryLevel": 78,
    "networkType": "5G",
    "streamStatus": "STREAMING",
    "resolution": "720p",
    "fps": 30
  }
  ```

---

## 3. One-Time Pairing Endpoints

### `POST /api/pairing/create`
Invoked by Phone 1 (Camera) to initiate a pairing session.
- **Request Body**:
  ```json
  { "cameraDeviceId": "c1f7b0d2-8b4d-44aa-9f0a-7e382ff207b5" }
  ```
- **Response `201 Created`**:
  ```json
  {
    "success": true,
    "data": {
      "pairingId": "uuid",
      "cameraDeviceId": "uuid",
      "cameraUuid": "VRT-CAM-984F2A10",
      "pairingCode": "ABC-123",
      "pairingToken": "random_token_string",
      "qrData": "{\"v\":\"1.0\",\"action\":\"VIRTUAL_PAIR\",\"code\":\"ABC-123\",\"token\":\"...\"}",
      "expiresAt": "2026-10-02T17:10:00.000Z",
      "expiresInSeconds": 600
    }
  }
  ```

### `POST /api/pairing/join`
Invoked by Phone 2 (Viewer) after scanning the QR or typing `ABC-123`.
- **Request Body**:
  ```json
  {
    "viewerDeviceId": "v2a4b6c8-9d0e-41fa-8a1b-3c5e7g9h1j3k",
    "pairingCode": "ABC-123"
  }
  ```
- **Response `200 OK`**:
  ```json
  {
    "success": true,
    "data": {
      "id": "uuid",
      "cameraDeviceId": "c1f7b...",
      "viewerDeviceId": "v2a4b...",
      "pairingStatus": "ACTIVE",
      "pairedAt": "2026-10-02T17:02:15.000Z"
    }
  }
  ```

### `DELETE /api/pairing/:id`
Revokes an active pairing relationship.

---

## 4. Camera & Live Streaming Endpoints

### `GET /api/cameras`
Returns paired cameras accessible to the viewer.
- **Response `200 OK`**:
  ```json
  {
    "success": true,
    "data": [
      {
        "id": "c1f7b0d2-8b4d-44aa-9f0a-7e382ff207b5",
        "deviceUuid": "VRT-CAM-984F2A10",
        "deviceName": "Living Room Camera",
        "status": "STREAMING",
        "lastSeen": "2026-10-02T17:05:00.000Z",
        "cameraDetail": {
          "resolution": "720p",
          "fps": 30,
          "batteryLevel": 85,
          "networkType": "WIFI",
          "streamStatus": "STREAMING"
        }
      }
    ]
  }
  ```

### `GET /api/webrtc/ice-servers`
Dynamic STUN and TURN server credentials.
- **Response `200 OK`**:
  ```json
  {
    "success": true,
    "data": {
      "iceServers": [
        { "urls": ["stun:stun.l.google.com:19302"] },
        {
          "urls": ["turn:turn.virtualcam.io:3478"],
          "username": "virtual_turn_user",
          "credential": "virtual_turn_secret_auth_token_99"
        }
      ]
    }
  }
  ```

---

## 5. WebRTC WebSocket / Socket.IO Events

| Event Name | Direction | Payload | Description |
|---|---|---|---|
| `register_device` | Client ➔ Server | `{ deviceId, deviceType }` | Registers device on socket and joins room |
| `webrtc_offer` | Client ➔ Server ➔ Peer | `{ targetDeviceId, sdp }` | Relays SDP offer to peer |
| `webrtc_answer` | Client ➔ Server ➔ Peer | `{ targetDeviceId, sdp }` | Relays SDP answer to peer |
| `ice_candidate` | Client ➔ Server ➔ Peer | `{ targetDeviceId, candidate }` | Relays trickle ICE candidate |
| `stream_started` | Camera ➔ Server ➔ Viewer | `{ cameraId, resolution, fps }` | Announces stream start |
| `stream_stopped` | Client ➔ Server ➔ Peer | `{ cameraId, reason }` | Announces stream termination |
| `camera_busy` | Camera ➔ Server ➔ Viewer | `{ busy: true, message: "..." }` | Notifies viewer that third-party app acquired camera |
| `pairing_success` | Server ➔ Both Devices | `{ pairingId, cameraName }` | Real-time QR pairing confirmation |
