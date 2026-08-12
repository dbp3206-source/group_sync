# GroupSync

GroupSync is a beginner-readable modular monolith for personal availability and group activity operations. The current foundation includes a Spring Boot REST API, a React/Vite frontend, PostgreSQL, simple session authentication, group core operations, personal calendar, availability suggestions, Study sessions, and the Badminton operations-to-results flow.

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

## Production-like local deployment

The final verified deployment uses the packaged Spring Boot JAR, the React/Vite production build, and the persistent local PostgreSQL service. It is intentionally simple and does not require Docker or a cloud account.

```powershell
cd backend
$env:DB_URL='jdbc:postgresql://127.0.0.1:54329/groupsync_dev'
$env:DB_USERNAME='groupsync'
$env:DB_PASSWORD='your-local-password'
$env:APP_CORS_ORIGINS='http://127.0.0.1:4173,http://localhost:4173'
$env:JAVA_HOME=(Resolve-Path '../.jdk21-runtime').Path
./mvnw.cmd test package
& "$env:JAVA_HOME\bin\java.exe" -jar target/backend-0.0.1-SNAPSHOT.jar
```

In another terminal:

```powershell
cd frontend
$env:VITE_API_URL='http://127.0.0.1:8080/api'
npm.cmd run build
npm.cmd run preview -- --host 127.0.0.1 --port 4173
```

Open `http://127.0.0.1:4173/`. Flyway applies pending migrations when the backend starts. See [docs/DEPLOYMENT_GUIDE.md](docs/DEPLOYMENT_GUIDE.md) for redeploy and troubleshooting details.

## Verify

```powershell
cd backend
./mvnw.cmd test
./mvnw.cmd package

cd ..\frontend
npm.cmd run build
```

See `docs/SETUP_REPORT.md` for the exact machine/tool versions and the PostgreSQL setup decision. See `docs/IMPLEMENTATION_STATUS.md` for the current phase boundary. Google Calendar synchronization, tournament brackets, WebSocket infrastructure and advanced ranking remain intentionally out of scope.

For the final presentation, see `docs/DEMO_GUIDE.md` and `docs/OOP_DEFENSE_GUIDE.md`. Set `GROUPSYNC_DB_PASSWORD` and `GROUPSYNC_DEMO_PASSWORD` locally, then run `scripts/reset-demo.ps1` to restore the reproducible demo state.
