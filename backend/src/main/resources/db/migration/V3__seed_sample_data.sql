-- Development/demo-only seed: populates realistic sample data for the
-- existing schema (customers, policies, claims, renewals) so the dashboard
-- and other screens have meaningful data to display in a local/dev
-- environment. No entities, APIs, controllers, services, or the frontend
-- are touched by this migration -- it only inserts rows using the tables
-- already created by V1__init_schema.sql.
--
-- All ids/business-keys use fixed, deterministic values (uuid prefixes
-- 'aaaaaaaa-'/'bbbbbbbb-'/'cccccccc-'/'dddddddd-', policy/claim numbers
-- prefixed 'POL-DEV-'/'CLM-DEV-', emails suffixed '@seed.local') that do
-- not overlap with any previously/manually seeded rows (e.g. the existing
-- 'admin' user or any data created via scripts/seed-dev-data.sh), and
-- ON CONFLICT DO NOTHING guards are added on the unique columns so this
-- migration is safe even if it is ever re-applied to a database that
-- already has some of these rows.
--
-- Dates are computed relative to CURRENT_DATE / now() at the time this
-- migration runs, so "policy expiring soon" / "recently filed claim" style
-- data stays meaningful whenever a fresh database is seeded.

-- ---------------------------------------------------------------------
-- 20 customers
-- ---------------------------------------------------------------------
INSERT INTO customers (
    id, first_name, last_name, email, phone_number, date_of_birth,
    address_line, city, postal_code, country, created_at, updated_at
)
VALUES
    ('aaaaaaaa-0000-0000-0000-000000000001', 'Michael',    'Johnson',   'michael.johnson@seed.local',   '555-0101', DATE '1985-03-14', '120 Maple Ave',      'Springfield',  '62701', 'USA', now(), now()),
    ('aaaaaaaa-0000-0000-0000-000000000002', 'Sarah',      'Williams',  'sarah.williams@seed.local',    '555-0102', DATE '1990-07-22', '45 Oak St',          'Franklin',     '37064', 'USA', now(), now()),
    ('aaaaaaaa-0000-0000-0000-000000000003', 'David',      'Brown',     'david.brown@seed.local',       '555-0103', DATE '1978-11-05', '89 Pine Rd',         'Georgetown',   '40324', 'USA', now(), now()),
    ('aaaaaaaa-0000-0000-0000-000000000004', 'Emily',      'Jones',     'emily.jones@seed.local',       '555-0104', DATE '1995-01-30', '210 Cedar Ln',       'Salem',        '97301', 'USA', now(), now()),
    ('aaaaaaaa-0000-0000-0000-000000000005', 'James',      'Garcia',    'james.garcia@seed.local',      '555-0105', DATE '1982-05-18', '33 Elm St',          'Fairview',     '75069', 'USA', now(), now()),
    ('aaaaaaaa-0000-0000-0000-000000000006', 'Olivia',     'Miller',    'olivia.miller@seed.local',     '555-0106', DATE '1988-09-09', '77 Birch Dr',        'Madison',      '53703', 'USA', now(), now()),
    ('aaaaaaaa-0000-0000-0000-000000000007', 'Robert',     'Davis',     'robert.davis@seed.local',      '555-0107', DATE '1975-12-25', '14 Spruce Ct',       'Arlington',    '22201', 'USA', now(), now()),
    ('aaaaaaaa-0000-0000-0000-000000000008', 'Sophia',     'Rodriguez', 'sophia.rodriguez@seed.local',  '555-0108', DATE '1993-04-02', '56 Willow Way',      'Clinton',      '52732', 'USA', now(), now()),
    ('aaaaaaaa-0000-0000-0000-000000000009', 'William',    'Martinez',  'william.martinez@seed.local',  '555-0109', DATE '1980-08-17', '98 Chestnut Blvd',   'Greenville',   '27858', 'USA', now(), now()),
    ('aaaaaaaa-0000-0000-0000-000000000010', 'Ava',        'Hernandez', 'ava.hernandez@seed.local',     '555-0110', DATE '1997-02-11', '23 Walnut St',       'Bristol',      '37620', 'USA', now(), now()),
    ('aaaaaaaa-0000-0000-0000-000000000011', 'Daniel',     'Lopez',     'daniel.lopez@seed.local',      '555-0111', DATE '1983-06-28', '61 Ashford Rd',      'Dover',        '19901', 'USA', now(), now()),
    ('aaaaaaaa-0000-0000-0000-000000000012', 'Isabella',   'Gonzalez',  'isabella.gonzalez@seed.local', '555-0112', DATE '1991-10-14', '145 Hillcrest Dr',   'Manchester',   '03101', 'USA', now(), now()),
    ('aaaaaaaa-0000-0000-0000-000000000013', 'Matthew',    'Wilson',    'matthew.wilson@seed.local',    '555-0113', DATE '1979-03-03', '8 Lakeview Ave',     'Auburn',       '36830', 'USA', now(), now()),
    ('aaaaaaaa-0000-0000-0000-000000000014', 'Mia',        'Anderson',  'mia.anderson@seed.local',      '555-0114', DATE '1996-12-19', '190 Sunset Blvd',    'Ashland',      '44805', 'USA', now(), now()),
    ('aaaaaaaa-0000-0000-0000-000000000015', 'Andrew',     'Thomas',    'andrew.thomas@seed.local',     '555-0115', DATE '1986-07-07', '27 Meadow Ln',       'Burlington',   '05401', 'USA', now(), now()),
    ('aaaaaaaa-0000-0000-0000-000000000016', 'Charlotte',  'Taylor',    'charlotte.taylor@seed.local',  '555-0116', DATE '1992-11-23', '66 Ridgewood Dr',    'Centerville',  '45459', 'USA', now(), now()),
    ('aaaaaaaa-0000-0000-0000-000000000017', 'Joseph',     'Moore',     'joseph.moore@seed.local',      '555-0117', DATE '1977-05-09', '39 Fairview St',     'Milford',      '06460', 'USA', now(), now()),
    ('aaaaaaaa-0000-0000-0000-000000000018', 'Amelia',     'Jackson',   'amelia.jackson@seed.local',    '555-0118', DATE '1994-09-15', '112 Brookside Ave',  'Newport',      '02840', 'USA', now(), now()),
    ('aaaaaaaa-0000-0000-0000-000000000019', 'Christopher','Martin',    'christopher.martin@seed.local','555-0119', DATE '1981-01-27', '5 Highland Rd',      'Oxford',       '38655', 'USA', now(), now()),
    ('aaaaaaaa-0000-0000-0000-000000000020', 'Harper',     'Lee',       'harper.lee@seed.local',        '555-0120', DATE '1998-04-08', '84 Riverside Dr',    'Riverside',    '92501', 'USA', now(), now())
