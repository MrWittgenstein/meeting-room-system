# 树莓派传感器 WebSocket 服务

本目录是树莓派端代码。它负责读取 GPIO / I2C 传感器数据，并同时支持两条链路：本机开启 `ws://树莓派IP:8775` 给第 12 章旧前端直连；主动连接 Spring Boot 后端 `/iot/ws/device`，把统一 JSON 上报入库并联动当前管理系统前端。

## 1. 项目文件

```text
raspberry_pi_server/
  sensor_server.py                 # 主程序：读取传感器并推送 WebSocket 数据
  config.example.json              # 配置模板
  requirements.txt                 # Python 依赖
  test_client.py                   # WebSocket 测试客户端
  face_system/                     # OpenCV YuNet + SFace 人脸识别代码
  models/                          # YuNet/SFace ONNX 模型
  data/face_db.npz                 # 已有人脸特征库
  known_faces/                     # 已有人脸照片备份
  scripts/install_on_pi.sh         # 树莓派一键安装脚本
  systemd/smart-room-sensor.service# 开机自启动服务模板
```

## 2. 硬件连接

基础部分至少连接 DHT11。

| 模块 | 连接方式 |
| --- | --- |
| DHT11 VCC | 树莓派 3.3V |
| DHT11 GND | 树莓派 GND |
| DHT11 DATA | GPIO17 |
| PCF8591 VCC | 树莓派 3.3V |
| PCF8591 GND | 树莓派 GND |
| PCF8591 SDA | GPIO2 / SDA |
| PCF8591 SCL | GPIO3 / SCL |
| 光敏电阻 | PCF8591 AIN0 |
| MQ-2 烟雾传感器 | PCF8591 AIN1 |
| HC-SR501 OUT | GPIO27 |

可选扩展：

| 模块 | 连接方式 |
| --- | --- |
| HC-SR04 TRIG | GPIO23 |
| HC-SR04 ECHO | 通过电阻分压后接 GPIO24 |
| SG90 SIGNAL | GPIO18 |
| CSI 摄像头 | 树莓派 Camera/CSI 排线接口 |
| USB 摄像头（备用） | USB 接口 |

注意：HC-SR04 的 ECHO 是 5V，必须分压到 3.3V 再接树莓派 GPIO。

## 3. 复制项目到树莓派

建议把本目录复制到树莓派：

```bash
ssh group1@树莓派IP "mkdir -p /home/group1"
scp -r raspberry_pi_server group1@树莓派IP:/home/group1/
```

也可以用 U 盘或 VS Code Remote SSH 复制。

进入树莓派：

```bash
ssh group1@树莓派IP
cd /home/group1/raspberry_pi_server
```

## 4. 安装依赖

推荐运行安装脚本：

```bash
chmod +x scripts/install_on_pi.sh
./scripts/install_on_pi.sh
```

脚本会安装：

- `python3-pip`
- `python3-venv`
- `python3-dev`
- `build-essential`
- `i2c-tools`
- `python3-picamera2`
- Python 包：`websockets`、`adafruit-circuitpython-dht`、`RPi.GPIO`、`smbus2`、`numpy`、`opencv-contrib-python`

如果不用脚本，也可以手动安装：

```bash
sudo apt-get update
sudo apt-get install -y python3-pip python3-venv python3-dev build-essential i2c-tools libgpiod2 libgl1 libglib2.0-0 python3-picamera2
sudo raspi-config nonint do_i2c 0

python3 -m venv .venv --system-site-packages
. .venv/bin/activate
python -m pip install --upgrade pip setuptools wheel
python -m pip install -r requirements.txt
cp config.example.json config.json
```

如果安装时出现 `Failed building wheel for Adafruit-DHT`，说明装到了旧版 `Adafruit-DHT` 包。当前项目已经改用第 6 章实验使用过的 `adafruit-circuitpython-dht`，不需要安装 `Adafruit-DHT`。建议删除旧虚拟环境后重装：

```bash
rm -rf .venv
python3 -m venv .venv --system-site-packages
. .venv/bin/activate
python -m pip install --upgrade pip setuptools wheel
python -m pip install -r requirements.txt
```

