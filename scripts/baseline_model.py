#!/usr/bin/env python3
"""Train and evaluate a reproducible Week 4 occupancy baseline."""

from __future__ import annotations

import argparse
import json
import shutil
from pathlib import Path
from typing import Any

import matplotlib.pyplot as plt
import matplotlib as mpl
import numpy as np
import pandas as pd

from week4_analysis import (
    DEFAULT_DOCS_DIR,
    DEFAULT_INPUT,
    DEFAULT_WEB_DIR,
    load_analysis_data,
    write_analysis_report,
)


HORIZON_MINUTES = 30

mpl.rcParams.update(
    {
        "font.family": "sans-serif",
        "font.sans-serif": ["Microsoft YaHei", "SimHei", "Arial", "DejaVu Sans"],
        "font.size": 8,
        "svg.fonttype": "none",
        "pdf.fonttype": 42,
        "savefig.facecolor": "white",
    }
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="训练并评估 30 分钟后占用状态基线。")
    parser.add_argument("--input", type=Path, default=DEFAULT_INPUT, help="清洗后 CSV 路径")
    parser.add_argument("--docs-dir", type=Path, default=DEFAULT_DOCS_DIR, help="文档输出目录")
    parser.add_argument("--web-dir", type=Path, default=DEFAULT_WEB_DIR, help="前端静态资源目录")
    return parser.parse_args()


def classification_metrics(actual: pd.Series, predicted: pd.Series) -> dict[str, float | int]:
    actual_values = actual.astype(bool).to_numpy()
    predicted_values = predicted.astype(bool).to_numpy()
    tp = int(np.sum(actual_values & predicted_values))
    tn = int(np.sum(~actual_values & ~predicted_values))
    fp = int(np.sum(~actual_values & predicted_values))
    fn = int(np.sum(actual_values & ~predicted_values))

    def divide(numerator: int, denominator: int) -> float:
        return numerator / denominator if denominator else 0.0

    precision = divide(tp, tp + fp)
    recall = divide(tp, tp + fn)
    specificity = divide(tn, tn + fp)
    return {
        "samples": int(len(actual_values)),
        "positive_rate": float(np.mean(actual_values)) if len(actual_values) else 0.0,
        "accuracy": divide(tp + tn, len(actual_values)),
        "precision": precision,
        "recall": recall,
        "specificity": specificity,
        "f1": divide(2 * precision * recall, precision + recall),
        "balanced_accuracy": (recall + specificity) / 2,
        "tp": tp,
        "tn": tn,
        "fp": fp,
        "fn": fn,
    }


def prepare_samples(df: pd.DataFrame, interval_minutes: int = 5) -> pd.DataFrame:
    steps = HORIZON_MINUTES // interval_minutes
    samples = df.loc[
        df["data_mode"].isin(["real", "simulated"]),
        ["timestamp", "room_id", "occupancy", "data_mode"],
    ].copy()
    grouped = samples.groupby("room_id", sort=False)
    samples["target_timestamp"] = grouped["timestamp"].shift(-steps)
    samples["target_occupancy"] = grouped["occupancy"].shift(-steps)
    samples = samples.loc[
        samples["occupancy"].notna()
        & samples["target_occupancy"].notna()
        & ((samples["target_timestamp"] - samples["timestamp"]) == pd.Timedelta(minutes=HORIZON_MINUTES))
        & samples["target_timestamp"].dt.hour.between(8, 21)
    ].copy()
    samples["target_date"] = samples["target_timestamp"].dt.date
    samples["target_weekday"] = samples["target_timestamp"].dt.dayofweek
    samples["target_slot"] = (
        samples["target_timestamp"].dt.hour * 2
        + samples["target_timestamp"].dt.minute.floordiv(30)
    )
    return samples.reset_index(drop=True)


def train_and_evaluate(samples: pd.DataFrame) -> tuple[pd.DataFrame, dict[str, Any]]:
    dates = sorted(samples["target_date"].unique())
    if len(dates) < 4:
        raise ValueError("基线评估至少需要 4 个自然日")
    # Four complete days are held out so the 14-day course dataset has a clear
    # 10-day training / 4-day testing chronology.
    split_index = len(dates) - 4 if len(dates) >= 14 else max(1, int(len(dates) * 0.7))
    train_dates = set(dates[:split_index])
    train = samples.loc[samples["target_date"].isin(train_dates)].copy()
    test = samples.loc[~samples["target_date"].isin(train_dates)].copy()
    if train.empty or test.empty:
        raise ValueError("按时间切分后训练集或测试集为空")

    exact = (
        train.groupby(["room_id", "target_weekday", "target_slot"])["target_occupancy"]
        .mean()
        .rename("probability")
        .reset_index()
    )
    room_slot = (
        train.groupby(["room_id", "target_slot"])["target_occupancy"]
        .mean()
        .rename("fallback_probability")
        .reset_index()
    )
    evaluated = test.merge(
        exact,
        on=["room_id", "target_weekday", "target_slot"],
        how="left",
    ).merge(room_slot, on=["room_id", "target_slot"], how="left")
    room_mean = train.groupby("room_id")["target_occupancy"].mean()
    global_mean = float(train["target_occupancy"].mean())
    evaluated["probability"] = (
        evaluated["probability"]
        .fillna(evaluated["fallback_probability"])
        .fillna(evaluated["room_id"].map(room_mean))
        .fillna(global_mean)
    )
    evaluated["predicted_occupancy"] = evaluated["probability"] >= 0.5
    evaluated["persistence_prediction"] = evaluated["occupancy"].astype(bool)

    result = {
        "model": "会议室×星期×30分钟时段历史占用率（阈值 0.5）",
        "horizon_minutes": HORIZON_MINUTES,
        "threshold": 0.5,
        "train_period": {
            "start": str(min(train_dates)),
            "end": str(max(train_dates)),
            "samples": int(len(train)),
        },
        "test_period": {
            "start": str(min(set(dates) - train_dates)),
            "end": str(max(set(dates) - train_dates)),
            "samples": int(len(evaluated)),
        },
        "metrics": classification_metrics(evaluated["target_occupancy"], evaluated["predicted_occupancy"]),
        "comparison": classification_metrics(evaluated["target_occupancy"], evaluated["persistence_prediction"]),
    }
    return evaluated, result


