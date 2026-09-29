#!/usr/bin/env python3
"""Generate the Week 4 analysis tables, figures, report, and web assets."""

from __future__ import annotations

import argparse
import json
import shutil
from pathlib import Path
from typing import Any

import matplotlib

matplotlib.use("Agg")
import matplotlib as mpl
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd


REPO_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_INPUT = REPO_ROOT / "data" / "processed" / "clean_sensor_data.csv"
DEFAULT_DOCS_DIR = REPO_ROOT / "docs" / "week4"
DEFAULT_WEB_DIR = REPO_ROOT / "frontend" / "public" / "analysis"

DEVICE_LABELS = {
    "lighting_on": "灯光",
    "air_conditioner_on": "空调",
    "projector_on": "投影",
}
ROOM_LABELS = {
    "meeting_room_14": "会议室 14",
    "meeting_room_15": "会议室 15",
    "meeting_room_32": "会议室 32",
}

mpl.rcParams.update(
    {
        "font.family": "sans-serif",
        "font.sans-serif": ["Microsoft YaHei", "SimHei", "Arial", "DejaVu Sans"],
        "font.size": 8,
        "axes.titlesize": 10,
        "axes.labelsize": 8,
        "xtick.labelsize": 7,
        "ytick.labelsize": 7,
        "legend.fontsize": 7,
        "axes.spines.right": False,
        "axes.spines.top": False,
        "axes.linewidth": 0.8,
        "legend.frameon": False,
        "svg.fonttype": "none",
        "pdf.fonttype": 42,
        "savefig.facecolor": "white",
    }
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="生成第 4 周数据分析图表与报告。")
    parser.add_argument("--input", type=Path, default=DEFAULT_INPUT, help="清洗后 CSV 路径")
    parser.add_argument("--docs-dir", type=Path, default=DEFAULT_DOCS_DIR, help="文档输出目录")
    parser.add_argument("--web-dir", type=Path, default=DEFAULT_WEB_DIR, help="前端静态资源目录")
    return parser.parse_args()


def parse_boolean_series(series: pd.Series) -> pd.Series:
    mapping = {
        "true": True,
        "1": True,
        "yes": True,
        "on": True,
        "false": False,
        "0": False,
        "no": False,
        "off": False,
    }
    return series.astype("string").str.strip().str.lower().map(mapping).astype("boolean")


def load_analysis_data(input_path: Path) -> pd.DataFrame:
    df = pd.read_csv(input_path, encoding="utf-8-sig")
    required = {
        "timestamp",
        "room_id",
        "data_mode",
        "occupancy",
        *DEVICE_LABELS,
    }
    missing = sorted(required.difference(df.columns))
    if missing:
        raise ValueError(f"清洗数据缺少第 4 周分析字段: {missing}")

    df["timestamp"] = pd.to_datetime(df["timestamp"], errors="coerce")
    if df["timestamp"].isna().any():
        raise ValueError("清洗数据仍包含无法解析的 timestamp")
    for column in ["occupancy", *DEVICE_LABELS]:
        df[column] = parse_boolean_series(df[column])
    df["data_mode"] = df["data_mode"].astype("string").str.lower()
    df = df.sort_values(["room_id", "timestamp"], kind="stable").reset_index(drop=True)
    return df


def infer_sampling_minutes(df: pd.DataFrame) -> int:
    differences = (
        df.groupby("room_id", sort=False)["timestamp"]
        .diff()
        .dropna()
        .dt.total_seconds()
        .div(60)
    )
    if differences.empty:
        raise ValueError("无法从数据推断采样间隔")
    interval = int(differences.mode().iloc[0])
    if interval <= 0:
        raise ValueError(f"采样间隔必须为正数，实际为 {interval}")
    return interval


