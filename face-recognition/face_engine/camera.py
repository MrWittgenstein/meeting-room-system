"""摄像头采集模块

Windows / 普通 Linux：OpenCV VideoCapture
树莓派（Raspberry Pi OS Bookworm）：优先 Picamera2（libcamera 栈），失败回退 OpenCV。
统一 read() 接口返回 BGR 图像，上层无需关心后端差异。

自主设计内容：统一采集抽象 + 分辨率控制（640x480 兼顾速度与清晰度）。
"""

from typing import Optional, Union

import cv2
import numpy as np


class CameraSource:
    def __init__(self, source: Union[int, str] = 0, width: int = 640, height: int = 480, fps: int = 20):
        self._backend = "opencv"
        self._picam2 = None
        if isinstance(source, int):
            # 树莓派上尝试 Picamera2（Raspberry Pi OS Bookworm 的推荐摄像头栈）
            try:
                from picamera2 import Picamera2

                picam2 = Picamera2()
                config = picam2.create_video_configuration(
                    main={"size": (width, height), "format": "BGR888"}
                )
                try:
                    # 固定帧率：FrameDurationLimits 单位微秒
                    config["controls"] = {
                        "FrameDurationLimits": (int(1e6 / fps), int(1e6 / fps))
                    }
                except Exception:
                    pass
                picam2.configure(config)
                picam2.start()
                self._picam2 = picam2
                self._backend = "picamera2"
            except Exception:
                self._picam2 = None
        self._cap = None if self._picam2 else cv2.VideoCapture(source)
        if self._cap is not None:
            self._cap.set(cv2.CAP_PROP_FRAME_WIDTH, width)
            self._cap.set(cv2.CAP_PROP_FRAME_HEIGHT, height)

    @property
    def backend(self) -> str:
        return self._backend

    @property
    def opened(self) -> bool:
        """摄像头/视频源是否成功打开"""
        if self._picam2 is not None:
            return True
        return self._cap is not None and self._cap.isOpened()

    def read(self) -> Optional[np.ndarray]:
        """读取一帧 BGR 图像；失败返回 None（自带 3 次预热重试，部分摄像头前几帧为空）"""
        if self._picam2 is not None:
            return self._picam2.capture_array("main")
        for _ in range(3):
            ok, frame = self._cap.read()
            if ok and frame is not None:
                return frame
        return None

    def release(self) -> None:
        if self._picam2 is not None:
            try:
                self._picam2.stop()
            except Exception:
                pass
        if self._cap is not None:
            self._cap.release()
