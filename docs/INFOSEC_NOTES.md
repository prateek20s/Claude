# InfoSec / DPDP review checklist — Beacon Consent App

| Area | Implementation | Reviewer action |
|---|---|---|
| Location data | Not requested. No `ACCESS_FINE/COARSE_LOCATION`. Bonded-device read only. | Confirm acceptable; note beacon side is separate. |
| Bluetooth scope | `BLUETOOTH_CONNECT` (API 31+); legacy capped at `maxSdkVersion=30`. | Confirm scoping. |
| Raw identifiers | MAC never sent raw. `SHA-256(salt+MAC)` only. Salt server-issued in prod. | Approve salt management + rotation. |
| Storage format | Defined schema (`consent_users`, `consent_devices`, `consent_audit`) — see docs/DATA_FORMAT.md. Only salted hashes stored; audit trail holds no MAC/PII. | Review schema; confirm encryption-at-rest + access controls with vendor. |
| Consent | Explicit, versioned, timestamped; purpose-bound; stored locally + sent. | **Legal to approve `ui/ConsentText.kt` copy.** |
| Retention | Tied to opt-in; withdrawal triggers server erasure + local wipe. | Confirm server-side purge SLA. |
| Transport | HTTPS enforced via network security config; cleartext only to localhost test host. | Confirm TLS/pinning expectations for prod. |
| API auth | `X-API-Key` header (POC). | Decide prod auth (mTLS / OAuth) with vendor. |
| Data minimisation | Only mac_hash + type + optional label + masked mobile. | Decide whether to drop `label` for zero-PII. |
| Backup | `allowBackup=false`. | OK. |
| Withdrawal UX | One tap + confirm, on Manage screen. | OK. |
| Granular consent | User can revoke consent per individual device; removing the last device triggers full erasure. Server deletes only that device's hash. | Confirm per-device erasure SLA with vendor. |

| Bluetooth gate | App blocks at the permission step until the user turns Bluetooth ON (system enable dialog); re-checks on resume. | OK. |

**Open items for vendor (EasyReach / Apurva):**
- Beacon must hash detected MACs with the *same* salt/algorithm (`SHA-256(salt+UPPER(MAC))`).
- Server must enforce consent-present before storing, and support `/withdraw` erasure.
- Agree salt provisioning (per-tenant, server-delivered) and rotation handling.
