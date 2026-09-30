"""零依赖压测脚本：验证 HPA 触发与 P95 指标（配合 Grafana 观察）。

用法:
    python scripts/load_test.py --url http://127.0.0.1:8080/api/tasks \
        --duration 30 --concurrency 10
"""
import argparse
import json
import statistics
import threading
import time
import urllib.request

parser = argparse.ArgumentParser(description="TaskFlow 简易压测")
parser.add_argument("--url", required=True)
parser.add_argument("--duration", type=int, default=30, help="持续时间（秒）")
parser.add_argument("--concurrency", type=int, default=10, help="并发线程数")
args = parser.parse_args()

latencies: list[float] = []
errors = 0
lock = threading.Lock()
deadline = time.time() + args.duration


def worker():
    global errors
    while time.time() < deadline:
        start = time.perf_counter()
        try:
            req = urllib.request.Request(args.url, headers={"User-Agent": "taskflow-loadtest/1.0"})
            with urllib.request.urlopen(req, timeout=5) as resp:
                resp.read()
                ok = resp.status == 200
        except Exception:  # noqa: BLE001
            ok = False
        elapsed_ms = (time.perf_counter() - start) * 1000
        with lock:
            latencies.append(elapsed_ms)
            if not ok:
                errors += 1


threads = [threading.Thread(target=worker) for _ in range(args.concurrency)]
print(f"压测 {args.url} | 并发 {args.concurrency} | 时长 {args.duration}s")
start = time.perf_counter()
for t in threads:
    t.start()
for t in threads:
    t.join()
elapsed = time.perf_counter() - start

latencies.sort()


def percentile(p: float) -> float:
    idx = min(int(len(latencies) * p / 100), len(latencies) - 1)
    return latencies[idx]


result = {
    "requests": len(latencies),
    "errors": errors,
    "rps": round(len(latencies) / elapsed, 1),
    "avg_ms": round(statistics.mean(latencies), 1),
    "p95_ms": round(percentile(95), 1),
    "p99_ms": round(percentile(99), 1),
}
print(json.dumps(result, indent=2, ensure_ascii=False))
