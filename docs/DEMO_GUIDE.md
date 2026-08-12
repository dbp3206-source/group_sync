# GroupSync Final Demo Guide

This guide prepares a clean local demo for the final presentation. It uses the real GroupSync APIs and database rules; the seed only shortens the amount of manual data entry.

## 1. Prerequisites

- Windows PowerShell.
- PostgreSQL 17 local instance on `127.0.0.1:54329`, database `groupsync_dev`, user `groupsync`.
- Java 21 runtime available at `../.jdk21-runtime` from the repository root.
- Node.js/npm.
- Repository checkout at `C:\Users\Bao Phuc\Documents\GroupSync_Build`.

The seed script automatically finds `psql.exe` under `C:\Program Files\PostgreSQL` or on `PATH`.

## 2. Start the local services

Check PostgreSQL:

```powershell
& 'C:\Program Files\PostgreSQL\17\bin\pg_isready.exe' -h 127.0.0.1 -p 54329 -U groupsync -d groupsync_dev
```

If it is stopped, open Windows Services, select the local PostgreSQL 17 service, and choose **Start**. No cloud account or external service is needed.

Start the backend in terminal 1:

```powershell
cd C:\Users\Bao Phuc\Documents\GroupSync_Build\backend
$env:JAVA_HOME=(Resolve-Path '../.jdk21-runtime').Path
$env:DB_PASSWORD='<local PostgreSQL password>'
.\mvnw.cmd spring-boot:run
```

Wait for `Started GroupSyncBackendApplication` and verify `http://127.0.0.1:8080/api/health` returns a successful response.

Create the reproducible demo state from the repository root. The password is supplied through the environment and is not stored in source:

```powershell
cd C:\Users\Bao Phuc\Documents\GroupSync_Build
$env:GROUPSYNC_DB_PASSWORD='<local PostgreSQL password>'
$env:GROUPSYNC_DEMO_PASSWORD='DemoOnly-GroupSync-2026!'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\reset-demo.ps1
```

The reset script truncates only the GroupSync development database and recreates the demo users, groups, schedules, sessions, results, rankings, news and notifications. Run it before each rehearsal so the golden flow starts from the same state.

Start the frontend in terminal 2:

```powershell
cd C:\Users\Bao Phuc\Documents\GroupSync_Build\frontend
npm.cmd install
npm.cmd run dev
```

Open the printed Vite URL, normally `http://127.0.0.1:5173`.

## 3. Demo accounts

All demo accounts use the same local-only password: `DemoOnly-GroupSync-2026!`. It is intentionally a demo credential, not a production secret. Do not reuse it outside this local presentation.

| Account | Purpose |
|---|---|
| `demo.organizer@groupsync.local` | Main owner and organizer |
| `demo.admin@groupsync.local` | System ADMIN account, if needed |
| `demo.member17@groupsync.local` | Waitlist/promotion demonstration |
| `demo.member01@groupsync.local` | Registered member who cancels; historical winner |
| `demo.member02` ... `demo.member16` | Additional badminton members |
| `demo.study01@groupsync.local` | Study attendance sample |
| `demo.study02@groupsync.local` | Study group member |

## 4. Golden demo: 8–12 minutes

The seeded `Demo Golden Session` is already **CONFIRMED**, has four courts, capacity 16, and 16 registered players. This is intentional: it leaves time to demonstrate automation instead of typing 17 registrations.

### Manual UI story

1. Log in as `demo.organizer@groupsync.local`.
   - Expected: the authenticated home/dashboard opens.
2. Open **Calendar**.
   - Expected: the `OOP class recurring schedule` appears as recurring busy time; confirmed study and badminton activities appear as derived calendar items.
3. Open **Groups → Demo Badminton Group → Open badminton operations**, then click the `Demo Golden Session` title.
   - Expected: the session detail shows capacity 16, four courts, responsibilities and registered participants. The actual allocation, pairing and result controls are on the **Badminton** operations page.
4. Log out and log in as `demo.member17@groupsync.local`; join the golden session.
   - Expected: the member is `WAITLISTED` because the 16 active places are full.
5. Log in as `demo.member01@groupsync.local`; leave the session.
   - Expected: the oldest waitlisted member is promoted automatically. `member17` becomes `REGISTERED` and receives a `WAITLIST_PROMOTED` notification.
6. Log back in as the organizer and open session detail.
   - Expected: there are 16 registered players again. The promoted member’s personal calendar contains `Badminton: Demo Golden Session`.
