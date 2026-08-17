# GroupSync Production QA Report

## Result

- Public URL: https://group-sync-khaki.vercel.app
- Backend URL: https://groupsync-backend-h68s.onrender.com
- Final status: `PUBLIC_SMOKE_PASS` and `PUBLIC_BROWSER_SMOKE_PASS`
- Production fix commit: `b7a16cd`
- Browser wait-hardening commit: `e8fbe47`

## Production fixes verified

- Frontend API traffic uses the Vercel same-origin `/api` proxy, so browser sessions and CSRF cookies no longer depend on third-party cookie behavior.
- Render serves PostgreSQL-backed Spring Boot successfully.
- Registration, login, logout and session restoration work through the public Vercel URL.
- New users no longer receive a non-existent avatar URL before uploading an avatar, eliminating the background `404` request found by the browser test.

## API workflow verification

The production smoke test created isolated test users and exercised:

1. deployment health and unauthenticated access control;
2. registration, profile update, avatar upload, logout and login;
3. personal events, recurring schedules, conflict detection, duplication and deletion;
4. Study Group invitation, joining, materials, goals, attendance and lifecycle;
5. Badminton Group profiles, venue/courts, registration, check-in and responsibilities;
6. court allocation, pairing, match scoring, result confirmation and derived ranking;
7. tournament creation, entries, bracket progression and champion selection;
8. availability search, notifications and preferences;
9. aggregated personal, study and badminton calendar data.

Result: all 10 API areas passed against the public URL.

## Browser verification

Playwright opened the real production deployment and completed UI registration, mandatory profile setup with avatar, logout and login. It then opened all primary routes at 1440×1000 and the five main product views at 390×844.

- Console errors: 0
- Page runtime errors: 0
- Unexpected API 4xx/5xx responses: 0
- Horizontal overflow: 0 routes
- Keyboard focus smoke check: passed
- Data loading: assertions and screenshots run only after browser network idle

Verified routes include dashboard, calendar, groups, both group details, availability, study, badminton, session detail, badminton profile, tournaments, notifications, profile and health.

## Build verification

- Backend: 32 tests, 0 failures, 0 errors; Maven build successful.
- Frontend: TypeScript and Vite production build successful.
- Vite reports one non-blocking bundle-size warning for the main JavaScript chunk.

## Evidence

- Browser JSON report: `design-work/qa/public-browser-smoke.json`
- Screenshots: `design-work/qa/screenshots/`
- Reusable API smoke test: `scripts/public-smoke.ps1`
- Reusable browser smoke test: `scripts/public-browser-smoke.mjs`

## Remaining hosting limitation

The Render free instance can spin down after inactivity. The first request after a quiet period may take roughly 50 seconds or more while the backend wakes up. This is a hosting-plan limitation; subsequent requests work normally. A paid always-on Render instance would remove that cold-start delay.

## Test data note

The production checks created isolated accounts and groups prefixed with `smoke`, `browser` and `deploy.probe`. They are not visible to ordinary users unless explicitly invited, but remain in the production database because the application intentionally has no destructive admin cleanup endpoint.
