# DBArena

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

- Package root `com.DBArena.<module>`. Module names match directory names.
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
- **B04** — Postgres materializer + introspection (`engine-adapters/adapter-postgres`),
  done in the table's own numeric order. See backend/CLAUDE.md for two further, un-numbered
  audit/fix passes made after B04 too (common-web extraction, EngineType/EngineKind
  reconciliation, AI hint rate limiting, frontend error/loading states).
- **B20 — MySQL adapter + type mapping** (`engine-adapters/adapter-mysql` +
  `engine-spi`'s `MySqlTypeMapper`) — not a real table row originally (B01-B19 never had
  a MySQL-adapter milestone at all, despite this file's own stack line naming MySQL as a
  target engine); added to the table and built on the human's explicit instruction, the
  same kind of deliberate override M13/M14/M16 were. First milestone with a real,
  network-verified `mvn compile`/`test` pass rather than hand-review-only — see its own
  Session Log entry for what that did and didn't cover.

**M13, M14, M16, and B20 were all built out of the milestone table's numeric order** (M13/
M14/M16 depend only on already-done milestones, so each was picked directly rather than
waiting on the full B02–B12 chain; B20 didn't exist in the table at all until this session
added it). Outside of an explicit override like these, **work follows the milestone
table's numeric order strictly**: the next milestone is always the lowest-numbered `🔴 not
started` row whose dependencies are already ✅/🟡, and a session never re-does a
milestone that's already 🟡 partial or ✅ complete — check backend/CLAUDE.md's table
first, every time, before starting anything. Right now that means **B05 — MongoDB
materializer + document shaping** is next; B04/B13/B14/B16/B20 stay done and are not
revisited until their own carried-forward items call for it.

For M01 through M16, `mvn -T1C verify` had not been run successfully in any environment
any of those sessions had network access to (Maven Central was unreachable from both the
cloud sandbox and the linked device's Cowork VM, every time) — M02's `engine-spi`
additions and M03's `typemap` package were the only exceptions, compiled clean under plain
`javac --release 21` (zero external dependencies); everything else from that era -
tools/dataset-cli's Jackson-based code, all of M13/M14/M16, B04 - was hand-reviewed only.

**That changed in the B20 (MySQL adapter) session**: this sandbox's `.m2` was already
populated and Maven Central was genuinely reachable, so `mvn compile`/`test` actually ran
(after fixing a real, previously-undiscovered missing-version bug in
`platform-common/common-security`'s `nimbus-jose-jwt` dependency that had silently blocked
*any* reactor-wide Maven read since M01 - see B20's Session Log entry). Still not fully
verified even now: Testcontainers-based integration tests (adapter-postgres's and
adapter-mysql's alike) fail in this specific environment for reasons B20 diagnosed but did
not fix (a JaCoCo/JDK-26 instrumentation crash, worked around per-run, and a deeper
Testcontainers-Java-vs-Windows-npipe connectivity gap that could not be resolved this
session) - fixing that gap is now the single highest-value infrastructure task open in
this repo, since it blocks hard rule #3 verification project-wide, not just for one
milestone. If you have network access, always prefer trying a real `mvn -T1C verify` over
assuming it still can't be done - re-verify the assumption every session rather than
carrying it forward as settled fact.

Work the remaining milestones in the order given in `docs/04-claude-build-playbook.md`
§3 once that doc exists; until then, `backend/CLAUDE.md`'s own milestone table is the
order of record, and (per the instruction above) that order is now followed strictly.

Next up: **B05 — MongoDB materializer + document shaping**.

---

## Backend Progress

### B01 Backend Audit + API Contract
Status: COMPLETE
Completed: 2026-08-29

Note on numbering: this "B01" is a fresh task-tracking sequence for this audit/planning
line of work, unrelated to the pre-existing `backend/CLAUDE.md` milestone-table row also
labeled B01 ("Maven reactor + common-* + engine-spi", 🟡 partial). That table and its
Session Log remain the source of truth for build milestones; this section tracks the
newer audit-and-roadmap task sequence layered on top of it.

Implemented:
- Read-only audit of all 4 existing backend services against the actual, currently-built
  frontend surface — including the many `apps/web` route/feature directories that are
  untracked in git and not yet reflected in `frontend/CLAUDE.md` (which is now stale).
- Every finding below was cross-checked directly against source (controller annotations,
  gateway route/public-path config, `services/pom.xml` module list, the frontend's
  `packages/api-client` types and its `lib/mock/*` repository layer) rather than taken
  from docs.

Changed:
- No implementation files changed — audit only, as scoped.

APIs:
- **Reusable as-is** (verified field-for-field against `packages/api-client`, no
  mismatches): identity-service `/api/v1/auth/{register,login,refresh,logout,me}`;
  catalog-service `/api/v1/catalog/{problems,problems/{slug},tags}` (public GET) +
  `/api/v1/catalog/problems` POST/PUT/`/publish`/`/unpublish` (admin); ai-assistant-service
  `/api/v1/ai/problems/{slug}/hint` (one action: hint only).
- **Missing entirely** — zero backend endpoint, frontend currently backed by
  `apps/web/src/lib/mock/*` + localStorage: user progress/XP/streak/mastery/badges/
  activity, daily-challenge, leaderboard, bookmarks, submissions + grading, playground
  query execution, dataset-content browsing (schema/sample rows — distinct from
  catalog-service's problem metadata), learning paths/lessons, and 5 of 6 ai-assistant
  actions (guide/explain/debug/solution/optimize — only "hint" is real).
- **Incomplete** (pre-existing gaps, flagged in the code itself, not new): catalog-service
  has no admin "preview own unpublished draft" endpoint; ai-assistant-service's
  `HintRateLimiter` is in-memory/single-instance only.
- **Duplicate APIs:** none found — every capability has exactly one owning controller.
- **Wrong ownership:** none in what exists today. Open question for later, not decided
  here: which service should own the new dataset-content-browsing API (catalog-service,
  since it already owns the problem→datasetSlug link, vs. a new small content service).

Database:
- No schema changes made. Confirmed gaps: no store anywhere for progress/XP, streaks,
  badges, activity log, daily-challenge state, leaderboard rankings, bookmarks,
  submissions/grading records, or playground history. `datasets/` (M02) is filesystem-only
  today, read only by `dataset-cli` and ai-assistant-service's `DatasetContextLoader` — no
  REST surface exposes dataset content.

Verification:
- Endpoint lists and DTO field names cross-checked directly against `ProblemController`,
  `AuthController`, `HintController`, `PublicPaths`, `ReverseProxyController`/
  `application.yml` route config, and `packages/api-client/src/{auth,catalog,ai}.ts` +
  `types.ts`.
- Confirmed via `backend/services/pom.xml` and a filesystem listing: no `execution-service`
  module exists yet.
- Confirmed `engine-spi` + `adapter-postgres`/`adapter-mysql` provide a complete,
  Spring-free `DatabaseEngineAdapter` contract usable by a future execution-service with no
  changes, and that `common-security`'s JWT/`@CurrentUser`/`@RequiresRole` is drop-in
  reusable by any new service — neither is wired to anything yet.

Known limitations:
- Planning artifact only — no code written. The execution-service dependency chain
  (B05→B07→B08→B09→B10→B11→B17) is unchanged from the existing milestone table; this
  audit confirmed it rather than reordering it.
- `frontend/CLAUDE.md` documents far less than what actually exists under
  `apps/web/src/app/(app)/` and `apps/web/src/features/` — noted here, not fixed, since
  it's outside a backend task's scope.

Next task:
- **B05 — MongoDB materializer + document shaping** — unchanged as the correct next step;
  nothing in this audit reorders it, since it's the first unblock in the
  execution-service chain that most of the new frontend surface ultimately depends on.
- Separately proposed (needs human sign-off before starting, since it means adding rows to
  `backend/CLAUDE.md`'s milestone table out of number order the same way M13/M14/M16/B20
  were): a dataset-content-browsing read API, a bookmarks store, and learning-paths
  content are all independent of execution-service and could be pulled forward for
  frontend-visible backend progress while B05→B11 is still in flight.

### B02 Backend Domain/Data Model Foundation
Status: COMPLETE
Completed: 2026-08-29

Same numbering note as B01: this "B02" is this audit/build task sequence, not
`backend/CLAUDE.md`'s own milestone-table B02 (CDM model, already ✅). Scope was
explicitly data-model only, per the task brief - no execution engine, no AI, no
controllers/service-layer business logic, no frontend changes.

Implemented:
- **catalog-service (extended, in place):** `Problem` gained a `version` int field
  (back-compat 11-arg constructor defaults it to 1, so every pre-existing call site
  compiles unchanged; `withRevisedContent(...)` increments it on update; the mapper
  defaults a missing `version` key to 1 for documents written before this change).
  Three brand-new domains added alongside it, each with domain record(s) + Mongo
  repository/mapper + cursor pagination + DTOs + a Mongock changelog: **Topic**
  (canonical tag/topic registry), **DatasetMetadata** (a summary read-model over a CDM
  dataset - schema/sample-row detail is explicitly out of scope, flagged in its own
  Javadoc), **LearningPath**+**Lesson** (lessons embedded in their path, never a child
  collection - a lesson has no independent lifecycle).
- **3 new Spring Boot modules** (data-model foundation only - domain + Mongo repository/
  mapper + DTOs + Mongock indexes; deliberately no controllers or business-logic service
  layer yet, consistent with "data-model foundation" scope):
  - **user-service** (B15, port 8085): `Bookmark` (userId + problemSlug, unique pair).
  - **gamification-service** (B17, port 8086): `UserProgress` (one doc per user, keyed
    on userId itself, not a generated id - see its Javadoc), embedded `SkillMastery`
    list, `ActivityItem` (own collection, unbounded feed), `BadgeDefinition`+`UserBadge`,
    `DailyChallenge`+`DailyChallengeCompletion`, `LeaderboardEntry` (materialized
    snapshot rows, scope+periodKey+rank indexed - not computed live).
  - **submission-service** (B11, port 8087): `Submission` only - ULID id (time-ordered,
    shard-friendly), every result field `Optional` (inserted `PENDING`, nothing grades
    it yet), `queryText` size-capped, no raw result sets ever stored here. No execution
    or grading logic - that's B09/B10/B11's own future work.
- Every new/changed collection's `userId` (or equivalent) is `TypedId<AuthenticatedUser>`
  - `common-security`'s existing cross-service user-reference type - never a copy of
    identity-service's email/displayName/password. This is what satisfies "no
    duplicated user identity data" throughout.
- Fixed one unrelated pre-existing compile error blocking all reactor test-compilation
  (`common-security`'s `RoleAuthorizationAspectTest` didn't declare/catch the checked
  `Throwable` `ProceedingJoinPoint.proceed()` throws) - needed to verify anything at all
  reactor-wide; otherwise untouched, per "do not modify unrelated files."

Changed modules/files:
- `backend/services/catalog-service`: `Problem`/`ProblemDocumentMapper`/`CatalogService`
  edited; new `domain/{topic,dataset,learning}`, `repository/{topic,dataset,learning}`,
  `web/dto/{topic,dataset,learning}`, `mongock/ChangeLog00{2,3,4}...`, `MongoConfig`
  extended with 3 collection beans.
- New modules: `backend/services/{user-service,gamification-service,submission-service}`
  (registered in `backend/services/pom.xml`).
- `backend/platform-common/common-security/.../RoleAuthorizationAspectTest.java`
  (unrelated one-line fix, see above).

Database/schema:
- catalog-service Mongo: `problems` gains a `version` field (no migration needed - reads
  default missing values to 1); new collections `topics`, `dataset_metadata`,
  `learning_paths`, each with a unique-slug index + `(createdAt, _id)` pagination index.
- user-service Mongo (new DB `DBArena_user`): `bookmarks`, unique `(userId, problemSlug)`.
- gamification-service Mongo (new DB `DBArena_gamification`): `user_progress` (no extra
  index - `_id` is the userId), `activity_log` (`userId, occurredAt desc, _id desc`),
  `badge_definitions` (unique slug), `user_badges` (unique `(userId, badgeSlug)`),
  `daily_challenges` (unique date), `daily_challenge_completions` (unique
  `(userId, date)`), `leaderboard_entries` (`(scope, periodKey, rank)` +
  unique `(scope, periodKey, userId)`).
- submission-service Mongo (new DB `DBArena_submission`): `submissions`, no unique index
  (a user submits the same problem many times) - compound `(userId, submittedAt desc,
  _id desc)` and `(userId, problemSlug, submittedAt desc, _id desc)`.

Tests:
- `mvn install -DskipTests` of the full dependency chain (`-pl <module> -am`) plus a
  per-module `mvn test -Djacoco.skip=true` (JaCoCo still can't instrument under this
  JDK - pre-existing, unrelated to this task) is how this was actually verified, since a
  reactor-wide `mvn test` still hits two pre-existing, unrelated environment gaps: no
  Docker for Testcontainers, and a separate `common-events` compile error (Spring Kafka
  API mismatch, nothing in its dependency chain touches any module this task changed -
  confirmed by grep - not fixed, out of scope).
- catalog-service: 15/15 unit tests green (existing suite + `version` round-trip/default/
  increment coverage + 3 new mapper round-trip tests for Topic/DatasetMetadata/
  LearningPath). Testcontainers-backed `MongoProblemRepositoryTest` and
  `ProblemControllerIntegrationTest` excluded from this run (pre-existing Docker gap).
- user-service: 1/1. gamification-service: 5/5. submission-service: 2/2. All mapper
  round-trip tests - no Testcontainers tests exist yet for the new modules (no Docker in
  this environment to write them against, same standing limitation).

Known limitations:
- No controllers or service-layer business logic for any of the 4 new domains + 3 new
  modules - by design, per this task's scope. Wiring them up (validation-driven
  create/list endpoints, RBAC, gateway routes) is follow-up work for whichever milestone
  actually owns each domain's feature (B15/B17/B11, or a new dataset/learning milestone
  per B01's proposal).
- No Testcontainers-backed repository tests for the 3 new modules - this environment has
  no Docker, the same gap that already blocks it for catalog-service/identity-service.
- Problem/DatasetMetadata/LearningPath versioning is a bare counter, not a version-history
  collection - a prior version's content is not retained anywhere.
- `common-events` still fails to compile reactor-wide (pre-existing, unrelated, not
  touched) - `mvn -T1C compile` from `backend/` will still fail on that module; every
  module this task touched was verified with targeted `-pl ... -am` builds instead.

Next task:
- **B03** — wire real read/write endpoints (controllers + minimal service layer) onto
  whichever of these 7 domains the human wants exposed first, OR continue straight down
  the existing execution chain (B05 MongoDB materializer next, unchanged). Needs a
  decision, not a default guess, since it changes what B03 actually is.

### Current Architecture State
- 7 services now exist: identity-service (8081), api-gateway (8080), catalog-service
  (8083), ai-assistant-service (8084), user-service (8085), gamification-service (8086),
  submission-service (8087). The 3 new ones are Mongo-backed, data-model-only (no
  controllers), and not yet wired into api-gateway's routes.
- `engine-spi` + `adapter-postgres` + `adapter-mysql` are complete but unconsumed by any
  service; no Mongo adapter exists yet (B05).
- Frontend has ~10 new route areas (dashboard, progress, daily-challenge, leaderboard,
  bookmarks, submissions, playground, datasets, learning, profile) that are 100%
  frontend-local mock data - B02 built the backend persistence shapes most of these will
  eventually need, but nothing serves them yet.

### B03 Problems + Dataset APIs
Status: COMPLETE
Completed: 2026-08-29

Implemented:
- **catalog-service, `/api/v1/problems`** (new `ProblemsController`, reuses
  `CatalogService`/`ProblemRepository`/`ProblemDetailResponse`/`ProblemSummaryResponse`
  from the existing `/api/v1/catalog/problems` - no logic duplicated): list (filters
  `q`/`difficulty`/`engine`/`topic`/`dataset`/`status`/`bookmarked`, sort
  `recommended|newest|difficulty|completion`, cursor pagination), `/{slug}`, `/{slug}/related`
  (`RelatedProblemsRanker`, a pure unit-tested scorer mirroring the frontend mock's
  heuristic exactly), `/{slug}/schema` and `/{slug}/sample-data` (both delegate to the
  problem's dataset via the new `DatasetService`).
- **catalog-service, `/api/v1/datasets`** (new `DatasetController`/`DatasetService`):
  list (filters `category`/`engine`/`q`, pagination), `/{slug}` (full detail incl. sample
  rows), `/{slug}/schema` (no sample rows), `/{slug}/sample-data`, `/{slug}/engines`.
  `DatasetMetadata` gained real schema/sample-row detail (`DatasetEntity`/`DatasetColumn`/
  `DatasetRelationship`, embedded, back-compat 11-arg constructor over B02's shape) -
  closing the gap B02 explicitly deferred. `problemCount` is computed live from
  `ProblemRepository`, never denormalized/stored.
- **Cross-service `bookmarked`/`status` filters** (hard rule #2 - OpenFeign, never a
  module dependency): added minimal internal, gateway-unreachable endpoints -
  user-service's `GET /internal/v1/users/{userId}/bookmarked-slugs` and
  submission-service's `GET /internal/v1/users/{userId}/problem-statuses` (a real Mongo
  aggregation, always empty today since nothing populates submissions pre-B09/B11, not a
  stub) - called by catalog-service's new `UserServiceClient`/`SubmissionServiceClient`.
  Requesting either filter while unauthenticated 401s via the existing
  `CurrentUserArgumentResolver.UnauthenticatedException` rather than being silently
  ignored.
- `ProblemFilter` extended (`datasetSlug`, `slugIn`, `slugNotIn` - back-compat 5-arg
  constructor preserved) so cross-service filters compose as ordinary Mongo
  `$in`/`$nin` clauses, same as any other filter axis.
- `ProblemSort` (`OLDEST_FIRST` unchanged for `/api/v1/catalog/problems`,
  `NEWEST_FIRST`, `DIFFICULTY_THEN_NEWEST` via an aggregation `$addFields`/`$indexOfArray`
  rank + 3-part cursor) added alongside the original 2-arg `findPage` (now a `default`
  method delegating to a new 3-arg one, so no existing call site changed).
- api-gateway: routes + `PublicPaths` GET-only entries for `/api/v1/problems` and
  `/api/v1/datasets` (same "convenience, not the boundary" posture as catalog's own
  routes - the 401 on filtered requests is enforced by catalog-service itself).
- Fixed one unrelated pre-existing bug discovered while getting a clean test run
  (`ReverseProxyController`'s `@RequestMapping` used `HttpMethod[]` where Spring wants
  `RequestMethod[]`) - already present, uncommitted, from a prior session; left as-is
  rather than re-fixing since it was already fixed, noted only for traceability.

Changed/added files: `catalog-service` (`ProblemFilter`, `ProblemSort` (new),
`MongoProblemRepository`, `ProblemRepository`, `CatalogService`, `Problem` domain
untouched, `DatasetMetadata`+3 new domain types, `DatasetMetadataDocumentMapper`,
`DatasetMetadataRepository`+Mongo impl, `DatasetFilter` (new), `DatasetService`+
`DatasetNotFoundException` (new), `RelatedProblemsRanker` (new), `ProblemsController`+
`DatasetController` (new), 8 new dataset DTOs, `UserServiceClient`+`SubmissionServiceClient`
(new Feign clients), `CatalogProperties`/`application.yml` (2 new URIs), `ChangeLog005...`
(new index), pom.xml (+openfeign)); `user-service` (`BookmarkRepository`+Mongo impl
extended, `BookmarkQueryController` (new)); `submission-service` (`SubmissionRepository`+
Mongo impl extended, `SubmissionStatusController` (new)); `api-gateway` (`PublicPaths`,
`application.yml` routes).

Database/schema: new index `problems.datasetSlug` (ChangeLog005). No other index changes
- B02's existing indexes already cover the rest of B03's query patterns.

Tests: catalog-service 21/21 (added: `RelatedProblemsRankerTest` (3), `CatalogServiceTest`
+1 sort-aware-overload case, `DatasetMetadataDocumentMapperTest` +2 (entities round-trip,
pre-B03-document defaulting)); user-service 1/1, submission-service 2/2 (unchanged - no
new mapper logic needed for the status aggregation, it reuses the existing
`SubmissionDocumentMapper` constants); api-gateway 11/12 (`PublicPathsTest` +1 case, all
passing) - the 1 failure (`ReverseProxyIntegrationTest.unroutedPathIsNotFound`, expects
404, gets 401) is pre-existing and unrelated to B03 (root cause: `GatewayAccessFilter`
runs before route-existence is known, for a path this session didn't touch), not fixed
here as it's outside this task's scope. `mvn install -DskipTests` of the full dependency
chain then `mvn test` per module (Testcontainers-backed tests excluded, same standing
Docker-less-environment limitation as every prior session) is how this was verified,
since whole-reactor `mvn compile` still fails on B02's already-flagged, unrelated,
untouched `common-events` module.

Known limitations:
- `sort=completion` has no real data source (needs submission volume) and silently
  behaves like `newest`; `sort=recommended` does a page-local unsolved-first reorder only
  (not a global DB-level sort) since "solved" status lives in a different service's data,
  not an indexed field here - both are documented in `ProblemSort`'s and
  `ProblemsController`'s own Javadoc, not just here.
- Bad `difficulty`/`engine` query values 500 instead of 400 (Spring's
  `MethodArgumentTypeMismatchException` isn't handled by `GlobalProblemExceptionHandler`)
  - a pre-existing gap shared with `/api/v1/catalog/problems` since M13, not introduced or
    fixed by B03.
- No datasets exist anywhere yet (no admin write endpoint for `DatasetMetadata`, and B03
  was scoped to GETs only) - every new endpoint is real and correct but returns 404/empty
  against a real database until something populates `dataset_metadata`.
- `findRelatedCandidates`/`countPublishedByDatasetSlug`/the new sort modes have no
  Testcontainers-backed test (same Docker-less-environment gap as `MongoProblemRepositoryTest`).

Next task:
- **B04** — populate real `DatasetMetadata` rows (schema/sample-row detail) from the
  existing `datasets/two-sum` CDM descriptor via `tools/dataset-cli`, so `/api/v1/datasets`
  and `/api/v1/problems/{slug}/schema`\`/sample-data` return real data instead of 404 - the
  natural next step now that the read API exists but nothing feeds it. Needs a decision:
  a one-off admin write endpoint on `DatasetMetadataRepository`, or a `dataset-cli` command
  that talks to catalog-service directly.

### Backend Roadmap
01 B05 — MongoDB materializer + document shaping
02 B10 — Result comparator (unblocks real grading; execution side already exists)
03 B11 — submission-service grading logic, wired to real execution-service calls
04 B08 — statement-classifier red-team suite as its own milestone (validator already built, see B04 below - this would be hardening/audit, not first-build)
05 B17 — gamification-service business logic (data model already in place from B02)
06 populate real DatasetMetadata from datasets/two-sum (still open - carried from B03, not done this session either)
07 proposed, not yet in the milestone table — learning-paths content controller
08 proposed, not yet in the milestone table — bookmarks CRUD (add/remove) controller on user-service

### B04 Secure Database Execution Service
Status: PARTIAL - implementation, unit, and integration tests all complete and green; the task's own explicit completion gate ("actual PostgreSQL execution verification") is not yet satisfied - see Verification below.
Completed: 2026-08-29

Note on scope: this session's B04 request superseded the roadmap line the prior
session had queued next ("populate real DatasetMetadata") - the human asked for the
execution architecture instead, the same kind of deliberate override M13/M14/M16/B20/
B02's-new-services were. It also does the job of three rows in `backend/CLAUDE.md`'s
original milestone table at once - B07 (sandbox), B08 (statement classifier), and B09
(execution-service) - collapsed into one because the task asked for "the first
production-grade execution architecture" as a single unit, not three sequential
milestones. Real container/process sandboxing (B07's original scope) is still not
built - see Known limitations.

Implemented - new module `backend/services/execution-service` (port 8088), first real
consumer of `engine-spi`/`adapter-postgres`:

- **Abstractions** (all requested by name): `DatabaseEngine` (registry over
  `engine-spi`'s existing `DatabaseEngineAdapter` - deliberately not a re-declaration of
  that interface, just a by-`EngineType` lookup, so MySQL/Mongo extensibility means
  registering a bean, not touching this code), `QueryValidator`/`QueryValidatorRegistry`,
  `DatasetMaterializer` (materialize-once/clone-per-request, in-memory template cache),
  `SandboxProvider`, `QueryExecutor`, `ResultEvaluator`, `ExplainProvider`.
- **QueryValidator** (`PostgresSqlQueryValidator`, JSqlParser 4.9 - hard rule #4, AST-only,
  never regex): parses via `CCJSqlParserUtil.parseStatements` (single-statement check
  blocks stacked-query injection), requires a `Select` with no `INTO` target (blocks
  every DML/DDL/admin statement type in one check), rejects any reference to
  `pg_catalog`/`information_schema`/`pg_toast` (via `TablesNamesFinder`), and rejects a
  deny-listed function call (`pg_sleep*`, file/process/dblink/large-object functions)
  found by `FunctionCallCollector` - a hand-written clause-by-clause `SelectVisitor` +
  `ExpressionVisitorAdapter` walk. **This walker went through a real fix mid-session**: a
  first version piggy-backed on `TablesNamesFinder`'s own traversal (override
  `visit(Function)`, let its existing walk do the work) - a test case
  (`pg_sleep(5) IS NOT NULL` in a WHERE clause) proved that miss-detects, because
  `TablesNamesFinder` is purpose-built for table-finding and skips expression shapes that
  can't contain a table reference, which is exactly the shape a hidden function call can
  hide in. Rewritten to drive every clause (select items, WHERE, HAVING, GROUP BY, ORDER
  BY, JOIN ON, subqueries in FROM/WHERE/CTEs) explicitly instead of trusting another
  visitor's incidental coverage.
- **SandboxProvider** (`PostgresSandboxProvider`): acquires a session via
  `DatasetMaterializer` (template-clone, cheap), asserts the session's database name
  matches this service's own naming convention before ever executing anything against it
  (a hard "no production DB" backstop - `NotASandboxDatabaseException` if it doesn't),
  applies `ALTER DATABASE ... CONNECTION LIMIT` as DB-level defense in depth (a real
  connection-refusal bug was found and fixed here too - see Verification). Uses a
  dedicated Postgres role (`dbarena_sandbox`: `LOGIN CREATEDB NOSUPERUSER NOCREATEROLE
  NOREPLICATION`, statement/idle timeouts set at the role level) - never the platform's
  own credentials, never anything client-supplied.
- **QueryExecutor** (`DefaultQueryExecutor`): runs the statement with a server-side
  timeout and row cap (`StatementRequest` gained a `maxRows` field for this - back-compat
  constructor preserved, see Changed modules), then best-effort re-runs the same
  statement via `EXPLAIN (ANALYZE)` under its own short budget purely to capture
  Postgres's own "Planning Time" line for the metrics ("planning time when available" -
  absence is normal, not an error, since plain `EXPLAIN` never reports it and the
  re-run can fail/timeout without affecting the primary result).
- **ResultEvaluator** (`DefaultResultEvaluator`): truncates to the row-limit policy,
  stringifies every `CdmValue` for safe/simple JSON+Mongo serialization
  (`CdmValueStringifier`), computes a result-size-bytes estimate - the seam a future
  reference-solution comparator (B10) would plug into, server-side only ("no hidden
  test-case exposure").
- **ExplainProvider** (`DefaultExplainProvider`): wraps the adapter's `explain()`,
  translating its throw-on-failure behavior into a clean `ExplainFailedException` rather
  than a raw 500.
- **State machine** (all 12 requested states) in `Execution`/`ExecutionStatus`: an
  immutable record with `withX` transition methods, persisted to MongoDB
  (`executions` collection, `(userId, requestedAt desc, _id desc)` index) after every
  transition, plus a structured audit-log line per transition (`ExecutionAuditLogger`,
  its own `EXECUTION_AUDIT` logger name, statement text truncated in the log line, not
  omitted). Flow: `POST` validates synchronously (fast rejection) then queues the rest
  (sandbox/execute/evaluate) onto a virtual-thread-per-task executor, bounded by a
  service-level `Semaphore` (the real "connection limit" control) and a per-user
  in-memory concurrency counter (`TooManyConcurrentExecutionsException` at capacity -
  same "in-memory, single-instance-scoped, deliberately" posture as ai-assistant-
  service's `HintRateLimiter`). Cancel is best-effort (`Future.cancel(true)` plus a
  cancellation flag checked at every phase boundary); the statement timeout is the hard
  backstop regardless of whether the interrupt itself lands.
- **API** (`ExecutionController`, all four endpoints, all requiring `@CurrentUser` -
  no `PublicPaths` entry, no anonymous access at all): `POST /api/v1/executions`,
  `GET /api/v1/executions/{id}` (+ `GET /api/v1/executions` list), `POST .../cancel`,
  `POST .../explain`. Ownership is enforced as indistinguishable-from-404 (never a 403 -
  "no hidden test-case exposure" extended to "don't even confirm another user's
  execution id exists").
- **Security requirements**, mapped 1:1: authenticated-only (✓ above); statement-size
  limit (`ExecutionPolicy.maxStatementLength`, checked pre-parse); timeout (JDBC
  `setQueryTimeout`, server-side only); row limit (JDBC `setMaxRows`, server-side only);
  connection limit (semaphore + per-database `ALTER DATABASE ... CONNECTION LIMIT`);
  restricted statements (`QueryValidator`, above); no production DB (naming-convention
  assertion, above); no client-supplied credentials (`CreateExecutionRequest` has no
  credential/timeout/limit field - everything is `ExecutionPolicy`, server-side only);
  audit logging (above); resource controls (timeout+rows+size+concurrency, all
  server-side ceilings); safe default policy (`ExecutionPolicy` - one record, every field
  a ceiling, `application.yml`'s `dbarena.execution.policy.*`).
- **Metrics**, all requested: execution time, planning time (when available, above),
  rows returned, result size (bytes), status (on `Execution` itself).
- MySQL/Mongo extensibility: confirmed intact by construction - `DatabaseEngine` and
  `QueryValidatorRegistry` are both bean-registry lookups; adding either engine needs a
  new `@Component`, not a change to anything in this module.

Changed modules (read/touched, per the task's own instruction, only what was relevant):
- `engine-spi`: `StatementRequest` gained `maxRows: Optional<Integer>` (back-compat 2-arg
  constructor preserved - the 3 existing call sites across engine-spi/adapter-postgres/
  adapter-mysql tests compile unchanged).
- `adapter-postgres`: `PostgresEngineAdapter.execute` now calls
  `Statement.setMaxRows(request.maxRows().orElse(0))`. Nothing else in this already-built
  module touched.
- New module `services/execution-service` (full list in the file tree - not repeated
  here per "keep CLAUDE.md concise, no code dumps").
- `services/pom.xml` (module registration), `api-gateway` (`/api/v1/executions` route,
  no `PublicPaths` entry - default-deny is correct here).

Database/schema: new Mongo collection `executions` in a new `DBArena_execution`
database, one index (`userId` desc, `requestedAt` desc, `_id` desc).

Tests:
- **Unit**: `PostgresSqlQueryValidatorTest` - 39 cases, the malicious-query suite the
  task asked for (11 real allow-cases incl. JOIN/subquery/CTE/UNION/aggregate; rejects
  every DML/DDL/admin statement type, stacked queries, `SELECT...INTO`, 9 denied-function
  evasion attempts incl. case variation and schema-qualification, 3 denied-schema cases,
  blank/malformed/oversized statements). `DefaultResultEvaluatorTest` (4),
  `CdmValueStringifierTest` (5), `ExecutionDocumentMapperTest` (3, incl. round-tripping a
  pending/no-result-yet execution), `PostgresSandboxProviderTest` (3, incl. the
  connection-limit-failure-must-not-be-fatal fix below), `ExecutionServiceTest` (5 -
  reject-without-touching-sandbox, full async run to `COMPLETED` via `Mockito.timeout`,
  per-user concurrency-cap enforcement under a real blocked worker, cross-user
  not-found, terminal-cancel-is-a-no-op).
- **Integration (live Postgres)**: `LivePostgresExecutionIntegrationTest` - 4 cases
  against the real `datasets/two-sum` CDM dataset materialized into real Postgres: a real
  SELECT returning real rows, a malicious `DROP TABLE` rejected pre-execution (then
  proving the table still exists via a follow-up real SELECT), row-limit truncation
  against real data, and a real Postgres EXPLAIN plan. Gated on
  `DBArena_EXECUTION_POSTGRES_PASSWORD` being set (same "environment-gated, not a stub"
  posture as this repo's Testcontainers-backed tests being gated on Docker) - **currently
  skipped**, not yet run, because this environment has a real native PostgreSQL 18
  install but this session does not have credentials for it; the human chose to
  provision a dedicated `dbarena_sandbox` role themselves rather than share the
  superuser password (script handed to them mid-session). **This is the one item this
  task's own completion gate requires that is not yet satisfied.**
- Two real bugs were caught and fixed by these tests before being trusted: (1)
  `PlainSelect.getIntoTables()` returns `null`, not an empty list, when there's no INTO
  clause - the original `!getIntoTables().isEmpty()` check NPE'd on every plain SELECT;
  (2) `PostgresSandboxProvider`'s connection-limit step caught only `SQLException`, but
  `PostgresConnectionFactory` wraps a connection failure as its own unchecked
  `PostgresAdapterException` - an unreachable admin connection was crashing `acquire()`
  entirely instead of logging and continuing as intended, caught by writing
  `PostgresSandboxProviderTest` against a real-but-unreachable `PostgresConnectionFactory`
  rather than mocking it.
- `mvn install -DskipTests` of the full dependency chain, then `mvn test` per module -
  `execution-service` 59/59 (55 real + 4 correctly-skipped live-Postgres cases),
  `engine-spi`/`adapter-mysql`/`catalog-service`/`user-service`/`gamification-service`/
  `submission-service`/`api-gateway` re-run clean (no regression from the
  `StatementRequest`/`PostgresEngineAdapter` change). `adapter-postgres` re-run clean
  (Testcontainers-backed test still excluded, standing Docker-less-environment gap).
  Whole-reactor `mvn compile` still blocked by the already-known, unrelated
  `common-events` module (documented since B02) - every touched/dependent module was
  verified via targeted `-pl ... -am` builds instead, same as every session since B02.

Known limitations:
- **Live Postgres verification not yet run** - blocks this task's own completion
  criterion; the moment `dbarena_sandbox`'s credentials are available, run
  `DBArena_EXECUTION_POSTGRES_PASSWORD=... mvn test -pl services/execution-service
  -Dtest=LivePostgresExecutionIntegrationTest` from `backend/` and update this entry.
- Sandboxing is database-level isolation (a disposable Postgres database + a
  privilege-narrowed role + AST validation as the real gate), not process/container
  isolation - B07's original "gRPC sandbox agent" scope needs Docker, unavailable in this
  environment; documented as the honest current boundary, not silently substituted for.
- Cancellation is best-effort (`Future.cancel(true)` may or may not interrupt a blocked
  pgjdbc call depending on where it's blocked) - the statement timeout is the actual hard
  guarantee.
- No hidden-test-case/reference-solution comparison exists yet (that's B10) -
  `ResultEvaluator` is the documented seam for it, not a stub of it.
- Per-user/global concurrency limits are in-memory, single-instance-scoped (same
  documented posture as `HintRateLimiter`) - need a shared store before more than one
  execution-service instance runs.
- No MySQL/Mongo `QueryValidator` or wired adapter yet - only Postgres, as the task asked
  ("Implement PostgreSQL first"); the registries are ready for both.
- `FunctionCallCollector`'s deny-list function/schema coverage is deliberately
  conservative, not exhaustive - documented as a starting set, not a claim of completeness.

Next task:
- **Finish B04**: get `dbarena_sandbox` credentials and run
  `LivePostgresExecutionIntegrationTest` for real; update this entry's Status to COMPLETE
  once it passes.
- **B05** — MongoDB materializer + document shaping remains the next new-ground
  milestone after B04 is actually closed out, per the roadmap above.
