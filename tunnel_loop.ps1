# Infinite Auto-reconnecting SSH Tunnel with Keep-Alive Pings
$ErrorActionPreference = 'SilentlyContinue'
while ($true) {
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Starting localhost.run SSH tunnel..."
    ssh -o ServerAliveInterval=5 -o ServerAliveCountMax=3 -o StrictHostKeyChecking=no -R 80:localhost:4000 nokey@localhost.run
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Tunnel disconnected, auto-reconnecting in 2 seconds..."
    Start-Sleep -Seconds 2
}
