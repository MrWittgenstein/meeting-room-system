from __future__ import annotations

from pathlib import Path

import numpy as np


def save_database(path: str | Path, names: list[str], embeddings: list[np.ndarray]) -> None:
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    if not names or not embeddings:
        raise ValueError("人脸库为空，无法保存。")

    stacked = np.vstack([np.asarray(vector, dtype=np.float32).reshape(1, -1) for vector in embeddings])
    np.savez_compressed(target, names=np.asarray(names, dtype=str), embeddings=stacked)


def load_database(path: str | Path) -> tuple[list[str], list[np.ndarray]]:
    source = Path(path)
    if not source.exists():
        raise FileNotFoundError(f"未找到人脸库文件: {source}")

    data = np.load(source, allow_pickle=False)
    names = [str(name) for name in data["names"].tolist()]
    embeddings = [np.asarray(vector, dtype=np.float32).reshape(1, -1) for vector in data["embeddings"]]
    return names, embeddings
