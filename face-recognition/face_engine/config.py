"""配置加载工具：兼容从任意目录运行，并将相对路径解析为绝对路径"""

import os
from typing import Dict

import yaml


def _abs(base: str, p: str) -> str:
    return p if os.path.isabs(p) else os.path.join(base, p)


def load_config(path: str = "config.yaml") -> Dict:
    if not os.path.exists(path):
        root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        path = os.path.join(root, path)
    path = os.path.abspath(path)
    base = os.path.dirname(path)

    with open(path, "r", encoding="utf-8") as f:
        cfg = yaml.safe_load(f)

    # 模型/人脸库相对路径统一基于 config.yaml 所在目录解析，
    # 避免"在哪个目录运行脚本"影响文件查找
    cfg.setdefault("model", {})["detector"] = _abs(base, cfg["model"].get("detector", ""))
    cfg["model"]["embedder"] = _abs(base, cfg["model"].get("embedder", ""))
    cfg.setdefault("gallery", {})["path"] = _abs(base, cfg["gallery"].get("path", "data/gallery.json"))
    return cfg