安装完成后建议重启一次，使 I2C 配置生效：

```bash
sudo reboot
```

## 5. 修改配置

编辑 `config.json`：

```bash
nano config.json
```

常用配置：

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
  },
  "dht": {
    "enabled": true,
    "gpio_pin": 17
  },
  "distance": {
    "enabled": true,
    "auto_open_door": false
  },
  "servo": {
    "enabled": true,
    "gpio_pin": 18
  },
  "face_recognition": {
    "enabled": true,
    "camera_source": "picamera2",
    "camera_index": 0,
    "preview_enabled": true,
    "preview_window_name": "Smart Room Camera",
    "preview_fps": 15,
    "authorized_names": [],
    "open_door_on_authorized": true
  }
}
```

默认配置已经开启主要传感/感知相关模块和人脸识别舵机门禁：`dht`、`pcf8591`、`light`、`smoke`、`pir`、`distance`、`face_recognition`、`servo`。超声波测距默认开启并会上报距离，但 `distance.auto_open_door` 默认关闭，避免未做人脸识别时仅凭距离自动开门；`outputs` 里的真实输出设备也默认关闭，需要控制继电器、LED、蜂鸣器等设备时，再把 `outputs` 中对应模块的 `enabled` 改成 `true`。

例如启用光照和烟雾传感器：

```json
{
  "pcf8591": {
    "enabled": true
  },
  "light": {
    "enabled": true
  },
  "smoke": {
    "enabled": true
  }
}
```

例如启用 CSI 摄像头人脸识别，并允许任意已知人脸开门：

```json
{
  "face_recognition": {
    "enabled": true,
    "camera_source": "picamera2",
    "camera_index": 0,
    "preview_enabled": true,
    "preview_window_name": "Smart Room Camera",
    "preview_fps": 15,
    "process_width": 480,
    "database": "data/face_db.npz",
    "yunet_model": "models/face_detection_yunet_2023mar.onnx",
    "sface_model": "models/face_recognition_sface_2021dec.onnx",
    "authorized_names": [],
    "open_door_on_authorized": true,
    "door_open_seconds": 20
  },
  "servo": {
    "enabled": true,
    "gpio_pin": 18,
    "pwm_hz": 50,
    "closed_angle": 0,
    "open_angle": 90
  }
}
```

CSI 摄像头读取方式与第 4 章实验一致：程序在树莓派本机通过 `Picamera2` 打开排线摄像头，调用 `capture_array()` 获取 RGB 图像，再转换为 OpenCV 可识别的 BGR 帧进行 YuNet + SFace 人脸识别。`camera_index` 只在 USB 摄像头模式下有意义；如果改用 USB 摄像头，把 `camera_source` 改成 `"opencv"`，再按实际设备编号设置 `camera_index`。

`preview_enabled: true` 表示人脸识别服务启动后，会在树莓派桌面上打开名为 `Smart Room Camera` 的摄像头预览窗口；退出服务时会自动关闭窗口。需要在树莓派桌面环境的终端里运行服务才能看到窗口，如果只通过纯 SSH 或 systemd 后台运行且没有桌面显示环境，程序会继续识别和上报，但不会弹出窗口。

如果只允许指定成员开门，把 `authorized_names` 改成名单，例如：

```json
"authorized_names": ["liuziwei", "shenyulong"]
```

## 6. 检查硬件

查看树莓派 IP：

```bash
hostname -I
```

检查 CSI 摄像头：

```bash
rpicam-hello
# 如果系统较旧，也可能是：
libcamera-hello
```

检查 I2C 设备，PCF8591 常见地址是 `0x48`：

```bash
i2cdetect -y 1
```

检查 GPIO 工具：

```bash
gpio readall
```

如果没有 `gpio` 命令，可以安装：

```bash
sudo apt-get install -y wiringpi
```

## 7. 运行服务器

前台运行，方便看日志：

```bash
cd /home/group1/raspberry_pi_server
. .venv/bin/activate
python sensor_server.py --config config.json
```

正常会看到类似：

```text
WebSocket server started at ws://0.0.0.0:8775
Spring backend connected: {"type":"CONNECTED", ...}
sample: {"temperature": 25.0, "humidity": 50.0, ...}
```

无硬件调试时可以启用模拟数据：

```bash
python sensor_server.py --config config.json --demo
```

## 8. 测试 WebSocket

在树莓派本机测试：

```bash
. .venv/bin/activate
python test_client.py ws://localhost:8775 --count 3
```

在电脑上测试，把 IP 换成树莓派 IP：

```bash
python test_client.py ws://树莓派IP:8775 --count 3
```

本机 `8775` 返回给旧前端的数据格式：

```json
{
  "current": {
    "temperature": 25.0,
    "humidity": 50.0,
    "light": 200,
    "light_raw": 200,
    "smoke": 0,
    "presence": false,
    "no_presence_duration": 0,
    "timestamp": "2026-06-26 15:00:00"
  },
  "history": []
}
```

旧前端只需要解析 `current` 实时控制 3D 模型，用 `history` 绘制历史曲线。

光照字段中，`light_raw` 是 PCF8591 读取到的 0-255 原始值，`light` 是按光敏电阻近似公式换算后的整数 lx 值。默认参考点是 `light_raw=200` 对应约 `200 lx`。

同时程序会主动向 Spring Boot 后端发送统一格式：

```json
{
  "device_id": "raspi-01",
  "current": {
    "room_id": "meeting_room_14",
    "roomId": 14,
    "event_type": "telemetry",
    "timestamp": "2026-06-26 15:00:00",
    "environment": {
      "temperature": 25.0,
      "humidity": 50.0,
      "light": 200,
      "light_raw": 200,
      "smoke": 20,
      "smoke_raw": 20,
      "smoke_level": "normal"
    },
    "presence": {
      "pir": false,
      "presence": false,
      "distance": 120.0,
      "person_near": false
    },
    "access_control": {
      "face_detected": false,
      "face_count": 0,
      "recognized_names": [],
      "unknown_face_count": 0,
      "face_best_match": null,
      "face_best_score": null,
      "door_state": "closed",
      "authorized": false
    },
    "devices": {
      "cooling": false,
      "heating": false,
      "humidifier": false,
      "dehumidifier": false,
      "light_on": true,
      "curtain_open": true,
      "buzzer_on": false,
      "alarm": false
    },
    "system": {
      "sensor_status": "ok",
      "error": null
    }
  },
  "history": []
}
```

`room_id` 是业务编码，`roomId` 必须对应 Spring Boot 数据库 `meetingroom.room_id`。当前管理系统里已验证可用的示例 ID 是 `14`、`15`、`32`。

## 9. 连接前端

在 `smart_room.html` 中把 WebSocket 地址改成树莓派 IP：

```javascript
ws = new WebSocket('ws://树莓派IP:8775');
```

然后启动前端页面：

```bash
cd 前端目录
python3 -m http.server 8080
```

浏览器访问：

```text
http://树莓派IP:8080/smart_room.html
```

如果前端在电脑上运行，只要电脑和树莓派在同一个局域网，也可以直接访问 `ws://树莓派IP:8775`。

