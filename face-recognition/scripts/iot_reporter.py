"""人脸识别结果接入会议室后端（WebSocket IoT 协议适配器）

对接上一学期后端 IotDeviceWebSocketHandler 的协议：
- 设备 → 后端：遥测 JSON（含 access_control 人脸门禁字段：face_detected/
  face_count/authorized/access_result/door_state/servo_angle/door_open）
- 后端 → 设备：COMMAND 命令（设备须在 3 秒内回复 COMMAND_ACK，否则后端判超时）
- 首次遥测上报后，后端按 device_id 绑定会话，随后即可下发命令
- 断线自动重连（5 秒间隔）

用法：
    python scripts/iot_reporter.py --mock-servo --headless
    python scripts/iot_reporter.py --ws-url ws://192.168.5.78:8080/ws/iot --device-id pi4b-01 --room-code A101
    python scripts/iot_reporter.py --authorized-name 张三 --mock-servo
    python scripts/iot_reporter.py --no-bridge --mock-servo   # 仅本地识别+门禁，不连后端

按键：q 退出（headless 模式用 Ctrl+C）
"""

import argparse
import json
import sys
import threading
import time
from datetime import datetime
from pathlib import Path

import cv2

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from face_engine.camera import CameraSource
from face_engine.config import load_config
from face_engine.engine import FaceEngine
from face_engine.recognizer import Status
from face_engine.servo import ServoDoorController

COLOR = {
    Status.CONFIRMED: (0, 255, 0),
    Status.PENDING: (0, 200, 255),
    Status.UNKNOWN: (0, 0, 255),
    Status.LOST: (128, 128, 128),
}


def now_iso() -> str:
    return datetime.now().strftime("%Y-%m-%dT%H:%M:%S")


class BackendBridge:
    """WebSocket 桥：遥测上报 + 命令接收（自动重连）"""

    def __init__(self, ws_url: str, device_id: str, room_code: str, on_command):
        import websocket  # pip install websocket-client

        self._ws_lib = websocket
        self.ws_url = ws_url
        self.device_id = device_id
        self.room_code = room_code
        self.on_command = on_command
        self._app = None
        self._lock = threading.Lock()
        self._thread = threading.Thread(target=self._run_forever, daemon=True)
        self.connected = False

    def start(self) -> None:
        self._thread.start()

    def _run_forever(self) -> None:
        while True:
            try:
                app = self._ws_lib.WebSocketApp(
                    self.ws_url,
                    on_open=self._on_open,
                    on_message=self._on_message,
                    on_error=lambda ws, err: None,
                    on_close=self._on_close,
                )
                with self._lock:
                    self._app = app
                app.run_forever(ping_interval=20)
            except Exception:
                pass
            self.connected = False
            time.sleep(5)  # 断线重连

    def _on_open(self, ws) -> None:
        self.connected = True
        print(f"[桥接] 已连接后端 {self.ws_url}")

    def _on_close(self, ws, code, msg) -> None:
        self.connected = False
        print("[桥接] 连接断开，5 秒后重连...")

    def _on_message(self, ws, message) -> None:
        try:
            data = json.loads(message)
        except Exception:
            return
        if data.get("type") != "COMMAND":
            if data.get("type") == "ACK" and data.get("status") == "error":
                print(f"[桥接] 后端回执异常: {data.get('message')}")
            return
        status, msg = "success", "ok"
        try:
            self.on_command(data)
        except Exception as exc:
            status, msg = "error", str(exc)
            print(f"[桥接] 命令执行失败: {exc}")
        self.send(
            {
                "type": "COMMAND_ACK",
                "commandId": data.get("commandId"),
                "status": status,
                "message": msg,
            }
        )

    def send(self, payload: dict) -> None:
        with self._lock:
            app = self._app
        if app is not None:
            try:
                app.send(json.dumps(payload, ensure_ascii=False))
            except Exception:
                pass

    def report(self, access: dict) -> None:
        """按后端协议上报 access_control 遥测"""
        self.send(
            {
                "device_id": self.device_id,
                "room_id": self.room_code,
                "event_type": "face_recognition",
                "current": {
                    "access_control": access,
                    "presence": {
                        "person_near": bool(access.get("face_detected", False)),
                        "people_count": int(access.get("face_count", 0)),
                        "presence": bool(access.get("face_detected", False)),
                    },
                    "system": {"sensor_status": "ok"},
                },
                "timestamp": now_iso(),
            }
        )


def make_command_handler(door: ServoDoorController):
    """后端 COMMAND → 本地舵机动作"""

    def handle(cmd: dict) -> None:
        command = str(cmd.get("command", "")).lower()
        angle = cmd.get("angle")
        if command in ("open_door", "set_servo", "open", "open_servo"):
            if angle is not None:
                door.open_angle = float(angle)
            door.open(30.0)
        elif command in ("close_door", "close", "close_servo"):
            door.close()
        elif command in ("buzzer", "alarm"):
            print("[命令] 蜂鸣器告警（演示环境仅记录）")
        else:
            raise ValueError(f"未知命令: {command}")

    return handle


