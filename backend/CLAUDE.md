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
| B02 | CDM model + validator (`dataset-cli`) | 🔴 not started | B01 |
| B03 | Type mapping (`CdmType` → Postgres/Mongo) | 🔴 not started | B01 |
| B04 | Postgres materializer + introspection | 🔴 not started | B02, B03 |
| B05 | MongoDB materializer + document shaping | 🔴 not started | B02, B03 |
| B06 | Cross-engine equivalence proof ⭐ | 🔴 not started | B04, B05 |
| B07 | Sandbox agent (gRPC execute/cancel/kill) | 🔴 not started | B01 |
| B08 | Statement classifier + red-team suite ⭐ | 🔴 not started | B01 |
| B09 | execution-service | 🔴 not started | B04, B05, B07, B08 |
| B10 | Result comparator ⭐ | 🔴 not started | B03 |
| B11 | submission-service + grading runs | 🔴 not started | B09, B10 |
| B12 | problem-validator (authoring gate) | 🔴 not started | B10, B11 |
| B13 | catalog-service | 🔴 not started | B01 |
| B14 | identity-service + api-gateway | 🟡 partial (code written, not `mvn verify`-green — see Session Log) | B01 |
| B15 | user-service | 🔴 not started | B01 |
| B16 | ai-assistant-service — ContextBuilder first | 🔴 not started | B13 |
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
</content>
</invoke>
