# 统一会议室状态接口说明

## 接口选择

项目保留现有接口：

```text
GET /iot/rooms/{roomId}/status
```

该接口继续返回后端原有的嵌套遥测结构，避免破坏现有前端。

本周新增统一状态接口：

```text
GET /iot/rooms/{roomId}/state
```

`roomId` 使用数据库整数主键。仓库中已经使用的示例编号为 `14`、`15` 和 `32`。

## 真实后端请求

```http
GET /iot/rooms/14/state HTTP/1.1
Host: localhost:8080
Authorization: Bearer <登录令牌>
```

真实接口需要具备 `IOT_READ` 权限。它从 `iot_room_status` 读取最新记录，并通过 `UnifiedRoomStateVo` 转换成统一对象。

### 成功响应

```json
{
  "code": 1,
  "message": "success",
  "data": {
    "timestamp": "2026-09-20T10:30:00",
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
}
```

### 无效会议室

真实后端沿用项目现有 `Result` 约定，业务错误返回 `code: 0`，并在 `message` 中说明会议室不存在或暂无状态数据：

```json
{
  "code": 0,
  "message": "meeting room does not exist, roomId=99",
  "data": null
}
```

## 本地模拟接口

没有 MySQL、Redis、登录令牌或树莓派时，可以运行：

```powershell
python -X utf8 mock\mock_state_api.py
```

服务地址：

```text
http://127.0.0.1:8000
```

正常查询：

```powershell
curl.exe http://127.0.0.1:8000/iot/rooms/14/state
```

也支持业务编码：

```powershell
curl.exe http://127.0.0.1:8000/iot/rooms/meeting_room_14/state
```

异常查询：

```powershell
curl.exe -i http://127.0.0.1:8000/iot/rooms/99/state
```

模拟接口对不存在的会议室返回 HTTP 404：

```json
{
  "code": 0,
  "message": "room not found: 99",
  "data": null
}
```

模拟接口读取 `mock/state.json`，其中固定使用 `source: mock`。该值用于明确说明数据并非真实传感器实时上报。

## 字段缺失和非法数据处理

- 后端转换时不伪造温湿度或人员状态。
- 缺失值保留为 `null`，同时令 `quality=abnormal`。
- 具体原因写入 `quality_issues`，例如 `missing_humidity`。
- 越界温度、湿度和非法门状态同样标记为异常。
- 消费端不得把 `null` 自动解释为 0、无人或设备关闭。

## 测试命令

模拟接口自动化测试：

```powershell
python -X utf8 -m unittest -v tests\test_mock_state_api.py
```

后端统一对象单元测试：

```powershell
mvn -f backend\pom.xml -Dtest=UnifiedRoomStateVoTest test
```
