#!/usr/bin/env python3
"""Raspberry Pi WebSocket sensor server for the smart meeting room lab."""

from __future__ import annotations

import argparse
import asyncio
from collections import deque
from copy import deepcopy
from datetime import datetime
import json
import logging
import math
import os
import random
import signal
import smtplib
import sys
import threading
import time
from email.header import Header
from email.mime.text import MIMEText
from pathlib import Path
from typing import Any

try:
    import websockets
except ImportError as exc:  # pragma: no cover - handled at runtime on Pi
    raise SystemExit(
        "Missing dependency: websockets. Run `. .venv/bin/activate` and then "
        "`python -m pip install -r requirements.txt`."
    ) from exc

try:
    import cv2
    from face_system.core import FaceEngine
    from face_system.database import load_database
except ImportError:  # pragma: no cover - optional face-recognition dependency
    cv2 = None
    FaceEngine = None
    load_database = None

try:
    import adafruit_dht
    import board
except ImportError:  # pragma: no cover - hardware dependency
    adafruit_dht = None
    board = None

try:
    import RPi.GPIO as GPIO
except ImportError:  # pragma: no cover - hardware dependency
    GPIO = None

try:
    from smbus2 import SMBus
except ImportError:  # pragma: no cover - hardware dependency
    try:
        from smbus import SMBus
    except ImportError:
        SMBus = None


DEFAULT_CONFIG: dict[str, Any] = {
    "device": {
        "device_id": "raspi-01",
        "room_id": 14,
        "room_code": "meeting_room_14",
    },
    "server": {
        "host": "0.0.0.0",
        "port": 8775,
        "sample_interval_seconds": 5,
        "history_size": 100,
        "ping_interval_seconds": 20,
        "ping_timeout_seconds": 20,
    },
    "runtime": {
        "demo_mode": False,
        "log_level": "INFO",
    },
    "backend": {
        "enabled": True,
        "websocket_url": "ws://192.168.5.8:8080/iot/ws/device",
        "reconnect_delay_seconds": 5,
    },
    "dht": {
        "enabled": True,
        "type": "DHT11",
        "gpio_pin": 17,
        "read_retries": 3,
        "retry_delay_seconds": 2,
        "default_temperature": 25.0,
        "default_humidity": 50.0,
    },
    "pcf8591": {
        "enabled": True,
        "i2c_bus": 1,
        "address": "0x48",
    },
    "light": {
        "enabled": True,
        "channel": 0,
        "default": 128,
        "invert": True,
        "fixed_resistance_ohm": 10000.0,
        "reference_raw": 200.0,
        "reference_lux": 200.0,
        "lux_exponent": 1.0,
        "min_lux": 0.1,
        "max_lux": 100000.0,
    },
    "smoke": {
        "enabled": True,
        "channel": 1,
        "default": 0,
        "threshold": 180,
    },
    "pir": {
        "enabled": True,
        "gpio_pin": 27,
    },
    "distance": {
        "enabled": True,
        "trig_pin": 23,
        "echo_pin": 24,
        "default_cm": 400.0,
        "timeout_seconds": 0.1,
        "auto_open_door": False,
        "door_open_below_cm": 30.0,
    },
    "servo": {
        "enabled": True,
        "gpio_pin": 18,
        "pwm_hz": 50,
        "closed_angle": 0,
        "open_angle": 90,
    },
    "face_recognition": {
        "enabled": True,
        "camera_source": "picamera2",
        "camera_index": 0,
        "width": 640,
        "height": 480,
        "fps": 20,
        "preview_enabled": True,
        "preview_window_name": "Smart Room Camera",
        "preview_fps": 15,
        "process_width": 480,
        "database": "data/face_db.npz",
        "yunet_model": "models/face_detection_yunet_2023mar.onnx",
        "sface_model": "models/face_recognition_sface_2021dec.onnx",
        "detector_score_threshold": 0.8,
        "metric": "cosine",
        "match_threshold": None,
        "authorized_names": [],
        "open_door_on_authorized": True,
        "door_open_seconds": 20,
    },
    "outputs": {
        "projector": {
            "enabled": False,
            "gpio_pin": 5,
            "active_high": True,
        },
        "light": {
            "enabled": False,
            "gpio_pin": 6,
            "active_high": True,
        },
        "curtain": {
            "enabled": False,
            "gpio_pin": 13,
            "active_high": True,
        },
        "buzzer": {
            "enabled": False,
            "gpio_pin": 26,
            "active_high": True,
        },
        "cooling": {
            "enabled": False,
            "gpio_pin": 16,
            "active_high": True,
        },
        "heating": {
            "enabled": False,
            "gpio_pin": 20,
            "active_high": True,
        },
        "humidifier": {
            "enabled": False,
            "gpio_pin": 21,
            "active_high": True,
        },
        "dehumidifier": {
            "enabled": False,
            "gpio_pin": 12,
            "active_high": True,
        },
    },
}


def deep_merge(base: dict[str, Any], override: dict[str, Any]) -> dict[str, Any]:
    result = deepcopy(base)
    for key, value in override.items():
        if isinstance(value, dict) and isinstance(result.get(key), dict):
            result[key] = deep_merge(result[key], value)
        else:
            result[key] = value
    return result


def load_config(path: Path | None) -> dict[str, Any]:
    if path is None or not path.exists():
        return deepcopy(DEFAULT_CONFIG)
    with path.open("r", encoding="utf-8") as file:
        user_config = json.load(file)
    return deep_merge(DEFAULT_CONFIG, user_config)


def parse_i2c_address(value: int | str) -> int:
    if isinstance(value, int):
        return value
    return int(str(value), 0)


class OpenCVCameraSource:
    def __init__(self, camera_index: int, width: int, height: int, fps: int) -> None:
        if cv2 is None:
            raise RuntimeError("OpenCV is unavailable.")
        self.capture = cv2.VideoCapture(camera_index)
        self.capture.set(cv2.CAP_PROP_FRAME_WIDTH, width)
        self.capture.set(cv2.CAP_PROP_FRAME_HEIGHT, height)
        self.capture.set(cv2.CAP_PROP_FPS, fps)
        if not self.capture.isOpened():
            self.capture.release()
            raise RuntimeError(f"OpenCV camera {camera_index} could not be opened.")

    def read(self) -> tuple[bool, Any | None]:
        ok, frame = self.capture.read()
        if not ok or frame is None:
            return False, None
        return True, frame

    def release(self) -> None:
        self.capture.release()


