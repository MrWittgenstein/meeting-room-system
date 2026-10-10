"""识别决策模块（完全自主设计，无现成库封装，本组核心改进点）

问题：单帧比对结果噪声大——光照波动、姿态变化、检测框抖动都会导致
识别结果闪烁（身份跳变）甚至误识。

改进方法（多帧投票）：维护长度为 window_size 的滑动窗口，每个窗口位置记录
该帧的最佳匹配身份；当某身份在窗口内占比达到 confirm_ratio 时才输出"确认"；
人脸消失（检测不到）立即清空窗口，避免旧结果残留。
配置 voting.enabled=false 可回退为单帧直接输出，用于消融对比实验。

判定规则：
- 帧内最佳相似度 < reject_threshold → 记为该帧"陌生人"
- 窗口已满且某身份占比 ≥ confirm_ratio → confirmed（确认身份）
- 窗口已满且"陌生人"占比 ≥ confirm_ratio → unknown（陌生人）
- 其余情况 → pending（已匹配但证据不足，暂不确认）
"""

from collections import Counter, deque
from dataclasses import dataclass, field
from enum import Enum
from typing import Optional, Tuple


class Status(str, Enum):
    UNKNOWN = "unknown"      # 陌生人 / 未注册
    PENDING = "pending"      # 已匹配但证据不足
    CONFIRMED = "confirmed"  # 连续多帧一致，确认身份
    LOST = "lost"            # 本帧未检测到人脸


@dataclass
class FaceResult:
    status: Status
    user_id: Optional[str] = None
    name: Optional[str] = None
    similarity: float = 0.0
    threshold: float = 0.0
    bbox: Optional[Tuple[int, int, int, int]] = None
    quality: dict = field(default_factory=dict)
    votes: dict = field(default_factory=dict)   # 窗口内各身份票数（调试/报告素材）
    timestamp: float = 0.0

    @property
    def display_name(self) -> str:
        if self.status is Status.CONFIRMED:
            return f"{self.name} ({self.similarity:.2f})"
        if self.status is Status.UNKNOWN:
            return "未注册人员"
        if self.status is Status.PENDING:
            return f"{self.name or '?'} 识别中..."
        return "无人脸"


class VotingRecognizer:
    def __init__(
        self,
        window_size: int = 7,
        confirm_ratio: float = 0.7,
        reject_threshold: float = 0.30,
        enabled: bool = True,
    ):
        self.window_size = window_size
        self.confirm_ratio = confirm_ratio
        self.reject_threshold = reject_threshold
        self.enabled = enabled
        self._window: deque = deque(maxlen=window_size)

    def update(self, match, timestamp: float = 0.0) -> FaceResult:
        """match: GalleryMatch 或 None（本帧未检测到人脸）"""
        if match is None:
            self._window.clear()
            return FaceResult(Status.LOST, timestamp=timestamp)

        # 相似度过低 → 本帧记为陌生人
        if match.similarity < self.reject_threshold:
            self._window.append(("__unknown__", match.similarity))
        else:
            self._window.append((match.user_id, match.similarity))

        votes = Counter(uid for uid, _ in self._window)
        best_uid, best_count = votes.most_common(1)[0]
        best_sim = max((s for uid, s in self._window if uid == best_uid), default=0.0)

        # 消融对比模式：单帧直接输出
        if not self.enabled:
            if best_uid == "__unknown__":
                return FaceResult(
                    Status.UNKNOWN, None, None, best_sim, match.threshold,
                    votes=dict(votes), timestamp=timestamp,
                )
            return FaceResult(
                Status.CONFIRMED, best_uid, match.name, best_sim, match.threshold,
                votes=dict(votes), timestamp=timestamp,
            )

        # 窗口未填满：证据不足
        if len(self._window) < self.window_size:
            if best_uid == "__unknown__":
                return FaceResult(
                    Status.UNKNOWN, None, None, best_sim, match.threshold,
                    votes=dict(votes), timestamp=timestamp,
                )
            return FaceResult(
                Status.PENDING, best_uid, match.name, best_sim, match.threshold,
                votes=dict(votes), timestamp=timestamp,
            )

        # 窗口已满：按占比投票
        if best_count / self.window_size >= self.confirm_ratio:
            if best_uid == "__unknown__":
                return FaceResult(
                    Status.UNKNOWN, None, None, best_sim, match.threshold,
                    votes=dict(votes), timestamp=timestamp,
                )
            return FaceResult(
                Status.CONFIRMED, best_uid, match.name, best_sim, match.threshold,
                votes=dict(votes), timestamp=timestamp,
            )
        return FaceResult(
            Status.PENDING,
            best_uid if best_uid != "__unknown__" else None,
            match.name if best_uid != "__unknown__" else None,
            best_sim,
            match.threshold,
            votes=dict(votes),
            timestamp=timestamp,
        )

    def reset(self) -> None:
        self._window.clear()
