# 统一会议室状态对象

## 目的

本对象是 Web、Spring Boot 后端、树莓派和 3D 场景之间的统一数据约定。树莓派和数据库中的内部字段先由后端转换为该对象，前端和 3D 端只依赖本对象，不再分别猜测各模块的字段名称。

真实后端接口返回 `source: sensor`，本地模拟接口返回 `source: mock`。模拟数据不得描述为真实树莓派实时数据。

## 完整示例

```json
{
  "timestamp": "2026-09-20T10:30:00+08:00",
  "room_id": "meeting_room_14",
  "room_db_id": 14,
  "device_id": "raspi-01",
  "occupancy": true,
  "people_count": 3,
  "temperature": 24.6,
  "humidity": 48.0,
  "door_status": "closed",
  "devices": {
    "light_on": true,
    "projector_on": false,
    "cooling": false,
    "heating": false,
    "humidifier": false,
    "dehumidifier": false,
    "curtain_open": false,
    "buzzer_on": false,
    "auto_mode": true,
    "alarm": false
  },
  "sensor_status": "ok",
  "source": "sensor",
  "quality": "normal",
  "quality_issues": []
}
```

## 顶层字段

| 字段 | 类型 | 必填 | 数据来源 | 异常处理 | Web 和 3D 用途 |
| --- | --- | --- | --- | --- | --- |
| `timestamp` | ISO 8601 字符串 | 是 | `last_reported_at` 或树莓派 `timestamp` | 缺失时 `quality=abnormal` | 显示更新时间、判断状态是否过期 |
| `room_id` | 字符串 | 是 | `room_code`，缺失时由数据库主键生成 | 统一为 `meeting_room_XX` | 页面和 3D 场景的稳定业务标识 |
| `room_db_id` | 整数 | 是 | 数据库 `meetingroom.room_id` | 缺失时标记异常 | 调用现有后端接口、关联预约数据 |
| `device_id` | 字符串 | 是 | 树莓派配置或 `iot_device.device_id` | 缺失时标记异常 | 设备诊断和状态追踪 |
| `occupancy` | 布尔值 | 是 | 依次取 `presence`、`motion_detected`、`person_near` | 全部缺失时为 `null` 并标记异常 | 人员状态图标、3D 人员提示 |
| `people_count` | 整数或 `null` | 否 | 人脸识别人数 | 负数不得返回；未知时为 `null` | 显示人数、判断容量风险 |
| `temperature` | 数值或 `null` | 否 | 温度传感器 | 不在 0 至 50 时标记异常 | 环境面板、空调联动 |
| `humidity` | 数值或 `null` | 否 | 湿度传感器 | 不在 0 至 100 时标记异常 | 环境面板、加湿除湿联动 |
| `door_status` | 枚举或 `null` | 是 | `door_state`，缺失时参考 `door_open` | 只允许 `open`、`closed`、`opening`、`closing` | 门图标和 3D 门动画 |
| `devices` | 对象 | 是 | 后端 `iot_room_status` 设备字段 | 单项未知时允许为 `null` | 控制面板与 3D 设备表现 |
| `sensor_status` | 字符串 | 是 | 树莓派 `system.sensor_status` | 缺失时统一为 `unknown` | 设备在线和告警提示 |
| `source` | 枚举 | 是 | 接口实现方填写 | 只允许 `sensor` 或 `mock` | 明确区分真实数据和模拟数据 |
| `quality` | 枚举 | 是 | 根据完整性和范围检查生成 | `normal` 或 `abnormal` | 决定是否展示告警样式 |
| `quality_issues` | 字符串数组 | 是 | 质量检查生成 | 无问题时为空数组 | 调试、日志和异常提示 |

## devices 字段

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| `light_on` | 布尔值或 `null` | 灯光是否开启 |
| `projector_on` | 布尔值或 `null` | 投影仪是否开启 |
| `cooling` | 布尔值或 `null` | 制冷状态 |
| `heating` | 布尔值或 `null` | 制热状态 |
| `humidifier` | 布尔值或 `null` | 加湿器状态 |
| `dehumidifier` | 布尔值或 `null` | 除湿器状态 |
| `curtain_open` | 布尔值或 `null` | 窗帘是否打开 |
| `buzzer_on` | 布尔值或 `null` | 蜂鸣器是否开启 |
| `auto_mode` | 布尔值或 `null` | 是否启用自动控制 |
| `alarm` | 布尔值或 `null` | 是否处于告警状态 |

## 现有字段到统一字段的映射

| 模块现有字段 | 统一字段 | 说明 |
| --- | --- | --- |
| 树莓派 `room_id`、后端 `roomCode` | `room_id` | 统一使用业务编码，例如 `meeting_room_14` |
| 树莓派 `roomId`、后端 `roomId` | `room_db_id` | 明确表示数据库整数主键 |
| 树莓派 `presence.presence`、`pir`，后端 `presence` | `occupancy` | 对外只保留“是否有人”的统一含义 |
| 树莓派 `access_control.door_state`、后端 `doorState`/`doorOpen` | `door_status` | 对外统一为小写状态字符串 |
| 后端 `lightOn` | `devices.light_on` | JSON 使用 snake_case |
| 后端 `projector` | `devices.projector_on` | 名称明确表示布尔开关状态 |
| 树莓派 `system.sensor_status`、后端 `sensorStatus` | `sensor_status` | 对外统一使用 snake_case |

## 质量规则

- 缺少时间、会议室编号、设备编号、人员状态、温度、湿度或门状态时，`quality` 为 `abnormal`。
- 温度不在 0 至 50、湿度不在 0 至 100、门状态不在规定枚举中时，`quality` 为 `abnormal`。
- `sensor_status` 不是 `ok`，或者设备上报了错误信息时，`quality` 为 `abnormal`。
- 具体问题必须写入 `quality_issues`，不能只给出一个无法解释的异常标志。

## 各模块使用约定

- 树莓派继续按现有遥测结构上传，不直接承担统一对象的转换工作。
- Spring Boot 从 `IotRoomStatus` 生成统一对象，真实接口固定填写 `source: sensor`。
- Web 端优先使用 `occupancy`、`temperature`、`humidity`、`door_status` 和 `devices`，并在 `quality=abnormal` 时展示数据异常提示。
- 3D 端以 `room_id` 绑定场景，根据 `occupancy`、`door_status` 和 `devices` 驱动人员、门、灯光、投影和空调效果。