def plot_confusion_matrix(metrics: dict[str, Any], figures_dir: Path) -> dict[str, str]:
    matrix = np.array([[metrics["tn"], metrics["fp"]], [metrics["fn"], metrics["tp"]]])
    fig, ax = plt.subplots(figsize=(7.1, 4.6), constrained_layout=True)
    image = ax.imshow(matrix, cmap="Blues", vmin=0)
    ax.set_title("30 分钟后占用预测混淆矩阵")
    ax.set_xlabel("预测值")
    ax.set_ylabel("真实值")
    ax.set_xticks([0, 1], labels=["无人", "有人"])
    ax.set_yticks([0, 1], labels=["无人", "有人"])
    threshold = matrix.max() / 2
    for row in range(2):
        for column in range(2):
            ax.text(
                column,
                row,
                f"{matrix[row, column]:,}",
                ha="center",
                va="center",
                color="white" if matrix[row, column] > threshold else "#222222",
                fontsize=10,
            )
    colorbar = fig.colorbar(image, ax=ax, pad=0.03)
    colorbar.set_label("测试样本数")
    figures_dir.mkdir(parents=True, exist_ok=True)
    paths = {
        "png": figures_dir / "04_baseline_confusion_matrix.png",
        "pdf": figures_dir / "04_baseline_confusion_matrix.pdf",
        "svg": figures_dir / "04_baseline_confusion_matrix.svg",
    }
    fig.savefig(paths["png"], dpi=300, bbox_inches="tight")
    fig.savefig(paths["pdf"], bbox_inches="tight")
    fig.savefig(paths["svg"], bbox_inches="tight")
    plt.close(fig)
    return {key: value.name for key, value in paths.items()}


def run_baseline(input_path: Path, docs_dir: Path, web_dir: Path) -> dict[str, Any]:
    df = load_analysis_data(input_path)
    samples = prepare_samples(df)
    evaluated, baseline = train_and_evaluate(samples)

    figures_dir = docs_dir / "figures"
    results_dir = docs_dir / "results"
    web_figures_dir = web_dir / "figures"
    results_dir.mkdir(parents=True, exist_ok=True)
    web_figures_dir.mkdir(parents=True, exist_ok=True)
    figure_files = plot_confusion_matrix(baseline["metrics"], figures_dir)
    shutil.copy2(figures_dir / figure_files["png"], web_figures_dir / figure_files["png"])

    prediction_columns = [
        "timestamp",
        "target_timestamp",
        "room_id",
        "occupancy",
        "target_occupancy",
        "probability",
        "predicted_occupancy",
        "persistence_prediction",
        "data_mode",
    ]
    evaluated[prediction_columns].to_csv(
        results_dir / "baseline_predictions.csv",
        index=False,
        encoding="utf-8-sig",
    )
    (results_dir / "baseline_metrics.json").write_text(
        json.dumps(baseline, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    summary_path = results_dir / "analysis_summary.json"
    if not summary_path.exists():
        raise FileNotFoundError("请先运行 week4_analysis.py 生成分析摘要")
    summary = json.loads(summary_path.read_text(encoding="utf-8"))
    summary["baseline"] = baseline
    summary["figures"] = [
        figure for figure in summary["figures"] if figure["file"] != figure_files["png"]
    ]
    summary["figures"].append(
        {
            "title": "30 分钟后占用预测混淆矩阵",
            "file": figure_files["png"],
            "definition": "测试集按时间后置切分；行是真实占用，列是预测占用，样本限定为 08:00–22:00。",
            "conclusion": (
                f"测试集 n={baseline['test_period']['samples']}，准确率 "
                f"{baseline['metrics']['accuracy']:.1%}，F1 {baseline['metrics']['f1']:.1%}。"
            ),
        }
    )
    summary_path.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    web_dir.mkdir(parents=True, exist_ok=True)
    (web_dir / "week4_summary.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    write_analysis_report(summary, docs_dir / "analysis_report.md")
    return baseline


def main() -> None:
    args = parse_args()
    baseline = run_baseline(args.input, args.docs_dir, args.web_dir)
    metrics = baseline["metrics"]
    print(f"训练集样本数: {baseline['train_period']['samples']}")
    print(f"测试集样本数: {baseline['test_period']['samples']}")
    print(f"测试集准确率: {metrics['accuracy']:.3f}")
    print(f"测试集 F1: {metrics['f1']:.3f}")


if __name__ == "__main__":
    main()
