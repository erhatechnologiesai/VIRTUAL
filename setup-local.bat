@echo off
echo ==============================================================================
echo   VIRTUAL Live-Camera System - Local Environment Setup
echo ==============================================================================

cd /d "%~dp0"

echo [1/5] Starting PostgreSQL and Coturn TURN containers via Docker...
cd infrastructure\docker
docker-compose up -d postgres coturn
if %errorlevel% neq 0 (
    echo [WARNING] Docker not available or containers failed to start. Ensure Docker is running if testing with local containers.
)
cd ..\..

echo [2/5] Installing backend dependencies...
cd backend
call npm.cmd install
if %errorlevel% neq 0 (
    echo [ERROR] Failed to install backend npm dependencies.
    pause
    exit /b %errorlevel%
)

echo [3/5] Generating Prisma Client...
node ./node_modules/prisma/build/index.js generate
if %errorlevel% neq 0 (
    echo [ERROR] Prisma generate failed.
    pause
    exit /b %errorlevel%
)

echo [4/5] Running Backend Unit Tests...
call npm.cmd test
if %errorlevel% neq 0 (
    echo [ERROR] Tests failed.
    pause
    exit /b %errorlevel%
)

echo [5/5] Building & Starting VIRTUAL Backend & Signaling Server...
call npm.cmd run build
node dist/index.js

pause
