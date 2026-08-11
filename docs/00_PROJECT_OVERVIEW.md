# 00 — GroupSync Project Overview

## 1. One-sentence product definition
**GroupSync is a personal-and-group activity management web app where each member's calendar and availability feed directly into the real operations of groups they join, especially Study Groups and Badminton Groups.**

Vietnamese pitch:

> **GroupSync là nền tảng quản lý hoạt động cá nhân và hội nhóm, nơi lịch học/lịch bận của mỗi người được dùng để hỗ trợ tìm thời gian chung và vận hành các nhóm như học nhóm hoặc cầu lông — từ tạo session, đăng ký, waitlist và phân bổ tài nguyên đến kết quả, ranking, thống kê, tin tức và thông báo.**

## 2. Problem GroupSync solves
People often belong to several groups at once. Their personal calendar is separate from group chats, while group operations are scattered across Messenger/Zalo, spreadsheets, forms and memory.

Typical problems:
- Members repeatedly tell organizers when they are free.
- A person can join a group session that conflicts with a class or another activity.
- A badminton organizer manually counts registrations and waitlists.
- When somebody cancels, another person must be contacted manually.
- Court/player pairing is handled ad hoc every session.
- Scores are written somewhere, but ranking and long-term statistics are inconsistent.
- Nobody remembers who was supposed to bring shuttlecocks/equipment.
- Schedule changes must be repeated in chat and personal calendars.
- Study-group sessions and badminton sessions live in completely separate workflows despite sharing the same people and availability.

GroupSync links these problems through a shared personal/group model.

## 3. Product mental model

```text
USER
│
├── Personal Calendar / Busy Time / Class Schedule
│           │
│           └──> Availability
│
├── Study Group(s)
│     └── Study Sessions / Topic / Materials / Attendance / Goals
│
└── Badminton Group(s)
      └── Venue / Court / Session / Registration / Waitlist
          / Check-in / Court allocation / Pairing / Match / Result
          / Ranking / Stats / Equipment responsibilities / News
```

The calendar is not the whole product. It is shared infrastructure that makes group operations smarter.

## 4. Core product principle: Input once, derive many
Users should provide facts that only humans know. The system should derive everything else it reasonably can.

Examples:
- User creates `OOP class: Monday + Wednesday 08:00-10:00` once. GroupSync treats future occurrences as busy automatically.
- User joins a confirmed badminton session once. The activity appears in My Calendar automatically.
- Organizer changes the session time once. Participant GroupSync calendar events update automatically and new conflicts can be shown.
- Session reaches capacity. The next registrations become waitlist entries automatically.
- A registered player cancels. First eligible waitlist entry is promoted automatically.
- Score `21-17` is confirmed once. Winner/loss, stats, recent form, ranking history and optional system news are derived.
- A person responsible for shuttlecocks cancels. Responsibility becomes unassigned or is reassigned according to a simple policy and the organizer is notified.

## 5. Primary actors
### User
Any registered person. Can manage personal calendar, join groups, join sessions and view their history.

### Group Owner
Creates a group and manages high-level group settings/membership.

### Group Organizer
A user with operational permissions inside one group. Can create sessions, manage registration, courts, results, responsibilities and announcements.

### Member
Normal member of a group.

### Admin
Platform-level administration. Keep this role small in MVP.

Important:
`USER/ADMIN` are system roles. `OWNER/ORGANIZER/MEMBER` are membership roles inside a group.

## 6. Two verticals

### 6.1 Study Group
Purpose: coordinate recurring or ad-hoc study sessions using member availability.

Core features:
- study session creation;
- find common time;
- topic;
- goals;
- materials/links;
- attendance;
- notes/summary optional;
- announcement/notification.

Study Group is intentionally lighter than a full LMS. It proves the group/calendar architecture is reusable outside sports.

### 6.2 Badminton Group — flagship
Purpose: manage the real weekly lifecycle of a badminton group.

A normal lifecycle:

```text
Organizer chooses/creates a venue & courts
        ↓
Availability suggestions / session time
        ↓
Session opens
        ↓
Members register
        ↓
Capacity reached -> waitlist
        ↓
Check-in
        ↓
Allocate courts
        ↓
Pair players
        ↓
Play matches
        ↓
Submit + confirm results
        ↓
Auto stats/ranking/history/news
        ↓
Next session
```

## 7. Why this is an OOP project
The project has real objects with behavior and lifecycle rather than CRUD screens only.

Examples:
- `BadmintonSession.openRegistration()`
- `SessionRegistration.cancel()`
- `Match.confirmResult()`
- `WaitlistPolicy.promoteNext()`
- `SchedulingStrategy.suggestSlots()`
- `PairingStrategy.createPairings()`
- `RankingStrategy.calculate()`

OOP is used because the domain has:
- multiple entities interacting;
- business rules;
- state transitions;
- interchangeable algorithms;
- reusable group/calendar core;
- domain-specific behavior in Study and Badminton modules.

## 8. Success criteria
The project is successful if a real group can:
1. Create accounts and enter personal recurring schedules.
2. Create/join a group.
3. Find a practical shared time without asking everybody in chat.
4. Run a badminton session from registration through result/stat update.
5. Run a study session using the same calendar/group core.
6. Return later and see meaningful history, upcoming activities, ranking/news and notifications.
7. Use the app on a phone browser comfortably.
