# Backend India localization

All platform money fields use INR (Indian rupees): `coverageAmount`, `premiumAmount`,
`claimAmount`, `approvedAmount`, and `revisedPremiumAmount`, in requests, responses,
AI/MCP tools, and persisted policy, claim and renewal records. Values remain numeric
Java `BigDecimal` / SQL numeric values; DTO field names and constructors are unchanged.
For example, `premiumAmount: 800` means INR 800.00. Clients may format using `en-IN`
and the rupee symbol, but must submit numbers without symbols or separators.

Existing stored amounts are treated as INR with their exact existing numeric values.
There is no automatic FX conversion, rescaling, or multiplication. Imports and callers
must provide INR amounts. The shared `IndiaCurrencyPolicy` instruction applies to all
four domain agents and orchestrator routing/synthesis. Knowledge answers retain explicit
foreign-currency attribution from documents instead of silently relabeling it as INR.

Flyway V1 through V4 remain unchanged. On the next backend startup, Flyway applies
`V5__localize_seed_customers_india.sql` after V4. It updates only the 20 known V3 customer
UUIDs whose complete original names, contact/address fields and birth dates still match.
Edited or real customers are preserved; destination-email conflicts are skipped.
The migration changes no auth usernames, policy/claim/renewal numbers, or money values.
It has not been run against a live database as part of this source change.

`scripts/seed-dev-data.sh` now creates an Indian sample customer in Pune and a future
`aarav_sharma` login. Existing `jamie_rivera` and other auth accounts remain unchanged.
The script performs API writes and an admin bootstrap SQL update; run it only deliberately
against a development environment. A database reset is not required for localization.

Run the normal unit suite with `mvn -o test` first (or an available cached Maven wrapper
distribution). This checkout contains no Maven wrapper script. Live AI evaluation tests
remain excluded by default; unit tests do not require a database or OpenAI key.
