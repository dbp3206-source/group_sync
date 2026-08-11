# GroupSync repository instructions

## Product
GroupSync is a responsive web app that combines personal calendar/availability with group operations. The two implemented verticals are Study Group and Badminton Group. Badminton is the flagship module.

Core principle: **Input once, derive many.**
Examples:
- A user enters a recurring class schedule once; availability derives from it.
- A confirmed group session appears on participant calendars automatically.
- A cancellation can promote the next waitlisted member automatically.
- A confirmed badminton result updates win/loss, ranking/history and related feed data automatically.

## Audience and code level
This is a third-year university OOP project maintained by beginners.
- Prefer clear, conventional Java/Spring code over clever abstractions.
- Complexity should be around or only modestly above the PeSoc reference project.
- Code should be explainable by a student during an oral defense.
- Prefer readable methods and explicit domain names.
- Do not over-engineer.

## Architecture
Repository has two apps:
- `backend/`: Java 21 + Spring Boot REST API + PostgreSQL.
- `frontend/`: React + TypeScript + Vite responsive SPA.

Backend style should remain familiar to a classic Spring project:
`controller -> service -> repository -> model`, with `dto`, `strategy`, `event`, `scheduler`, `exception`, and `configuration` only where useful.

Do NOT introduce microservices, CQRS, event sourcing, hexagonal/clean architecture ceremony, message brokers, Kubernetes, GraphQL, AI/ML, IoT, real payment, or mandatory WebSocket infrastructure.

## Backend conventions
- Controllers: HTTP concerns only; no important business rules.
- Services: coordinate use cases and transactions.
- Models/entities: may contain simple domain behavior such as `cancel()`, `confirm()`, `checkIn()`, `complete()` when it improves encapsulation.
- Repositories: persistence/query only.
- DTOs: API input/output; do not expose JPA entities directly as public API contracts.
- Strategies: only for real variable algorithms such as scheduling, pairing, court allocation, waitlist or ranking.
- Events/listeners: only for simple derived actions such as notifications/stat updates; keep the flow traceable.
- Prefer composition to deep inheritance.
- Use enums for lifecycle states.
- Validate both API input and domain/business rules.

## Security
Keep auth intentionally simple:
- email/username + password;
- Spring Security;
- BCrypt password hashing;
- USER and ADMIN system roles only initially;
- group roles OWNER / ORGANIZER / MEMBER are domain roles, not global security roles.
A simple access-token JWT is acceptable. Do not add refresh-token rotation, social login, SSO, MFA, or complex auth unless explicitly requested later.
Never commit real secrets.

## Frontend conventions
- React + TypeScript + Vite.
- React Router for navigation.
- Axios or a small API wrapper for REST calls.
- Bootstrap 5 or similarly simple styling; avoid a large custom design system.
- FullCalendar may be used for calendar rendering.
- No Redux unless a concrete problem proves it necessary. Prefer local state + AuthContext + small reusable hooks.
- Mobile-first responsive UI; desktop remains comfortable for organizers.

## Database
Use PostgreSQL. Keep the relational model understandable. Add indexes/constraints where they protect correctness, but do not optimize prematurely.

## Testing
Each feature should include the smallest useful tests:
- unit tests for algorithms/business rules;
- service/integration tests for important workflows;
- build/lint/typecheck before handoff.

Particularly test:
- schedule overlap/availability;
- session capacity + waitlist promotion;
- duplicate registration prevention;
- result confirmation -> ranking/stat updates;
- permissions for organizer actions.

## Automation
Prefer Spring Scheduler and simple Spring application events. No real-time stack is required for MVP.

## Scope protection
Core verticals implemented deeply:
1. Study Group.
2. Badminton Group.

Do not implement Football/Running/Boardgame/etc. during core build. Architecture may leave extension points but should not create unused abstractions.

## When making changes
Before coding:
1. Read the relevant docs in `/docs`.
2. Inspect existing code and reuse current patterns.
3. State a short implementation plan internally.

After coding:
1. Run relevant backend tests/build.
2. Run frontend typecheck/build when frontend changed.
3. Fix failures you can resolve reasonably; do not stop after the first compilation error.
4. Summarize changed files, business behavior, tests run, and any remaining limitation.
5. Update docs only when behavior or architecture actually changed.

If requirements leave a minor implementation detail unspecified, choose the simplest reasonable solution instead of asking the user. Ask only when a choice would materially alter product behavior or scope.
