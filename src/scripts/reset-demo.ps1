param(
    [string]$BackendUrl = $(if ($env:GROUPSYNC_BACKEND_URL) { $env:GROUPSYNC_BACKEND_URL } else { 'http://127.0.0.1:8080' }),
    [string]$DbHost = $(if ($env:GROUPSYNC_DB_HOST) { $env:GROUPSYNC_DB_HOST } else { '127.0.0.1' }),
    [int]$DbPort = $(if ($env:GROUPSYNC_DB_PORT) { [int]$env:GROUPSYNC_DB_PORT } else { 54329 }),
    [string]$DbName = $(if ($env:GROUPSYNC_DB_NAME) { $env:GROUPSYNC_DB_NAME } else { 'groupsync_dev' }),
    [string]$DbUser = $(if ($env:GROUPSYNC_DB_USER) { $env:GROUPSYNC_DB_USER } else { 'groupsync' })
)

$ErrorActionPreference = 'Stop'

if (-not $env:GROUPSYNC_DB_PASSWORD) { throw 'Set GROUPSYNC_DB_PASSWORD before seeding.' }
if (-not $env:GROUPSYNC_DEMO_PASSWORD) { throw 'Set GROUPSYNC_DEMO_PASSWORD before seeding.' }
$psqlPath = $null
$psqlCommand = Get-Command psql.exe -ErrorAction SilentlyContinue
if ($psqlCommand) {
    $psqlPath = $psqlCommand.Source
} else {
    $defaultPsql = Get-ChildItem 'C:\Program Files\PostgreSQL' -Filter psql.exe -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($defaultPsql) { $psqlPath = $defaultPsql.FullName }
}
if (-not $psqlPath) { throw 'psql.exe was not found on PATH or under C:\Program Files\PostgreSQL.' }
$env:PGPASSWORD = $env:GROUPSYNC_DB_PASSWORD

function Invoke-Psql([string]$Sql) {
    & $psqlPath -h $DbHost -p $DbPort -U $DbUser -d $DbName --set=ON_ERROR_STOP=1 -c $Sql
    if ($LASTEXITCODE -ne 0) { throw "psql failed with exit code $LASTEXITCODE." }
}

function New-DemoAccount([string]$Email, [string]$DisplayName) {
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    Invoke-WebRequest -UseBasicParsing -WebSession $session -Uri "$BackendUrl/api/auth/csrf" | Out-Null
    $token = $session.Cookies.GetCookies($BackendUrl)['XSRF-TOKEN'].Value
    $body = @{ email = $Email; password = $env:GROUPSYNC_DEMO_PASSWORD; displayName = $DisplayName } | ConvertTo-Json
    Invoke-RestMethod -UseBasicParsing -WebSession $session -Uri "$BackendUrl/api/auth/register" -Method Post -Headers @{ 'X-XSRF-TOKEN' = $token } -ContentType 'application/json' -Body $body | Out-Null
}

Invoke-RestMethod -UseBasicParsing "$BackendUrl/api/health" | Out-Null

Invoke-Psql @'
TRUNCATE TABLE
    tournament_matches, tournament_participants, tournaments,
    badminton_checkin_tokens, notification_preferences,
    badminton_match_participants, badminton_match_sides, badminton_matches,
    badminton_ranking_history, badminton_player_stats, badminton_allocation_players,
    badminton_allocations, badminton_responsibilities, badminton_registrations,
    badminton_session_courts, badminton_sessions, badminton_courts, badminton_venues,
    badminton_profiles, badminton_seasons, group_news, notifications,
    study_goals, study_materials, study_participants, study_sessions,
    weekly_schedule_days, weekly_schedules, personal_busy_events,
    group_invitations, group_memberships, groups, users
RESTART IDENTITY CASCADE;
'@

New-DemoAccount 'demo.admin@groupsync.local' 'Demo Admin'
New-DemoAccount 'demo.organizer@groupsync.local' 'Demo Main Organizer'
1..17 | ForEach-Object { New-DemoAccount ("demo.member{0:D2}@groupsync.local" -f $_) ("Demo Badminton Member {0:D2}" -f $_) }
New-DemoAccount 'demo.study01@groupsync.local' 'Demo Study Guest 01'
New-DemoAccount 'demo.study02@groupsync.local' 'Demo Study Guest 02'

Invoke-Psql @'
UPDATE users SET profile_completed = TRUE, time_zone = 'Asia/Bangkok';

UPDATE users SET system_role = 'ADMIN' WHERE email = 'demo.admin@groupsync.local';

INSERT INTO groups (name, description, group_type, created_at)
VALUES
    ('Demo Study Group', 'Stable Study Group data for final presentation.', 'STUDY', now()),
    ('Demo Badminton Group', 'Stable Badminton flagship data for final presentation.', 'BADMINTON', now());

