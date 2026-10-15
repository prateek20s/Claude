#!/usr/bin/env python3
"""
Mock Bajaj/EasyReach registration + matching server for the Beacon POC.
Standard library only. Persists consented MAC data in the CANONICAL FORMAT
defined in schema.sql (SQLite here; same shape maps to MySQL/Postgres in prod).

PRIVACY: raw MAC addresses are never stored — only salted SHA-256 hashes.

Endpoints:
  POST /api/v1/register        app registers a consented user + devices
  POST /api/v1/detect          beacon reports MAC hashes seen on air -> matches
  POST /api/v1/revoke_device   remove consent for one device (erasure)
  POST /api/v1/withdraw        remove consent for the whole user (erasure)
  GET  /health                 liveness + counts
  GET  /debug/export           canonical JSON view of stored records (testing only)

Run:  python3 server.py        (listens on :8000, writes beaconstore.db beside this file)
"""
import json, uuid, os, sqlite3, datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

HERE = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.path.join(HERE, "beaconstore.db")
SCHEMA_PATH = os.path.join(HERE, "schema.sql")
API_KEY = "REPLACE_WITH_POC_KEY"          # must match app BuildConfig.POC_API_KEY

def now(): return datetime.datetime.now(datetime.timezone.utc).isoformat()

def db():
    con = sqlite3.connect(DB_PATH)
    con.row_factory = sqlite3.Row
    con.execute("PRAGMA foreign_keys=ON")
    return con

def init_db():
    with open(SCHEMA_PATH) as f:
        sql = f.read()
    con = db(); con.executescript(sql); con.commit(); con.close()

def audit(con, user_ref, event, detail=""):
    con.execute("INSERT INTO consent_audit(user_ref,event,detail,ts_utc) VALUES(?,?,?,?)",
                (user_ref, event, detail, now()))

