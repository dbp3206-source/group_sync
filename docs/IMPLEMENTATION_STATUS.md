# Implementation Status

## KnowledgeOS acceptance checkpoint

- Branch: `codex/knowledgeos-migration`.
- Search now supports title, Vietnamese title, tag, collection, combined AND filters, clear/reset, no-result state, and server-side owner isolation.
- Smart Organization is a reviewed flow in Resource Workspace. It proposes normalized tags, existing/new collections, and semantic related resources; only confirmed selections are persisted.
- RAG hardening separates trusted application rules from untrusted retrieved text, reports insufficient context for weak evidence, preserves scope filters, and asks Gemini to answer in the question language when practical.
- Live benchmark: 5 controlled cases executed through parser, Gemini embeddings, pgvector retrieval, Gemini generation, and citation mapping. Recall@5 1.000, MRR 1.000, citation validity 1.000, grounded answer rate 1.000, scope leakage 0, unsupported hallucinations 0, Vietnamese PASS, prompt-injection PASS.
- Controlled dataset: 25 version-controlled cases in `qa/fixtures/rag-cases.json`; the live benchmark is intentionally smaller to keep provider usage bounded while retaining direct-fact, Vietnamese, injection, and scope coverage.

## KnowledgeOS migration

- Migration branch: `codex/knowledgeos-migration` from verified GroupSync baseline `01c1297`.
- Knowledge foundation: V9 adds user-owned resources, collections, tags, resource links, notes, and learning progress without changing V1-V8.
- Resource import: controlled local `StorageService`, ownership-scoped resource CRUD, duplicate checksum protection, note resources, and protected content access are implemented.
- Next: parser/chunking lifecycle, then V10 pgvector and the Gemini-backed retrieval slice.

## Current checkpoint

Current redesign and workflow checkpoint: Flyway V8 is applied. The product now includes profile completion/avatar storage, a responsive application shell, FullCalendar, a dedicated group-availability workspace, and organizer-managed Singles/Doubles knockout entries with automatic byes and bracket progression. The packaged backend JAR and production frontend build were verified against the persistent local PostgreSQL service. Google Calendar remains intentionally excluded.

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
- Final hardening fixed the verified Critical gaps: Badminton venue/court and multi-court UI, Study availability timezone conversion, Badminton lifecycle guards, and responsibility unassignment notifications.
- Final hardening added equal-side/round/player match invariants, match row locking, stable invalid-state errors, valid large-pool pairing output, confirmed-only dashboard results, correct news target IDs, and a working Windows Maven Wrapper invocation.
- Final demo preparation added `scripts/reset-demo.ps1` for reproducible local seed data and `scripts/demo-golden.ps1` for the real end-to-end demonstration flow.
- Final demo data includes 21 local demo users, Study and Badminton groups, recurring/busy calendar data, a Study rehearsal session, Season 1, one venue with four courts, a capacity-16 near-full session, historical match/ranking/news data, and notifications.
- Final presentation guides added: `docs/DEMO_GUIDE.md` and `docs/OOP_DEFENSE_GUIDE.md`.
- Full feature pass added V6 incremental schema changes for calendar metadata, notification preferences, QR check-in tokens and tournament state.
- Full feature pass added calendar edit/duplicate metadata, daily recurrence, optional season ranking strategy, advanced player analytics, round-robin responsibilities, QR token check-in and tournament registration/bracket/champion derivation.
- Product redesign added a Vietnamese responsive app shell, improved login/register/profile flows, PostgreSQL-backed compressed avatar storage, visual group workspaces, FullCalendar, and an organizer-only availability screen that sends a selected slot directly to Study or Badminton creation.
- Tournament redesign added explicit `SINGLES` and `DOUBLES` entries, organizer roster/seed management, power-of-two knockout generation, automatic bye advancement, server-side winner recording and champion completion. The former participant-only tournament model remains migration-compatible for existing data.

## Verification