def compute_analysis_tables(df: pd.DataFrame) -> dict[str, Any]:
    interval_minutes = infer_sampling_minutes(df)
    valid_mode = df["data_mode"].isin(["real", "simulated"])
    valid_occupancy = valid_mode & df["occupancy"].notna()
    device_columns = list(DEVICE_LABELS)
    valid_devices = valid_occupancy & df[device_columns].notna().all(axis=1)

    working = df.loc[valid_devices].copy()
    working["date"] = working["timestamp"].dt.date
    working["hour"] = working["timestamp"].dt.hour
    working["any_device_on"] = working[device_columns].any(axis=1)
    working["invalid_runtime"] = (~working["occupancy"]) & working["any_device_on"]
    working["invalid_room_minutes"] = working["invalid_runtime"].astype(int) * interval_minutes

    all_dates = pd.date_range(df["timestamp"].min().normalize(), df["timestamp"].max().normalize(), freq="D")
    heatmap = (
        working.groupby(["date", "hour"])["invalid_room_minutes"]
        .sum()
        .unstack(fill_value=0)
        .reindex(index=[value.date() for value in all_dates], columns=range(24), fill_value=0)
    )

    device_records: list[dict[str, Any]] = []
    for column, label in DEVICE_LABELS.items():
        valid = valid_occupancy & df[column].notna()
        subset = df.loc[valid, ["occupancy", column]]
        on_count = int(subset[column].sum())
        invalid_count = int(((~subset["occupancy"]) & subset[column]).sum())
        valid_count = len(subset)
        device_records.append(
            {
                "device": column,
                "label": label,
                "valid_samples": valid_count,
                "on_samples": on_count,
                "invalid_on_samples": invalid_count,
                "on_minutes": on_count * interval_minutes,
                "invalid_minutes": invalid_count * interval_minutes,
                "usage_rate": on_count / valid_count if valid_count else np.nan,
                "invalid_ratio": invalid_count / on_count if on_count else np.nan,
            }
        )
    device_metrics = pd.DataFrame(device_records)

    active = df.loc[
        valid_occupancy & df["timestamp"].dt.hour.between(8, 21),
        ["timestamp", "room_id", "occupancy"],
    ].copy()
    active["hour"] = active["timestamp"].dt.hour
    hourly_occupancy = (
        active.groupby(["room_id", "hour"], as_index=False)["occupancy"]
        .agg(occupancy_rate="mean", sample_size="count")
        .sort_values(["room_id", "hour"])
    )

    return {
        "interval_minutes": interval_minutes,
        "valid_occupancy_records": int(valid_occupancy.sum()),
        "valid_device_records": int(valid_devices.sum()),
        "working": working,
        "heatmap": heatmap,
        "device_metrics": device_metrics,
        "hourly_occupancy": hourly_occupancy,
    }


def save_figure(fig: plt.Figure, output_dir: Path, stem: str) -> dict[str, str]:
    output_dir.mkdir(parents=True, exist_ok=True)
    paths = {
        "png": output_dir / f"{stem}.png",
        "pdf": output_dir / f"{stem}.pdf",
        "svg": output_dir / f"{stem}.svg",
    }
    fig.savefig(paths["png"], dpi=300, bbox_inches="tight")
    fig.savefig(paths["pdf"], bbox_inches="tight")
    fig.savefig(paths["svg"], bbox_inches="tight")
    plt.close(fig)
    return {key: value.name for key, value in paths.items()}


def plot_invalid_runtime_heatmap(heatmap: pd.DataFrame, figures_dir: Path) -> dict[str, str]:
    fig, ax = plt.subplots(figsize=(7.1, 4.6), constrained_layout=True)
    image = ax.imshow(heatmap.to_numpy(), aspect="auto", cmap="Blues", vmin=0)
    ax.set_title("无人但设备仍开启的时段分布")
    ax.set_xlabel("小时（0–23 时）")
    ax.set_ylabel("日期")
    ax.set_xticks(range(24))
    ax.set_xticklabels([str(hour) for hour in range(24)])
    ax.set_yticks(range(len(heatmap.index)))
    ax.set_yticklabels([value.strftime("%m-%d") for value in heatmap.index])
    colorbar = fig.colorbar(image, ax=ax, pad=0.02)
    colorbar.set_label("无效运行会议室分钟数")
    return save_figure(fig, figures_dir, "01_invalid_runtime_heatmap")


