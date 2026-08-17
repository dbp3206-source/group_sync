# 03 — Implementation Roadmap

## How to use this roadmap
Each phase should end in a runnable/checkpointed repository. Do not start the next major phase while the previous phase does not compile or has broken core tests.

A phase is not necessarily exactly one week. The team can adjust based on semester schedule.

## Phase 0 — Understand + Bootstrap
Deliverables:
- repo layout;
- backend Spring Boot project starts;
- frontend React app starts;
- PostgreSQL connection works;
- simple `/api/health` endpoint;
- frontend can call health API;
- README run instructions;
- `.env.example` / safe config pattern;
- no real secrets committed.

No domain features yet.

## Phase 1 — Auth + User
Deliverables:
- User entity/repository/service/controller;
- register;
- login;
- BCrypt;
- simple authentication strategy;
- current user endpoint;
- basic React login/register/profile;
- protected frontend routes;
- tests for registration/login validation.

## Phase 2 — Group Core
Deliverables:
- Group;
- Membership;
- group type STUDY/BADMINTON;
- group role OWNER/ORGANIZER/MEMBER;
- create group;
- invite/join simplified flow;
- list my groups;
- member list;
- permission checks.

## Phase 3 — Personal Calendar + Recurring Schedule
Deliverables:
- one-time personal busy event;
- weekly recurring schedule;
- date range;
- My Calendar UI;
- overlap helper functions;
- ability to query busy blocks for a date range.

Focus on correctness before visual polish.

## Phase 4 — Availability Engine
Deliverables:
- combine busy blocks from selected members;
- find candidate common slots;
- return available-member counts;
- MaximumAttendanceStrategy;
- conflict detection;
- UI to ask `find a 2-hour slot this week`.

Unit-test interval logic heavily.

## Phase 5 — Study Group Vertical
Deliverables:
- StudySession;
- create from manually selected or suggested slot;
- join/leave;
- automatic personal calendar link;
- topic;
- goals;
- materials;
- attendance;
- basic session detail page.

At this point shared core must prove it can support a complete non-sports flow.

## Phase 6 — Badminton Foundation
Deliverables:
- Badminton profile per membership;
- Venue;
- Court;
- BadmintonSession;
- choose courts;
- capacity;
- open/confirm/cancel lifecycle;
- session list/detail page.

## Phase 7 — Registration + Waitlist + Check-in
Deliverables:
- join session;
- duplicate prevention;
- capacity enforcement;
- FIFO waitlist;
- cancellation;
- automatic promotion;
- calendar add/remove/update;
- organizer check-in/no-show;
- notifications.

This is one of the most important business-rule phases.

## Phase 8 — Court Allocation + Pairing
Deliverables:
- checked-in player pool;
- manual allocation;
- simple automatic allocation;
- manual/random/balanced pairing;
- visual court board.

Keep algorithms deterministic and explainable.

## Phase 9 — Match + Result
Deliverables:
- doubles-compatible Match/MatchSide/Participants;
- match lifecycle;
- score submission;
- organizer/authorized result confirmation;
- match history;
- match detail.

## Phase 10 — Ranking + Stats + History
Deliverables:
- simple ranking strategy;
- win/loss/win rate;
- recent form;
- ranking history snapshot;
- profile stats scoped to group;
- group leaderboard;
- automatic recalculation/update after confirmed result.

Optional late in phase: Season.

## Phase 11 — Responsibilities + News + Notification
Deliverables:
- session responsibilities/equipment;
- shuttlecock assignment;
- cancellation recheck;
- manual announcements;
- simple system news templates;
- notification inbox;
- scheduled reminders.

## Phase 12 — Dashboard + UX
Deliverables:
- Home dashboard: today's activities, next session, groups, recent result, ranking/news;
- responsive mobile UI;
- loading/error/empty states;
- organizer dashboard improvements.

## Phase 13 — Hardening + Testing
Deliverables:
- critical service tests;
- permission tests;
- validation/error responses;
- concurrency review on registration/waitlist;
- frontend build/typecheck;
- seed/demo data;
- bug fixes;
- cleanup unused code.

## Phase 14 — P1 Improvements
Choose only what core is ready for:
- Season;
- head-to-head;
- partner statistics;
- awards;
- better ranking history chart;
- tournament;
- Google Calendar integration;
- QR check-in.

Do not attempt all of them.

## Phase 15 — Deployment + Pilot + Presentation
- deploy backend/frontend/database;
- create demo accounts/group/session/history;
- let real friends use it for several sessions;
- collect bugs/usability feedback;
- fix high-value issues;
- prepare UML/OOP explanation;
- prepare end-to-end demo story.

## Suggested 5-person ownership
Ownership is not isolation. Everyone reviews across modules.

### Member A — Core/Auth/Groups
- auth;
- group core;
- permissions;
- admin basics.

### Member B — Calendar/Availability
- recurring schedules;
- calendar UI integration;
- availability algorithms;
- conflict handling.

### Member C — Study + Content
- Study Group;
- study sessions/materials/goals/attendance;
- announcements/news UI.

### Member D — Badminton Operations
- venue/court;
- badminton session;
- registration/waitlist/check-in;
- responsibilities.

### Member E — Badminton Competition/Automation
- court allocation/pairing;
- match/result;
- ranking/statistics/history;
- domain events/notifications.

Pair on integration points. Avoid five isolated mini-projects.
