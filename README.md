# 树莓派智能会议室管理系统

基于树莓派、Spring Boot、Vue 3和WebSocket实现的智能会议室管理系统。

项目由Web前端、Spring Boot后端、MySQL/Redis数据服务以及树莓派采集与控制程序组成，支持会议室预约管理、环境监测、设备控制、人脸识别、门禁联动、实时状态展示和会议室3D场景等功能。

## 项目状态

本仓库由上一阶段的智能会议室项目整理而来，目前正在进行系统接手、运行环境恢复和功能升级。

当前主要目标：

- 恢复并验证原有系统
- 跑通树莓派、后端和前端之间的数据链路
- 规范项目配置和代码管理
- 清理敏感信息和生成文件
- 为后续功能升级和系统联调提供稳定基础

## 项目成员

| 姓名   | 主要职责           |
| ------ | ------------------ |
| 待填写 | 项目管理与系统集成 |
| 待填写 | 前端开发           |
| 待填写 | 后端开发           |
| 待填写 | 树莓派与传感器     |
| 待填写 | 数据库与测试       |

## 功能概览

### 会议室管理

- 会议室信息管理
- 会议室预约申请
- 预约审批
- 预约状态查询
- 会议室使用统计
- 管理员和普通用户权限区分

### 环境监测

- 温度采集
- 湿度采集
- 光照强度采集
- 烟雾状态采集
- 人体存在检测
- 超声波距离检测
- 实时数据上报与存储

### 设备控制

- 灯光控制
- 投影仪控制
- 窗帘控制
- 蜂鸣器控制
- 空调制冷、制热状态控制
- 加湿、除湿状态控制
- 舵机门禁控制
- 模拟设备状态控制

### 人脸识别

- 摄像头图像采集
- YuNet人脸检测
- SFace人脸特征识别
- 已知人员身份匹配
- 未知人员识别
- 人脸识别与门禁舵机联动

### Web与3D展示

- Vue管理页面
- 环境数据实时展示
- 历史数据图表
- 设备状态显示
- 会议室3D场景
- 3D对象与实时状态联动

## 系统架构

```text
DHT11 / PCF8591 / PIR / 摄像头 / 模拟数据
                         │
                         ▼
            树莓派Python采集与控制程序
                         │
               WebSocket实时通信
                         │
                         ▼
                Spring Boot后端
                    │         │
                    ▼         ▼
                  MySQL      Redis
                    │
                    ▼
                 REST API
                    │
                    ▼
                Vue 3前端
                    │
                    ▼
          管理页面、图表和3D场景
```

主要数据链路：

```text
传感器、摄像头或模拟数据
        ↓
raspberry-pi/sensor_server.py
        ↓ WebSocket
Spring Boot后端
        ↓
MySQL / Redis
        ↓
Vue页面及3D场景
```

设备控制链路：

```text
前端设备控制按钮
        ↓ HTTP
Spring Boot设备控制接口
        ↓ WebSocket
树莓派设备程序
        ↓
GPIO、舵机或模拟设备
        ↓
状态确认与实时数据回传
```

## 项目结构

```text
meeting-room-system/
├── backend/                       # Spring Boot后端
│   ├── src/
│   │   └── main/
│   │       ├── java/              # Java业务代码
│   │       └── resources/
│   │           ├── application.yml
│   │           ├── mapper/        # MyBatis映射文件
│   │           ├── sql/           # 数据库脚本
│   │           └── static/        # 静态资源和3D模型
│   └── pom.xml                    # Maven依赖配置
├── frontend/                      # Vue 3前端
│   ├── public/                    # 公共静态资源
│   ├── src/                       # 前端源码
│   ├── package.json
│   ├── package-lock.json
│   └── vite.config.js
├── raspberry-pi/                  # 树莓派端程序
│   ├── face_system/               # 人脸识别模块
│   ├── models/                    # ONNX模型
│   ├── scripts/                   # 安装脚本
│   ├── systemd/                   # 开机自启动服务
│   ├── config.example.json        # 配置模板
│   ├── requirements.txt           # Python依赖
│   ├── sensor_server.py           # 主程序
│   └── test_client.py             # WebSocket测试客户端
├── .gitattributes                 # Git LFS配置
├── .gitignore                     # Git忽略规则
└── README.md
```

