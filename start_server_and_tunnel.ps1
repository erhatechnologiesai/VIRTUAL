# PowerShell Script to Launch VIRTUAL Backend + Cloudflare Tunnel
$Host.UI.RawUI.WindowTitle = "VIRTUAL - Backend & Signaling Server + Tunnel"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Starting VIRTUAL Backend Server (Port 4000)..." -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan

$backendProcess = Start-Process -FilePath "node" -ArgumentList "dist/index.js" -WorkingDirectory "$PSScriptRoot\backend" -PassThru -NoNewWindow

Start-Sleep -Seconds 3

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "  Starting Cloudflare Quick Tunnel for WebRTC Signaling..." -ForegroundColor Green
Write-Host "  (Copy the *.trycloudflare.com URL into VIRTUAL app's ⚙️ icon)" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan

try {
    & "$PSScriptRoot\cloudflared.exe" tunnel --url http://localhost:4000
} finally {
    Write-Host "`nStopping backend process (PID: $($backendProcess.Id))..." -ForegroundColor Red
    Stop-Process -Id $backendProcess.Id -Force -ErrorAction SilentlyContinue
}
