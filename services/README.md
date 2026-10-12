# Meeting room platform services

The platform is deployed as three independent Spring Boot applications:

```text
meeting-room-system-main/meetingroom-main       # meeting rooms, reservations, approvals, IoT
user-service                                     # users, authentication, groups, permissions
notification-service                             # announcements, in-app messages, email, SMS
```

跨服务公告和预约事件使用 RabbitMQ 异步投递，通知服务仍通过 HTTP 提供主页查询接口。RabbitMQ 的启动方式见 [`deploy/rabbitmq/README.md`](../deploy/rabbitmq/README.md)。

Default local ports:

```text
user-service:         8181
notification-service: 8182
meetingroom-main:     8084
topbiz gateway:       8080
```

## Service discovery

Nacos is the service registry. Start the local registry with:

```powershell
docker compose -f deploy/nacos/docker-compose.yml up -d
```

Open `http://127.0.0.1:8848/nacos`, create a namespace whose ID is
`meeting-room`, and keep the group `MEETING_ROOM_GROUP`.

Then enable discovery for all services before starting them:

```powershell
$env:NACOS_ENABLED="true"
$env:PLATFORM_DISCOVERY_ENABLED="true"
$env:GATEWAY_DISCOVERY_ENABLED="true"
```

Registered service names:

```text
meetingroom-main
meeting-user-service
meeting-notification-service
zongshe-topbiz
```

When discovery is disabled, the project uses the configured local URLs as a
development fallback. Seata at `127.0.0.1:8091` is a transaction coordinator,
not the service registry.

The meeting application calls the two platform services over HTTP. Configure
`USER_SERVICE_URL`, `NOTIFICATION_SERVICE_URL`, and `PLATFORM_INTERNAL_TOKEN`
through the environment. The legacy integer users are imported with
`legacy_user_id` mappings. The copied services use dedicated
`meetingroom_users` and `meetingroom_notifications` databases; notification
Redis data uses DB 3.
