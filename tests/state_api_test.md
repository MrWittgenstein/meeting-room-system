# 统一会议室状态接口测试记录

## 测试范围

本次测试覆盖以下内容：

- 正常会议室编号能够返回统一状态对象；
- 业务编码 `meeting_room_14` 可以作为查询别名；
- 非法会议室编号返回明确错误信息；
- 不存在的接口路径返回明确错误信息；
- Java 后端能够把 `IotRoomStatus` 转换为统一对象；
- 缺失值、越界温度、非法门状态和设备错误能够触发 `quality=abnormal`。

## 模拟接口自动化测试

执行命令：

```powershell
python -X utf8 -m unittest -v tests\test_mock_state_api.py
```

执行结果：

```text
test_invalid_room_returns_clear_error ... ok
test_room_code_alias_is_supported ... ok
test_unknown_endpoint_returns_clear_error ... ok
test_valid_room_returns_unified_state ... ok

Ran 4 tests
OK
```

## 正常查询

请求：

```http
GET http://127.0.0.1:8000/iot/rooms/14/state
```

实际结果：HTTP 200。

```json
{
  "code": 1,
  "message": "success",
  "data": {
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
    "source": "mock",
    "quality": "normal",
    "quality_issues": []
  }
}
```

## 非法会议室查询

请求：

```http
GET http://127.0.0.1:8000/iot/rooms/99/state
```

实际结果：HTTP 404。

```json
{
  "code": 0,
  "message": "room not found: 99",
  "data": null
}
```

## Java 统一对象测试

执行命令：

```powershell
mvn -f backend\pom.xml -Dtest=UnifiedRoomStateVoTest test
```

执行结果：

```text
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 结论

统一状态对象、正常查询、异常查询、模拟数据标记和质量检查均达到本周任务的验收要求。模拟接口的 `source` 明确为 `mock`；真实 Spring Boot 接口返回 `source: sensor`。
