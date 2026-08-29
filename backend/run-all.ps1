<#
.SYNOPSIS
  Starts the complete DBArena backend and launches every runnable service
  (identity-service, catalog-service, ai-assistant-service, api-gateway) as
  a hidden background process - no PowerShell windows are opened. Each
  service's output goes to its own log file under backend/logs/.

  identity-service and catalog-service both read from MongoDB Atlas (see
  .env's DBArena_IDENTITY_MONGO_URI / DBArena_CATALOG_MONGO_URI) - no local
  Mongo install needed for either. MySQL is still local-only and not used
  by any backend service yet (no execution-service/B09 exists to wire
  adapter-mysql in) - this script only makes sure it's running for your own
  manual testing convenience.

.DESCRIPTION
  Dev convenience only - this repo has no root Makefile/docker-compose yet
  (see root CLAUDE.md's Commands section: "make backend ... not implemented
  yet"). This script is the single-command stand-in for that until one
  exists. Not part of any milestone's deliverable - safe to edit freely.

  Assumes MySQL is already installed as a local Windows service (MySQL80 by
  default - see $mysqlServiceName below if yours is named differently) for
  the manual-testing convenience above. identity-service and catalog-service
  need no local database service at all - both point at MongoDB Atlas via
  .env. (setup-postgres-DBArena.ps1 is no longer needed by identity-service -
  it moved off Postgres onto Mongo; keep it around only if some other,
  not-yet-built service ends up needing a local Postgres role/database.)

  Loads ../.env (repo root) into each spawned process's environment, since
  Spring Boot reads real env vars, not the .env file itself.

.USAGE
  From backend/:  .\run-all.ps1
  Watch a service: Get-Content backend\logs\<service-name>.log -Wait -Tail 50
  Stop:            .\stop-all.ps1  (kills every process this script started,
                    including mvn's forked JVM child - closing a window no
                    longer applies, nothing is visible to close). The
                    database services are left running (they're normal
                    Windows services, not something this script owns).
#>

$mysqlServiceName = "MySQL80"

$repoRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $repoRoot ".env"

if (-not (Test-Path $envFile)) {
    Write-Error "No .env found at $envFile - see README/CLAUDE.md for what it needs."
    exit 1
}

function Get-EnvMap($path) {
    $map = @{}
    Get-Content $path | Where-Object { $_ -match "=" -and $_ -notmatch '^\s*#' } | ForEach-Object {
        $name, $value = $_ -split "=", 2
        $map[$name.Trim()] = $value.Trim()
    }
    return $map
}
$envMap = Get-EnvMap $envFile

# --- 1. Make sure each local database service is actually running - start it if not. ---
function Confirm-LocalService($serviceName, $whatFor) {
    $svc = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
    if (-not $svc) {
        Write-Warning "No Windows service named '$serviceName' found ($whatFor) - is it installed, or is the service name different on your machine? Edit `$${serviceName}ServiceName at the top of this script if so."
        return
    }
    if ($svc.Status -eq "Running") {
        Write-Host "'$serviceName' already running ($whatFor)."
        return
    }
    try {
        Write-Host "Starting '$serviceName' ($whatFor)..."
        Start-Service -Name $serviceName
        Write-Host "'$serviceName' started."
    } catch {
        Write-Warning "Could not start '$serviceName' - it may need an elevated (Administrator) PowerShell, or start it manually via services.msc. Error: $($_.Exception.Message)"
    }
}

Confirm-LocalService $mysqlServiceName "adapter-mysql manual testing only - no backend service needs this yet"

Write-Host ""
Write-Host "identity-service and catalog-service both read from MongoDB Atlas (see .env) -"
Write-Host "no local database service needed for either."
Write-Host ""

# --- 2. Each service as a hidden background process, with .env loaded first ---
# api-gateway's port comes from DBArena_GATEWAY_URL in .env (single source of
# truth) rather than being hardcoded twice - see .env's own comment on why
# it's 8090, not the documented default 8080, on this machine specifically
# (an unrelated project's Docker container already owns 8080 here).
$gatewayPort = if ($envMap.DBArena_GATEWAY_URL -match ':(\d+)\s*$') { $matches[1] } else { "8080" }

$services = @(
    @{ Name = "identity-service";      Path = "services/identity-service" },
    @{ Name = "catalog-service";       Path = "services/catalog-service" },
    @{ Name = "ai-assistant-service";  Path = "services/ai-assistant-service" },
    @{ Name = "api-gateway";           Path = "services/api-gateway"; Port = $gatewayPort }
)

# NOTE: built with .Replace(), not the -f format operator - this template's own
# PowerShell script blocks (Where-Object { ... }, ForEach-Object { ... }) contain
# literal { } characters that -f would wrongly try to parse as format placeholders
# and throw on ("Error formatting a string: Input string was not in a correct format").
$loadEnvTemplate = @'
Get-Content "__ENV_FILE__" | Where-Object { $_ -match "=" -and $_ -notmatch "^\s*#" } | ForEach-Object {
    $name, $value = $_ -split "=", 2
    Set-Item -Path "Env:$($name.Trim())" -Value $value.Trim()
}
'@
$loadEnv = $loadEnvTemplate.Replace("__ENV_FILE__", $envFile)

# --- No visible windows: each service is a hidden background process, output
# redirected to its own log file. PIDs are recorded so stop-all.ps1 can kill
# the whole tree later (mvn spring-boot:run forks a child JVM by default -
# closing a window used to stop that; there's no window now, so stop-all.ps1
# uses `taskkill /T` to take the fork with it, not just the mvn wrapper).
$logsDir = Join-Path $PSScriptRoot "logs"
if (-not (Test-Path $logsDir)) { New-Item -ItemType Directory -Path $logsDir | Out-Null }
$pidFile = Join-Path $logsDir "run-all.pids"
if (Test-Path $pidFile) { Remove-Item $pidFile }

foreach ($svc in $services) {
    # $loadEnv sets whatever PORT (if any) came from .env for every service -
    # api-gateway's own explicit override here is set AFTER $loadEnv runs, so
    # it always wins for that process regardless of what .env does or
    # doesn't say about PORT.
    $portOverride = if ($svc.Port) { "`$env:PORT = '$($svc.Port)'; " } else { "" }
    $cmd = "$loadEnv; ${portOverride}mvn -pl $($svc.Path) -am spring-boot:run"
    $outLog = Join-Path $logsDir "$($svc.Name).log"
    $errLog = Join-Path $logsDir "$($svc.Name).err.log"
    $proc = Start-Process powershell `
        -ArgumentList "-NoLogo", "-NoProfile", "-Command", $cmd `
        -WorkingDirectory $PSScriptRoot `
        -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput $outLog -RedirectStandardError $errLog
    Add-Content -Path $pidFile -Value "$($svc.Name)=$($proc.Id)"
    Write-Host "Launched $($svc.Name) (PID $($proc.Id)) -> log: $outLog"
}

Write-Host ""
Write-Host "All services launching in the background (no windows opened). Once ready:"
Write-Host "  api-gateway          -> http://localhost:$gatewayPort"
Write-Host "  identity-service     -> http://localhost:8081"
Write-Host "  catalog-service      -> http://localhost:8083"
Write-Host "  ai-assistant-service -> http://localhost:8084"
Write-Host ""
Write-Host "Tail a service's log:  Get-Content backend\logs\<service-name>.log -Wait -Tail 50"
Write-Host "Stop everything:       .\stop-all.ps1"
Write-Host ""
Write-Host "The database services keep running as normal Windows services until you stop"
Write-Host "them yourself (services.msc, or 'Stop-Service $pgServiceName' etc. from an"
Write-Host "elevated prompt) - stop-all.ps1 does not touch them."
