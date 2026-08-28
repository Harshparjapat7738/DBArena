# DBForge

A LeetCode-style practice platform for **databases**. The core premise: one dataset is
authored once in an engine-neutral form and materialized into PostgreSQL, MySQL and
MongoDB, so a learner can solve the same problem in SQL and again as an aggregation
pipeline against byte-identical data.

The workbench UX is modelled on JetBrains DataGrip (schema explorer, query console
bound to a session, result grid, execution log).

---

## Read before writing code

| Doc | Covers |
|---|---|
| `docs/01-system-architecture.md` | Services, stack, data model, APIs, security, layout |
| `docs/02-core-engine-design.md` | CDM, sandbox execution, evaluation, complexity metrics, AI, import/export |
| `docs/03-delivery-plan.md` | Phasing, milestones, risks, open decisions |
| `docs/04-claude-build-playbook.md` | How work is handed to you, milestone order |

**These docs are the source of truth.** If a task conflicts with them, stop and say so
rather than silently deviating. If something is underspecified, ask — do not fill the
gap with a plausible guess.

**Status as of 2026-08-28: none of the four docs above exist yet.** M01 and M14 (below)
were started anyway, on the human's explicit instruction, using only the detail already
present in the four `CLAUDE.md` files. Do not assume docs/01-04 will match what M01/M14
built — read backend/CLAUDE.md's Session Log for what was actually decided, and treat
these four docs as still needed before milestones that need CDM/API/security detail
beyond what's already in the `CLAUDE.md` files (B02 onward, most immediately).

---

## Repository layout

```
backend/    Java 21 / Spring Boot / Maven reactor  (see backend/CLAUDE.md)
frontend/   Next.js 15 + TypeScript pnpm workspace (see frontend/CLAUDE.md)
ai/         Prompts, context schemas, eval harness  (see ai/CLAUDE.md)
datasets/   Canonical Dataset Model descriptors + data
deploy/     compose (local), Helm, Terraform, K8s manifests
docs/       Architecture and planning
scripts/    Dev utilities
```

`datasets/`, `deploy/`, `docs/`, `scripts/` don't exist yet — they're created by the
milestones that need them (B02 for `datasets/`, B19 for `deploy/`, and `docs/` whenever
someone writes docs/01-04).

---

## Stack — non-negotiable

Java 21 · Spring Boot 3.3.x · Maven multi-module · MongoDB (primary metadata store) ·
PostgreSQL (ledgers, and also a practice engine) · Redis · Kafka · S3/MinIO ·
Next.js 15 + React 19 + TypeScript · Monaco · Tailwind + shadcn/ui.

Do not introduce a new framework, ORM, build tool, or state library without asking.

---

## Hard rules

These exist because breaking them causes damage that surfaces months later.

1. **`engine-spi` and `engine-adapters/*` must not depend on Spring.** They depend on
   the SPI and a driver, nothing else. ArchUnit enforces this.
2. **No service module imports another service module.** Cross-service reads go through
   OpenFeign clients; writes go through Kafka events.
3. **Never mock a database.** Adapter, repository and integration tests use Testcontainers
   against the real engine.
4. **Statement validation is AST-based.** JSqlParser for SQL, a restricted command AST for
   MongoDB. Regex-based filtering of user SQL is forbidden — it is defeated in minutes.
5. **The AI context builder has a hard-coded 10-row-per-entity cap.** It is not a config
   value, not overridable, and not adjustable by prompt. It must never receive the
   reference solution or hidden-run dataset values.
6. **Auth tokens never touch `localStorage`.** Access token in memory, refresh token in an
   `HttpOnly; Secure; SameSite=Strict` cookie.
7. **Sandbox code holds no platform credentials.** The data plane and control plane share
   no network, no secret, no store.
8. **Migrations are never edited after merge.** Write a new one.
9. **Decimals are compared as scaled integers, timestamps as UTC epoch millis.** Never as
   doubles, never with engine-local timezone. Collations are pinned (Postgres `C`,
   MySQL `utf8mb4_bin`, Mongo binary).
10. **No TODOs in merged code.** Open an issue instead.

---

## Commands

```bash
make up                 # start local infra (postgres, mongo, redis, redpanda, minio, otel)
make down
make reset              # wipe volumes and restart clean
make health             # verify every container is healthy

make backend            # mvn -T 1C verify from backend/
make frontend           # pnpm -C frontend dev
make verify             # full: backend verify + frontend lint/test

# single backend module
cd backend && mvn -pl services/execution-service -am verify
```

`make up`/`make down`/`make reset`/`make health`/`make backend`/`make frontend`/
`make verify` are not implemented yet — there is no root `Makefile` in this repo yet.
Run `mvn -T 1C verify` directly from `backend/` until one exists.

---

## Conventions

- Package root `com.dbforge.<module>`. Module names match directory names.
- Java **records** for DTOs and value objects. No Lombok.
- Constructor injection only. No field `@Autowired`.
- Flyway for PostgreSQL (`src/main/resources/db/migration`), Mongock for MongoDB.
- REST: `/api/v1`, cursor pagination, RFC 7807 `application/problem+json` errors.
- Kafka events: Avro, schema registry, backward-compatible evolution enforced in CI.
- Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `test:`).
- Branch per milestone: `m07-mongo-materializer`.

---

## Definition of done

Code + tests + updated OpenAPI (if it exposes HTTP) + `make verify` green.

Coverage gates: 80% on services. **95% on the result comparator, the statement
classifier, and the type-mapping code** — a silent bug in those three produces wrong
verdicts or a sandbox escape.

---

## Current phase

**Phase 0 — Foundations.** Two milestones have been scaffolded in `backend/` so far —
see `backend/CLAUDE.md`'s Session Log for exactly what was built and what's carried
forward on each:

- **M01** — Maven reactor + `common-*` + `engine-spi`.
- **M14** — `identity-service` + `api-gateway` (registration/login/refresh-rotation/
  logout behind a reverse-proxy gateway).

Most importantly: **`mvn -T1C verify` has not been run successfully in any environment
either session had network access to** (Maven Central was unreachable from both the
cloud sandbox and the linked device's Cowork VM, both times). Run it yourself before
trusting the build — opening the project in IntelliJ (`.idea/` is already here) will do
this automatically — and fix whatever it surfaces. M14 depends on more third-party APIs
reconstructed from memory than M01 did (see backend/CLAUDE.md's M14 entry for the two
flagged spots), so give it the closer look.

Work the remaining milestones in the order given in `docs/04-claude-build-playbook.md`
§3 once that doc exists; until then, `backend/CLAUDE.md`'s own milestone table is the
order of record. Do not start a milestone whose dependencies are not yet ✅.

Next up: close out M01's and M14's carried-forward items (`mvn verify` green, fix
whatever it surfaces on both), then either **M02 — CDM model + validator
(`dataset-cli`)** or another B01-only-dependent milestone (B13 catalog-service, B15
user-service, B07 sandbox agent, B08 statement classifier) — ask the human which, per
this file's own "ask, don't guess" rule.
</content>
