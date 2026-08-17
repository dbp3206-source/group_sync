$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$runDir = Join-Path $repoRoot '.run'

foreach ($name in @('frontend', 'backend')) {
    $pidFile = Join-Path $runDir "$name.pid"
    if (-not (Test-Path -LiteralPath $pidFile)) { continue }
    $processId = [int](Get-Content -LiteralPath $pidFile -Raw)
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if ($process) {
        Stop-Process -Id $processId -Force
        Write-Output "Stopped $name process $processId."
    }
    Remove-Item -LiteralPath $pidFile -Force
}

