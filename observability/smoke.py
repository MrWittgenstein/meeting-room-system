"""Read-only HTTP/log collection smoke test; does not log in or control hardware."""
import json
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
import sys
import subprocess
import threading
import os
from pathlib import Path


def query_clickhouse(sql):
    request = urllib.request.Request("http://127.0.0.1:8123/?" + urllib.parse.urlencode({"query": sql}))
    with urllib.request.urlopen(request, timeout=10) as response:
        return json.load(response)


def main():
    started = time.time()
    recovery = len(sys.argv) > 1
    if recovery:
        vector_exe = str(Path(sys.argv[1]).resolve())
        def resume_collection():
            time.sleep(60)
            with open("run-logs/vector-recovery.stdout.log", "w") as stdout, open("run-logs/vector-recovery.stderr.log", "w") as stderr:
                process = subprocess.Popen([vector_exe, "--config", "observability/vector.toml"],
                                           stdout=stdout, stderr=stderr,
                                           env={**os.environ, "VECTOR_THREADS": "2"},
                                           creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
                print(f"Vector resumed after 60 seconds, PID={process.pid}", flush=True)
        threading.Thread(target=resume_collection, daemon=False).start()
    requests = []
    for index in range(50):
        path = "/v3/api-docs" if index % 2 == 0 else "/user/info"
        request = urllib.request.Request("http://127.0.0.1:8080" + path,
                                        headers={"X-Authenticated-User-Id": "forged-user"})
        try:
            response = urllib.request.urlopen(request, timeout=10)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            trace = response.headers.get("X-Trace-Id", "").split(",")[0].strip()
            uuid.UUID(trace)
            requests.append({"path": path, "status": response.status, "trace_id": trace})
            assert response.status == (200 if index % 2 == 0 else 401), requests[-1]
            body = response.read()
            if index % 2 == 0:
                assert "openapi" in json.loads(body), "Expected API documentation, not a business error"
        time.sleep(0.25)
    print("50 HTTP requests complete; waiting for collection", flush=True)
    traces = ",".join("'" + item["trace_id"] + "'" for item in requests)
    sql = ("SELECT microservice, count() AS events, uniqExact(trace_id) AS traces, "
           "countIf(user_id = 'forged-user') AS forged_identities "
           "FROM meetingroom_logs.access_events FINAL WHERE trace_id IN (" + traces + ") "
           "GROUP BY microservice FORMAT JSON")
    deadline = time.time() + (300 if recovery else 60)
    while True:
        result = query_clickhouse(sql)["data"]
        counts = {row["microservice"]: int(row["events"]) for row in result}
        if counts.get("topbiz-gateway") == 50 and counts.get("meetingroom-main") == 25:
            break
        if time.time() >= deadline:
            raise AssertionError(counts)
        time.sleep(1)
    assert all(int(row["forged_identities"]) == 0 for row in result)
    report = {"test": "Local HTTP and ClickHouse smoke, no hardware", "started_epoch": started,
              "elapsed_seconds": round(time.time() - started, 2), "requests": requests, "clickhouse": result}
    report["collector_interruption_seconds"] = 60 if recovery else 0
    output = Path("run-logs/acceptance-recovery.json" if recovery else "run-logs/acceptance-smoke.json")
    output.write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(json.dumps({"counts": counts, "elapsed_seconds": report["elapsed_seconds"], "evidence": str(output)}))


if __name__ == "__main__":
    main()
