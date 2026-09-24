# 工程化改进实施记录

日期：2026-09-23。依据上级目录《智能会议室系统工程化改进方案_五要素版.docx》。
实施目录为 `meeting-room-system-main`，其中已有 Session/RBAC、设备预约鉴权和 `topbiz-gateway`。
`user_management` 仅用于参考日志过滤器、Logback、Vector、ClickHouse 和日志查询服务的组织方式。
另两个 GitHub 副本未修改。

## 5.1 服务访问日志

- [x] 网关生成 `X-Trace-Id`，传递给业务服务，并在响应中返回唯一追踪头。
- [x] 每次 HTTP 访问记录 `event_id`、UTC 时间、路径、方法、可信用户编号、HTTP 状态、业务结果码和耗时。
- [x] 设备控制关联设备、会议室、指令编号及最终结果，并单独记录指令已下发事件。
- [x] 不记录请求体、查询参数、会话或设备认证头；去除原注册/登录日志中的完整对象输出，关闭 MyBatis 参数打印。
- [x] JSONL 按日期和大小滚动，默认保留 30 天、单文件 100 MB、归档容量上限 3 GB。
- [x] Vector 文件检查点、磁盘缓冲、批量采集；独立 `meetingroom_logs.access_events` 表。
- [x] 本机中断采集 60 秒后，恢复约 6 秒补齐 75 条事件；`FINAL` 查询验证 50 条网关记录和 25 条后端记录。
- [x] 日志查询服务提供 `POST /api/v1/logs/query`，重新校验 Redis 会话，仅允许会议室管理员、超级管理员读取。
- [x] 查询使用参数绑定、最大 31 天范围、最多 200 条/页、查询超时；返回检索耗时，并使用 `FINAL` 去重。
- [ ] L1 文档规定的五种场景各 10 次完整联调验收。当前已完成入口到 ClickHouse 的 50 请求采集验证和独立错误场景测试。
- [ ] L3 缩短时间周期后的自动归档清理实测。配置已支持覆盖，尚未作为验收通过项。

日志查询参数示例（UTC 时间必须按实际测试时间填写）：

```json
{
  "startTime": "2026-09-23T00:00:00Z",
  "endTime": "2026-09-24T00:00:00Z",
  "microservice": "meetingroom-main",
  "page": 1,
  "pageSize": 20
}
```

可选过滤字段：`traceId`、`userId`、`deviceId`、`commandId`、`statusCode`。
请求头为管理员登录得到的 `SessionId`。无存储连接时返回 503，不返回模拟日志。

## 5.2 精细化设备控制权限

- [x] 每次命令重新查询设备绑定和本人有效预约，不缓存控制许可。
- [x] 查询采用 `start_time <= 当前时间 AND end_time > 当前时间`，仅接受使用中/已通过预约。
- [x] 使用可注入的服务端 Clock，默认 `Asia/Shanghai`，日期和时间来自同一时刻。
- [x] 管理员可以全局控制，但设备必须已登记且绑定存在的会议室。
- [x] 前端切换会议室重新读取权限；环境查询跟随真实 roomId，忽略旧会议室的迟到响应。
- [x] 灯光、投影等使用 `set_device_state`；制冷、制热、除湿使用 `set_climate_mode`，切换时先关闭先前模式。
- [x] 没有对应硬件能力的温度、风速、风向设置禁用；设备无输出不能伪装成执行成功。
- [x] `COMMAND_ACK` 必须来自指令发送到的同一连接，设备编号必须匹配。
- [x] 仅 `success` 为成功；错误/noop 返回 502，离线 503，回执超时 504。前端回滚并重新查询状态。
- [x] 关闭树莓派直连接口的控制入口，避免绕过后端预约权限。
- [ ] 真实硬件动作、回执和页面状态逐项对照；模拟和浏览器桩测试不替代真实设备验收。

设备凭据不再有默认通用密码。单设备 `raspi-01` 可通过 `IOT_DEVICE_TOKEN` 设置至少 32 字符随机凭据，后端和树莓派配置相同值。
多设备使用外部配置 `iot.device-auth.tokens.<deviceId>`，每台设备独立分配；不要把真实凭据提交到仓库。
设备连接前需在 `iot_device` 中登记并绑定已存在的会议室；遥测不能自动创建或改变绑定。
树莓派通过 `X-Device-Id`、`X-Device-Token` 请求头认证，连接地址为 `ws://网关地址:8080/iot/ws/device`；跨不可信网络部署时使用 TLS。

## 5.3 统一网关

- [x] 保留原业务接口路径，增加日志服务路由和设备 WebSocket 专用路由。
- [x] 清除客户端身份/代理来源头，校验 Redis Session，修复成功请求完成后误返回 401 的响应式分支。
- [x] 公共路径按完整路径或目录边界匹配，避免 `/user/login-admin` 之类路径误放行。
- [x] 登录、查询、控制分别限流，使用单进程令牌桶；默认分别为 5、20、5 次/秒，突发容量 5。
- [x] 返回明确的 401、403、429、503、504；限流含 Retry-After；连接超时 2 秒、响应超时 8 秒；没有设备动作自动重试。
- [x] 业务和日志服务默认绑定 `127.0.0.1`。网关暴露入口，前端默认仍请求 8080。
- [ ] 部署网络外部探测确认不能直接访问业务端口。
- [ ] 真设备连续运行 30 分钟、20 条受支持指令、断网恢复后 30 秒内重连的现场验收。

