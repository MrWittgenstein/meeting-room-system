#!/usr/bin/env python3
"""Simple WebSocket client for checking the Raspberry Pi sensor server."""

from __future__ import annotations

import argparse
import asyncio
import json

import websockets


async def main() -> None:
    parser = argparse.ArgumentParser(description="Smart room sensor WebSocket test client")
    parser.add_argument("url", nargs="?", default="ws://localhost:8775")
    parser.add_argument("--count", type=int, default=5, help="Number of messages to print.")
    args = parser.parse_args()

    async with websockets.connect(args.url) as websocket:
        for index in range(args.count):
            message = await websocket.recv()
            data = json.loads(message)
            print(f"[{index + 1}] current = {json.dumps(data['current'], ensure_ascii=False)}")


if __name__ == "__main__":
    asyncio.run(main())
