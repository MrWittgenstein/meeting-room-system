#!/usr/bin/env python3
"""Run the complete reproducible Week 4 data-to-web pipeline."""

from clean_data import DEFAULT_OUTPUT, DEFAULT_REPORT, clean_data
from generate_weekly_data import DEFAULT_OUTPUT as DEFAULT_RAW, generate_weekly_data
from baseline_model import run_baseline
from week4_analysis import DEFAULT_DOCS_DIR, DEFAULT_WEB_DIR, run_analysis


def main() -> None:
    generated = generate_weekly_data(DEFAULT_RAW)
    cleaned = clean_data(DEFAULT_RAW, DEFAULT_OUTPUT, DEFAULT_REPORT)
    summary = run_analysis(DEFAULT_OUTPUT, DEFAULT_DOCS_DIR, DEFAULT_WEB_DIR)
    baseline = run_baseline(DEFAULT_OUTPUT, DEFAULT_DOCS_DIR, DEFAULT_WEB_DIR)

    print("第 4 周流水线完成")
    print(f"原始记录: {generated['output_count']}")
    print(f"清洗后记录: {cleaned['after_count']}")
    print(f"分析图表: {len(summary['figures']) + 1}")
    print(f"基线测试样本: {baseline['test_period']['samples']}")
    print(f"基线准确率: {baseline['metrics']['accuracy']:.3f}")


if __name__ == "__main__":
    main()