限流环境变量：`GATEWAY_LOGIN_RPS`、`GATEWAY_QUERY_RPS`、`GATEWAY_CONTROL_RPS`、`GATEWAY_BURST`。
当前适用于单网关实例。多实例部署前需改为共享 Redis 限流；代理部署前需明确可信来源，当前直接使用连接的对端地址。
跨机器部署需显式设置绑定地址，并通过内网/防火墙保护 8081、8083、Redis、ClickHouse。

## 启动和验证

从本目录构建。Windows 上打包前停止占用 JAR 的本项目进程，避免重打包文件被锁定。

```powershell
mvn package '-Dtest=AccessLogFilterTest,IotControlAuthorizationServiceTest,ReservationPermissionSqlTest,CommandAcknowledgementTest,DeviceHandshakeTest,GatewayFiltersTest,TopbizGatewayApplicationTests,LogServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false'
```

主业务原有的全上下文测试会连接本地 MySQL/Redis，运行前应准备独立测试库；上述选择集的业务测试使用 Mock 或独立 H2。
测试报告位于各模块的 `target/surefire-reports`。

三个服务分别启动：

```powershell
java -jar meetingroom-main/target/meetroom_management-0.0.1-SNAPSHOT.jar
java -jar topbiz-gateway/target/topbiz-gateway-1.0.0-SNAPSHOT.jar
java -jar log-service/target/log-service-1.0.0-SNAPSHOT.jar
```

ClickHouse 执行 `observability/access_events.sql`。本机现有 WSL 安装可使用 `observability/start-clickhouse-wsl.sh`，新数据目录为 WSL 下 `~/meetingroom-clickhouse`。
Vector 从本目录启动，保留 `run-logs/vector-data`，不能在重启时删除检查点或缓冲：

```powershell
powershell -File observability/run-vector.ps1 -VectorExe 'Vector可执行文件的绝对路径'
python observability/smoke.py
mvn -pl log-service -Dtest=ClickHouseQueryIT test
```

`smoke.py` 只读取公开 API 文档和未登录用户接口，不创建预约、不发送设备动作。检查 50 条入口日志、25 条后端日志和追踪关联。
补采测试：先停止本项目 Vector，再运行 `python observability/smoke.py 'Vector可执行文件路径'`，脚本等待 60 秒后恢复采集，并最多等待 5 分钟补齐。
证据分别写入 `run-logs/acceptance-smoke.json` 和 `run-logs/acceptance-recovery.json`。

归档测试可在独立日志目录覆盖 `ACCESS_LOG_DATE_PATTERN=yyyy-MM-dd_HH-mm-ss`、`ACCESS_LOG_HISTORY=10`、`ACCESS_LOG_MAX_FILE_SIZE=10KB`。
`ACCESS_LOG_TOTAL_SIZE` 控制容量上限；恢复正式配置时移除这些测试环境变量。

前端在 Node 22 下构建通过，本机 Node 24.11.1 构建发生原生进程异常退出；可使用：

```powershell
cd frontend
npm exec --yes --package=node@22 -- node node_modules/vite/bin/vite.js build
npm run dev
```

浏览器回归脚本：`frontend/tests/control-smoke.mjs`，参数为已安装 Playwright 的 `index.mjs` 路径，使用本机 Edge 和模拟接口。
覆盖五次错误回执、五次超时、每次只发送一次、失败后重新读取状态、切换会议室后禁用控制；截图写入 `run-logs`。
树莓派端：在 `raspberry-pi` 中执行 `python -m unittest test_command_security -v`。

本机测试证据只能证明对应测试场景。完整 L1/L3、真实硬件和部署网络验收仍需按原文档逐项补齐。

## 本轮验证结果

| 验证 | 结果与范围 |
| --- | --- |
| Maven 定向测试和三个模块打包 | 通过，28 个测试；命令见上文 |
| ClickHouse JDBC 集成查询 | 通过，1 个测试，读取实际采集数据 |
| 树莓派 Python 测试 | 4 项通过，包括直连拒绝、硬件失败状态不变、制冷制热互斥 |
| 浏览器模拟接口回归 | 5 次错误回执、5 次超时、无重复下发、重读状态、会议室切换权限均通过 |
| 正常采集 | 50 次请求，75 条事件，约 14 秒内完成采集 |
| 中断补采 | 停止 60 秒，约 66 秒时全部入库；停止期间只产生请求，不执行硬件动作 |
| 前端生产构建 | Node 22 通过；仍有原有大包体积警告 |

复现证据保留在 `run-logs` 和各模块测试报告中，生成文件已加入忽略规则。