## 技术栈

### 前端

- Vue 3
- Vite
- Vue Router
- Pinia
- Axios
- Element Plus
- ECharts
- Chart.js
- STOMP/WebSocket
- Sass

### 后端

- Java 17
- Spring Boot 3.5.6
- Spring Web
- Spring Security
- Spring WebSocket
- Spring Data Redis
- MyBatis
- MySQL
- Redisson
- Quartz
- JWT
- Maven

### 树莓派端

- Python 3
- WebSocket
- OpenCV
- OpenCV YuNet
- OpenCV SFace
- NumPy
- Picamera2
- RPi.GPIO
- Adafruit CircuitPython DHT
- SMBus/I2C

### 数据服务

- MySQL
- Redis

## 环境要求

### 后端环境

- JDK 17
- Maven
- MySQL
- Redis

### 前端环境

- Node.js
- npm

### 树莓派环境

- Raspberry Pi OS
- Python 3
- Python虚拟环境
- I2C和GPIO支持
- CSI或USB摄像头（使用人脸识别时）

## 获取项目

```bash
git clone <仓库地址>
cd meeting-room-system
```

项目使用Git LFS管理ONNX模型，首次克隆后执行：

```bash
git lfs install
git lfs pull
```

## 后端配置

后端配置文件位于：

```text
backend/src/main/resources/application.yml
```

仓库中不得保存真实密码、邮箱授权码、Token或OSS密钥。敏感配置应通过环境变量传入。

推荐使用以下环境变量：

| 环境变量                       | 说明                        |
| ------------------------------ | --------------------------- |
| `DB_URL`                       | MySQL连接地址               |
| `DB_USERNAME`                  | MySQL用户名                 |
| `DB_PASSWORD`                  | MySQL密码                   |
| `REDIS_PASSWORD`               | Redis密码，没有密码时可留空 |
| `MAIL_USERNAME`                | 邮箱账号                    |
| `MAIL_PASSWORD`                | 邮箱授权码                  |
| `ALIYUN_OSS_ACCESS_KEY_ID`     | OSS访问Key ID               |
| `ALIYUN_OSS_ACCESS_KEY_SECRET` | OSS访问Key Secret           |
| `JWT_SECRET`                   | JWT签名密钥                 |

请确保 `application.yml` 使用环境变量，而不是直接保存真实值，例如：

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/meetingroom}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}

  mail:
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}

aliyun:
  oss:
    access-key-id: ${ALIYUN_OSS_ACCESS_KEY_ID:}
    access-key-secret: ${ALIYUN_OSS_ACCESS_KEY_SECRET:}
```

具体配置层级以项目中的 `application.yml` 为准。

### Windows PowerShell环境变量示例

以下内容仅为示例，请替换成本机配置：

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/meetingroom?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME = "数据库用户名"
$env:DB_PASSWORD = "数据库密码"
$env:REDIS_PASSWORD = ""
$env:MAIL_USERNAME = ""
$env:MAIL_PASSWORD = ""
$env:ALIYUN_OSS_ACCESS_KEY_ID = ""
$env:ALIYUN_OSS_ACCESS_KEY_SECRET = ""
$env:JWT_SECRET = "本地开发JWT密钥"
```

环境变量只对当前PowerShell窗口有效，关闭窗口后需要重新设置。

## 数据库准备

创建数据库：

