"""实时识别演示（本地摄像头或视频文件）

用法：
    python scripts/demo.py                          # 摄像头实时识别
    python scripts/demo.py --source demo.mp4        # 视频文件（对比实验/答辩预演）
    python scripts/demo.py --no-voting              # 关闭多帧投票（消融对比）
    python scripts/demo.py --no-quality             # 关闭质量门控（消融对比）
    python scripts/demo.py --log demo_log.csv       # 逐帧结果 CSV（供 eval.py 对比分析）
    python scripts/demo.py --headless               # 无窗口模式（树莓派无显示器）
    python scripts/demo.py --save-video out.mp4     # 导出带标注框的结果视频
    python scripts/demo.py --no-unknown-log         # 关闭陌生人截图记录

自动记录（默认开启，输出到 config.yaml 的 logging.dir）：
    recognition_log.csv  事件日志：进场/离场/身份切换/陌生人出现（签到流水）
    unknown_log.csv      陌生人截图记录
    unknown_faces/       陌生人截图（带冷却去重，防止同一人连续刷屏）

按键：q 退出（headless 模式用 Ctrl+C）
"""

import argparse
import csv
import sys
import time
from datetime import datetime
from pathlib import Path

import cv2

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from face_engine.camera import CameraSource
from face_engine.config import load_config
from face_engine.engine import FaceEngine
from face_engine.recognizer import Status

COLOR = {
    Status.CONFIRMED: (0, 255, 0),
    Status.PENDING: (0, 200, 255),
    Status.UNKNOWN: (0, 0, 255),
    Status.LOST: (128, 128, 128),
}


def _now_str() -> str:
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")


def _draw(frame, result, fps):
    color = COLOR[result.status]
    if result.bbox:
        x, y, w, h = result.bbox
        cv2.rectangle(frame, (x, y), (x + w, y + h), color, 2)
        cv2.putText(frame, result.display_name, (x, max(y - 8, 16)),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)
    cv2.putText(frame, f"FPS: {fps:.1f}", (8, 24),
                cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 255), 2)


