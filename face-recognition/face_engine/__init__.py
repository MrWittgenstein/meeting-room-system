"""会议室人脸识别引擎（方案 B：YuNet 检测 + MobileFaceNet 特征，树莓派本地端到端）"""

from .engine import FaceEngine

__all__ = ["FaceEngine"]