INSERT INTO group_memberships (group_id, user_id, role, created_at)
SELECT g.id, u.id, CASE WHEN u.email = 'demo.organizer@groupsync.local' THEN 'OWNER' ELSE 'MEMBER' END, now()
FROM groups g CROSS JOIN users u
WHERE g.name = 'Demo Badminton Group'
  AND (u.email = 'demo.organizer@groupsync.local' OR u.email ~ '^demo\.member(0[1-9]|1[0-7])@groupsync\.local$');

INSERT INTO group_memberships (group_id, user_id, role, created_at)
SELECT g.id, u.id, CASE WHEN u.email = 'demo.organizer@groupsync.local' THEN 'OWNER' ELSE 'MEMBER' END, now()
FROM groups g CROSS JOIN users u
WHERE g.name = 'Demo Study Group'
  AND (u.email IN ('demo.organizer@groupsync.local', 'demo.study01@groupsync.local', 'demo.study02@groupsync.local')
       OR u.email ~ '^demo\.member0[1-5]@groupsync\.local$');

INSERT INTO badminton_profiles (membership_id, skill_level, bio, updated_at)
SELECT m.id,
       CASE WHEN u.email ~ 'demo\.member(0[1-5])@' THEN 'ADVANCED' WHEN u.email ~ 'demo\.member(0[6-9]|1[0-2])@' THEN 'INTERMEDIATE' ELSE 'BEGINNER' END,
       'Demo profile for OOP defense.', now()
FROM group_memberships m JOIN users u ON u.id = m.user_id JOIN groups g ON g.id = m.group_id
WHERE g.name = 'Demo Badminton Group';

INSERT INTO personal_busy_events (user_id, title, start_at, end_at, created_at, updated_at)
SELECT u.id, 'Demo focused study block',
       ((CURRENT_DATE + 3)::date + time '12:00') AT TIME ZONE 'Asia/Bangkok',
       ((CURRENT_DATE + 3)::date + time '13:00') AT TIME ZONE 'Asia/Bangkok', now(), now()
FROM users u WHERE u.email IN ('demo.organizer@groupsync.local', 'demo.member01@groupsync.local');

INSERT INTO weekly_schedules (user_id, title, start_time, end_time, valid_from, valid_until, timezone, created_at, updated_at)
SELECT id, 'OOP class recurring schedule', time '18:00', time '20:00', CURRENT_DATE - 30, CURRENT_DATE + 180, 'Asia/Bangkok', now(), now()
FROM users WHERE email = 'demo.organizer@groupsync.local';
INSERT INTO weekly_schedule_days (schedule_id, day_of_week)
SELECT id, day FROM weekly_schedules CROSS JOIN (VALUES ('MONDAY'), ('WEDNESDAY')) AS d(day)
WHERE title = 'OOP class recurring schedule';

INSERT INTO study_sessions (group_id, organizer_id, topic, goal, location, start_at, end_at, capacity, status, created_at, updated_at)
SELECT g.id, u.id, 'OOP defense rehearsal', 'Trace React to REST to service, domain and repository.', 'Room B / local demo',
       ((CURRENT_DATE + 3)::date + time '09:00') AT TIME ZONE 'Asia/Bangkok',
       ((CURRENT_DATE + 3)::date + time '10:30') AT TIME ZONE 'Asia/Bangkok', 8, 'CONFIRMED', now(), now()
FROM groups g CROSS JOIN users u WHERE g.name = 'Demo Study Group' AND u.email = 'demo.organizer@groupsync.local';
INSERT INTO study_participants (session_id, user_id, attendance, joined_at)
SELECT s.id, u.id, CASE WHEN u.email = 'demo.study01@groupsync.local' THEN 'ATTENDED' ELSE 'REGISTERED' END, now()
FROM study_sessions s CROSS JOIN users u
WHERE s.topic = 'OOP defense rehearsal'
  AND u.email IN ('demo.organizer@groupsync.local', 'demo.member01@groupsync.local', 'demo.member02@groupsync.local', 'demo.study01@groupsync.local');
INSERT INTO study_materials (session_id, title, url)
SELECT id, 'OOP defense checklist', 'https://example.invalid/groupsync/oop-checklist' FROM study_sessions WHERE topic = 'OOP defense rehearsal';
INSERT INTO study_goals (session_id, description, completed)
SELECT id, 'Explain one real Strategy implementation', TRUE FROM study_sessions WHERE topic = 'OOP defense rehearsal';
INSERT INTO study_goals (session_id, description, completed)
SELECT id, 'Trace waitlist promotion to notification', FALSE FROM study_sessions WHERE topic = 'OOP defense rehearsal';

