#!/usr/bin/env bash
# Development-only seed script for the Insurance AI Assistant backend.
#
# Populates a fresh dev database with realistic, business-rule-valid data by
# driving the REAL running API (not hand-written SQL), so every row is
# exactly what the application itself would produce. This guarantees
# correct policy/claim numbers, valid status transitions, and coverage-limit
# enforcement, instead of risking hand-crafted rows the service layer would
# never actually create.
#
# The one unavoidable exception: this backend has NO API path to create the
# very first ADMIN/AGENT account (self-registration is hard-coded to
# CUSTOMER, and there is no "create staff user" endpoint). Bootstrapping the
# first admin therefore requires one direct, minimal SQL statement that only
# flips a role column on an already-registered (and correctly password-
# hashed) user — this is a standard, common bootstrap pattern, not a
# workaround of business rules.
#
# Requirements: the backend + postgres containers from backend/docker-compose.yml
# must already be running (`docker compose up -d` from backend/), and `curl`,
# `node`, and `docker` must be on PATH.
#
# Usage: bash backend/scripts/seed-dev-data.sh
# Re-running against a non-empty DB just adds more data (no idempotency
# guard) — reset with `docker compose down -v && docker compose up -d` first
# if you want a clean slate.

set -euo pipefail

API="http://localhost:8082/api"
DB_CONTAINER="backend-postgres-1"
# A plain relative dir (not mktemp -d) avoids POSIX/Windows path-translation
# mismatches between Git Bash and native curl.exe/node.exe on Windows.
TMP="./.seed-tmp-$$"
mkdir -p "$TMP"
trap 'rm -rf "$TMP"' EXIT

# Fixture password for seeded dev/test accounts only — not a real secret.
SEED_PASSWORD="DevPassword123!"

json() { node -pe "JSON.parse(require('fs').readFileSync('$1','utf8'))$2"; }

echo "== 1. Registering bootstrap admin account (starts as CUSTOMER by design) =="
curl -sf -X POST "$API/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"seed_admin\",\"email\":\"seed_admin@example.dev\",\"password\":\"$SEED_PASSWORD\"}" \
  -o "$TMP/register_admin.json"
echo "  registered: $(json "$TMP/register_admin.json" .username)"

echo "== 2. Promoting seed_admin to ADMIN (the one direct SQL step — see header comment) =="
docker exec "$DB_CONTAINER" psql -U postgres -d insurance_ai -q \
  -c "UPDATE users SET role = 'ADMIN' WHERE username = 'seed_admin';"

echo "== 3. Logging in as seed_admin to get a staff-scoped JWT =="
curl -sf -X POST "$API/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"seed_admin\",\"password\":\"$SEED_PASSWORD\"}" \
  -o "$TMP/login_admin.json"
ADMIN_TOKEN="$(json "$TMP/login_admin.json" .token)"
echo "  role confirmed: $(json "$TMP/login_admin.json" .role)"

auth_curl() { curl -sf -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" "$@"; }

echo "== 4. Creating the seed customer =="
auth_curl -X POST "$API/customers" -d '{
  "firstName": "Jamie",
  "lastName": "Rivera",
  "email": "jamie.rivera@example.dev",
  "phoneNumber": "555-0142",
  "dateOfBirth": "1988-04-12",
  "addressLine": "482 Maple Street",
  "city": "Springfield",
  "postalCode": "62704",
  "country": "USA"
}' -o "$TMP/customer.json"
CUSTOMER_ID="$(json "$TMP/customer.json" .id)"
echo "  customer id: $CUSTOMER_ID"

echo "== 5. Registering the CUSTOMER-role user, linked to that customer =="
curl -sf -X POST "$API/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"jamie_rivera\",\"email\":\"jamie.login@example.dev\",\"password\":\"$SEED_PASSWORD\",\"customerId\":\"$CUSTOMER_ID\"}" \
  -o "$TMP/register_customer.json"
echo "  linked user: $(json "$TMP/register_customer.json" .username) (role $(json "$TMP/register_customer.json" .role))"

echo "== 6. Creating policies (only ACTIVE/CANCELLED are reachable via the API — see header) =="

# Policy A: ACTIVE, ends in 20 days -> shows up in the "expiring within 30 days" panel
END_A="$(node -pe "new Date(Date.now()+20*86400000).toISOString().slice(0,10)")"
START_A="$(node -pe "new Date(Date.now()-11*30*86400000).toISOString().slice(0,10)")"
auth_curl -X POST "$API/policies" -d "{
  \"customerId\": \"$CUSTOMER_ID\",
  \"policyType\": \"AUTO\",
  \"coverageAmount\": 15000,
  \"premiumAmount\": 800,
  \"startDate\": \"$START_A\",
  \"endDate\": \"$END_A\"
}" -o "$TMP/policy_a.json"
POLICY_A="$(json "$TMP/policy_a.json" .id)"
echo "  policy A (expiring soon): $(json "$TMP/policy_a.json" .policyNumber) ends $END_A"

# Policy B: ACTIVE, ends in 200 days -> not expiring soon, used for claim variety
END_B="$(node -pe "new Date(Date.now()+200*86400000).toISOString().slice(0,10)")"
START_B="$(node -pe "new Date(Date.now()-6*30*86400000).toISOString().slice(0,10)")"
auth_curl -X POST "$API/policies" -d "{
  \"customerId\": \"$CUSTOMER_ID\",
  \"policyType\": \"HOME\",
  \"coverageAmount\": 250000,
  \"premiumAmount\": 1200,
  \"startDate\": \"$START_B\",
  \"endDate\": \"$END_B\"
}" -o "$TMP/policy_b.json"
POLICY_B="$(json "$TMP/policy_b.json" .id)"
echo "  policy B (long-dated): $(json "$TMP/policy_b.json" .policyNumber) ends $END_B"

