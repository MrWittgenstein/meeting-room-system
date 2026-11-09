param([switch]$SkipBuild, [switch]$SkipInfrastructure)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
Set-Location $taskRoot
$configDir = Join-Path $taskRoot 'run-logs/local'
$settingsPath = Join-Path $configDir 'settings.json'
if (-not (Test-Path -LiteralPath $settingsPath)) {
    throw 'Missing run-logs/local/settings.json. See services/local-runtime.md.'
}
$settings = Get-Content -LiteralPath $settingsPath -Raw | ConvertFrom-Json
$env:RABBITMQ_USERNAME = $settings.rabbitUsername
$env:RABBITMQ_PASSWORD = $settings.rabbitPassword
$env:CANAL_DB_USERNAME = $settings.canalUsername
$env:CANAL_DB_PASSWORD = $settings.canalPassword
$env:CANAL_FILTER_REGEX = 'meetingroom\..*'
$env:CANAL_TSDB_ENABLED = 'false'
$env:XXL_JOB_DB_NAME = 'xxl_job_local'
$env:XXL_JOB_DB_USERNAME = $settings.jobDbUsername
$env:XXL_JOB_MYSQL_ROOT_PASSWORD = $settings.jobDbPassword
$env:XXL_JOB_ACCESS_TOKEN = $settings.jobToken
if (-not $SkipInfrastructure) {
docker info --format '{{.ServerVersion}}' | Out-Host
if ($LASTEXITCODE -ne 0) { throw 'Docker Desktop is not ready.' }
foreach ($compose in @('deploy/rabbitmq/docker-compose.yml', 'deploy/canal/docker-compose.yml', 'deploy/xxl-job/docker-compose.local.yml', 'deploy/seata/docker-compose.yml')) {
    docker compose -f $compose up -d --pull never
    if ($LASTEXITCODE -ne 0) { throw "Compose failed: $compose" }
}
}

# Only stop application processes whose known module jar paths belong to this project.
$modulePattern = '(backend|topbiz-gateway|log-service|user-service|notification-service|cache-invalidation-service)[/\\]target[/\\]'
$processes = Get-CimInstance Win32_Process | Where-Object {
    $_.Name -eq 'java.exe' -and $_.CommandLine -match $modulePattern
}
foreach ($process in $processes) { Stop-Process -Id $process.ProcessId -ErrorAction SilentlyContinue }
$frontend = Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue
foreach ($connection in $frontend) {
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$($connection.OwningProcess)"
    if ($process.CommandLine -match 'node_modules[/\\]vite[/\\]bin[/\\]vite.js') {
        Stop-Process -Id $process.ProcessId -ErrorAction SilentlyContinue
    } else { throw 'Port 5173 is occupied by a different application.' }
}
if (-not $SkipBuild) {
    foreach ($pom in @('backend/pom.xml','user-service/pom.xml','notification-service/pom.xml','cache-invalidation-service/pom.xml')) {
        mvn -f $pom -DskipTests package
        if ($LASTEXITCODE -ne 0) { throw "Build failed: $pom" }
    }
}
$java = (Get-Command java).Source
$apps = @(
    @{name='user-service'; jar='user-service/target/user-service-1.0.0-SNAPSHOT.jar'; config='users.yml'},
    @{name='notification-service'; jar='notification-service/target/notification-service-1.0.0-SNAPSHOT.jar'; config='notification.yml'},
    @{name='meetingroom-main'; jar='backend/target/meetroom_management-0.0.1-SNAPSHOT.jar'; config='backend.yml'},
    @{name='cache-bridge'; jar='cache-invalidation-service/target/cache-invalidation-service-1.0.0-SNAPSHOT.jar'; config='cache.yml'},
    @{name='gateway'; jar='topbiz-gateway/target/topbiz-gateway-1.0.0-SNAPSHOT.jar'; config='gateway.yml'},
    @{name='log-service'; jar='log-service/target/log-service-1.0.0-SNAPSHOT.jar'; config='logs.yml'}
)
# Explicitly remove disabled flags left by an earlier minimal local session.
foreach ($variable in @('XXL_JOB_ENABLED','MEETING_EVENTS_ENABLED','CACHE_INVALIDATION_ENABLED','SEATA_ENABLED','USER_SERVICE_URL','NOTIFICATION_SERVICE_URL','SPRING_DATA_REDIS_DATABASE','MEETING_EVENTS_EXCHANGE','MEETING_EVENTS_QUEUE','MEETING_EVENTS_DLX','MEETING_EVENTS_DLQ','CACHE_INVALIDATION_EXCHANGE')) {
    Remove-Item -LiteralPath "Env:$variable" -ErrorAction SilentlyContinue
}
foreach ($app in $apps) {
    $config = 'file:run-logs/local/' + $app.config
    $process = Start-Process -FilePath $java -ArgumentList @('-Xmx512m','-jar',$app.jar,"--spring.config.additional-location=$config") -WorkingDirectory $taskRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $configDir "$($app.name).out.log") -RedirectStandardError (Join-Path $configDir "$($app.name).err.log") -PassThru
    Write-Host "$($app.name): PID $($process.Id)"
}
$process = Start-Process -FilePath (Get-Command node).Source -ArgumentList @('node_modules/vite/bin/vite.js','--host','127.0.0.1','--port','5173','--strictPort') -WorkingDirectory (Join-Path $taskRoot 'frontend') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $configDir 'frontend.out.log') -RedirectStandardError (Join-Path $configDir 'frontend.err.log') -PassThru
Write-Host "Frontend PID $($process.Id): http://127.0.0.1:5173/"
$deadline = (Get-Date).AddSeconds(75)
do {
    $ports = @(Get-NetTCPConnection -State Listen | Select-Object -ExpandProperty LocalPort)
    $missing = @(5173,8080,8083,8084,8181,8182,9996,9997,9998 | Where-Object { $_ -notin $ports })
    if ($missing.Count -eq 0) { break }
    if ((Get-Date) -ge $deadline) { throw "Services did not start on ports: $missing. Inspect run-logs/local." }
    Start-Sleep -Seconds 2
} while ($true)
Write-Host 'Frontend, backend services and all three job executors are listening.'
