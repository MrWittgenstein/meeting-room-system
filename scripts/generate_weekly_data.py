#!/usr/bin/env python3
"""Generate one deterministic week of simulated smart-room telemetry.

The data follows the Raspberry Pi/Spring telemetry fields already used by the
repository. A small, fixed set of malformed records is included so the
cleaning pipeline can demonstrate duplicate, missing, invalid and out-of-range
handling without claiming that the file contains real sensor observations.
"""

from __future__ import annotations

import argparse
import csv
import math
import random
from copy import deepcopy
from datetime import datetime, timedelta
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT = REPO_ROOT / "data" / "raw" / "原始数据样例.csv"
START_TIME = datetime(2026, 9, 15, 0, 0)
DAYS = 7
INTERVAL_MINUTES = 5
RANDOM_SEED = 20260915

HEADERS = [
    "Timestamp",
    "Room Code",
    "Room DB ID",
    "Device ID",
    "Event Type",
    "Temperature",
    "Humidity",
    "Light Raw",
    "Smoke Raw",
    "Presence",
    "People Count",
    "Door State",
    "Sensor Status",
]

ROOMS = [
    {
        "room_id": "meeting_room_14",
        "room_db_id": 14,
        "device_id": "raspi-01",
        "capacity": 12,
        "temperature": 24.2,
        "humidity": 50.0,
        "schedule": ((9 * 60, 11 * 60 + 30), (14 * 60, 17 * 60)),
    },
    {
        "room_id": "meeting_room_15",
        "room_db_id": 15,
        "device_id": "raspi-02",
        "capacity": 8,
        "temperature": 23.4,
        "humidity": 47.0,
        "schedule": ((8 * 60 + 30, 10 * 60), (13 * 60, 15 * 60), (17 * 60, 18 * 60 + 30)),
    },
    {
        "room_id": "meeting_room_32",
        "room_db_id": 32,
        "device_id": "raspi-03",
        "capacity": 20,
        "temperature": 22.8,
        "humidity": 55.0,
        "schedule": ((10 * 60, 12 * 60), (14 * 60 + 30, 18 * 60)),
    },
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="生成连续一周的会议室模拟监测数据。")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT, help="原始 CSV 输出路径")
    return parser.parse_args()


def is_occupied(timestamp: datetime, room: dict[str, Any], room_index: int) -> bool:
    minute_of_day = timestamp.hour * 60 + timestamp.minute
    if timestamp.weekday() < 5:
        day_shift = ((timestamp.weekday() + room_index) % 3 - 1) * 10
        return any(start + day_shift <= minute_of_day < end + day_shift for start, end in room["schedule"])

    weekend_start = 10 * 60 + room_index * 45
    weekend_end = weekend_start + (90 if timestamp.weekday() == 5 else 60)
    return minute_of_day >= weekend_start and minute_of_day < weekend_end


def encode_presence(occupied: bool, sample_index: int, room_index: int) -> str:
    truthy = ("true", "1", "yes", "occupied")
    falsy = ("false", "0", "no", "vacant")
    choices = truthy if occupied else falsy
    return choices[(sample_index + room_index) % len(choices)]