```sql
CREATE DATABASE meetingroom
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

数据库脚本位于：

```text
backend/src/main/resources/sql/
```

项目只应提交数据库结构和脱敏测试数据，不应提交包含真实用户密码、邮箱、电话或其他个人信息的完整数据库备份。

Redis默认地址：

```text
127.0.0.1:6379
```

MySQL默认地址：

```text
localhost:3306
```

## 启动后端

进入后端目录：

```bash
cd backend
```

检查Java和Maven：

```bash
java -version
mvn -version
```

启动后端：

```bash
mvn spring-boot:run
```

后端默认地址：

```text
http://localhost:8080
```

运行后端测试：

```bash
mvn test
```

构建后端：

```bash
mvn clean package
```

生成的 `target/` 和JAR文件不会提交到Git仓库。

## 启动前端

进入前端目录：

```bash
cd frontend
```

安装依赖：

```bash
npm ci
```

启动开发服务器：

```bash
npm run dev
```

终端会显示实际访问地址。

构建生产版本：

```bash
npm run build
```

检查前端代码：

```bash
npm run lint
```

生成的 `node_modules/` 和 `dist/` 不会提交到Git仓库。

## 树莓派端配置

详细说明见：

```text
raspberry-pi/README.md
```

进入树莓派程序目录：

```bash
cd raspberry-pi
```

创建Python虚拟环境：

```bash
python3 -m venv .venv --system-site-packages
```

激活虚拟环境：

```bash
source .venv/bin/activate
```

安装依赖：

```bash
python -m pip install --upgrade pip setuptools wheel
python -m pip install -r requirements.txt
```

从模板创建本地配置：

```bash
cp config.example.json config.json
```

`config.json` 是本地配置文件，不会提交到Git仓库。

主要配置示例：

```json
{
  "device": {
    "device_id": "raspi-01",
    "room_id": 14,
    "room_code": "meeting_room_14"
  },
  "server": {
    "host": "0.0.0.0",
    "port": 8775,
    "sample_interval_seconds": 5
  },
  "backend": {
    "enabled": true,
    "websocket_url": "ws://后端电脑IP:8080/iot/ws/device"
  }
}
```

请将“后端电脑IP”替换为运行Spring Boot后端的电脑IP地址。

## 启动树莓派程序

### 连接真实硬件

```bash
python sensor_server.py --config config.json
```

### 使用模拟数据

没有树莓派或传感器时，可以使用模拟模式：

```bash
python sensor_server.py --config config.json --demo
```

正常情况下可以看到类似输出：

```text
WebSocket server started at ws://0.0.0.0:8775
Spring backend connected
sample: temperature=25.0, humidity=50.0
```

### 测试树莓派WebSocket

另开一个终端：

```bash
cd raspberry-pi
source .venv/bin/activate
python test_client.py ws://localhost:8775 --count 3
```

从其他电脑测试时，将 `localhost` 替换为树莓派IP：

```bash
python test_client.py ws://树莓派IP:8775 --count 3
```

## 默认端口

| 服务              | 默认地址或端口                   |
| ----------------- | -------------------------------- |
| Spring Boot后端   | `http://localhost:8080`          |
| 树莓派WebSocket   | `ws://树莓派IP:8775`             |
| 设备接入WebSocket | `ws://后端IP:8080/iot/ws/device` |
| MySQL             | `localhost:3306`                 |
| Redis             | `localhost:6379`                 |
| Vue开发服务器     | 以Vite终端输出为准               |

## 主要接口

### 树莓派设备接入

```text
/iot/ws/device
```

树莓派通过该WebSocket接口向后端上报实时数据，并接收设备控制命令。

### 查询会议室状态

```text
GET /iot/rooms/{roomId}/status
```

### 下发设备命令

```text
POST /iot/devices/{deviceId}/commands
```

请求示例：

```json
{
  "command": "set_device_state",
  "target": "light",
  "value": true
}
```

开门命令示例：

```json
{
  "command": "open_door"
}
```

舵机角度命令示例：

```json
{
  "command": "set_servo_angle",
  "angle": 90
}
```

## 3D场景

会议室3D资源位于：

```text
backend/src/main/resources/static/assets/models/
```

3D场景用于显示会议室布局和设备状态，并可根据后端或WebSocket提供的实时数据更新显示效果。

修改3D资源时，应注意：

