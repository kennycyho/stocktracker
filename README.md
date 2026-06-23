# Stock Tracker

A Spring Boot application that monitors product pages for in-stock items, parses the HTML structure, tracks item notification status via PostgreSQL, and sends instant email alerts when stock becomes available.

## Core Features
- **Automated Scraping:** Periodically pulls search result pages and applies CSS query selection via Jsoup.
- **Smart Notification Filtering:** Implements a cooldown system to prevent spamming your inbox for the same product.
- **Dynamic Extensibility:** Uses a factory map pattern to register target site strategies via a central JSON file without modifying core application beans.
- **Resilient Scheduling:** Sequential execution isolation ensures a single site failure or network timeout won't block subsequent trackers.

---

## Technical Stack
- **Language:** Java 21
- **Framework:** Spring Boot 3.3 (Data JPA, Mail, Validation)
- **Database:** PostgreSQL 16
- **Parsing Library:** Jsoup
- **Deployment:** Docker / Docker Compose

---

## Project Architecture & Structure

```text
src/main/java/app/
 ├── Application.java               # Main application entry point
 ├── checker/                       
 │    ├── Checker.java              # Interface definition for checkers
 │    ├── AbstractChecker.java      # Base scraper logic, HTTP response filtering, and regex piping
 │    └── impl/                     # Site-specific scraper strategies (Jsoup parsers)
 │         ├── CooksEdgeChecker.java
 │         ├── SharpKnifeShopChecker.java
 │         └── StaySharpChecker.java
 ├── config/
 │    └── AppConfig.java            # Factory wiring mapping configs to proper Checker instances
 ├── cooldown/                      # Domain logic for managing notification state
 │    ├── CooldownService.java      
 │    ├── model/Cooldown.java       # JPA Entity for state tracking
 │    └── repository/CooldownRepository.java
 ├── dto/                           # Immutable records for configuration and items
 │    ├── CheckerConfig.java
 │    └── Product.java
 ├── fetcher/
 │    └── HttpFetcher.java          # Null-safe HttpClient wrapper with request timeouts
 ├── notifier/                      # Alerts system
 │    ├── Notifier.java
 │    └── impl/EmailNotifier.java   # Spring Mail integration with cooldown validation
 └── scheduler/
      └── Scheduler.java            # Scheduled orchestration engine
```

---

## Component Behavior & Strategy

### 1. Scraping Layer (`AbstractChecker`)
Individual site scrapers inherit from `AbstractChecker`. The network layer (`HttpFetcher`) evaluates response codes safely.
- **Status 200:** Passes content forward to the parser.
- **Status 5xx:** Suppressed into an informational log entry.
- **Other Status:** Generates an application error flag.
- Custom regex filtering is applied down the stream via `CheckerConfig.regexFilter()`.

### 2. Wiring & Factory Setup (`AppConfig`)
The trackers are configured decoupled from Spring context lifecycle events. `AppConfig` parses a `checkers.json` file inside the resources directory using Jackson. It initializes them using a functional factory pattern:
```java
Map<String, Function<CheckerConfig, Checker>>
```
Adding support for a new storefront requires creating a specialized implementation under `checker/impl/` and updating the factory lookup.

### 3. Cooldown & Persistence (`CooldownService`)
To protect against high-frequency email delivery, found links are persisted inside a PostgreSQL `COOLDOWN` schema.
- **Validation:** An alert triggers only if a product URL has never been cataloged, or if it is outside the expiration window.
- **Default Window:** 1 week (Overridden via `cooldown.interval-ms`).

---

## Local Configuration

### Configuration Files
- **`src/main/resources/application.properties`:** Manages DB initialization states and maps environment property keys.
- **`src/main/resources/checkers.json`:** Stores target site registration properties:
  ```json
  [
    {
      "name": "Example Checker",
      "checker": "checkerImpl",
      "url": "https://example-site.com/search?q=example",
      "regexFilter": "example"
    }
  ]
  ```

### Local Environment Setup
Create a `.env` file in the root directory containing your credentials:

```env
DB_PASSWORD=your_secure_db_password
MAIL_USERNAME=your_sender_email@gmail.com
MAIL_PASSWORD=your_email_app_password
NOTIFIER_RECIPIENT=your_alert_destination@domain.com
```

---

## How to Run Locally

Launch the multi-container setup via Docker Compose:

```bash
docker compose up -d
```

This triggers the Postgres image, builds/runs the application image using a multi-stage optimized Dockerfile, executes `schema.sql` to initialize tables, and sets the schedule tracker intervals (defaulting to 30 minutes).

To view runtime tracking logs:
```bash
docker compose logs -f stocktracker
```
