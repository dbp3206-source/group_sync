# GroupSync Deployment Guide

## Deployment choice

This repository uses a simple production-like local deployment for the final student project:

```text
React production build -> Vite preview -> Spring Boot executable JAR -> PostgreSQL
```

Docker is not installed on the target machine, and no cloud provider or account was selected. A provider login, plan confirmation, DNS/TLS setup, or external secret was therefore not required. This guide describes the verified local deployment, not a public Internet deployment.

## Verified local URLs

- Frontend: `http://127.0.0.1:4173/`
- Backend: `http://127.0.0.1:8080/`
- Health: `http://127.0.0.1:8080/api/health`
- PostgreSQL: `127.0.0.1:54329`, database `groupsync_dev`

The backend CORS allow-list must contain the exact frontend origin used in the browser.

## Prerequisites

- Java 21 (the repository runtime was verified with Java `21.0.12`).
- Node.js `24.15.0` and npm `11.12.1`.
- PostgreSQL `17.10` accepting connections on port `54329`.
- PowerShell and Git.

Docker is optional and was not used.

## Environment variables

Set these in the process environment or in a local ignored `.env` file. Never commit real passwords or secrets.

| Variable | Example | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://127.0.0.1:54329/groupsync_dev` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `groupsync` | Database role |
| `DB_PASSWORD` | local-only value | Database password |
| `APP_CORS_ORIGINS` | `http://127.0.0.1:4173,http://localhost:4173` | Allowed frontend origins |
| `SERVER_PORT` | `8080` | Spring Boot port |
| `APP_TIMEZONE` | `Asia/Bangkok` | Application timezone |
| `VITE_API_URL` | `http://127.0.0.1:8080/api` | Absolute API URL embedded in the production frontend build |

## First deploy / start

Open three PowerShell terminals.

### 1. Verify PostgreSQL

```powershell
pg_isready -h 127.0.0.1 -p 54329
```

The expected result contains `accepting connections`.

### 2. Build and start the backend

```powershell
cd backend
$env:DB_URL='jdbc:postgresql://127.0.0.1:54329/groupsync_dev'
$env:DB_USERNAME='groupsync'
$env:DB_PASSWORD='your-local-password'
$env:APP_CORS_ORIGINS='http://127.0.0.1:4173,http://localhost:4173'
$env:SERVER_PORT='8080'
$env:JAVA_HOME=(Resolve-Path '../.jdk21-runtime').Path
./mvnw.cmd test package
& "$env:JAVA_HOME\bin\java.exe" -jar target/backend-0.0.1-SNAPSHOT.jar
```

On startup, Flyway validates and applies pending incremental migrations automatically. Do not edit an already-applied migration.

Verify:

```powershell
Invoke-RestMethod http://127.0.0.1:8080/api/health
```

Expected response: `status=UP`.

### 3. Build and serve the frontend

```powershell
cd frontend
$env:VITE_API_URL='http://127.0.0.1:8080/api'
npm.cmd install
npm.cmd run build
npm.cmd run preview -- --host 127.0.0.1 --port 4173
```

Open `http://127.0.0.1:4173/`.

## Database and seed policy

The verified machine uses the existing persistent PostgreSQL service and database `groupsync_dev`. The `groupsync` role does not have `CREATEDB`, so a separate `groupsync_prod` database was not created without an administrator credential. This is documented and intentional for the local student deployment.

Production-like startup uses the same Flyway-managed schema and does not seed demo data automatically. For a presentation or local rehearsal only:

```powershell
$env:GROUPSYNC_DB_PASSWORD='your-local-password'
$env:GROUPSYNC_DEMO_PASSWORD='DemoOnly-GroupSync-2026!'
powershell.exe -ExecutionPolicy Bypass -File .\scripts\reset-demo.ps1
```

The reset script truncates and reseeds the development database. Do not run it against a real production database.

Personal calendar events and recurring schedules are stored in PostgreSQL. There is no Google Calendar integration. Study and Badminton calendar entries are derived by the application from persisted group participation/session state.

## Update / redeploy

1. Pull or copy the new source.
2. Keep the database running and preserve the environment variables.
3. Run `backend\mvnw.cmd test package`; restart the JAR. Flyway applies only new migrations.
4. Rebuild the frontend with the production `VITE_API_URL`; restart the preview/static server.
5. Verify `/api/health`, frontend load, CORS, and the smoke flow.

## Troubleshooting

- **Database connection failure:** check PostgreSQL service status, port `54329`, `DB_URL`, username and local password.
- **Flyway failure:** inspect the first migration error; restore the database backup or fix configuration. Never rewrite an applied migration.
- **Frontend calls the wrong API:** rebuild after setting `VITE_API_URL`; Vite embeds this value at build time.
- **CORS error:** use the exact frontend origin in `APP_CORS_ORIGINS`, including port, then restart the backend.
- **Port collision:** stop the process using `8080` or `4173`, or choose another port and update CORS/API URL consistently.
- **Demo state changed:** stop using the golden flow and run `reset-demo.ps1` against the development database only.

