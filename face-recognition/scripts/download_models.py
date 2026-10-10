"""模型自动下载（YuNet + MobileFaceNet）

用法：
    python scripts/download_models.py                 # 下载缺失的模型（默认走 gh-proxy 镜像）
    python scripts/download_models.py --force         # 覆盖已有模型
    python scripts/download_models.py --mirror ""     # 网络可直连 GitHub 时去掉镜像前缀
"""

import argparse
import shutil
import sys
import time
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MODELS_DIR = ROOT / "models"

YUNET_URL = (
    "https://github.com/opencv/opencv_zoo/raw/main/models/"
    "face_detection_yunet/face_detection_yunet_2023mar.onnx"
)
YUNET_NAME = "face_detection_yunet_2023mar.onnx"

MBF_ZIP_URL = (
    "https://github.com/deepinsight/insightface/releases/download/v0.7/buffalo_s.zip"
)
MBF_IN_ZIP = "w600k_mbf.onnx"
MBF_NAME = "w600k_mbf.onnx"

DEFAULT_MIRROR = "https://gh-proxy.com/"


def download(url: str, target: Path, force: bool, attempts: int = 3) -> None:
    if target.exists() and not force:
        print(f"[跳过] {target.name} 已存在")
        return

    target.parent.mkdir(parents=True, exist_ok=True)
    print(f"[下载] {target.name} <- {url}")
    headers = {"User-Agent": "Mozilla/5.0"}
    for attempt in range(1, attempts + 1):
        try:
            request = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(request, timeout=60) as response, target.open("wb") as f:
                shutil.copyfileobj(response, f)
            print(f"[完成] {target.name}（{target.stat().st_size / 1e6:.1f} MB）")
            return
        except Exception as exc:
            print(f"[重试] {target.name} 第 {attempt} 次失败: {exc}")
            if target.exists():
                target.unlink()
            time.sleep(2)
    raise RuntimeError(f"{target.name} 下载失败")


def main():
    parser = argparse.ArgumentParser(description="下载 YuNet 与 MobileFaceNet 模型")
    parser.add_argument("--force", action="store_true", help="覆盖已存在的模型")
    parser.add_argument("--mirror", default=DEFAULT_MIRROR, help="GitHub 镜像前缀（空串表示直连）")
    args = parser.parse_args()
    prefix = args.mirror

    download(prefix + YUNET_URL, MODELS_DIR / YUNET_NAME, args.force)

    mbf_path = MODELS_DIR / MBF_NAME
    if mbf_path.exists() and not args.force:
        print(f"[跳过] {MBF_NAME} 已存在")
    else:
        zip_path = MODELS_DIR / "buffalo_s.zip"
        download(prefix + MBF_ZIP_URL, zip_path, True)
        print("[解压] 提取 w600k_mbf.onnx ...")
        with zipfile.ZipFile(zip_path) as z:
            matches = [n for n in z.namelist() if n.endswith(MBF_IN_ZIP)]
            if not matches:
                zip_path.unlink(missing_ok=True)
                raise RuntimeError(f"压缩包内未找到 {MBF_IN_ZIP}")
            with z.open(matches[0]) as src, open(mbf_path, "wb") as dst:
                shutil.copyfileobj(src, dst)
        zip_path.unlink(missing_ok=True)
        print(f"[完成] {MBF_NAME}（{mbf_path.stat().st_size / 1e6:.1f} MB）")

    print("模型就绪。可运行: python scripts/smoke_test.py --with-models 自检")


if __name__ == "__main__":
    main()
