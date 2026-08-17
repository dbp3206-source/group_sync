param(
    [int]$BackendPort = 8080,
    [int]$FrontendPort = 4173
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$runDir = Join-Path $repoRoot '.run'
$envFile = Join-Path $repoRoot '.env'
$localEnvFile = Join-Path $repoRoot '.env.local'
$javaPath = Join-Path $repoRoot '.jdk21-runtime\bin\java.exe'
$backendJar = Join-Path $repoRoot 'backend\target\backend-0.0.1-SNAPSHOT.jar'
$frontendDir = Join-Path $repoRoot 'frontend'
$vitePath = Join-Path $frontendDir 'node_modules\vite\bin\vite.js'

function Import-LocalEnvironment([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) { return }
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith('#') -or -not $trimmed.Contains('=')) { continue }
        $name, $value = $trimmed.Split('=', 2)
        [Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim(), 'Process')
    }
}

function Test-Url([string]$Url) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 2
        return $response.StatusCode -eq 200
    } catch {
        return $false
    }
}

function Wait-ForUrl([string]$Url, [int]$Seconds, [System.Diagnostics.Process]$Process) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    while ((Get-Date) -lt $deadline) {
        if ($Process.HasExited) { return $false }
        if (Test-Url $Url) { return $true }
        Start-Sleep -Milliseconds 750
    }
    return $false
}

function Repair-DuplicatePathVariable {
    $variables = [Environment]::GetEnvironmentVariables('Process')
    $pathValue = [string]$variables['Path']
    if (-not $pathValue) { $pathValue = [string]$variables['PATH'] }
    [Environment]::SetEnvironmentVariable('PATH', $null, 'Process')
    [Environment]::SetEnvironmentVariable('Path', $null, 'Process')
    [Environment]::SetEnvironmentVariable('Path', $pathValue, 'Process')
}

New-Item -ItemType Directory -Path $runDir -Force | Out-Null
Import-LocalEnvironment $envFile
Import-LocalEnvironment $localEnvFile
Repair-DuplicatePathVariable

if (-not $env:SPRING_DATASOURCE_PASSWORD -and -not $env:DB_PASSWORD) {
    throw 'A datasource password is missing. Copy .env.local.example to .env.local and set the Neon development credentials, or use the existing .env local PostgreSQL credentials.'
}
if (-not (Test-Path -LiteralPath $javaPath)) { throw "Java 21 runtime not found: $javaPath" }
if (-not (Test-Path -LiteralPath $backendJar)) { throw "Backend JAR not found. Run backend\mvnw.cmd test package first." }
if (-not (Test-Path -LiteralPath $vitePath)) { throw 'Frontend dependencies not found. Run npm.cmd install inside frontend first.' }

$backendUrl = "http://127.0.0.1:$BackendPort"
$frontendUrl = "http://127.0.0.1:$FrontendPort"

$env:SERVER_PORT = "$BackendPort"
if (-not $env:SPRING_DATASOURCE_URL -and -not $env:DB_URL) { $env:DB_URL = 'jdbc:postgresql://127.0.0.1:54329/groupsync_dev' }
if (-not $env:SPRING_DATASOURCE_USERNAME -and -not $env:DB_USERNAME) { $env:DB_USERNAME = 'groupsync' }
$env:APP_CORS_ORIGINS = "$frontendUrl,http://localhost:$FrontendPort"

$backendOut = Join-Path $runDir 'backend.out.log'
$backendErr = Join-Path $runDir 'backend.err.log'
$backendStarted = $false
$backend = $null
if (Test-Url "$backendUrl/api/health") {
    Write-Output "Backend already healthy at $backendUrl; reusing it."
} else {
    $backend = Start-Process -FilePath $javaPath -ArgumentList @('-jar', ('"' + $backendJar + '"')) -WorkingDirectory $repoRoot -WindowStyle Hidden -RedirectStandardOutput $backendOut -RedirectStandardError $backendErr -PassThru
    $backendStarted = $true
    $backend.Id | Set-Content -LiteralPath (Join-Path $runDir 'backend.pid')

    if (-not (Wait-ForUrl "$backendUrl/api/health" 150 $backend)) {
        if (-not $backend.HasExited) { Stop-Process -Id $backend.Id -Force }
        $details = if (Test-Path -LiteralPath $backendOut) { (Get-Content -LiteralPath $backendOut -Tail 25) -join [Environment]::NewLine } else { 'No backend log was written.' }
        throw "Backend did not become healthy. Log:`n$details"
    }
}

$env:VITE_API_URL = ''
Push-Location $frontendDir
try {
    & npm.cmd run build
    if ($LASTEXITCODE -ne 0) { throw "Frontend build failed with exit code $LASTEXITCODE." }
} finally {
    Pop-Location
}

$frontendOut = Join-Path $runDir 'frontend.out.log'
$frontendErr = Join-Path $runDir 'frontend.err.log'
$frontendPidFile = Join-Path $runDir 'frontend.pid'
if (Test-Path -LiteralPath $frontendPidFile) {
    $savedFrontendId = [int](Get-Content -LiteralPath $frontendPidFile -Raw)
    if (-not (Get-Process -Id $savedFrontendId -ErrorAction SilentlyContinue)) {
        Remove-Item -LiteralPath $frontendPidFile -Force
    }
}

if (Test-Url $frontendUrl) {
    Write-Output "Frontend already healthy at $frontendUrl; reusing it."
} else {
    $frontend = Start-Process -FilePath 'C:\Program Files\nodejs\node.exe' -ArgumentList @(('"' + $vitePath + '"'), 'preview', '--host', '127.0.0.1', '--port', "$FrontendPort", '--strictPort') -WorkingDirectory $frontendDir -WindowStyle Hidden -RedirectStandardOutput $frontendOut -RedirectStandardError $frontendErr -PassThru
    $frontend.Id | Set-Content -LiteralPath $frontendPidFile

    if (-not (Wait-ForUrl $frontendUrl 30 $frontend)) {
        if (-not $frontend.HasExited) { Stop-Process -Id $frontend.Id -Force }
        if ($backendStarted -and -not $backend.HasExited) { Stop-Process -Id $backend.Id -Force }
        throw "Frontend did not become healthy. Check $frontendErr."
    }
}

Write-Output "GROUPSYNC_READY frontend=$frontendUrl backend=$backendUrl/api/health"
Write-Output "Stop both services with: powershell.exe -ExecutionPolicy Bypass -File .\scripts\stop-groupsync.ps1"
exit 0