class H(BaseHTTPRequestHandler):
    def _send(self, code, obj):
        body = json.dumps(obj).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _read(self):
        n = int(self.headers.get("Content-Length", 0))
        return json.loads(self.rfile.read(n) or b"{}")

    def _auth_ok(self):
        return self.headers.get("X-API-Key") == API_KEY

    # ---------------- GET ----------------
    def do_GET(self):
        if self.path == "/health":
            con = db()
            u = con.execute("SELECT COUNT(*) c FROM consent_users").fetchone()["c"]
            d = con.execute("SELECT COUNT(*) c FROM consent_devices WHERE status='active'").fetchone()["c"]
            con.close()
            return self._send(200, {"ok": True, "users": u, "active_devices": d})
        if self.path == "/debug/export":          # canonical JSON view (testing only)
            return self._send(200, {"users": self._export()})
        self._send(404, {"error": "not found"})

    def _export(self):
        con = db(); out = []
        for u in con.execute("SELECT * FROM consent_users ORDER BY registered_at").fetchall():
            devs = con.execute(
                "SELECT device_id,mac_hash,hash_algo,salt_version,device_type,label,status,"
                "registered_at,revoked_at FROM consent_devices WHERE user_ref=?",
                (u["user_ref"],)).fetchall()
            out.append({
                "user_ref": u["user_ref"],
                "user_mobile_masked": u["user_mobile_masked"],
                "consent": {
                    "given": bool(u["consent_given"]),
                    "version": u["consent_version"],
                    "timestamp_utc": u["consent_ts_utc"],
                    "purposes": json.loads(u["purposes"]),
                },
                "status": u["status"],
                "registered_at": u["registered_at"],
                "updated_at": u["updated_at"],
                "devices": [dict(d) for d in devs],
            })
        con.close(); return out

    # ---------------- POST ----------------
    def do_POST(self):
        if not self._auth_ok():
            return self._send(401, {"error": "bad api key"})
        try:
            data = self._read()
        except Exception:
            return self._send(400, {"error": "bad json"})
        con = db()
        try:
            if self.path == "/api/v1/register":   return self._register(con, data)
            if self.path == "/api/v1/detect":     return self._detect(con, data)
            if self.path == "/api/v1/revoke_device": return self._revoke(con, data)
            if self.path == "/api/v1/withdraw":   return self._withdraw(con, data)
            return self._send(404, {"error": "not found"})
        finally:
            con.commit(); con.close()

    def _register(self, con, data):
        c = data.get("consent", {})
        if not c.get("given"):
            return self._send(400, {"error": "consent required"})
        user_ref = "u_" + uuid.uuid4().hex[:12]
        ts = now()
        con.execute(
            "INSERT INTO consent_users(user_ref,user_mobile_masked,consent_given,consent_version,"
            "consent_ts_utc,purposes,status,registered_at,updated_at) VALUES(?,?,?,?,?,?, 'active', ?, ?)",
            (user_ref, data.get("user_mobile_masked", ""), 1, c.get("version", ""),
             c.get("timestamp_utc", ts), json.dumps(c.get("purposes", [])), ts, ts))
        for p in data.get("peripherals", []):
            con.execute(
                "INSERT OR IGNORE INTO consent_devices(device_id,user_ref,mac_hash,hash_algo,salt_version,"
                "device_type,label,status,registered_at,revoked_at) VALUES(?,?,?,?,?,?,?, 'active', ?, NULL)",
                ("d_" + uuid.uuid4().hex[:10], user_ref, p["mac_hash"],
                 p.get("hash_algo", "SHA-256"), p.get("salt_version", "v1"),
                 p.get("device_type", p.get("type")), p.get("label"), ts))
        audit(con, user_ref, "REGISTERED", f'{len(data.get("peripherals", []))} device(s)')
        print(f"[REGISTER] {user_ref}  {len(data.get('peripherals', []))} device(s)")
        return self._send(200, {"user_ref": user_ref, "status": "registered"})

    def _detect(self, con, data):
        seen = data.get("mac_hashes", [])
        matches = []
        for h in seen:
            row = con.execute(
                "SELECT d.user_ref, u.user_mobile_masked FROM consent_devices d "
                "JOIN consent_users u ON u.user_ref=d.user_ref "
                "WHERE d.mac_hash=? AND d.status='active' AND u.status='active'", (h,)).fetchone()
            if row:
                matches.append({"mac_hash": h, "user_ref": row["user_ref"],
                                "mobile_masked": row["user_mobile_masked"]})
        print(f"[DETECT] saw {len(seen)} hash(es) -> {len(matches)} match(es)")
        return self._send(200, {"matches": matches})

    def _revoke(self, con, data):
        ref, mh = data.get("user_ref"), data.get("mac_hash")
        cur = con.execute("DELETE FROM consent_devices WHERE user_ref=? AND mac_hash=?", (ref, mh))
        if cur.rowcount == 0:
            return self._send(404, {"error": "device not found"})
        audit(con, ref, "DEVICE_REVOKED", "")
        remaining = con.execute("SELECT COUNT(*) c FROM consent_devices WHERE user_ref=?", (ref,)).fetchone()["c"]
        if remaining == 0:
            con.execute("DELETE FROM consent_users WHERE user_ref=?", (ref,))
            audit(con, ref, "WITHDRAWN", "last device removed")
            print(f"[REVOKE] {ref} last device removed -> erased")
            return self._send(200, {"status": "erased", "remaining": 0})
        print(f"[REVOKE] {ref} removed 1 device -> {remaining} left")
        return self._send(200, {"status": "removed", "remaining": remaining})

    def _withdraw(self, con, data):
        ref = data.get("user_ref")
        con.execute("DELETE FROM consent_devices WHERE user_ref=?", (ref,))
        cur = con.execute("DELETE FROM consent_users WHERE user_ref=?", (ref,))
        audit(con, ref, "WITHDRAWN", "full withdrawal")
        print(f"[WITHDRAW] {ref} erased")
        return self._send(200, {"status": "erased"})

    def log_message(self, *a): pass

if __name__ == "__main__":
    init_db()
    print(f"Mock Beacon server on http://0.0.0.0:8000  (DB: {DB_PATH})")
    print("  health: /health   canonical export: /debug/export")
    ThreadingHTTPServer(("0.0.0.0", 8000), H).serve_forever()
