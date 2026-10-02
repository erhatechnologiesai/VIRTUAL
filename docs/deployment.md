# VIRTUAL — Deployment Guide

## 1. Local Development & MVP Verification

### Prerequisites
- Node.js 20+
- Docker & Docker Compose
- Android Studio Iguana / Jellyfish (API 34 SDK)

### Step 1: Launch Local Backend Stack
From the project root:
```bash
cd infrastructure/docker
docker-compose up -d postgres coturn
```
This starts:
- PostgreSQL 16 on `localhost:5432` (`virtual_db`, `virtual_user`)
- Coturn TURN/STUN server on `localhost:3478`

### Step 2: Initialize Database & Run Backend
In `backend/`:
```bash
cd backend
npm install
npx prisma generate
npx prisma migrate dev --name init
npm run dev
```
The server will start at `http://localhost:4000` with WebRTC signaling ready on Socket.IO.

### Step 3: Run Unit Tests
```bash
npm test
```

---

## 2. Production AWS Deployment Architecture

```
AWS Cloud
│
├── Route 53 (api.virtualcam.io & turn.virtualcam.io)
│    │
│    ├──► Application Load Balancer (ACM SSL Certificate)
│    │     │
│    │     └──► ECS Fargate (virtual-backend Service)
│    │           ├── Auto-scaling (2 - 10 tasks)
│    │           ├── CloudWatch Container Insights
│    │           └── REST API & WebRTC Socket.IO Signaling
│    │
│    ├──► EC2 Instance / Elastic IP (Coturn TURN Server)
│    │     ├── UDP/TCP 3478 (STUN/TURN)
│    │     ├── TLS 5349 (TURNS)
│    │     └── UDP 49152-65535 (WebRTC Media Relay Ports)
│    │
│    └──► AWS RDS PostgreSQL (Multi-AZ)
│          └── Encrypted at rest (AWS KMS)
```

### AWS Components & Sizing
1. **ECS Fargate**:
   - 0.5 vCPU, 1 GB RAM per task.
   - Secrets mapped directly from **AWS Secrets Manager**.
2. **EC2 for Coturn**:
   - `t3.medium` or `c6i.large` in public subnet with Elastic IP.
   - Direct UDP packet forwarding for low-latency media relay.
3. **AWS RDS PostgreSQL**:
   - `db.t4g.micro` for MVP / staging; `db.r6g.large` with Multi-AZ for high-scale production.
4. **AWS Certificate Manager (ACM)**:
   - Free SSL/TLS certificates for `*.virtualcam.io`.
5. **CloudWatch**:
   - Centralized structured logs and alarm triggers for HTTP 5xx errors or CPU spikes.

---

## 3. Environment Variables Reference

| Variable | Description | Example |
|---|---|---|
| `PORT` | HTTP/Signaling port | `4000` |
| `NODE_ENV` | Environment mode | `production` |
| `DATABASE_URL` | PostgreSQL connection URI | `postgresql://user:pass@host:5432/virtual_db` |
| `JWT_SECRET` | 256-bit secret for Access Tokens | `openssl rand -hex 32` |
| `REFRESH_TOKEN_SECRET`| 256-bit secret for Refresh Tokens | `openssl rand -hex 32` |
| `STUN_SERVER_URL` | STUN discovery server | `stun:stun.l.google.com:19302` |
| `TURN_SERVER_URL` | Coturn TURN relay URI | `turn:turn.virtualcam.io:3478` |
| `TURN_USERNAME` | Long-term TURN username | `virtual_turn_user` |
| `TURN_PASSWORD` | Long-term TURN credential | `virtual_turn_secret_token` |
| `FIREBASE_PROJECT_ID` | FCM Project ID | `virtual-cam-system` |
