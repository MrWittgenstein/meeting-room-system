#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

sudo apt-get update
sudo apt-get install -y python3-pip python3-venv python3-dev build-essential i2c-tools libgpiod2 libgl1 libglib2.0-0 python3-picamera2
sudo raspi-config nonint do_i2c 0 || true

python3 -m venv .venv --system-site-packages
. .venv/bin/activate
python -m pip install --upgrade pip setuptools wheel
python -m pip install -r requirements.txt

if [ ! -f config.json ]; then
  cp config.example.json config.json
fi

echo "Install finished."
echo "Edit config.json, then run: .venv/bin/python sensor_server.py --config config.json"
