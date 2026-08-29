<#
.SYNOPSIS
  Stops every service run-all.ps1 started. No windows exist to close (they
  run hidden), so this reads the PIDs run-all.ps1 recorded and kills each
  one's whole process tree - mvn spring-boot:run forks a child JVM by
  default, and a plain Stop-Process on just the mvn wrapper would leave that
  child running with the port still bound.

.USAGE
  From backend/:  .\stop-all.ps1
#>

$pidFile = Join-Path $PSScriptRoot "logs\run-all.pids"

if (-not (Test-Path $pidFile)) {
    Write-Warning "No $pidFile found - nothing recorded to stop (did run-all.ps1 ever run?)."
    exit 0
}

Get-Content $pidFile | Where-Object { $_ -match "=" } | ForEach-Object {
    $name, $procId = $_ -split "=", 2
    $procId = $procId.Trim()

    if (-not (Get-Process -Id $procId -ErrorAction SilentlyContinue)) {
        Write-Host "$name (PID $procId) - already stopped."
        return
    }

    Write-Host "Stopping $name (PID $procId, and its child processes)..."
    # /T kills the whole process tree (mvn's forked JVM included), /F forces it.
    & taskkill /PID $procId /T /F 2>&1 | Out-Null
}

Remove-Item $pidFile -Force
Write-Host ""
Write-Host "Done. Database services (Postgres/MongoDB/MySQL) were left running - stop"
Write-Host "those yourself via services.msc or Stop-Service if you want them down too."
