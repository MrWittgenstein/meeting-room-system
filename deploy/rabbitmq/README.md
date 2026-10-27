# RabbitMQ 业务事件

会议室服务发布公告和预约生命周期事件，消息通告服务消费事件后写入自己的 `message` 表。主页继续通过通知服务 HTTP 接口读取消息。

## 启动

```powershell
$env:RABBITMQ_USERNAME = "meeting"
$env:RABBITMQ_PASSWORD = "change-this-password"
docker compose -f deploy/rabbitmq/docker-compose.yml up -d
```

管理界面：<http://127.0.0.1:15672>

服务默认使用 `127.0.0.1:5672`、用户名 `guest`。使用本 Compose 文件时，请把应用连接配置改为相同的 `RABBITMQ_USERNAME` 和 `RABBITMQ_PASSWORD`。线上环境应使用独立账号和强密码，不要使用 RabbitMQ 默认 `guest` 账号。

## 可靠性

- 业务库中的 `meeting_event_outbox` 与预约/公告写入使用同一事务，避免业务成功但事件丢失。
- XXL-JOB 每 5 秒投递待发布事件，发布失败采用退避重试。
- 通知服务的 `consumed_meeting_event` 以 `event_id` 去重。
- 消费失败超过监听器重试次数后进入 RabbitMQ 死信队列 `meeting.notification.events.dlq`。
