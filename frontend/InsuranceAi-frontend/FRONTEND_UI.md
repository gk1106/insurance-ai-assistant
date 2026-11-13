# Insurance AI frontend

The existing React 19, React Router and Vite application now uses a shared premium insurance design system. CSS Modules remain the styling architecture; Tailwind was not added. Inter and IBM Plex Mono are hosted locally. Lucide icons are tree-shaken. Secondary routes load on demand.

## Run and verify

```sh
npm ci
npm run dev
npm run lint
npm run build
npx playwright install chromium
npm run test:e2e
```

The existing Vite proxy forwards `/api` to `http://localhost:8082`. Set `VITE_API_BASE_URL` when a different API origin is required. Existing login, JWT storage, customer access rules, policy/claim mutations and renewal workflows remain connected to their original endpoints.

## Pages and behavior

- Dashboard: authenticated records, active-policy coverage totals, pending claims, next-expiring active policy premium, record activity, quick actions.
- AI Assistant: all four existing agent endpoints, safe Markdown and GFM tables, structured insurance cards, source document names, timestamps, copy, retry, optional local text history and conversation restoration.
- Policies: cards/table views, current-page status filters, coverage/details dialog and existing staff/customer actions.
- Claims: current-page summary, details dialog, lifecycle stages and existing claim submission/review/payment workflows.
- Premium Calculator: validated educational model with coverage, deductible and term inputs; monthly, yearly and term estimates. Rates are explicitly illustrative, not insurer quotes.
- Documents: real PDF validation and browser IndexedDB persistence, category filters, drag/drop, save progress, preview, download and deletion.
- Chat History and Settings: user-scoped browser history, conversation search/restoration, account details and persisted theme preference.

## Backend boundaries

There is no customer document upload/download endpoint, premium-rating endpoint, notifications feed or payment ledger. Documents remain on this browser and are not sent to the AI. The calculator uses clearly disclosed sample rates. Notifications show a truthful empty state. Dashboard premium means the recorded premium on the next-expiring active policy; no scheduled debit is inferred.

The AI APIs return complete JSON responses rather than a stream. The UI presents an honest pending state and displays the completed answer. Regeneration uses `/api/ai/knowledge-agent/chat` for source-backed answers only; record-changing requests are never automatically replayed.

Chat text persistence is opt-in. Session metadata is local; structured result DTOs and document-based answer text are excluded from saved history. Local data is scoped by username for organization and is not application-encrypted. It does not synchronize between devices.

## Verification

Playwright uses isolated browser contexts and explicit API fixtures, with no real credentials or production writes. Tests cover customer request scoping, dashboard values, Markdown/source rendering, read-only regeneration, history restoration, Enter/Shift+Enter, errors/retry, policy filters/dialogs, claim lifecycle, calculator validation, PDF persistence/preview/download/delete, mobile focus handling/overflow, search and dark mode. Screenshots are written into ignored `test-results/` for desktop and mobile inspection. These tests do not replace live backend integration testing.

## Indian localization

All financial displays and input labels use INR. The shared `src/utils/currency.js` formatter uses `en-IN` grouping and two decimal places for paise. Numeric amounts are not converted or scaled. Demo customer names use Indian identities; authenticated usernames continue to come from the account. Run `npm run test:unit` for formatting coverage.
