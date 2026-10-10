"""人脸注册脚本（多角度注册流程——自主设计）

用法：
    python scripts/enroll.py --id s2023001 --name 张三 [--count 5] [--interval 1.0]

流程：
    摄像头实时预览，自动抓取 count 张通过质量门控的人脸；
    按采集序号提示角度（正视 → 左转 → 右转），实现多角度注册；
    全部采集完成后批量提取特征并写入人脸库。
按键：q 提前退出（已采集的图像仍会入库）
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

ANGLES = [
    "请正视镜头",
    "请正视镜头",
    "请缓慢左转",
    "请缓慢左转",
    "请缓慢右转",
    "请缓慢右转",
]


def main():
    parser = argparse.ArgumentParser(description="人脸注册")
    parser.add_argument("--id", required=True, help="用户 ID（与系统用户表一致）")
    parser.add_argument("--name", required=True, help="姓名")
    parser.add_argument("--count", type=int, default=5, help="采集张数")
    parser.add_argument("--interval", type=float, default=1.0, help="采集间隔（秒）")
    parser.add_argument("--camera", type=int, default=0, help="摄像头编号")
    args = parser.parse_args()

    engine = FaceEngine(load_config())
    cam = CameraSource(args.camera)
    if not cam.opened:
        print("摄像头打开失败，可能原因：")
        print("  1) 摄像头被其他程序占用（demo.py 或其他视频应用是否还开着？）")
        print("  2) Windows 隐私设置：设置 → 隐私和安全性 → 摄像头 → 打开应用访问权限")
        print(f"  3) 设备编号不对，尝试 --camera 1 或其他编号")
        return
    print(f"摄像头已打开（后端: {cam.backend}）")

    captured = []
    last_capture = 0.0
    print(f"开始采集 {args.count} 张合格人脸（按 q 退出）")
    print("画面中绿色框=检测到合格人脸；若一直没有框，请对准镜头并保证光线充足")
    try:
        while len(captured) < args.count:
            frame = cam.read()
            if frame is None:
                print("摄像头读取失败，退出")
                break
            hint = ANGLES[min(len(captured), len(ANGLES) - 1)]

            # 每帧检测：用于画面反馈与自动采集
            faces = engine.detector.detect(frame)
            now = time.time()
            if faces:
                face = faces[0]
                x, y, w, h = face.bbox
                cv2.rectangle(frame, (x, y), (x + w, y + h), (0, 255, 0), 2)
                if now - last_capture >= args.interval:
                    captured.append(face.image)
                    last_capture = now
                    print(f"已采集 {len(captured)}/{args.count}")
                    status = "采集成功，保持姿势"
                else:
                    status = f"距下次采集 {args.interval - (now - last_capture):.1f}s"
            else:
                status = "未检测到合格人脸（对准镜头/光线充足）"

            cv2.putText(frame, hint, (16, 32), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 0), 2)
            cv2.putText(frame, f"{len(captured)}/{args.count}", (16, 60),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 0), 2)
            cv2.putText(frame, status, (16, 88), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 200, 255), 2)
            cv2.imshow("enroll", frame)
            if cv2.waitKey(1) & 0xFF == ord("q"):
                break
    finally:
        cam.release()
        cv2.destroyAllWindows()

    if not captured:
        print("未采集到合格人脸（光线是否充足？是否通过了质量门控？），未写入人脸库")
        return
    count = engine.register_aligned(args.id, args.name, captured)
    print(f"注册完成：{args.name}（{args.id}）入库 {count} 个特征向量")


if __name__ == "__main__":
    main()
