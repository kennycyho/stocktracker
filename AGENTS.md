# StockTracker — Agent Guide

## Quick start

```bash
docker build -t ghcr.io/kennycyho/stocktracker:latest .
docker compose up -d
```

Secrets live in `.env` (`MAIL_USERNAME`, `MAIL_PASSWORD`, `NOTIFIER_RECIPIENT`, `DB_PASSWORD`). Tracked products/sites in `checkers.json`. Both files already exist in the repo root (gitignored).

## Testing

Tests need Postgres + Redis. Run in isolated containers:

```bash
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from test
docker compose -f compose.test.yaml down
```

This mounts `checkers.test.json` (empty array `[]`) at `/config/checkers.json` so checker beans don't fail. Tests use `@ActiveProfiles("test")` which loads `src/test/resources/application-test.properties` (stubs mail, keeps schema init).

**Unit tests** (`@ExtendWith(MockitoExtension.class)` or pure JUnit 5 — no Spring context) can run locally without services:

```bash
mvn test -Dtest=AbstractCheckerTest,SchedulerTest,HttpFetcherTest,EmailNotifierTest -DfailIfNoTests=false
```

Affected: `AbstractCheckerTest`, `AbstractCheckerTestClaude`, `SchedulerTest`, `HttpFetcherTest`, `EmailNotifierTest`.

**Integration tests** (`@SpringBootTest` + `@ActiveProfiles("test")`) need Postgres + Redis. They only run inside the test container or with local services running:

```bash
# requires Postgres on localhost:5432, Redis on localhost:6379
mvn test -Dtest=CooldownServiceTest,CooldownCacheServiceTest,ApplicationContextTest -DfailIfNoTests=false
```

## Build

```bash
mvn package -DskipTests
```

No lint, typecheck, or formatter config in the repo. `mvn compile` / `mvn test` are the only verification steps.

## Architecture

- **Entrypoint**: `app.Application` (`src/main/java/app/Application.java`)
- **Scheduled service**, not a web API. No REST controllers. Port 8080 is just the Spring Boot default (unused).
- Checkers run on a fixed delay (`checker.interval-ms`, default 30 min). Each checker is submitted to a virtual-thread executor — they run concurrently, failures are isolated per-checker.
- **Checker registry is config-driven**: `AppConfig` reads `checkers.json` at startup and maps the `"checker"` field via a `Map<String, Function>` factory. Adding a new checker **requires two things**: (1) a new class in `app.checker.impl` extending `AbstractChecker`, and (2) a new entry in the `checkFactory` map in `AppConfig.checkers()`.
- **Two-tier cooldown**: Redis cache → Postgres fallback. Every Redis op is try/caught — a Redis outage degrades performance, not correctness.
- **Schema**: `src/main/resources/schema.sql` creates the `COOLDOWN` table (id, url, last_seen, disabled). `spring.sql.init.mode=always` runs it on startup.

## Key files

| File | Purpose |
|---|---|
| `checkers.json` | Runtime config for tracked products/sites (gitignored, copy from example) |
| `.env` | Secrets: mail creds, DB password (gitignored, copy from example) |
| `compose.yaml` | Production stack: app + Postgres + Redis |
| `compose.test.yaml` | Test stack with dedicated Postgres + Redis, mounts source |
| `src/main/resources/application.properties` | All tunables (interval, cooldown period, DB/Redis/mail config) |

## Conventions

- Forward slash in `checkers.json` URLs  may affect Java `URI.resolve()` — some checker implementations call `setBaseUri()` with a resolved base. Don't change URL format without understanding the parsing.
- `checkers.json` entries use site-specific `"checker"` values (`cooksEdgeChecker`, `sharpKnifeShopChecker`, `staySharpChecker`) that must match the factory map in `AppConfig`.
- All configuration is externalized via `application.properties` and environment variables. No hardcoded secrets.
- `checkers.json` is mounted read-only (`:ro`) in `compose.yaml`.
