# GroupSync OOP Defense Guide

This guide explains object-oriented concepts using the current source, not invented examples. The project is a conventional Spring modular monolith so the code can be explained by a third-year student.

## 1. Encapsulation

**Source:** `badminton/model/BadmintonSession.java`, `BadmintonRegistration.java`, `BadmintonMatch.java`, and `study/model/StudySession.java`.

**Behavior:** entities expose operations such as `open()`, `confirm()`, `start()`, `complete()`, `cancel()`, `promote()`, `checkIn()`, `submitResult()` and `confirmResult()`. Services call these methods instead of directly changing lifecycle fields.

**Business purpose:** the object protects valid transitions. A Badminton Session cannot start unless confirmed, and a Match cannot be confirmed before a valid score has been submitted.

**Answer:** “Encapsulation means the state is kept inside the domain object and changed through meaningful methods. The method checks the business rule at the point where state changes, so a controller cannot accidentally set an invalid status.”

## 2. Abstraction

**Source:** `calendar/aggregation/CalendarSource.java`, `calendar/model/CalendarItem.java`, and `availability/strategy/SchedulingStrategy.java`.

**Behavior:** a `CalendarSource` supplies normalized `CalendarItem` values without exposing whether data came from a manual event, recurring schedule, study session or badminton session.

**Business purpose:** the calendar aggregator can ask every source for busy items and build one personal view. The availability engine depends on the common scheduling contract instead of knowing every feature’s tables.

**Answer:** “The interface is the useful contract. The caller needs a calendar item and time range, not each source’s internal details. This keeps the aggregator small and allows a new source without rewriting the availability algorithm.”

## 3. Polymorphism

**Source:**

- `availability/strategy/SchedulingStrategy.java`
- `availability/strategy/MaximumAttendanceStrategy.java`
- `availability/strategy/EarliestPossibleStrategy.java`
- `badminton/pairing/PairingStrategy.java`
- `badminton/pairing/BalancedPairingStrategy.java`
- `badminton/pairing/RandomPairingStrategy.java`
- `badminton/ranking/RankingStrategy.java`
- `badminton/ranking/PointsRankingStrategy.java`
- `badminton/ranking/EloRankingStrategy.java`
- `badminton/responsibility/ResponsibilityAssignmentStrategy.java`
- `badminton/responsibility/RoundRobinResponsibilityAssignmentStrategy.java`

**Behavior:** different classes implement the same operation. Availability can use Maximum Attendance or Earliest Possible; pairing can use Balanced or Random while the caller still works with the interface.

**Business purpose:** an algorithm can change without duplicating the controller or use-case flow. The same idea is used for Points/Elo ranking and manual/round-robin responsibility assignment.

**Answer:** “Polymorphism lets one variable refer to different implementations of the same contract. Balanced pairing produces a skill-aware suggestion, while Random pairing changes the player selection, but both return the same pairing suggestion shape.”

## 4. Composition

**Source:** `BadmintonSession` contains courts and is related to registrations and responsibilities. `BadmintonMatch` is composed of `BadmintonMatchSide` objects, and each side contains `BadmintonMatchParticipant` objects. `Group` is connected to `Membership`, and a badminton profile belongs to that membership context.

**Behavior:** a doubles match is represented as Match → Side A/Side B → Participants. It is not limited to fields such as `player1` and `player2`.

**Business purpose:** composition models ownership and relationships clearly. It makes doubles first-class and keeps group membership/rating scoped to a group.

**Answer:** “Composition means building a larger object from smaller objects. Match is composed of sides and participants, so the model directly represents doubles and can validate each part.”

## 5. Inheritance

The application does not use a deep custom inheritance hierarchy. JPA entities are separate domain classes, and behavior is shared through services, interfaces and composition.

**Answer:** “Inheritance is useful when there is a genuine stable ‘is-a’ relationship. A Study Session is not a kind of Badminton Session because their rules differ. Composition and small interfaces are clearer, reduce coupling, and are easier to maintain.”

## 6. Strategy Pattern

**Scheduling:** `SchedulingStrategy` is the variable scheduling algorithm. `MaximumAttendanceStrategy` is the first group-availability priority, while `EarliestPossibleStrategy` provides another valid choice.