## 10. 设置开机自启动

确认项目路径是 `/home/group1/raspberry_pi_server`，并确认 `systemd/smart-room-sensor.service` 里的运行用户是 `group1`，然后执行：

```bash
sudo cp systemd/smart-room-sensor.service /etc/systemd/system/smart-room-sensor.service
sudo systemctl daemon-reload
sudo systemctl enable smart-room-sensor
sudo systemctl start smart-room-sensor
```

查看状态和日志：

```bash
sudo systemctl status smart-room-sensor
journalctl -u smart-room-sensor -f
```

停止服务：

```bash
sudo systemctl stop smart-room-sensor
```

## 11. 常见问题

### pip3 提示 externally-managed-environment

这是 Raspberry Pi OS 对系统 Python 的保护。不要执行 `pip3 install -r requirements.txt`，也不要优先使用 `--break-system-packages`。应当在项目虚拟环境里安装：

```bash
cd /home/group1/raspberry_pi_server
deactivate 2>/dev/null || true
rm -rf .venv
python3 -m venv .venv --system-site-packages
. .venv/bin/activate
python -m pip install --upgrade pip setuptools wheel
python -m pip install -r requirements.txt
```

检查当前命令是否真的来自虚拟环境：

```bash
which python
python -m pip --version
```

正常路径应包含 `/home/group1/raspberry_pi_server/.venv/`。

