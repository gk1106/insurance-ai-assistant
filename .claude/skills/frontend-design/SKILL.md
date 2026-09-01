---
name: frontend-design
description: UI/UX guidelines for the Insurance AI Assistant React dashboard — layout, navigation, cards/tables/forms, loading/error/empty states, and accessibility. Use when designing, building, or reviewing the visual structure of any frontend screen, page, or component.
---

# Frontend Design — Insurance AI Assistant

Guidelines for a professional, no-frills insurance dashboard. This is a learning project — favor a small set of consistent, reusable patterns over a large design system. If a pattern isn't listed here, keep it simple rather than inventing something new.

## Layout

- Standard app shell: fixed **sidebar** (navigation) + **topbar** (page title, user menu, logout) + **content area**.
- Sidebar sections match the domains: Dashboard, Customers, Policies, Claims, Renewals. Show/hide items by role (e.g. a CUSTOMER-role user doesn't see a "Customers" admin list).
- Content area uses a single max-width container with consistent page padding — don't let tables/forms stretch edge-to-edge on wide screens.
- **Responsive breakpoints**: collapse the sidebar into a toggleable drawer below ~768px; stack multi-column forms/cards into a single column below ~640px. Desktop-first is fine since this is an internal/agent-facing dashboard, but every screen must remain usable on a laptop-width window at minimum.

## Navigation

- Sidebar items show an active state for the current route.
- Breadcrumbs or a page title + short description at the top of each page so the user always knows where they are (e.g. "Policies / POL-2026-ABC123").
- Primary actions (e.g. "New Policy", "File Claim") live top-right of the page header, not buried in the sidebar.

## Core components

Build a small, reusable set — don't create a one-off variant per page:

- **Card** — bordered container with optional header/title, used for grouped info (customer summary, policy summary).
- **Table** — for lists (customers, policies, claims, renewals). Support empty state, loading state, and pagination controls matching the backend's `Page` response (page/size/totalPages).
- **Form** — labeled inputs with inline validation error text under each field (map directly to the backend's `fieldErrors` array — see the `api-integration` skill).
- **StatusBadge** — small colored pill for domain statuses. Use a consistent color mapping and reuse it everywhere a status appears (table cell, detail page, card):
  - Policy: `ACTIVE`/`CONFIRMED`/`PAID`/`APPROVED` → green; `PENDING`/`UNDER_REVIEW`/`SUBMITTED`/`DRAFT` → amber/neutral; `EXPIRED`/`LAPSED` → gray; `CANCELLED`/`REJECTED` → red.
  - Don't invent a new color per status name — map to this small green/amber/gray/red set.

## Loading, error, and empty states

Every data-fetching screen must explicitly handle all three — never leave a blank page while data is missing:

- **Loading**: a simple spinner or skeleton placeholder shaped like the eventual content (a few gray bars for a table, a gray card outline for a detail card). Don't block the whole page for a small partial refresh.
- **Error**: a visible inline message (not a silent console log) with the backend's `message` field when available, plus a retry action where it makes sense (e.g. "Failed to load policies. Retry"). Never show a raw stack trace or JSON blob to the user.
- **Empty**: a short message + optional call-to-action, distinct from the error state (e.g. "No policies yet — create one" vs "Failed to load policies").

## Accessibility

- Use semantic HTML: `<nav>`, `<main>`, `<table>`, `<button>`, `<form>`, real `<label htmlFor>` on every input — not `<div onClick>` for interactive elements.
- All interactive elements must be keyboard-reachable and show a visible focus outline (don't remove `:focus` styles without replacing them).
- Color is never the only signal for status — pair the `StatusBadge` color with the status text itself (already the case if you always render the label inside the badge).
- Sufficient contrast for text on colored backgrounds (badges, buttons) — check against WCAG AA, not just "looks fine."
- Async state changes (form submit success/failure, data loaded) should be announced via visible text, not just a toast that disappears instantly.

## Consistency

- One spacing scale, one type scale, one color palette — defined once (CSS variables or a single constants file) and reused, not re-picked per page.
- Reuse the Card/Table/Form/StatusBadge components everywhere the same kind of data appears rather than writing bespoke markup per page.
- Match backend vocabulary in the UI: field names, status values, and domain terms (Policy, Claim, Renewal, Coverage Amount, Premium) should read the same in the UI as in the API — don't invent new terminology.

## Don't over-engineer

- No heavyweight UI/component library or design token system unless the project already depends on one — the current frontend has no UI library installed, so start with plain CSS (or CSS Modules) and the component set above before reaching for anything larger.
- No dark mode, animation system, or theming layer unless explicitly requested.
- No custom design system documentation site, Storybook, or Figma-to-code pipeline for a learning project — a handful of well-named, reused components is enough.
