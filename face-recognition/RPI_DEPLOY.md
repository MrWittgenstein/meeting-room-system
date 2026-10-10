# 树莓派 4B 部署手册

人脸识别模块从 Windows 开发机迁移到树莓派 4B 的完整步骤。

## 0. 准备清单

| 项 | 说明 |
|---|---|
| 树莓派 4B | 建议 4GB 内存版；**必须配散热片 + 风扇**（持续推理负载下无散热会降频，FPS 明显下降） |
| SD 卡 | 32GB 以上，Class 10 |
| 摄像头 | CSI Camera Module V2/V3，或任意 UVC USB 摄像头 |
| 舵机（门禁用） | SG90，信号线接 GPIO18（物理引脚 12） |
| 系统 | Raspberry Pi OS（64-bit）Bookworm，烧录工具 Raspberry Pi Imager |

## 1. 系统烧录与基础配置

1. 用 Raspberry Pi Imager 烧录 **Raspberry Pi OS（64-bit）Bookworm**
2. 烧录时在设置里开启 SSH、配好 Wi-Fi，或首次开机后桌面配置
3. 开启摄像头接口：`sudo raspi-config` → Interface Options → Camera → Enable
4. 验证摄像头：`libcamera-hello --list-cameras` 能列出设备

## 2. 拷贝项目并安装依赖

```bash
# 拷贝项目（U 盘 / scp / git 均可）
cd ~
# 例如: scp -r smart-meeting-face pi@192.168.x.x:~/

cd smart-meeting-face

# 虚拟环境 + 依赖（清华镜像，国内网络更快）
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

# CSI 摄像头额外依赖（USB 摄像头跳过）
sudo apt install -y python3-picamera2
.venv/bin/pip install picamera2 -i https://pypi.tuna.tsinghua.edu.cn/simple

# 门禁舵机依赖（不做门禁可跳过）
.venv/bin/pip install RPi.GPIO -i https://pypi.tuna.tsinghua.edu.cn/simple
```

## 3. 下载模型并自检

```bash
.venv/bin/python scripts/download_models.py
.venv/bin/python scripts/smoke_test.py --with-models
# 期望输出：gallery/voting/detector/embedder 全部 [OK]
```

## 4. 注册与识别

```bash
# 注册（CSI 摄像头和 USB 摄像头走同一套代码，自动选择 Picamera2 或 OpenCV）
.venv/bin/python scripts/enroll.py --id s001 --name 张三

# 实时识别（有显示器）
.venv/bin/python scripts/demo.py

# 无显示器（headless）：终端每 1 秒打印识别状态，Ctrl+C 退出
.venv/bin/python scripts/demo.py --headless

# 事件日志与陌生人截图会自动写入 data/recognition_log.csv、data/unknown_faces/
```

## 5. 舵机门禁

```bash
# 接线：舵机红线 5V、黑/棕线 GND、黄/橙线 GPIO18；外部供电必须与树莓派共地
.venv/bin/python scripts/servo_door.py --authorized-name 张三
# 无显示器: 加上 --headless
```

## 6. 启动 HTTP 服务（供小组框架对接）

```bash
.venv/bin/python -m uvicorn face_server:app --host 0.0.0.0 --port 8000
```

## 7. 开机自启（可选，systemd）

`sudo nano /etc/systemd/system/face-demo.service`：

```ini
[Unit]
Description=Meeting Room Face Recognition
After=network.target

[Service]
User=pi
WorkingDirectory=/home/pi/smart-meeting-face
ExecStart=/home/pi/smart-meeting-face/.venv/bin/python scripts/demo.py --headless
Restart=always

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable face-demo
sudo systemctl start face-demo
journalctl -u face-demo -f   # 查看日志
```

## 8. 性能调优（树莓派 4B）

| 手段 | 位置 | 说明 |
|---|---|---|
| 降检测分辨率 | config.yaml `model.detect_input_size` | 320 改 160 更快，但小脸检测变弱 |
| 增加跳帧间隔 | `pipeline.embed_every` | 2 改 3~4，特征提取计算量再降 |
| 线程数 | `pipeline.threads` | 4B 四核建议 2~4 |
| 关闭增强回退链 | `detection.enhance.enabled` | 若已保证光照充足可关闭，省算力 |

## 9. 常见问题

| 现象 | 排查 |
|---|---|
| 摄像头打不开 | `libcamera-hello --list-cameras` 确认设备；CSI 排线方向；是否开启 Camera 接口 |
| 温度过高降频 | `vcgencmd measure_temp`；加强散热，确保风扇供电 |
| 识别明显变慢 | `top` 看负载；按第 8 节调参；确认 onnxruntime 用 CPUExecutionProvider |
| 中文显示乱码 | SSH 终端换 UTF-8 编码；不影响识别功能 |
