# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

- Run: `docker compose up -d db && ./mvnw spring-boot:run` (Postgres), or `SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run` (H2, no Docker).
- Test: `./mvnw test` (Java, 25 tests) and `npm test` (Jest+jsdom, 45 tests) — both must stay green.
- Layout: Spring/JDBC backend in `src/main/java/com/marlowefinch/ops/` (controllers → `DashboardRepository`/`VendorRepository`, plain SQL, no JPA); vanilla-JS frontend in `src/main/resources/static/` (`app.js`); Flyway migrations + seed in `src/main/resources/db/migration/`.
- App clock is pinned to `2026-09-21` (`ClockConfig`) so seed data and test assertions use exact numbers, not ranges.
- New element id in `index.html` → also add it to `REGISTERED_IDS` in `src/test/javascript/setup/loadApp.js` or the harness test fails.
- `DateRange` query params are deliberately unvalidated (malformed date = 500) — this is TODO-232, not a bug to fix incidentally.
- `pom.xml` dependencies are frozen. Any change needs a CHG ticket.
