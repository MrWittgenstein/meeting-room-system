"""人脸检测与对齐模块

依赖库说明（报告中"库的作用与调用流程"素材）：
- OpenCV 的 cv2.FaceDetectorYN：加载 YuNet ONNX 模型完成人脸检测，输出
  人脸框 (x, y, w, h)、5 点关键点（右眼、左眼、鼻尖、右嘴角、左嘴角）和置信度。
- cv2.estimateAffinePartial2D / cv2.warpAffine：估算相似变换（旋转缩放平移），
  将检测到的人脸对齐到 112x112 标准模板。
- cv2.createCLAHE：受限对比度自适应直方图均衡，用于暗光回退检测。

本模块的自主设计内容：
1. 检测参数选型：输入尺寸、置信度阈值、NMS 阈值、top_k（见 config.yaml）
2. 检测结果过滤：人脸最小尺寸、画面边缘剔除
3. 质量评估门控（本组改进点）：模糊度 / 亮度 / 人脸占比三项检查，
   低质量帧直接丢弃——既提高后续识别准确率，又节省特征提取算力
4. 对齐流程设计：YuNet 5 点关键点 → 相似变换 → 标准模板裁剪
5. 暗光/侧脸检测增强回退链（本组改进点）：主检测无结果时依次尝试
   CLAHE 增强 + 低阈值检测器、±12° 旋转检测，最后经 IoU 去重合并。
   注意：CLAHE 只改像素不改几何（框坐标 1:1 映射回原图）；
   旋转检测需把框和关键点经逆旋转矩阵映射回原图坐标。
6. 输入归一化（本组改进点）：长边超过上限时先等比缩小再检测，检测结果
   再映射回原图坐标。理由：YuNet 的锚框尺度与输入分辨率绑定，把 3072×4096
   的手机原图直接送进去，同一张脸的置信度会从 0.94 掉到 0.68，框也会明显偏小
   （实测框面积只有准确值的 27%），进而被后续的人脸占比过滤误杀。
   归一化后置信度与框尺寸都恢复正常；且人脸占比是与分辨率无关的比例量，
   归一化让"人脸够不够大"这条阈值在手机照片和摄像头画面之间变得可比。
"""

from dataclasses import dataclass, field
from typing import List, Optional, Tuple

import cv2
import numpy as np

# 112x112 标准人脸模板（与 insightface 一致）：5 点依次为
# [右眼, 左眼, 鼻尖, 右嘴角, 左嘴角]（画面左→右顺序），与 YuNet 关键点输出顺序一一对应
_ALIGN_TEMPLATE = np.array(
    [
        [38.2946, 51.6963],
        [73.5318, 51.5014],
        [56.0252, 71.7366],
        [41.5493, 92.3655],
        [70.7299, 92.2041],
    ],
    dtype=np.float32,
)


@dataclass
class AlignedFace:
    """对齐后的单张人脸及其元信息"""

    image: np.ndarray                          # 112x112x3 BGR
    bbox: Tuple[int, int, int, int]            # 原图上的框 (x, y, w, h)
    score: float                               # 检测置信度
    quality: dict = field(default_factory=dict)  # 质量评估指标（报告/调试素材）


