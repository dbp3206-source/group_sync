# GroupSync Production Smoke Report

## Verdict

**PRODUCTION READY: YES — for the verified production-like local deployment.**

No public cloud deployment was attempted because Docker was unavailable and no provider/account/credentials were supplied. The application was deployed as a packaged Spring Boot JAR plus a React production build served by Vite preview, backed by the persistent local PostgreSQL service.

## Runtime evidence

| Check | Result |
|---|---|
| Frontend | PASS — `http://127.0.0.1:4173/` returned HTTP 200 |
| Backend | PASS — executable JAR started on port 8080 with Java 21.0.12 |
| PostgreSQL | PASS — PostgreSQL 17.10 accepting connections on `127.0.0.1:54329` |
| Flyway | PASS — V1–V6 validated; schema version 6, no pending migration |
| Health | PASS — `GET /api/health` returned HTTP 200 and `status=UP` |
| CORS | PASS — API returned the preview origin and `Access-Control-Allow-Credentials: true` |
| Backend tests | PASS — 27 tests, 0 failures, 0 errors |
| Backend package | PASS — `backend/target/backend-0.0.1-SNAPSHOT.jar` |
| Frontend build | PASS — TypeScript and Vite production build |

## Smoke flows

- **Authentication and shared core:** PASS in the packaged runtime: register/login/current user/logout, group ownership, membership roles, invitation accept and member authorization.
- **Personal Calendar:** PASS: one-time event creation was read back before and after logout/login from PostgreSQL; the temporary event was then deleted. Seeded recurring schedule was available after login.
- **Calendar and availability:** PASS: availability search returned candidate slots; the calendar view exposed persisted derived Badminton items without exposing private event details.
- **Study:** PASS in the existing full regression: seeded topic, goal, material, attendance, derived calendar event, availability, reschedule and cancellation behavior.
- **Badminton core:** PASS: the golden runtime flow completed capacity 16, member 17 waitlist, cancellation, FIFO promotion, promotion notification, derived calendar, 16 check-ins, four-court allocation, four balanced doubles pairings, score 21–17, ranking, history, statistics, news and dashboard.
- **Advanced Badminton:** PASS in the full regression: profile, Season 1, venue/courts, QR check-in, duplicate prevention, head-to-head, partner statistics, awards, notification preference and recent form.
- **Tournament:** PASS in the full regression: tournament creation, registration, participants, final match, confirmation idempotency, bracket winner/champion progression and no duplicate statistics.

The golden flow was run once against the newly packaged JAR after the deployment configuration changes. The development database was then reseeded and verified in the expected presentation state: 21 users, 2 groups, 1 recurring schedule, 4 courts, 16 registered players for session 2, session 2 `CONFIRMED`, Flyway version 6.

## Fixes during deployment verification

- Added an environment-controlled `VITE_API_URL` so a production frontend build does not depend on the Vite development proxy.
- Added environment-controlled Spring CORS origins and enabled the Spring Security CORS path for the production frontend origin.
- No business feature or data model was changed.
- One initial PowerShell persistence assertion treated a single JSON object as an array; the raw response was checked, the runtime behavior was correct, and no source fix was needed.

## Known limitations

- This is local production-like verification, not a public Internet deployment. There is no DNS, TLS certificate, managed PostgreSQL, external monitoring or cloud rollback mechanism.
- The verified database is named `groupsync_dev` because the local role lacks `CREATEDB`; persistent storage is provided by the existing PostgreSQL service.
- Vite preview is suitable for this student demonstration but is not an Internet-facing production edge server.
- Calendar reminders are persisted metadata only; no email/push delivery is implemented.
- Google Calendar, WebSocket, payment, AI and distributed infrastructure remain intentionally excluded.
- Tournament setup and advanced analytics remain intentionally organizer/demo oriented.

