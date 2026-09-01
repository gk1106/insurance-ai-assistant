-- Development/test-only seed: creates a single ADMIN user so the deployed
-- app can be logged into and tested before any real users exist.
--
-- Password hash was generated with the application's own BCryptPasswordEncoder
-- (default strength) for the plaintext "admin123" — never store plaintext
-- passwords. No plaintext password appears anywhere in this file or the DB.
--
-- SECURITY NOTE: "admin"/"admin123" is a weak, publicly-known test credential.
-- This migration is safe for a learning/demo deployment, but before any real
-- production use: log in once and change the password (no "change password"
-- endpoint exists yet, so for now that means updating password_hash directly,
-- or simply deleting this row), or remove this migration file entirely.
--
-- Idempotent: ON CONFLICT (username) DO NOTHING means this is a no-op if an
-- "admin" user already exists (e.g. created some other way) or if this
-- migration is ever re-applied to a database that already has one.

INSERT INTO users (id, username, email, password_hash, role, customer_id, enabled, created_at, updated_at)
VALUES (
    '70975aff-125f-4c0b-952f-0477d412dfa4',
    'admin',
    'admin@insuranceai.local',
    '$2a$10$b4Id0vRTguWIRHnvebHkAepDywUQfYxFEIBVKKsG/9x3ffkMJhu82',
    'ADMIN',
    NULL,
    TRUE,
    now(),
    now()
)
ON CONFLICT (username) DO NOTHING;
