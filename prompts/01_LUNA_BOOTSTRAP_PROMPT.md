# Prompt 01 — Luna High / Codex — Bootstrap only

You are the primary coding agent for GroupSync. Before modifying files, read:
- AGENTS.md
- docs/00_PROJECT_OVERVIEW.md
- docs/01_SCOPE_AND_FEATURES.md
- docs/02_ARCHITECTURE_AND_REPO_STRUCTURE.md
- docs/03_ROADMAP_AND_PHASES.md
- docs/04_CODING_LEVEL_AND_RULES.md
- docs/IMPLEMENTATION_PLAN.md if it exists
- reference/PESOC_REFERENCE_NOTES.md

First briefly inspect the repository and explain to yourself what already exists. Then implement **Phase 0 only: project bootstrap and connectivity**.

Goal:
Create a clean, beginner-readable two-app repository that builds and runs before any real GroupSync domain feature is implemented.

Expected shape:
- `backend/`: Java 21, Spring Boot 4.1.x, Maven, REST API, JPA, Spring Security dependency available, Validation, PostgreSQL driver, test dependencies.
- `frontend/`: React + TypeScript + Vite, React Router, Axios or small API wrapper, Bootstrap 5. FullCalendar may be installed now or later; do not build calendar features yet.
- root `README.md` with clear local setup/run commands.
- safe configuration using environment variables / `.env.example`; never commit real secrets.

Implement the smallest proof of connectivity:
1. Backend has `GET /api/health` returning a simple JSON payload.
2. PostgreSQL datasource configuration exists and the application can start against a local database.
3. Frontend has a simple Home/Health page that calls `/api/health` and visibly shows backend connectivity.
4. Configure development CORS or Vite proxy in a simple understandable way.
5. Add a minimal global error shape only if needed; do not build a large framework.
6. Add useful `.gitignore` files.
7. Optionally add `docker-compose.yml` for PostgreSQL if it makes local setup easier, but local Docker must not become mandatory; document both routes when practical.

Important constraints:
- Do NOT implement User/Auth, Group, Calendar, Study or Badminton business features in this task.
- Do NOT introduce microservices, Dockerized full stack, WebSocket, Redis, Redux, GraphQL, AI, CQRS, event sourcing or other unnecessary infrastructure.
- Keep code style conventional and roughly comparable in difficulty to the PeSoc reference.
- Use constructor injection in Spring code.
- Do not expose secrets.
- If current dependency versions differ slightly from the docs, choose compatible current stable versions while preserving Java 21 + Spring Boot 4.1.x and React 19.x intent.

Verification before finishing:
- run backend tests/build;
- run frontend install/build/typecheck;
- if possible start both apps and verify the health request path;
- fix straightforward build/config errors instead of stopping at the first failure.

At handoff, report:
1. files/directories created;
2. exact commands to run backend/frontend/database;
3. checks/tests you ran and results;
4. any local prerequisite the user needs;
5. the next recommended task (Phase 1 Auth/User) but do not implement it yet.