def build_access(result, door, authorized_name):
    """识别结果 + 门状态 → 后端 access_control 字段（失败不放行原则在此体现）"""
    base = {
        "face_detected": result.status in (Status.CONFIRMED, Status.PENDING, Status.UNKNOWN),
        "face_count": 1 if result.status in (Status.CONFIRMED, Status.PENDING, Status.UNKNOWN) else 0,
        "door_state": "open" if door.is_open else "closed",
        "servo_angle": int(door.open_angle) if door.is_open else int(door.close_angle),
        "door_open": door.is_open,
    }
    if result.status is Status.CONFIRMED:
        if authorized_name is None or result.name == authorized_name:
            return {**base, "authorized": True, "access_result": "granted",
                    "face_name": result.name}
        return {**base, "authorized": False, "access_result": "denied",
                "face_name": result.name}
    if result.status is Status.UNKNOWN:
        return {**base, "authorized": False, "access_result": "denied"}
    return {**base, "authorized": False, "access_result": "pending"}


def main():
    parser = argparse.ArgumentParser(description="人脸识别 IoT 上报（对接会议室后端）")
    parser.add_argument("--authorized-name", default=None, help="授权开门人员姓名；留空则任何注册人员可开门")
    parser.add_argument("--camera", type=int, default=0, help="摄像头编号")
    parser.add_argument("--mock-servo", action="store_true", help="模拟舵机（不控制 GPIO）")
    parser.add_argument("--headless", action="store_true", help="无窗口终端模式")
    parser.add_argument("--open-duration", type=float, default=30.0, help="开门保持时长（秒）")
    parser.add_argument("--servo-open-angle", type=float, default=90.0)
    parser.add_argument("--servo-close-angle", type=float, default=0.0)
    parser.add_argument("--ws-url", default=None, help="后端 WebSocket 地址（默认取 config.yaml）")
    parser.add_argument("--device-id", default=None, help="设备编号（默认取 config.yaml）")
    parser.add_argument("--room-code", default=None, help="会议室编号（默认取 config.yaml）")
    parser.add_argument("--no-bridge", action="store_true", help="不连后端，仅本地识别+门禁")
    parser.add_argument("--report-interval", type=float, default=1.0, help="遥测兜底上报间隔（秒）")
    args = parser.parse_args()

    config = load_config()
    backend_cfg = config.get("backend", {})
    ws_url = args.ws_url or backend_cfg.get("ws_url", "ws://192.168.5.1:8080/ws/iot")
    device_id = args.device_id or backend_cfg.get("device_id", "pi4b-face-01")
    room_code = args.room_code or backend_cfg.get("room_code", "A101")

    engine = FaceEngine(config)
    cam = CameraSource(args.camera)
    if not cam.opened:
        print("摄像头打开失败，检查占用或更换 --camera 编号")
        return
    door = ServoDoorController(
        close_angle=args.servo_close_angle,
        open_angle=args.servo_open_angle,
        mock=args.mock_servo,
    )

    bridge = None
    if not args.no_bridge:
        bridge = BackendBridge(ws_url, device_id, room_code, make_command_handler(door))
        bridge.start()
    print(f"授权名单: {args.authorized_name or '全部注册人员'}")
    print(f"后端桥接: {'关闭' if bridge is None else ws_url}")

    last_report = 0.0
    last_state = None
    try:
        while True:
            frame = cam.read()
            if frame is None:
                print("摄像头读取失败，退出")
                break
            result = engine.recognize_frame(frame)

            # 授权判定 + 门控（多帧投票确认 + 授权名单，失败不放行）
            if result.status is Status.CONFIRMED:
                if args.authorized_name is None or result.name == args.authorized_name:
                    door.open(args.open_duration)
                    print(f"✓ 授权人员 {result.name}（sim={result.similarity:.3f}），开门")
                else:
                    print(f"✗ {result.name} 不在授权名单，拒绝开门")
            elif result.status is Status.UNKNOWN:
                print("陌生人，不开门")
            elif result.status is Status.LOST:
                print("无人")
            door.update()

            # 上报：状态变化立即上报，否则按兜底间隔
            access = build_access(result, door, args.authorized_name)
            if bridge is not None:
                key = json.dumps(access, sort_keys=True)
                now = time.time()
                if key != last_state or now - last_report >= args.report_interval:
                    bridge.report(access)
                    last_state = key
                    last_report = now

            if not args.headless:
                color = COLOR[result.status]
                if result.bbox:
                    x, y, w, h = result.bbox
                    cv2.rectangle(frame, (x, y), (x + w, y + h), color, 2)
                    cv2.putText(frame, result.display_name, (x, max(y - 8, 16)),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)
                cv2.putText(frame, "DOOR: " + ("OPEN" if door.is_open else "CLOSED"),
                            (8, 48), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 255), 2)
                cv2.imshow("iot-face-reporter", frame)
                if cv2.waitKey(1) & 0xFF == ord("q"):
                    break
    except KeyboardInterrupt:
        print("\n收到中断，退出")
    finally:
        door.close()
        door.cleanup()
        cam.release()
        cv2.destroyAllWindows()


if __name__ == "__main__":
    main()
