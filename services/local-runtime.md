# 本机完整启动

2026-11-09 已将当前界面使用的原本地业务库接入 RabbitMQ、Canal、XXL-JOB 和 Seata。前端 `http://127.0.0.1:5173/`，网关 8080，会议室 8084，用户 8181，通知 8182，日志查询 8083。用户管理页和登录继续使用会议室兼容用户库；独立用户库没有自动双写。

本机连接设置和六个服务配置位于忽略目录 `run-logs/local/`；其中包含连接凭据，不提交 Git。`settings.json` 字段为 `rabbitUsername`、`rabbitPassword`、`canalUsername`、`canalPassword`、`jobDbUsername`、`jobDbPassword`、`jobToken`。已有当前机器配置后，可从仓库根目录运行：

```powershell
.\services\start-local.ps1
# 已构建时可快速重启
.\services\start-local.ps1 -SkipBuild
# Docker 暂不可用时，只拉起应用（中间件连接会失败，不代表完整启动）
.\services\start-local.ps1 -SkipBuild -SkipInfrastructure
```

脚本要求 Docker Desktop、MySQL、Redis 和 ClickHouse 就绪；本机 JAR 构建需要 Java/Maven，前端 node_modules 已安装。它启动中间件并重启应用/前端，移除之前最小模式的禁用环境变量，检查服务及执行器端口。Vector 日志采集为独立进程，本次保持运行，配置为 `observability/vector.toml`。

## 运行数据源

- 业务库 `meetingroom`、用户库 `meetingroom_users`、通知库 `meetingroom_notifications`。
- 调度独立库 `xxl_job_local`；Admin 使用 `deploy/xxl-job/docker-compose.local.yml` 对接宿主机 MySQL，不需要再运行第二个 MySQL 容器。该库已使用 `deploy/xxl-job/mysql/01-schema.sql`、`02-project-jobs.sql` 初始化。
- Session/业务 Redis DB 0；通知 Redis DB 3。
- Rabbit 使用 `meeting.events`、`meeting.notification.events` 和 `cache.invalidation`。Canal 订阅 `meetingroom\..*`，TSDB 本机关闭，MySQL 已开 ROW/FULL Binlog 并配置复制账号。
- Seata TC `127.0.0.1:8091`；用户/通知服务启用真实 TM/RM 注册。当前没有全局事务业务入口，不能将注册视为业务回滚保证。

## 调度和派发

XXL-JOB Admin 为 `http://127.0.0.1:18080/xxl-job-admin/`，三个执行器端口 9996/9997/9998。已启用预约过期检查、公告过期更新、预约状态更新、提醒、站内消息派发、策略派发和 Outbox 投递。物理删除历史预约及临时成员清理仍停用，可经管理员核对后手动执行。

本机通知仅启用站内信，邮件和短信保持关闭；外部渠道需要核对账号配置及发送需求后启用。原公告页面仍读会议室公告接口，站内信在通知服务内部查询接口提供，暂未接入原公告前端。

## 验证

完整隔离联调结果见 `docs/integration-test-status.md`。接入原库后已确认三个执行器注册完整宿主机地址，Outbox/通知派发回调成功，Rabbit 事件及缓存队列均有消费者，Canal 订阅当前库，Seata 注册实际用户/通知库。系统管理员真实登录、12 条用户列表及 ClickHouse 日志查询通过。

本机配置是可重启运行配置，尚不是全新机器自动安装脚本。新机器需初始化数据库、补齐凭据和配置文件，再执行启动脚本。

2026-11-09 下午系统重启后 Secrets Engine socket 故障复发；用户处理后，14:50 已恢复 Docker 并通过完整启动脚本重启前后端。实查前端、网关、用户/通知健康、ClickHouse 均为 200，管理员登录/12 条用户列表/日志查询通过，MQ 消费者和 Canal 连接正常，三个执行器重新注册，Outbox 与通知派发新的回调为 200/200。启动期间 Admin 曾产生连接失败记录，应以恢复后的新执行记录判断当前状态。
