@echo off
title VIRTUAL - Backend ^& Cloudflare Tunnel Launcher
powershell -ExecutionPolicy Bypass -File "%~dp0start_server_and_tunnel.ps1"
pause
