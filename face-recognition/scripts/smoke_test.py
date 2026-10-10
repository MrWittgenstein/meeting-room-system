"""冒烟测试：不依赖摄像头/模型的逻辑自检；--with-models 时额外检查模型推理

用法：
    python scripts/smoke_test.py              # 仅逻辑测试（无需模型文件）
    python scripts/smoke_test.py --with-models  # 需先下载模型到 models/ 目录
"""

import argparse
import os
import sys
import tempfile
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from face_engine.config import load_config
from face_engine.gallery import FaceGallery
from face_engine.recognizer import Status, VotingRecognizer


class _M:
    """GalleryMatch 的极简替身"""

    def __init__(self, uid, sim, name="X", threshold=0.4):
        self.user_id, self.similarity = uid, sim
        self.name, self.threshold = name, threshold


def test_gallery():
    path = os.path.join(tempfile.mkdtemp(), "gallery.json")
    g = FaceGallery(
        path,
        adaptive={"enabled": True, "k": 2.0, "global_min": 0.35, "global_max": 0.55},
    )
    a = np.random.rand(512).astype(np.float32)
    g.register("u1", "张三", a)
    g.register("u2", "李四", np.random.rand(512).astype(np.float32))

    m = g.match(a)
    assert m.user_id == "u1" and m.similarity > 0.999, "注册向量应能精确匹配"
    assert len(g.list_users()) == 2
    assert g.delete("u2") and not g.delete("u2")

    # 持久化：重新加载后数据仍在
    g2 = FaceGallery(path)
    assert len(g2.list_users()) == 1 and g2.match(a).similarity > 0.999
    print("[OK] gallery：注册/匹配/删除/持久化/自适应阈值")


def test_voting():
    r = VotingRecognizer(window_size=5, confirm_ratio=0.6, reject_threshold=0.3)
    # 连续 5 帧同一身份 → 确认
    res = None
    for _ in range(5):
        res = r.update(_M("u1", 0.8), 0.0)
    assert res.status is Status.CONFIRMED and res.user_id == "u1"

    # 陌生人（低相似度）连续出现 → unknown
    r.reset()
    for _ in range(5):
        res = r.update(_M("u2", 0.1), 0.0)
    assert res.status is Status.UNKNOWN and res.user_id is None

    # 人脸丢失 → 窗口清空
    r.reset()
    for _ in range(3):
        r.update(_M("u1", 0.8), 0.0)
    assert r.update(None, 0.0).status is Status.LOST
    assert r.update(_M("u1", 0.8), 0.0).status is Status.PENDING  # 窗口已清空重新累计

    # 单帧模式（消融）
    r2 = VotingRecognizer(window_size=5, confirm_ratio=0.6, reject_threshold=0.3, enabled=False)
    assert r2.update(_M("u1", 0.8), 0.0).status is Status.CONFIRMED
    print("[OK] voting：确认/陌生人/丢失/单帧消融模式")


def test_models():
    import cv2

    from face_engine.detector import FaceDetector
    from face_engine.embedder import FaceEmbedder

    config = load_config()
    det_path, emb_path = config["model"]["detector"], config["model"]["embedder"]
    root = Path(__file__).resolve().parent.parent
    for p in (det_path, emb_path):
        if not (root / p).exists():
            print(f"[跳过] 未找到模型 {p}，无法测试推理（见 models/README.md）")
            return

    det = FaceDetector(str(root / det_path))
    blank = np.zeros((480, 640, 3), dtype=np.uint8)
    assert det.detect(blank) == [], "空白图像不应检测到人脸"
    print("[OK] detector：模型加载 + 空白图无检测")

    emb = FaceEmbedder(str(root / emb_path))
    vec = emb.embed(np.zeros((112, 112, 3), dtype=np.uint8))
    assert vec.shape == (512,) and abs(float(np.linalg.norm(vec)) - 1.0) < 1e-3
    print("[OK] embedder：模型加载 + 输出 512 维归一化向量")


def main():
    parser = argparse.ArgumentParser(description="冒烟测试")
    parser.add_argument("--with-models", action="store_true")
    args = parser.parse_args()
    test_gallery()
    test_voting()
    if args.with_models:
        test_models()
    print("冒烟测试全部通过")


if __name__ == "__main__":
    main()