INSERT INTO badminton_seasons (group_id, name, starts_on, ends_on, active, created_at)
SELECT id, 'Season 1', CURRENT_DATE - 30, CURRENT_DATE + 180, TRUE, now() FROM groups WHERE name = 'Demo Badminton Group';
INSERT INTO badminton_venues (group_id, name, address, created_at)
SELECT id, 'Demo Sports Hall', 'Campus Building A', now() FROM groups WHERE name = 'Demo Badminton Group';
INSERT INTO badminton_courts (venue_id, name, active)
SELECT v.id, 'Court ' || n, TRUE FROM badminton_venues v CROSS JOIN generate_series(1, 4) AS n WHERE v.name = 'Demo Sports Hall';

INSERT INTO badminton_sessions (group_id, season_id, venue_id, title, start_at, end_at, registration_deadline, capacity, status, created_at, updated_at)
SELECT g.id, s.id, v.id, 'Historical Demo Session',
       ((CURRENT_DATE - 21)::date + time '19:00') AT TIME ZONE 'Asia/Bangkok',
       ((CURRENT_DATE - 21)::date + time '21:00') AT TIME ZONE 'Asia/Bangkok',
       ((CURRENT_DATE - 22)::date + time '19:00') AT TIME ZONE 'Asia/Bangkok', 16, 'COMPLETED', now(), now()
FROM groups g JOIN badminton_seasons s ON s.group_id = g.id JOIN badminton_venues v ON v.group_id = g.id
WHERE g.name = 'Demo Badminton Group';

INSERT INTO badminton_sessions (group_id, season_id, venue_id, title, start_at, end_at, registration_deadline, capacity, status, created_at, updated_at)
SELECT g.id, s.id, v.id, 'Demo Golden Session',
       ((CURRENT_DATE + 5)::date + time '19:00') AT TIME ZONE 'Asia/Bangkok',
       ((CURRENT_DATE + 5)::date + time '21:00') AT TIME ZONE 'Asia/Bangkok',
       ((CURRENT_DATE + 4)::date + time '19:00') AT TIME ZONE 'Asia/Bangkok', 16, 'CONFIRMED', now(), now()
FROM groups g JOIN badminton_seasons s ON s.group_id = g.id JOIN badminton_venues v ON v.group_id = g.id
WHERE g.name = 'Demo Badminton Group';

INSERT INTO badminton_session_courts (session_id, court_id)
SELECT s.id, c.id FROM badminton_sessions s JOIN badminton_venues v ON v.id = s.venue_id JOIN badminton_courts c ON c.venue_id = v.id
WHERE s.title IN ('Historical Demo Session', 'Demo Golden Session');

INSERT INTO badminton_registrations (session_id, user_id, status, queued_at, registered_at, updated_at)
SELECT s.id, u.id, 'CHECKED_IN', NULL, now() - interval '20 days', now()
FROM badminton_sessions s CROSS JOIN users u
WHERE s.title = 'Historical Demo Session' AND u.email ~ '^demo\.member0[1-8]@groupsync\.local$';
INSERT INTO badminton_registrations (session_id, user_id, status, queued_at, registered_at, updated_at)
SELECT s.id, u.id, 'REGISTERED', NULL, now() - interval '2 hours', now()
FROM badminton_sessions s CROSS JOIN users u
WHERE s.title = 'Demo Golden Session'
  AND (u.email = 'demo.organizer@groupsync.local' OR u.email ~ '^demo\.member(0[1-9]|1[0-5])@groupsync\.local$');

INSERT INTO badminton_responsibilities (session_id, item_name, status, assignee_id, note)
SELECT s.id, 'Shuttlecock tube', 'ASSIGNED', u.id, 'Cancellation should release this responsibility.'
FROM badminton_sessions s CROSS JOIN users u WHERE s.title = 'Demo Golden Session' AND u.email = 'demo.member01@groupsync.local';
INSERT INTO badminton_responsibilities (session_id, item_name, status, assignee_id, note)
SELECT s.id, 'First-aid kit', 'NEEDED', NULL, 'Organizer can assign this during defense.'
FROM badminton_sessions s WHERE s.title = 'Demo Golden Session';

INSERT INTO badminton_allocations (session_id, court_id, round_number, status, created_at)
SELECT s.id, c.id, 1, 'CONFIRMED', now() - interval '20 days'
FROM badminton_sessions s JOIN badminton_session_courts sc ON sc.session_id = s.id JOIN badminton_courts c ON c.id = sc.court_id
WHERE s.title = 'Historical Demo Session';
WITH allocation_order AS (
    SELECT a.id, row_number() OVER (ORDER BY c.name) AS court_no FROM badminton_allocations a JOIN badminton_courts c ON c.id = a.court_id JOIN badminton_sessions s ON s.id = a.session_id WHERE s.title = 'Historical Demo Session'
), player_order AS (
    SELECT u.id, row_number() OVER (ORDER BY u.email) AS player_no FROM users u WHERE u.email ~ '^demo\.member0[1-8]@groupsync\.local$'
)
INSERT INTO badminton_allocation_players (allocation_id, user_id, position)
SELECT a.id, p.id, ((p.player_no - 1) % 2) + 1 FROM allocation_order a JOIN player_order p ON ((p.player_no - 1) / 2) + 1 = a.court_no;

