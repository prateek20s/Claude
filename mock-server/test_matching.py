#!/usr/bin/env python3
"""Smoke test: register a user, then simulate a beacon detecting their device hash."""
import json, hashlib, urllib.request

BASE="http://127.0.0.1:8000"; KEY="REPLACE_WITH_POC_KEY"
SALT="BFL_BEACON_POC_SALT_v1"   # must match app ServerClient.hashSalt

def sha(mac): return hashlib.sha256((SALT+mac.upper()).encode()).hexdigest()
def post(path, body):
    req=urllib.request.Request(BASE+path, data=json.dumps(body).encode(),
        headers={"Content-Type":"application/json","X-API-Key":KEY})
    return json.loads(urllib.request.urlopen(req).read())

watch_mac="A4:C3:11:22:9F:12"
r=post("/api/v1/register",{"consent":{"given":True,"version":"1.0","timestamp_utc":"t","purposes":["x"]},
       "user_mobile_masked":"98XXXXXX21",
       "peripherals":[{"mac_hash":sha(watch_mac),"type":"Watch","label":"My Watch"}]})
print("register ->", r)
# beacon sees two devices on air; one is our registered watch
d=post("/api/v1/detect",{"mac_hashes":[sha(watch_mac), sha("FF:FF:00:00:00:01")]})
print("detect   ->", d)
assert len(d["matches"])==1 and d["matches"][0]["user_ref"]==r["user_ref"]
print("\nPASS: registered device matched, unknown device ignored.")