class Picamera2CameraSource:
    def __init__(self, width: int, height: int, fps: int) -> None:
        if cv2 is None:
            raise RuntimeError("OpenCV is unavailable.")
        try:
            from picamera2 import Picamera2
        except ImportError as exc:
            raise RuntimeError(
                "未安装 picamera2，无法读取树莓派 CSI 摄像头。请先执行：sudo apt install -y python3-picamera2"
            ) from exc

        self.picam2 = Picamera2()
        main_config = {"size": (max(1, width), max(1, height)), "format": "RGB888"}
        controls = {"FrameRate": max(1, fps)}
        if hasattr(self.picam2, "create_video_configuration"):
            try:
                config = self.picam2.create_video_configuration(main=main_config, controls=controls)
            except TypeError:
                config = self.picam2.create_video_configuration(main=main_config)
        else:
            try:
                config = self.picam2.create_preview_configuration(main=main_config, controls=controls)
            except TypeError:
                config = self.picam2.create_preview_configuration(main=main_config)
        self.picam2.configure(config)
        self.picam2.start()
        time.sleep(0.3)

    def read(self) -> tuple[bool, Any | None]:
        frame_rgb = self.picam2.capture_array()
        if frame_rgb is None:
            return False, None
        return True, cv2.cvtColor(frame_rgb, cv2.COLOR_RGB2BGR)

    def release(self) -> None:
        try:
            self.picam2.stop()
        except Exception:
            pass


class PreviewCameraSource:
    def __init__(self, source: Any, enabled: bool, window_name: str, fps: int) -> None:
        self.source = source
        self.enabled = bool(enabled)
        self.window_name = window_name
        self.fps = max(1, int(fps))
        self.stop_event = threading.Event()
        self.source_lock = threading.Lock()
        self.frame_lock = threading.Lock()
        self.latest_frame: Any | None = None
        self.window_created = False
        self.thread: threading.Thread | None = None

        if self.enabled:
            self.thread = threading.Thread(target=self._preview_loop, name="camera-preview", daemon=True)
            self.thread.start()

    def _preview_loop(self) -> None:
        delay = 1.0 / self.fps
        show_window = bool(os.getenv("DISPLAY") or os.getenv("WAYLAND_DISPLAY"))
        if not show_window:
            logging.warning("Camera preview is enabled, but no desktop display was detected.")

        while not self.stop_event.is_set():
            started_at = time.monotonic()
            with self.source_lock:
                ok, frame = self.source.read()
            if ok and frame is not None:
                with self.frame_lock:
                    self.latest_frame = frame.copy()
                if show_window:
                    try:
                        if not self.window_created:
                            cv2.namedWindow(self.window_name, cv2.WINDOW_NORMAL)
                            self.window_created = True
                        cv2.imshow(self.window_name, frame)
                        key = cv2.waitKey(1) & 0xFF
                        if key in {ord("q"), 27}:
                            show_window = False
                            self._destroy_window()
                    except Exception as exc:
                        show_window = False
                        logging.warning("Camera preview window is disabled: %s", exc)

            elapsed = time.monotonic() - started_at
            time.sleep(max(0.001, delay - elapsed))

    def _destroy_window(self) -> None:
        if not self.window_created or cv2 is None:
            return
        try:
            cv2.destroyWindow(self.window_name)
            cv2.waitKey(1)
        except Exception:
            pass
        self.window_created = False

    def read(self) -> tuple[bool, Any | None]:
        if self.enabled:
            with self.frame_lock:
                if self.latest_frame is not None:
                    return True, self.latest_frame.copy()
        with self.source_lock:
            return self.source.read()

    def release(self) -> None:
        self.stop_event.set()
        if self.thread is not None:
            self.thread.join(timeout=2.0)
        self._destroy_window()
        self.source.release()


