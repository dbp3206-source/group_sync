# 04 — Coding Level, Clean-Code Rules and What NOT to Build

## 1. Target difficulty
Code must be suitable for a third-year student who is learning Java/OOP through the project.

The repo should feel approximately like a cleaned-up, slightly more structured version of PeSoc, not an enterprise architecture showcase.

A student should be able to open a feature and trace:

```text
React page
-> API request
-> Controller
-> Service
-> Repository / Domain object
-> Database
```

without reading a framework abstraction handbook.

## 2. Preferred Java style
- descriptive class/method names;
- methods generally small enough to explain;
- constructor injection preferred;
- enums instead of magic strings;
- `Optional`/exceptions handled consistently;
- DTO validation with Jakarta Validation;
- `@Transactional` around use cases that update multiple related records;
- comments explain business reason, not obvious syntax.

## 3. OOP should be visible, but natural
Good:
- `PairingStrategy` because several real pairing algorithms exist.
- `RankingStrategy` because ranking policy can change.
- `registration.cancel()` because registration owns cancellation state rules.
- `MatchSide` composition because doubles needs multiple participants.

Bad:
- Strategy interface with only one implementation and no likely second behavior.
- Abstract factories for every entity.
- Generic `BaseService<T,R,D>`.
- deep inheritance tree just to show inheritance.

## 4. Backend rules
### Controller
Thin. Example:

```java
@PostMapping("/{sessionId}/register")
public SessionRegistrationResponse register(...) {
    return badmintonSessionService.register(sessionId, currentUserId);
}
```

### Service
Owns workflow/use-case coordination.
It may call repositories, strategies and entity methods.

### Entity/model
Can enforce local rules:

```java
public void cancel() {
    if (status == RegistrationStatus.CANCELLED) {
        throw new InvalidRegistrationStateException(...);
    }
    this.status = RegistrationStatus.CANCELLED;
}
```

Do not put HTTP or repository code in entities.

## 5. Error handling
Use one global exception handler with readable JSON response, e.g.:

```json
{
  "code": "SESSION_FULL",
  "message": "The session is full; you were added to the waitlist.",
  "timestamp": "..."
}
```

Keep error model simple.

## 6. Frontend rules
- pages should compose smaller components;
- API calls live in `api/` or feature API files, not scattered everywhere;
- shared TypeScript types mirror API DTO meaning, not JPA entities;
- show useful loading/error/empty states;
- responsive Bootstrap layout;
- avoid complex global state.

## 7. Database rules
Use clear foreign keys and constraints for correctness.
Examples:
- unique active membership per user/group;
- no duplicate session registration per user/session;
- indexes for common queries such as session group/date and notifications user/read state.

Do not add dozens of speculative indexes.

## 8. Testing level
Must-have automated tests:
- Availability interval calculation.
- Duplicate registration.
- Capacity -> waitlist.
- Cancel -> promote first waitlist member.
- Only confirmed results affect ranking/statistics.
- Group permission checks.

Nice-to-have:
- API integration tests for a complete badminton session flow.
- frontend component tests for critical forms.

Do not chase 100% coverage.

## 9. Explicit non-goals
Do not introduce unless the user later changes scope:
- Microservices.
- Docker/Kubernetes orchestration beyond optional local PostgreSQL compose.
- Redis.
- Kafka/RabbitMQ.
- event sourcing.
- CQRS.
- generic workflow engine.
- AI ranking/scheduling.
- optimization solver.
- WebSocket just for "real time" label.
- payment gateway.
- OAuth social login.
- refresh token rotation system.
- media/video feed.
- complex file storage/CDN.
- mobile native app.

## 10. Autonomy rule for Codex
Codex should not ask about every implementation detail.

It may choose reasonable details when:
- behavior is not materially changed;
- option is conventional;
- choice can be changed later cheaply.

Examples Codex may decide:
- exact DTO class names;
- whether a tiny mapper is a method or class;
- component split;
- minor SQL/JPA query approach;
- exact Bootstrap layout;
- test fixture organization.

Codex should stop/flag only when the decision changes product behavior, data integrity, security or core scope.
