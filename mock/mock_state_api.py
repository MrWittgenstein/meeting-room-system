#!/usr/bin/env python3
"""Dependency-free mock server for the unified meeting-room state endpoint."""

from __future__ import annotations

import argparse
import copy
import json
import re
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any
from urllib.parse import unquote, urlparse


DEFAULT_STATE_PATH = Path(__file__).with_name("state.json")
STATE_ROUTE = re.compile(r"^/iot/rooms/([^/]+)/state/?$")


def load_state(path: Path) -> dict[str, Any]:
    state = json.loads(path.read_text(encoding="utf-8"))
    required = {
        "timestamp",
        "room_id",
        "room_db_id",
        "device_id",
        "occupancy",
        "temperature",
        "humidity",
        "door_status",
        "devices",
        "source",
        "quality",
        "quality_issues",
    }
    missing = sorted(required - state.keys())
    if missing:
        raise ValueError(f"state.json 缺少字段: {', '.join(missing)}")
    if state["source"] != "mock":
        raise ValueError('模拟数据必须明确标记为 "source": "mock"')
    return state


def create_handler(state: dict[str, Any]) -> type[BaseHTTPRequestHandler]:
    class StateHandler(BaseHTTPRequestHandler):
        server_version = "MeetingRoomMock/1.0"

        def do_GET(self) -> None:  # noqa: N802 - BaseHTTPRequestHandler API
            path = urlparse(self.path).path
            if path == "/health":
                self.send_json(HTTPStatus.OK, {"status": "ok", "source": "mock"})
                return

            match = STATE_ROUTE.fullmatch(path)
            if not match:
                self.send_error_payload(HTTPStatus.NOT_FOUND, "endpoint not found")
                return

            requested_room = unquote(match.group(1)).strip().lower()
            valid_ids = {str(state["room_db_id"]), str(state["room_id"]).lower()}
            if requested_room not in valid_ids:
                self.send_error_payload(
                    HTTPStatus.NOT_FOUND,
                    f"room not found: {requested_room}",
                )
                return

            self.send_json(
                HTTPStatus.OK,
                {"code": 1, "message": "success", "data": copy.deepcopy(state)},
            )

        def do_OPTIONS(self) -> None:  # noqa: N802 - BaseHTTPRequestHandler API
            self.send_response(HTTPStatus.NO_CONTENT)
            self.send_common_headers("application/json; charset=utf-8", 0)
            self.end_headers()

        def send_error_payload(self, status: HTTPStatus, message: str) -> None:
            self.send_json(status, {"code": 0, "message": message, "data": None})

        def send_json(self, status: HTTPStatus, payload: dict[str, Any]) -> None:
            body = json.dumps(payload, ensure_ascii=False, indent=2).encode("utf-8")
            self.send_response(status)
            self.send_common_headers("application/json; charset=utf-8", len(body))
            self.end_headers()
            self.wfile.write(body)

        def send_common_headers(self, content_type: str, length: int) -> None:
            self.send_header("Content-Type", content_type)
            self.send_header("Content-Length", str(length))
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header("Access-Control-Allow-Methods", "GET, OPTIONS")
            self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")

        def log_message(self, format_string: str, *args: Any) -> None:
            print(f"[{self.log_date_time_string()}] {format_string % args}")

    return StateHandler


def create_server(host: str, port: int, state_path: Path) -> ThreadingHTTPServer:
    state = load_state(state_path)
    return ThreadingHTTPServer((host, port), create_handler(state))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="启动统一会议室状态模拟接口")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8000)
    parser.add_argument("--state", type=Path, default=DEFAULT_STATE_PATH)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    server = create_server(args.host, args.port, args.state)
    host, port = server.server_address[:2]
    print(f"模拟接口已启动: http://{host}:{port}/iot/rooms/14/state")
    print('数据来源已标记为: "source": "mock"')
    print("按 Ctrl+C 停止服务")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n模拟接口已停止")
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
