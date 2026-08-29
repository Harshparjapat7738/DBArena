<#
.SYNOPSIS
  One-time setup: creates the 'DBArena' Postgres role + 'DBArena_identity'
  database that identity-service's .env already expects, on your local
  native Postgres 18 install. MUST be run as Administrator (it stops/starts
  the Windows service and briefly edits pg_hba.conf).

.DESCRIPTION
  You said you don't know/never set the postgres superuser password, so
  this can't just connect and CREATE ROLE directly. Instead it:
    1. Stops the postgresql-x64-18 service.
    2. Backs up pg_hba.conf, then temporarily sets local/127.0.0.1/::1
       connections to 'trust' (no password) so psql can connect as
       postgres with no credentials at all.
    3. Starts the service, creates role 'DBArena' (password 'DBArena') and
       database 'DBArena_identity' owned by it - matching .env's
       DBArena_IDENTITY_DB_URL/_USER/_PASSWORD exactly, no .env change
       needed.
    4. Stops the service, restores the original pg_hba.conf (back to
       scram-sha-256 - your Postgres is not left any less secure than it
       was), starts the service again.
  Safe to re-run - CREATE ROLE/DATABASE steps are skipped if they already
  exist.

.USAGE
  Right-click PowerShell -> Run as Administrator, then:
    cd "C:\Users\Harsh\OneDrive\Desktop\New folder (2)\DBArena\backend"
    .\setup-postgres-DBArena.ps1
#>

$ErrorActionPreference = "Stop"

$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Error "Not running as Administrator. Right-click PowerShell -> 'Run as Administrator' and try again."
    exit 1
}

$serviceName = "postgresql-x64-18"
$pgHome = "C:\Program Files\PostgreSQL\18"
$psql = "$pgHome\bin\psql.exe"
$hbaPath = "$pgHome\data\pg_hba.conf"
$hbaBackup = "$hbaPath.DBArena-setup-backup"

if (-not (Test-Path $psql)) { Write-Error "psql.exe not found at $psql - adjust `$pgHome if your install differs."; exit 1 }
if (-not (Test-Path $hbaPath)) { Write-Error "pg_hba.conf not found at $hbaPath"; exit 1 }

Write-Host "Stopping $serviceName..."
Stop-Service $serviceName

Write-Host "Backing up pg_hba.conf and switching local auth to 'trust' temporarily..."
Copy-Item $hbaPath $hbaBackup -Force
(Get-Content $hbaBackup) -replace 'scram-sha-256', 'trust' | Set-Content $hbaPath

try {
    Write-Host "Starting $serviceName with trust auth..."
    Start-Service $serviceName
    Start-Sleep -Seconds 3

    Write-Host "Creating role 'DBArena' and database 'DBArena_identity' (skipped if they already exist)..."
    & $psql -h localhost -U postgres -d postgres -v ON_ERROR_STOP=0 -c "DO `$`$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'DBArena') THEN CREATE ROLE DBArena LOGIN PASSWORD 'DBArena'; END IF; END `$`$;"
    $exists = & $psql -h localhost -U postgres -d postgres -t -A -c "SELECT 1 FROM pg_database WHERE datname = 'DBArena_identity';"
    if ($exists -ne "1") {
        & $psql -h localhost -U postgres -d postgres -c "CREATE DATABASE DBArena_identity OWNER DBArena;"
    } else {
        Write-Host "Database 'DBArena_identity' already exists - left as-is."
    }
}
finally {
    Write-Host "Stopping $serviceName to restore secure auth..."
    Stop-Service $serviceName
    Copy-Item $hbaBackup $hbaPath -Force
    Remove-Item $hbaBackup -Force
    Write-Host "Starting $serviceName with scram-sha-256 restored..."
    Start-Service $serviceName
}

Write-Host ""
Write-Host "Done. Verifying (should print '1' with no password prompt failure)..." -ForegroundColor Cyan
$env:PGPASSWORD = "DBArena"
& $psql -h localhost -U DBArena -d DBArena_identity -t -A -c "SELECT 1;"
Remove-Item Env:\PGPASSWORD
