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
