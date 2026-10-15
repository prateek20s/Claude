# Server Storage Format — Consented Device (MAC) Data

Canonical format in which all consented peripheral-MAC data is stored on the
server. The POC implements this in SQLite (`mock-server/schema.sql`); the same
shape maps 1:1 to MySQL/Postgres in production.

> **Core privacy rule:** the raw MAC address is **never stored**. Only a salted
> SHA-256 hash of the MAC is persisted. The beacon hashes detected MACs with the
> same salt and matches on the hash, so no readable device identifier ever sits
> in the database.

---

## 1. The MAC hash — exact definition

The single value everything matches on.

```
mac_hash = SHA-256( SALT_VALUE + UPPERCASE(mac_with_colons) )
```

| Rule            | Value                                                        |
|-----------------|--------------------------------------------------------------|
| Input MAC form  | Uppercase, colon-separated — e.g. `A4:C3:61:22:9F:12`        |
| Salt            | Server-issued secret, identified by `salt_version` (e.g. `v1`); **kept in server config/secrets, never in the DB** |
| Algorithm       | SHA-256                                                      |
| Output          | 64 lowercase hex characters                                  |

Both sides must compute this identically: the **app** at registration and the
**beacon** at detection. A mismatch in case, separators, or salt = no match.

---

## 2. Record format (canonical JSON)

What one consented customer looks like (as returned by `/debug/export`):

```json
{
  "user_ref": "u_37bee504922d",
  "user_mobile_masked": "98XXXXXX21",
  "consent": {
    "given": true,
    "version": "1.0",
    "timestamp_utc": "2026-10-15T08:52:00Z",
    "purposes": ["in_store_identification", "personalized_offers"]
  },
  "status": "active",
  "registered_at": "2026-10-15T08:52:39Z",
  "updated_at": "2026-10-15T08:52:39Z",
  "devices": [
    {
      "device_id": "d_bb9e721ea0",
      "mac_hash": "356222c5e423bdf26dbdba7dfe3840ed6216b156364622934ddfe8d70b433fbd",
      "hash_algo": "SHA-256",
      "salt_version": "v1",
      "device_type": "Watch",
      "label": "Galaxy Watch6",
      "status": "active",
      "registered_at": "2026-10-15T08:52:39Z",
      "revoked_at": null
    }
  ]
}
```

---

## 3. Relational schema (the source of truth)

### `consent_users` — one row per consented customer
| Column             | Type    | Notes                                              |
|--------------------|---------|----------------------------------------------------|
| user_ref           | TEXT PK | Opaque server ID (`u_…`)                            |
| user_mobile_masked | TEXT    | Display only, e.g. `98XXXXXX21` — never full number |
| consent_given      | INT     | 1 / 0                                              |
| consent_version    | TEXT    | Notice version accepted                            |
| consent_ts_utc     | TEXT    | ISO-8601 UTC                                       |
| purposes           | TEXT    | JSON array                                         |
| status             | TEXT    | `active` \| `withdrawn`                            |
| registered_at      | TEXT    | ISO-8601 UTC                                       |
| updated_at         | TEXT    | ISO-8601 UTC                                       |

### `consent_devices` — one row per consented peripheral (the beacon match table)
| Column        | Type    | Notes                                               |
|---------------|---------|-----------------------------------------------------|
| device_id     | TEXT PK | Opaque ID (`d_…`)                                   |
| user_ref      | TEXT FK | → `consent_users.user_ref`, `ON DELETE CASCADE`     |
| **mac_hash**  | TEXT    | **Salted SHA-256, 64 lowercase hex — the match key**|
| hash_algo     | TEXT    | `SHA-256`                                           |
| salt_version  | TEXT    | Which salt produced the hash (`v1`) — enables rotation |
| device_type   | TEXT    | `Watch` \| `Earbuds` \| `Phone` \| `Other`          |
| label         | TEXT    | Optional human label; drop for zero-PII mode        |
| status        | TEXT    | `active` \| `revoked`                               |
| registered_at | TEXT    | ISO-8601 UTC                                        |
| revoked_at    | TEXT    | null while active                                   |

Indexes: `idx_devices_machash(mac_hash)` for the match path; unique `(user_ref, mac_hash)`.

### `consent_audit` — append-only compliance trail (holds NO MAC / PII)
`id, user_ref, event (REGISTERED | DEVICE_REVOKED | WITHDRAWN), detail, ts_utc`

---

## 4. The match path (what the beacon query is)
```sql
SELECT d.user_ref, u.user_mobile_masked
FROM consent_devices d JOIN consent_users u ON u.user_ref = d.user_ref
WHERE d.mac_hash = :detected_hash
  AND d.status = 'active' AND u.status = 'active';
```

## 5. Lifecycle → what changes in the store
| Action              | Effect on storage                                             |
|---------------------|---------------------------------------------------------------|
| Register            | Insert `consent_users` row + one `consent_devices` row per device; audit `REGISTERED` |
| Revoke one device   | **Delete** that `consent_devices` row (erasure); audit `DEVICE_REVOKED`. If it was the last device, also delete the user → audit `WITHDRAWN` |
| Withdraw (all)      | **Delete** all the user's `consent_devices` rows + the `consent_users` row; audit `WITHDRAWN` |

Deletion is true erasure (DPDP). The audit trail records that an event happened,
without retaining any MAC or device identifier.

## 6. Prod notes for the vendor (EasyReach / Apurva)
- Store the **salt** in a secrets manager, versioned; never in the DB or client.
- Rotating the salt means `salt_version` increments; existing hashes stay valid
  under their old version until re-registered.
- Encrypt at rest; restrict read access on `consent_devices` to the match service.
- `mac_hash` is pseudonymised personal data under DPDP — treat accordingly.