def plot_device_metrics(device_metrics: pd.DataFrame, figures_dir: Path) -> dict[str, str]:
    labels = device_metrics["label"].tolist()
    x = np.arange(len(labels))
    width = 0.34
    usage = device_metrics["usage_rate"].to_numpy() * 100
    invalid = device_metrics["invalid_ratio"].to_numpy() * 100

    fig, ax = plt.subplots(figsize=(6.5, 3.7), constrained_layout=True)
    bars_usage = ax.bar(x - width / 2, usage, width, label="设备使用率", color="#78A6C8")
    bars_invalid = ax.bar(x + width / 2, invalid, width, label="无效运行占开启时长比例", color="#D98B5F")
    ax.set_title("设备开启与无效运行比例")
    ax.set_ylabel("比例（%）")
    ax.set_xticks(x)
    ax.set_xticklabels(labels)
    ax.set_ylim(0, max(100, float(np.nanmax([usage, invalid])) * 1.18))
    ax.legend(loc="upper center", ncols=2)
    for bars in (bars_usage, bars_invalid):
        ax.bar_label(bars, fmt="%.1f%%", padding=3, fontsize=7)
    return save_figure(fig, figures_dir, "02_device_invalid_ratio")


def plot_hourly_occupancy(hourly: pd.DataFrame, figures_dir: Path) -> dict[str, str]:
    palette = {
        "meeting_room_14": "#366F9C",
        "meeting_room_15": "#5A9B76",
        "meeting_room_32": "#B36A5E",
    }
    fig, ax = plt.subplots(figsize=(6.5, 3.8), constrained_layout=True)
    for room_id, room_data in hourly.groupby("room_id", sort=True):
        ax.plot(
            room_data["hour"],
            room_data["occupancy_rate"] * 100,
            marker="o",
            markersize=3.5,
            linewidth=1.7,
            color=palette.get(room_id, "#666666"),
            label=ROOM_LABELS.get(room_id, room_id),
        )
    ax.set_title("工作时段各会议室占用率")
    ax.set_xlabel("小时（08:00–22:00）")
    ax.set_ylabel("占用率（%）")
    ax.set_xticks(range(8, 22))
    ax.set_ylim(0, 100)
    ax.grid(axis="y", color="#D9D9D9", linewidth=0.6, alpha=0.7)
    ax.legend(loc="upper center", ncols=3)
    return save_figure(fig, figures_dir, "03_hourly_occupancy")


def build_summary(df: pd.DataFrame, tables: dict[str, Any], figure_files: list[dict[str, Any]]) -> dict[str, Any]:
    heatmap = tables["heatmap"]
    peak_position = np.unravel_index(int(np.nanargmax(heatmap.to_numpy())), heatmap.shape)
    peak_date = heatmap.index[peak_position[0]]
    peak_hour = int(heatmap.columns[peak_position[1]])
    peak_minutes = int(heatmap.iloc[peak_position])

    device_metrics = tables["device_metrics"]
    worst_device = device_metrics.loc[device_metrics["invalid_ratio"].idxmax()]
    hourly = tables["hourly_occupancy"]
    busiest = hourly.loc[hourly["occupancy_rate"].idxmax()]

    summary = {
        "title": "第 4 周会议室数据分析",
        "data": {
            "mode": sorted(df["data_mode"].dropna().unique().tolist()),
            "start": df["timestamp"].min().strftime("%Y-%m-%d %H:%M:%S"),
            "end": df["timestamp"].max().strftime("%Y-%m-%d %H:%M:%S"),
            "records": int(len(df)),
            "rooms": int(df["room_id"].nunique()),
            "sampling_minutes": tables["interval_minutes"],
            "valid_occupancy_records": tables["valid_occupancy_records"],
            "valid_device_records": tables["valid_device_records"],
        },
        "definitions": {
            "occupancy_rate": "08:00–22:00 内 occupancy=true 的有效采样数 ÷ occupancy 有效采样数。",
            "invalid_runtime": "occupancy=false 且灯光、空调或投影至少一项为 true 的有效 5 分钟会议室时段。",
            "device_invalid_ratio": "某设备在无人时开启的有效分钟数 ÷ 该设备全部开启分钟数。",
        },
        "highlights": [
            {
                "label": "无效运行峰值",
                "value": f"{peak_date:%Y-%m-%d} {peak_hour:02d}:00",
                "detail": f"该小时累计 {peak_minutes} 个会议室分钟存在无人但设备仍开启。",
            },
            {
                "label": "无效运行比例最高设备",
                "value": str(worst_device["label"]),
                "detail": f"无人开启占该设备全部开启时长的 {worst_device['invalid_ratio']:.1%}（{int(worst_device['invalid_minutes'])} 分钟）。",
            },
            {
                "label": "占用率峰值",
                "value": f"{ROOM_LABELS.get(str(busiest['room_id']), busiest['room_id'])} {int(busiest['hour']):02d}:00",
                "detail": f"08:00–22:00 口径下占用率为 {busiest['occupancy_rate']:.1%}，样本量 n={int(busiest['sample_size'])}。",
            },
        ],
        "device_metrics": [
            {
                "device": row.device,
                "label": row.label,
                "valid_samples": int(row.valid_samples),
                "on_minutes": int(row.on_minutes),
                "invalid_minutes": int(row.invalid_minutes),
                "usage_rate": round(float(row.usage_rate), 6),
                "invalid_ratio": round(float(row.invalid_ratio), 6),
            }
            for row in device_metrics.itertuples(index=False)
        ],
        "figures": figure_files,
        "limitations": [
            "当前数据由固定随机种子生成，data_mode=simulated，不能表述为真实传感器实测结论。",
            "5 分钟采样只支持以 5 分钟为最小单位估计无效运行时长。",
        ],
    }
    return summary


