# backend/ — Java 21 · Spring Boot 3.3 · Maven reactor

Scope: everything server-side. Frontend lives in `../frontend`, prompt assets in `../ai`.

## Reactor layout

```
platform-bom/          dependency + plugin version management (import this, pin nothing locally)
platform-common/
  common-core/         errors, IDs, pagination, Result types, canonical value model
  common-security/     JWT resolver, @CurrentUser, RBAC annotations
  common-events/       Avro schemas, Kafka producer/consumer config, outbox
  common-observability/OpenTelemetry + structured logging conventions
  common-testing/      Testcontainers fixtures, ArchUnit rules
engine-spi/            DatabaseEngineAdapter + models.  NO SPRING.
engine-adapters/
  adapter-postgres/    JDBC. materialize, template-clone, introspect, execute, explain
  adapter-mongodb/     Mongo driver. materialize + document shaping, execute, explain
services/              one Spring Boot app each
tools/
  dataset-cli/         author/validate/generate/materialize CDM datasets
  problem-validator/   cross-engine reference-solution gate
```

## Rules specific to backend

- `engine-spi` and `engine-adapters/*`: no `org.springframework` import, ever. Enforced by
  ArchUnit in `common-testing`.
- No service module may depend on another service module. Also ArchUnit-enforced.
- Enable virtual threads in every service that does blocking I/O:
  `spring.threads.virtual.enabled=true`. Query execution is blocking JDBC by design —
  do not rewrite to WebFlux/R2DBC.
- Records for DTOs. Constructor injection. No Lombok.
- Every Kafka consumer must be idempotent, keyed on an event or submission id with a
  UNIQUE constraint doing the work (see `docs/01` §6.3).
- Every producer writes through the transactional outbox in `common-events`.

## Testing

- Unit: JUnit 5 + AssertJ.
- Property-based (jqwik) for the comparator and the statement classifier.
- Integration: Testcontainers. Real Postgres, real Mongo, real Redis, real Redpanda.
  Never a mock, never an embedded fake.
- Contract: Spring Cloud Contract between services.

## Commands

```bash
mvn -T 1C verify                              # whole reactor
mvn -pl services/execution-service -am verify # one module + its deps
mvn -pl backend/tools/dataset-cli exec:java   # run the CLI
```

## Adding a new engine

Implement `DatabaseEngineAdapter`, a `Materializer`, a `StatementAnalyzer`, a `PlanParser`,
and declare a total `TypeMapper`. Touch nothing else. If you find yourself modifying
`execution-service` or `submission-service` to add an engine, the abstraction has leaked —
stop and flag it.

---

## Session protocol — read this every time

This file is a **living log**, not just a rulebook. Follow this exact sequence every
session, no exceptions:

1. **Read this entire file first**, especially the Session Log at the bottom. It tells you
   what already exists, what decisions were already made, and what the previous session
   left in a known-broken or partial state. Do not re-derive decisions already logged.
2. Check `docs/04-claude-build-playbook.md` §3 for the milestone map and confirm the
   milestone you're about to start has all its dependencies marked ✅ in the log below.
3. Do the work for **one milestone only**. Do not start a second milestone in the same
   session even if there's time left — end the session, log it, start fresh.
4. Before ending the session: `cd backend && mvn -T 1C verify` must be green.
5. **Append** a new entry to the Session Log — never edit or delete a prior entry. If a
   past decision turned out wrong, log a new entry that supersedes it; don't rewrite
   history.
6. Update the milestone status table (below the log) so the next session knows what's
   unblocked.

### Session Log entry template

```
### [YYYY-MM-DD] M<nn> — <milestone name>

**Status:** ✅ complete | 🟡 partial (see Carried forward) | 🔴 blocked

**Built:**
- <module/class> — <one line on what it does>

**Key decisions:**
- <anything not already in docs/01–03 that future sessions need to know>

**Deviations from docs:**
- <none, or exactly what and why — if this happens, also flag it to the human>

**Tests:** <n> added, `mvn verify` <green/red>, coverage on <module> = <x>%

**Carried forward:** <anything left undone in this milestone, or "none">

**Unblocks:** M<nn>, M<nn>
```

Keep entries terse — this is a machine-readable changelog for your future self, not prose
for a human reader. Full rationale belongs in code comments or ADRs, not here.

---

## Milestone status

| # | Milestone | Status | Depends on |
|---|---|---|---|
| B01 | Maven reactor + common-* + engine-spi interfaces | 🟡 partial (code written, not `mvn verify`-green — see Session Log) | — |
| B02 | CDM model + validator (`dataset-cli`) | 🟡 partial (code written, not `mvn verify`-green — see Session Log) | B01 |
| B03 | Type mapping (`CdmType` → Postgres/Mongo) | 🟡 partial (code written, not `mvn verify`-green — see Session Log) | B01 |
| B04 | Postgres materializer + introspection | 🔴 not started | B02, B03 |
| B05 | MongoDB materializer + document shaping | 🔴 not started | B02, B03 |
| B06 | Cross-engine equivalence proof ⭐ | 🔴 not started | B04, B05 |
| B07 | Sandbox agent (gRPC execute/cancel/kill) | 🔴 not started | B01 |
| B08 | Statement classifier + red-team suite ⭐ | 🔴 not started | B01 |
| B09 | execution-service | 🔴 not started | B04, B05, B07, B08 |
| B10 | Result comparator ⭐ | 🔴 not started | B03 |
| B11 | submission-service + grading runs | 🔴 not started | B09, B10 |
| B12 | problem-validator (authoring gate) | 🔴 not started | B10, B11 |
| B13 | catalog-service | 🟡 partial (code written, not `mvn verify`-green — see Session Log) | B01 |
| B14 | identity-service + api-gateway | 🟡 partial (code written, not `mvn verify`-green — see Session Log) | B01 |
| B15 | user-service | 🔴 not started | B01 |
| B16 | ai-assistant-service — ContextBuilder first | 🟡 partial (code written, not `mvn verify`-green — see Session Log) | B13 |
| B17 | gamification-service | 🔴 not started | B11 |
| B18 | ingestion-service | 🔴 not started | B02, B04, B05 |
| B19 | sandbox-manager + Helm/K8s | 🔴 not started | B07, B09 |

⭐ = requires the test-suite-first protocol from `docs/04-claude-build-playbook.md` §4.
Update the status column yourself (🔴 not started / 🟡 in progress / ✅ complete) as part
of every Session Log entry.

---

## Session Log

