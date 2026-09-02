from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import cv2
import numpy as np

from .config import (
    DEFAULT_COSINE_THRESHOLD,
    DEFAULT_DETECTOR_SCORE_THRESHOLD,
    DEFAULT_L2_THRESHOLD,
    DEFAULT_NMS_THRESHOLD,
    DEFAULT_TOP_K,
)


@dataclass
class FaceMatch:
    name: str
    score: float
    is_known: bool
    face: np.ndarray


class FaceEngine:
    def __init__(
        self,
        yunet_model: str | Path,
        sface_model: str | Path,
        detector_score_threshold: float = DEFAULT_DETECTOR_SCORE_THRESHOLD,
        detector_nms_threshold: float = DEFAULT_NMS_THRESHOLD,
        detector_top_k: int = DEFAULT_TOP_K,
        metric: str = "cosine",
        match_threshold: float | None = None,
        detection_min_long_side: int = 960,
        detection_max_long_side: int = 1280,
        rotation_angles: tuple[float, ...] = (-12.0, 12.0),
        merge_iou_threshold: float = 0.35,
    ) -> None:
        self.metric = metric.lower()
        if self.metric not in {"cosine", "l2"}:
            raise ValueError("metric only supports 'cosine' or 'l2'")

        self.match_threshold = match_threshold
        if self.match_threshold is None:
            self.match_threshold = (
                DEFAULT_COSINE_THRESHOLD if self.metric == "cosine" else DEFAULT_L2_THRESHOLD
            )

        self.detector_nms_threshold = float(detector_nms_threshold)
        self.detector_top_k = int(detector_top_k)
        self.detector_score_threshold = float(detector_score_threshold)
        self.detector = self._create_detector(yunet_model, self.detector_score_threshold)
        self.fallback_detector = self._create_detector(
            yunet_model,
            max(0.45, self.detector_score_threshold - 0.15),
        )
        self.recognizer = cv2.FaceRecognizerSF.create(str(sface_model), "")
        self.min_detection_long_side = int(max(320, detection_min_long_side))
        self.max_detection_long_side = int(max(self.min_detection_long_side, detection_max_long_side))
        self.rotation_angles = tuple(float(angle) for angle in rotation_angles)
        self.merge_iou_threshold = float(merge_iou_threshold)
        self._clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))

    def detect_faces(self, frame: np.ndarray) -> list[np.ndarray]:
        if frame is None or frame.size == 0:
            return []

        prepared_frame, resize_matrix = self._resize_for_detection(frame)
        detections = self._detect_with_transform(
            self.detector,
            prepared_frame,
            resize_matrix,
            frame.shape,
        )

        enhanced_frame = None
        if not detections or self._needs_recall_boost(frame, detections):
            enhanced_frame = self._enhance_for_detection(prepared_frame)
            detections.extend(
                self._detect_with_transform(
                    self.fallback_detector,
                    enhanced_frame,
                    resize_matrix,
                    frame.shape,
                )
            )

        if not detections:
            rotation_source = enhanced_frame if enhanced_frame is not None else prepared_frame
            for angle in self.rotation_angles:
                rotated_frame, rotation_matrix = self._rotate_image(rotation_source, angle)
                detections.extend(
                    self._detect_with_transform(
                        self.fallback_detector,
                        rotated_frame,
                        rotation_matrix @ resize_matrix,
                        frame.shape,
                    )
                )

        return self._merge_faces(detections)

    def get_largest_face(self, frame: np.ndarray) -> np.ndarray:
        faces = self.detect_faces(frame)
        if not faces:
            raise ValueError("图片中没有检测到人脸。")
        return max(faces, key=lambda face: float(face[2] * face[3]))

    def extract_feature(self, frame: np.ndarray, face: np.ndarray) -> np.ndarray:
        aligned = self.recognizer.alignCrop(frame, face)
        feature = self.recognizer.feature(aligned)
        return self._normalize(feature)

    def identify(
        self,
        feature: np.ndarray,
        known_names: list[str],
        known_embeddings: list[np.ndarray],
    ) -> FaceMatch:
        if not known_names or not known_embeddings:
            return FaceMatch(name="Unknown", score=0.0, is_known=False, face=feature)

        best_name = "Unknown"
        best_score = -1.0 if self.metric == "cosine" else float("inf")
        for name, known_feature in zip(known_names, known_embeddings):
            score = float(self.recognizer.match(feature, known_feature, self._distance_type()))
            if self.metric == "cosine":
                if score > best_score:
                    best_name = name
                    best_score = score
            else:
                if score < best_score:
                    best_name = name
                    best_score = score

        is_known = (
            best_score >= self.match_threshold
            if self.metric == "cosine"
            else best_score <= self.match_threshold
        )
        return FaceMatch(
            name=best_name if is_known else "Unknown",
            score=best_score,
            is_known=is_known,
            face=feature,
        )

    def _create_detector(self, yunet_model: str | Path, score_threshold: float):
        return cv2.FaceDetectorYN.create(
            str(yunet_model),
            "",
            (320, 320),
            float(score_threshold),
            self.detector_nms_threshold,
            self.detector_top_k,
        )

    def _detect_with_transform(
        self,
        detector,
        frame: np.ndarray,
        transform_matrix: np.ndarray,
        original_shape: tuple[int, ...],
    ) -> list[np.ndarray]:
        height, width = frame.shape[:2]
        detector.setInputSize((width, height))
        _, faces = detector.detect(frame)
        if faces is None:
            return []

        inverse_matrix = np.linalg.inv(transform_matrix)
        return [
            self._restore_face(np.asarray(face, dtype=np.float32), inverse_matrix, original_shape)
            for face in faces
        ]

    def _resize_for_detection(self, frame: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
        height, width = frame.shape[:2]
        long_side = max(height, width)
        if long_side == 0:
            return frame, np.eye(3, dtype=np.float32)

        target_long_side = int(np.clip(long_side, self.min_detection_long_side, self.max_detection_long_side))
        scale = target_long_side / float(long_side)
        if abs(scale - 1.0) < 1e-3:
            return frame, np.eye(3, dtype=np.float32)

        new_width = max(1, int(round(width * scale)))
        new_height = max(1, int(round(height * scale)))
        interpolation = cv2.INTER_CUBIC if scale > 1.0 else cv2.INTER_AREA
        resized = cv2.resize(frame, (new_width, new_height), interpolation=interpolation)
        matrix = np.array(
            [[scale, 0.0, 0.0], [0.0, scale, 0.0], [0.0, 0.0, 1.0]],
            dtype=np.float32,
        )
        return resized, matrix

    def _enhance_for_detection(self, frame: np.ndarray) -> np.ndarray:
        ycrcb = cv2.cvtColor(frame, cv2.COLOR_BGR2YCrCb)
        y_channel, cr_channel, cb_channel = cv2.split(ycrcb)
        enhanced_y = self._clahe.apply(y_channel)
        enhanced = cv2.merge((enhanced_y, cr_channel, cb_channel))
        return cv2.cvtColor(enhanced, cv2.COLOR_YCrCb2BGR)

    def _rotate_image(self, frame: np.ndarray, angle: float) -> tuple[np.ndarray, np.ndarray]:
        height, width = frame.shape[:2]
        center = (width / 2.0, height / 2.0)
        rotation = cv2.getRotationMatrix2D(center, angle, 1.0)
        cosine = abs(rotation[0, 0])
        sine = abs(rotation[0, 1])

        bound_width = int(round(height * sine + width * cosine))
        bound_height = int(round(height * cosine + width * sine))
        rotation[0, 2] += bound_width / 2.0 - center[0]
        rotation[1, 2] += bound_height / 2.0 - center[1]

        rotated = cv2.warpAffine(
            frame,
            rotation,
            (bound_width, bound_height),
            flags=cv2.INTER_LINEAR,
            borderMode=cv2.BORDER_REPLICATE,
        )
        matrix = np.vstack((rotation, np.array([0.0, 0.0, 1.0], dtype=np.float32))).astype(
            np.float32
        )
        return rotated, matrix

    def _restore_face(
        self,
        face: np.ndarray,
        inverse_matrix: np.ndarray,
        original_shape: tuple[int, ...],
    ) -> np.ndarray:
        restored = face.astype(np.float32, copy=True)
        original_height, original_width = original_shape[:2]
        bbox = np.array(
            [
                [restored[0], restored[1]],
                [restored[0] + restored[2], restored[1]],
                [restored[0], restored[1] + restored[3]],
                [restored[0] + restored[2], restored[1] + restored[3]],
            ],
            dtype=np.float32,
        )
        bbox = self._transform_points(bbox, inverse_matrix)
        x_min = float(np.clip(np.min(bbox[:, 0]), 0.0, max(original_width - 1, 0)))
        y_min = float(np.clip(np.min(bbox[:, 1]), 0.0, max(original_height - 1, 0)))
        x_max = float(np.clip(np.max(bbox[:, 0]), x_min + 1.0, float(original_width)))
        y_max = float(np.clip(np.max(bbox[:, 1]), y_min + 1.0, float(original_height)))
        restored[:4] = np.array([x_min, y_min, x_max - x_min, y_max - y_min], dtype=np.float32)

        landmarks = restored[4:14].reshape(-1, 2)
        landmarks = self._transform_points(landmarks, inverse_matrix)
        landmarks[:, 0] = np.clip(landmarks[:, 0], 0.0, max(original_width - 1, 0))
        landmarks[:, 1] = np.clip(landmarks[:, 1], 0.0, max(original_height - 1, 0))
        restored[4:14] = landmarks.reshape(-1)
        return restored

    def _needs_recall_boost(self, frame: np.ndarray, detections: list[np.ndarray]) -> bool:
        if not detections:
            return True

        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        if float(np.mean(gray)) < 85.0:
            return True

        frame_area = float(frame.shape[0] * frame.shape[1])
        largest_face_area = max(float(face[2] * face[3]) for face in detections)
        largest_face_side = max(float(max(face[2], face[3])) for face in detections)
        return len(detections) < 2 and (
            largest_face_area / max(frame_area, 1.0) < 0.05 or largest_face_side < 96.0
        )

    def _merge_faces(self, detections: list[np.ndarray]) -> list[np.ndarray]:
        ordered = sorted(
            (np.asarray(face, dtype=np.float32) for face in detections if face[2] >= 8 and face[3] >= 8),
            key=lambda face: float(face[-1]),
            reverse=True,
        )
        merged: list[np.ndarray] = []
        for candidate in ordered:
            if any(self._iou(candidate, existing) >= self.merge_iou_threshold for existing in merged):
                continue
            merged.append(candidate)
        return merged

    @staticmethod
    def _transform_points(points: np.ndarray, matrix: np.ndarray) -> np.ndarray:
        homogeneous = np.concatenate(
            [np.asarray(points, dtype=np.float32), np.ones((len(points), 1), dtype=np.float32)],
            axis=1,
        )
        transformed = (matrix @ homogeneous.T).T
        scale = np.where(transformed[:, 2:3] == 0, 1.0, transformed[:, 2:3])
        return transformed[:, :2] / scale

    @staticmethod
    def _iou(face_a: np.ndarray, face_b: np.ndarray) -> float:
        ax1, ay1, aw, ah = map(float, face_a[:4])
        bx1, by1, bw, bh = map(float, face_b[:4])
        ax2, ay2 = ax1 + aw, ay1 + ah
        bx2, by2 = bx1 + bw, by1 + bh

        inter_x1 = max(ax1, bx1)
        inter_y1 = max(ay1, by1)
        inter_x2 = min(ax2, bx2)
        inter_y2 = min(ay2, by2)
        inter_w = max(0.0, inter_x2 - inter_x1)
        inter_h = max(0.0, inter_y2 - inter_y1)
        inter_area = inter_w * inter_h
        if inter_area <= 0:
            return 0.0

        area_a = aw * ah
        area_b = bw * bh
        union = area_a + area_b - inter_area
        if union <= 0:
            return 0.0
        return inter_area / union

    def _distance_type(self) -> int:
        if self.metric == "cosine":
            return cv2.FaceRecognizerSF_FR_COSINE
        return cv2.FaceRecognizerSF_FR_NORM_L2

    @staticmethod
    def _normalize(feature: np.ndarray) -> np.ndarray:
        vector = np.asarray(feature, dtype=np.float32).reshape(1, -1)
        norm = np.linalg.norm(vector)
        if norm == 0:
            return vector
        return vector / norm
