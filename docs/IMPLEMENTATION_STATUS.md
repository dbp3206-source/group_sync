# Implementation Status

## Current checkpoint

Phase 5 - Integration and Polish: complete; backend/frontend builds and the PostgreSQL-backed golden runtime flow are green.

## Completed

- Local Git repository initialized with `origin` set to the GroupSync GitHub repository.
- Foundation `AGENTS.md`, docs, prompts, and reference notes preserved.
- Root `.gitignore`, `README.md`, and `.env.example` added.
- Backend Spring Boot 4.1.0 scaffolded with Java 21 Maven Wrapper, REST/Web MVC, JPA, Security, Validation, PostgreSQL driver, Flyway, and test dependencies.
- `GET /api/health` implemented as a public JSON endpoint.
- Basic Spring Security filter chain added; all non-health endpoints remain protected for future phases.
- Flyway baseline added without domain tables.
- Frontend React + TypeScript + Vite scaffolded with React Router, Axios API wrapper, Bootstrap dependency, and a responsive health page.
- Vite `/api` proxy configured for the local backend.
- PostgreSQL 17.10 local development runtime configured on `127.0.0.1:54329`.
- Development database `groupsync_dev` and role `groupsync` created.
- Simple Authentication implemented with register, login, current user, logout, BCrypt password hashing, server-side session cookie, and CSRF token endpoint.
- Stable JSON error shape added for API/domain/validation/authentication/permission failures.
- User accounts support system roles `USER` and `ADMIN`; public responses never include password hashes.
- Group Core implemented with `STUDY`, `BADMINTON`, and `OTHER` types, creator ownership, memberships, invitations, role changes, ownership transfer, and owner-leave guard.
- Group roles are `OWNER`, `ORGANIZER`, and `MEMBER`; write permissions are checked in `GroupService`.
- React auth context, login/register pages, protected routing, groups list/create, invitation acceptance, group detail/member list, invite, and organizer promotion UI added.
- Personal Calendar implemented with owned one-time busy events and weekly recurring schedules; recurrence occurrences are expanded at query time in the schedule timezone.
- Source-aware Calendar Aggregator implemented with `MANUAL`, `RECURRING`, and `STUDY` calendar items, bounded range queries, and half-open conflict detection.
- Availability Engine implemented with 30-minute candidate grid, required-member filtering, 14-day bound, `MaximumAttendanceStrategy`, and `EarliestPossibleStrategy`.
- Study Group implemented with `OPEN`, `CONFIRMED`, `COMPLETED`, and `CANCELLED` lifecycle, capacity, join/leave, goals, materials, attendance, reschedule, and cancellation.
- Confirmed Study participants contribute derived busy items directly from Study participation; reschedule and cancel are reflected without duplicate calendar rows or manual user edits.
- React calendar page supports one-time events, weekly schedules, source labels, and derived Study items; Study page supports session creation, availability suggestions, join, confirm, and cancel.
- Badminton profile is scoped to a group membership; no rating or skill data was added to the global user.
- Badminton foundation includes a group-scoped default Season, Venue/Court management, session lifecycle, selected courts, registration deadline, and configurable capacity defaulting to 16.
- Registration uses one row per session/user, server-side duplicate prevention, pessimistic session locking, FIFO waitlist promotion, conflict warnings, check-in, no-show, and lightweight session responsibilities.
- Badminton calendar items are derived from active registered/check-in participants and disappear or move automatically when a session is cancelled or rescheduled.
- In-app notification storage and endpoints cover waitlist promotion and badminton session confirmation/change/cancellation side effects.
- React Badminton operations page supports group selection, venue/court-backed session creation, lifecycle actions, join/leave, capacity and waitlist/conflict display.
- Court allocation assigns checked-in players to active courts in deterministic round order and supports draft/confirmed allocation state.
- Pairing strategies include seeded Random and skill-weighted Balanced suggestions; Manual pairing is supported by explicit match creation.
- Doubles-first Match and MatchSide models support one or two players per side, with SCHEDULED, PLAYING, RESULT_SUBMITTED, CONFIRMED and CANCELLED lifecycle states.
- Confirmed match results derive the winner, apply simple 3/1 ranking points idempotently, write ranking history, and update player matches/wins/losses/attendance/recent form statistics.
- Match confirmation publishes system news and idempotent participant notifications; organizer announcements and READ/UNREAD notification inbox endpoints are available.
- Group dashboard exposes next activities, registration count, recent matches, leaderboard and news; React now includes dashboard and notification pages.
- Integration pass added group invitation notifications, unified Study/Badminton home dashboard handling, Badminton session detail and player ranking-history endpoints/pages, and stable `400 INVALID_REQUEST` handling for malformed request parameters.

