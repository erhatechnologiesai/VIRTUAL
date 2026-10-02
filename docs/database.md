# VIRTUAL — PostgreSQL Database Schema

## 1. Storage Philosophy & Golden Rule

> **Important Rule**: The database is **NOT** the video transport system.
> The database strictly stores users, registered devices, permanent authorized pairings, settings, session metadata, device telemetry, and security audit logs.
> Live video frames travel exclusively through WebRTC peer-to-peer or coturn TURN relays.

---

## 2. Entity-Relationship Overview

```mermaid
erDiagram
    users ||--o{ devices : "registers"
    users ||--o{ audit_logs : "triggers"
    devices ||--o| camera_devices : "extends (if CAMERA)"
    devices ||--o| device_settings : "configures"
    devices ||--o{ push_tokens : "registers"
    devices ||--o{ pairings : "acts as camera or viewer"
    devices ||--o{ sessions : "participates in"

    users {
        uuid id PK
        string name
        string email UK
        string password_hash
        timestamp created_at
        timestamp updated_at
    }

    devices {
        uuid id PK
        uuid user_id FK
        string device_uuid UK "e.g. VRT-CAM-XXXXXXXX"
        string device_name
        enum device_type "CAMERA, VIEWER"
        string platform "ANDROID"
        string app_version
        enum status "ONLINE, OFFLINE, STREAMING, CONFLICT_BUSY"
        timestamp last_seen
        timestamp created_at
        timestamp updated_at
    }

    camera_devices {
        uuid id PK
        uuid device_id FK
        string camera_type "FRONT, BACK"
        string resolution "360p, 480p, 720p, 1080p"
        int fps "15, 30"
        boolean audio_enabled
        string stream_status "IDLE, STREAMING, CONFLICT_BUSY, ERROR"
        int battery_level
        string network_type "WIFI, 4G, 5G, NONE"
        timestamp last_stream_started
        timestamp last_stream_ended
        timestamp created_at
        timestamp updated_at
    }

    pairings {
        uuid id PK
        uuid camera_device_id FK
        uuid viewer_device_id FK
        enum pairing_status "PENDING, ACTIVE, REVOKED, EXPIRED"
        string pairing_token_hash
        string pairing_code "e.g. ABC-123"
        timestamp expires_at
        timestamp paired_at
        timestamp revoked_at
        timestamp created_at
        timestamp updated_at
    }

    sessions {
        uuid id PK
        uuid camera_device_id FK
        uuid viewer_device_id FK
        timestamp started_at
        timestamp ended_at
        enum status "ACTIVE, COMPLETED, FAILED"
        string disconnect_reason
        timestamp created_at
    }

    push_tokens {
        uuid id PK
        uuid device_id FK
        string provider "FCM"
        string token UK
        timestamp created_at
        timestamp updated_at
    }

    device_settings {
        uuid id PK
        uuid device_id FK
        string video_quality "720p"
        int fps "30"
        boolean audio_enabled "false"
        boolean auto_reconnect "true"
        timestamp created_at
        timestamp updated_at
    }

    audit_logs {
        uuid id PK
        uuid user_id FK
        uuid device_id FK
        string event_type
        string ip_address
        json metadata
        timestamp created_at
    }
```

---

## 3. Pairing Security Model

1. When a camera generates a pairing code, a **cryptographically secure random token** is generated and hashed with SHA-256 before storing in `pairings.pairing_token_hash`.
2. A human-friendly 6-character code (`ABC-123`) is stored in `pairings.pairing_code` with a 10-minute expiry (`expires_at`).
3. Upon viewer claim:
   - `pairing_status` transitions from `PENDING` to `ACTIVE`.
   - `pairing_token_hash` and `pairing_code` are set to `NULL` (invalidated).
   - `paired_at` is stamped.
4. Permanent pairing remains valid across reboots and app restarts until explicitly revoked via `DELETE /api/pairing/:id`.
