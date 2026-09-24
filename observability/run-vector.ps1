param([Parameter(Mandatory = $true)][string]$VectorExe)
$ErrorActionPreference = 'Stop'
if (-not $env:VECTOR_THREADS) { $env:VECTOR_THREADS = '2' }
$platformRoot = Split-Path -Parent $PSScriptRoot
Push-Location $platformRoot
try {
    New-Item -ItemType Directory -Force -Path 'run-logs/vector-data', 'run-logs/access' | Out-Null
    & $VectorExe validate 'observability/vector.toml'
    if ($LASTEXITCODE -ne 0) { throw 'Vector configuration validation failed' }
    & $VectorExe --config 'observability/vector.toml'
} finally {
    Pop-Location
}