## Verification

- Backend tests: PASS, 23 tests passed, including pairing invariants, match lifecycle, ranking idempotency, and event-driven news/notification behavior.
- Backend package: PASS, `backend/target/backend-0.0.1-SNAPSHOT.jar` produced after the Phase 4 changes.
- Frontend: `npm.cmd run build` - PASS, TypeScript build and Vite production build passed with Badminton, dashboard and notification UI integration.
- Java compiler evidence: Maven compiled with `javac [debug parameters release 21]` and tests ran on Java `21.0.12`.
- PostgreSQL: PASS, `pg_isready` reports `127.0.0.1:54329 - accepting connections`.
- Flyway: PASS, V1 through V5 validated; `V5__badminton_match_stats_news.sql` applied and recorded in `flyway_schema_history`.
- Runtime backend: PASS, live `GET /api/health` returned HTTP 200.
- Runtime frontend: PASS, Vite served React app, Vite proxy returned HTTP 200 for `/api/health`, and headless Chrome rendered `Backend connected` / `groupsync-backend - UP`.
- Phase 1 runtime: PASS, real HTTP flow completed register A/B, login A/B, current user, create STUDY and OTHER groups, invite, pending invitation, accept, promote to ORGANIZER, member list, logout (204), and post-logout current user rejection (401).
- Frontend runtime: PASS, Vite served the React login page and headless Chrome rendered the real `Welcome back` auth screen.
- Phase 2 migration/runtime: PASS, Flyway V3 applied to PostgreSQL 17.10 and Hibernate started with 10 repository interfaces.
- Phase 2 runtime: PASS, real HTTP flow created a manual busy event and weekly schedule, returned recurring calendar items, searched availability, created/joined/confirmed a Study session, exposed a derived Study calendar item, reflected reschedule immediately, and removed the derived item after cancellation.
- Phase 3 migration/runtime: PASS, Flyway V4 applied to PostgreSQL 17.10 and Hibernate started with 18 repository interfaces.
- Phase 3 runtime: PASS, real HTTP flow created a BADMINTON group with default Season 1, venue and court, created/opened/confirmed a capacity-16 session, registered a member, and returned a derived `BADMINTON` calendar item.
- Phase 4 runtime: PASS, real HTTP flow created four players, checked them in, generated court allocation, generated balanced doubles pairing, created a match, submitted and confirmed a result, then verified leaderboard rows, ranking-derived statistics, system news, participant notifications, and dashboard recent results.
- Phase 5 golden runtime: PASS, live HTTP flow verified 17 badminton actors, 16 active registrations plus FIFO waitlist promotion, 4-court allocation for 16 checked-in players, four balanced 2-vs-2 pairings, match confirmation, ranking/history/statistics/news/dashboard/session-detail responses, duplicate-registration `409`, member authorization `403`, malformed-parameter `400`, and invitation notification creation.
- Phase 5 frontend runtime: PASS, Vite served the React app and proxied `/api/health` with HTTP 200; headless Chrome rendered the real login screen.

## Known limitations

- Google Calendar and other external calendar synchronization remain intentionally out of scope.
- Google Calendar sync, tournament brackets, and advanced ranking algorithms remain intentionally out of scope.
- Pairing is deliberately lightweight: Random and Balanced suggestions are available, while manual pairing is entered only when creating a match.
- The current frontend remains an MVP operations UI; advanced charting, partner/head-to-head analytics and real-time updates are intentionally deferred.
- Invitation email delivery is intentionally not implemented; invitations target existing GroupSync accounts and are stored in PostgreSQL.
- Session storage is the default local Spring session; production deployment hardening is a later infrastructure concern.
- Maven Wrapper `.cmd` currently has a Windows/PowerShell invocation quirk in this environment, so verification used the Maven 3.9.16 distribution downloaded by the wrapper directly from `.m2/wrapper/dists`.

## Next task

Phase 5 is complete. The next planned boundary is tournament/advanced badminton analytics only if explicitly approved; Google Calendar sync and other out-of-scope integrations remain deferred.
