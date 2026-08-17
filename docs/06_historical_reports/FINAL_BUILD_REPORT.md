# Final Hardening Build Report

## Review findings verified

The independent review was read and checked against the current source, tests, migrations and
runtime. Findings were not implemented mechanically. The four Critical findings were confirmed
as real correctness/demo-readiness gaps. Important findings were fixed only where the change was
small, directly protective of existing behavior, and within the current scope.

## Critical issues fixed

1. Badminton UI now creates venues and courts, selects multiple courts for a session, links to
   session detail, and only offers match creation for valid pairing sides.
2. Study availability candidates are converted from UTC `Instant` values to local
   `datetime-local` values before being submitted, preserving the selected wall-clock time.
3. Badminton registration cancellation is closed after `OPEN`/`CONFIRMED`; check-in/no-show is
   restricted to `CONFIRMED`/`PLAYING`. This prevents post-completion attendance/statistics drift.
4. Cancelling an assigned Badminton registration unassigns the responsibility and notifies group
   organizers that the item is needed again.

## Important issues fixed

- Match creation now requires equal 1-vs-1 or 2-vs-2 sides, checks the session is `PLAYING`, and
  prevents a checked-in player from appearing in another match in the same session round.
- Match start/submit/confirm now uses a pessimistic row lock; confirmed-match retries remain
  idempotent and concurrent confirmation is serialized at the database transaction boundary.
- Result submission now requires `PLAYING`; invalid entity transitions and invalid arguments are
  returned as stable `409 INVALID_STATE` or `400 INVALID_REQUEST` responses.
- Pairing suggestions for a large court pool produce one valid doubles match per court allocation
  and leave extra players unassigned instead of returning an invalid 8-vs-8 match shape.
- Dashboard recent results now filters to confirmed matches, and system news target IDs derive
  the source match ID from the existing source key.
- The Windows Maven Wrapper PowerShell null-array check was corrected; `backend/mvnw.cmd
  -version` now works in this environment.
- Added focused regression tests for large-pool pairing, result lifecycle and responsibility
  unassignment. The suite increased from 23 to 25 tests.

## Findings intentionally rejected

- No architecture rewrite, shared-module extraction, DDD/Clean Architecture migration,
  distributed lock, scheduler, polling, OAuth, tournament, Google Calendar or other new feature
  was added. These are outside a hardening pass and contradict the repository scope guard.
- Full PostgreSQL concurrency integration tests, Testcontainers and fresh-database test fixtures
  were not added. They would add infrastructure and setup complexity; the production match row
  lock and existing PostgreSQL runtime smoke provide the proportionate safeguard for this pass.
- Study notifications, full Study leave/attendance/material UI, profile editing, remove-member,
  transfer/decline UI, advanced availability controls and responsibility management UI remain
  deferred. Their backend/API behavior was not needed to correct a current data-integrity bug.
- Calendar interval merging, reminders, frontend polling, manual allocation correction and
  advanced analytics remain intentionally deferred. The existing item-based conflict behavior is
  sufficient for the current MVP and changing the calendar contract would expand scope.
- `OTHER` remains in the existing foundation enum for backward compatibility; no new vertical
  behavior was added for it.
- Nice-to-have formatting and lint-warning cleanup was not performed because it does not affect
  correctness or scope.

## Verification

- Backend tests: PASS, 25 tests, 0 failures.
- Backend package: PASS, Java 21 target, Spring Boot JAR produced.
- Frontend build: PASS, TypeScript and Vite production build.
- PostgreSQL: PASS, `pg_isready` reports `127.0.0.1:54329 - accepting connections`.
- Flyway/database startup: PASS, V1 through V5 validated; schema version 5 is up to date.
- Runtime golden smoke: PASS. Live checks returned health `UP`, 4 balanced 2-vs-2 pairings,
  leaderboard/history/dashboard data, and `409` for mutation of a confirmed match.
- Frontend runtime: PASS, Vite page and `/api/health` proxy returned HTTP `200`.
- Working tree: verified clean after checkpoint commit.

## Known limitations

- The frontend remains an MVP operations UI; Study operational controls, richer Badminton
  responsibility management, advanced analytics, notifications polling and full integration test
  infrastructure remain deferred.
- External calendar synchronization, tournaments, advanced ranking algorithms, email delivery,
  realtime transport and production deployment hardening remain out of scope.

## Core Stable verdict

**CORE STABLE: YES**

The Critical review findings that affected correctness or the flagship demo path were fixed and
verified. Remaining items are documented scope limitations or proportionate test/developer-
experience improvements, not unresolved Critical issues.
