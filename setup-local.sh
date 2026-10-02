#!/usr/bin/env bash
set -e

echo "=============================================================================="
echo "  VIRTUAL Live-Camera System - Local Environment Setup (Unix/Linux/macOS)"
echo "=============================================================================="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "[1/5] Starting PostgreSQL and Coturn TURN containers..."
cd infrastructure/docker
docker compose up -d postgres coturn || echo "[WARNING] Docker not available, skipping container launch."
cd ../..

echo "[2/5] Installing backend dependencies..."
cd backend
npm install

echo "[3/5] Generating Prisma Client..."
npx prisma generate

echo "[4/5] Running Backend Unit Tests..."
npm test

echo "[5/5] Building & Starting VIRTUAL Backend & Signaling Server..."
npm run build
node dist/index.js