### 前端连不上

检查：

```bash
hostname -I
netstat -tuln | grep 8775
```

确认前端地址是 `ws://树莓派IP:8775`，电脑和树莓派在同一网络。

### DHT11 一直是默认值

检查 DATA 是否接 GPIO17，VCC 是否是 3.3V，配置里的 `dht.gpio_pin` 是否为 `17`。DHT11 上电后需要等待约 1 秒，读取时偶尔失败是正常现象；程序会按配置重试，仍失败时才使用默认温湿度，保证前端不断流。

也可以先运行第 6 章同款最小测试程序确认 DHT11 本身正常：

```bash
python - <<'PY'
import time
import board
import adafruit_dht

dht_device = adafruit_dht.DHT11(board.D17)
time.sleep(1)
try:
    for _ in range(5):
        try:
            print("Temperature:", dht_device.temperature, "Humidity:", dht_device.humidity)
        except RuntimeError as exc:
            print("Read failed:", exc)
        time.sleep(2)
finally:
    dht_device.exit()
PY
```

### PCF8591 没有数据

先确认启用了 I2C：

```bash
sudo raspi-config nonint do_i2c 0
sudo reboot
i2cdetect -y 1
```

如果看不到 `48`，优先检查 SDA/SCL 和供电。

## 12. 前端按钮控制树莓派设备

当前管理系统前端可以通过 Spring Boot 后端控制树莓派上的设备。整体链路如下：

```text
前端按钮
 -> 调用 POST /iot/devices/{deviceId}/commands
 -> Spring Boot 通过 /iot/ws/device 下发 COMMAND 消息
 -> 树莓派执行 GPIO 输出、舵机动作，或更新模拟状态
 -> 树莓派返回 COMMAND_ACK 命令确认
 -> 树莓派立即上传一条新的 telemetry 状态数据
 -> 前端重新请求 /iot/rooms/{roomId}/status 刷新页面状态
```

下面是几个后端命令接口测试示例。把 `backend-pc-ip` 换成后端电脑 IP：

```bash
curl -X POST http://backend-pc-ip:8080/iot/devices/raspi-01/commands \
  -H "Content-Type: application/json" \
  -d '{"command":"set_device_state","target":"light","value":true}'

curl -X POST http://backend-pc-ip:8080/iot/devices/raspi-01/commands \
  -H "Content-Type: application/json" \
  -d '{"command":"set_device_state","target":"projector","value":true}'

curl -X POST http://backend-pc-ip:8080/iot/devices/raspi-01/commands \
  -H "Content-Type: application/json" \
  -d '{"command":"open_door"}'

curl -X POST http://backend-pc-ip:8080/iot/devices/raspi-01/commands \
  -H "Content-Type: application/json" \
  -d '{"command":"set_servo_angle","angle":90}'
```

支持的命令名称：

- `set_device_state`：设置某个设备状态，需要传 `target` 和 `value`。
- `set_auto_mode`：设置自动模式开关，需要传 `value`。
- `open_door`：按配置中的开门角度控制舵机开门。
- `close_door`：按配置中的关门角度控制舵机关门。
- `set_servo_angle`：直接设置舵机角度，需要传 `angle`。

`set_device_state.target` 支持的设备名称：

- `projector`：投影仪
- `light`：灯光
- `curtain`：窗帘
- `buzzer`：蜂鸣器
- `alarm`：报警状态
- `cooling`：空调制冷
- `heating`：空调制热
- `humidifier`：加湿器
- `dehumidifier`：除湿器
- `auto`：自动模式

如果 `outputs.<target>.enabled` 是 `false`，程序仍会更新内部模拟状态，并返回 `COMMAND_ACK`，但不会真的改变 GPIO 电平。要控制真实继电器、LED、蜂鸣器或其他输出模块，需要在 `config.json` 里打开对应的 `outputs` 配置。
