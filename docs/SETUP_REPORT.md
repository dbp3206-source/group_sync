# GroupSync Setup Report

## Scope

This report records the Phase 0 repository bootstrap and PostgreSQL runtime verification performed on 2026-08-11. No User, Group, Calendar, Study, or Badminton business feature was implemented.

## Machine and toolchain

| Tool | Result |
|---|---|
| OS | Windows 10, amd64 (reported by Maven runtime; WMI OS query was access-denied in the sandbox) |
| Git | 2.53.0.windows.2 |
| Node.js | v24.15.0 |
| npm | 11.12.1 via `npm.cmd` (PowerShell script execution policy blocks `npm.ps1`) |
| Java system runtime | 26.0.2; not used for the backend checkpoint |
| Java project runtime | Temurin OpenJDK 21.0.12 portable, stored in ignored `.jdk21-runtime/` |
| Maven | 3.9.16, downloaded by Maven Wrapper under `.m2/wrapper/dists` |
| PostgreSQL | 17.10 local development runtime on `127.0.0.1:54329` |
| Docker / Docker Compose | Not available |
| winget / Chocolatey | Not available |
| Browser verification | Google Chrome headless rendered the React app |

## Setup decisions

- The current workspace was initialized as the local Git repository and connected to `https://github.com/dbp3206-source/group_sync.git` as `origin`.
- The foundation kit and supplied planning documents were copied into `AGENTS.md`, `docs/`, `prompts/`, and `reference/`.
- Backend uses Spring Boot `4.1.0`, Java release `21`, Maven Wrapper, Web MVC, JPA, Security, Validation, PostgreSQL, and Flyway.
- Frontend uses React, TypeScript, Vite, React Router, Axios, and Bootstrap package availability. Vite proxy forwards `/api` to `http://localhost:8080`.
- PostgreSQL configuration defaults to port `54329` to avoid colliding with a normal local PostgreSQL instance on `5432`.
- The active development database is `groupsync_dev`, owned by role `groupsync`.
- Secrets are supplied through environment variables or an ignored `.env`; `.env.example` contains placeholders only.
- A Flyway `V1__foundation_baseline.sql` contains only `SELECT 1` because Phase 0 intentionally has no domain tables.

## Commands and evidence

1. Read the supplied foundation kit, `IMPLEMENTATION_PLAN.md`, `PROJECT_DECISIONS.md`, `RISK_AND_SCOPE_GUARD.md`, and PeSoc reference README/pom before coding.
2. Verified the GitHub remote with `git ls-remote`.
3. Generated the backend from Spring Initializr and the frontend from Vite.
4. Ran `npm.cmd install` and `npm.cmd run build` successfully.
5. Ran backend tests with JDK 21 successfully: 1 test, 0 failures, 0 errors.
6. Ran backend package successfully and produced `backend/target/backend-0.0.1-SNAPSHOT.jar`.
7. Verified PostgreSQL with `pg_isready`: `127.0.0.1:54329 - accepting connections`.
8. Verified Flyway created `public.flyway_schema_history` and applied `V1__foundation_baseline.sql`.
9. Verified live backend `GET /api/health` returned HTTP 200 with `groupsync-backend` and `UP`.
10. Verified Vite proxy `GET http://127.0.0.1:5173/api/health` returned HTTP 200.
11. Rendered the React app in headless Chrome and confirmed the page displayed `Backend connected` and `groupsync-backend - UP`.

## PostgreSQL setup notes

The machine initially had no PostgreSQL, Docker, or package manager. Docker was skipped because it was not installed. A PostgreSQL Windows installer attempt unpacked partial files under `C:\Program Files\PostgreSQL\17`, but did not complete cleanly enough to use that install as the project runtime.

The working development runtime uses PostgreSQL 17.10 on `127.0.0.1:54329`. Application credentials are supplied through environment variables:

```text
DB_URL=jdbc:postgresql://localhost:54329/groupsync_dev
DB_USERNAME=groupsync
DB_PASSWORD=<local development password>
```

The local password was used only in process environment variables and database role setup; it was not committed to source.
