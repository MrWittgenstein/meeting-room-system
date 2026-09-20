#!/usr/bin/env python3
"""Clean smart-meeting-room telemetry data and write a quality report.

The input schema is derived from the Raspberry Pi telemetry payload and the
Spring Boot IotTelemetryRequest/IotSensorRecord models in this repository.
"""

from __future__ import annotations

import argparse
import re
from collections import Counter
from pathlib import Path
from typing import Any

import pandas as pd


REPO_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_INPUT = REPO_ROOT / "data" / "raw" / "原始数据样例.csv"
DEFAULT_OUTPUT = REPO_ROOT / "data" / "processed" / "clean_sensor_data.csv"
DEFAULT_REPORT = REPO_ROOT / "docs" / "data_quality_report.md"

EXPECTED_COLUMNS = [
    "timestamp",
    "room_id",
    "room_db_id",
    "device_id",
    "event_type",
    "temperature",
    "humidity",
    "light_raw",
    "smoke_raw",
    "occupancy",
    "people_count",
    "door_state",
    "sensor_status",
]

OUTPUT_COLUMNS = [
    *EXPECTED_COLUMNS,
    "quality",
    "quality_issues",
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="清洗智能会议室传感器 CSV，并生成数据质量报告。"
    )
    parser.add_argument("--input", type=Path, default=DEFAULT_INPUT, help="原始 CSV 路径")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT, help="清洗后 CSV 路径")
    parser.add_argument("--report", type=Path, default=DEFAULT_REPORT, help="质量报告路径")
    return parser.parse_args()


def normalize_column_name(name: str) -> str:
    name = re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", str(name).strip())
    name = re.sub(r"[^0-9A-Za-z]+", "_", name)
    return name.strip("_").lower()


def blank_to_na(value: Any) -> Any:
    if isinstance(value, str) and not value.strip():
        return pd.NA
    return value


def normalize_text(value: Any) -> Any:
    if pd.isna(value):
        return pd.NA
    text = str(value).strip().lower()
    return text or pd.NA


def parse_timestamp(value: Any) -> pd.Timestamp | pd.NaT:
    if pd.isna(value) or not str(value).strip():
        return pd.NaT

    parsed = pd.to_datetime(str(value).strip(), errors="coerce")
    if pd.isna(parsed):
        return pd.NaT

    timestamp = pd.Timestamp(parsed)
    if timestamp.tzinfo is not None:
        timestamp = timestamp.tz_convert("Asia/Shanghai").tz_localize(None)
    return timestamp


def normalize_room_id(value: Any) -> Any:
    """Convert room variants to the repository's meeting_room_<id> convention."""
    if pd.isna(value):
        return pd.NA
    matches = re.findall(r"\d+", str(value))
    if not matches:
        return pd.NA
    room_number = int(matches[-1])
    if room_number <= 0:
        return pd.NA
    return f"meeting_room_{room_number:02d}"


def normalize_binary(value: Any) -> Any:
    if pd.isna(value):
        return pd.NA
    normalized = str(value).strip().lower()
    truthy = {"1", "1.0", "true", "yes", "y", "occupied", "present", "on"}
    falsy = {"0", "0.0", "false", "no", "n", "empty", "vacant", "off"}
    if normalized in truthy:
        return 1
    if normalized in falsy:
        return 0
    return pd.NA


def normalize_door_state(value: Any) -> Any:
    normalized = normalize_text(value)
    if pd.isna(normalized):
        return pd.NA
    aliases = {
        "open": "open",
        "opened": "open",
        "close": "closed",
        "closed": "closed",
    }
    return aliases.get(normalized, pd.NA)


def normalize_sensor_status(value: Any) -> Any:
    normalized = normalize_text(value)
    if normalized in {"ok", "warning", "error"}:
        return normalized
    return pd.NA


def relative_display(path: Path) -> str:
    try:
        return path.resolve().relative_to(REPO_ROOT).as_posix()
    except ValueError:
        return str(path.resolve())


