# 02 — Architecture and Repository Structure

## 1. Architecture goal
Use a **simple modular monolith**, not microservices.

There are two deployable applications:

```text
Browser
   ↓
React SPA (frontend)
   ↓ REST/JSON
Spring Boot API (backend)
   ↓ JPA
PostgreSQL
```

This keeps the system understandable for a third-year OOP team while preserving a clean frontend/backend boundary.

## 2. Technology baseline
### Backend
- Java 21.
- Spring Boot 4.1.x.
- Spring Web MVC.
- Spring Data JPA.
- Spring Security.
- Bean Validation.
- Spring Scheduler.
- Spring Application Events when useful.
- PostgreSQL 18.x (or another current supported PostgreSQL 18 minor release).
- Maven.
- JUnit 5 + Spring Boot Test.

### Frontend
- React 19.2 major/minor line.
- TypeScript.
- Vite.
- React Router.
- Axios (recommended) or a very small fetch wrapper.
- Bootstrap 5 for responsive UI.
- FullCalendar React for the calendar UI if useful.

Do not add Redux, Next.js, GraphQL, Tailwind, WebSocket, Redis, Kafka, Kubernetes or a component mega-library unless a later requirement provides a real reason.

## 3. Repository layout
Keep backend layout intentionally familiar to PeSoc.

```text
groupsync/
├── AGENTS.md
├── README.md
├── docs/
├── prompts/
├── reference/
│
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/groupsync/backend/
│       │   │   ├── GroupSyncApplication.java
│       │   │   ├── configuration/
│       │   │   ├── controller/
│       │   │   ├── dto/
│       │   │   ├── model/
│       │   │   ├── repository/
│       │   │   ├── service/
│       │   │   ├── strategy/
│       │   │   ├── event/
│       │   │   ├── scheduler/
│       │   │   └── exception/
│       │   └── resources/
│       │       ├── application.properties
│       │       └── application-dev.properties
│       └── test/
│
└── frontend/
    ├── package.json
    ├── vite.config.ts
    └── src/
        ├── api/
        ├── components/
        ├── layouts/
        ├── pages/
        ├── features/
        │   ├── auth/
        │   ├── calendar/
        │   ├── groups/
        │   ├── study/
        │   ├── badminton/
        │   ├── news/
        │   └── notifications/
        ├── hooks/
        ├── types/
        ├── utils/
        ├── App.tsx
        └── main.tsx
```

Why this layout:
- `controller/service/repository/model` is easy for beginners and similar to PeSoc.
- `dto` prevents API contracts from becoming JPA objects.
- `strategy` gives OOP algorithms an obvious home.
- `event/scheduler` supports "input once, derive many" without hiding logic in a giant service.
- frontend `features/` keeps badminton code separate from study/calendar while shared UI stays in components.

## 4. Backend responsibilities

### controller
Receives HTTP request, validates DTO, calls service, returns response.
Do not calculate ranking or waitlist logic here.

### service
Coordinates a use case and transaction.
Examples:
- `BadmintonSessionService.registerUser(...)`
- `MatchService.confirmResult(...)`
- `AvailabilityService.suggestSlots(...)`

### model
JPA entities + enums + simple meaningful domain behavior.
Examples:
- `SessionRegistration.cancel()`
- `BadmintonSession.open()`
- `Match.confirmResult()`

Avoid both extremes:
- anemic entities with absolutely no behavior;
- highly abstract DDD aggregate framework.

### repository
Spring Data repository/query methods only.

### dto
Request/response objects such as:
- `CreateBadmintonSessionRequest`;
- `SessionSummaryResponse`;
- `ScheduleSuggestionResponse`.

### strategy
Real variable algorithms only:
- SchedulingStrategy;
- PairingStrategy;
- CourtAllocationStrategy;
- RankingStrategy;
- WaitlistPolicy if it becomes configurable.

### event
Simple application events:
- `SessionTimeChangedEvent`;
- `WaitlistPromotedEvent`;
- `MatchResultConfirmedEvent`.

Listeners may update derived records/news/notifications. Keep event chains short and documented.

### scheduler
Time-based automation:
- session reminders;
- registration deadline checks if needed.

### exception
Custom exceptions + one global REST exception handler.

## 5. OOP mapping

### Encapsulation
Instead of arbitrary state mutation:

```java
registration.cancel();
match.confirmResult(score);
session.openRegistration();
```

These methods enforce valid state transitions.

### Abstraction + polymorphism

```java
public interface PairingStrategy {
    List<Pairing> pair(List<PlayerSnapshot> players);
}
```

Implementations may include manual/random/balanced strategies.

Same idea for scheduling, court allocation and ranking.

### Inheritance
Use sparingly. A small shared base such as an audited entity is fine if useful. Do not create a ten-level tree like `Activity -> SportActivity -> RacketActivity -> BadmintonActivity -> ...`.

### Composition
Primary design tool:
- Group has Memberships.
- BadmintonSession has Registrations and CourtAssignments.
- Match has Sides; sides have participants.
- StudySession has Goals/Materials/Attendance.

## 6. Authentication
Recommended MVP:
- Register/login with email or username + password.
- BCrypt.
- Spring Security.
- Simple access-token JWT is acceptable for React REST flow.
- No refresh-token lifecycle in core.
- Frontend stores only what is needed and sends Authorization header.

If Codex determines server-session auth is materially simpler in the final deployment setup, it may choose that instead, but it must remain Spring Security + hashed passwords and document the choice.

## 7. Calendar model
Keep recurrence intentionally small.

Possible objects:
- `PersonalCalendarEvent` — one-time event or a single occurrence generated by app logic.
- `RecurringSchedule` — weekly recurring class/work/busy pattern.
- `GroupActivityCalendarLink` — relationship/reference showing that a group activity belongs on a member calendar.

Availability should be calculated, not stored as a huge FREE/BUSY table for every hour.

## 8. Time handling
- Database/backend: use consistent timestamps and timezone policy.
- Project default display timezone can be Asia/Bangkok / UTC+7 unless deployment requirements say otherwise.
- React displays user-local dates consistently.
- Do not mix strings and timestamps casually.

## 9. Badminton match model
Do not copy PeSoc's 1-v-1 assumption.

Recommended conceptual model:

```text
Match
├── MatchSide A
│   └── MatchParticipants [1 or 2]
├── MatchSide B
│   └── MatchParticipants [1 or 2]
└── MatchResult
```

This supports doubles cleanly and allows singles later.

## 10. Data derivation flow
Example result flow:

```text
React score form
   ↓ POST result
MatchController
   ↓
MatchService
   ↓ validate + save confirmed result
MatchResultConfirmedEvent
   ├── Stats listener
   ├── Ranking/history listener/service
   ├── News generator (optional)
   └── Notification service (if needed)
   ↓
PostgreSQL
```

Do not overuse asynchronous infrastructure. Spring application events may remain synchronous.

## 11. Deployment shape
For the semester project:
- frontend static build;
- backend single Spring Boot app;
- single PostgreSQL database.

Local development may use Docker Compose for PostgreSQL if convenient, but Docker is not a learning objective and should not block development.
