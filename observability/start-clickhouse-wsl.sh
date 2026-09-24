#!/usr/bin/env bash
set -euo pipefail
CLICKHOUSE_BIN="${CLICKHOUSE_BIN:-$HOME/clickhouse/clickhouse-common-static-25.5.2.47/usr/bin/clickhouse}"
TASK_DATA="${MEETINGROOM_CLICKHOUSE_DATA:-$HOME/meetingroom-clickhouse}"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
if [[ ! -x "$CLICKHOUSE_BIN" ]]; then
  echo 'Set CLICKHOUSE_BIN to an installed ClickHouse executable.' >&2
  exit 1
fi
mkdir -p "$TASK_DATA/data" "$TASK_DATA/tmp" "$TASK_DATA/logs"
if ! curl -fsS http://127.0.0.1:8123/ping >/dev/null; then
  "$CLICKHOUSE_BIN" server --daemon -- --path="$TASK_DATA/data" --tmp_path="$TASK_DATA/tmp" \
    --logger.log="$TASK_DATA/logs/server.log" --logger.errorlog="$TASK_DATA/logs/error.log"
fi
for attempt in {1..20}; do
  if curl -fsS http://127.0.0.1:8123/ping >/dev/null; then
    "$CLICKHOUSE_BIN" client --multiquery < "$SCRIPT_DIR/access_events.sql"
    echo 'meetingroom_logs.access_events ready'
    exit 0
  fi
  sleep 1
done
echo "ClickHouse did not start; inspect $TASK_DATA/logs/error.log" >&2
exit 1
