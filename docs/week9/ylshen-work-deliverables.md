# ylshen 项目工作整理

> 本文按实际完成的工作项整理，不按周次分组。课程第 9—11 周要求仅用作工程化异常处理、功能集成和测试闭环的检查参考。

## 工作范围

| 工作项 | 主要交付 | 代码/文档证据 |
| --- | --- | --- |
| 访问日志与审计 | 网关到后端的追踪、设备操作审计、Vector/ClickHouse 查询链路 | `49517cb`，`observability/`、`log-service/` |
| 设备安全与控制 | 设备认证、预约授权、ACK 校验、失败状态回读 | `49517cb`，`backend/`、`raspberry-pi/`、`frontend/` |
| 服务拆分与注册 | 独立用户管理服务、消息通告服务及 Nacos 配置 | `7e859d4`、`efc171d` |
| 定时任务治理 | 任务从 Spring 定时调度迁移至 XXL-JOB | `9c52578`，`deploy/xxl-job/` |
| 跨服务通知 | Outbox + RabbitMQ 传递公告、预约生命周期通知 | `fae6f7d`，`deploy/rabbitmq/` |
| 链路追踪 | 可选 SkyWalking OAP/UI 与 Java Agent 启动支持 | `9b9abf5`、`94fbbb2`，`deploy/skywalking/` |
| 缓存一致性 | Caffeine L1 + Redis L2，Canal Binlog 经 RabbitMQ 广播失效 | 当前工作区未提交改动，`cache-invalidation-service/`、`deploy/canal/` |

提交作者在仓库记录为 `SYL <1160579753@qq.com>`；此处按用户说明对应 ylshen 整理。缓存双层与 Canal 部分目前仍在工作区，尚未提交。

## 访问日志与审计

**解决的问题**：请求经过网关、业务服务和设备控制后，原有日志难以串联；发生超时、拒绝或设备失败时，定位链路不便。

**实现与异常处理**：

- 网关生成 `X-Trace-Id` 并向后端传递；去除客户端提供的身份和代理来源头，再从可信 Session 注入身份。
- 网关和会议室后端写 JSONL 访问事件，包含时间、服务、路径、方法、状态码、耗时、用户编号、IP 和 trace ID。
- 设备命令关联设备、会议室、命令 ID、结果和异常；不记录请求体、Session、设备 token 或人脸图像/特征。
- Vector 以文件检查点和磁盘缓冲采集到 ClickHouse；日志服务只允许授权管理员查询，限制时间范围、分页大小和查询时间。
- 采集暂停时由 Vector 保留缓冲，恢复后续传；日志查询不可用时明确返回错误，不生成模拟记录。

**测试证据**：访问日志、网关过滤、日志查询和 ClickHouse 查询有自动化测试。已有本机采集记录为 50 次请求产生 75 条入口及后端事件；停止采集 60 秒后恢复，积压日志补齐。对应实施记录见 `ENGINEERING_PROGRESS.md`。

**主要位置**：`backend/src/main/java/com/uestcfir/logging/`、`topbiz-gateway/src/main/java/com/uestcfir/gateway/TraceLoggingFilter.java`、`log-service/`、`observability/`。

## 设备安全与控制

**解决的问题**：避免客户端伪造身份控制设备，避免用户越过预约权限控制其他会议室，并正确处理设备断线、回执错误和超时。

**实现与异常处理**：

- 网关校验 Redis Session，剥离浏览器身份头后注入可信身份；日志查询额外验证 Session 并限制管理角色。
- 树莓派通过设备编号和独立 token 握手；设备必须预先登记并绑定会议室。
- 每次控制重新检查设备绑定和当前有效预约，权限不缓存；服务端时间使用 `Asia/Shanghai`。
- ACK 必须来自发送命令的同一连接且设备编号匹配。失败、设备离线、超时分别返回错误状态；前端不把失败显示为成功，而是回读设备状态。
- 硬件不支持的操作不伪装成已执行；树莓派直连控制入口关闭，避免绕过后端权限。

**角色与数据访问**：

| 角色 | 权限范围 |
| --- | --- |
| 普通用户/预约人 | 查询本人预约和消息；仅在有效预约时控制对应会议室设备 |
| 会议室管理员/审批人 | 管理会议室和审批预约；在授权范围内控制设备；授权管理员可以查询审计日志 |
| 设备端 | 只能用自己的设备身份上报数据并接收已授权命令 |

