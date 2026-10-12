# XXL-JOB 调度中心

会议室服务、用户服务和消息通告服务的定时任务统一由 XXL-JOB Admin 调度，服务本身只保留 `@XxlJob` 处理器。跨服务通知事件通过 RabbitMQ 投递，会议室服务的 Outbox 投递任务也由 XXL-JOB 调度。

## 启动

在仓库根目录执行：

```powershell
$env:XXL_JOB_MYSQL_ROOT_PASSWORD = "change-this-password"
$env:XXL_JOB_ACCESS_TOKEN = "change-this-access-token"
docker compose -f deploy/xxl-job/docker-compose.yml up -d
```

管理端地址：<http://127.0.0.1:18080/xxl-job-admin>

首次登录账号为 `admin`，密码为 `123456`。部署到共享环境后请立即修改密码和访问令牌。

## 服务配置

三个服务默认连接 `http://127.0.0.1:18080/xxl-job-admin`，并使用环境变量 `XXL_JOB_ACCESS_TOKEN`。服务直接运行在宿主机时，执行器注册地址默认是 `host.docker.internal`，这样 Docker 中的 Admin 可以回调宿主机执行器；服务运行在容器中时，请将 `XXL_JOB_EXECUTOR_IP` 改为容器网络中可访问的地址。

| 服务 | 执行器 AppName | 默认端口 |
| --- | --- | --- |
| 会议室服务 | `meetingroom-executor` | `9996` |
| 用户服务 | `meeting-user-executor` | `9997` |
| 消息通告服务 | `meeting-notification-executor` | `9998` |

可通过 `XXL_JOB_ENABLED=false` 临时关闭某个服务的执行器，任务会停止注册，但业务接口不受影响。

## 已初始化任务

`mysql/01-schema.sql` 使用官方 XXL-JOB 3.4.2 表结构，`mysql/02-project-jobs.sql` 删除示例任务并初始化本项目的 9 个处理器。任务首次导入时为停止状态，需要在管理端为对应任务点击“启动”：

- `reservationExpiryCheck`
- `expiredReservationCleanup`
- `importantNewsExpiryUpdate`
- `reservationStateUpdate`
- `reservationReminderDispatch`
- `temporaryMemberCleanup`
- `notificationMessageDispatch`
- `notificationPolicyDispatch`
- `meetingEventOutboxDispatch`

初始化脚本只会在 MySQL 数据卷第一次创建时自动执行。已经存在 `xxl-job-mysql-data` 数据卷时，修改脚本不会覆盖已有数据，需要按变更内容在 XXL-JOB 数据库中手动执行。

官方项目文档和版本说明见 [XXL-JOB GitHub](https://github.com/xuxueli/xxl-job)。
