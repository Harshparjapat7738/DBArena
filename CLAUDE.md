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

**Status as of 2026-08-28: none of the four docs above exist yet.** M01, M14, M13, and M02
(below) were started anyway, on the human's explicit instruction, using only the detail
already present in the four `CLAUDE.md` files. Do not assume docs/01-04 will match what
was actually built — read backend/CLAUDE.md's Session Log for what was actually decided,
and treat these four docs as still needed before milestones that need detail beyond
what's already in the `CLAUDE.md` files and what M02 itself designed (see that milestone's
Session Log entry - it's the one that had to invent the CDM shape docs/02 was meant to
define, in the absence of that doc).

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

`deploy/`, `docs/`, `scripts/` don't exist yet — they're created by the milestones that
need them (B19 for `deploy/`, `docs/` whenever someone writes docs/01-04). `datasets/`
now exists (M02) with one sample: `datasets/two-sum/`.

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

**Phase 0 — Foundations.** Four milestones have been scaffolded in `backend/` so far —
see `backend/CLAUDE.md`'s Session Log for exactly what was built and what's carried
forward on each:

- **M01** — Maven reactor + `common-*` + `engine-spi`. (B01)
- **M02** — the Canonical Dataset Model (`engine-spi`'s new `cdm` package) + its
  validator + `tools/dataset-cli` + a real sample dataset. (B02)
- **M03** — type mapping: `engine-spi`'s new `typemap` package (`TypeMapper<T>` +
  `PostgresTypeMapper`/`MongoTypeMapper`, both total switches over `CdmType`). (B03)
- **M13** — `catalog-service` (public problem browsing + admin authoring, the first
  service in the reactor to actually use MongoDB), plus a method-aware public-path
  update to api-gateway so catalog browsing is public while writes still require a
  token. (B13)
- **M14** — `identity-service` + `api-gateway` (registration/login/refresh-rotation/
  logout behind a reverse-proxy gateway). (B14)
- **M16** — `ai-assistant-service`: one endpoint, graduated non-solution-revealing hints
  (CONCEPT/APPROACH/NEAR_MISS) for a catalog problem, Groq primary / Gemini fallback,
  first real Feign use in the reactor. (B16)

**M13, M14, and M16 were all built out of the milestone table's numeric order** (each
depends only on already-done milestones, so each was picked directly rather than waiting
on the full B02–B12 chain — M16 specifically per the human's explicit "start AI
implementation now" instruction, the same kind of deliberate, explicit override M13/M14
were). Outside of an explicit override like these, **work follows the milestone table's
numeric order strictly**: the next milestone is always the lowest-numbered `🔴 not
started` row whose dependencies are already ✅/🟡, and a session never re-does a
milestone that's already 🟡 partial or ✅ complete — check backend/CLAUDE.md's table
first, every time, before starting anything. Right now that means **B04 — Postgres
materializer + introspection** is next; B13/B14/B16 stay done and are not revisited until
their own carried-forward items call for it.

Most importantly: **`mvn -T1C verify` has not been run successfully in any environment
any of these sessions had network access to** (Maven Central was unreachable from both
the cloud sandbox and the linked device's Cowork VM, every time). Run it yourself before
trusting the build — opening the project in IntelliJ (`.idea/` is already here) will do
this automatically — and fix whatever it surfaces. M02's `engine-spi` additions are the
one exception: they compiled clean under plain `javac --release 21` this session (same
zero-external-jars trick M01 used) and a real bug was caught that way before it shipped
(see backend/CLAUDE.md's M02 entry). M03's `typemap` package (also zero external dependencies) compiled clean the same way, with
no bugs caught this time - the two mappers are small total switches with no branching logic
to get wrong. Everything else - tools/dataset-cli's Jackson-based code, all of M13/M14, and
now M16 - is hand-reviewed only, not compiler-verified; give catalog-service's Mongock
package and M16's Feign/HTTP-provider code the closest look first among those (each flagged
in its own milestone's Session Log Tests section).

Work the remaining milestones in the order given in `docs/04-claude-build-playbook.md`
§3 once that doc exists; until then, `backend/CLAUDE.md`'s own milestone table is the
order of record, and (per the instruction above) that order is now followed strictly.

Next up: **B04 — Postgres materializer + introspection**.