人脸识别数据仅供树莓派识别流程使用，不进入访问日志。日志不记录人脸图像/特征、Session 或设备 token；日志查询服务限制为管理员角色。

**测试证据**：`IotControlAuthorizationServiceTest`、`ReservationPermissionSqlTest`、`DeviceHandshakeTest`、`CommandAcknowledgementTest`、`GatewayFiltersTest`；树莓派 `test_command_security.py`；前端 `control-smoke.mjs` 覆盖错误回执、超时、不重复下发、状态回读及切换会议室权限。

**主要位置**：`backend/src/main/java/com/uestcfir/service/IotControlAuthorizationService.java`、`backend/src/main/java/com/uestcfir/config/DeviceHandshakeInterceptor.java`、`backend/src/main/java/com/uestcfir/websocket/IotDeviceWebSocketHandler.java`、`topbiz-gateway/src/main/java/com/uestcfir/gateway/SessionAuthenticationFilter.java`、`raspberry-pi/`、`frontend/tests/control-smoke.mjs`。

## 服务拆分与注册

**解决的问题**：用户和通知业务与会议室核心服务耦合，不利于独立维护、部署及明确数据归属。

**实现**：拆分 `user-service` 和 `notification-service`，保留会议室后端，通过内部 API/服务发现协作；新增 Nacos 部署配置和服务名配置。用户库、通知库与会议室库分开，通知服务继续通过 HTTP 为主页提供消息查询接口。

**测试/运行证据**：三个服务各自具备独立配置与构建入口；本地 URL 与 Nacos 服务名可通过环境配置切换。部署关系和端口见 `services/README.md`。运行中的多机 Nacos 环境验收需由部署环境提供证据。

**主要位置**：`user-service/`、`notification-service/`、`platform-services/`、`deploy/nacos/`、`services/README.md`。

## XXL-JOB 定时任务

**解决的问题**：定时任务分散在各应用内部，无法集中查看任务、调度记录和失败状态。

**实现与异常处理**：将会议室预约过期/清理、预约状态、重要信息、提醒，以及用户临时成员清理、通知派发任务改为 `@XxlJob`；增加三个独立执行器和 XXL-JOB Admin/MySQL 部署配置。处理异常时保留异常日志并向调度器抛出失败状态，由管理端查看执行结果和重试情况。

**验证与使用**：三个模块 Maven 打包通过；初始化任务默认为停止状态，需要在 XXL-JOB Admin 中启动。部署说明和增量 SQL 位于 `deploy/xxl-job/`。真实 Admin、Executor 联调取决于 Docker 服务运行状态。

## 跨服务通知事件

**需求与价值**：公告发布、预约提交/审批/拒绝/取消/过期和提醒要进入用户消息列表；避免会议室服务同步等待通知服务，并降低服务短暂不可用导致的漏通知。

**实现**：会议室服务与业务数据同事务写 `meeting_event_outbox`；XXL-JOB 投递 RabbitMQ；通知服务按 `eventId` 幂等消费并写入原有 `message` 表。消费异常回滚幂等记录，由监听器重试，耗尽后进入死信队列。主页仍通过通知服务 HTTP 查询消息。

**验收标准**：公告与预约生命周期事件能生成对应用户消息；重复投递不重复写入；故障恢复后可重新投递；主页可查询消息与未读状态。

**测试证据**：`MeetingEventConsumerTest` 两项通过，覆盖重复投递去重和消费失败回滚后重试。RabbitMQ、Outbox 调度、消息落库到主页读取的真实部署端到端联调仍需留存运行证据。

**方向备注**：该通知功能按自拟扩展方向整理，任务要求其他方向需教师确认；仓库没有教师确认记录，小组需确认后再据此申报功能扩展项。

## 链路追踪与部署支持

- 可选接入 SkyWalking OAP/UI 与 Java Agent；提供 Docker Compose、Agent 启动脚本和模块服务名配置。
- Agent 不随代码提交，未配置 Agent 时不影响普通启动。
- 文档位于 `deploy/skywalking/README.md`，可用于从网关到业务服务查看 trace。

