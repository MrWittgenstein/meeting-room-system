[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateScript({ Test-Path -LiteralPath $_ -PathType Leaf })]
    [string]$AgentJar,

    [string]$CollectorAddress = "127.0.0.1:11800",
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = "Stop"
$agentPath = (Resolve-Path -LiteralPath $AgentJar).Path
$root = (Resolve-Path -LiteralPath $ProjectRoot).Path
$logRoot = Join-Path $root "run-logs\skywalking"
New-Item -ItemType Directory -Force -Path $logRoot | Out-Null

$services = @(
    @{ Name = "zongshe-topbiz"; Jar = "topbiz-gateway\target\topbiz-gateway-1.0.0-SNAPSHOT.jar" },
    @{ Name = "meetingroom-main"; Jar = "backend\target\meetroom_management-0.0.1-SNAPSHOT.jar" },
    @{ Name = "meeting-user-service"; Jar = "user-service\target\user-service-1.0.0-SNAPSHOT.jar" },
    @{ Name = "meeting-notification-service"; Jar = "notification-service\target\notification-service-1.0.0-SNAPSHOT.jar" }
)

foreach ($service in $services) {
    $jarPath = Join-Path $root $service.Jar
    if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
        throw "Missing JAR for $($service.Name): $jarPath. Build the service before starting it."
    }

    $safeName = $service.Name.Replace("/", "_")
    $stdout = Join-Path $logRoot "$safeName.out.log"
    $stderr = Join-Path $logRoot "$safeName.err.log"
    $javaArgs = @(
        "-javaagent:$agentPath",
        "-Dskywalking.agent.service_name=$($service.Name)",
        "-Dskywalking.collector.backend_service=$CollectorAddress",
        "-jar",
        $jarPath
    )

    Start-Process -FilePath "java.exe" `
        -ArgumentList $javaArgs `
        -WorkingDirectory $root `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr `
        -WindowStyle Hidden | Out-Null

    Write-Host "Started $($service.Name); logs: $stdout"
}