- Backend tests: PASS after the full feature pass, including pairing invariants, match lifecycle, ranking idempotency, strategy behavior, and event-driven news/notification behavior.
- Backend package: PASS, `backend/target/backend-0.0.1-SNAPSHOT.jar` produced after the full feature changes.
- Frontend: `npm.cmd run build` - PASS, TypeScript build and Vite production build passed with Badminton, dashboard and notification UI integration.
- Java compiler evidence: Maven compiled with `javac [debug parameters release 21]` and tests ran on Java `21.0.12`.
- PostgreSQL: PASS, `pg_isready` reports `127.0.0.1:54329 - accepting connections`.
- Flyway: PASS, V1 through V8 validated; V7 user profile/avatar storage and V8 tournament-entry/knockout schema were applied and recorded in `flyway_schema_history`.
- Redesign runtime: PASS; local backend health returned HTTP 200 after V8 validation and Vite served the rebuilt production frontend on `127.0.0.1:4173`.
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
- Final hardening verification: PASS, 25 backend tests, backend package, frontend build, Flyway V1-V5 startup, live health, pairing 2-vs-2, ranking/history/dashboard response, confirmed-match mutation rejection `409`, and Vite proxy HTTP 200.
- Final demo verification: PASS, `reset-demo.ps1` seeded 21 users and 2 groups; `demo-golden.ps1` verified waitlist promotion, notification, derived calendar, 16 check-ins, 4 allocations, 4 balanced 2-vs-2 pairings, score 21-17, ranking/history/statistics/news/dashboard; final database reseed restored 16 registered players and Flyway version 6.
- Final frontend runtime: PASS, Vite served the React app on an available local port and proxied `/api/health` with HTTP 200.
- Final rehearsal: PASS before this feature pass; the feature pass also smoke-tested calendar metadata/duplicate, notification preferences and QR token/idempotent check-in on the live PostgreSQL-backed application.
- Full feature runtime: PASS after V6 startup; `reset-demo.ps1` plus `demo-golden.ps1` passed again, and a real FINAL tournament match confirmation updated bracket winner and tournament champion automatically.
- Final regression/hardening: PASS; shared Core, Personal Calendar persistence/privacy, Study derived reschedule/cancel, Badminton advanced analytics, QR validity/idempotency, Tournament champion progression, notification preferences and critical invariants were checked against the live PostgreSQL runtime.
- Production-like deployment: PASS; Java 21 packaged JAR, React/Vite production build, PostgreSQL 17.10, Flyway V1-V6 startup validation, environment-controlled API URL, CORS and live health were verified. The golden runtime flow passed after deployment configuration changes, then the development database was reseeded to the stable demo state. See `docs/DEPLOYMENT_GUIDE.md` and `docs/PRODUCTION_SMOKE_REPORT.md`.
- Authentication usability hardening: PASS; local email/Gmail addresses work with the existing password-based auth, browser autofill/recent-email suggestions and demo-account shortcut. Localhost/127.0.0.1 API host normalization, local-port CORS patterns and clearer validation/network errors were verified without adding OAuth.
- Authentication runtime fix: PASS; the reported registration failure was reproduced as a stopped backend while a stale frontend remained visible. `scripts/start-groupsync.ps1` now starts/reuses PostgreSQL-backed Spring Boot first, waits for health, builds the frontend with a same-origin `/api` preview proxy, and starts/reuses Vite only after backend readiness. A clean stop/start plus browser-origin register, login, current-user, logout and re-login flow passed with no backend/frontend error log entries.

## Known limitations

- KnowledgeOS Step 2 completion checkpoint: the Spring Boot resource pipeline supports Gemini embeddings, pgvector retrieval, persistent chat/citations, deterministic Focus Next, and real Insights. The workspace exposes authenticated collections, collection-scoped retrieval, persisted chat history, notes, explicit related resources, truthful reading-progress/activity data, file import, and responsive KnowledgeOS routes. Resource ingestion uses a committed post-upload transaction with Gemini embedding and pgvector persistence.

- Google Calendar and other external calendar synchronization remain intentionally out of scope.
- Google Calendar sync and advanced ranking algorithms remain intentionally out of scope.
- Pairing is deliberately lightweight: Random and Balanced suggestions are available, while manual pairing is entered only when creating a match.
- The current frontend remains an MVP operations UI; advanced charting, partner/head-to-head analytics and real-time updates are intentionally deferred.
- Invitation email delivery is intentionally not implemented; invitations target existing GroupSync accounts and are stored in PostgreSQL.
- Session storage is the default local Spring session; production deployment hardening is a later infrastructure concern.
- The local demo depends on the PostgreSQL development service and its local password being supplied through environment variables; neither is committed.
- The golden flow mutates session state to PLAYING and creates a result. Run `scripts/reset-demo.ps1` before each rehearsal; the final checkpoint database was reseeded to the near-capacity starting state.
- Tournament winner recording is organizer-controlled. It currently records the bracket result directly; connecting each bracket match to a detailed Badminton scorecard is a later enhancement.
- Calendar reminder minutes are persisted metadata only; no external reminder delivery is implemented.
- Tournament does not create a separate calendar source; it reuses Badminton session/match calendar data.

## Next task

Deployment-specific work is complete for the local production-like target. A public provider deployment would require a chosen provider, account authorization, managed database, DNS/TLS and secret provisioning. Google Calendar and other explicitly excluded integrations remain out of scope.
