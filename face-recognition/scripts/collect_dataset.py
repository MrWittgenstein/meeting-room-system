"""数据集采集工具：对着摄像头自动批量拍摄，构建 eval.py 用的测试数据集

用法：
    python scripts/collect_dataset.py --name 张三                  # 采集 30 张
    python scripts/collect_dataset.py --name 李四 --count 20        # 自定义张数
    python scripts/collect_dataset.py --name 王五 --negative        # 采集为陌生人测试集
    python scripts/collect_dataset.py --camera 1 --name 赵六        # 指定摄像头

目录结构（与 eval.py 配套）：
    data/dataset/<姓名>/01.jpg ...    注册人员：前 5 张（按文件名排序）用作注册，其余作测试
    data/dataset/__negative__/01.jpg  未注册人员（陌生人测试集）

采集设计：
    - 检测到人脸才保存整帧（不应用质量门控，统一留给 eval 阶段过滤，保证消融实验公平）
    - 按进度提示变换姿态/表情：前 5 张正视（注册用），随后依次左转/右转/
      仰视/俯视/微笑/远距离——覆盖测试集的变化维度
按键：q 提前结束
"""

import argparse
import sys
import time
from pathlib import Path

import cv2

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from face_engine.camera import CameraSource
from face_engine.config import load_config
from face_engine.detector import FaceDetector

# (提示语, 持续张数)：前 5 张为注册照片，要求正脸标准
STAGES = [
    ("正视镜头（注册照）", 5),
    ("缓慢左转", 5),
    ("缓慢右转", 5),
    ("抬头仰视", 5),
    ("低头俯视", 5),
    ("微笑/表情变化", 5),
    ("后退一步（远距离）", 5),
]


def main():
    parser = argparse.ArgumentParser(description="数据集采集工具")
    parser.add_argument("--name", required=True, help="人员姓名（文件夹名）")
    parser.add_argument("--count", type=int, default=30, help="采集张数")
    parser.add_argument("--interval", type=float, default=0.9, help="抓拍间隔（秒）")
    parser.add_argument("--camera", type=int, default=0, help="摄像头编号")
    parser.add_argument("--negative", action="store_true", help="存入陌生人测试集 __negative__")
    args = parser.parse_args()

    root = Path(__file__).resolve().parent.parent / "data" / "dataset"
    sub = "__negative__" if args.negative else args.name
    out_dir = root / sub
    out_dir.mkdir(parents=True, exist_ok=True)
    # 已有文件则续拍（避免覆盖）
    start = sum(1 for p in out_dir.iterdir() if p.suffix.lower() in {".jpg", ".jpeg", ".png"})

    config = load_config()
    qc = dict(config.get("quality_gate", {}))
    qc["enabled"] = False  # 采集阶段不做质量门控，统一留给 eval 过滤，保证消融公平
    detector = FaceDetector(
        config["model"]["detector"],
        input_size=tuple(config["model"].get("detect_input_size", [320, 320])),
        score_threshold=config["model"].get("score_threshold", 0.6),
        quality_config=qc,
        enhance_config=config.get("detection", {}).get("enhance", {}),
    )
    cam = CameraSource(args.camera)
    if not cam.opened:
        print("摄像头打开失败，检查占用或更换 --camera 编号")
        return

    hints = [text for text, n in STAGES for _ in range(n)]
    captured = 0
    last = 0.0
    print(f"目标目录: {out_dir}（从 {start + 1:03d}.jpg 开始，共 {args.count} 张）")
    try:
        while captured < args.count:
            frame = cam.read()
            if frame is None:
                print("摄像头读取失败，退出")
                break
            hint = hints[min(captured, len(hints) - 1)]
            faces = detector.detect(frame)
            now = time.time()
            if faces and now - last >= args.interval:
                idx = start + captured + 1
                cv2.imwrite(str(out_dir / f"{idx:03d}.jpg"), frame)
                captured += 1
                last = now
                print(f"[{captured}/{args.count}] 已保存 {idx:03d}.jpg（{hint}）")

            if faces:
                x, y, w, h = faces[0].bbox
                cv2.rectangle(frame, (x, y), (x + w, y + h), (0, 255, 0), 2)
                status = (
                    f"距下次抓拍 {args.interval - (now - last):.1f}s"
                    if now - last < args.interval
                    else "准备抓拍..."
                )
            else:
                status = "未检测到人脸"
            cv2.putText(frame, hint, (16, 32), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 0), 2)
            cv2.putText(frame, f"{captured}/{args.count}", (16, 60),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 0), 2)
            cv2.putText(frame, status, (16, 88), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 200, 255), 2)
            cv2.imshow("dataset-collect", frame)
            if cv2.waitKey(1) & 0xFF == ord("q"):
                break
    finally:
        cam.release()
        cv2.destroyAllWindows()
    print(f"完成：本次采集 {captured} 张 → {out_dir}")


if __name__ == "__main__":
    main()
