# 会议室传感器数据质量报告

## 数据说明

- 原始文件：`data/raw/原始数据样例.csv`
- 清洗结果：`data/processed/clean_sensor_data.csv`
- 数据性质：当前没有可导出的真实历史数据，因此本次使用模拟原始数据。
- 字段来源：本仓库树莓派遥测载荷、后端 `IotTelemetryRequest`、`IotSensorRecord` 和 `iot_sensor_record` 表结构。
- 模拟数据专门包含重复、缺失、越界值和命名不一致，用于验证清洗流程；不得描述为真实树莓派采集数据。

## 清洗前后对比

| 指标 | 数量 |
| --- | ---: |
| 原始记录 | 37 |
| 删除的重复记录 | 2 |
| 因时间或会议室编号无效而删除 | 2 |
| 清洗后记录 | 33 |
| 质量正常记录 | 18 |
| 质量异常或不完整记录 | 15 |

## 字段和格式统一

- 列名统一为小写 snake_case。
- 原始 `room_code` 统一命名为 `room_id`；数据库整数主键保留为 `room_db_id`。
- `room-14`、`room_14`、`14`、`Meeting Room 14` 等写法统一为 `meeting_room_14`。
- 时间统一为 `YYYY-MM-DD HH:MM:SS`，并按时间、会议室和设备升序排列。
- 原始 `presence` 统一为分析字段 `occupancy`，取值统一为 0 或 1。
- `opened`、`open` 统一为 `open`，`close`、`closed` 统一为 `closed`。
- `device_id`、`event_type` 和 `sensor_status` 统一为小写。

## 异常值检查

| 异常类型 | 发现数量 | 处理方式 |
| --- | ---: | --- |
| `missing_timestamp` | 1 | 时间为必填字段，记录不进入清洗结果 |
| `invalid_room_id` | 1 | 会议室编号无法识别，记录不进入清洗结果 |
| `room_db_id_mismatch` | 1 | 以 room_id 中的数字为准重新生成 room_db_id |
| `temperature_out_of_range` | 3 | 置为空值并标记 abnormal |
| `humidity_out_of_range` | 2 | 置为空值并标记 abnormal |
| `light_raw_out_of_range` | 1 | 置为空值并标记 abnormal |
| `people_count_out_of_range` | 1 | 置为空值并标记 abnormal |
| `invalid_occupancy` | 1 | 置为空值并标记 abnormal |
| `invalid_door_state` | 1 | 置为空值并标记 abnormal |
| `invalid_sensor_status` | 1 | 置为空值并标记 abnormal |

温度允许范围为 0 至 50 摄氏度，湿度允许范围为 0% 至 100%，`light_raw` 允许范围为 0 至 255，人数不得小于 0。烟雾值 650 和 950 分别用于模拟告警和严重告警，它们不是格式错误，因此予以保留。

## 缺失值统计

| 字段 | 原始缺失数 | 清洗后缺失数 |
| --- | ---: | ---: |
| `timestamp` | 1 | 0 |
| `room_id` | 0 | 0 |
| `room_db_id` | 0 | 0 |
| `device_id` | 0 | 0 |
| `event_type` | 0 | 0 |
| `temperature` | 1 | 4 |
| `humidity` | 2 | 4 |
| `light_raw` | 0 | 1 |
| `smoke_raw` | 0 | 0 |
| `occupancy` | 1 | 2 |
| `people_count` | 0 | 1 |
| `door_state` | 0 | 1 |
| `sensor_status` | 0 | 1 |

处理原则：时间和会议室编号是定位记录所必需的字段，缺失或非法时删除整条记录；温度、湿度、人员状态等传感器字段不凭空填充，保留为空并通过 `quality=abnormal` 和 `quality_issues` 说明原因。

## 清洗后会议室分布

| 会议室 | 记录数 |
| --- | ---: |
| `meeting_room_14` | 15 |
| `meeting_room_15` | 9 |
| `meeting_room_32` | 9 |

## 可复现方式

在仓库根目录执行：

```powershell
python -m pip install -r scripts/requirements.txt
python scripts/clean_data.py
```

脚本每次都从原始 CSV 重新生成清洗结果和本报告，不依赖上一次运行的输出。