- 保持模型相对路径不变
- 不要将构建缓存提交到仓库
- 确保资源能通过后端静态资源路径访问
- 记录设备对象与后端状态字段之间的对应关系

## 人脸识别模型

模型文件位于：

```text
raspberry-pi/models/
```

主要模型：

```text
face_detection_yunet_2023mar.onnx
face_recognition_sface_2021dec.onnx
```

ONNX文件通过Git LFS管理。

首次克隆后如果模型文件不完整，执行：

```bash
git lfs pull
```

真实人脸照片和人脸特征数据库不会提交到仓库：

```text
raspberry-pi/known_faces/
raspberry-pi/data/face_db.npz
```

需要使用人脸识别功能时，应在本地按照授权范围建立测试人脸库。

## 配置与数据安全

禁止向仓库提交：

- 数据库真实密码
- 邮箱授权码
- API Token
- OSS访问密钥
- JWT生产密钥
- 树莓派实际配置文件
- 真实人脸照片
- 人脸特征数据库
- 包含用户个人信息的数据库备份
- `node_modules`
- Maven的 `target`
- Python虚拟环境
- 日志文件
- 构建产物
- 演示视频和原始压缩包

提交前建议执行：

```bash
git status
git diff --cached
```

并搜索敏感字段：

```bash
rg -n --hidden -g "!**/.git/**" -g "!**/node_modules/**" "(password|secret|token|access[-_.]?key)"
```

## Git开发流程

获取远程更新：

```bash
git pull
```

创建个人功能分支：

```bash
git switch -c feature/功能名称
```

提交修改：

```bash
git add .
git commit -m "feat: 简要说明本次修改"
```

推送分支：

```bash
git push -u origin feature/功能名称
```

推荐提交类型：

| 类型       | 说明                 |
| ---------- | -------------------- |
| `feat`     | 新增功能             |
| `fix`      | 修复问题             |
| `docs`     | 修改README或其他说明 |
| `refactor` | 重构代码             |
| `test`     | 增加或修改测试       |
| `build`    | 修改依赖或构建配置   |
| `chore`    | 其他维护工作         |

提交示例：

```text
feat: 增加会议室环境实时展示
fix: 修复树莓派断线后无法重连的问题
docs: 补充后端启动说明
refactor: 重构设备状态处理逻辑
test: 增加传感器模拟数据测试
```

## 常见问题

### 前端无法访问后端

检查：

- Spring Boot是否已启动
- 前端请求地址是否正确
- 前端和后端是否在同一网络
- 后端CORS配置是否允许当前前端地址
- 防火墙是否允许后端端口

### 树莓派无法连接后端

检查：

- `config.json` 中的后端IP是否正确
- 后端是否监听 `8080`
- 树莓派和后端电脑是否位于同一局域网
- `/iot/ws/device` 是否可以连接
- Windows防火墙是否放行对应端口

### 后端无法连接MySQL

检查：

- MySQL服务是否已启动
- `meetingroom` 数据库是否存在
- 数据库用户名和密码是否正确
- 环境变量是否在当前终端生效
- JDBC连接地址是否正确

### 后端无法连接Redis

检查：

- Redis服务是否启动
- Redis地址和端口是否正确
- Redis密码是否与配置一致

### ONNX模型没有正常下载

执行：

```bash
git lfs install
git lfs pull
git lfs ls-files
```

### 没有树莓派或传感器

使用模拟模式：

```bash
python sensor_server.py --config config.json --demo
```

## 后续计划

- 完成原有系统运行基线验证
- 统一传感器数据字段和时间戳格式
- 优化树莓派断线重连机制
- 完善Web页面实时状态展示
- 优化3D场景与设备状态联动
- 完善人脸识别业务流程
- 增加权限控制和安全检查
- 增加自动化测试
- 完善部署与运行说明

## 说明

本项目仅用于课程学习、系统开发和功能验证。

涉及摄像头、人脸照片、识别特征和用户个人数据时，应取得相关人员授权，并避免将真实数据提交到公共或未经授权访问的仓库。