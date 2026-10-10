"""FaceEngine：人脸识别核心引擎（流水线组装）

流水线：采集 → 检测 → 质量门控 → 对齐 → 特征提取 → 库比对 → 多帧投票 → 结果
其中"检测、特征提取"调用开源模型推理，其余环节均为自主设计。

性能优化（本组改进点）：帧间结果复用——若上一帧成功识别且未丢失人脸，
则每 embed_every 帧才重新走完整流水线，其余帧直接复用上一帧结果。
摄像头画面相邻帧变化很小，此策略在不明显损失准确率的前提下，
将特征提取计算量降低约一半（效果见视频日志对比实验）。
"""

import time
from typing import Dict, List, Optional

import numpy as np

from .detector import FaceDetector
from .embedder import FaceEmbedder
from .gallery import FaceGallery, GalleryMatch
from .recognizer import FaceResult, Status, VotingRecognizer


class FaceEngine:
    def __init__(self, config: Dict):
        m = config["model"]
        self.detector = FaceDetector(
            m["detector"],
            input_size=tuple(m.get("detect_input_size", [320, 320])),
            score_threshold=m.get("score_threshold", 0.6),
            nms_threshold=m.get("nms_threshold", 0.3),
            top_k=m.get("top_k", 1),
            quality_config=config.get("quality_gate", {}),
            enhance_config=config.get("detection", {}).get("enhance", {}),
            normalize_config=config.get("preprocess", {}),
            alignment_config=config.get("alignment", {}),
        )
        self.embedder = FaceEmbedder(
            m["embedder"],
            norm=m.get("embed_input_norm", -1.0),
            threads=config.get("pipeline", {}).get("threads", None),
        )
        self.gallery = FaceGallery(
            config["gallery"]["path"],
            merge_strategy=config["gallery"].get("merge_strategy", "max"),
            adaptive=config["gallery"].get("adaptive_threshold", {}),
        )
        v = config.get("voting", {})
        self.recognizer = VotingRecognizer(
            window_size=v.get("window_size", 7),
            confirm_ratio=v.get("confirm_ratio", 0.7),
            reject_threshold=v.get("reject_threshold", 0.30),
            enabled=v.get("enabled", True),
        )
        self._embed_every = config.get("pipeline", {}).get("embed_every", 2)
        self._frame_counter = 0
        self._last_result: Optional[FaceResult] = None

    # ---------- 注册 ----------

    def register(self, user_id: str, name: str, images: List[np.ndarray]) -> int:
        """原始照片注册（HTTP 接口用）：检测 → 质量门控 → 对齐 → 特征 → 入库"""
        count = 0
        for img in images:
            faces = self.detector.detect(img)
            if not faces:
                continue
            emb = self.embedder.embed(faces[0].image)
            self.gallery.register(user_id, name, emb)
            count += 1
        return count

    def register_aligned(self, user_id: str, name: str, face_crops: List[np.ndarray]) -> int:
        """已对齐的人脸裁剪图（112x112，来自 detector.detect）直接入库"""
        count = 0
        for img in face_crops:
            self.gallery.register(user_id, name, self.embedder.embed(img))
            count += 1
        return count

    # ---------- 识别 ----------

    def recognize_frame(self, frame: np.ndarray, force: bool = False) -> FaceResult:
        """单帧识别（含多帧投票决策），配合帧间复用策略"""
        timestamp = time.time()
        reuse_ok = (
            self._last_result is not None
            and self._last_result.status is not Status.LOST
        )
        if not force and self._frame_counter % self._embed_every != 0 and reuse_ok:
            # 帧间复用：直接返回上一帧结果（跳帧策略）
            self._frame_counter += 1
            return self._last_result
        self._frame_counter += 1

        faces = self.detector.detect(frame)
        if not faces:
            self.recognizer.reset()
            self._last_result = FaceResult(Status.LOST, timestamp=timestamp)
            return self._last_result

        # 签到场景设计为单人识别：多人同框时取检测置信度最高者（top_k 由配置控制）
        face = faces[0]
        emb = self.embedder.embed(face.image)
        result = self._decide(self.gallery.match(emb), timestamp)
        result.bbox = face.bbox
        result.quality = face.quality
        self._last_result = result
        return result

    def recognize_single(self, frame: np.ndarray) -> FaceResult:
        """单张图片识别（不投票，用于 HTTP 接口 / 数据集评估）"""
        timestamp = time.time()
        faces = self.detector.detect(frame)
        if not faces:
            return FaceResult(Status.LOST, timestamp=timestamp)
        face = faces[0]
        emb = self.embedder.embed(face.image)
        result = self._decide_single(self.gallery.match(emb), timestamp)
        result.bbox = face.bbox
        result.quality = face.quality
        return result

    def _decide(self, match: GalleryMatch, timestamp: float) -> FaceResult:
        return self.recognizer.update(match, timestamp)

    def _decide_single(self, match: GalleryMatch, timestamp: float) -> FaceResult:
        if match.user_id is None:
            return FaceResult(Status.UNKNOWN, None, None, match.similarity,
                              match.threshold, timestamp=timestamp)
        if match.similarity < self.recognizer.reject_threshold:
            return FaceResult(Status.UNKNOWN, None, None, match.similarity,
                              match.threshold, timestamp=timestamp)
        return FaceResult(Status.CONFIRMED, match.user_id, match.name,
                          match.similarity, match.threshold, timestamp=timestamp)

    def reset(self) -> None:
        self.recognizer.reset()
        self._last_result = None

    # ---------- 用户管理 ----------

    def delete_user(self, user_id: str) -> bool:
        return self.gallery.delete(user_id)

    def list_users(self) -> List[dict]:
        return self.gallery.list_users()
