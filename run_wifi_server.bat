@echo off
title VIRTUAL - Local Wi-Fi Backend Server
echo ==========================================================
echo    VIRTUAL Backend Server (Local Wi-Fi Mode)
echo ==========================================================
echo.
echo Your Computer Wi-Fi IP: 192.168.18.20
echo Backend Port: 4000
echo Mobile App Server URL: http://192.168.18.20:4000/
echo.
echo Make sure both phones are connected to the SAME Wi-Fi!
echo ==========================================================
echo.
cd /d "%~dp0backend"
node dist/index.js
pause
