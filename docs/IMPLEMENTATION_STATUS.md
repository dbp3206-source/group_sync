# Implementation Status

## Current checkpoint

Phase 1 - Foundation Application (Authentication + Group Core): implemented; backend/frontend builds and the PostgreSQL-backed runtime golden flow are green.

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

## Verification

- Backend tests: PASS, 5 tests passed (health, auth service, and group permission/ownership rules).
- Backend package: PASS, `backend/target/backend-0.0.1-SNAPSHOT.jar` produced after the Phase 1 changes.
- Frontend: `npm.cmd run build` - PASS, TypeScript build and Vite production build passed.
- Java compiler evidence: Maven compiled with `javac [debug parameters release 21]` and tests ran on Java `21.0.12`.
- PostgreSQL: PASS, `pg_isready` reports `127.0.0.1:54329 - accepting connections`.
- Flyway: PASS, `V1__foundation_baseline.sql` applied and recorded in `flyway_schema_history`.
- Runtime backend: PASS, live `GET /api/health` returned HTTP 200.
- Runtime frontend: PASS, Vite served React app, Vite proxy returned HTTP 200 for `/api/health`, and headless Chrome rendered `Backend connected` / `groupsync-backend - UP`.
- Phase 1 runtime: PASS, real HTTP flow completed register A/B, login A/B, current user, create STUDY and OTHER groups, invite, pending invitation, accept, promote to ORGANIZER, member list, logout (204), and post-logout current user rejection (401).
- Frontend runtime: PASS, Vite served the React login page and headless Chrome rendered the real `Welcome back` auth screen.

## Known limitations

- Calendar, Study session, and Badminton operational features remain intentionally out of scope for this checkpoint.
- Invitation email delivery is intentionally not implemented; invitations target existing GroupSync accounts and are stored in PostgreSQL.
- Session storage is the default local Spring session; production deployment hardening is a later infrastructure concern.
- Maven Wrapper `.cmd` currently has a Windows/PowerShell invocation quirk in this environment, so verification used the Maven 3.9.16 distribution downloaded by the wrapper directly from `.m2/wrapper/dists`.

## Next task

Phase 1 Foundation Application is complete. The next implementation task can begin with the next planned shared/core phase; Calendar, Study, and Badminton remain untouched.