def main():
    parser = argparse.ArgumentParser(description="实时人脸识别演示")
    parser.add_argument("--source", default="0", help="摄像头编号或视频路径")
    parser.add_argument("--no-voting", action="store_true", help="关闭多帧投票（消融对比用）")
    parser.add_argument("--no-quality", action="store_true", help="关闭质量门控（消融对比用）")
    parser.add_argument("--log", default=None, help="逐帧结果 CSV 输出路径")
    parser.add_argument("--headless", action="store_true", help="无窗口模式（树莓派无显示器）")
    parser.add_argument("--save-video", default=None, help="导出带标注框的结果视频")
    parser.add_argument("--no-unknown-log", action="store_true", help="关闭陌生人截图记录")
    args = parser.parse_args()

    config = load_config()
    if args.no_voting:
        config["voting"]["enabled"] = False
    if args.no_quality:
        config["quality_gate"]["enabled"] = False

    engine = FaceEngine(config)
    if not engine.list_users():
        print("提示：人脸库为空，识别将一直显示\"未注册人员\"。")
        print("请先注册：.venv\\Scripts\\python scripts/enroll.py --id s001 --name 你的名字")
    source = int(args.source) if args.source.isdigit() else args.source
    cam = CameraSource(source)
    if not cam.opened:
        print("摄像头/视频打开失败：检查是否被其他程序占用、Windows 摄像头隐私设置，或更换 --source 编号")
        return

    # ---- 日志目录与文件 ----
    log_cfg = config.get("logging", {})
    log_dir = Path(log_cfg.get("dir", "data"))
    if not log_dir.is_absolute():
        log_dir = Path(__file__).resolve().parent.parent / log_dir
    log_dir.mkdir(parents=True, exist_ok=True)

    event_file = open(log_dir / "recognition_log.csv", "a", newline="", encoding="utf-8")
    event_writer = csv.writer(event_file)
    if event_file.tell() == 0:
        event_writer.writerow(["time", "event", "user_id", "name", "sim"])

    unknown_writer = None
    unknown_file = None
    if not args.no_unknown_log:
        unknown_dir = log_dir / "unknown_faces"
        unknown_file = open(log_dir / "unknown_log.csv", "a", newline="", encoding="utf-8")
        unknown_writer = csv.writer(unknown_file)
        if unknown_file.tell() == 0:
            unknown_writer.writerow(["time", "image", "sim"])

    frame_writer = None
    frame_file = None
    if args.log:
        frame_file = open(args.log, "w", newline="", encoding="utf-8")
        frame_writer = csv.writer(frame_file)
        frame_writer.writerow(["frame", "ts", "status", "user_id", "sim"])

    video_writer = None

    stats = {}               # user_id -> {"name", "first", "last", "frames"}
    prev_user = None         # 上一个有效身份（事件日志用）
    last_unknown_ts = 0.0
    unknown_cooldown = float(log_cfg.get("unknown_cooldown", 5.0))
    last_status_print = 0.0
    frame_idx = 0
    fps = 0.0

    try:
        while True:
            t0 = time.time()
            frame = cam.read()
            if frame is None:
                print("摄像头/视频读取失败，退出")
                break
            result = engine.recognize_frame(frame)

            # ---- 事件日志（进场/离场/身份切换/陌生人出现）----
            status = result.status
            if status is not Status.PENDING:
                event = None
                if status is Status.CONFIRMED:
                    if prev_user is None:
                        event = "进场"
                    elif prev_user != result.user_id:
                        event = "身份切换"
                elif status is Status.UNKNOWN and prev_user is not None:
                    event = "陌生人出现"
                elif status is Status.LOST and prev_user is not None:
                    event = "离场"
                if event:
                    event_writer.writerow(
                        [_now_str(), event, result.user_id, result.name,
                         f"{result.similarity:.4f}"]
                    )
                    print(f"[事件] {event} {result.name or ''} (sim={result.similarity:.3f})")
                prev_user = result.user_id if status is Status.CONFIRMED else None

            # ---- 出现统计 ----
            if status is Status.CONFIRMED and result.user_id:
                s = stats.setdefault(
                    result.user_id, {"name": result.name, "first": time.time(), "frames": 0}
                )
                s["frames"] += 1
                s["last"] = time.time()

            # ---- 陌生人截图（冷却去重）----
            if (
                status is Status.UNKNOWN
                and unknown_writer is not None
                and time.time() - last_unknown_ts >= unknown_cooldown
            ):
                ts = datetime.now().strftime("%Y%m%d_%H%M%S")
                unknown_dir.mkdir(parents=True, exist_ok=True)
                cv2.imwrite(str(unknown_dir / f"{ts}.jpg"), frame)
                unknown_writer.writerow([_now_str(), f"{ts}.jpg", f"{result.similarity:.4f}"])
                print(f"陌生人截图已保存: unknown_faces/{ts}.jpg")
                last_unknown_ts = time.time()

            # ---- 绘制与输出 ----
            _draw(frame, result, fps)
            if video_writer is None and args.save_video:
                h, w = frame.shape[:2]
                video_writer = cv2.VideoWriter(
                    args.save_video, cv2.VideoWriter_fourcc(*"mp4v"), 20.0, (w, h)
                )
            if video_writer is not None:
                video_writer.write(frame)
            if frame_writer:
                frame_writer.writerow([
                    frame_idx, f"{time.time():.3f}", status.value,
                    result.user_id, f"{result.similarity:.4f}",
                ])

            if args.headless:
                if time.time() - last_status_print >= 1.0:
                    print(f"[{_now_str()}] {status.value} user={result.user_id or '-'} "
                          f"sim={result.similarity:.3f} fps={fps:.1f}")
                    last_status_print = time.time()
            else:
                cv2.imshow("smart-meeting-face", frame)
                if cv2.waitKey(1) & 0xFF == ord("q"):
                    break

            frame_idx += 1
            fps = 0.9 * fps + 0.1 / max(time.time() - t0, 1e-6)
    except KeyboardInterrupt:
        print("\n收到中断，退出")
    finally:
        # ---- 出现统计汇总 ----
        if stats:
            print("\n===== 出现统计 =====")
            for uid, s in stats.items():
                dur = s.get("last", s["first"]) - s["first"]
                print(f"{s['name']}（{uid}）：确认帧 {s['frames']} 帧，出现时长约 {dur:.0f}s")
        for f in (event_file, unknown_file, frame_file):
            if f:
                f.close()
        if video_writer is not None:
            video_writer.release()
            print(f"结果视频已保存: {args.save_video}")
        cam.release()
        cv2.destroyAllWindows()


if __name__ == "__main__":
    main()
