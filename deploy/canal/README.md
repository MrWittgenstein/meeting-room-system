# Canal Binlog 缓存失效链路

```text
MySQL binlog
  -> Canal Server
  -> cache-invalidation-service
  -> RabbitMQ cache.invalidation exchange
  -> 每个会议室服务实例的独立临时队列
  -> 删除本机 Caffeine L1 + Redis L2
```

## MySQL 前置条件

Canal 读取 MySQL binlog，源库必须开启 ROW 格式 binlog，并为 Canal 账号授予复制权限：

```sql
CREATE USER 'canal'@'%' IDENTIFIED BY 'change-this-password';
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';
FLUSH PRIVILEGES;
```

MySQL 配置至少需要：

```ini
server-id=223344
log-bin=mysql-bin
binlog-format=ROW
binlog-row-image=FULL
```

## 启动

```powershell
$env:CANAL_DB_USERNAME = "canal"
$env:CANAL_DB_PASSWORD = "change-this-password"
docker compose -f deploy/canal/docker-compose.yml up -d
```

再启动 `cache-invalidation-service`，并确保它使用与业务服务相同的 RabbitMQ 配置。Canal Server 使用官方 `canal/canal-server:v1.1.8` 镜像；其 Docker 参数支持通过 `canal.instance.*` 配置源 MySQL。

业务服务默认启用 `CACHE_INVALIDATION_ENABLED=true`。每个实例的缓存广播队列默认带随机后缀，避免多实例竞争消费同一条失效消息。

当前映射：

- `meetingroom` 变更：清空 `meetingroom:list`
- `iot_sensor_record`、`iot_room_status`、`iot_device` 变更：删除对应 room/device 的最新缓存，并清空近期列表缓存
