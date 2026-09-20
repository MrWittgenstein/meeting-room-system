#!/usr/bin/env python3
"""Integration tests for the dependency-free unified state mock API."""

from __future__ import annotations

import json
import sys
import threading
import unittest
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import urlopen


REPO_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO_ROOT / "mock"))

from mock_state_api import create_server  # noqa: E402


class MockStateApiTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.server = create_server("127.0.0.1", 0, REPO_ROOT / "mock" / "state.json")
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        cls.base_url = f"http://127.0.0.1:{cls.server.server_port}"

    @classmethod
    def tearDownClass(cls) -> None:
        cls.server.shutdown()
        cls.server.server_close()
        cls.thread.join(timeout=2)

    def get_json(self, path: str) -> tuple[int, dict]:
        try:
            with urlopen(self.base_url + path, timeout=3) as response:
                return response.status, json.load(response)
        except HTTPError as error:
            return error.code, json.load(error)

    def test_valid_room_returns_unified_state(self) -> None:
        status, payload = self.get_json("/iot/rooms/14/state")

        self.assertEqual(200, status)
        self.assertEqual(1, payload["code"])
        self.assertEqual("success", payload["message"])
        data = payload["data"]
        self.assertEqual("meeting_room_14", data["room_id"])
        self.assertEqual(14, data["room_db_id"])
        self.assertIsInstance(data["occupancy"], bool)
        self.assertEqual("mock", data["source"])
        self.assertEqual("normal", data["quality"])
        self.assertIn("devices", data)
        self.assertIn("door_status", data)

    def test_room_code_alias_is_supported(self) -> None:
        status, payload = self.get_json("/iot/rooms/meeting_room_14/state")
        self.assertEqual(200, status)
        self.assertEqual("meeting_room_14", payload["data"]["room_id"])

    def test_invalid_room_returns_clear_error(self) -> None:
        status, payload = self.get_json("/iot/rooms/99/state")

        self.assertEqual(404, status)
        self.assertEqual(0, payload["code"])
        self.assertIn("room not found", payload["message"])
        self.assertIsNone(payload["data"])

    def test_unknown_endpoint_returns_clear_error(self) -> None:
        status, payload = self.get_json("/not-found")
        self.assertEqual(404, status)
        self.assertEqual("endpoint not found", payload["message"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
