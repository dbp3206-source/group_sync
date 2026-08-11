# GroupSync Setup Report

## Scope

This report records the Phase 0 repository bootstrap performed on 2026-08-11. No User, Group, Calendar, Study, or Badminton business feature was implemented.

## Machine and toolchain

| Tool | Result |
|---|---|
| OS | Windows 10, amd64 (reported by Maven runtime; WMI OS query was access-denied in the sandbox) |
| Git | 2.53.0.windows.2 |
| Node.js | v24.15.0 |
| npm | 11.12.1 via `npm.cmd` (PowerShell script execution policy blocks `npm.ps1`) |
| Java system runtime | 26.0.2; not used for the backend checkpoint |
| Java project runtime | Temurin OpenJDK 21.0.12 portable, stored in ignored `.jdk21-runtime/` |
| Maven | 3.9.16 through `backend/mvnw.cmd` |
| PostgreSQL | Not installed as a system service before this task |
| Docker / Docker Compose | Not available |
| winget / Chocolatey | Not available |
| Chrome/Chromium | Not available in the checked command path |

## Setup decisions

- The current workspace was initialized as the local Git repository and connected to `https://github.com/dbp3206-source/group_sync.git` as `origin`.
- The foundation kit and supplied planning documents were copied into `AGENTS.md`, `docs/`, `prompts/`, and `reference/`.
- Backend uses Spring Boot `4.1.0`, Java release `21`, Maven Wrapper, Web MVC, JPA, Security, Validation, PostgreSQL, and Flyway.
- Frontend uses React `19.2.8`, TypeScript, Vite `8.2.1`, React Router, Axios, and Bootstrap package availability. Vite proxy forwards `/api` to `http://localhost:8080`.
- PostgreSQL configuration defaults to port `54329` to avoid colliding with a normal local PostgreSQL instance on `5432`.
- Secrets are supplied through environment variables or an ignored `.env`; `.env.example` contains placeholders only.
- A Flyway `V1__foundation_baseline.sql` contains only `SELECT 1` because Phase 0 intentionally has no domain tables.

## Commands and evidence

1. Read the supplied foundation kit, `IMPLEMENTATION_PLAN.md`, `PROJECT_DECISIONS.md`, `RISK_AND_SCOPE_GUARD.md`, and PeSoc reference README/pom before coding.
2. Verified the GitHub remote with `git ls-remote`.
3. Generated the backend from Spring Initializr and the frontend from Vite.
4. Ran `npm.cmd install` and `npm.cmd run build` successfully.
5. Ran `backend/mvnw.cmd test` with JDK 21 successfully: 1 test, 0 failures, 0 errors.

## PostgreSQL limitation

The machine had no PostgreSQL, Docker, or package manager. A PostgreSQL 18 Windows binaries ZIP was inspected and downloaded temporarily, but it was a development/binaries bundle without the server `share` catalog (`postgres.bki`), so it could not safely initialize a cluster. The temporary archive was removed. A native installer would require additional disk space and may require an interactive UAC step; it was not run blindly.

The repository is ready to use PostgreSQL through `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Before a real runtime smoke test, install PostgreSQL 17/18 or make Docker available, create `groupsync_dev`, and run the commands in the root README.

