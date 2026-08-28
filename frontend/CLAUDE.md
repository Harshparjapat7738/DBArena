# frontend/ — Next.js 15 · React 19 · TypeScript

pnpm workspace. `apps/web` is the product; `packages/*` are shared libraries.

## Layout

```
apps/web/src/
  app/                 App Router. (marketing) SSG, (app) authed, s/[shareId] public SSR
  features/
    workbench/         SchemaExplorer, QueryConsole, ResultGrid, ExecutionLog, SessionProvider
    submissions/       verdict panel, plan analysis, percentiles
    ai-assistant/      hint ladder, SSE stream
    catalog/  erd/  import-export/  leaderboard/
  lib/                 api client wiring, hooks, utils
  styles/
packages/
  ui/                  design system (shadcn-derived, owned not vendored)
  editor/              Monaco dialect tokenizers, completion providers, Mongo typings gen
  api-client/          typed client generated from the gateway OpenAPI
  config/              shared tsconfig, eslint, tailwind preset
```

## Rules

- Server Components by default. `"use client"` only where interactivity requires it —
  the workbench is a client island, the catalogue is not.
- **No `localStorage` for auth.** Access token in memory (React context), refresh token in
  an HttpOnly cookie handled by the BFF route handlers.
- Server state: TanStack Query. Client state: Zustand. Do not add Redux.
- Styling: Tailwind + `packages/ui`. No inline style objects, no CSS-in-JS runtime.
- Result grid must be virtualized (TanStack Virtual). It has to survive 100k rows.
- Streaming (AI, submission progress) uses **SSE**, not WebSocket. WebSocket is reserved
  for contests in Phase 4.
- Forms: react-hook-form + Zod. Zod schemas are shared with the API contract.
- Accessibility is a gate, not a polish item: full keyboard operation of the workbench,
  ARIA grid semantics on results, WCAG 2.1 AA contrast in both themes.

## Session lifecycle (the fiddly bit)

`SessionProvider` acquires a sandbox lease on mount, heartbeats to renew, releases on
unmount and `beforeunload`, and must recover gracefully when a lease is lost mid-session:
re-acquire, replay the console buffer, and warn the user that writable state was reset.
Write tests for the lease-loss path before the happy path.

## Commands

```bash
pnpm install
pnpm dev            # apps/web on :3000
pnpm test           # vitest
pnpm e2e            # playwright
pnpm storybook
pnpm lint && pnpm typecheck
```

## Status (as of 2026-08-28)

Built and verified (`pnpm install && pnpm lint && pnpm typecheck && pnpm test && pnpm build`
all pass clean in-repo):

- Workspace scaffold: `apps/web` + `packages/{config,ui,api-client}`.
- Auth: `/login`, `/register` (react-hook-form + Zod, mirrors identity-service's bean
  validation exactly), in-memory-only access token (Zustand), silent `/auth/refresh` on
  load via `AuthProvider`, and a same-origin BFF proxy (`app/api/proxy/[...path]`) that
  forwards to api-gateway and relays `Set-Cookie` — this is the sanctioned workaround for
  api-gateway still having no CORS config (carried forward from M14).
- Authed shell (`(app)/layout.tsx` → `AppShell`): sidebar nav, logout, redirect-to-login
  gated on `status === "anonymous"` only.
- Catalog: server-rendered list page (`(app)/catalog`) with URL-param-driven filters
  (search/difficulty/engine/tag) and a client-island "Load more" cursor pager; problem
  detail page (`(app)/catalog/[slug]`).
- AI assistant: `features/ai-assistant/HintPanel.tsx` on the detail page — learner pastes
  their query + optional error/result text, picks a hint tier, calls
  `ai-assistant-service` (M16/B16) through the proxy. **Paste-in, not SSE**: there is no
  execution service yet, so this is a plain request/response call, not the streaming
  ladder the Layout/Rules sections above describe as the target shape.
- **Settings / theme system** (`(app)/settings` → `<ThemeSwitcher/>` from `packages/ui`):
  17 themes (11 VS Code built-ins + 6 community picks — Dracula, One Dark Pro, Nord,
  GitHub Dark/Light, Night Owl), runtime CSS-variable engine (`--db-*` vars + Tailwind v4
  `@theme` mapping), pre-hydration inline script to avoid a flash of the wrong theme,
  System/Light/Dark quick toggle plus a full swatch gallery with live per-theme mockups.
  `localStorage` is used here deliberately for this one UI preference — distinct from the
  forbidden auth-token use above.
- Design system primitives: `Button`, `Card`, `Badge`, `Input`, `Skeleton` (cva-based).
- Tests: theme store + api-client error-mapping (vitest + jsdom), 9 passing.
- Route-level error/loading/empty states: root `error.tsx` (retry) and `not-found.tsx`
  (on-brand 404), plus `loading.tsx` skeletons for the two server-rendered catalog routes -
  added in the same audit pass as the backend fixes below, closing a gap where those routes
  showed a blank screen (or the framework default) during their server-side fetch/on an
  unhandled error.

Not yet built: the workbench (`SchemaExplorer`/`QueryConsole`/`ResultGrid`/
`ExecutionLog`/`SessionProvider`), submissions, ERD, import/export, leaderboard,
`s/[shareId]` public share route, Storybook, Playwright e2e, Monaco editor package.

### Documented deviations from the sections above

- `packages/api-client` is **hand-written**, not generated from a gateway OpenAPI spec —
  no service in the reactor emits a consolidated spec yet. DTOs were verified field-for-
  field against the real backend Java records (identity-service, catalog-service,
  common-core's `CursorPage<T>`), not guessed.
- `typescript` is pinned to **6.0.3**, not the npm `latest` (7.0.2). `typescript-eslint`
  8.68.0 hard-rejects TS 7.0 at load time (open upstream issue) — pin down, don't fight
  the toolchain, and revisit once typescript-eslint ships TS 7 support.
- `eslint-config-next`'s bundled presets go through `@rushstack/eslint-patch` for legacy
  config compatibility, which throws under ESLint 10 (targets ESLint ≤9). Replaced with
  an equivalent flat config (`apps/web/eslint.config.mjs`) built directly from
  `typescript-eslint`, `eslint-plugin-react-hooks`, and `@next/eslint-plugin-next` — the
  same rule sets `eslint-config-next` re-exports, just without the broken shim.
