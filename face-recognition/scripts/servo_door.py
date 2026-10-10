"""人脸识别舵机门禁（移植自 OPENcv 项目的 face_servo_door.py，使用本组识别引擎）

与本组识别引擎的衔接（相对原版的改进）：
原版在单帧匹配通过即开门；本版要求多帧投票 CONFIRMED 且命中授权名单才开门，
避免单帧误识导致误开，提升安全性（对应任务书"安全性和隐私保护"选做项）。

接线建议：
- 舵机红线接 5V；黑线/棕线接 GND；黄线/橙线接 GPIO18（物理引脚 12）
- 舵机使用外部电源时，电源 GND 必须与树莓派 GND 共地

用法：
    python scripts/servo_door.py --mock-servo                          # 电脑调试，任何人可开门
    python scripts/servo_door.py --authorized-name 张三 --mock-servo   # 电脑调试，仅张三可开门
    python scripts/servo_door.py --authorized-name 张三                # 树莓派真实舵机
    python scripts/servo_door.py --headless --mock-servo               # 无窗口终端模式

按键：q 退出（headless 模式用 Ctrl+C）
"""

import argparse
import sys
import time
from pathlib import Path

import cv2

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from face_engine.camera import CameraSource
from face_engine.config import load_config
from face_engine.engine import FaceEngine
from face_engine.recognizer import Status
from face_engine.servo import ServoDoorController


def main():
    parser = argparse.ArgumentParser(description="人脸识别舵机门禁")
    parser.add_argument("--authorized-name", default=None, help="授权开门人员姓名（与人脸库一致）；留空则任何注册人员可开门")
    parser.add_argument("--camera", type=int, default=0, help="摄像头编号")
    parser.add_argument("--mock-servo", action="store_true", help="模拟舵机（不控制 GPIO）")
    parser.add_argument("--headless", action="store_true", help="无窗口终端模式")
    parser.add_argument("--open-duration", type=float, default=30.0, help="开门保持时长（秒）")
    parser.add_argument("--servo-open-angle", type=float, default=90.0)
    parser.add_argument("--servo-close-angle", type=float, default=0.0)
    args = parser.parse_args()

    engine = FaceEngine(load_config())
    cam = CameraSource(args.camera)
    if not cam.opened:
        print("摄像头打开失败，检查占用情况或更换 --camera 编号")
        return
    door = ServoDoorController(
        close_angle=args.servo_close_angle,
        open_angle=args.servo_open_angle,
        mock=args.mock_servo,
    )

    print(f"门禁启动。授权名单: {args.authorized_name or '全部注册人员'}")
    try:
        while True:
            frame = cam.read()
            if frame is None:
                print("摄像头读取失败，退出")
                break
            result = engine.recognize_frame(frame)

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

            if not args.headless:
                color = (0, 255, 0) if result.status is Status.CONFIRMED else (0, 0, 255)
                if result.bbox:
                    x, y, w, h = result.bbox
                    cv2.rectangle(frame, (x, y), (x + w, y + h), color, 2)
                    cv2.putText(frame, result.display_name, (x, max(y - 8, 16)),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)
                cv2.putText(frame, "DOOR: " + ("OPEN" if door.is_open else "CLOSED"),
                            (8, 48), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 255), 2)
                cv2.imshow("servo-door", frame)
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