def write_analysis_report(summary: dict[str, Any], output_path: Path) -> None:
    figure_lines = []
    for index, figure in enumerate(summary["figures"], start=1):
        figure_lines.extend(
            [
                f"### 图 {index} {figure['title']}",
                "",
                f"![{figure['title']}](figures/{figure['file']})",
                "",
                f"- 口径：{figure['definition']}",
                f"- 数字结论：{figure['conclusion']}",
                "",
            ]
        )

    highlights = summary["highlights"]
    baseline = summary.get("baseline")
    if baseline:
        metrics = baseline["metrics"]
        comparison = baseline["comparison"]
        baseline_lines = [
            "## 基线预测",
            "",
            f"- 任务：使用当前及历史时段信息，预测 {baseline['horizon_minutes']} 分钟后会议室是否有人。",
            f"- 切分：训练集 {baseline['train_period']['start']} 至 {baseline['train_period']['end']}，n={baseline['train_period']['samples']}；测试集 {baseline['test_period']['start']} 至 {baseline['test_period']['end']}，n={baseline['test_period']['samples']}。",
            f"- 主要指标：准确率 {metrics['accuracy']:.1%}，F1 {metrics['f1']:.1%}，平衡准确率 {metrics['balanced_accuracy']:.1%}。",
            f"- 对照：直接用当前占用状态预测 30 分钟后的持久性基线，准确率 {comparison['accuracy']:.1%}，F1 {comparison['f1']:.1%}。",
            f"- 混淆矩阵：TP={metrics['tp']}、TN={metrics['tn']}、FP={metrics['fp']}、FN={metrics['fn']}。",
            "- 解释：该结果只验证数据到模型到展示的技术链路；输入为模拟数据，不代表真实部署效果。",
            "",
        ]
    else:
        baseline_lines = [
            "## 基线预测",
            "",
            "尚未运行 `scripts/baseline_model.py`。",
            "",
        ]
    report = "\n".join(
        [
            "# 第 4 周数据分析与基线结果",
            "",
            "## 数据范围",
            "",
            f"- 数据性质：教学模拟数据，`data_mode=simulated`。",
            f"- 时间范围：{summary['data']['start']} 至 {summary['data']['end']}。",
            f"- 规模：{summary['data']['records']} 条记录，{summary['data']['rooms']} 个会议室，每 {summary['data']['sampling_minutes']} 分钟采样一次。",
            f"- 设备分析有效样本：{summary['data']['valid_device_records']} 条。",
            "",
            "## 主要结论",
            "",
            *[
                f"- **{item['label']}：{item['value']}。** {item['detail']}"
                for item in highlights
            ],
            "",
            "## 图表结果",
            "",
            *figure_lines,
            *baseline_lines,
            "## 可复现方式",
            "",
            "```powershell",
            "python -m pip install -r scripts/requirements.txt",
            "python scripts/run_week4.py",
            "python -m unittest discover -s tests -p \"test_*.py\" -v",
            "```",
            "",
            "## 限制",
            "",
            *[f"- {item}" for item in summary["limitations"]],
            "",
        ]
    )
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(report, encoding="utf-8")