# Policy C: created ACTIVE then cancelled -> CANCELLED
END_C="$(node -pe "new Date(Date.now()+270*86400000).toISOString().slice(0,10)")"
START_C="$(node -pe "new Date(Date.now()-3*30*86400000).toISOString().slice(0,10)")"
auth_curl -X POST "$API/policies" -d "{
  \"customerId\": \"$CUSTOMER_ID\",
  \"policyType\": \"HEALTH\",
  \"coverageAmount\": 50000,
  \"premiumAmount\": 400,
  \"startDate\": \"$START_C\",
  \"endDate\": \"$END_C\"
}" -o "$TMP/policy_c.json"
POLICY_C="$(json "$TMP/policy_c.json" .id)"
auth_curl -X POST "$API/policies/$POLICY_C/cancel" -o "$TMP/policy_c_cancelled.json"
echo "  policy C (cancelled): $(json "$TMP/policy_c_cancelled.json" .policyNumber) -> $(json "$TMP/policy_c_cancelled.json" .status)"

echo "== 7. Creating claims and driving them through real status transitions =="

# Claim 1 on Policy A -> left as SUBMITTED
IDATE1="$(node -pe "new Date(Date.now()-5*86400000).toISOString().slice(0,10)")"
auth_curl -X POST "$API/claims" -d "{
  \"policyId\": \"$POLICY_A\", \"claimAmount\": 2000, \"incidentDate\": \"$IDATE1\",
  \"description\": \"Minor collision damage to front bumper.\"
}" -o "$TMP/claim1.json"
echo "  claim 1: $(json "$TMP/claim1.json" .claimNumber) -> $(json "$TMP/claim1.json" .status)"

# Claim 2 on Policy A -> reviewed -> UNDER_REVIEW
IDATE2="$(node -pe "new Date(Date.now()-10*86400000).toISOString().slice(0,10)")"
auth_curl -X POST "$API/claims" -d "{
  \"policyId\": \"$POLICY_A\", \"claimAmount\": 1500, \"incidentDate\": \"$IDATE2\",
  \"description\": \"Windshield crack from road debris.\"
}" -o "$TMP/claim2.json"
CLAIM2="$(json "$TMP/claim2.json" .id)"
auth_curl -X POST "$API/claims/$CLAIM2/review" -o "$TMP/claim2_reviewed.json"
echo "  claim 2: $(json "$TMP/claim2_reviewed.json" .claimNumber) -> $(json "$TMP/claim2_reviewed.json" .status)"

# Claim 3 on Policy B -> reviewed -> approved -> APPROVED
IDATE3="$(node -pe "new Date(Date.now()-30*86400000).toISOString().slice(0,10)")"
auth_curl -X POST "$API/claims" -d "{
  \"policyId\": \"$POLICY_B\", \"claimAmount\": 5000, \"incidentDate\": \"$IDATE3\",
  \"description\": \"Storm damage to roof shingles.\"
}" -o "$TMP/claim3.json"
CLAIM3="$(json "$TMP/claim3.json" .id)"
auth_curl -X POST "$API/claims/$CLAIM3/review" -o /dev/null
auth_curl -X POST "$API/claims/$CLAIM3/approve" -d '{"approvedAmount": 4500}' -o "$TMP/claim3_approved.json"
echo "  claim 3: $(json "$TMP/claim3_approved.json" .claimNumber) -> $(json "$TMP/claim3_approved.json" .status)"

# Claim 4 on Policy B -> reviewed -> approved -> paid -> PAID
IDATE4="$(node -pe "new Date(Date.now()-45*86400000).toISOString().slice(0,10)")"
auth_curl -X POST "$API/claims" -d "{
  \"policyId\": \"$POLICY_B\", \"claimAmount\": 3000, \"incidentDate\": \"$IDATE4\",
  \"description\": \"Water damage from burst pipe.\"
}" -o "$TMP/claim4.json"
CLAIM4="$(json "$TMP/claim4.json" .id)"
auth_curl -X POST "$API/claims/$CLAIM4/review" -o /dev/null
auth_curl -X POST "$API/claims/$CLAIM4/approve" -d '{"approvedAmount": 3000}' -o /dev/null
auth_curl -X POST "$API/claims/$CLAIM4/pay" -o "$TMP/claim4_paid.json"
echo "  claim 4: $(json "$TMP/claim4_paid.json" .claimNumber) -> $(json "$TMP/claim4_paid.json" .status)"

# Claim 5 on Policy B -> rejected -> REJECTED
IDATE5="$(node -pe "new Date(Date.now()-2*86400000).toISOString().slice(0,10)")"
auth_curl -X POST "$API/claims" -d "{
  \"policyId\": \"$POLICY_B\", \"claimAmount\": 1000, \"incidentDate\": \"$IDATE5\",
  \"description\": \"Claim for pre-existing fence damage.\"
}" -o "$TMP/claim5.json"
CLAIM5="$(json "$TMP/claim5.json" .id)"
auth_curl -X POST "$API/claims/$CLAIM5/reject" -d '{"reason": "Damage pre-dates policy start date."}' \
  -o "$TMP/claim5_rejected.json"
echo "  claim 5: $(json "$TMP/claim5_rejected.json" .claimNumber) -> $(json "$TMP/claim5_rejected.json" .status)"

echo ""
echo "== Done =="
echo "Staff login  -> username: seed_admin       password: $SEED_PASSWORD  (role: ADMIN)"
echo "Customer login -> username: jamie_rivera   password: $SEED_PASSWORD  (role: CUSTOMER, linked to customer $CUSTOMER_ID)"
