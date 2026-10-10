"""人脸库模块

自主设计内容（报告中"本组改进内容"素材）：
1. 库结构设计：每人可存储多个特征向量（多角度注册），比对时取与库中该人
   所有向量的最大相似度（merge=max），相比"每人单向量"显著提升姿态/光照鲁棒性
2. 向量化比对：注册/变更时预构建全库归一化矩阵，识别时一次矩阵乘法完成
   与所有人的比对（对比逐人循环，n 人提速约 n 倍）——性能优化点
3. 自适应阈值（本组改进点）：按每个用户注册向量之间的相似度分布计算个性化
   阈值 threshold = clamp(mean - k*std, global_min, global_max)。
   注册质量好（向量一致）的用户阈值收紧，注册质量差的用户阈值放宽，
   替代"全局一刀切"阈值
4. 持久化：JSON 明文存储便于调试与迁移；读写加锁保证服务场景线程安全
"""

import json
import os
import threading
from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple

import numpy as np


@dataclass
class GalleryMatch:
    user_id: Optional[str]
    name: Optional[str]
    similarity: float
    threshold: float                      # 该用户的自适应阈值（判定用）
    top_k: List[Tuple[str, float]]        # 相似度最高的前几名（调试/报告素材）


class FaceGallery:
    def __init__(
        self,
        path: str,
        merge_strategy: str = "max",
        adaptive: Optional[dict] = None,
    ):
        self.path = path
        self.merge = merge_strategy
        self.adaptive = adaptive or {}
        self._lock = threading.Lock()
        self._users: Dict[str, dict] = {}            # user_id -> {"name": str, "embeddings": [[...]]}
        self._matrix: Optional[np.ndarray] = None    # Nx512 归一化矩阵（比对缓存）
        self._index: List[str] = []                  # 矩阵行号 -> user_id
        self._thresholds: Dict[str, float] = {}      # user_id -> 自适应阈值
        self._load()

    # ---------- 用户管理 ----------

    def register(self, user_id: str, name: str, embedding: np.ndarray) -> None:
        with self._lock:
            user = self._users.setdefault(user_id, {"name": name, "embeddings": []})
            user["embeddings"].append(embedding.tolist())
            self._save_locked()
            self._rebuild_locked()

    def delete(self, user_id: str) -> bool:
        with self._lock:
            if user_id not in self._users:
                return False
            del self._users[user_id]
            self._save_locked()
            self._rebuild_locked()
            return True

    def list_users(self) -> List[dict]:
        with self._lock:
            return [
                {
                    "user_id": uid,
                    "name": u["name"],
                    "embedding_count": len(u["embeddings"]),
                }
                for uid, u in self._users.items()
            ]

    # ---------- 比对 ----------

    def match(self, embedding: np.ndarray) -> GalleryMatch:
        emb = embedding / (np.linalg.norm(embedding) + 1e-8)
        with self._lock:
            if self._matrix is None or len(self._index) == 0:
                return GalleryMatch(None, None, 0.0, 0.0, [])
            sims = self._matrix @ emb  # 向量化余弦相似度

            if self.merge == "mean":
                # 备选策略：按用户聚合后取平均相似度
                per_user: Dict[str, List[float]] = {}
                for i, uid in enumerate(self._index):
                    per_user.setdefault(uid, []).append(float(sims[i]))
                agg = {uid: float(np.mean(v)) for uid, v in per_user.items()}
                best_uid = max(agg, key=agg.get)
                best_sim = agg[best_uid]
            else:
                # max：取库中任一注册向量与当前向量的最高相似度
                best_i = int(np.argmax(sims))
                best_uid, best_sim = self._index[best_i], float(sims[best_i])

            order = np.argsort(-sims)
            top_k = [(self._index[i], float(sims[i])) for i in order[:3]]

        return GalleryMatch(
            best_uid,
            self._users[best_uid]["name"],
            best_sim,
            self._thresholds.get(best_uid, self._global_threshold()),
            top_k,
        )

    # ---------- 持久化与缓存 ----------

    def _load(self) -> None:
        if os.path.exists(self.path):
            with open(self.path, "r", encoding="utf-8") as f:
                data = json.load(f)
            self._users = data.get("users", {})
        self._rebuild_locked()

    def _save_locked(self) -> None:
        os.makedirs(os.path.dirname(self.path), exist_ok=True)
        with open(self.path, "w", encoding="utf-8") as f:
            json.dump({"users": self._users}, f, ensure_ascii=False, indent=2)

    def _rebuild_locked(self) -> None:
        matrix, index = [], []
        for uid, user in self._users.items():
            for emb in user["embeddings"]:
                arr = np.asarray(emb, dtype=np.float32)
                matrix.append(arr / (np.linalg.norm(arr) + 1e-8))
                index.append(uid)
            self._thresholds[uid] = self._adaptive_threshold(user["embeddings"])
        self._matrix = np.stack(matrix) if matrix else None
        self._index = index

    # ---------- 自适应阈值（本组改进点） ----------

    def _global_threshold(self) -> float:
        return (
            self.adaptive.get("global_min", 0.35) + self.adaptive.get("global_max", 0.55)
        ) / 2

    def _adaptive_threshold(self, embeddings: List[list]) -> float:
        cfg = self.adaptive
        if not cfg.get("enabled", True) or len(embeddings) < 2:
            return self._global_threshold()
        arr = np.stack([np.asarray(e, dtype=np.float32) for e in embeddings])
        arr = arr / (np.linalg.norm(arr, axis=-1, keepdims=True) + 1e-8)
        sims = (arr @ arr.T)[np.triu_indices(len(arr), k=1)]  # 注册向量两两相似度
        threshold = float(sims.mean() - cfg.get("k", 2.0) * sims.std())
        return float(
            np.clip(threshold, cfg.get("global_min", 0.35), cfg.get("global_max", 0.55))
        )