def clean_data(input_path: Path, output_path: Path, report_path: Path) -> dict[str, Any]:
    raw = pd.read_csv(input_path, encoding="utf-8-sig")
    before_count = len(raw)

    raw.columns = [normalize_column_name(column) for column in raw.columns]
    raw = raw.map(blank_to_na)
    raw = raw.rename(columns={"room_code": "room_id", "presence": "occupancy"})

    duplicated_columns = raw.columns[raw.columns.duplicated()].tolist()
    if duplicated_columns:
        raise ValueError(f"字段规范化后出现重名列: {duplicated_columns}")

    missing_columns = [column for column in EXPECTED_COLUMNS if column not in raw.columns]
    if missing_columns:
        raise ValueError(f"原始数据缺少必要字段: {missing_columns}")

    df = raw[EXPECTED_COLUMNS].copy()
    missing_before = df.isna().sum().to_dict()
    issues = pd.Series([[] for _ in range(len(df))], index=df.index, dtype=object)
    anomaly_counts: Counter[str] = Counter()

    def add_issue(mask: pd.Series, label: str) -> None:
        normalized_mask = mask.fillna(False).astype(bool)
        count = int(normalized_mask.sum())
        if count == 0:
            return
        anomaly_counts[label] += count
        for index in df.index[normalized_mask]:
            issues.at[index].append(label)

    original_timestamp_present = df["timestamp"].notna()
    df["timestamp"] = df["timestamp"].map(parse_timestamp)
    add_issue(original_timestamp_present & df["timestamp"].isna(), "invalid_timestamp")
    add_issue(~original_timestamp_present, "missing_timestamp")

    original_room_id = df["room_id"].copy()
    df["room_id"] = df["room_id"].map(normalize_room_id)
    add_issue(original_room_id.notna() & df["room_id"].isna(), "invalid_room_id")
    add_issue(original_room_id.isna(), "missing_room_id")

    input_room_db_id = pd.to_numeric(df["room_db_id"], errors="coerce").astype("Int64")
    derived_room_db_id = pd.to_numeric(
        df["room_id"].astype("string").str.extract(r"(\d+)$", expand=False),
        errors="coerce",
    ).astype("Int64")
    add_issue(
        input_room_db_id.notna()
        & derived_room_db_id.notna()
        & input_room_db_id.ne(derived_room_db_id),
        "room_db_id_mismatch",
    )
    df["room_db_id"] = derived_room_db_id

    df["device_id"] = df["device_id"].map(normalize_text)
    df["event_type"] = df["event_type"].map(normalize_text)
    missing_event_type = df["event_type"].isna()
    add_issue(missing_event_type, "missing_event_type_filled")
    df.loc[missing_event_type, "event_type"] = "telemetry"

    numeric_columns = [
        "temperature",
        "humidity",
        "light_raw",
        "smoke_raw",
        "people_count",
    ]
    for column in numeric_columns:
        original_present = df[column].notna()
        df[column] = pd.to_numeric(df[column], errors="coerce")
        add_issue(original_present & df[column].isna(), f"invalid_{column}")

    temperature_outlier = df["temperature"].notna() & ~df["temperature"].between(0, 50)
    humidity_outlier = df["humidity"].notna() & ~df["humidity"].between(0, 100)
    light_outlier = df["light_raw"].notna() & ~df["light_raw"].between(0, 255)
    smoke_outlier = df["smoke_raw"].notna() & (df["smoke_raw"] < 0)
    people_outlier = df["people_count"].notna() & (df["people_count"] < 0)

    add_issue(temperature_outlier, "temperature_out_of_range")
    add_issue(humidity_outlier, "humidity_out_of_range")
    add_issue(light_outlier, "light_raw_out_of_range")
    add_issue(smoke_outlier, "smoke_raw_out_of_range")
    add_issue(people_outlier, "people_count_out_of_range")

    df.loc[temperature_outlier, "temperature"] = pd.NA
    df.loc[humidity_outlier, "humidity"] = pd.NA
    df.loc[light_outlier, "light_raw"] = pd.NA
    df.loc[smoke_outlier, "smoke_raw"] = pd.NA
    df.loc[people_outlier, "people_count"] = pd.NA

    original_occupancy = df["occupancy"].copy()
    df["occupancy"] = df["occupancy"].map(normalize_binary).astype("Int64")
    add_issue(original_occupancy.notna() & df["occupancy"].isna(), "invalid_occupancy")

    original_door_state = df["door_state"].copy()
    df["door_state"] = df["door_state"].map(normalize_door_state)
    add_issue(original_door_state.notna() & df["door_state"].isna(), "invalid_door_state")

    original_sensor_status = df["sensor_status"].copy()
    df["sensor_status"] = df["sensor_status"].map(normalize_sensor_status)
    add_issue(
        original_sensor_status.notna() & df["sensor_status"].isna(),
        "invalid_sensor_status",
    )

    for column in [
        "temperature",
        "humidity",
        "occupancy",
        "people_count",
        "door_state",
        "sensor_status",
    ]:
        add_issue(df[column].isna(), f"missing_{column}")

    mandatory_invalid = df["timestamp"].isna() | df["room_id"].isna()
    dropped_mandatory_count = int(mandatory_invalid.sum())
    df = df.loc[~mandatory_invalid].copy()
    issues = issues.loc[df.index]

    duplicate_key = ["timestamp", "room_id", "device_id", "event_type"]
    duplicate_mask = df.duplicated(subset=duplicate_key, keep="first")
    duplicate_count = int(duplicate_mask.sum())
    df = df.loc[~duplicate_mask].copy()
    issues = issues.loc[df.index]

    df["quality_issues"] = issues.map(lambda values: ";".join(dict.fromkeys(values)))
    df["quality"] = df["quality_issues"].map(lambda value: "normal" if not value else "abnormal")

    df = df.sort_values(["timestamp", "room_id", "device_id"], kind="stable")
    df["timestamp"] = df["timestamp"].dt.strftime("%Y-%m-%d %H:%M:%S")
    df["light_raw"] = df["light_raw"].astype("Int64")
    df["smoke_raw"] = df["smoke_raw"].astype("Int64")
    df["people_count"] = df["people_count"].astype("Int64")

    missing_after = df[EXPECTED_COLUMNS].isna().sum().to_dict()
    after_count = len(df)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.parent.mkdir(parents=True, exist_ok=True)
    df[OUTPUT_COLUMNS].to_csv(output_path, index=False, encoding="utf-8-sig", na_rep="")

    report = build_report(
        input_path=input_path,
        output_path=output_path,
        before_count=before_count,
        after_count=after_count,
        duplicate_count=duplicate_count,
        dropped_mandatory_count=dropped_mandatory_count,
        missing_before=missing_before,
        missing_after=missing_after,
        anomaly_counts=anomaly_counts,
        cleaned=df,
    )
    report_path.write_text(report, encoding="utf-8")

    return {
        "before_count": before_count,
        "after_count": after_count,
        "duplicate_count": duplicate_count,
        "dropped_mandatory_count": dropped_mandatory_count,
        "output_path": output_path,
        "report_path": report_path,
    }


