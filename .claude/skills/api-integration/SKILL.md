---
name: api-integration
description: Axios-based API service layer conventions for the Insurance AI Assistant frontend — centralized config, JWT auth handling, request/response and error handling. Use when writing or reviewing any code that calls the backend API.
---

# API Integration — Insurance AI Assistant

The frontend talks to the Spring Boot backend (`backend/`, base path `/api`, JWT bearer auth). All API access goes through a small, centralized service layer — **never call `axios` directly from a component or page**.

## Centralized Axios instance

One configured instance, created once:

```
src/api/client.js
```

- `baseURL` comes from an environment variable (`import.meta.env.VITE_API_BASE_URL`, e.g. `http://localhost:8082/api`), never hardcoded inline in multiple files.
- A **request interceptor** attaches the JWT from storage as `Authorization: Bearer <token>` on every request (skip it for the two public auth endpoints if you're calling them through the same instance).
- A **response interceptor**:
  - On `401 Unauthorized`, clears the stored session and redirects to `/login` — a single place handles "the token expired/is invalid," instead of every call site checking for it.
  - Otherwise passes through the backend's error shape (see below) so callers can read `error.response.data`.

## One module per backend domain

Mirror the backend's domains — don't scatter endpoint calls across the codebase:

```
src/api/
├── client.js        # the axios instance + interceptors
├── authApi.js        # register, login
├── customerApi.js
├── policyApi.js
├── claimApi.js
└── renewalApi.js
```

Each module exports plain functions wrapping the actual endpoints, e.g. `policyApi.js`:

- `create(data)` → `POST /policies`
- `getById(id)` → `GET /policies/{id}`
- `getByPolicyNumber(policyNumber)` → `GET /policies/number/{policyNumber}`
- `list({ customerId, page, size })` → `GET /policies` or `GET /policies?customerId=`
- `update(id, data)` → `PUT /policies/{id}`
- `cancel(id)` → `POST /policies/{id}/cancel`
- `renew(id, data)` → `POST /policies/{id}/renew`
- `getExpiring(days)` → `GET /policies/expiring?days=`

Follow the same one-function-per-endpoint shape for `customerApi`, `claimApi` (including `review`, `approve`, `reject`, `pay`), and `renewalApi` (including `confirm`, `reject`). Pages/components import these functions — they never touch `axios` or a raw URL string.

## Auth handling

- `authApi.login({ username, password })` and `authApi.register({ username, email, password, customerId })` call `POST /api/auth/login` / `POST /api/auth/register`.
- Both return `{ token, username, role, expiresAt }`. Store the token (and decoded role/expiry) in `AuthContext` and persist it (`localStorage` is the simplest option for this project) so a page refresh doesn't log the user out.
- There is no refresh-token endpoint in the current backend — a token is valid until `expiresAt`. Treat any `401` as "session invalid, go to `/login`," rather than trying to silently refresh.
- Never log the token or send it anywhere other than the `Authorization` header of requests to this backend.

## Request/response handling

- Every service function returns the *unwrapped* data (`response.data`), not the raw axios response — callers shouldn't need to know axios's response envelope.
- Pagination: the backend returns Spring's `Page` shape (`content`, `totalElements`, `totalPages`, `number`, `size`); pass `page`/`size` query params through and return the page object as-is so list pages can drive their pagination controls from it directly.
- Don't transform/rename fields between the backend DTO and the frontend — keep field names identical (e.g. `coverageAmount`, `policyNumber`) so the API contract stays traceable end-to-end.

## Error handling

The backend returns a consistent shape (`ApiError`) on failure:

```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/policies",
  "fieldErrors": [{ "field": "endDate", "message": "must be a future date" }]
}
```

- Service functions let errors propagate (don't swallow them) — the calling page's `try/catch` decides how to display them, per the `react-development` skill.
- When `fieldErrors` is present, map it onto form field error state so each input shows its own message, instead of a single generic banner.
- When `fieldErrors` is absent, show `message` as a single inline error (see `frontend-design` skill for how).
- Never surface a raw axios error object or stack trace to the UI — always go through `error.response.data.message` (falling back to a generic "Something went wrong" only if the backend didn't return the expected shape, e.g. a network failure).

## Keep API calls out of UI components

- Components and pages depend on `src/api/*` functions, never on `axios` directly and never on a hardcoded URL.
- If a page needs data transformation beyond what the API returns (e.g. combining two calls), do that composition in the page/hook, not by adding UI-specific logic into the `api/` modules — those stay a thin, faithful mirror of the backend's actual endpoints.