INSERT INTO badminton_matches (session_id, season_id, court_id, round_number, status, score_a, score_b, winner_side, created_at, updated_at)
SELECT s.id, s.season_id, c.id, 1, 'CONFIRMED', 21, 17, 'A', now() - interval '20 days', now() - interval '20 days'
FROM badminton_sessions s JOIN badminton_courts c ON c.name = 'Court 1' AND c.venue_id = s.venue_id WHERE s.title = 'Historical Demo Session';
INSERT INTO badminton_match_sides (match_id, side_code)
SELECT m.id, code FROM badminton_matches m CROSS JOIN (VALUES ('A'), ('B')) AS sides(code) JOIN badminton_sessions s ON s.id = m.session_id WHERE s.title = 'Historical Demo Session';
INSERT INTO badminton_match_participants (side_id, user_id)
SELECT ms.id, u.id FROM badminton_match_sides ms JOIN badminton_matches m ON m.id = ms.match_id CROSS JOIN users u
WHERE m.session_id = (SELECT id FROM badminton_sessions WHERE title = 'Historical Demo Session')
  AND ((ms.side_code = 'A' AND u.email IN ('demo.member01@groupsync.local', 'demo.member02@groupsync.local')) OR (ms.side_code = 'B' AND u.email IN ('demo.member03@groupsync.local', 'demo.member04@groupsync.local')));
INSERT INTO badminton_player_stats (group_id, season_id, user_id, matches_played, wins, losses, points, attended, no_shows)
SELECT g.id, s.id, u.id, 1, CASE WHEN u.email ~ 'demo\.member0[1-2]@' THEN 1 ELSE 0 END, CASE WHEN u.email ~ 'demo\.member0[3-4]@' THEN 1 ELSE 0 END, CASE WHEN u.email ~ 'demo\.member0[1-2]@' THEN 3 ELSE 1 END, 1, 0
FROM groups g JOIN badminton_seasons s ON s.group_id = g.id CROSS JOIN users u WHERE g.name = 'Demo Badminton Group' AND u.email ~ '^demo\.member0[1-4]@groupsync\.local$';
INSERT INTO badminton_player_stats (group_id, season_id, user_id, matches_played, wins, losses, points, attended, no_shows)
SELECT g.id, s.id, u.id, 0, 0, 0, 0, 1, 0 FROM groups g JOIN badminton_seasons s ON s.group_id = g.id CROSS JOIN users u
WHERE g.name = 'Demo Badminton Group' AND u.email ~ '^demo\.member0[5-8]@groupsync\.local$';
INSERT INTO badminton_ranking_history (match_id, group_id, season_id, user_id, points_after, wins_after, matches_after, created_at)
SELECT m.id, g.id, s.id, u.id, CASE WHEN u.email ~ 'demo\.member0[1-2]@' THEN 3 ELSE 1 END, CASE WHEN u.email ~ 'demo\.member0[1-2]@' THEN 1 ELSE 0 END, 1, now() - interval '20 days'
FROM badminton_matches m JOIN badminton_sessions bs ON bs.id = m.session_id JOIN groups g ON g.id = bs.group_id JOIN badminton_seasons s ON s.id = bs.season_id CROSS JOIN users u
WHERE bs.title = 'Historical Demo Session' AND u.email ~ '^demo\.member0[1-4]@groupsync\.local$';
INSERT INTO group_news (group_id, author_id, news_type, title, content, source_key, created_at)
SELECT g.id, NULL, 'MATCH_RESULT', 'Historical demo result', 'Court 1 finished 21-17. Ranking and history were derived.', 'MATCH_RESULT:DEMO_HISTORICAL', now() - interval '20 days' FROM groups g WHERE g.name = 'Demo Badminton Group';
INSERT INTO notifications (user_id, notification_type, title, message, target_type, target_id, is_read, created_at)
SELECT u.id, 'DEMO_WELCOME', 'Demo data ready', 'Use the golden demo guide to trace automation.', 'GROUP', g.id, FALSE, now()
FROM users u CROSS JOIN groups g WHERE u.email = 'demo.organizer@groupsync.local' AND g.name = 'Demo Badminton Group';
'@
Write-Output 'DEMO_SEED_PASS users=21 groups=2 studySessions=1 badmintonSessions=2 historicalMatch=1 upcomingSessionCapacity=16 preRegistered=16'
Write-Output 'Demo password was supplied through GROUPSYNC_DEMO_PASSWORD and is not stored in this repository.'
