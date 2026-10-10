# SkyWalking

SkyWalking is optional for local tracing. It receives telemetry from the Java
services through the SkyWalking Java Agent and exposes the dashboard at
`http://127.0.0.1:8088`.

## Start the collector and UI

From the repository root:

```powershell
docker compose -f deploy/skywalking/docker-compose.yml up -d
```

The compose file uses the OAP server's H2 storage for local development. Data
is not persisted across container replacement. Use an external storage backend
before deploying this setup to a shared or production environment.

## Download the Java Agent

Download a matching Apache SkyWalking Java Agent release and extract it outside
the repository. The repository must contain only the startup scripts and
configuration; do not commit `skywalking-agent.jar` or the extracted agent
directory.

## Start the Java services with tracing

Build the services first, then run the PowerShell helper:

```powershell
.'observability\start-skywalking-services.ps1' `
  -AgentJar 'D:\tools\skywalking-agent\skywalking-agent.jar'
```

The helper starts these services with distinct SkyWalking service names:

| Application | Service name | Port |
| --- | --- | --- |
| Gateway | `zongshe-topbiz` | 8080 |
| Meeting room | `meetingroom-main` | 8084 |
| User management | `meeting-user-service` | 8181 |
| Notification | `meeting-notification-service` | 8182 |

The collector address defaults to `127.0.0.1:11800` and can be changed with
`-CollectorAddress`. The helper writes process output to `run-logs/skywalking`.

SkyWalking is disabled when services are started using the existing commands or
JARs directly.
