"""本地开发联调代理：无需 Docker 时前后端联调用。

行为与 app/web/nginx.conf 对齐：
  - /            -> 静态文件（app/web/site）
  - /api/<path>  -> 转发到 http://127.0.0.1:8000/<path>（剥离 /api 前缀）
  - /healthz     -> 返回 ok（容器健康检查等价物）

用法:
  python scripts/dev-proxy.py            # 前端 http://127.0.0.1:8080
  API_UPSTREAM=http://127.0.0.1:9000 DEV_PORT=8081 python scripts/dev-proxy.py
"""
import http.server
import mimetypes
import os
import sys
import urllib.error
import urllib.request

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
STATIC_DIR = os.path.abspath(os.path.join(BASE_DIR, "..", "app", "web", "site"))
API_UPSTREAM = os.environ.get("API_UPSTREAM", "http://127.0.0.1:8000").rstrip("/")
PORT = int(os.environ.get("DEV_PORT", "8080"))

mimetypes.add_type("application/javascript", ".js")
mimetypes.add_type("text/css", ".css")


class DevProxyHandler(http.server.SimpleHTTPRequestHandler):

    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=STATIC_DIR, **kwargs)

    # ---- 路由 ----
    def do_GET(self):
        if self.path.startswith("/api/"):
            self._proxy("GET")
        elif self.path == "/healthz":
            self._respond(200, "ok\n", "text/plain")
        else:
            super().do_GET()

    def do_POST(self):
        self._proxy("POST")

    def do_PATCH(self):
        self._proxy("PATCH")

    def do_PUT(self):
        self._proxy("PUT")

    def do_DELETE(self):
        self._proxy("DELETE")

    # ---- 转发逻辑：与 nginx `location /api/ { proxy_pass http://api:8000/; }` 等价 ----
    def _proxy(self, method: str):
        upstream_path = self.path[len("/api"):] or "/"
        url = f"{API_UPSTREAM}{upstream_path}"
        length = int(self.headers.get("Content-Length") or 0)
        body = self.rfile.read(length) if length else None
        req = urllib.request.Request(url, data=body, method=method)
        if content_type := self.headers.get("Content-Type"):
            req.add_header("Content-Type", content_type)
        try:
            with urllib.request.urlopen(req, timeout=10) as resp:
                self._respond(resp.status, resp.read(), resp.headers.get("Content-Type", "application/json"))
        except urllib.error.HTTPError as exc:
            self._respond(exc.code, exc.read(), exc.headers.get("Content-Type", "application/json"))
        except Exception as exc:  # noqa: BLE001 - 代理对任何上游故障返回 502
            self._respond(502, f'{{"error":"upstream unavailable: {exc}"}}', "application/json")

    def _respond(self, status: int, payload, content_type: str):
        data = payload if isinstance(payload, bytes) else str(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        if data:
            self.wfile.write(data)

    def log_message(self, fmt, *args):  # noqa: A002 - 标准库签名
        sys.stderr.write("[dev-proxy] %s - %s\n" % (self.address_string(), fmt % args))


if __name__ == "__main__":
    if not os.path.isdir(STATIC_DIR):
        sys.exit(f"静态目录不存在: {STATIC_DIR}")
    server = http.server.ThreadingHTTPServer(("0.0.0.0", PORT), DevProxyHandler)
    print(f"[dev-proxy] 前端: http://127.0.0.1:{PORT}  ->  API: {API_UPSTREAM}  (Ctrl+C 停止)")
    server.serve_forever()
