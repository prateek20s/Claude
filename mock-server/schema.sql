-- ============================================================================
-- BFL Beacon POC — Canonical server storage format for consented device data
-- Engine shown: SQLite (POC). Same shape maps 1:1 to MySQL/Postgres in prod.
-- PRIVACY: raw MAC addresses are NEVER stored. Only a salted SHA-256 hash.
-- ============================================================================

-- One row per consented customer -------------------------------------------
CREATE TABLE IF NOT EXISTS consent_users (
    user_ref            TEXT PRIMARY KEY,         -- opaque server ID, e.g. u_0d47c4b814b8
    user_mobile_masked  TEXT,                     -- display only, e.g. 98XXXXXX21 (never full number)
    consent_given       INTEGER NOT NULL,         -- 1 = yes, 0 = no
    consent_version     TEXT    NOT NULL,         -- notice version the user accepted, e.g. "1.0"
    consent_ts_utc      TEXT    NOT NULL,         -- ISO-8601 UTC when consent was given
    purposes            TEXT    NOT NULL,         -- JSON array, e.g. ["in_store_identification","personalized_offers"]
    status              TEXT    NOT NULL DEFAULT 'active',  -- active | withdrawn
    registered_at       TEXT    NOT NULL,         -- ISO-8601 UTC
    updated_at          TEXT    NOT NULL          -- ISO-8601 UTC
);

-- One row per consented peripheral (the data the beacon matches against) -----
CREATE TABLE IF NOT EXISTS consent_devices (
    device_id      TEXT PRIMARY KEY,              -- opaque ID, e.g. d_9f1c7a2b
    user_ref       TEXT NOT NULL REFERENCES consent_users(user_ref) ON DELETE CASCADE,
    mac_hash       TEXT NOT NULL,                 -- salted SHA-256 of the MAC, 64 lowercase hex chars
    hash_algo      TEXT NOT NULL DEFAULT 'SHA-256',
    salt_version   TEXT NOT NULL,                 -- which server salt produced this hash, e.g. "v1"
    device_type    TEXT,                          -- Watch | Earbuds | Phone | Other
    label          TEXT,                          -- optional human label, e.g. "Galaxy Watch6"
    status         TEXT NOT NULL DEFAULT 'active',-- active | revoked
    registered_at  TEXT NOT NULL,                 -- ISO-8601 UTC
    revoked_at     TEXT                           -- ISO-8601 UTC, null while active
);

-- The beacon match path hits this index: WHERE mac_hash = ? AND status='active'
CREATE INDEX IF NOT EXISTS idx_devices_machash ON consent_devices(mac_hash);
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_machash ON consent_devices(user_ref, mac_hash);

-- Append-only consent audit trail (NO MAC / PII) — compliance evidence -------
CREATE TABLE IF NOT EXISTS consent_audit (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_ref   TEXT NOT NULL,
    event      TEXT NOT NULL,                     -- REGISTERED | DEVICE_REVOKED | WITHDRAWN
    detail     TEXT,                              -- e.g. device_type, never a MAC
    ts_utc     TEXT NOT NULL
);