### [2026-08-28] M01 — Maven reactor and shared modules

**Status:** 🟡 partial (see Carried forward)

**Built:**
- `backend/pom.xml` — reactor root (Java 21, Spring Boot 3.3.5, jacoco/surefire/compiler plugin management + execution)
- `platform-bom` — single dependency-version pin point (Spring Boot 3.3.5, Spring Cloud 2023.0.3, Testcontainers 1.20.3, ArchUnit 1.3.0, jqwik, JSqlParser, Avro, Mongo driver, Flyway 10, Mongock, OpenTelemetry, logstash-logback-encoder)
- `common-core` — DomainException/NotFoundException/ConflictException/ValidationException, TypedId + UlidIdGenerator, PageRequest/CursorPage/Cursors, Result<T,E>, CdmValue + CdmValues (canonical scalar model implementing hard rule #9: scaled-integer decimals, epoch-millis timestamps)
- `common-security` — AuthenticatedUser, JwtVerifier + Hs256JwtVerifier (Nimbus), CurrentUserContext, JwtAuthenticationFilter, @CurrentUser + resolver, @RequiresRole + RoleAuthorizationAspect, CommonSecurityAutoConfiguration
- `common-events` — OutboxDispatchedEvent.avsc (+ avro-maven-plugin codegen), OutboxRecord/OutboxRepository/OutboxRelay, AvroEventSerializer, ProcessedEventGuard, idempotent-producer autoconfiguration
- `common-observability` — CorrelationIdFilter, MdcKeys, logback-json-base.xml (logstash-logback-encoder), CommonTagsMeterRegistryCustomizer, autoconfiguration
- `common-testing` — ArchitectureRules (noSpringDependency, noServiceDependsOnAnotherService), Testcontainers fixtures (Postgres/Mongo/Redis/Redpanda)
- `engine-spi` — DatabaseEngineAdapter + model records (SessionHandle, DatasetDescriptor, MaterializationResult, StatementRequest, ColumnMeta, ResultRow, ExecutionResult, ExecutionError, ExplainPlan, EntitySchema, SchemaSnapshot); EngineSpiArchitectureTest enforces hard rule #1 for real, not just by convention

**Key decisions:**
- engine-spi depends on common-core (also framework-free) to reuse CdmValue for ResultRow instead of inventing a second canonical value type - only Spring is forbidden by hard rule #1, not common-core.
- Confluent Schema Registry is deliberately NOT wired into common-events yet - plain Apache Avro binary (de)serialization only. Registry integration needs a live Kafka cluster and belongs to whichever milestone stands one up for real (B09+).
- common-security defaults to HS256 (Nimbus) for local/dev; identity-service (B14) can add RS256/JWKS verification against its own keys without changing the JwtVerifier interface.
- engine-spi's DatasetDescriptor is a deliberate placeholder - docs/02 (Canonical Dataset Model design) does not exist yet, so B02 (tools/dataset-cli) will very likely extend or replace it.

**Deviations from docs:**
- docs/01-04 referenced by root CLAUDE.md do not exist in the repo yet (only the four CLAUDE.md files existed at session start). Per the human's explicit choice this session, M01 proceeded using only the detail already in backend/CLAUDE.md rather than blocking on those docs - flagged to the human directly, not silently assumed.

**Tests:** 33 test methods across 8 test classes. `mvn -T1C verify` could NOT be run this session: this sandbox, and the Cowork VM on the linked device, both have no network route to repo.maven.apache.org (HTTP 403 via the egress proxy in both places). What WAS verified: `common-core` and `engine-spi` main sources (their only dependency is each other - zero external jars) compile cleanly with plain `javac --release 21`, 0 errors. `common-security`, `common-events`, `common-observability`, `common-testing`, and all test sources were hand-reviewed against the Spring Boot 3.3 / Spring Kafka 3 / Nimbus JOSE / Avro / ArchUnit / Testcontainers APIs but not compiler-verified.

**Carried forward:**
- Run `mvn -T1C verify` on a machine with normal internet access (not this sandbox) - opening this project in IntelliJ (there's already an `.idea/` here) will do this automatically. The two spots most likely to need a small fix, since their exact APIs were reconstructed from memory rather than checked: common-events' use of `DefaultKafkaProducerFactoryCustomizer#updateConfigs`, and common-testing's `org.testcontainers:redpanda` artifact/class name.
- Coverage gates (80% instruction) not measured yet - needs the same `mvn verify` run.

**Unblocks:** M02, M03, M07, M08, M13, M14, M15 per the table below - each still needs its own milestone session, and none should start until M01's carried-forward items above are closed out.

### [2026-08-28] M14 — identity-service + api-gateway

**Status:** 🟡 partial (see Carried forward)

**Built:**
- `services/identity-service` — registration, login, `/me`, refresh-token rotation with reuse detection (a reused/already-rotated token revokes every live session for that user), logout. Plain JDBC (`NamedParameterJdbcTemplate`) + Flyway over Postgres (`users`, `user_roles`, `refresh_tokens`). BCrypt password hashing (`spring-security-crypto` only, not the full Spring Security stack). Access tokens minted with Nimbus HS256 using the same `dbforge.security.jwt.secret` common-security's `Hs256JwtVerifier` checks against. Refresh tokens are opaque (256-bit random, base64url), SHA-256-hashed at rest, returned only via an `HttpOnly; Secure; SameSite=Strict` cookie scoped to `/api/v1/auth` (hard rule #6) — never in the JSON body. RFC 7807 error mapping via `ProblemDetail`. `springdoc-openapi` wired for a live `/v3/api-docs` + `/swagger-ui.html`.
- `services/api-gateway` — single entry point at `/api/v1/**`. Deliberately a hand-rolled Spring MVC + `RestClient` reverse proxy, not Spring Cloud Gateway (see Key decisions). Longest-prefix route table (`dbforge.gateway.routes`), currently routing `/api/v1/auth/**` to identity-service and `/api/v1/catalog/**` to a not-yet-built catalog-service (B13). `GatewayAccessFilter` rejects (401, before proxying) any non-public path with no `AuthenticatedUser` resolved by common-security's filter — a convenience, not the security boundary: every backend service still verifies the token itself.
- Reactor: added `services` aggregator module (`identity-service`, `api-gateway`) to `backend/pom.xml`; added `spring-boot-maven-plugin` to root `pluginManagement` (each service's own pom opts in with the `repackage` execution — library modules never do); added `springdoc-openapi-starter-webmvc-ui` to `platform-bom`.

**Key decisions:**
- api-gateway uses a hand-rolled proxy (Spring MVC + `RestClient`), not Spring Cloud Gateway. Classic Spring Cloud Gateway requires WebFlux, which conflicts with backend/CLAUDE.md's "do not rewrite to WebFlux/R2DBC" - a servlet-based alternative (`spring-cloud-starter-gateway-mvc`) exists in principle, but its API wasn't something this session could verify against real dependency jars (no network - see M01's carried-forward note, still true), so a smaller, fully-hand-written proxy was chosen to keep the invented-API surface as small and as reviewable as possible. Revisit if Spring Cloud Gateway MVC is preferred once `mvn verify` is actually runnable.
- Gateway forwards the full original request path unchanged (no prefix stripping) - every backend service's own `@RequestMapping` matches the same path a client used, so there's exactly one path scheme to keep straight, not two.
- identity-service uses plain JDBC, not JPA/Hibernate - no ORM entity-proxy machinery, stays close to the "records for DTOs" spirit even though `UserAccount`/`RefreshTokenRecord` are persistence rows, not DTOs.
- User-lifecycle event publishing (`common-events`' outbox) was deliberately left out of this milestone - B14's table entry is "identity-service + api-gateway", not event integration, and it can't be meaningfully tested without a live Kafka/Redpanda anyway.

**Deviations from docs:** none beyond M01's standing note (docs/01-04 still don't exist).

**Tests:** 34 test methods across 6 test classes. Same standing limitation as M01: `mvn -T1C verify` could not be run (no network route to Maven Central from this sandbox or the linked device's Cowork VM). Everything here needs Spring/Nimbus/JDBC, so unlike M01 nothing could be javac-verified standalone either — all of it is hand-reviewed against the Spring Boot 3.3 Web/JDBC/Validation, Spring Kafka-adjacent `RestClient`, Nimbus, and Testcontainers APIs, not compiler-verified. Two spots carry the most reconstructed-from-memory risk, flagged for a close look at `mvn verify` time:
  - `RestClient`'s exception type on connection failure (assumed `ResourceAccessException`, mirroring `RestTemplate`) in `ReverseProxyController`.
  - Record-based `@ConfigurationProperties` binding for `GatewayProperties`/`GatewayProperties.RouteRule` (nested list-of-records relaxed binding).
  Identity-service's integration test (`AuthControllerIntegrationTest`) exercises the full stack against a real Testcontainers Postgres: register → `/me` → login → refresh rotation (asserts the cookie actually changes and the old one is rejected) → reuse-detection revokes the whole chain → logout clears the cookie. The gateway's integration test (`ReverseProxyIntegrationTest`) uses a plain JDK `HttpServer` as a fake upstream (no real identity-service needed) to prove routing, header passthrough, and the 401-before-proxying behavior in isolation.

**Carried forward:**
- Run `mvn -T1C verify` on a machine with real internet access - same as M01's note, now covering ~2x the code.
- `NoRouteFoundException`/`UpstreamUnavailableException` mapping and identity-service's `GlobalExceptionHandler` are duplicated verbatim between the two services - lift into a shared `common-web` module the next time a third HTTP service needs the same RFC 7807 wiring, rather than copy-pasting a fourth time.
- No CORS configuration on api-gateway yet - needed before frontend/CLAUDE.md's workbench can call this from a browser origin.
- catalog-service (B13) and user-service (B15) are still 🔴 not started; the gateway's `/api/v1/catalog/**` route points at a service that doesn't exist yet.

**Unblocks:** nothing new becomes unblocked by B14 itself per the dependency table (B02/B03/B07/B08 were already unblocked by B01) - but a real, callable auth flow now exists for whichever service builds next to sit behind.

### [2026-08-28] M13 — catalog-service

**Status:** 🟡 partial (see Carried forward)

**Built:**
- `services/catalog-service` — problem catalog: public browsing (`GET /problems` cursor-paginated + filterable by tag/difficulty/engine/title search, `GET /problems/{slug}`, `GET /tags`) and admin authoring (`POST /problems`, `PUT /problems/{slug}`, `POST /problems/{slug}/publish`, `POST /problems/{slug}/unpublish`, all `@RequiresRole("admin")`). MongoDB via the plain `mongodb-driver-sync` (deliberately not Spring Data MongoDB - same "no ORM, stay close to records" posture identity-service (M14) took with plain JDBC). Mongock changelog creates a unique index on `slug` plus filter/sort-support indexes on `tags`, `difficulty`, `published`, and a compound `(createdAt, _id)` index backing cursor pagination. Timestamps stored as plain `long` epoch millis in every document, never a BSON `Date` (hard rule #9). An unpublished problem 404s for `GET` exactly like a nonexistent slug - no "preview my own draft" endpoint yet (Carried forward).
- api-gateway: `PublicPaths`/`GatewayAccessFilter` are now HTTP-method-aware (a `PublicRoute(method, pattern)` list, `null` method = any) so catalog *browsing* (`GET /api/v1/catalog/problems`, `/problems/**`, `/tags`) is public at the gateway while catalog *writes* still require a token - enforced again, independently, by catalog-service's own `@RequiresRole`, same "gateway check is a convenience, not the boundary" posture as M14.
- Reactor: added `catalog-service` to `services/pom.xml`.

**Key decisions:**
- MongoDB (not Postgres) for catalog-service, per root CLAUDE.md's stack line naming Mongo the "primary metadata store" - the first service in this reactor to actually use it. `EngineKind` (POSTGRES/MYSQL/MONGODB) is redeclared locally rather than shared with engine-spi, which has no such enum yet (its `DatasetDescriptor` is still M01's documented placeholder pending the real CDM design in B02) - delete this local enum in favor of a CDM-owned one once B02/B03 land, don't keep two sources of truth (Carried forward).
- `GET /problems/{slug}` and the admin endpoints share one `ProblemDetailResponse` DTO rather than two nearly-identical shapes - defensible because the service layer 404s an unpublished slug before that DTO is ever built for an anonymous caller, so nothing non-public leaks through it.
- Cursor pagination sorts on `(createdAt, _id)` - `createdAt` alone isn't unique, `_id` (a ULID, already time-ordered) is the tiebreaker, same pattern reused from common-core's `Cursors`/`CursorPage` (M01) rather than inventing a second pagination scheme.
- Title search is a case-insensitive regex on `title`, not real text search - a placeholder adequate for a catalog with a handful of problems, explicitly not an Atlas Search / text-index solution (Carried forward).

**Deviations from docs:** none beyond M01's standing note (docs/01-04 still don't exist).

**Tests:** 4 test classes in catalog-service (mapper round-trip unit test; `MongoProblemRepositoryTest` against real Testcontainers Mongo - insert/find/replace, cursor pagination + `hasMore`, tag/difficulty filtering, tag-count aggregation excluding unpublished; `CatalogServiceTest` with Mockito - duplicate-slug rejection, unpublished-is-404, publish/unpublish timestamp stamping, update preserving id/slug/createdAt, and that browsing always forces `publishedOnly=true` regardless of what's passed in; `ProblemControllerIntegrationTest` - full `@SpringBootTest` + Testcontainers Mongo + a real Mongock migration run + real JWT verification, covering unpublished→404→publish→visible, `@RequiresRole` 403 for both a non-admin token and no token at all, duplicate-slug 409, bean-validation 422, and paginated/filtered listing) plus a new `PublicPathsTest` in api-gateway for the method-aware public-path change. Same standing limitation as M01/M14: `mvn -T1C verify` could not be run (no network route to Maven Central). Nothing here could be javac-verified standalone either (Spring/Mongo driver/Mongock/Nimbus dependencies) - all hand-reviewed. The Mongock package (`ChangeLog001CreateProblemsIndexes` and its `mongock.*` application.yml properties) carries the highest reconstructed-from-memory risk in this milestone - flagged in its own Javadoc, check it first if `mvn verify` fails on this module. Two real bugs were caught and fixed during self-review before shipping: `listTagCounts` originally read the aggregation's `$sum` result via `Document.getInteger`, which returns `null` (not the value) when the driver decodes the sum as a `Long` rather than an `Integer`; and the integration test originally supplied `dbforge.catalog.mongo-database` and `mongock.mongo-db.database` as two independently-evaluated `System.nanoTime()` suppliers, which could resolve to two different random database names and silently split the app's Mongo database from Mongock's migration target - both fixed before packaging. A third, unrelated bug was also fixed while re-reading this file just now: the M14 append at the end of this Session Log had picked up two stray `</content>`/`</invoke>` lines (an artifact of a copy/paste while assembling that update) - removed.

**Carried forward:**
- Run `mvn -T1C verify` on a machine with real internet access - same standing note as M01/M14, now covering catalog-service too. Give the Mongock package the closest look (see Tests).
- No "preview my own unpublished draft" endpoint - an author can create/publish/unpublish but can't fetch their own draft's detail view before publishing it public. Needed once there's an authoring UI.
- Title search is a naive case-insensitive regex, not real text search - fine for now, revisit before the catalog has more than a handful of problems.
- `EngineKind` is duplicated locally pending a real CDM-owned enum from B02/B03 (see Key decisions).
- `GlobalExceptionHandler` is now duplicated a third time verbatim (identity-service, api-gateway, catalog-service) - the `common-web` extraction noted as carried-forward in M14 is now overdue; do it the next time a fourth HTTP service needs the same RFC 7807 wiring, rather than a fifth copy-paste.
- `PageRequest`'s constructor throws a plain `IllegalArgumentException` for an out-of-range `limit`, which none of the three services' `GlobalExceptionHandler`s map to an RFC 7807 response yet - a malformed `?limit=` query param currently surfaces as a raw 500. Worth an `IllegalArgumentException` handler in the eventual `common-web` module.
- `datasets/` (B02) still doesn't exist, so `Problem.datasetSlug` is a loose, unvalidated string reference, not a real foreign key - tighten once B02 lands.

**Unblocks:** B16 (ai-assistant-service — ContextBuilder first) per the table's dependency on B13. catalog-service also gives the frontend workbench (frontend/CLAUDE.md) a real problem list/detail API to build the schema explorer and problem-browsing UI against.

### [2026-08-28] M02 — CDM model + validator (dataset-cli)

**Status:** 🟡 partial (see Carried forward)

**Built:**
- `engine-spi` — new `com.dbforge.engine.spi.cdm` package: `CdmType` (a closed, 1:1 mirror of `CdmValue`'s sealed variants: BOOLEAN/INTEGER/DECIMAL/TEXT/TIMESTAMP/JSON), `CdmColumn` (name/type/nullable/primaryKey - a PK column can never be nullable, enforced in its constructor), `CdmForeignKey` (single-column only), `CdmRow` (column name -> `CdmValue`, order-preserving), `CdmEntity` (columns + foreign keys + seed rows, with `column()`/`primaryKeyColumns()` lookups), `CdmDataset` (the root: datasetId/name/schemaVersion/entities). This **replaces** M01's `DatasetDescriptor` placeholder per that class's own Javadoc instruction ("expect B02 to ... replace it") - `DatabaseEngineAdapter.materialize` now takes a `CdmDataset`. Nothing outside engine-spi referenced `DatasetDescriptor` yet, so this was a clean swap, not a breaking change to any built code.
- `engine-spi` — `CdmDatasetValidator` + `CdmValidationResult`: structural validation reusing common-core's existing `FieldViolation` rather than inventing a parallel type. Checks: case-insensitive unique entity/column names (a same-case-different-spelling collision is exactly what Postgres's unquoted-identifier lowercasing would silently merge at materialization time - caught here instead), every entity has at least one PK column, every FK's own column/target entity/target column actually exist, every seed row's key set exactly matches its entity's declared columns (no missing, no extra), every seed value's `CdmValue` variant matches its column's declared `CdmType`, non-nullable columns never get a null seed value, PK values are unique across an entity's seed rows, and FK values in seed data actually match a seed row in the referenced entity (a null value on a nullable FK column is treated as "no reference", not an error). Framework-free per hard rule #1, so B12 (problem-validator) and B18 (ingestion-service) can reuse it rather than reimplementing.
- `tools/dataset-cli` (new reactor module, `tools` aggregator added alongside it) — a plain CLI (not Spring Boot; see Key decisions), `mvn -pl backend/tools/dataset-cli exec:java -Dexec.args="validate <path>"`. `CdmDatasetLoader` parses a dataset.yaml (Jackson + `jackson-dataformat-yaml`) into intermediate `Yaml*` records, then converts each seed-row scalar into the right `CdmValue` variant using the column's declared `CdmType` (decimals via `BigDecimal` - `USE_BIG_DECIMAL_FOR_FLOATS` is enabled precisely so a seed value like `1.50` never round-trips through a `double`, per hard rule #9; timestamps via `Instant.parse` on an ISO-8601 string; JSON fragments re-serialized to canonical text through a second, plain `ObjectMapper`). `DatasetCli.run` exit codes: `0` valid, `1` parsed but `CdmDatasetValidator` found real problems (printed one per line), `2` usage/IO/YAML-parse error - kept as a package-visible method taking the arg array and both output streams so tests never have to fork a JVM or intercept `System.exit`.
- `datasets/two-sum/dataset.yaml` (new top-level `datasets/` directory, per root CLAUDE.md's repository layout) - a real sample exercising every `CdmType` variant, a PK, and a nullable FK, doubling as documentation of the authoring format and as a fixture other sessions can validate against.

**Key decisions:**
- dataset-cli is a plain CLI, not a Spring Boot app - unlike `services/*`, tools/* isn't bound by "one Spring Boot app each" and a fast-starting, dependency-light tool an author runs on every YAML edit has nothing to gain from a DI container. Uses `exec-maven-plugin` (added to root `pluginManagement` only, same "not auto-applied" pattern M14 used for `spring-boot-maven-plugin` - only tools/* opts in).
- This milestone deliberately stops at "validate" - author is manual YAML editing for now, and generate/materialize (turning a valid `CdmDataset` into real rows in a real engine) belong to B04/B05's engine adapters, not this CLI, even though the reactor-layout table's one-line description for `dataset-cli` mentions all four verbs. Scoped this way to match B02's actual milestone name and avoid reaching into engine-adapter territory before it exists.
- The YAML-to-`CdmValue` coercion logic lives in tools/dataset-cli, not engine-spi - it's authoring-format-specific (exact YAML scalar handling), whereas the CDM model and validator are format-agnostic and reusable by a future non-YAML importer (B18). If a second authoring format shows up needing the same coercion, promote it to a shared utility then rather than now.
- **Correction to M13's note:** M13's catalog-service Carried-forward said its local `EngineKind` enum was "pending a real CDM-owned enum from B02/B03" - that undersold what already existed. `engine-spi.EngineType` (POSTGRES, MONGODB) has existed since **M01**, not B02/B03, and B02 didn't touch it. The real gap is narrower and still open: `EngineType` has 2 values, catalog-service's `EngineKind` has 3 (adds MYSQL, per root CLAUDE.md's stack line naming MySQL as a target engine) - reconciling those is still Carried forward, just not something this milestone was the source of.

**Deviations from docs:** none beyond M01's standing note (docs/01-04 still don't exist). docs/02 ("core engine design", which was supposed to define the CDM) still doesn't exist either - this milestone designed the CDM shape itself, from root/backend CLAUDE.md's premise and hard rule #9 alone, per the human's now three-times-established "proceed with a documented, reasonable decision rather than block" pattern. Flagged here explicitly: **if docs/02 is later written and defines the CDM differently, this model is what needs to change to match it, not the other way around.**

**Tests:** 26 test methods across 4 test classes (`CdmModelTest` and `CdmDatasetValidatorTest` in engine-spi; `CdmDatasetLoaderTest` and `DatasetCliTest` in tools/dataset-cli). Same standing network limitation as M01/M13/M14. Unlike M13/M14, though, engine-spi's new `cdm` package (all 8 main classes plus the existing engine-spi/common-core sources) **was fully javac-verified this time** - `javac --release 21` against the real `common-core` + `engine-spi` sources compiled clean, 0 errors, because that pairing is still zero-external-jars just like M01. It caught a real bug before it ever reached the human: `CdmValidationResult` had both an instance method `valid()` and a same-named, same-arity static factory `valid()` - a genuine compile error, not a style nit - fixed by renaming the factory to `ok()`. tools/dataset-cli's main/test sources (Jackson, JUnit, AssertJ) could not be javac-verified the same way - no cached jars, same 403 from Maven Central - so those were hand-reviewed only. One real bug was caught that way and fixed before packaging: the loader's `INTEGER` case originally accepted any `Number`, so a mistyped decimal seed value (e.g. `1.5` for an INTEGER column) would silently truncate to `1` via `longValue()` instead of being rejected - tightened to only accept `Integer`/`Long`/`Short`/`BigInteger`, with a new test (`aDecimalLiteralForAnIntegerColumnIsRejectedRatherThanSilentlyTruncated`) proving it.

**Carried forward:**
- Run `mvn -T1C verify` on a machine with real internet access - same standing note as every prior milestone, now covering `tools/dataset-cli` and the `cdm` package too. Everything not javac-verified above should get the closest look.
- `EngineType` (engine-spi, 2 values) vs catalog-service's `EngineKind` (3 values, includes MYSQL) still need reconciling into one enum - see Key decisions' correction to M13. Natural home is probably `engine-spi.EngineType` gaining MYSQL, with catalog-service's `EngineKind` deleted in favor of it.
- Composite (multi-column) foreign keys and composite primary keys spanning application-level uniqueness beyond what's already supported (multiple `primaryKey: true` columns are allowed structurally, but the validator's tuple-uniqueness check is the only thing exercising that path - no dataset uses it yet) are unexercised by the sample dataset - add a second fixture once a real problem needs one.
- `datasets/two-sum/dataset.yaml` isn't loaded by any test via its real repo path (a relative-path assumption about Surefire's working directory wasn't something this session could verify without a real `mvn test` run) - the loader/CLI tests use inline YAML fixtures instead. Consider a copy of the real file as a classpath test resource once `mvn verify` is runnable, to catch drift between the shipped sample and the loader.
- No `generate`/`materialize` dataset-cli subcommands yet - deliberately deferred to B04/B05 (see Key decisions).
- Title/description authoring fields beyond bare column typing (e.g. a human-readable problem statement referencing this dataset) live in catalog-service (`Problem.datasetSlug`, still just a loose string reference per M13's carried-forward note) - the CDM model itself has no such fields and shouldn't; that's catalog metadata, not dataset structure.

**Unblocks:** B03 (type mapping, `CdmType` → Postgres/Mongo native types - now has a real `CdmType` to map from) and, transitively through B03, B04/B05/B10/B18 per the table below. Also unblocks nothing by itself for B13/B14/B15 (already B01-only).

### [2026-08-28] Audit — B01–B12 implementation status check

**Type:** read-only inspection, NOT a build session. No code written, modified, or deleted.
No `mvn` command run (no `verify`, no tests added or executed). Requested by the human,
scoped explicitly to B01–B12. Do not confuse this with a numbered `M<nn>` milestone entry
above — nothing here changes what's built, it only confirms what already is.

**Method:** filesystem inspection + grep only, from `backend/`:
- `find . -maxdepth 2 -name pom.xml` — lists actual reactor modules.
- `find . -iname "*adapter*"` — checks for `engine-adapters/*` (B04–B06).
- `ls services/`, `ls tools/` — checks for execution-service/submission-service (B09/B11)
  and problem-validator (B12).
- `grep -rl` across every `.java` file for milestone-identifying symbols: `TypeMapper`,
  `StatementClassifier`, `ResultComparator`, `SandboxAgent`, `execution-service`,
  `submission-service`, `problem-validator`, `CrossEngine`.

**Findings:**
- B01 (Maven reactor + common-* + engine-spi) — ✅ present, matches table (M01).
- B02 (CDM model + validator, dataset-cli) — ✅ present, matches table (M02).
- B03 (type mapping) — 🔴 confirmed absent. No `TypeMapper` symbol anywhere.
- B04 (Postgres materializer) — 🔴 confirmed absent. No `engine-adapters/` directory exists at all.
- B05 (MongoDB materializer) — 🔴 confirmed absent. Same — `engine-adapters/adapter-mongodb` does not exist.
- B06 (cross-engine equivalence proof) — 🔴 confirmed absent. No `CrossEngine*` symbol; depends on B04/B05, neither of which exists.
- B07 (sandbox agent) — 🔴 confirmed absent. No `SandboxAgent` symbol, no sandbox-agent module.
- B08 (statement classifier) — 🔴 confirmed absent. No `StatementClassifier` symbol.
- B09 (execution-service) — 🔴 confirmed absent. `services/` contains only `api-gateway`, `catalog-service`, `identity-service`.
- B10 (result comparator) — 🔴 confirmed absent. No `ResultComparator` symbol.
- B11 (submission-service) — 🔴 confirmed absent. No `submission-service` directory under `services/`.
- B12 (problem-validator) — 🔴 confirmed absent. `tools/` contains only `dataset-cli`.

The only three grep hits found (`StatementRequest.java`, `DatabaseEngineAdapter.java`,
`CdmDatasetValidator.java`) are pre-existing B01/B02 files that matched incidentally — an
M01 model record's name and a Javadoc cross-reference, not an actual B03–B12
implementation. None of B03 through B12 has any implementation footprint: no scaffolded
module, no empty class, nothing.

**Conclusion:** the milestone status table above already matches reality exactly for
B01–B12 — no cell needed changing. B01/B02 are the only implemented work in this range
(both still 🟡 partial pending an `mvn verify` run, per their own entries); B03–B12 are
correctly 🔴 not started. Per the standing sequential-order policy (see root `CLAUDE.md`'s
"Current phase"), **B03 (type mapping) is next.**

### [2026-08-28] M03 — type mapping (`CdmType` → Postgres/Mongo)

**Status:** 🟡 partial (see Carried forward)

**Built:**
- `engine-spi` — new `com.dbforge.engine.spi.typemap` package: `TypeMapper<T>` (a generic,
  intentionally minimal contract — `T map(CdmType)`), `PostgresColumnType` (an enum of the
  six native Postgres column types the mapping can produce, each carrying its literal SQL
  type name), `MongoBsonType` (the BSON-side equivalent), `PostgresTypeMapper` and
  `MongoTypeMapper` (stateless, thread-safe implementations of `TypeMapper<T>`, one per
  engine). Both mappers are the "total `TypeMapper`" backend/CLAUDE.md's "Adding a new
  engine" section requires: each is a `switch` over `CdmType` with **no `default` branch**,
  so adding a seventh `CdmType` variant is a compiler error in both mappers until each is
  updated — never a silent runtime gap or an `UnsupportedOperationException` discovered in
  production.

**Key decisions:**
- `DECIMAL` → Postgres `numeric` (unbounded, no fixed precision/scale) rather than
  `numeric(p,s)` — a `CdmColumn` declares no precision/scale of its own (`CdmValue.Decimal`
  carries its own scale per value, per hard rule #9), so the column type must accept
  whatever scale any given row uses, not one fixed in advance.
- `TIMESTAMP` → Postgres `timestamptz`, not a bare `timestamp` — Postgres normalizes
  `timestamptz` to UTC internally regardless of session timezone, so every read is
  convertible to hard rule #9's epoch-millis-UTC representation without depending on
  connection-level timezone configuration. The actual epoch-millis conversion at the JDBC
  boundary is B04's job, not this mapping's.
- `TIMESTAMP` → Mongo `MongoBsonType.INT64_EPOCH_MILLIS`, a plain BSON int64, **not** the
  BSON `date` type, and there is deliberately no `MongoBsonType.DATE` variant at all. A BSON
  date is an int64 epoch-millis value on the wire, but most driver/tooling layers decode it
  straight into a language-local date object on the way out — reintroducing the exact
  "engine-local timezone" risk hard rule #9 forbids. Storing a plain int64 keeps the on-disk
  representation identical to the platform's own canonical timestamp with no implicit
  conversion anywhere in the read path. This also matches the convention catalog-service
  (M13) already established independently for its own documents — B03 makes it the type
  mapper's rule instead of a decision every service has to remember to repeat.
- `DECIMAL` → Mongo `DECIMAL128`, never `double` — the one BSON numeric representation that
  stores an exact scaled value rather than a binary floating-point approximation, matching
  hard rule #9 on the Mongo side the same way `numeric` does on the Postgres side.
- `JSON` → Postgres `jsonb` (not `json`) and → Mongo an embedded `DOCUMENT` (not a `string`
  field) — both preserve `CdmValue.Json`'s canonical-text contract and let a learner query
  into the structure natively instead of treating it as an opaque blob.
- `INTEGER` → Postgres `bigint` and → Mongo `INT64` — `CdmValue.Int` carries a `long`, so
  anything narrower on either side risks silent overflow on materialization.
- Kept `TypeMapper<T>` generic over one type parameter rather than writing two unrelated,
  same-shaped interfaces — the "total switch, no default" discipline is identical for both
  engines and is worth stating once.
- Scope stopped at the mapping table itself — no DDL-fragment builder, no BSON document
  shaper. Those are B04/B05's job (they also need `CdmColumn`'s nullable/primaryKey flags
  and actual `CdmValue` instances, not just the type), and reaching into that here would
  duplicate what those milestones are meant to build.

**Deviations from docs:** none beyond M01's standing note (docs/01-04 still don't exist).

**Tests:** 18 test methods across 2 test classes (`PostgresTypeMapperTest`,
`MongoTypeMapperTest`), covering: every `CdmType` → documented native-type mapping
explicitly; a `@ParameterizedTest @EnumSource(CdmType.class)` sweep proving neither mapper
ever returns null for any variant (this is really re-proving switch-exhaustiveness, which
the compiler already guarantees, but it pins the behavior at the test level too, so a
future refactor that somehow reintroduces a `default` branch fails loudly here as well);
the specific "never a double/never a BSON date" decisions above asserted individually, not
just implied by the full-mapping test; purity (`map()` called twice for the same input
returns the same value); and that no two `CdmType` variants collapse onto the same
native type. Same standing network limitation as every prior milestone: `mvn -T1C verify`
could not be run (no route to Maven Central). Unlike M13/M14 and like M01/M02, though, this
milestone's main sources have **zero external dependencies** (only `common-core` and
`engine-spi` itself), so they were fully javac-verified this session: `javac --release 21`
against `common-core` + `engine-spi` (39 source files total) compiled clean, 0 errors, 0
warnings. No bug was caught this time — the two mappers are small, total switch
expressions with no branching logic to get wrong, unlike M02's record validators. Test
sources (JUnit 5 params, AssertJ) could not be javac-verified — same 403 from Maven
Central as everywhere else — so those are hand-reviewed only, same as every other
milestone's test code.

**Carried forward:**
- Run `mvn -T1C verify` on a machine with real internet access — same standing note as
  every prior milestone, now covering the `typemap` package too.
- No DDL-fragment or BSON-document-shaping code yet — deliberately deferred to B04/B05 (see
  Key decisions). Those milestones consume `PostgresTypeMapper`/`MongoTypeMapper` directly
  rather than re-deriving the mapping.
- `EngineType` (engine-spi, 2 values: POSTGRES, MONGODB) vs catalog-service's `EngineKind`
  (3 values, includes MYSQL) still need reconciling — unchanged from M02's note, this
  milestone didn't touch either enum. Worth flagging again here since B03's own milestone
  name literally says "Postgres/Mongo" and doesn't mention MySQL at all — root CLAUDE.md's
  stack line names MySQL as a target engine, but no MySQL type mapping, adapter, or
  reconciliation of `EngineType` exists yet anywhere in the codebase. That gap is real and
  still open, not resolved by this milestone.
- No precision/scale ceiling is enforced anywhere on `DECIMAL` values — Postgres `numeric`
  and Mongo `decimal128` both have their own real limits (`decimal128` tops out at 34
  significant digits), and nothing in `CdmDatasetValidator` (M02) or this mapping checks a
  seed value against them before it reaches an adapter. Worth a validator check once B04/B05
  exist to make an actual overflow observable end-to-end.

**Unblocks:** B04 (Postgres materializer — now has `PostgresTypeMapper` to build `CREATE
TABLE` column definitions from) and B05 (MongoDB materializer — now has `MongoTypeMapper`
to build document-shaping logic from). Also unblocks B10 (result comparator) per the
table's dependency on B03, though B10 also depends on B09/B00-adjacent execution
machinery that doesn't exist yet.

### [2026-08-28] M16 — ai-assistant-service (graduated hints)

**Status:** 🟡 partial (see Carried forward)

**Built:**
- `services/ai-assistant-service` (new, added to `services/pom.xml`) — one endpoint, `POST
  /api/v1/ai/problems/{slug}/hint`, returning a graduated, non-solution-revealing hint
  (`HintLevel`: `CONCEPT` → `APPROACH` → `NEAR_MISS`, chosen explicitly by the learner per
  request — no server-side hint history/auto-escalation yet, see Carried forward). Not in
  api-gateway's `PublicPaths` allowlist, so every call needs a valid access token
  (`@CurrentUser AuthenticatedUser`, non-optional) both for cost control (an LLM call has a
  real per-request cost) and to give every hint a resolvable requester for future
  rate-limiting.
- `client.CatalogServiceClient` — an OpenFeign client (`GET /api/v1/catalog/problems/{slug}`)
  calling catalog-service directly, not through api-gateway (hard rule #2: cross-service
  reads go through Feign; this is a service-to-service call, the gateway is for external
  clients). First real Feign use in this reactor — identity-service and api-gateway's own
  inter-service calls so far used a hand-rolled `RestClient` (M14), not Feign.
- `dataset.DatasetContextLoader` — loads a problem's `datasets/<slug>/dataset.yaml` via
  tools/dataset-cli's existing `CdmDatasetLoader` (a new *library* dependency on
  `dataset-cli`, not a service-to-service one — hard rule #2 only forbids one service
  module depending on another; `dataset-cli` lives under `tools/`). Missing or unparseable
  dataset.yaml degrades gracefully (schema section omitted from the hint context) rather
  than failing the request — a problem with no authored dataset yet is an expected state,
  not a bug.
- `context.AiContextBuilder` — the actual implementation of hard rule #5: `MAX_SAMPLE_ROWS_PER_ENTITY
  = 10` is a `public static final int`, not a config value, not a constructor parameter, not
  read from any request. Truncates the problem statement, the learner's query, and the
  learner's pasted error/result text to fixed character caps so the whole context stays
  compact regardless of how much a learner pastes in.
- `prompt.HintPromptBuilder` — builds a dense, non-prose system+user prompt: explicit rules
  (never write a complete runnable final query, never claim/invent a "reference solution",
  reference only the given schema/rows/query, plain text, hard word limit) plus a
  level-specific guide (CONCEPT/APPROACH/NEAR_MISS), and a compact single-line-per-item
  rendering of the `HintContext` (schema as `entity(col:TYPE FLAGS, ...)`, sample rows as
  raw maps, no markdown tables).
- `provider` package — `AiCompletionClient` interface; `GroqCompletionClient` (primary,
  Groq's OpenAI-compatible `/chat/completions`, default model `llama-3.3-70b-versatile`) and
  `GeminiCompletionClient` (fallback, Gemini's `generateContent` REST endpoint, default model
  `gemini-3-flash-preview`) — both plain JDK `HttpClient` + Jackson, no AI SDK dependency,
  same "plain driver, not a framework" posture identity-service (plain JDBC) and
  catalog-service (plain Mongo driver) already established. `FallbackAiCompletionGateway`
  tries Groq first, falls back to Gemini on any `AiProviderException` (not configured,
  timeout, non-2xx, unparseable response), throws `AiUnavailableException` (502) only if
  both fail.
- `guard.OutputGuard` — the actual enforcement of "output must be within the output range":
  `HARD_MAX_CHARS = 1200` is applied to whatever the provider returns, after the fact,
  regardless of provider or prompt — a model ignoring the prompt's word limit is a "when",
  not an "if", so this is a real guarantee, not just a request.
- `service.HintService` — sequences the above: catalog lookup → dataset lookup → context
  build → prompt build → fallback gateway → output guard → response. No business logic
  anywhere else.
- api-gateway: added a new `/api/v1/ai` route to `dbforge.gateway.routes` (`application.yml`
  only — no `PublicPaths` change, so it inherits "auth required" by default).

**Key decisions:**
- Scope narrowed to exactly one feature (context-aware graduated hints) per the human's
  explicit choice this session, out of a broader menu (mistake explainer, next-problem
  recommender, concept chat) - those remain open, not started, not designed.
- Built now, on top of catalog-service (B13) alone, rather than waiting for B09–B12
  (execution-service/submission-service/result comparator) to exist — per the human's
  explicit choice. Consequence: the hint endpoint has no access to real query-execution
  output or pass/fail state; `errorOrResultText` is whatever the learner pastes in by hand.
  This is a request-shape decision, not a throwaway one — once execution-service exists, its
  real error/result output can be passed into that same field with no API change, only a
  richer caller.
- Groq primary / Gemini fallback, not a config-driven provider list — per the human's
  explicit instruction. `FallbackAiCompletionGateway`'s constructor takes the two concrete
  clients by name (not a `List<AiCompletionClient>` with an `@Order`), so "Groq is primary"
  is a compile-time-visible fact, not an ordering that depends on Spring bean registration.
- Model defaults (`llama-3.3-70b-versatile` for Groq, `gemini-3-flash-preview` for Gemini)
  were chosen from each provider's own current-as-of-2026-08 model documentation
  (console.groq.com/docs/models; ai.google.dev/gemini-api/docs/gemini-3), not invented -
  both are overridable per-environment via `dbforge.ai.groq.model` / `dbforge.ai.gemini.model`
  with no code change needed if either provider ships a newer default later.
- No reference-solution field exists anywhere in `HintContext` - not because one was removed,
  but because none exists yet anywhere in this system (B12/problem-validator would be what
  introduces one). The prompt's "never reveal a reference solution" rule is written to hold
  once one does exist, not just written for today's actual data shape.
- `datasets-root` is a relative path (`../../../datasets` by default) assuming the process
  cwd is this module's own directory - same category of assumption M02 flagged about
  Surefire's working directory, unverified end-to-end without a real run.

**Deviations from docs:** none beyond M01's standing note (docs/01-04 still don't exist).

**Tests:** 25 test methods across 5 test classes: `AiContextBuilderTest` (row-cap
enforcement pinned at exactly 10, truncation of long statement/query, every `CdmValue`
variant renders as plain text, missing dataset degrades gracefully), `HintPromptBuilderTest`
(system prompt names the level/word limit/forbids a full solution and a claimed reference
solution, user prompt includes/omits sections correctly), `OutputGuardTest` (hard-cap
truncation and flagging, exact-boundary case, whitespace stripping),
`FallbackAiCompletionGatewayTest` (Mockito - primary-succeeds, falls-back-on-failure,
both-fail-throws, Gemini never called when Groq succeeds), and `HintControllerIntegrationTest`
(`@SpringBootTest` + `MockMvc`, no Testcontainers needed - this service has no database of
its own; fakes catalog-service, Groq, and Gemini each as a plain JDK `HttpServer`, same
pattern api-gateway's `ReverseProxyIntegrationTest` and catalog-service's
`ProblemControllerIntegrationTest` already established - covering the happy path via Groq,
the Gemini fallback path, the both-providers-down 502, no-token 401, unknown-slug 404, and
blank-learner-query 422). Same standing network limitation as every prior milestone: `mvn
-T1C verify` could not be run (no route to Maven Central). This milestone's main sources
depend on Spring/Feign/Jackson, so - like M13/M14 and unlike M01–M03 - none of it could be
javac-verified standalone; all hand-reviewed. Highest reconstructed-from-memory risk areas,
flagged for the closest look at `mvn verify` time: `feign.FeignException.NotFound` as the
exact type thrown for a 404 response (this is the first real Feign use in the reactor - M14's
own inter-service call used a hand-rolled `RestClient`, not Feign); the exact Groq
`/chat/completions` and Gemini `generateContent` request/response JSON shapes (built from
each provider's documented API format, not tested against the real APIs - no network access
to either from this sandbox); and `@ConfigurationProperties`-nested-class binding for
`AiProviderProperties.Groq`/`Gemini`.

**Carried forward:**
- Run `mvn -T1C verify` on a machine with real internet access - same standing note as every
  prior milestone, now covering ai-assistant-service and its Feign/Jackson/HTTP-client code.
  Give the Feign 404 handling and both provider clients' JSON shapes the closest look (see
  Tests).
- No hint-history or auto-escalation - the learner picks `HintLevel` explicitly every call;
  a "give me the next level" endpoint or session-scoped hint tracking is future work, not
  this milestone.
- No rate limiting or per-user cost control on the hint endpoint beyond requiring
  authentication - an LLM call has a real cost per request; this needs addressing before any
  real deployment, not just before scale.
- `GlobalExceptionHandler` is now a **fourth** verbatim copy (identity-service, api-gateway,
  catalog-service, ai-assistant-service). catalog-service's own Session Log already flagged
  the third copy as overdue for a shared `common-web` extraction; still not done here -
  retrofitting three already-shipped services was more than a "one milestone, no side
  quests" session should take on. Do it the next time a fifth HTTP service needs this.
- The other three AI-feature options from this session's menu (query mistake explainer,
  personalized next-problem recommender, concept mini-tutor chat) are still fully open - not
  started, not designed beyond the one-line option descriptions offered to the human.
- Execution-grounded hints (using real query output instead of learner-pasted text) are
  blocked on B09/B11 (execution-service/submission-service) - `HintCommand.errorOrResultText`
  is shaped to accept that input with no API change once those exist.
- No precision/scale-aware rendering for `CdmValue.Decimal` sample rows beyond
  `toBigDecimal().toPlainString()` - fine for the values in the one sample dataset that
  exists (`datasets/two-sum`), unexercised against anything with unusual scale.

**Unblocks:** nothing new per the table's dependency graph (B16 depended only on B13, already
done) - but this is the first working slice of the AI-assistant surface the frontend
(frontend/CLAUDE.md) or any future feature in this area can build against.
