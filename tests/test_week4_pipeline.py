#!/usr/bin/env python3
"""Tests for the Week 4 analysis and occupancy baseline."""

from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path

import pandas as pd


REPO_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO_ROOT / "scripts"))

from baseline_model import prepare_samples, train_and_evaluate  # noqa: E402
from clean_data import clean_data  # noqa: E402
from generate_weekly_data import generate_weekly_data  # noqa: E402
from week4_analysis import compute_analysis_tables, load_analysis_data  # noqa: E402


class Week4PipelineTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_directory = tempfile.TemporaryDirectory()
        temp_path = Path(self.temp_directory.name)
        self.raw_path = temp_path / "raw.csv"
        self.clean_path = temp_path / "clean.csv"
        self.report_path = temp_path / "quality.md"
        generate_weekly_data(self.raw_path)
        clean_data(self.raw_path, self.clean_path, self.report_path)
        self.data = load_analysis_data(self.clean_path)

    def tearDown(self) -> None:
        self.temp_directory.cleanup()

    def test_analysis_answers_three_questions_with_explicit_samples(self) -> None:
        tables = compute_analysis_tables(self.data)

        self.assertEqual(5, tables["interval_minutes"])
        self.assertEqual((14, 24), tables["heatmap"].shape)
        self.assertGreater(int(tables["heatmap"].to_numpy().sum()), 0)
        self.assertEqual(3, len(tables["device_metrics"]))
        self.assertEqual(42, len(tables["hourly_occupancy"]))
        self.assertTrue((tables["device_metrics"]["valid_samples"] > 0).all())
        self.assertTrue((tables["hourly_occupancy"]["sample_size"] > 0).all())

    def test_baseline_uses_chronological_ten_day_four_day_split(self) -> None:
        samples = prepare_samples(self.data)
        evaluated, result = train_and_evaluate(samples)

        self.assertEqual("2026-09-15", result["train_period"]["start"])
        self.assertEqual("2026-09-24", result["train_period"]["end"])
        self.assertEqual("2026-09-25", result["test_period"]["start"])
        self.assertEqual("2026-09-28", result["test_period"]["end"])
        self.assertEqual(result["test_period"]["samples"], len(evaluated))
        self.assertLess(
            pd.Timestamp(result["train_period"]["end"]),
            pd.Timestamp(result["test_period"]["start"]),
        )
        for metric in ["accuracy", "precision", "recall", "specificity", "f1", "balanced_accuracy"]:
            self.assertGreaterEqual(result["metrics"][metric], 0)
            self.assertLessEqual(result["metrics"][metric], 1)


if __name__ == "__main__":
    unittest.main(verbosity=2)