class SmartRoomSensorServer:
    def __init__(self, config: dict[str, Any]) -> None:
        self.config = config
        self.history: deque[dict[str, Any]] = deque(
            maxlen=int(config["server"]["history_size"])
        )
        self.clients: set[Any] = set()
        self.running = True
        self.bus: Any | None = None
        self.dht_device: Any | None = None
        self.servo_pwm: Any | None = None
        self.face_capture: Any | None = None
        self.face_engine: Any | None = None
        self.face_known_names: list[str] = []
        self.face_known_embeddings: list[Any] = []
        self.face_authorized_names: set[str] = set()
        self.face_door_close_at: float | None = None
        self.last_presence_time = time.monotonic()
        self.last_alert_time = 0.0
        self.last_door_status: str | None = None
        self.demo_tick = 0
        self.device_state: dict[str, Any] = {
            "projector": False,
            "light_on": True,
            "auto_mode": True,
            "curtain_open": False,
            "buzzer_on": False,
            "cooling": False,
            "heating": False,
            "humidifier": False,
            "dehumidifier": False,
            "alarm": False,
            "door_state": "closed",
            "servo_angle": int(config["servo"]["closed_angle"]),
        }

    @property
    def demo_mode(self) -> bool:
        return bool(self.config["runtime"].get("demo_mode"))

    def setup_hardware(self) -> None:
        if self.demo_mode:
            logging.warning("Demo mode is enabled; hardware sensors will be simulated.")
            return

        if GPIO is not None:
            GPIO.setwarnings(False)
            GPIO.setmode(GPIO.BCM)

        self.setup_dht()

        if self.config["pcf8591"]["enabled"]:
            if SMBus is None:
                logging.warning("smbus/smbus2 is unavailable; PCF8591 data uses defaults.")
            else:
                bus_id = int(self.config["pcf8591"]["i2c_bus"])
                self.bus = SMBus(bus_id)
                logging.info("PCF8591 initialized on I2C bus %s.", bus_id)

        if self.config["pir"]["enabled"]:
            self.setup_gpio_input("pir", int(self.config["pir"]["gpio_pin"]))

        if self.config["distance"]["enabled"]:
            self.setup_distance_sensor()

        self.setup_output_devices()

        if self.config["servo"]["enabled"]:
            self.setup_servo()

        if self.config["face_recognition"]["enabled"]:
            self.setup_face_recognition()

    def setup_gpio_input(self, name: str, pin: int) -> None:
        if GPIO is None:
            logging.warning("RPi.GPIO is unavailable; %s data uses defaults.", name)
            return
        GPIO.setup(pin, GPIO.IN)
        logging.info("%s input initialized on GPIO%s.", name, pin)

    def setup_dht(self) -> None:
        cfg = self.config["dht"]
        if not cfg["enabled"]:
            return
        if adafruit_dht is None or board is None:
            logging.warning(
                "adafruit-circuitpython-dht/board is unavailable; DHT data uses defaults."
            )
            return

        pin = int(cfg["gpio_pin"])
        pin_name = f"D{pin}"
        if not hasattr(board, pin_name):
            logging.warning("board.%s is unavailable; DHT data uses defaults.", pin_name)
            return

        sensor_name = str(cfg["type"]).upper()
        sensor_cls = getattr(adafruit_dht, sensor_name, None)
        if sensor_cls is None:
            logging.warning("Unsupported DHT sensor type %s; DHT data uses defaults.", sensor_name)
            return

        try:
            self.dht_device = sensor_cls(getattr(board, pin_name))
            time.sleep(1)
            logging.info("%s initialized on board.%s.", sensor_name, pin_name)
        except Exception as exc:
            self.dht_device = None
            logging.warning("Failed to initialize %s on board.%s: %s", sensor_name, pin_name, exc)

    def setup_output_devices(self) -> None:
        if GPIO is None:
            logging.warning("RPi.GPIO is unavailable; output device control is simulated.")
            return
        for name, cfg in self.config.get("outputs", {}).items():
            if not cfg.get("enabled"):
                continue
            pin = int(cfg["gpio_pin"])
            GPIO.setup(pin, GPIO.OUT)
            self.write_output(name, bool(self.output_state_value(name)), log_result=False)
            logging.info("%s output initialized on GPIO%s.", name, pin)

    def setup_distance_sensor(self) -> None:
        if GPIO is None:
            logging.warning("RPi.GPIO is unavailable; distance data uses defaults.")
            return
        trig_pin = int(self.config["distance"]["trig_pin"])
        echo_pin = int(self.config["distance"]["echo_pin"])
        GPIO.setup(trig_pin, GPIO.OUT)
        GPIO.setup(echo_pin, GPIO.IN)
        GPIO.output(trig_pin, GPIO.LOW)
        logging.info("HC-SR04 initialized: TRIG=GPIO%s, ECHO=GPIO%s.", trig_pin, echo_pin)

    def setup_servo(self) -> None:
        if GPIO is None:
            logging.warning("RPi.GPIO is unavailable; servo control is disabled.")
            return
        pin = int(self.config["servo"]["gpio_pin"])
        pwm_hz = int(self.config["servo"]["pwm_hz"])
        GPIO.setup(pin, GPIO.OUT)
        self.servo_pwm = GPIO.PWM(pin, pwm_hz)
        self.servo_pwm.start(0)
        self.set_servo_angle(float(self.config["servo"]["closed_angle"]))
        logging.info("Servo initialized on GPIO%s at %s Hz.", pin, pwm_hz)

    def setup_face_recognition(self) -> None:
        cfg = self.config["face_recognition"]
        if cv2 is None or FaceEngine is None or load_database is None:
            logging.warning(
                "OpenCV face recognition dependencies are unavailable; face data uses disabled state."
            )
            return

        database_path = self.resolve_project_path(cfg["database"])
        yunet_model = self.resolve_project_path(cfg["yunet_model"])
        sface_model = self.resolve_project_path(cfg["sface_model"])
        missing = [path for path in (database_path, yunet_model, sface_model) if not path.exists()]
        if missing:
            logging.warning("Face recognition files are missing: %s", ", ".join(str(p) for p in missing))
            return

        try:
            self.face_known_names, self.face_known_embeddings = load_database(database_path)
            self.face_engine = FaceEngine(
                yunet_model=yunet_model,
                sface_model=sface_model,
                detector_score_threshold=float(cfg["detector_score_threshold"]),
                metric=str(cfg["metric"]),
                match_threshold=cfg.get("match_threshold"),
            )
            self.face_authorized_names = {
                str(name).strip().lower()
                for name in cfg.get("authorized_names", [])
                if str(name).strip()
            }

            camera_source = str(cfg.get("camera_source", "picamera2")).strip().lower()
            camera_index = int(cfg["camera_index"])
            if camera_source in {"picamera2", "csi"}:
                camera = Picamera2CameraSource(
                    int(cfg["width"]),
                    int(cfg["height"]),
                    int(cfg["fps"]),
                )
            elif camera_source in {"opencv", "usb"}:
                camera = OpenCVCameraSource(
                    camera_index,
                    int(cfg["width"]),
                    int(cfg["height"]),
                    int(cfg["fps"]),
                )
            else:
                logging.warning("Unsupported face camera source %s.", camera_source)
                return
            self.face_capture = PreviewCameraSource(
                camera,
                bool(cfg.get("preview_enabled", True)),
                str(cfg.get("preview_window_name", "Smart Room Camera")),
                int(cfg.get("preview_fps", cfg["fps"])),
            )

            logging.info(
                "Face recognition initialized: source=%s, camera=%s, preview=%s, known_faces=%s, authorized=%s.",
                camera_source,
                camera_index if camera_source in {"opencv", "usb"} else "CSI",
                bool(cfg.get("preview_enabled", True)),
                len(self.face_known_names),
                sorted(self.face_authorized_names) if self.face_authorized_names else "any known face",
            )
        except Exception as exc:
            self.face_engine = None
            if self.face_capture is not None:
                self.face_capture.release()
                self.face_capture = None
            logging.warning("Failed to initialize face recognition: %s", exc)

    def resolve_project_path(self, value: str | Path) -> Path:
        path = Path(value)
        if path.is_absolute():
            return path
        return Path(__file__).resolve().parent / path

    def read_sensors(self) -> dict[str, Any]:
        if self.demo_mode:
            data = self.read_demo_sensors()
        else:
            data = {}
            data.update(self.read_dht())
            data.update(self.read_pcf8591_sensors())
            data.update(self.read_pir())
            data.update(self.read_distance_and_door())
            data.update(self.read_face_recognition())

        data["timestamp"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        self.apply_automation(data)
        self.update_scheduled_door()
        data.update(self.device_state)
        return data

    def read_demo_sensors(self) -> dict[str, Any]:
        self.demo_tick += 1
        angle = self.demo_tick / 6
        temperature = 25 + math.sin(angle) * 4 + random.uniform(-0.3, 0.3)
        humidity = 52 + math.cos(angle / 1.4) * 12 + random.uniform(-1.0, 1.0)
        light_raw = int(150 + math.sin(angle / 2) * 90)
        light = self.convert_light_raw_to_lux(max(0, min(255, light_raw)))
        smoke = 220 if self.demo_tick % 40 in range(31, 36) else random.randint(5, 30)
        presence = self.demo_tick % 24 < 18
        data = {
            "temperature": round(temperature, 1),
            "humidity": round(humidity, 1),
            "light_raw": max(0, min(255, light_raw)),
            "light": light,
            "smoke_raw": smoke,
            "smoke": smoke,
            "presence": presence,
            "no_presence_duration": 0 if presence else 60,
        }
        if self.config["distance"]["enabled"]:
            distance = 20.0 if self.demo_tick % 20 < 6 else 120.0
            data["distance"] = distance
            if self.config["distance"].get("auto_open_door", False):
                data["door"] = (
                    "open" if distance < float(self.config["distance"]["door_open_below_cm"]) else "closed"
                )
        return data

    def read_dht(self) -> dict[str, float]:
        cfg = self.config["dht"]
        default_temperature = float(cfg["default_temperature"])
        default_humidity = float(cfg["default_humidity"])
        if not cfg["enabled"]:
            return {}
        if self.dht_device is None:
            logging.warning("DHT device is unavailable; DHT data uses defaults.")
            return {"temperature": default_temperature, "humidity": default_humidity}

        retries = int(cfg.get("read_retries", 3))
        retry_delay = float(cfg.get("retry_delay_seconds", 2))
        last_error: Exception | None = None
        for attempt in range(1, retries + 1):
            try:
                temperature = self.dht_device.temperature
                humidity = self.dht_device.humidity
                if temperature is not None and humidity is not None:
                    return {
                        "temperature": round(float(temperature), 1),
                        "humidity": round(float(humidity), 1),
                    }
            except RuntimeError as exc:
                last_error = exc
                logging.debug("DHT read attempt %s/%s failed: %s", attempt, retries, exc)
            except Exception as exc:
                last_error = exc
                logging.warning("Unexpected DHT read error: %s", exc)
                break
            if attempt < retries:
                time.sleep(retry_delay)

        if last_error:
            logging.warning("Failed to read DHT sensor; using default values: %s", last_error)
        else:
            logging.warning("Failed to read DHT sensor; using default values.")
        return {"temperature": default_temperature, "humidity": default_humidity}

    def read_pcf8591_sensors(self) -> dict[str, Any]:
        data: dict[str, Any] = {}
        if self.config["light"]["enabled"]:
            raw_light = self.read_pcf8591_channel(
                int(self.config["light"]["channel"]),
                int(self.config["light"]["default"]),
            )
            data["light_raw"] = raw_light
            data["light"] = self.convert_light_raw_to_lux(raw_light)

        if self.config["smoke"]["enabled"]:
            raw_smoke = self.read_pcf8591_channel(
                int(self.config["smoke"]["channel"]),
                int(self.config["smoke"]["default"]),
            )
            data["smoke_raw"] = raw_smoke
            data["smoke"] = raw_smoke

        return data

    def convert_light_raw_to_lux(self, raw_light: int) -> int:
        cfg = self.config["light"]
        adc_max = 255.0
        raw = max(1.0, min(adc_max - 1.0, float(raw_light)))
        fixed_ohm = max(1.0, float(cfg.get("fixed_resistance_ohm", 10000.0)))
        invert = bool(cfg.get("invert", True))

        def estimate_ldr_ohm(raw_value: float) -> float:
            ratio = raw_value / adc_max
            if invert:
                return fixed_ohm * ratio / max(1e-6, 1.0 - ratio)
            return fixed_ohm * (1.0 - ratio) / max(1e-6, ratio)

        ldr_ohm = estimate_ldr_ohm(raw)
        exponent = max(0.1, float(cfg.get("lux_exponent", 1.0)))

        if "reference_raw" in cfg and "reference_lux" in cfg:
            reference_raw = max(1.0, min(adc_max - 1.0, float(cfg["reference_raw"])))
            reference_lux = max(0.1, float(cfg["reference_lux"]))
            reference_ldr_ohm = estimate_ldr_ohm(reference_raw)
            coefficient = reference_lux * max(0.001, (reference_ldr_ohm / 1000.0) ** exponent)
        else:
            coefficient = float(cfg.get("lux_coefficient", 7272.7))

        lux = coefficient / max(0.001, (ldr_ohm / 1000.0) ** exponent)
        min_lux = float(cfg.get("min_lux", 0.1))
        max_lux = float(cfg.get("max_lux", 100000.0))
        return int(round(max(min_lux, min(max_lux, lux))))

    def read_pcf8591_channel(self, channel: int, default: int) -> int:
        if self.bus is None:
            return default
        try:
            address = parse_i2c_address(self.config["pcf8591"]["address"])
            control = 0x40 | (channel & 0x03)
            self.bus.write_byte(address, control)
            self.bus.read_byte(address)
            value = self.bus.read_byte(address)
            return max(0, min(255, int(value)))
        except Exception as exc:
            logging.warning("Failed to read PCF8591 channel %s: %s", channel, exc)
            return default

    def read_pir(self) -> dict[str, Any]:
        if not self.config["pir"]["enabled"]:
            return {}
        if GPIO is None:
            return {"presence": False, "no_presence_duration": 0}
        try:
            presence = GPIO.input(int(self.config["pir"]["gpio_pin"])) == GPIO.HIGH
        except Exception as exc:
            logging.warning("Failed to read PIR sensor: %s", exc)
            presence = False

        if presence:
            self.last_presence_time = time.monotonic()
            no_presence_duration = 0
        else:
            no_presence_duration = int(time.monotonic() - self.last_presence_time)

        return {"presence": presence, "no_presence_duration": no_presence_duration}

    def read_distance_and_door(self) -> dict[str, Any]:
        if not self.config["distance"]["enabled"]:
            return {}

        distance = self.read_distance()
        if not self.config["distance"].get("auto_open_door", False):
            return {"distance": distance}

        open_below = float(self.config["distance"]["door_open_below_cm"])
        door_status = "open" if distance < open_below else "closed"

        if self.config["servo"]["enabled"] and door_status != self.last_door_status:
            angle_key = "open_angle" if door_status == "open" else "closed_angle"
            self.set_servo_angle(float(self.config["servo"][angle_key]))
            self.last_door_status = door_status

        return {"distance": distance, "door": door_status}

    def read_face_recognition(self) -> dict[str, Any]:
        cfg = self.config["face_recognition"]
        if not cfg["enabled"]:
            return {}

        if self.face_capture is None or self.face_engine is None:
            return {
                "face_detected": False,
                "face_count": 0,
                "recognized_names": [],
                "unknown_face_count": 0,
                "face_authorized": False,
                "face_best_match": None,
                "face_best_score": None,
                "face_error": "face_recognition_not_initialized",
            }

        ok, frame = self.face_capture.read()
        if not ok or frame is None:
            logging.warning("Failed to read face camera frame.")
            return {
                "face_detected": False,
                "face_count": 0,
                "recognized_names": [],
                "unknown_face_count": 0,
                "face_authorized": False,
                "face_best_match": None,
                "face_best_score": None,
                "face_error": "camera_read_failed",
            }

        process_frame = self.resize_face_frame(frame, int(cfg["process_width"]))
        try:
            faces = self.face_engine.detect_faces(process_frame)
            recognized_names: list[str] = []
            unknown_count = 0
            authorized = False
            best_match = None
            best_score = None

            for face in faces:
                feature = self.face_engine.extract_feature(process_frame, face)
                result = self.face_engine.identify(
                    feature,
                    self.face_known_names,
                    self.face_known_embeddings,
                )
                if best_score is None or float(result.score) > float(best_score):
                    best_match = result.name
                    best_score = float(result.score)
                if result.is_known:
                    recognized_names.append(result.name)
                    if self.is_face_authorized(result.name):
                        authorized = True
                else:
                    unknown_count += 1

            if authorized and bool(cfg["open_door_on_authorized"]):
                self.open_door_for_face(float(cfg["door_open_seconds"]))

            return {
                "face_detected": bool(faces),
                "face_count": len(faces),
                "recognized_names": sorted(set(recognized_names)),
                "unknown_face_count": unknown_count,
                "face_authorized": authorized,
                "face_best_match": best_match,
                "face_best_score": round(float(best_score), 4) if best_score is not None else None,
                "face_error": None,
            }
        except Exception as exc:
            logging.warning("Face recognition failed: %s", exc)
            return {
                "face_detected": False,
                "face_count": 0,
                "recognized_names": [],
                "unknown_face_count": 0,
                "face_authorized": False,
                "face_best_match": None,
                "face_best_score": None,
                "face_error": str(exc),
            }

    def resize_face_frame(self, frame: Any, process_width: int) -> Any:
        if cv2 is None or process_width <= 0 or frame.shape[1] <= process_width:
            return frame
        scale = process_width / frame.shape[1]
        return cv2.resize(frame, (process_width, max(1, int(frame.shape[0] * scale))))

    def is_face_authorized(self, name: str) -> bool:
        if not self.face_authorized_names:
            return True
        return name.strip().lower() in self.face_authorized_names

    def open_door_for_face(self, seconds: float) -> None:
        self.set_servo_angle(float(self.config["servo"]["open_angle"]))
        self.face_door_close_at = time.monotonic() + max(0.0, seconds)

    def update_scheduled_door(self) -> None:
        if self.face_door_close_at is None:
            return
        if time.monotonic() < self.face_door_close_at:
            return
        self.face_door_close_at = None
        self.set_servo_angle(float(self.config["servo"]["closed_angle"]))

    def read_distance(self) -> float:
        if GPIO is None:
            return float(self.config["distance"]["default_cm"])

        trig_pin = int(self.config["distance"]["trig_pin"])
        echo_pin = int(self.config["distance"]["echo_pin"])
        timeout = float(self.config["distance"]["timeout_seconds"])
        default_cm = float(self.config["distance"]["default_cm"])

        try:
            GPIO.output(trig_pin, GPIO.LOW)
            time.sleep(0.000002)
            GPIO.output(trig_pin, GPIO.HIGH)
            time.sleep(0.00001)
            GPIO.output(trig_pin, GPIO.LOW)

            start_wait = time.monotonic()
            while GPIO.input(echo_pin) == GPIO.LOW:
                if time.monotonic() - start_wait > timeout:
                    return default_cm
            pulse_start = time.monotonic()

            start_wait = time.monotonic()
            while GPIO.input(echo_pin) == GPIO.HIGH:
                if time.monotonic() - start_wait > timeout:
                    return default_cm
            pulse_end = time.monotonic()

            pulse_duration = pulse_end - pulse_start
            distance_cm = pulse_duration * 34300 / 2
            return round(distance_cm, 1)
        except Exception as exc:
            logging.warning("Failed to read distance sensor: %s", exc)
            return default_cm

    def set_servo_angle(self, angle: float) -> bool:
        if self.servo_pwm is None:
            self.device_state["servo_angle"] = round(angle)
            self.device_state["door_state"] = (
                "open"
                if angle >= float(self.config["servo"]["open_angle"]) / 2
                else "closed"
            )
            return False
        try:
            duty_cycle = 2.5 + (angle / 180.0) * 10.0
            self.servo_pwm.ChangeDutyCycle(duty_cycle)
            time.sleep(0.2)
            self.servo_pwm.ChangeDutyCycle(0)
            self.device_state["servo_angle"] = round(angle)
            self.device_state["door_state"] = (
                "open"
                if angle >= float(self.config["servo"]["open_angle"]) / 2
                else "closed"
            )
            return True
        except Exception as exc:
            logging.warning("Failed to set servo angle %.1f: %s", angle, exc)
            return False

    def apply_automation(self, data: dict[str, Any]) -> None:
        if not self.device_state.get("auto_mode", True):
            return
        temperature = data.get("temperature")
        humidity = data.get("humidity")
        light = data.get("light")
        smoke = data.get("smoke")
        if temperature is not None:
            self.device_state["cooling"] = float(temperature) > 28
            self.device_state["heating"] = float(temperature) < 20
        if humidity is not None:
            self.device_state["humidifier"] = float(humidity) < 40
            self.device_state["dehumidifier"] = float(humidity) > 65
        if light is not None:
            light_lux = float(light)
            self.device_state["light_on"] = light_lux < 70
            self.device_state["curtain_open"] = light_lux < 80
        alarm = bool(smoke is not None and float(smoke) > float(self.config["smoke"]["threshold"]))
        self.device_state["alarm"] = alarm
        self.device_state["buzzer_on"] = alarm
        for name in ("light", "curtain", "buzzer", "cooling", "heating", "humidifier", "dehumidifier"):
            self.write_output(name, bool(self.output_state_value(name)))

    def output_state_value(self, output_name: str) -> Any:
        mapping = {
            "light": "light_on",
            "curtain": "curtain_open",
            "buzzer": "buzzer_on",
            "projector": "projector",
            "cooling": "cooling",
            "heating": "heating",
            "humidifier": "humidifier",
            "dehumidifier": "dehumidifier",
        }
        return self.device_state.get(mapping.get(output_name, output_name), False)

    def write_output(self, output_name: str, enabled: bool, log_result: bool = True) -> bool:
        cfg = self.config.get("outputs", {}).get(output_name)
        if not cfg or not cfg.get("enabled"):
            return False
        if GPIO is None:
            if log_result:
                logging.info("Simulated output %s=%s", output_name, enabled)
            return False
        pin = int(cfg["gpio_pin"])
        active_high = bool(cfg.get("active_high", True))
        level = GPIO.HIGH if enabled == active_high else GPIO.LOW
        GPIO.output(pin, level)
        if log_result:
            logging.info("Output %s on GPIO%s set to %s.", output_name, pin, enabled)
        return True

    def normalize_bool(self, value: Any) -> bool:
        if isinstance(value, bool):
            return value
        if isinstance(value, (int, float)):
            return value != 0
        if isinstance(value, str):
            return value.strip().lower() in {"1", "true", "yes", "on", "open"}
        return bool(value)

    async def sample_loop(self) -> None:
        interval = float(self.config["server"]["sample_interval_seconds"])
        while self.running:
            data = self.read_sensors()
            self.history.append(data)
            self.handle_alerts(data)
            await self.broadcast(self.build_payload(data))
            logging.info("sample: %s", json.dumps(data, ensure_ascii=False))
            await asyncio.sleep(interval)

    def build_payload(self, current: dict[str, Any] | None = None) -> dict[str, Any]:
        if current is None:
            current = self.history[-1] if self.history else {}
        return {"current": current, "history": list(self.history)}

    def build_backend_payload(self, current: dict[str, Any] | None = None) -> dict[str, Any]:
        if current is None:
            current = self.history[-1] if self.history else self.read_sensors()

        device_config = self.config["device"]
        room_id = int(device_config["room_id"])
        room_code = str(device_config.get("room_code") or f"meeting_room_{room_id}")
        device_id = str(device_config["device_id"])
        distance = current.get("distance")
        person_near = bool(distance is not None and float(distance) < 100)
        presence = bool(current.get("presence", person_near))
        light_raw = current.get("light_raw")
        light = current.get("light")
        smoke_raw = current.get("smoke_raw")
        smoke = current.get("smoke")
        door_state = current.get("door_state") or current.get("door") or self.device_state.get("door_state", "closed")
        servo_angle = current.get("servo_angle", self.device_state.get("servo_angle"))
        alarm = bool(current.get("alarm", self.device_state.get("alarm", False)))
        face_detected = bool(current.get("face_detected", False))
        face_count = int(current.get("face_count", 0) or 0)
        face_authorized = bool(current.get("face_authorized", False))
        face_error = current.get("face_error")

        payload_current = {
            "room_id": room_code,
            "roomId": room_id,
            "event_type": "telemetry",
            "timestamp": current.get("timestamp") or datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "environment": {
                "temperature": current.get("temperature"),
                "humidity": current.get("humidity"),
                "light": light,
                "light_raw": light_raw,
                "smoke": smoke,
                "smoke_raw": smoke_raw,
                "smoke_level": self.get_smoke_level(smoke),
            },
            "presence": {
                "pir": presence,
                "presence": presence,
                "distance": distance,
                "person_near": person_near,
            },
            "access_control": {
                "face_detected": face_detected,
                "face_count": face_count,
                "recognized_names": current.get("recognized_names", []),
                "unknown_face_count": int(current.get("unknown_face_count", 0) or 0),
                "face_best_match": current.get("face_best_match"),
                "face_best_score": current.get("face_best_score"),
                "face_error": face_error,
                "door_state": door_state,
                "servo_angle": servo_angle,
                "authorized": face_authorized,
            },
            "devices": {
                "projector": bool(current.get("projector", self.device_state.get("projector", False))),
                "cooling": bool(current.get("cooling", self.device_state.get("cooling", False))),
                "heating": bool(current.get("heating", self.device_state.get("heating", False))),
                "humidifier": bool(current.get("humidifier", self.device_state.get("humidifier", False))),
                "dehumidifier": bool(current.get("dehumidifier", self.device_state.get("dehumidifier", False))),
                "light_on": bool(current.get("light_on", self.device_state.get("light_on", False))),
                "curtain_open": bool(current.get("curtain_open", self.device_state.get("curtain_open", False))),
                "buzzer_on": bool(current.get("buzzer_on", self.device_state.get("buzzer_on", False))),
                "auto_mode": bool(current.get("auto_mode", self.device_state.get("auto_mode", True))),
                "alarm": alarm,
            },
            "system": {
                "sensor_status": "warning" if face_error else "ok",
                "error": face_error,
            },
        }
        return {
            "device_id": device_id,
            "current": payload_current,
            "history": [],
        }

    def get_smoke_level(self, smoke: Any) -> str | None:
        if smoke is None:
            return None
        value = float(smoke)
        if value < 180:
            return "normal"
        if value < 210:
            return "warning"
        if value < 240:
            return "danger"
        return "critical"

    async def broadcast(self, payload: dict[str, Any]) -> None:
        if not self.clients:
            return
        message = json.dumps(payload, ensure_ascii=False)
        disconnected = []
        for client in list(self.clients):
            try:
                await client.send(message)
            except Exception:
                disconnected.append(client)
        for client in disconnected:
            self.clients.discard(client)

    async def backend_upload_loop(self) -> None:
        if not self.config["backend"]["enabled"]:
            return

        websocket_url = str(os.getenv("BACKEND_WS_URL") or self.config["backend"]["websocket_url"])
        reconnect_delay = float(self.config["backend"]["reconnect_delay_seconds"])
        while self.running:
            try:
                async with websockets.connect(websocket_url, ping_interval=20) as websocket:
                    greeting = await websocket.recv()
                    logging.info("Spring backend connected: %s", greeting)
                    sender_task = asyncio.create_task(self.backend_sender_loop(websocket))
                    receiver_task = asyncio.create_task(self.backend_receiver_loop(websocket))
                    done, pending = await asyncio.wait(
                        {sender_task, receiver_task},
                        return_when=asyncio.FIRST_EXCEPTION,
                    )
                    for task in pending:
                        task.cancel()
                    for task in done:
                        task.result()
            except Exception as exc:
                logging.warning(
                    "Spring backend websocket unavailable: %s. Reconnecting in %.1fs.",
                    exc,
                    reconnect_delay,
                )
                await asyncio.sleep(reconnect_delay)

    async def backend_sender_loop(self, websocket: Any) -> None:
        interval = float(self.config["server"]["sample_interval_seconds"])
        while self.running:
            if not self.history:
                await asyncio.sleep(0.2)
                continue
            await self.send_backend_telemetry(websocket)
            await asyncio.sleep(interval)

    async def backend_receiver_loop(self, websocket: Any) -> None:
        async for message in websocket:
            await self.handle_backend_message(websocket, message)

    async def send_backend_telemetry(self, websocket: Any) -> None:
        payload = self.build_backend_payload()
        await websocket.send(json.dumps(payload, ensure_ascii=False))

    async def handle_backend_message(self, websocket: Any, message: str) -> None:
        try:
            data = json.loads(message)
        except json.JSONDecodeError:
            logging.debug("Ignoring non-json backend message: %s", message)
            return

        message_type = str(data.get("type", "")).upper()
        if message_type == "ACK":
            logging.debug("Spring backend ack: %s", message)
            return
        if message_type != "COMMAND":
            logging.debug("Ignoring backend message: %s", message)
            return

        ack = await self.execute_backend_command(data)
        await websocket.send(json.dumps(ack, ensure_ascii=False))
        data = self.read_sensors()
        self.history.append(data)
        await self.broadcast(self.build_payload(data))
        await self.send_backend_telemetry(websocket)

    async def execute_backend_command(self, command_message: dict[str, Any]) -> dict[str, Any]:
        command_id = command_message.get("commandId") or command_message.get("command_id")
        command = str(command_message.get("command", "")).strip()
        try:
            result = self.apply_command(command_message)
            status = "success" if result else "noop"
            message = "command executed" if result else "command accepted without hardware output"
        except Exception as exc:
            status = "error"
            message = str(exc)
            logging.warning("Failed to execute backend command %s: %s", command, exc)

        return {
            "type": "COMMAND_ACK",
            "commandId": command_id,
            "deviceId": self.config["device"]["device_id"],
            "command": command,
            "status": status,
            "message": message,
            "state": self.device_state,
            "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        }

    def apply_command(self, command_message: dict[str, Any]) -> bool:
        command = str(command_message.get("command", "")).strip()
        target = command_message.get("target")
        value = command_message.get("value")
        params = command_message.get("params") or {}
        if command in {"set_device_state", "set_state"}:
            if target is None:
                raise ValueError("target is required")
            return self.set_device_state(str(target), self.normalize_bool(value))
        if command in {"set_servo_angle", "servo"}:
            angle = command_message.get("angle", params.get("angle"))
            if angle is None:
                raise ValueError("angle is required")
            return self.set_servo_angle(float(angle))
        if command == "open_door":
            return self.set_servo_angle(float(self.config["servo"]["open_angle"]))
        if command == "close_door":
            return self.set_servo_angle(float(self.config["servo"]["closed_angle"]))
        if command == "set_auto_mode":
            return self.set_device_state("auto", self.normalize_bool(value))
        raise ValueError(f"unsupported command: {command}")

    def set_device_state(self, target: str, enabled: bool) -> bool:
        normalized = target.strip().lower()
        aliases = {
            "projector": ("projector", "projector"),
            "light": ("light_on", "light"),
            "light_on": ("light_on", "light"),
            "curtain": ("curtain_open", "curtain"),
            "curtain_open": ("curtain_open", "curtain"),
            "buzzer": ("buzzer_on", "buzzer"),
            "buzzer_on": ("buzzer_on", "buzzer"),
            "alarm": ("alarm", None),
            "cooling": ("cooling", "cooling"),
            "heating": ("heating", "heating"),
            "humidifier": ("humidifier", "humidifier"),
            "dehumidifier": ("dehumidifier", "dehumidifier"),
            "auto": ("auto_mode", None),
            "auto_mode": ("auto_mode", None),
        }
        if normalized not in aliases:
            raise ValueError(f"unsupported target: {target}")

        state_key, output_name = aliases[normalized]
        self.device_state[state_key] = enabled
        if state_key == "alarm":
            self.device_state["buzzer_on"] = enabled
            output_name = "buzzer"
        if state_key != "auto_mode":
            self.device_state["auto_mode"] = False
        return self.write_output(output_name, enabled) if output_name else True

    async def handle_client(self, websocket: Any, path: str | None = None) -> None:
        self.clients.add(websocket)
        logging.info("client connected: %s", getattr(websocket, "remote_address", "unknown"))
        if self.history:
            await websocket.send(json.dumps(self.build_payload(), ensure_ascii=False))
        try:
            async for message in websocket:
                await self.handle_client_message(websocket, message)
        except Exception as exc:
            logging.info("client disconnected: %s", exc)
        finally:
            self.clients.discard(websocket)

    async def handle_client_message(self, websocket: Any, message: str) -> None:
        try:
            data = json.loads(message)
        except json.JSONDecodeError:
            return

        if data.get("type") == "ping":
            await websocket.send(json.dumps({"type": "pong", "timestamp": time.time()}))
        elif data.get("type") == "set_servo_angle" and self.config["servo"]["enabled"]:
            angle = float(data.get("angle", self.config["servo"]["closed_angle"]))
            ok = self.set_servo_angle(angle)
            await websocket.send(json.dumps({"type": "set_servo_angle", "ok": ok}))
        elif data.get("type") in {"COMMAND", "set_device_state", "set_state", "set_auto_mode"}:
            command_data = data if data.get("type") == "COMMAND" else {
                "command": data.get("type"),
                "target": data.get("target"),
                "value": data.get("value"),
                "params": data.get("params", {}),
            }
            ack = await self.execute_backend_command(command_data)
            await websocket.send(json.dumps(ack, ensure_ascii=False))

    def handle_alerts(self, data: dict[str, Any]) -> None:
        if "smoke" not in data:
            return
        threshold = int(self.config["smoke"]["threshold"])
        if int(data["smoke"]) <= threshold:
            return

        logging.warning("Smoke level is above threshold: %s > %s", data["smoke"], threshold)
        self.send_email_alert_once(data)

    def send_email_alert_once(self, data: dict[str, Any]) -> None:
        cfg = self.config.get("email_alert", {"enabled": False})
        if not cfg["enabled"]:
            return

        now = time.monotonic()
        cooldown = float(cfg["cooldown_seconds"])
        if now - self.last_alert_time < cooldown:
            return

        required = ["smtp_server", "smtp_port", "sender", "password", "receiver"]
        if any(not cfg.get(item) for item in required):
            logging.warning("Email alert is enabled, but email configuration is incomplete.")
            return

        subject = "Smart room smoke alert"
        body = (
            "Warning: abnormal smoke level detected in the meeting room.\n\n"
            f"Smoke value: {data.get('smoke')}\n"
            f"Time: {data.get('timestamp')}\n"
        )
        msg = MIMEText(body, "plain", "utf-8")
        msg["Subject"] = Header(subject, "utf-8")
        msg["From"] = cfg["sender"]
        msg["To"] = cfg["receiver"]

        try:
            with smtplib.SMTP(cfg["smtp_server"], int(cfg["smtp_port"]), timeout=10) as server:
                if cfg["use_tls"]:
                    server.starttls()
                server.login(cfg["sender"], cfg["password"])
                server.sendmail(cfg["sender"], [cfg["receiver"]], msg.as_string())
            self.last_alert_time = now
            logging.info("Smoke alert email sent.")
        except Exception as exc:
            logging.warning("Failed to send alert email: %s", exc)

    async def run(self) -> None:
        self.setup_hardware()
        host = str(self.config["server"]["host"])
        port = int(self.config["server"]["port"])
        ping_interval = self.config["server"]["ping_interval_seconds"]
        ping_timeout = self.config["server"]["ping_timeout_seconds"]

        async with websockets.serve(
            self.handle_client,
            host,
            port,
            ping_interval=ping_interval,
            ping_timeout=ping_timeout,
        ):
            logging.info("WebSocket server started at ws://%s:%s", host, port)
            backend_task = asyncio.create_task(self.backend_upload_loop())
            try:
                await self.sample_loop()
            finally:
                backend_task.cancel()
                try:
                    await backend_task
                except asyncio.CancelledError:
                    pass

    def stop(self) -> None:
        self.running = False

    def cleanup(self) -> None:
        if self.dht_device is not None:
            try:
                self.dht_device.exit()
            except Exception:
                pass
        if self.servo_pwm is not None:
            self.servo_pwm.stop()
        if self.face_capture is not None:
            try:
                self.face_capture.release()
            except Exception:
                pass
        if GPIO is not None and not self.demo_mode:
            GPIO.cleanup()


def configure_logging(config: dict[str, Any]) -> None:
    level_name = os.getenv("LOG_LEVEL", str(config["runtime"]["log_level"])).upper()
    logging.basicConfig(
        level=getattr(logging, level_name, logging.INFO),
        format="%(asctime)s %(levelname)s %(message)s",
    )


async def async_main() -> None:
    parser = argparse.ArgumentParser(description="Smart room Raspberry Pi sensor server")
    parser.add_argument(
        "--config",
        type=Path,
        default=Path("config.json"),
        help="Path to config JSON. Defaults to ./config.json.",
    )
    parser.add_argument(
        "--demo",
        action="store_true",
        help="Use simulated sensor data. Useful for testing without Raspberry Pi hardware.",
    )
    args = parser.parse_args()

    config = load_config(args.config)
    if args.demo:
        config["runtime"]["demo_mode"] = True
    configure_logging(config)

    server = SmartRoomSensorServer(config)
    loop = asyncio.get_running_loop()

    def request_stop() -> None:
        logging.info("Stopping server...")
        server.stop()

    for sig in (signal.SIGINT, signal.SIGTERM):
        try:
            loop.add_signal_handler(sig, request_stop)
        except NotImplementedError:
            signal.signal(sig, lambda *_: request_stop())

    try:
        await server.run()
    finally:
        server.cleanup()


if __name__ == "__main__":
    try:
        asyncio.run(async_main())
    except KeyboardInterrupt:
        sys.exit(0)
