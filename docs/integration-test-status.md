# 中间件集成验证记录

日期：2026-11-09。Docker 已恢复，RabbitMQ、Canal、XXL-JOB Admin、Seata 均已实际启动。测试使用独立数据库和测试用户，结果区分实际链路与尚未实现的业务能力。

## 验证结果

| 项目 | 实际执行 | 结果与边界 |
| --- | --- | --- |
| 预约事件完整链路 | 真实 HTTP 创建预约、审批、拒绝、取消；Admin 触发过期处理、Outbox 和站内信派发 | `reservation.created/approved/rejected/cancelled/expired` 均进入 Outbox，最终 `PUBLISHED`，通知侧 `PROCESSED`，消息 `sent` |
| 公告完整链路 | 公告 API → 用户服务查询接收者 → Outbox → RabbitMQ → 通知库 | `announcement.published` 成功，覆盖两个合成接收者，不发送外部邮件/短信 |
| 通知幂等 | 同一 `eventId` 再投递两次 | 仍只有一条对应消息 |
| 消费死信 | 发布非法 JSON | listener 重试耗尽后 reject，DLQ 读到原消息，`x-death.reason=rejected` |
| 通知 HTTP 查询/已读 | 查询 `/internal/site-messages/user/990001`，再调用已读接口 | 实际返回消息；标记后未读数为 0；不代表原公告前端已接入站内信 |
| RabbitMQ 停机恢复 | 停 Rabbit，向测试 Outbox 写事件并触发；重启后再次触发 | 停机保留 `FAILED, attempts=1`；恢复后 `PUBLISHED, attempts=2`，消费、派发和 HTTP 查询成功 |
| XXL-JOB 成功回调 | Admin 手动触发全部 9 个项目 handler | 全部 `trigger_code=200, handle_code=200`；周期任务保持停用 |
| XXL-JOB 失败记录 | 在隔离任务库增加不存在 handler 的任务并触发 | `trigger_code=500`，Admin 保留失败；不代表所有异常/重试均已覆盖 |
| 两实例缓存失效 | 先预热两实例，再直接 UPDATE MySQL，观察 Redis 及 HTTP | 约 0.2 秒观察到 Redis 条目删除，两实例均返回新名称，未等 5 分钟 TTL |
| Canal 停机恢复 | 停 Canal、改记录、确认旧缓存、启 Canal | 两实例约 4 秒获得新值，日志有 disconnect/reconnect |
| Rabbit 重启缓存恢复 | 预热两实例、停 Rabbit、改 MySQL、启 Rabbit | 首轮发现旧实现恢复失败；修复后重跑约 3.5 秒两实例均返回新值 |
| Seata 客户端连接 | 两服务启用 Seata 2.0.0，连接真实 TC | 服务端确认用户/通知服务 TM、RM 注册成功；**仅连接层通过** |
| Seata 全局事务回滚 | 检查业务入口及 XID 传播 | **尚不能验收：没有 `@GlobalTransactional` 业务入口及跨服务事务闭环**，注册成功不等于回滚测试通过 |

耗时是本机单轮观察值，不是性能 SLA。读取与 Binlog 广播之间仍有最终一致性窗口，不宣称强一致或恰好一次投递。

## 测试隔离

| 组件 | 测试位置 |
| --- | --- |
| 会议室库 | `meetingroom_integration`，仅 schema、RBAC 基础数据及专用会议室 |
| 用户/通知库 | `meetingroom_users_it`、`meetingroom_notifications_it`，仅合成身份、站内信 |
| 调度库 | `xxl_job_integration`，9 个项目任务加 1 个故障任务，周期任务停用 |
| 业务实例 | `8284/8285`，执行器 `19996/19995` |
| 用户/通知实例 | `8281/8282`，执行器 `19997/19998` |
| Redis | 测试业务缓存和 Session DB 12；通知 DB 13 |
| Rabbit 事件 | `meeting.integration.events`、`meeting.integration.notification.events` |
| 缓存广播 | `cache.integration.invalidation`，两个实例独立临时队列 |
| Canal | `meetingroom_integration\..*`；本轮 TSDB 关闭 |

合成用户 ID 990001/990002，测试会议室 990001。外部邮件和短信关闭。测试初期默认业务队列绑定到测试交换机，已解除跨测试绑定；确认原队列 4 条均为本轮合成消息后清理，没有消费真实业务消息。

## 发现与修复

1. 缓存失效服务缺 ObjectMapper Bean：显式注册支持时间类型的 Bean，补上下文测试。
2. 非 Web 服务只有 daemon 消费线程，启动后退出：改非 daemon，并增加关闭清理。
3. Canal 镜像未应用过滤与 TSDB 参数：增加启动 wrapper 写入实际 instance.properties，订阅范围也可配置。
4. XXL-JOB IP 校验忽略 `host.docker.internal`，自动注册到外网 IP：三个服务显式设置完整 address。会议室实例已验证完整地址注册；用户/通知新配置编译通过，但现有测试进程仍用旧 jar，通过手动指定 address 完成回调测试。
5. 公告表/前端状态“未过期/已过期”与“已发布”判断冲突：统一状态并兼容旧输入，从可信会话补发布人姓名。
6. 公告 Outbox 异常被吞：改为抛出，让外层事务感知失败，补测试。
7. 关闭发现时仍强制 LoadBalanced，把固定 IP 当服务名：按配置分开直连/发现客户端，均有测试。
8. Rabbit 重启后临时队列丢事件导致旧缓存：发布等待确认/路由；本实例订阅恢复时清空缓存；实际故障已复测通过。

## 构建与证据

```powershell
mvn -pl backend '-Dtest=CacheSubscriberRecoveryTest,PlatformServiceClientConfigTest,AnnouncementEventTest,BusinessCacheTest,SystemAdminUserTest' package
mvn -f cache-invalidation-service/pom.xml package
mvn -f notification-service/pom.xml -Dtest=MeetingEventConsumerTest test
```

后端 18 项、失效服务上下文 1 项、通知消费者 2 项通过；用户/通知配置修改编译通过。本机忽略目录 `run-logs/integration/` 留存 `business-e2e.py`、`mq-recovery.py`、`cache-recovery.py` 与进程日志；含本地连接配置的生成文件不提交。测试报告在各模块 `target/surefire-reports/`。

## 环境及剩余事项

Docker 起初被 Secrets Engine socket 阻塞，用户处理后恢复。镜像由官方 Registry 经现有本地代理下载、校验后导入；archive 留在忽略目录 `run-logs/image-import/`，没有改 Docker 全局代理。

联调完成后，按用户要求已停止隔离应用实例，将完整链路接入原本地库并重启前后端；调度使用独立 `xxl_job_local` 库。Outbox、站内信派发和非删除类业务任务已启用，物理删除预约与临时成员清理仍停用，外部邮件/短信关闭。运行入口及限制见 [`services/local-runtime.md`](../services/local-runtime.md)。

Seata 如需全局事务验收，应先选定真正需要跨库原子写入的业务，实现全局事务入口及 XID 传播，再做第二分支失败和回滚验证；Outbox 通知采用最终一致性。其他未覆盖项包括长时间稳定性、性能压测、所有业务异常和真实多机网络。