## 缓存一致性改进（当前未提交）

**目标链路**：Caffeine 本机 L1 -> Redis 共享 L2；Canal 读取 MySQL Binlog，将表变更转成 RabbitMQ 缓存失效事件；每个业务实例使用自己的广播队列，收到事件后删除本机 Caffeine 和 Redis 条目。

**已实现部分**：

- `TwoLevelCache`、`TwoLevelCacheManager` 实现 Caffeine L1 + Redis L2；两级均配置过期时间，L1 最大容量可配置；缓存回源开启 `sync=true`。
- 会议室列表变更清理 `meetingroom:list`；IoT 表变更按 room/device 清理最新值，并清空近期列表。
- `cache-invalidation-service` 使用 Canal 1.1.8 客户端读取 `meetingroom.*` Binlog 并向 `cache.invalidation` RabbitMQ exchange 发布事件。
- 每个会议室服务实例创建独立临时队列，避免多实例竞争消费导致其他实例未清本地缓存。

**测试/限制**：后端打包、Canal 服务打包、`BusinessCacheTest` 6 项和 Canal Compose 配置检查通过。Canal/MySQL/RabbitMQ/至少两个业务实例尚未进行真实环境联调；当前实现还需补断线重连、Binlog 重放、发布确认和多实例失效的现场证据。代码仍处于未提交工作区。

**主要位置**：`backend/src/main/java/com/uestcfir/cache/`、`backend/src/main/java/com/uestcfir/config/CacheConfiguration.java`、`cache-invalidation-service/`、`deploy/canal/`。

## 综合测试与问题闭环

| 问题/异常 | 修复措施 | 验证方式 | 结果/剩余事项 |
| --- | --- | --- | --- |
| 浏览器伪造用户身份头 | 网关清除身份头，从 Redis Session 注入可信身份 | `GatewayFiltersTest`、会话过滤测试 | 已修复，有自动化测试 |
| 网关与后端日志难以关联 | 网关生成并传递 `X-Trace-Id` | 日志采集与 trace 查询 | 已实现并有采集记录 |
| 设备命令失败可能被误显示为成功 | 校验 ACK 来源和设备 ID，失败后回读 | ACK/超时和浏览器模拟测试 | 已修复，有自动化测试；真实硬件待验 |
| Redis 缓存故障影响业务 | 缓存读取失败回源数据库，写入/失效失败记录警告 | `BusinessCacheTest` | 6 项通过；新 Canal 广播链未现场验收 |
| RabbitMQ 通知重复消费或消费异常 | `eventId` 幂等，消费异常事务回滚 | `MeetingEventConsumerTest` | 2 项通过；部署端到端链路待验 |

构建与测试记录：backend、notification-service、cache-invalidation-service Maven 打包通过；`BusinessCacheTest` 6/6、`MeetingEventConsumerTest` 2/2。既有工程化验证还记录了网关/后端日志采集、Vector 中断补采、树莓派和浏览器模拟测试，详情见 `ENGINEERING_PROGRESS.md` 及各模块 `target/surefire-reports/`。

## 尚需补充的验收证据

- 真实设备动作、ACK、断线重连及页面状态逐项核对。
- 按部署网络探测确认后端端口无法绕过网关直接访问。
- Canal 真实连接 MySQL ROW Binlog；启动 RabbitMQ、Canal、失效服务及两个会议室实例，验证写入后两实例 Caffeine 与 Redis 均失效。
- 断开 Canal/RabbitMQ 后恢复，确认事件重放和缓存最终一致，记录日志及恢复时间。
- 公告/预约事件经 Outbox、RabbitMQ、通知消息表到主页查询的完整端到端验证。
- 补充通知扩展方向的教师确认记录。

## 复核入口

- 工程化实现记录：`ENGINEERING_PROGRESS.md`
- 服务拆分：`services/README.md`
- RabbitMQ 业务通知：`deploy/rabbitmq/README.md`
- XXL-JOB：`deploy/xxl-job/README.md`
- Canal 缓存失效：`deploy/canal/README.md`
- SkyWalking：`deploy/skywalking/README.md`

本文按工作项整理，不代表所有集成组件已完成部署验收。当前工作区有未提交代码改动，本文件也未提交到 Git。
