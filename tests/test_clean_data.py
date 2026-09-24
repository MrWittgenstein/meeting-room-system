#!/usr/bin/env python3
"""Tests for the weekly data generator and telemetry cleaning pipeline."""

from __future__ import annotations

import csv
import sys
import tempfile
import unittest
from pathlib import Path

import pandas as pd


REPO_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO_ROOT / "scripts"))

from clean_data import clean_data  # noqa: E402
from generate_weekly_data import generate_weekly_data  # noqa: E402


class CleanDataTest(unittest.TestCase):
    def test_missing_temperature_is_not_reported_as_out_of_range(self) -> None:
        with tempfile.TemporaryDirectory() as temp_directory:
            temp_path = Path(temp_directory)
            raw_path = temp_path / "raw.csv"
            output_path = temp_path / "clean.csv"
            report_path = temp_path / "report.md"
            fieldnames = [
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
            rows = [
                ["2026-09-15 09:00:00", "meeting_room_14", 14, "raspi-01", "telemetry", "", 50, 120, 10, 1, 3, "closed", "ok"],
                ["2026-09-15 09:05:00", "meeting_room_14", 14, "raspi-01", "telemetry", 85, 50, 120, 10, 0, 0, "closed", "ok"],
            ]
            with raw_path.open("w", encoding="utf-8-sig", newline="") as raw_file:
                writer = csv.writer(raw_file)
                writer.writerow(fieldnames)
                writer.writerows(rows)

            clean_data(raw_path, output_path, report_path)
            cleaned = pd.read_csv(output_path, dtype=str, keep_default_na=False)

            missing_issues = cleaned.loc[cleaned["timestamp"] == "2026-09-15 09:00:00", "quality_issues"].iloc[0]
            outlier_issues = cleaned.loc[cleaned["timestamp"] == "2026-09-15 09:05:00", "quality_issues"].iloc[0]
            self.assertIn("missing_temperature", missing_issues)
            self.assertNotIn("temperature_out_of_range", missing_issues)
            self.assertIn("temperature_out_of_range", outlier_issues)
            self.assertNotIn("missing_temperature", outlier_issues)
            self.assertEqual(["true", "false"], cleaned["occupancy"].tolist())

    def test_weekly_dataset_has_expected_coverage_and_boolean_occupancy(self) -> None:
        with tempfile.TemporaryDirectory() as temp_directory:
            temp_path = Path(temp_directory)
            raw_path = temp_path / "raw.csv"
            output_path = temp_path / "clean.csv"
            report_path = temp_path / "report.md"

            generated = generate_weekly_data(raw_path)
            cleaned_result = clean_data(raw_path, output_path, report_path)
            cleaned = pd.read_csv(output_path, dtype=str, keep_default_na=False)

            self.assertEqual(6048, generated["base_count"])
            self.assertEqual(6053, generated["output_count"])
            self.assertEqual(2, cleaned_result["duplicate_count"])
            self.assertEqual(3, cleaned_result["dropped_mandatory_count"])
            self.assertEqual(6048, cleaned_result["after_count"])
            self.assertEqual("2026-09-15 00:00:00", cleaned["timestamp"].min())
            self.assertEqual("2026-09-21 23:55:00", cleaned["timestamp"].max())
            self.assertEqual(
                {"meeting_room_14", "meeting_room_15", "meeting_room_32"},
                set(cleaned["room_id"]),
            )
            self.assertEqual(
                {"meeting_room_14": 2016, "meeting_room_15": 2016, "meeting_room_32": 2016},
                cleaned["room_id"].value_counts().sort_index().to_dict(),
            )
            self.assertLessEqual(set(cleaned["occupancy"]), {"", "true", "false"})
            conflicting = cleaned["quality_issues"].str.contains("temperature_out_of_range") & cleaned[
                "quality_issues"
            ].str.contains("missing_temperature")
            self.assertFalse(conflicting.any())


if __name__ == "__main__":
    unittest.main(verbosity=2)
