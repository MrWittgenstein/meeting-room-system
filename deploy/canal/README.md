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

镜像只自动应用部分实例参数，因此 Compose 挂载 `start-canal.sh`，在启动前把 `CANAL_FILTER_REGEX` 和 TSDB 设置写入实际 `instance.properties`。默认过滤规则为 `meetingroom\..*`，失效服务同样通过 `CANAL_FILTER_REGEX` 配置订阅范围。隔离测试可改为 `meetingroom_integration\..*`。TSDB 默认启用，可用 `CANAL_TSDB_ENABLED=false` 关闭；本轮测试关闭 TSDB，避免元数据初始化扫描未授权库。

失效服务显式注册支持 Java 时间类型的 ObjectMapper，并使用非 daemon 消费线程保持非 Web 进程运行。每批失效发布等待 Rabbit Publisher Confirm；NACK、未路由或超时触发 Canal rollback。业务实例缓存订阅启动/恢复时清空缓存，防止 Rabbit 重启后临时队列丢失消息造成旧 Caffeine 条目继续使用。该机制提供最终一致性，不保证数据库提交到广播消费之间不存在短暂旧值。

2026-11-09 的两实例实测与故障恢复结果见 [`docs/integration-test-status.md`](../../docs/integration-test-status.md)。

业务服务默认启用 `CACHE_INVALIDATION_ENABLED=true`。每个实例的缓存广播队列默认带随机后缀，避免多实例竞争消费同一条失效消息。

当前映射：

- `meetingroom` 变更：清空 `meetingroom:list`
- `iot_sensor_record`、`iot_room_status`、`iot_device` 变更：删除对应 room/device 的最新缓存，并清空近期列表缓存