class FaceDetector:
    """YuNet 人脸检测 + 增强回退链 + 质量门控 + 对齐"""

    def __init__(
        self,
        model_path: str,
        input_size: Tuple[int, int] = (320, 320),
        score_threshold: float = 0.6,
        nms_threshold: float = 0.3,
        top_k: int = 1,
        quality_config: Optional[dict] = None,
        enhance_config: Optional[dict] = None,
        normalize_config: Optional[dict] = None,
        alignment_config: Optional[dict] = None,
    ):
        self._detector = cv2.FaceDetectorYN.create(
            model_path, "", input_size, score_threshold, nms_threshold, top_k
        )
        self.qc = quality_config or {}
        self.ec = enhance_config or {}
        self.norm = normalize_config or {}
        self.align = alignment_config or {}
        # 回退检测器：阈值更低，用于增强/旋转后的兜底检测
        fallback_score = max(
            0.45, score_threshold - self.ec.get("fallback_score_drop", 0.15)
        )
        self._fallback = cv2.FaceDetectorYN.create(
            model_path, "", input_size, fallback_score, nms_threshold, top_k
        )
        self._clahe = cv2.createCLAHE(
            clipLimit=self.ec.get("clahe_clip", 2.0), tileGridSize=(8, 8)
        )

    def detect(self, frame: np.ndarray) -> List[AlignedFace]:
        """检测帧中的所有人脸，通过质量门控后对齐输出

        检测在归一化后的图像上进行（见 _normalize），结果映射回原图坐标后，
        对齐与质量评估仍在原图上做——对齐取原图能保留更多细节。
        """
        work, scale = self._normalize(frame)
        raw = self._detect_raw(work)
        if scale != 1.0:
            raw = [self._rescale_face(f, 1.0 / scale) for f in raw]

        results: List[AlignedFace] = []
        for face in raw:
            x, y, fw, fh = (int(v) for v in face[:4])
            landmarks = face[4:14].reshape(5, 2)
            score = float(face[14])
            bbox = (x, y, fw, fh)

            if not self._is_valid_face(frame, bbox):
                continue

            aligned = self._align(frame, landmarks)
            if aligned is None:
                continue

            if self.qc.get("enabled", True):
                quality = self._assess(aligned)
                if not self._quality_pass(quality):
                    continue
            else:
                quality = {}

            results.append(AlignedFace(image=aligned, bbox=bbox, score=score, quality=quality))

        results.sort(key=lambda r: r.score, reverse=True)
        return results

    # ---------- 输入归一化（本组改进点） ----------

    def _normalize(self, frame: np.ndarray):
        """长边超过上限时等比缩小，返回 (用于检测的图像, 相对原图的缩放比)

        只缩小不放大：摄像头画面（如 640×480）本来就够小，放大只会白费算力，
        而且人脸占比是与分辨率无关的比例量，不需要靠放大来"凑"占比。
        """
        if not self.norm.get("enabled", True):
            return frame, 1.0
        h, w = frame.shape[:2]
        limit = int(self.norm.get("max_long_side", 1280))
        long_side = max(h, w)
        if long_side <= limit:
            return frame, 1.0
        scale = limit / float(long_side)
        resized = cv2.resize(
            frame,
            (max(1, int(round(w * scale))), max(1, int(round(h * scale)))),
            interpolation=cv2.INTER_AREA,
        )
        return resized, scale

    @staticmethod
    def _rescale_face(face: np.ndarray, k: float) -> np.ndarray:
        """把检测结果（框 + 5 点关键点）按比例缩放回原图坐标

        归一化是绕原点等比缩放，没有平移，所以框和关键点各自乘 k 即可。
        """
        out = np.array(face, dtype=np.float32, copy=True)
        out[:4] *= k        # x, y, w, h
        out[4:14] *= k      # 5 个关键点的 x, y
        return out

    # ---------- 检测与回退链 ----------

    def _detect_raw(self, frame: np.ndarray) -> list:
        """主检测器 + 暗光/侧脸回退链（本组改进点）：主检测无结果才触发回退"""
        h, w = frame.shape[:2]
        self._detector.setInputSize((w, h))
        _, faces = self._detector.detect(frame)
        raw = [] if faces is None else [np.asarray(f, dtype=np.float32) for f in faces]

        if not raw and self.ec.get("enabled", True):
            enhanced = self._enhance(frame)                       # CLAHE 只改像素，坐标 1:1
            raw.extend(self._detect_on(self._fallback, enhanced))
            if not raw:
                for angle in self.ec.get("rotation_angles", [-12.0, 12.0]):
                    raw.extend(self._detect_rotated(self._fallback, enhanced, angle))
                    if raw:
                        break
            raw = self._merge(raw)
        return raw

    def _enhance(self, frame: np.ndarray) -> np.ndarray:
        """CLAHE 暗光增强：仅对 Y 通道做对比度均衡（几何不变，框坐标与原图 1:1）"""
        ycrcb = cv2.cvtColor(frame, cv2.COLOR_BGR2YCrCb)
        y, cr, cb = cv2.split(ycrcb)
        y = self._clahe.apply(y)
        return cv2.cvtColor(cv2.merge((y, cr, cb)), cv2.COLOR_YCrCb2BGR)

    def _detect_on(self, detector, frame: np.ndarray) -> list:
        h, w = frame.shape[:2]
        detector.setInputSize((w, h))
        _, faces = detector.detect(frame)
        return [] if faces is None else [np.asarray(f, dtype=np.float32) for f in faces]

    def _detect_rotated(self, detector, frame: np.ndarray, angle: float) -> list:
        """旋转画面检测，并把框/关键点经逆旋转矩阵映射回原图坐标"""
        h, w = frame.shape[:2]
        center = (w / 2.0, h / 2.0)
        matrix = cv2.getRotationMatrix2D(center, angle, 1.0)
        cos, sin = abs(matrix[0, 0]), abs(matrix[0, 1])
        bound_w = int(round(h * sin + w * cos))
        bound_h = int(round(h * cos + w * sin))
        matrix[0, 2] += bound_w / 2.0 - center[0]
        matrix[1, 2] += bound_h / 2.0 - center[1]

        rotated = cv2.warpAffine(
            frame, matrix, (bound_w, bound_h),
            flags=cv2.INTER_LINEAR, borderMode=cv2.BORDER_REPLICATE,
        )
        faces = self._detect_on(detector, rotated)
        if not faces:
            return []

        inverse = cv2.invertAffineTransform(matrix)
        restored = []
        for face in faces:
            f = np.asarray(face, dtype=np.float32)
            corners = np.array(
                [[f[0], f[1]], [f[0] + f[2], f[1]], [f[0], f[1] + f[3]], [f[0] + f[2], f[1] + f[3]]],
                dtype=np.float32,
            )
            pts = cv2.transform(corners.reshape(1, -1, 2), inverse).reshape(-1, 2)
            x_min, y_min = pts.min(axis=0)
            x_max, y_max = pts.max(axis=0)
            x_min, y_min = max(x_min, 0.0), max(y_min, 0.0)
            x_max, y_max = min(x_max, float(w)), min(y_max, float(h))
            if x_max - x_min < 8 or y_max - y_min < 8:
                continue
            landmarks = cv2.transform(f[4:14].reshape(1, -1, 2), inverse).reshape(-1)
            restored.append(
                np.concatenate([[x_min, y_min, x_max - x_min, y_max - y_min], landmarks, f[14:]])
            )
        return restored

    def _merge(self, detections: list, iou_threshold: float = 0.35) -> list:
        """按置信度降序做 IoU 去重，消除回退链产生的重叠框"""
        ordered = sorted(
            (f for f in detections if f[2] >= 8 and f[3] >= 8),
            key=lambda f: float(f[-1]),
            reverse=True,
        )
        merged: list = []
        for candidate in ordered:
            if any(self._iou(candidate, existing) >= iou_threshold for existing in merged):
                continue
            merged.append(candidate)
        return merged

    @staticmethod
    def _iou(face_a, face_b) -> float:
        ax1, ay1, aw, ah = map(float, face_a[:4])
        bx1, by1, bw, bh = map(float, face_b[:4])
        ax2, ay2, bx2, by2 = ax1 + aw, ay1 + ah, bx1 + bw, by1 + bh
        inter_w = max(0.0, min(ax2, bx2) - max(ax1, bx1))
        inter_h = max(0.0, min(ay2, by2) - max(ay1, by1))
        inter = inter_w * inter_h
        if inter <= 0:
            return 0.0
        union = aw * ah + bw * bh - inter
        return inter / union if union > 0 else 0.0

    # ---------- 过滤与质量评估 ----------

    def _is_valid_face(self, frame: np.ndarray, bbox: Tuple[int, int, int, int]) -> bool:
        """基础过滤：人脸尺寸与画面边缘

        几何完整性检查（框必须完整落在画面内）与质量门控无关，始终执行；
        "人脸占比"属于质量门控的一部分，随 quality_gate.enabled 一起开关——
        否则消融实验里"关掉质量门控"其实关不干净，占比过滤仍在偷偷剔除样本。
        """
        h, w = frame.shape[:2]
        x, y, fw, fh = bbox
        if fw <= 0 or fh <= 0:
            return False
        # 框的边缘应完整落在画面内（留 2px 余量，避免对齐时取到画面外）
        if x < 2 or y < 2 or x + fw > w - 2 or y + fh > h - 2:
            return False
        if not self.qc.get("enabled", True):
            return True
        # 人脸占比：太小的人脸特征信息不足
        ratio = (fw * fh) / (w * h)
        if ratio < self.qc.get("min_face_ratio", 0.05):
            return False
        return True

    def _align(self, frame: np.ndarray, landmarks: np.ndarray) -> Optional[np.ndarray]:
        """5 点关键点 → 相似变换 → 112x112 标准模板

        估计方法可切换（见 config.alignment），两种都保留用于消融对比：
        - lmeds（默认）：最小中值稳健估计，用全部 5 点求解，不需要人工设定阈值。
        - ransac：OpenCV 默认方法，要给出重投影阈值作为内点判据。该阈值在模板
          坐标系（跨度约 35~92）里取 3.0，折算到 3072×4096 的原图相当于要求
          关键点误差不超过约 28px——对高分辨率照片过严：实测 58 张里有 11 张
          明明检出了人脸，却因为只凑到 2 个内点被判"对齐失败"而丢弃，
          且四个对象都中招，属系统性误杀而非个例。

        注意：若发现对齐后嘴部错位，说明所用 YuNet 版本关键点顺序不同，
        将 landmarks 重排为 [1, 0, 2, 4, 3] 即可。
        """
        if self.align.get("method", "lmeds") == "ransac":
            transform, inliers = cv2.estimateAffinePartial2D(
                landmarks,
                _ALIGN_TEMPLATE,
                ransacReprojThreshold=self.align.get("ransac_threshold", 3.0),
            )
            if (transform is None or inliers is None
                    or int(inliers.sum()) < self.align.get("min_inliers", 3)):
                return None
        else:
            transform, _ = cv2.estimateAffinePartial2D(
                landmarks, _ALIGN_TEMPLATE, method=cv2.LMEDS
            )
            if transform is None:
                return None
        return cv2.warpAffine(
            frame, transform, (112, 112), flags=cv2.INTER_LINEAR, borderValue=0.0
        )

    def _assess(self, face: np.ndarray) -> dict:
        """质量评估（本组改进点）：输出指标字典"""
        gray = cv2.cvtColor(face, cv2.COLOR_BGR2GRAY)
        blur = float(cv2.Laplacian(gray, cv2.CV_64F).var())  # 拉普拉斯方差：越低越模糊
        brightness = float(gray.mean())
        return {"blur": blur, "brightness": brightness}

    def _quality_pass(self, q: dict) -> bool:
        if q["blur"] < self.qc.get("min_blur", 60.0):
            return False
        if q["brightness"] < self.qc.get("min_brightness", 40.0):
            return False
        if q["brightness"] > self.qc.get("max_brightness", 220.0):
            return False
        return True
