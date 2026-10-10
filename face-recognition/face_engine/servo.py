"""舵机门禁控制（SG90，RPi.GPIO + PWM；mock 模式不控制硬件）

公共模块：scripts/servo_door.py 和 scripts/iot_reporter.py 共用。
门禁安全设计（本组改进点）：只有多帧投票 CONFIRMED 且命中授权名单才允许开门，
失败不放行（对应任务书"安全性和隐私保护"与"接入业务"环节）。

接线：信号线 GPIO18（物理引脚12）；红线 5V；黑/棕线 GND；外部供电需与树莓派共地。
"""

import threading
import time


class ServoDoorController:
    def __init__(
        self,
        pin: int = 18,
        pwm_frequency: int = 50,
        close_angle: float = 0.0,
        open_angle: float = 90.0,
        move_time: float = 0.8,
        mock: bool = False,
    ):
        self.pin = pin
        self.close_angle = close_angle
        self.open_angle = open_angle
        self.move_time = move_time
        self.mock = mock
        self.is_open = False
        self.close_at = 0.0
        self._gpio = None
        self._pwm = None
        self._lock = threading.Lock()

        if mock:
            print("[门禁] mock 模式：不会实际控制 GPIO")
            return
        try:
            import RPi.GPIO as GPIO
        except ImportError:
            raise RuntimeError(
                "未安装 RPi.GPIO（pip install RPi.GPIO），或改用 --mock-servo 调试"
            )

        self._gpio = GPIO
        GPIO.setmode(GPIO.BCM)
        GPIO.setup(pin, GPIO.OUT)
        self._pwm = GPIO.PWM(pin, pwm_frequency)
        self._pwm.start(self._duty(close_angle))

    @staticmethod
    def _duty(angle: float) -> float:
        """SG90：占空比 2.5%~12.5% 对应 0°~180°"""
        return 2.5 + angle / 180.0 * 10.0

    def open(self, duration: float = 30.0) -> None:
        """开门并设置自动关门时间；已开门时仅刷新关门时间（人一直在门前则保持开门）"""
        with self._lock:
            if self.mock:
                print("[门禁] 已开门（mock）")
            elif not self.is_open:
                self._pwm.ChangeDutyCycle(self._duty(self.open_angle))
                time.sleep(self.move_time)
                print("[门禁] 已开门")
            self.is_open = True
            self.close_at = time.time() + duration

    def close(self) -> None:
        with self._lock:
            if not self.is_open:
                return
            if self.mock:
                print("[门禁] 已关门（mock）")
            else:
                self._pwm.ChangeDutyCycle(self._duty(self.close_angle))
                time.sleep(self.move_time)
                self._pwm.ChangeDutyCycle(0)  # 释放占空，防止舵机抖动
                print("[门禁] 已关门")
            self.is_open = False

    def update(self) -> None:
        """到点自动关门（主循环每帧调用）"""
        if self.is_open and time.time() >= self.close_at:
            self.close()

    def cleanup(self) -> None:
        if self._gpio is not None:
            try:
                self._gpio.cleanup()
            except Exception:
                pass
