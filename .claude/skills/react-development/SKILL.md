---
name: react-development
description: React + Vite conventions for the Insurance AI Assistant frontend — component/page structure, hooks, React Router, and state/error handling. Use when writing or reviewing any React component, page, hook, or route.
---

# React Development — Insurance AI Assistant

The frontend lives at `frontend/InsuranceAi-frontend` (Vite + React 19, linted with `oxlint` via `npm run lint` — not ESLint). Keep patterns simple and consistent; this is a learning project, not a place to demonstrate every React technique available.

## Project structure

```
src/
├── main.jsx            # entry point, router + providers
├── App.jsx              # top-level layout (sidebar/topbar) + <Outlet />
├── api/                  # axios service layer — see api-integration skill
├── components/           # small, reusable, presentation-focused pieces
│   ├── layout/            # Sidebar, Topbar, PageHeader
│   └── ui/                 # Card, Table, StatusBadge, FormField, etc.
├── pages/                # one file/folder per route (CustomersPage, PolicyDetailPage, ...)
├── hooks/                # reusable custom hooks (useAuth, useFetch, ...)
├── context/              # React Context providers (auth/session state)
└── routes/               # route definitions (or inline in main.jsx if small)
```

- **Pages** own data fetching and page-level state; they compose **components**.
- **Components** are presentation-focused and receive data via props — they don't call the API directly.
- Only split a component out of a page once it's reused or the page file gets unwieldy — don't pre-split every page into a dozen tiny files on day one.

## Components and hooks

- Function components only, with hooks — no class components.
- One component per file, file name matches the component name (`PolicyCard.jsx` exports `PolicyCard`).
- Keep props minimal and explicit; prefer a few well-named props over passing a whole raw API object down when only three fields are used.
- Custom hooks encapsulate reusable stateful logic (`useAuth()`, `usePagination()`) — extract one once a pattern repeats, not preemptively.
- Don't reach for `useMemo`/`useCallback` by default; only add them when there's an actual measured or obvious performance problem (e.g. an expensive computation or a dependency of a `useEffect` that would otherwise loop).

## State management

- Local component state (`useState`) is the default for page/form state.
- `useReducer` only when a page has several related pieces of state that change together (e.g. a multi-field form with interdependent validation) — don't reach for it for a single boolean.
- Shared state (current user, JWT, role) lives in one `AuthContext` via `useContext` — don't introduce Redux, Zustand, or another state library for this project's scope.
- Server data (lists of policies, claims, etc.) is fetched per-page and kept in that page's local state — no client-side cache/store layer unless the app grows enough to need one.

## React Router

- `react-router-dom` v7 is the routing library (already a dependency — see note below on where it's installed).
- Define routes centrally (in `main.jsx` or `routes/index.jsx`), not scattered across components.
- Protect authenticated routes with a small `RequireAuth` wrapper component that checks `AuthContext` and redirects to `/login` if there's no valid session — don't duplicate that check inside every page.
- Role-gated routes (e.g. admin-only Customers list) follow the same wrapper pattern with an added role check, mirroring the backend's ADMIN/AGENT/CUSTOMER authorization — the frontend check is for UX only; the backend remains the actual authority (see `CLAUDE.md` security rules).

## Data fetching, state, and error handling

- Fetch data in a page-level `useEffect` (or a small `useFetch`-style hook if the pattern repeats across pages), always tracking three states: `loading`, `error`, `data` — never assume a fetch succeeded.
- Every async handler (`onSubmit`, button actions) wraps its API call in `try/catch`, sets a loading flag while in flight, and surfaces a caught error to the UI (see `frontend-design` skill for how to render it) — never let a rejected promise fail silently.
- Don't put API call logic inside components/pages directly — always go through the service layer described in the `api-integration` skill. Pages call `policyApi.getById(id)`, not `axios.get(...)` inline.
- Keep forms simple: controlled inputs with `useState`, client-side validation only for obvious cases (required fields, number/date format) — the backend remains the source of truth for business-rule validation, and its `fieldErrors` should be mapped back onto the form (see `api-integration`).

## Project setup note

`axios` and `react-router-dom` are currently listed as dependencies in `frontend/package.json` (an outer folder), **not** in `frontend/InsuranceAi-frontend/package.json`, which is the actual Vite app. Before writing code that imports either package, confirm they're installed in `frontend/InsuranceAi-frontend` (`npm install axios react-router-dom` from that directory) rather than assuming they're already available there.

## General principles

- Prefer the simplest pattern that works; add abstraction (custom hooks, context, extra components) only once a concrete duplication or complexity problem appears.
- Match backend naming (field names, statuses, endpoints) rather than renaming things on the frontend for style reasons.
- Run `npm run lint` (oxlint) before considering a change done.
