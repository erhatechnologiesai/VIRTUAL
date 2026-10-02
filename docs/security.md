# VIRTUAL — Security & Privacy Architecture

## 1. Authentication & Token Lifecycle

- **Password Storage**: Passwords are never stored in plaintext. They are salted and hashed using `bcrypt` (10 rounds).
- **Access Tokens**: Short-lived JSON Web Tokens (1 hour TTL) containing `userId` and `email`.
- **Refresh Tokens**: Long-lived tokens (30 days TTL) with rotation on every refresh request.
- **Transport Security**: All communication uses strict TLS 1.3 (HTTPS) and Secure WebSockets (WSS). No cleartext transmission permitted in production.

---

## 2. One-Time Pairing Security Model

1. **Short-Lived Secrets**:
   - The QR code contains an ephemeral 32-byte cryptographically secure random token and a 6-character code (`ABC-123`).
   - The token expires strictly after **10 minutes**.
2. **Immediate Invalidation**:
   - The moment Phone 2 claims the pairing, the temporary token and code are destroyed from the database and invalidated.
   - Even if the QR code is photographed, it cannot be reused.
3. **Permanent Authorized Pairing**:
   - An immutable record is created linking `cameraDeviceId` and `viewerDeviceId`.
   - On Android devices, pairing credentials are saved in **`EncryptedSharedPreferences`** backed by Android Keystore (AES-256 SIV + AES-256 GCM).
4. **Instant Revocation**:
   - Either Phone 1 or Phone 2 can revoke the pairing at any time.
   - Revocation triggers instant WebSocket signaling termination, dropping any active WebRTC peer connection.

---

## 3. Privacy & Anti-Covert Camera Safeguards

> **Zero Covert Operation**: VIRTUAL is built for authorized remote monitoring of user-owned devices. It deliberately implements transparent system indicators:
- **Foreground Notification**: Streaming cannot run silently. Android requires a persistent notification with the text *"Streaming live video to paired viewer"* and an immediate *"Stop Streaming"* action button.
- **Hardware Conflict Awareness**: If another app (e.g. WhatsApp video call, Instagram) accesses the camera, VIRTUAL immediately steps back and notifies the viewer. It never attempts to steal camera access covertly.
- **Explicit User Actions**: Camera streaming must be initiated explicitly by the user on Phone 1.
