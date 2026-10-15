# BFL Beacon — Consent & Registration App (POC)

Android app that asks a user to **consent (DPDP)** and **register the Bluetooth MAC
addresses of their paired peripherals** (watch, earbuds) to the Bajaj / EasyReach
server, so the in-store beacon can later recognise them and trigger personalized offers.

This is the **permission + registration app** for Beacon POC Approach 2 (peripheral
devices, not phone MAC).

---

## Why peripherals, not the phone's MAC
An app **cannot** read its own phone's Bluetooth MAC (OS returns a dummy value since
Android 6), and a beacon sniffing the air sees **randomised** phone MACs that won't
match anything. Paired peripherals (Bluetooth Classic) expose **stable** MACs the
beacon can match. This app therefore registers the user's chosen **paired** devices.

## What the app does (maps to your 5 steps)
1. Beacon (separate hardware) captures MAC hashes on air — not part of this app.
2. **This app** takes DPDP consent + Bluetooth permission.
3. On consent, it reads the user's **paired** devices, hashes each MAC, and sends the
   hashes to the server.
4. Beacon posts detected MAC hashes to the server.
5. Server matches beacon hashes against registered hashes → identifies the customer.

## Privacy by design (for InfoSec / Legal)
- **No location permission.** We enumerate *bonded* devices only; we never scan the air.
- **No raw MAC leaves the phone.** We send `SHA-256(salt + MAC)`. The beacon hashes with
  the same salt; matching happens on hashes. Raw identifiers never sit in the DB in clear.
- **Explicit DPDP consent** with purpose, retention, rights, and DPO grievance contact
  (see `ui/ConsentText.kt`) — **review with Legal before production**.
- **Withdrawal = erasure.** The Manage screen calls `/withdraw`, server purges the user,
  and local data is wiped.
- Scoped `BLUETOOTH_CONNECT` only on Android 12+; legacy perms capped at `maxSdkVersion=30`.

---

## Build the APK (your team / vendor does this)
> This repo is a complete Android Studio project. The APK must be signed with **your**
> key before distribution, so building is a deliberate on-your-side step.

**Option A — Android Studio (easiest)**
1. Open the folder in Android Studio (Giraffe or newer).
2. Let Gradle sync.
3. `Build ▸ Build Bundle(s)/APK(s) ▸ Build APK(s)` → `app/build/outputs/apk/`.

**Option B — command line**
```bash
# Debug APK (points at the local mock server at 10.0.2.2:8000):
./gradlew assembleDebug
# Release APK (points at SERVER_BASE_URL; sign it before you distribute):
./gradlew assembleRelease
```

### Before you build for real
In `app/build.gradle.kts` → `defaultConfig`, set:
- `SERVER_BASE_URL` → your EasyReach/BFL registration endpoint (HTTPS).
- `POC_API_KEY` → the key issued for the POC.
In `net/ServerClient.kt`, the `hashSalt` should be fetched from the server per tenant
(hardcoded only for the POC).

### Signing (release) — standard Android
```bash
keytool -genkey -v -keystore bfl-beacon.jks -keyalg RSA -keysize 2048 \
        -validity 3650 -alias bfl
# add signingConfigs to app/build.gradle.kts, or sign the unsigned APK with apksigner
```

## Distribute (sideload)
1. Host the signed APK on an internal link.
2. Testers enable **Install unknown apps** for their browser/file manager.
3. They download + install, open the app, consent, and register.
> For a wider pilot, Firebase App Distribution or the Play Internal Testing track avoids
> the "unknown sources" friction and gives you install analytics.

## Test the full loop today (no beacon needed)
```bash
cd mock-server
python3 server.py           # terminal 1 — starts server on :8000
python3 test_matching.py    # terminal 2 — registers + simulates a beacon match
```
Run the **debug** APK on an emulator; it talks to this mock server automatically.

## Project layout
```
app/src/main/java/com/bajajfinance/beaconconsent/
  MainActivity.kt            screen navigation
  bt/PeripheralReader.kt     reads paired (bonded) peripherals; no scanning
  data/Models.kt             data classes
  data/ConsentStore.kt       local consent + registration persistence
  net/ServerClient.kt        register / revokeDevice / withdraw; hashes MACs before sending
  ui/ConsentText.kt          DPDP notice copy  <-- Legal reviews this
  ui/Screens.kt              7 Compose screens (incl. Home landing)
  ui/AppViewModel.kt         flow orchestration
mock-server/                 standalone test server + smoke test
  mock-server/schema.sql       canonical storage schema (consent_users, consent_devices, consent_audit)
  docs/DATA_FORMAT.md          server storage format spec (how consented MAC data is stored)
docs/INFOSEC_NOTES.md        reviewer checklist
```
