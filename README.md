# StockTracker — Automated E-Commerce Restock Monitor & Email Alerting Service

## Overview

StockTracker is a Spring Boot service that continuously polls e-commerce product listing pages, parses their HTML for
in-stock items, and emails alerts the moment a tracked product reappears. It's built for a real use case: monitoring
niche retailers (specialty knife shops) for limited restocks, but the design generalizes to any
site-scraping-and-notify workflow. The interesting engineering here is the pluggable checker architecture, a two-tier (
Redis + Postgres) cooldown system that prevents duplicate notifications while staying resilient to cache outages, and a
fully containerized, config-driven deployment model that requires no code changes to add a new target site.

## Tech Stack

| Category                | Technology                                                                             |
|-------------------------|----------------------------------------------------------------------------------------|
| **Language / Runtime**  | Java 21                                                                                |
| **Framework**           | Spring Boot 3.3 (Web, Scheduling, Mail, Data JPA, Data Redis)                          |
| **Persistence**         | PostgreSQL 16 (via Spring Data JPA / Hibernate)                                        |
| **Caching**             | Redis 7 (Spring Data Redis, JSON-serialized entities)                                  |
| **Testing**             | JUnit 5 / Spring Boot Test                                                             |
| **Containerization**    | Docker (multi-stage build), Docker Compose                                             |
| **CI-oriented Tooling** | Dedicated `compose.test.yaml` + shell script for isolated containerized test runs      |
| **Config**              | Externalized via `application.properties`, `.env`, and a hot-swappable `checkers.json` |

## Architecture Highlights

- **Strategy pattern for site-specific scraping.** `Checker` is an interface, `AbstractChecker` owns the shared
  pipeline (fetch → parse → regex-filter → cooldown-filter → notify → refresh cooldown), and each retailer (
  `CooksEdgeChecker`, `SharpKnifeShopChecker`, `StaySharpChecker`) only implements HTML parsing for its own DOM
  structure. Adding a new site means writing one class and one JSON entry, the orchestration logic is untouched.
- **Config-driven checker registry, no redeploys for new targets.** `AppConfig` builds the list of active `Checker`
  beans at startup by reading a `checkers.json` file (mounted as a read-only volume) and mapping each entry's `checker`
  field to a factory function via a `Map<String, Function<CheckerConfig, Checker>>`. New sites can be enabled/disabled
  by editing config, not code.
- **Two-tier cooldown system with graceful cache degradation.** `CooldownService` checks Redis first via
  `CooldownCacheService`, falling back to Postgres on a cache miss and repopulating the cache afterward. Every Redis
  operation is wrapped in try/catch that logs and falls back to the database rather than failing the request, a
  deliberate resilience choice so a Redis outage degrades performance, not correctness.
- **Fault-isolated scheduled execution.** `Scheduler` iterates all registered checkers on a fixed delay and catches
  exceptions per-checker, so one site's parsing failure (e.g., a broken CSS selector after a redesign) never blocks or
  crashes the checks for every other tracked site.
- **Security-conscious multi-stage Docker build.** The image separates a Maven build stage from a minimal
  `eclipse-temurin:21-jre-alpine` runtime stage, runs as a non-root user, and pulls all secrets (mail credentials, DB
  password) from environment variables rather than baking them into the image.

## Getting Started

### Prerequisites

- Docker and Docker Compose
- (For local development without Docker) JDK 21 and Maven

### Run with Docker Compose

1. Copy the example environment file and fill in credentials:
   ```bash
   cp .env.example .env
   ```
   Set `MAIL_USERNAME`, `MAIL_PASSWORD` (an SMTP app password), `NOTIFIER_RECIPIENT`, and `DB_PASSWORD`.

2. Copy the example checker config and define the products/sites to track:
   ```bash
   cp checkers.example.json checkers.json
   ```

3. Build the image and start the stack (app + Postgres + Redis):
   ```bash
   docker build -t ghcr.io/kennycyho/stocktracker:latest .
   docker compose up -d
   ```

### Run tests in an isolated container

```bash
docker compose -f compose.test.yaml up --abort-on-container-exit --exit-code-from test
docker compose -f compose.test.yaml down
```

This spins up a dedicated test compose stack (`compose.test.yaml`), runs the suite to completion, and tears the stack
down.

### Configuration reference

Key tunables live in `src/main/resources/application.properties`:

- `checker.interval-ms` — how often all checkers run (default: 30 minutes)
- `cooldown.interval-ms` — minimum time before re-notifying on the same product (default: 1 week)
- `app.checkers-file` — path to the mounted `checkers.json`

## Project Structure

```
src/main/java/app/
├── Application.java              # Spring Boot entry point
├── checker/
│   ├── Checker.java               # Strategy interface
│   ├── AbstractChecker.java       # Shared fetch → filter → notify pipeline
│   └── impl/                      # Per-retailer HTML parsing logic
├── scheduler/
│   └── Scheduler.java             # Fixed-delay job that runs all checkers, isolates failures
├── fetcher/
│   └── HttpFetcher.java           # RestClient wrapper with defensive error handling
├── cooldown/
│   ├── CooldownService.java       # Cache-then-DB cooldown lookup/refresh logic
│   ├── CooldownCacheService.java  # Redis access layer with fallback-safe error handling
│   ├── model/Cooldown.java        # JPA entity
│   └── repository/                # Spring Data JPA repository
├── notifier/
│   ├── Notifier.java               # Notification interface
│   └── impl/EmailNotifier.java     # SMTP email implementation
├── dto/                           # Product, CheckerConfig records
└── config/                        # RestClient, Redis, and checker-registry bean definitions

src/main/resources/
├── application.properties         # Scheduling, mail, DB, Redis, checker-file settings
└── schema.sql                     # Cooldown table DDL

checkers.json / checkers.example.json  # Runtime-editable list of tracked sites
Dockerfile                          # Multi-stage build, non-root runtime user
compose.yaml / compose.test.yaml    # Production and test container orchestration
```