def generate_records() -> list[dict[str, Any]]:
    rng = random.Random(RANDOM_SEED)
    records: list[dict[str, Any]] = []
    records_by_key: dict[tuple[datetime, str], dict[str, Any]] = {}
    sample_count = DAYS * 24 * 60 // INTERVAL_MINUTES

    for sample_index in range(sample_count):
        timestamp = START_TIME + timedelta(minutes=sample_index * INTERVAL_MINUTES)
        minute_of_day = timestamp.hour * 60 + timestamp.minute
        daily_wave = math.sin((minute_of_day - 8 * 60) / (24 * 60) * 2 * math.pi)

        for room_index, room in enumerate(ROOMS):
            occupied = is_occupied(timestamp, room, room_index)
            people_count = rng.randint(2, room["capacity"] - 1) if occupied else 0
            temperature = room["temperature"] + daily_wave * 1.3 + (0.7 if occupied else 0) + rng.uniform(-0.25, 0.25)
            humidity = room["humidity"] - daily_wave * 2.8 + (1.5 if occupied else 0) + rng.uniform(-0.8, 0.8)
            daylight = max(0.0, math.sin((minute_of_day - 6 * 60) / (12 * 60) * math.pi))
            light_raw = int(25 + daylight * 105 + (70 if occupied else 0) + rng.randint(-5, 5))
            smoke_raw = max(0, 8 + room_index * 2 + rng.randint(-3, 5))
            door_state = "open" if rng.random() < (0.035 if occupied else 0.008) else "closed"

            record = {
                "Timestamp": timestamp.strftime("%Y-%m-%d %H:%M:%S"),
                "Room Code": room["room_id"],
                "Room DB ID": room["room_db_id"],
                "Device ID": room["device_id"],
                "Event Type": "telemetry",
                "Temperature": f"{temperature:.1f}",
                "Humidity": f"{humidity:.1f}",
                "Light Raw": light_raw,
                "Smoke Raw": smoke_raw,
                "Presence": encode_presence(occupied, sample_index, room_index),
                "People Count": people_count,
                "Door State": door_state,
                "Sensor Status": "ok",
            }
            records.append(record)
            records_by_key[(timestamp, room["room_id"])] = record

    def update(timestamp_text: str, room_id: str, **changes: Any) -> None:
        record = records_by_key[(datetime.fromisoformat(timestamp_text), room_id)]
        record.update(changes)

    # Harmless source-format variants exercise the normalizers.
    update("2026-09-15 09:00:00", "meeting_room_14", **{"Timestamp": "2026/09/15 09:00"})
    update("2026-09-15 09:05:00", "meeting_room_15", **{"Room Code": "ROOM-15"})
    update("2026-09-15 09:10:00", "meeting_room_32", **{"Room Code": "Meeting Room 32", "Device ID": "RASPI-03"})
    update("2026-09-15 09:15:00", "meeting_room_14", **{"Door State": "opened"})

    # Fixed anomalies make the cleaning result and report reproducible.
    update("2026-09-16 03:00:00", "meeting_room_14", **{"Temperature": ""})
    update("2026-09-16 03:05:00", "meeting_room_14", **{"Temperature": 85})
    update("2026-09-16 03:10:00", "meeting_room_15", **{"Humidity": ""})
    update("2026-09-16 03:15:00", "meeting_room_15", **{"Humidity": 140})
    update("2026-09-16 03:20:00", "meeting_room_32", **{"Light Raw": 300})
    update("2026-09-16 03:25:00", "meeting_room_32", **{"Smoke Raw": -5})
    update("2026-09-16 03:30:00", "meeting_room_14", **{"Presence": ""})
    update("2026-09-16 03:35:00", "meeting_room_14", **{"Presence": "unknown"})
    update("2026-09-16 03:40:00", "meeting_room_15", **{"People Count": -1})
    update("2026-09-16 03:45:00", "meeting_room_15", **{"Door State": "ajar"})
    update("2026-09-16 03:50:00", "meeting_room_32", **{"Door State": ""})
    update("2026-09-16 03:55:00", "meeting_room_32", **{"Sensor Status": "offline"})
    update("2026-09-16 04:00:00", "meeting_room_14", **{"Device ID": ""})
    update("2026-09-16 04:05:00", "meeting_room_15", **{"Event Type": ""})
    update("2026-09-16 04:10:00", "meeting_room_32", **{"Room DB ID": 999})
    update("2026-09-17 11:00:00", "meeting_room_14", **{"Smoke Raw": 650, "Sensor Status": "warning"})
    update("2026-09-18 16:00:00", "meeting_room_32", **{"Smoke Raw": 950, "Sensor Status": "error"})

    # Mandatory-field failures are additional rows so the valid weekly series
    # remains complete after these rows are rejected by the cleaning pipeline.
    invalid_timestamp = deepcopy(records_by_key[(datetime(2026, 9, 16, 4, 15), "meeting_room_14")])
    invalid_timestamp["Timestamp"] = "not-a-time"
    missing_timestamp = deepcopy(records_by_key[(datetime(2026, 9, 16, 4, 20), "meeting_room_15")])
    missing_timestamp["Timestamp"] = ""
    invalid_room = deepcopy(records_by_key[(datetime(2026, 9, 16, 4, 25), "meeting_room_32")])
    invalid_room["Room Code"] = "unknown-room"
    records.extend((invalid_timestamp, missing_timestamp, invalid_room))

    # Append two exact duplicates; the cleaning script removes them by business key.
    records.append(deepcopy(records_by_key[(datetime(2026, 9, 17, 10, 0), "meeting_room_14")]))
    records.append(deepcopy(records_by_key[(datetime(2026, 9, 19, 16, 0), "meeting_room_32")]))
    return records


def generate_weekly_data(output_path: Path) -> dict[str, int | Path]:
    records = generate_records()
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8-sig", newline="") as output_file:
        writer = csv.DictWriter(output_file, fieldnames=HEADERS)
        writer.writeheader()
        writer.writerows(records)
    return {
        "base_count": DAYS * 24 * 60 // INTERVAL_MINUTES * len(ROOMS),
        "output_count": len(records),
        "output_path": output_path,
    }


def main() -> None:
    args = parse_args()
    result = generate_weekly_data(args.output)
    print(f"基础监测记录数: {result['base_count']}")
    print(f"含测试异常和重复的原始记录数: {result['output_count']}")
    print(f"原始数据: {result['output_path']}")


if __name__ == "__main__":
    main()