**Pairing:** `PairingStrategy` is implemented by `BalancedPairingStrategy` and `RandomPairingStrategy`. The service selects the implementation from the requested type and returns the same `PairingSuggestion` shape.

**Ranking:** `ranking/PointsRankingStrategy.java` centralizes the simple rule that a winner earns 3 points and a loser earns 1 point. It is easier to defend than Elo for this MVP.

`PointsRankingStrategy` is the default and `EloRankingStrategy` is an optional season-scoped alternative. Both implement `RankingStrategy`, so `RankingService` selects the behavior without changing match confirmation or statistics code.

**Court allocation:** `badminton/service/AllocationService.java` uses simple round-robin distribution across active courts. A separate strategy was not added because there is only one allocation algorithm today; adding one would add ceremony without a real choice.

**Responsibility assignment:** `ResponsibilityAssignmentStrategy` is implemented by `RoundRobinResponsibilityAssignmentStrategy`; existing manual assignment remains a direct organizer action. The round-robin class assigns unassigned responsibilities to registered/check-in members in order.

**Answer:** “I used Strategy only where business behavior can reasonably vary. It lets us add or replace an algorithm without changing the surrounding service workflow.”

## 7. Event-driven automation

**Source:** `badminton/event/MatchConfirmedEvent.java`, `news/service/MatchConfirmedListener.java`, and `badminton/service/MatchService.java`.

**Behavior:** after a match is confirmed, `MatchService` publishes a Spring application event. `MatchConfirmedListener` creates system news and participant notifications using an idempotent source key.

**Business purpose:** the result workflow stays focused on confirming the match, while derived engagement data is updated automatically.

`TournamentMatchConfirmedListener` consumes the same event to copy the already-derived winner into a FINAL tournament bracket and complete the tournament. It does not run a second ranking/statistics pipeline.

**Answer:** “This is a simple in-process Spring event, not a distributed event architecture. It is useful because news and notifications are consequences of a confirmed result, and the source key prevents duplicate derived records.”

## 8. State and business workflows

### Study Session

`StudySessionStatus` models the lifecycle from draft/open to confirmed, completed or cancelled. `StudySession` methods and `StudyService` protect who can confirm, reschedule, cancel, complete and record attendance.

### Badminton Session

`BadmintonSessionStatus` is `DRAFT → OPEN → CONFIRMED → PLAYING → COMPLETED`, with cancellation guarded by domain rules. The same session drives registration, calendar synchronization, check-in, allocation and matches.

### Registration and waitlist

`RegistrationStatus` distinguishes `REGISTERED`, `WAITLISTED`, `CHECKED_IN`, `NO_SHOW` and `CANCELLED`. `BadmintonService` counts active registrations, prevents duplicates, and promotes the oldest waitlisted registration when a place opens. The promoted member receives a notification and the derived calendar item appears automatically.

### Match result

`MatchStatus` moves from `SCHEDULED` to `PLAYING`, then `RESULT_SUBMITTED` and `CONFIRMED`. `BadmintonMatch` validates score transitions and derives the winner side. `MatchService` applies ranking/statistics and publishes the result event, so the user enters only the score.

**Answer:** “Enums make the workflow visible, and domain methods guard transitions. This is easier to test than allowing every controller to write status values directly.”

## 9. Calendar architecture and privacy

`PersonalCalendarSource`, `StudyCalendarSource` and `BadmintonCalendarSource` implement the shared `CalendarSource` contract. `CalendarAggregatorService` merges normalized `CalendarItem` values for one user; `AvailabilityService` uses those busy intervals and returns only candidate times/member IDs. Private personal event titles and descriptions are not returned by group availability.

The defense flow is:

`User Calendar → Calendar Aggregator → Availability Engine → Study/Badminton operations`.

This demonstrates “input once, derive many”: a recurring class or busy block is stored once and reused by later availability searches. Tournament reuses Badminton session/match data and does not introduce a fifth calendar source.

## 10. Layered request path

The source follows a readable flow:

`React component → Controller → Service → Model/domain behavior → Repository → PostgreSQL`.

Controllers handle HTTP and validation. Services coordinate authorization, transactions and use cases. Models protect local state transitions. Repositories persist and query. DTOs keep entities out of the public API contract.

This is a modular monolith, not microservices. It keeps the OOP defense focused on real classes and business rules rather than infrastructure ceremony.
