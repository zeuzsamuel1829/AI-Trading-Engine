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
    "ask": None,
    "balance": None,
    "equity": None,
    "margin": None,
    "free_margin": None,
    "profit": None,
    "symbols": [],
    "candles": [],
    "candles_by_tf": {},
    "timeframe": None,
    "pending_order": None,
    "last_order_result": None
}

class Handler(BaseHTTPRequestHandler):

    def send_json(self, code, data):
        body = json.dumps(data, separators=(',', ':')).encode()
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

        if self.path == "/mt5/order/pending":
            return self.send_json(200, {"ok": True, "order": state.get("pending_order")})

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
            text = raw.decode("utf-8", errors="strict").rstrip("\x00").strip()
            data = json.loads(text)
        except Exception as e:
            print("JSON ERROR:", repr(e))
            print("RAW:", repr(raw[:500]))
            return self.send_json(400, {
                "ok": False,
                "error": "invalid_json"
            })

        if self.path == "/mt5/market/symbols":
            state["symbols"] = data.get("symbols", [])
            return self.send_json(200, {
                "ok": True,
                "bridge": "online",
                "mt5": "connected",
                "symbols": state["symbols"]
            })

        if self.path == "/mt5/market/candles":
            state["symbol"] = data.get("symbol")
            tf = str(data.get("timeframe") or "UNKNOWN")
            state["timeframe"] = tf
            candles = data.get("candles", [])
            state["candles"] = candles
            state["candles_by_tf"][tf] = candles
            return self.send_json(200, {
                "ok": True,
                "bridge": "online",
                "mt5": "connected",
                "symbol": state["symbol"],
                "timeframe": tf,
                "candles": len(candles),
                "tfs": list(state["candles_by_tf"].keys())
            })

        if self.path == "/mt5/order/pending":
            state["pending_order"] = data if data else None
            return self.send_json(200, {"ok": True, "accepted": False, "reason": "orders_disabled", "order": state.get("pending_order")})
        if self.path == "/mt5/order/result":
            state["last_order_result"] = data
            return self.send_json(200, {"ok": True, "accepted": False, "reason": "orders_disabled"})
        if self.path == "/mt5/heartbeat":
            state["mt5"] = "connected"
            state["last_heartbeat"] = int(time.time())
            state["symbol"] = data.get("symbol")
            state["bid"] = data.get("bid")
            state["ask"] = data.get("ask")
            state["balance"] = data.get("balance")
            state["equity"] = data.get("equity")
            state["margin"] = data.get("margin")
            state["free_margin"] = data.get("free_margin")
            state["profit"] = data.get("profit")

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
