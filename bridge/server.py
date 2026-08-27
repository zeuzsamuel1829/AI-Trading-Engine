from http.server import BaseHTTPRequestHandler, HTTPServer
import json
import os
import time

HOST = "0.0.0.0"
PORT = int(os.environ.get("PORT", "8080"))
TOKEN = os.environ.get("BRIDGE_TOKEN", "DEMO-ZEUS-2026")

state = {
    "bridge": "online",
    "mt5": "pending",
    "mode": "DEMO",
    "last_heartbeat": None,
    "symbol": None,
    "bid": None,
    "ask": None
}

class Handler(BaseHTTPRequestHandler):

    def send_json(self, code, data):
        body = json.dumps(data).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(body)

    def authorized(self):
        return self.headers.get("X-Bridge-Token") == TOKEN

    def do_GET(self):
        if self.path == "/health":
            return self.send_json(200, {
                "ok": True,
                "bridge": state["bridge"],
                "mt5": state["mt5"],
                "mode": state["mode"]
            })

        if self.path == "/mt5/status":
            return self.send_json(200, state)

        return self.send_json(404, {"ok": False, "error": "not_found"})

    def do_POST(self):
        if not self.authorized():
            return self.send_json(401, {
                "ok": False,
                "error": "unauthorized"
            })

        length = int(self.headers.get("Content-Length", "0"))
        raw = self.rfile.read(length) if length else b"{}"

        try:
            data = json.loads(raw.decode())
        except Exception:
            return self.send_json(400, {
                "ok": False,
                "error": "invalid_json"
            })

        if self.path == "/mt5/heartbeat":
            state["mt5"] = "connected"
            state["last_heartbeat"] = int(time.time())
            state["symbol"] = data.get("symbol")
            state["bid"] = data.get("bid")
            state["ask"] = data.get("ask")

            return self.send_json(200, {
                "ok": True,
                "bridge": "online",
                "mt5": "connected",
                "mode": "DEMO"
            })

        return self.send_json(404, {"ok": False, "error": "not_found"})

    def log_message(self, fmt, *args):
        print("[%s] %s" % (self.log_date_time_string(), fmt % args))

print("====================================")
print(" ZEUS MT5 DEMO BRIDGE")
print("====================================")
print(f"Listening on 0.0.0.0:{PORT}")
print("Mode: DEMO")
print("Orders: DISABLED")
print("Health: /health")
print("MT5 status: /mt5/status")
print("====================================")

HTTPServer((HOST, PORT), Handler).serve_forever()