def run_analysis(input_path: Path, docs_dir: Path, web_dir: Path) -> dict[str, Any]:
    df = load_analysis_data(input_path)
    tables = compute_analysis_tables(df)
    figures_dir = docs_dir / "figures"
    results_dir = docs_dir / "results"
    web_figures_dir = web_dir / "figures"
    results_dir.mkdir(parents=True, exist_ok=True)
    web_figures_dir.mkdir(parents=True, exist_ok=True)

    heatmap_files = plot_invalid_runtime_heatmap(tables["heatmap"], figures_dir)
    device_files = plot_device_metrics(tables["device_metrics"], figures_dir)
    hourly_files = plot_hourly_occupancy(tables["hourly_occupancy"], figures_dir)

    heatmap = tables["heatmap"]
    peak_position = np.unravel_index(int(np.nanargmax(heatmap.to_numpy())), heatmap.shape)
    peak_date = heatmap.index[peak_position[0]]
    peak_hour = int(heatmap.columns[peak_position[1]])
    peak_minutes = int(heatmap.iloc[peak_position])
    device_metrics = tables["device_metrics"]
    worst_device = device_metrics.loc[device_metrics["invalid_ratio"].idxmax()]
    hourly = tables["hourly_occupancy"]
    busiest = hourly.loc[hourly["occupancy_rate"].idxmax()]

    figures = [
        {
            "title": "无人但设备仍开启的时段分布",
            "file": heatmap_files["png"],
            "definition": "每个日期和小时内，occupancy=false 且至少一个设备开启的 5 分钟会议室时段累计分钟数。",
            "conclusion": f"峰值出现在 {peak_date:%Y-%m-%d} {peak_hour:02d}:00，累计 {peak_minutes} 个会议室分钟。",
        },
        {
            "title": "设备开启与无效运行比例",
            "file": device_files["png"],
            "definition": "使用率=开启样本数÷有效样本数；无效运行比例=无人时开启分钟数÷全部开启分钟数。",
            "conclusion": f"{worst_device['label']}的无效运行比例最高，为 {worst_device['invalid_ratio']:.1%}，对应 {int(worst_device['invalid_minutes'])} 分钟。",
        },
        {
            "title": "工作时段各会议室占用率",
            "file": hourly_files["png"],
            "definition": "08:00–22:00 内按会议室和小时统计，occupancy=true 的采样数÷occupancy 有效采样数。",
            "conclusion": f"最高点为 {ROOM_LABELS.get(str(busiest['room_id']), busiest['room_id'])} {int(busiest['hour']):02d}:00，占用率 {busiest['occupancy_rate']:.1%}，n={int(busiest['sample_size'])}。",
        },
    ]

    tables["heatmap"].to_csv(results_dir / "invalid_runtime_heatmap.csv", encoding="utf-8-sig")
    tables["device_metrics"].to_csv(results_dir / "device_metrics.csv", index=False, encoding="utf-8-sig")
    tables["hourly_occupancy"].to_csv(results_dir / "hourly_occupancy.csv", index=False, encoding="utf-8-sig")

    for figure in figures:
        shutil.copy2(figures_dir / figure["file"], web_figures_dir / figure["file"])

    summary = build_summary(df, tables, figures)
    summary_path = results_dir / "analysis_summary.json"
    summary_path.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    web_dir.mkdir(parents=True, exist_ok=True)
    (web_dir / "week4_summary.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    write_analysis_report(summary, docs_dir / "analysis_report.md")
    return summary


def main() -> None:
    args = parse_args()
    summary = run_analysis(args.input, args.docs_dir, args.web_dir)
    print(f"分析记录数: {summary['data']['records']}")
    print(f"生成图表数: {len(summary['figures'])}")
    for item in summary["highlights"]:
        print(f"{item['label']}: {item['value']} - {item['detail']}")


if __name__ == "__main__":
    main()
