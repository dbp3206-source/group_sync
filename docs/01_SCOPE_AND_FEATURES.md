# 01 — Scope, Modules and Business Features

## 1. Scope philosophy
Implement a small number of connected modules deeply enough to feel real. Do not build ten shallow group types.

### Core implemented verticals
- Study Group.
- Badminton Group.

### Out of core scope
- Football/Running/Boardgame verticals.
- AI/ML recommendations.
- IoT court/equipment tracking.
- real online payment.
- full social network.
- private chat system.
- microservices.
- mandatory WebSocket/realtime.

## 2. Module A — Authentication & User Profile
Minimum:
- register;
- login/logout;
- current user;
- edit basic profile;
- simple avatar URL or optional upload only if easy;
- USER/ADMIN system role.

Security:
- password hashes with BCrypt;
- no plaintext password;
- no refresh token complexity;
- no OAuth in core.

## 3. Module B — Personal Calendar
Purpose: produce availability with minimal repeated input.

### User-created data
- one-time busy event;
- recurring weekly schedule, especially class/work schedule;
- title and optional description;
- start/end time;
- date range;
- visibility: group members only need BUSY/FREE, not private description.

### System-created data
GroupSync creates calendar entries when the user joins/participates in confirmed group activities.

### MVP recurrence
Support:
- NONE (single event);
- WEEKLY (one or more weekdays until an end date).

Do not implement a complete RFC recurrence engine in core.

## 4. Module C — Group Core
Features:
- create group;
- group type: STUDY / BADMINTON;
- invite/join member;
- membership status;
- group role OWNER / ORGANIZER / MEMBER;
- group dashboard;
- member list;
- announcement/news feed;
- leave/remove member with simple rules.

## 5. Module D — Calendar Aggregator & Availability Engine
Inputs:
- personal busy events;
- recurring schedule occurrences;
- confirmed GroupSync activities;
- optional external calendar later.

Outputs:
- free/busy blocks;
- schedule conflicts;
- common slots for selected members;
- attendance count per suggested slot.

MVP scheduling strategies:
- MaximumAttendanceStrategy;
- EarliestPossibleStrategy (optional but easy);
- RequiredMembersStrategy (slot must include selected required members).

Avoid complex optimization algorithms.

## 6. Module E — Study Group
### StudySession
Fields/behavior:
- title/topic;
- start/end;
- location/link;
- status: DRAFT / OPEN / CONFIRMED / COMPLETED / CANCELLED;
- capacity optional;
- participant registrations;
- goals;
- materials.

Features:
- organizer asks Availability Engine for candidate times;
- create session from a suggestion;
- members join/leave;
- calendar auto-update;
- attendance marking;
- add materials (link/title; file upload only if easy);
- goals/checklist;
- announcement/reminder.

Do not turn Study Group into a full LMS in this project.

## 7. Module F — Badminton Group (flagship)

### F1. Venue and Court
- Venue has name, address text, notes.
- Venue contains Courts.
- Organizer can select courts for a session.
- Actual payment/booking API is out of scope; GroupSync records that a court was planned/booked.

### F2. BadmintonSession
Recommended statuses:
`DRAFT -> OPEN -> CONFIRMED -> PLAYING -> COMPLETED`
with `CANCELLED` as an alternative.

Important fields:
- group;
- date/time;
- venue;
- selected courts;
- capacity (default may be 16, configurable);
- registration deadline;
- status.

### F3. Registration + Capacity
Rules:
- user cannot have duplicate active registration;
- before capacity: REGISTERED;
- after capacity: WAITLISTED;
- cancelled registrations release a place;
- show schedule conflict before/while joining;
- conflict can be a warning rather than an absolute block unless group policy says otherwise.

### F4. Waitlist
MVP: FIFO.
When a registered member cancels:
- first active waitlist member is promoted;
- participant calendar is updated;
- notification is created.

Keep promotion transaction-safe enough to prevent two people receiving the same slot.

### F5. Check-in and No-show
Registration statuses can include:
- REGISTERED;
- WAITLISTED;
- CANCELLED;
- CHECKED_IN;
- NO_SHOW.

Organizer can mark check-in manually. QR is advanced.

### F6. Court allocation
Strategies:
- ManualCourtAllocation;
- BalancedCourtAllocation or simple sequential allocation.

Do not solve an advanced optimization problem. A deterministic, explainable algorithm is better.

### F7. Pairing
Strategies:
- ManualPairing;
- RandomPairing;
- BalancedPairing based on current rating/skill;
- AvoidRecentPartnerPairing as P1 if easy.

Support doubles from the beginning. Data model should not assume one player per side.

### F8. Match + Result
Model concept:

```text
Match
├── sideA -> participants [1 or 2]
├── sideB -> participants [1 or 2]
└── result
```

MVP badminton can focus on doubles while model allows singles.

Result flow:
`SCHEDULED -> PLAYING -> RESULT_SUBMITTED -> CONFIRMED`
with optional `DISPUTED` only if time permits.

Only CONFIRMED results affect official ranking/statistics.

### F9. Ranking + stats
Core stats:
- played;
- wins;
- losses;
- win rate;
- current ranking points/rating;
- recent form;
- attendance;
- no-show count.

MVP ranking should be easy to explain. Recommended default:
- PointsRankingStrategy (e.g. win = 3, loss = 1) or another simple points formula agreed by group.

Architecture may support Elo later through `RankingStrategy`, but do not make doubles Elo a core blocker.

### F10. Ranking history and sports profile
Should-have inspired by PeSoc:
- current group rank;
- current rating/points;
- peak rating/points;
- history after each confirmed match/session;
- recent form;
- head-to-head/partner stats as P1.

All sports stats are scoped to a Badminton Group/Season, not globally on User.

### F11. Season
P1 but strongly recommended.
A badminton group may have seasons such as `Fall 2026`.
Season scopes rankings, stats and awards.

### F12. Equipment responsibilities
Not a warehouse system.
Represent simple session responsibilities:
- shuttlecocks;
- first-aid kit;
- tripod;
- speaker;
- scoreboard.

Statuses: NEEDED / ASSIGNED / CONFIRMED / BROUGHT / RETURNED when relevant.
If assigned member cancels, the responsibility must be rechecked and organizer notified.

## 8. Module G — News / Group Feed
Keep content purposeful.

Manual:
- organizer announcements;
- venue change;
- internal tournament announcement.

System-generated (template based, no AI):
- session full;
- ranking leader changed;
- win streak milestone;
- season completed;
- next session confirmed.

Comments are optional but reasonable. No follower/feed algorithm.

## 9. Module H — Notifications & Automation
In-app notifications stored in DB.
Examples:
- invitation;
- session confirmed/changed;
- waitlist promotion;
- session reminder;
- result confirmed;
- responsibility assigned/unassigned;
- new study material.

Use scheduled jobs for reminders. Poll/refetch from frontend is enough for core.

## 10. Admin
Keep small:
- list/search users;
- deactivate/activate account if needed;
- basic group moderation;
- system overview counts.

Do not spend major project time building a generic enterprise admin suite.

## 11. P1 / Advanced candidates
P1 recommended:
- Season;
- ranking history;
- sports profile;
- match detail;
- recent form;
- head-to-head;
- partner statistics;
- group dashboard;
- system-generated news;
- simple awards.

Advanced only after core is stable:
- Google Calendar sync;
- tournament group/knockout;
- QR check-in;
- push notification;
- Elo ranking;
- CSV export.