ON CONFLICT (email) DO NOTHING;

-- ---------------------------------------------------------------------
-- 50 policies (customer_idx 1-20, cycling ~2-3 policies per customer)
-- Status mix: 20 ACTIVE (far from expiry), 6 ACTIVE (expiring within the
-- next 30 days -- exercises the dashboard's "upcoming renewal" view),
-- 2 DRAFT (future start date), 12 EXPIRED, 6 CANCELLED, 4 LAPSED.
-- ---------------------------------------------------------------------
INSERT INTO policies (
    id, policy_number, customer_id, policy_type, status,
    coverage_amount, premium_amount, start_date, end_date,
    created_at, updated_at
)
SELECT
    ('bbbbbbbb-0000-0000-0000-' || lpad(idx::text, 12, '0'))::uuid,
    'POL-DEV-' || lpad(idx::text, 4, '0'),
    ('aaaaaaaa-0000-0000-0000-' || lpad(customer_idx::text, 12, '0'))::uuid,
    policy_type,
    status,
    coverage_amount,
    ROUND(coverage_amount * rate, 2),
    (CURRENT_DATE + make_interval(days => end_offset_days) - INTERVAL '365 days')::date,
    (CURRENT_DATE + make_interval(days => end_offset_days))::date,
    now(),
    now()
FROM (
    SELECT
        v.idx,
        v.customer_idx,
        v.status,
        v.end_offset_days,
        (ARRAY['AUTO', 'HEALTH', 'HOME', 'LIFE', 'TRAVEL'])[((v.idx - 1) % 5) + 1] AS policy_type,
        CASE (ARRAY['AUTO', 'HEALTH', 'HOME', 'LIFE', 'TRAVEL'])[((v.idx - 1) % 5) + 1]
            WHEN 'AUTO'   THEN 20000  + (v.idx * 733  % 30000)
            WHEN 'HEALTH' THEN 50000  + (v.idx * 2222 % 150000)
            WHEN 'HOME'   THEN 150000 + (v.idx * 4111 % 250000)
            WHEN 'LIFE'   THEN 100000 + (v.idx * 5555 % 400000)
            WHEN 'TRAVEL' THEN 5000   + (v.idx * 333  % 15000)
        END::numeric(19,2) AS coverage_amount,
        CASE (ARRAY['AUTO', 'HEALTH', 'HOME', 'LIFE', 'TRAVEL'])[((v.idx - 1) % 5) + 1]
            WHEN 'AUTO'   THEN 0.045
            WHEN 'HEALTH' THEN 0.06
            WHEN 'HOME'   THEN 0.008
            WHEN 'LIFE'   THEN 0.015
            WHEN 'TRAVEL' THEN 0.03
        END AS rate
    FROM (VALUES
        (1,  1,  'ACTIVE',    100),
        (2,  2,  'ACTIVE',    120),
        (3,  3,  'ACTIVE',    140),
        (4,  4,  'ACTIVE',    160),
        (5,  5,  'ACTIVE',    180),
        (6,  6,  'ACTIVE',    200),
        (7,  7,  'ACTIVE',    220),
        (8,  8,  'ACTIVE',    240),
        (9,  9,  'ACTIVE',    260),
        (10, 10, 'ACTIVE',    280),
        (11, 11, 'ACTIVE',    300),
        (12, 12, 'ACTIVE',    320),
        (13, 13, 'ACTIVE',    340),
        (14, 14, 'ACTIVE',    360),
        (15, 15, 'ACTIVE',    380),
        (16, 16, 'ACTIVE',    440),
        (17, 17, 'ACTIVE',    460),
        (18, 18, 'ACTIVE',    480),
        (19, 19, 'ACTIVE',    500),
        (20, 20, 'ACTIVE',    520),
        (21, 1,  'DRAFT',     400),
        (22, 2,  'DRAFT',     420),
        (23, 3,  'ACTIVE',    5),
        (24, 4,  'ACTIVE',    10),
        (25, 5,  'ACTIVE',    15),
        (26, 6,  'ACTIVE',    20),
        (27, 7,  'ACTIVE',    25),
        (28, 8,  'ACTIVE',    29),
        (29, 9,  'EXPIRED',   -15),
        (30, 10, 'EXPIRED',   -30),
        (31, 11, 'EXPIRED',   -45),
        (32, 12, 'EXPIRED',   -60),
        (33, 13, 'EXPIRED',   -90),
        (34, 14, 'EXPIRED',   -120),
        (35, 15, 'EXPIRED',   -150),
        (36, 16, 'EXPIRED',   -180),
        (37, 17, 'EXPIRED',   -210),
        (38, 18, 'EXPIRED',   -240),
        (39, 19, 'EXPIRED',   -270),
        (40, 20, 'EXPIRED',   -300),
        (41, 1,  'CANCELLED', 50),
        (42, 2,  'CANCELLED', 150),
        (43, 3,  'CANCELLED', 250),
        (44, 4,  'CANCELLED', -20),
        (45, 5,  'CANCELLED', -100),
        (46, 6,  'CANCELLED', 90),
        (47, 7,  'LAPSED',    -10),
        (48, 8,  'LAPSED',    -25),
        (49, 9,  'LAPSED',    -40),
        (50, 10, 'LAPSED',    -55)
    ) AS v(idx, customer_idx, status, end_offset_days)
) t
ON CONFLICT (policy_number) DO NOTHING;

-- ---------------------------------------------------------------------
-- 15 claims against 15 of the policies above (3 each across the 5
-- ClaimStatus values: SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED, PAID)
-- ---------------------------------------------------------------------
INSERT INTO claims (
    id, claim_number, policy_id, claim_amount, approved_amount, status,
    incident_date, description, rejection_reason, filed_at, resolved_at,
    created_at, updated_at
)
SELECT
    ('cccccccc-0000-0000-0000-' || lpad(claim_idx::text, 12, '0'))::uuid,
    'CLM-DEV-' || lpad(claim_idx::text, 4, '0'),
    ('bbbbbbbb-0000-0000-0000-' || lpad(policy_idx::text, 12, '0'))::uuid,
    claim_amount,
    approved_amount,
    status,
    (CURRENT_DATE + make_interval(days => incident_offset))::date,
    description,
    rejection_reason,
    now() + make_interval(days => filed_offset),
    CASE WHEN resolved_offset IS NULL THEN NULL ELSE now() + make_interval(days => resolved_offset) END,
    now(),
    now()
FROM (VALUES
    (1,  1,  'SUBMITTED',    3200.00,  NULL::numeric,     NULL, -10,  -8,   NULL::int, 'Rear-end collision - bumper and trunk damage'),
    (2,  3,  'SUBMITTED',    8600.00,  NULL::numeric,     NULL, -5,   -3,   NULL::int, 'Water damage from burst pipe in upstairs bathroom'),
    (3,  5,  'SUBMITTED',    950.00,   NULL::numeric,     NULL, -3,   -1,   NULL::int, 'Trip cancellation due to airline strike'),
    (4,  7,  'UNDER_REVIEW', 4300.00,  NULL::numeric,     NULL, -20,  -18,  NULL::int, 'Emergency room visit for fractured arm'),
    (5,  9,  'UNDER_REVIEW', 15000.00, NULL::numeric,     NULL, -25,  -22,  NULL::int, 'Critical illness rider claim - cancer diagnosis'),
    (6,  23, 'UNDER_REVIEW', 5400.00,  NULL::numeric,     NULL, -15,  -12,  NULL::int, 'Storm damage to roof shingles'),
    (7,  25, 'APPROVED',     1800.00,  1600.00,            NULL, -40,  -38,  -30,       'Lost luggage during international trip'),
    (8,  27, 'APPROVED',     6200.00,  5800.00,            NULL, -50,  -47,  -35,       'Surgical procedure - appendectomy'),
    (9,  29, 'APPROVED',     20000.00, 18000.00,           NULL, -60,  -55,  -40,       'Terminal illness accelerated benefit claim'),
    (10, 31, 'REJECTED',     3900.00,  NULL::numeric,     'Damage predates policy start date', -70, -65, -55,        'Collision damage claimed outside coverage period'),
    (11, 33, 'REJECTED',     12000.00, NULL::numeric,     'Damage caused by lack of maintenance, not a covered peril', -80, -75, -60, 'Water damage attributed to long-term neglect'),
    (12, 35, 'REJECTED',     700.00,   NULL::numeric,     'Cancellation reason not covered under policy terms', -90, -85, -70,        'Trip cancellation for personal reasons'),
    (13, 41, 'PAID',         4100.00,  3900.00,            NULL, -120, -115, -95,       'Collision with guardrail - front-end damage'),
    (14, 43, 'PAID',         9800.00,  9200.00,            NULL, -150, -145, -120,      'Kitchen fire smoke damage'),
    (15, 47, 'PAID',         5600.00,  5200.00,            NULL, -200, -195, -170,      'Hospitalization for pneumonia treatment')
) AS v(claim_idx, policy_idx, status, claim_amount, approved_amount, rejection_reason, incident_offset, filed_offset, resolved_offset, description)
ON CONFLICT (claim_number) DO NOTHING;

-- ---------------------------------------------------------------------
-- 20 renewals against 20 of the policies above: 8 PENDING (mostly the
-- policies expiring within 30 days, so the dashboard has real upcoming
-- renewals to show), 6 CONFIRMED, 3 REJECTED, 3 EXPIRED_UNRENEWED.
-- previous_end_date is derived from the same end_offset_days used for
-- that policy above, so it matches the policy's actual end_date.
-- ---------------------------------------------------------------------
INSERT INTO renewals (
    id, policy_id, previous_end_date, new_end_date, revised_premium_amount,
    status, requested_at, decided_at, created_at, updated_at
)
SELECT
    ('dddddddd-0000-0000-0000-' || lpad(renewal_idx::text, 12, '0'))::uuid,
    ('bbbbbbbb-0000-0000-0000-' || lpad(policy_idx::text, 12, '0'))::uuid,
    (CURRENT_DATE + make_interval(days => prev_end_offset))::date,
    (CURRENT_DATE + make_interval(days => prev_end_offset) + INTERVAL '365 days')::date,
    revised_premium,
    status,
    now() + make_interval(days => requested_offset),
    CASE WHEN decided_offset IS NULL THEN NULL ELSE now() + make_interval(days => decided_offset) END,
    now(),
    now()
FROM (VALUES
    (1,  23, 'PENDING',           5,    1450.00, -5,   NULL::int),
    (2,  24, 'PENDING',           10,   980.00,  -3,   NULL::int),
    (3,  25, 'PENDING',           15,   2200.00, -7,   NULL::int),
    (4,  26, 'PENDING',           20,   1750.00, -2,   NULL::int),
    (5,  27, 'PENDING',           25,   3100.00, -4,   NULL::int),
    (6,  28, 'PENDING',           29,   1600.00, -1,   NULL::int),
    (7,  2,  'PENDING',           120,  2900.00, -10,  NULL::int),
    (8,  4,  'PENDING',           160,  1350.00, -15,  NULL::int),
    (9,  29, 'CONFIRMED',         -15,  1500.00, -35,  -30),
    (10, 30, 'CONFIRMED',         -30,  2100.00, -50,  -45),
    (11, 31, 'CONFIRMED',         -45,  1850.00, -65,  -60),
    (12, 32, 'CONFIRMED',         -60,  2600.00, -80,  -75),
    (13, 33, 'CONFIRMED',         -90,  1400.00, -110, -105),
    (14, 34, 'CONFIRMED',         -120, 1950.00, -140, -135),
    (15, 35, 'REJECTED',          -150, 1300.00, -170, -165),
    (16, 36, 'REJECTED',          -180, 2400.00, -200, -195),
    (17, 37, 'REJECTED',          -210, 1700.00, -230, -225),
    (18, 38, 'EXPIRED_UNRENEWED', -240, 1550.00, -270, NULL::int),
    (19, 39, 'EXPIRED_UNRENEWED', -270, 2050.00, -300, NULL::int),
    (20, 40, 'EXPIRED_UNRENEWED', -300, 1250.00, -330, NULL::int)
) AS v(renewal_idx, policy_idx, status, prev_end_offset, revised_premium, requested_offset, decided_offset)
ON CONFLICT (id) DO NOTHING;