def build_report(
    *,
    input_path: Path,
    output_path: Path,
    before_count: int,
    after_count: int,
    duplicate_count: int,
    dropped_mandatory_count: int,
    missing_before: dict[str, int],
    missing_after: dict[str, int],
    anomaly_counts: Counter[str],
    cleaned: pd.DataFrame,
) -> str:
    missing_rows = [
        f"| `{column}` | {int(missing_before.get(column, 0))} | {int(missing_after.get(column, 0))} |"
        for column in EXPECTED_COLUMNS
    ]

    anomaly_actions = {
        "invalid_timestamp": "时间无法转换，记录不进入清洗结果",
        "missing_timestamp": "时间为必填字段，记录不进入清洗结果",
        "invalid_room_id": "会议室编号无法识别，记录不进入清洗结果",
        "missing_room_id": "会议室编号为必填字段，记录不进入清洗结果",
        "room_db_id_mismatch": "以 room_id 中的数字为准重新生成 room_db_id",
        "temperature_out_of_range": "置为空值并标记 abnormal",
        "humidity_out_of_range": "置为空值并标记 abnormal",
        "light_raw_out_of_range": "置为空值并标记 abnormal",
        "smoke_raw_out_of_range": "置为空值并标记 abnormal",
        "people_count_out_of_range": "置为空值并标记 abnormal",
        "invalid_occupancy": "置为空值并标记 abnormal",
        "invalid_door_state": "置为空值并标记 abnormal",
        "invalid_sensor_status": "置为空值并标记 abnormal",
    }
    anomaly_rows = []
    for label, action in anomaly_actions.items():
        count = int(anomaly_counts.get(label, 0))
        if count:
            anomaly_rows.append(f"| `{label}` | {count} | {action} |")
    if not anomaly_rows:
        anomaly_rows.append("| 无 | 0 | 无需处理 |")

    room_counts = cleaned["room_id"].value_counts().sort_index()
    room_rows = [f"| `{room_id}` | {int(count)} |" for room_id, count in room_counts.items()]
    normal_count = int((cleaned["quality"] == "normal").sum())
    abnormal_count = int((cleaned["quality"] == "abnormal").sum())

    return "\n".join(
        [
            "# 会议室传感器数据质量报告",
            "",
            "## 数据说明",
            "",
            f"- 原始文件：`{relative_display(input_path)}`",
            f"- 清洗结果：`{relative_display(output_path)}`",
            "- 数据性质：当前没有可导出的真实历史数据，因此本次使用模拟原始数据。",
            "- 字段来源：本仓库树莓派遥测载荷、后端 `IotTelemetryRequest`、`IotSensorRecord` 和 `iot_sensor_record` 表结构。",
            "- 模拟数据专门包含重复、缺失、越界值和命名不一致，用于验证清洗流程；不得描述为真实树莓派采集数据。",
            "",
            "## 清洗前后对比",
            "",
            "| 指标 | 数量 |",
            "| --- | ---: |",
            f"| 原始记录 | {before_count} |",
            f"| 删除的重复记录 | {duplicate_count} |",
            f"| 因时间或会议室编号无效而删除 | {dropped_mandatory_count} |",
            f"| 清洗后记录 | {after_count} |",
            f"| 质量正常记录 | {normal_count} |",
            f"| 质量异常或不完整记录 | {abnormal_count} |",
            "",
            "## 字段和格式统一",
            "",
            "- 列名统一为小写 snake_case。",
            "- 原始 `room_code` 统一命名为 `room_id`；数据库整数主键保留为 `room_db_id`。",
            "- `room-14`、`room_14`、`14`、`Meeting Room 14` 等写法统一为 `meeting_room_14`。",
            "- 时间统一为 `YYYY-MM-DD HH:MM:SS`，并按时间、会议室和设备升序排列。",
            "- 原始 `presence` 统一为分析字段 `occupancy`，取值统一为 0 或 1。",
            "- `opened`、`open` 统一为 `open`，`close`、`closed` 统一为 `closed`。",
            "- `device_id`、`event_type` 和 `sensor_status` 统一为小写。",
            "",
            "## 异常值检查",
            "",
            "| 异常类型 | 发现数量 | 处理方式 |",
            "| --- | ---: | --- |",
            *anomaly_rows,
            "",
            "温度允许范围为 0 至 50 摄氏度，湿度允许范围为 0% 至 100%，`light_raw` 允许范围为 0 至 255，人数不得小于 0。烟雾值 650 和 950 分别用于模拟告警和严重告警，它们不是格式错误，因此予以保留。",
            "",
            "## 缺失值统计",
            "",
            "| 字段 | 原始缺失数 | 清洗后缺失数 |",
            "| --- | ---: | ---: |",
            *missing_rows,
            "",
            "处理原则：时间和会议室编号是定位记录所必需的字段，缺失或非法时删除整条记录；温度、湿度、人员状态等传感器字段不凭空填充，保留为空并通过 `quality=abnormal` 和 `quality_issues` 说明原因。",
            "",
            "## 清洗后会议室分布",
            "",
            "| 会议室 | 记录数 |",
            "| --- | ---: |",
            *room_rows,
            "",
            "## 可复现方式",
            "",
            "在仓库根目录执行：",
            "",
            "```powershell",
            "python -m pip install -r scripts/requirements.txt",
            "python scripts/clean_data.py",
            "```",
            "",
            "脚本每次都从原始 CSV 重新生成清洗结果和本报告，不依赖上一次运行的输出。",
            "",
        ]
    )


def main() -> None:
    args = parse_args()
    result = clean_data(args.input, args.output, args.report)
    print(f"清洗前记录数: {result['before_count']}")
    print(f"删除重复记录数: {result['duplicate_count']}")
    print(f"删除必填字段无效记录数: {result['dropped_mandatory_count']}")
    print(f"清洗后记录数: {result['after_count']}")
    print(f"清洗结果: {result['output_path']}")
    print(f"质量报告: {result['report_path']}")


if __name__ == "__main__":
    main()