7. On the **Badminton** operations page, check in all registered players and start the session.
   - Expected: registration status changes to `CHECKED_IN`, then the session becomes `PLAYING`.
   - Manual note: this is 16 repeated `check in` clicks and the page refreshes after each action. For an 8–12 minute defense, run `scripts/demo-golden.ps1` for the full real flow and demonstrate one or two visible check-in changes in the UI.
8. Choose **Generate allocation** for round 1.
   - Expected: 16 checked-in players are distributed across four courts.
9. Choose **Balanced pairing**.
   - Expected: each court suggests a 2-versus-2 doubles pairing.
10. Create a match for the first court, start it, enter `21` and `17`, then confirm.
    - Expected: winner side, W/L, points and ranking history are derived from the score. The user does not enter winner or statistics manually.
11. Open **Leaderboard**, **Recent results**, **News**, and **Notifications**.
    - Expected: the result appears in all relevant views, including system-generated news and participant notifications.
12. To show the winner’s Statistics and Ranking history, open `http://localhost:5173/badminton/profile?groupId=2&userId=3` for seeded `demo.member01`.
    - Expected: Season 1 statistics and the confirmed match history are visible. This profile route is currently not linked from the main navigation, so keep the URL ready or use the API result during the defense.

### One-command verification of the same real flow

After running `reset-demo.ps1`, run:

```powershell
$env:GROUPSYNC_DEMO_PASSWORD='DemoOnly-GroupSync-2026!'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\demo-golden.ps1
```

Expected output starts with `GOLDEN_DEMO_PASS` and reports `registered=16`, `allocations=4`, `pairings=4`, `doubles=2v2`, `score=21-17`, plus leaderboard/history/news counts. The script uses the real REST endpoints; it does not insert fake result data.

## 5. Study Group and availability story

Open **Demo Study Group** as the organizer.

- Upcoming session: `OOP defense rehearsal`.
- Its topic, goal, material and attendance sample are seeded.
- The organizer has a recurring Monday/Wednesday class and a manual busy block, so availability has both busy and candidate periods.
- The recurring schedule is stored once and expanded by the calendar aggregator; the UI does not require entering every week separately.

## 6. Simple architecture explanation

```text
React page
  -> REST Controller (HTTP input/output)
  -> Service (authorization, transaction and use-case coordination)
  -> Domain Model (state transitions and protected business rules)
  -> Repository (JPA persistence/query)
  -> PostgreSQL
```

```text
Personal busy event + recurring schedule
Study session + Badminton session
  -> Calendar sources
  -> Calendar Aggregator
  -> Availability Engine
  -> candidate slots / conflict warning / group operations
```

The sources normalize different busy information into one calendar view. A confirmed group session is derived onto participant calendars; cancellation or rescheduling changes the derived item instead of asking participants to edit it manually.

## 7. Fallbacks and demo reset

- **Backend cannot start:** confirm PostgreSQL is listening on port 54329, set `DB_PASSWORD`, and retry `mvnw.cmd spring-boot:run`.
- **`psql.exe` not found:** install PostgreSQL 17 locally or add its `bin` directory to PATH; the script also checks the default installation folder.
- **PowerShell blocks the script:** keep the documented `powershell.exe -ExecutionPolicy Bypass -File ...` command; it changes no system policy.
- **The golden session is already PLAYING/COMPLETED or a registration already exists:** run `reset-demo.ps1` again.
- **Frontend shows an API/network error:** keep backend on port 8080, restart Vite, and reload `http://127.0.0.1:5173`.

The main demo risk is state mutation: after a successful golden run the session is PLAYING and has a match. Resetting the development database restores the rehearsal state. PostgreSQL credentials remain local environment values and are never committed.

## 8. Rehearsal observations

- Backend startup took about 17 seconds on the rehearsal machine; this is normal while Spring initializes JPA and Flyway.
- Seed reset took about 11 seconds and the complete real golden script took about 7 seconds after services were ready.
- The manual multi-account waitlist story requires switching organizer, `member17`, and `member01`; it is easy to lose the intended account. Keep the three accounts and password visible.
- The business rules are clearest on the session card: `16 / 16`, `WAITLISTED`, `REGISTERED`, `CHECKED_IN`, `PLAYING`, court allocation, 2-vs-2 pairing, and the result status are visible. Statistics/history are less discoverable because the profile route has no navigation link.
- The frontend runtime is available at `http://localhost:5173` when that port is free. If Vite selects another port, use the URL printed by Vite and adjust the profile URL accordingly.

For oral-defense preparation, see [OOP_DEFENSE_GUIDE.md](OOP_DEFENSE_GUIDE.md).
