# GroupSync

GroupSync is a beginner-readable modular monolith for personal availability and group activity operations. Phase 0 contains only the runnable foundation: a Spring Boot REST API, a React/Vite frontend, and PostgreSQL development configuration.

## Repository layout

- `backend/` — Java 21 + Spring Boot REST API.
- `frontend/` — React + TypeScript + Vite SPA.
- `docs/` — product decisions, implementation plan, scope guard, setup and status reports.
- `AGENTS.md` — repository rules; preserve it when making future changes.

## Prerequisites

- Java 21.
- Node.js 24+ and npm.
- PostgreSQL 17+ (PostgreSQL 18 is the local target when available).
- Git.

Maven is provided through the Maven Wrapper in `backend/`; a system Maven installation is not required.

## Local configuration

Copy `.env.example` to `.env` for local values if you do not want to export variables in your shell. `.env` is ignored by Git. The backend also accepts the variables directly from the process environment.

The development database is expected at `localhost:54329` so it does not collide with a normal PostgreSQL installation on port `5432`.

## Run the backend

From PowerShell:

```powershell
cd backend
./mvnw.cmd spring-boot:run
```

The API listens on `http://localhost:8080`. Health check:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

## Run the frontend

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

Open the Vite URL shown in the terminal. The `/api` Vite proxy forwards requests to the backend.

## Verify

```powershell
cd backend
./mvnw.cmd test
./mvnw.cmd package

cd ..\frontend
npm.cmd run build
```

See `docs/SETUP_REPORT.md` for the exact machine/tool versions and the PostgreSQL setup decision. This checkpoint intentionally does not implement authentication, groups, calendar, Study, or Badminton business features.

