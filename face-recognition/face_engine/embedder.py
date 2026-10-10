"""特征提取模块

依赖库说明：
- ONNX Runtime：加载 MobileFaceNet ONNX 模型并执行推理，输入 112x112 对齐人脸，
  输出 512 维人脸特征向量。选择 ONNX Runtime 的原因：跨平台（Windows 开发 /
  ARM 树莓派部署同一套代码）、CPU 推理快、无需在树莓派上编译 C++ 扩展。

本模块的自主设计内容：
1. 预处理流程：BGR→RGB、数值归一化（[-1,1]，与 insightface 预处理一致）、
   HWC→NCHW 布局转换——这些步骤库不提供现成封装，由本组实现
2. 特征 L2 归一化：把人脸向量映射到单位超球面，使余弦相似度与欧氏距离等价，
   比对时可直接用矩阵乘法批量计算
"""

from typing import List, Optional

import cv2
import numpy as np
import onnxruntime as ort


class FaceEmbedder:
    def __init__(self, model_path: str, norm: float = -1.0, threads: Optional[int] = None):
        options = ort.SessionOptions()
        if threads:
            options.intra_op_num_threads = threads
        self._session = ort.InferenceSession(
            model_path, sess_options=options, providers=["CPUExecutionProvider"]
        )
        self._input_name = self._session.get_inputs()[0].name
        # norm=-1 表示归一化到 [-1,1]（insightface w600k_mbf 等模型）；
        # 个别转换版模型要求 [0,1]，改为 0 即可
        self.norm = norm

    def embed(self, face_bgr: np.ndarray) -> np.ndarray:
        """单张 112x112 人脸 → 512 维归一化特征向量"""
        blob = self._preprocess(face_bgr)[np.newaxis, ...]
        out = self._session.run(None, {self._input_name: blob})[0]
        return self._l2_normalize(out[0])

    def embed_batch(self, faces: List[np.ndarray]) -> np.ndarray:
        """批量推理（注册多张照片时使用），返回 Nx512 归一化矩阵"""
        batch = np.stack([self._preprocess(f) for f in faces])
        out = self._session.run(None, {self._input_name: batch})[0]
        return self._l2_normalize(out)

    def _preprocess(self, face_bgr: np.ndarray) -> np.ndarray:
        img = cv2.cvtColor(face_bgr, cv2.COLOR_BGR2RGB).astype(np.float32)
        if self.norm < 0:
            img = img / 127.5 - 1.0
        else:
            img = img / 255.0
        return img.transpose(2, 0, 1)  # 3x112x112

    @staticmethod
    def _l2_normalize(vectors: np.ndarray) -> np.ndarray:
        norms = np.linalg.norm(vectors, axis=-1, keepdims=True)
        norms[norms == 0] = 1e-8
        return vectors / norms
