# 本地体验

安装并启动 Docker Desktop，使用 Linux 容器。在仓库根目录运行：

```powershell
docker compose -f compose.demo.yml up --build -d
```

打开 [会议室体验](http://127.0.0.1:5173/)。首次需要下载镜像和构建依赖。若本机的 5173 已占用，启动前在 PowerShell 执行 `$env:DEMO_PORT = '5175'`，然后打开 `http://127.0.0.1:5175/`。

| 角色 | 邮箱 |
| --- | --- |
| 普通用户 | `user@example.test` |
| 会议室管理员 | `manager@example.test` |
| 系统管理员 | `admin@example.test` |

三个合成账号共用体验密码 `DemoRoom2026!`。选择邮箱密码登录。

验收时，用普通用户预约明天的一号会议室，再用会议室管理员审批。回到普通用户查看审批状态，并尝试提交同一房间的重叠时段。系统管理员可打开用户管理。

体验环境只监听本机。其独立数据卷首次启动时自动创建两间会议室和三个测试账号，后续启动沿用预约数据。真机设备、邮件与 OSS、站内信投递、日志查询和中间件联调使用 [完整运行说明](../../services/local-runtime.md)。本体验配置仅供本地试用。

```powershell
docker compose -f compose.demo.yml ps
docker compose -f compose.demo.yml logs --tail 100 api db
docker compose -f compose.demo.yml down
```

若要恢复初始体验数据，执行 `docker compose -f compose.demo.yml down -v` 后重新启动。该命令会删除本体验环境的预约和用户改动。
