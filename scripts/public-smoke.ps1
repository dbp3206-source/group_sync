param(
    [string]$PublicUrl = 'https://group-sync-khaki.vercel.app'
)

$ErrorActionPreference = 'Stop'
$PublicUrl = $PublicUrl.TrimEnd('/')
$RunId = (Get-Date).ToUniversalTime().ToString('yyyyMMddHHmmss')
$Password = 'GroupSync-Smoke-2026!'
$results = [System.Collections.Generic.List[object]]::new()

function Assert-Smoke([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw "PUBLIC_SMOKE_FAIL: $Message" }
}

function Add-Pass([string]$Area, [string]$Check) {
    $results.Add([PSCustomObject]@{ area = $Area; check = $Check; status = 'PASS' })
}

function Refresh-Csrf($Client) {
    $response = Invoke-RestMethod -UseBasicParsing -WebSession $Client.Session -Uri "$PublicUrl/api/auth/csrf"
    $Client.Csrf = $response.token
}

function Invoke-PublicApi($Client, [string]$Method, [string]$Path, $Body = $null) {
    $params = @{
        UseBasicParsing = $true
        WebSession = $Client.Session
        Uri = "$PublicUrl$Path"
        Method = $Method
    }
    if ($Method -notin @('GET', 'HEAD')) {
        Refresh-Csrf $Client
        $params.Headers = @{ 'X-XSRF-TOKEN' = $Client.Csrf }
    }
    if ($null -ne $Body) {
        $params.ContentType = 'application/json'
        $params.Body = ($Body | ConvertTo-Json -Depth 8)
    }
    try {
        $result = Invoke-RestMethod @params
    } catch {
        $status = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 'NO_STATUS' }
        $detail = $_.ErrorDetails.Message
        throw "PUBLIC_API_FAIL: $Method $Path -> $status $detail"
    }
    # Windows PowerShell 5 wraps top-level JSON arrays in an object that exposes
    # Count/value. Normalize it so list assertions behave the same as PowerShell 7.
    if ($null -ne $result -and
        $result.PSObject.Properties.Name -contains 'value' -and
        $result.PSObject.Properties.Name -contains 'Count') {
        return @($result.value)
    }
    return $result
}

function New-SmokeUser([string]$Label, [int]$Number) {
    $email = "smoke.$RunId.$Number@example.com"
    $client = [PSCustomObject]@{
        Session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
        Csrf = $null
        Email = $email
        Password = $Password
        User = $null
    }
    $client.User = Invoke-PublicApi $client POST '/api/auth/register' @{
        email = $email
        password = $Password
        displayName = $Label
    }
    Assert-Smoke ($client.User.email -eq $email) "Registration failed for $email."
    return $client
}

function Set-SmokeAvatar($Client) {
    Refresh-Csrf $Client
    $png = [Convert]::FromBase64String('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=')
    $boundary = '----GroupSyncSmoke' + [Guid]::NewGuid().ToString('N')
    $head = [Text.Encoding]::UTF8.GetBytes("--$boundary`r`nContent-Disposition: form-data; name=`"file`"; filename=`"avatar.png`"`r`nContent-Type: image/png`r`n`r`n")
    $tail = [Text.Encoding]::UTF8.GetBytes("`r`n--$boundary--`r`n")
    $body = New-Object byte[] ($head.Length + $png.Length + $tail.Length)
    [Array]::Copy($head, 0, $body, 0, $head.Length)
    [Array]::Copy($png, 0, $body, $head.Length, $png.Length)
    [Array]::Copy($tail, 0, $body, $head.Length + $png.Length, $tail.Length)
    $response = Invoke-RestMethod -UseBasicParsing -WebSession $Client.Session -Uri "$PublicUrl/api/users/me/avatar" -Method Post -Headers @{ 'X-XSRF-TOKEN' = $Client.Csrf } -ContentType "multipart/form-data; boundary=$boundary" -Body $body
    Assert-Smoke ($response.profileCompleted -eq $true) "Avatar/profile completion failed for $($Client.Email)."
    $Client.User = $response
}

function Invite-And-Accept($Owner, $Member, [long]$GroupId) {
    $invitation = Invoke-PublicApi $Owner POST "/api/groups/$GroupId/invitations" @{ email = $Member.Email }
    $pending = @(Invoke-PublicApi $Member GET '/api/groups/invitations/pending')
    Assert-Smoke (@($pending | Where-Object { $_.id -eq $invitation.id }).Count -eq 1) "Invitation was not visible to $($Member.Email)."
    $accepted = Invoke-PublicApi $Member POST "/api/groups/invitations/$($invitation.id)/accept"
    Assert-Smoke ($accepted.status -eq 'ACCEPTED') "Invitation acceptance failed for $($Member.Email)."
}

$health = Invoke-RestMethod -UseBasicParsing "$PublicUrl/api/health"
Assert-Smoke ($health.status -eq 'UP') 'Public health endpoint is not UP.'
Add-Pass 'deployment' 'Vercel same-origin API proxy and Render health'

$unauthorized = $null
try {
    Invoke-RestMethod -UseBasicParsing "$PublicUrl/api/auth/me" | Out-Null
    $unauthorized = 200
} catch {
    $unauthorized = [int]$_.Exception.Response.StatusCode
}
Assert-Smoke ($unauthorized -eq 401) 'Unauthenticated /auth/me did not return 401.'
Add-Pass 'security' 'Unauthenticated access rejected'

$owner = New-SmokeUser 'Smoke Owner' 1
$memberA = New-SmokeUser 'Smoke Member A' 2
$memberB = New-SmokeUser 'Smoke Member B' 3
$memberC = New-SmokeUser 'Smoke Member C' 4
$clients = @($owner, $memberA, $memberB, $memberC)

foreach ($client in $clients) {
    $updated = Invoke-PublicApi $client PATCH '/api/users/me/profile' @{ displayName = $client.User.displayName; timeZone = 'Asia/Bangkok' }
    Assert-Smoke ($updated.timeZone -eq 'Asia/Bangkok') "Profile update failed for $($client.Email)."
    Set-SmokeAvatar $client
}
Add-Pass 'auth-profile' 'Register four users, update profile, upload avatar'

Invoke-PublicApi $owner POST '/api/auth/logout' | Out-Null
$afterLogout = $null
try {
    Invoke-RestMethod -UseBasicParsing -WebSession $owner.Session -Uri "$PublicUrl/api/auth/me" | Out-Null
    $afterLogout = 200
} catch {
    $afterLogout = [int]$_.Exception.Response.StatusCode
}
Assert-Smoke ($afterLogout -eq 401) 'Logout did not invalidate the session.'
$login = Invoke-PublicApi $owner POST '/api/auth/login' @{ email = $owner.Email; password = $owner.Password }
Assert-Smoke ($login.email -eq $owner.Email) 'Login after logout failed.'
Add-Pass 'auth-profile' 'Logout, session invalidation, login again'

$eventStart = [DateTimeOffset]::UtcNow.AddDays(1)
$eventEnd = $eventStart.AddHours(2)
$event = Invoke-PublicApi $owner POST '/api/calendar/events' @{
    title = 'Production smoke event'
    start = $eventStart.ToString('o')
    end = $eventEnd.ToString('o')
    description = 'Created by the public deployment smoke test.'
    category = 'PERSONAL'
    location = 'Online'
    visibility = 'PRIVATE'
    reminderMinutes = 15
}
$duplicateEvent = Invoke-PublicApi $owner POST "/api/calendar/events/$($event.id)/duplicate"
$eventList = @(Invoke-PublicApi $owner GET '/api/calendar/events')
Assert-Smoke (@($eventList | Where-Object { $_.id -in @($event.id, $duplicateEvent.id) }).Count -eq 2) 'Calendar create/duplicate/list failed.'
$conflictStart = [uri]::EscapeDataString($eventStart.AddMinutes(15).ToString('o'))
$conflictEnd = [uri]::EscapeDataString($eventEnd.AddMinutes(-15).ToString('o'))
$conflicts = Invoke-PublicApi $owner GET "/api/calendar/conflicts?start=$conflictStart&end=$conflictEnd"
Assert-Smoke (@($conflicts.items).Count -ge 1) 'Calendar conflict detection failed.'

$today = [DateTime]::UtcNow.Date
$schedule = Invoke-PublicApi $owner POST '/api/calendar/recurring' @{
    title = 'Weekly smoke schedule'
    weekdays = @('MONDAY', 'WEDNESDAY')
    startTime = '09:00:00'
    endTime = '10:30:00'
    validFrom = $today.ToString('yyyy-MM-dd')
    validUntil = $today.AddMonths(1).ToString('yyyy-MM-dd')
    timezone = 'Asia/Bangkok'
    description = 'Recurring public smoke schedule.'
    category = 'CLASS'
    location = 'Room S1'
    visibility = 'PRIVATE'
    reminderMinutes = 10
    frequency = 'WEEKLY'
}
$duplicateSchedule = Invoke-PublicApi $owner POST "/api/calendar/recurring/$($schedule.id)/duplicate"
$scheduleList = @(Invoke-PublicApi $owner GET '/api/calendar/recurring')
Assert-Smoke (@($scheduleList | Where-Object { $_.id -in @($schedule.id, $duplicateSchedule.id) }).Count -eq 2) 'Recurring schedule create/duplicate/list failed.'
Invoke-PublicApi $owner DELETE "/api/calendar/events/$($duplicateEvent.id)" | Out-Null
Invoke-PublicApi $owner DELETE "/api/calendar/recurring/$($duplicateSchedule.id)" | Out-Null
Add-Pass 'calendar' 'Events, recurring schedules, conflict detection, duplicate and delete'

$studyGroup = Invoke-PublicApi $owner POST '/api/groups' @{ name = "Smoke Study $RunId"; description = 'Public smoke study group.'; type = 'STUDY' }
Invite-And-Accept $owner $memberA $studyGroup.id
$studyStart = [DateTimeOffset]::UtcNow.AddDays(3)
$study = Invoke-PublicApi $owner POST "/api/study/groups/$($studyGroup.id)/sessions" @{
    topic = 'Production smoke study session'
    goal = 'Validate the complete study workflow.'
    location = 'Library'
    start = $studyStart.ToString('o')
    end = $studyStart.AddHours(2).ToString('o')
    capacity = 8
}
Invoke-PublicApi $memberA POST "/api/study/sessions/$($study.id)/join" | Out-Null
$study = Invoke-PublicApi $owner POST "/api/study/sessions/$($study.id)/materials" @{ title = 'Smoke notes'; url = 'https://example.com/smoke-notes' }
$study = Invoke-PublicApi $owner POST "/api/study/sessions/$($study.id)/goals" @{ description = 'Complete the smoke checklist' }
$goal = @($study.goals) | Select-Object -First 1
Invoke-PublicApi $owner POST "/api/study/sessions/$($study.id)/goals/$($goal.id)/toggle" | Out-Null
Invoke-PublicApi $owner POST "/api/study/sessions/$($study.id)/confirm" | Out-Null
Invoke-PublicApi $owner PATCH "/api/study/sessions/$($study.id)/participants/$($memberA.User.id)/attendance" @{ attendance = 'ATTENDED' } | Out-Null
$studyDone = Invoke-PublicApi $owner POST "/api/study/sessions/$($study.id)/complete"
Assert-Smoke ($studyDone.status -eq 'COMPLETED') 'Study session did not complete.'
Add-Pass 'study' 'Group invitation, join, material, goal, attendance and lifecycle'

$badmintonGroup = Invoke-PublicApi $owner POST '/api/groups' @{ name = "Smoke Badminton $RunId"; description = 'Public smoke badminton group.'; type = 'BADMINTON' }
foreach ($member in @($memberA, $memberB, $memberC)) { Invite-And-Accept $owner $member $badmintonGroup.id }
$groupDetail = Invoke-PublicApi $owner GET "/api/groups/$($badmintonGroup.id)"
Assert-Smoke (@($groupDetail.members).Count -eq 4) 'Badminton group does not have four members.'

foreach ($client in $clients) {
    $profile = Invoke-PublicApi $client PUT "/api/badminton/groups/$($badmintonGroup.id)/profile" @{ skillLevel = 'INTERMEDIATE'; bio = 'Production smoke player.' }
    Assert-Smoke ($profile.skillLevel -eq 'INTERMEDIATE') "Badminton profile failed for $($client.Email)."
}
$season = @(Invoke-PublicApi $owner GET "/api/badminton/groups/$($badmintonGroup.id)/seasons") | Select-Object -First 1
Assert-Smoke ($null -ne $season.id) 'Default badminton season was not created.'
$venue = Invoke-PublicApi $owner POST "/api/badminton/groups/$($badmintonGroup.id)/venues" @{ name = 'Smoke Sports Hall'; address = 'Public deployment test venue' }
$court1 = Invoke-PublicApi $owner POST "/api/badminton/groups/$($badmintonGroup.id)/venues/$($venue.id)/courts" @{ name = 'Court 1' }
$court2 = Invoke-PublicApi $owner POST "/api/badminton/groups/$($badmintonGroup.id)/venues/$($venue.id)/courts" @{ name = 'Court 2' }

$sessionStart = [DateTimeOffset]::UtcNow.AddDays(4)
$badminton = Invoke-PublicApi $owner POST "/api/badminton/groups/$($badmintonGroup.id)/sessions" @{
    title = 'Production smoke badminton'
    start = $sessionStart.ToString('o')
    end = $sessionStart.AddHours(3).ToString('o')
    registrationDeadline = $sessionStart.AddDays(-1).ToString('o')
    capacity = 4
    seasonId = $season.id
    venueId = $venue.id
    # Four checked-in players on one court produces a complete 2v2 pairing.
    courtIds = @($court1.id)
}
Invoke-PublicApi $owner POST "/api/badminton/sessions/$($badminton.id)/open" | Out-Null
foreach ($client in $clients) { Invoke-PublicApi $client POST "/api/badminton/sessions/$($badminton.id)/registrations" | Out-Null }
$confirmedSession = Invoke-PublicApi $owner POST "/api/badminton/sessions/$($badminton.id)/confirm"
Assert-Smoke (@($confirmedSession.registrations | Where-Object { $_.status -eq 'REGISTERED' }).Count -eq 4) 'Badminton registration count is not four.'
foreach ($client in $clients) { Invoke-PublicApi $owner POST "/api/badminton/sessions/$($badminton.id)/registrations/$($client.User.id)/check-in" | Out-Null }
$responsibility = Invoke-PublicApi $owner POST "/api/badminton/sessions/$($badminton.id)/responsibilities" @{ itemName = 'Bring shuttlecocks'; note = 'Smoke test responsibility.' }
$assigned = Invoke-PublicApi $owner PATCH "/api/badminton/sessions/$($badminton.id)/responsibilities/$($responsibility.id)" @{ userId = $memberA.User.id }
Assert-Smoke ($assigned.assigneeId -eq $memberA.User.id) 'Responsibility assignment failed.'
Invoke-PublicApi $owner POST "/api/badminton/groups/$($badmintonGroup.id)/news" @{ title = 'Smoke announcement'; content = 'The production workflow is being verified.' } | Out-Null
Invoke-PublicApi $owner POST "/api/badminton/sessions/$($badminton.id)/start" | Out-Null
$allocations = @(Invoke-PublicApi $owner POST "/api/badminton/sessions/$($badminton.id)/allocations/generate?round=1")
$pairings = @(Invoke-PublicApi $owner GET "/api/badminton/sessions/$($badminton.id)/pairings?round=1&strategy=BALANCED&seed=42")
Assert-Smoke ($allocations.Count -ge 1 -and $pairings.Count -ge 1) 'Allocation or pairing generation failed.'
$pairing = $pairings | Select-Object -First 1
$match = Invoke-PublicApi $owner POST "/api/badminton/sessions/$($badminton.id)/matches" @{
    courtId = $pairing.courtId
    roundNumber = 1
    sideAUserIds = @($pairing.sideA | ForEach-Object { $_.userId })
    sideBUserIds = @($pairing.sideB | ForEach-Object { $_.userId })
}
Invoke-PublicApi $owner POST "/api/badminton/matches/$($match.id)/start" | Out-Null
Invoke-PublicApi $owner POST "/api/badminton/matches/$($match.id)/result" @{ scoreA = 21; scoreB = 17 } | Out-Null
$confirmedMatch = Invoke-PublicApi $owner POST "/api/badminton/matches/$($match.id)/confirm"
Assert-Smoke ($confirmedMatch.status -eq 'CONFIRMED' -and $confirmedMatch.winnerSide -eq 'A') 'Badminton match result confirmation failed.'
$completedSession = Invoke-PublicApi $owner POST "/api/badminton/sessions/$($badminton.id)/complete"
Assert-Smoke ($completedSession.status -eq 'COMPLETED') 'Badminton session did not complete.'

$leaderboard = @(Invoke-PublicApi $owner GET "/api/badminton/groups/$($badmintonGroup.id)/leaderboard?seasonId=$($season.id)")
$history = @(Invoke-PublicApi $owner GET "/api/badminton/groups/$($badmintonGroup.id)/players/$($owner.User.id)/ranking-history?seasonId=$($season.id)")
$news = @(Invoke-PublicApi $owner GET "/api/badminton/groups/$($badmintonGroup.id)/news")
$dashboard = Invoke-PublicApi $owner GET "/api/dashboard/groups/$($badmintonGroup.id)?seasonId=$($season.id)"
Assert-Smoke ($leaderboard.Count -ge 4 -and $history.Count -ge 1 -and $news.Count -ge 1 -and @($dashboard.recentResults).Count -ge 1) 'Derived ranking, history, news or dashboard data is missing.'
Add-Pass 'badminton' 'Profiles, venue/courts, registration, check-in, responsibilities, pairing, match, stats and dashboard'

$tournamentSessionStart = [DateTimeOffset]::UtcNow.AddDays(7)
$tournamentSession = Invoke-PublicApi $owner POST "/api/badminton/groups/$($badmintonGroup.id)/sessions" @{
    title = 'Production smoke tournament session'
    start = $tournamentSessionStart.ToString('o')
    end = $tournamentSessionStart.AddHours(4).ToString('o')
    registrationDeadline = $tournamentSessionStart.AddDays(-1).ToString('o')
    capacity = 8
    seasonId = $season.id
    venueId = $venue.id
    courtIds = @($court1.id, $court2.id)
}
$tournament = Invoke-PublicApi $owner POST "/api/tournaments/groups/$($badmintonGroup.id)" @{
    name = 'Production smoke singles tournament'
    seasonId = $season.id
    sessionId = $tournamentSession.id
    competitionMode = 'SINGLES'
    maxEntries = 8
}
Invoke-PublicApi $owner POST "/api/tournaments/$($tournament.id)/open" | Out-Null
$seed = 1
$entries = @()
foreach ($client in $clients) {
    $entries += Invoke-PublicApi $owner POST "/api/tournaments/$($tournament.id)/entries" @{ displayName = $client.User.displayName; memberIds = @($client.User.id); seedNumber = $seed }
    $seed++
}
$startedTournament = Invoke-PublicApi $owner POST "/api/tournaments/$($tournament.id)/start"
Assert-Smoke ($startedTournament.status -eq 'IN_PROGRESS') 'Tournament did not start.'
$bracket = @(Invoke-PublicApi $owner GET "/api/tournaments/$($tournament.id)/bracket")
while ($true) {
    $ready = @($bracket | Where-Object { $_.status -eq 'READY' }) | Select-Object -First 1
    if ($null -eq $ready) { break }
    Invoke-PublicApi $owner POST "/api/tournaments/$($tournament.id)/matches/$($ready.id)/winner" @{ winnerEntryId = $ready.entryA.id } | Out-Null
    $bracket = @(Invoke-PublicApi $owner GET "/api/tournaments/$($tournament.id)/bracket")
}
$tournamentList = @(Invoke-PublicApi $owner GET "/api/tournaments/groups/$($badmintonGroup.id)")
$finishedTournament = $tournamentList | Where-Object { $_.id -eq $tournament.id } | Select-Object -First 1
Assert-Smoke ($finishedTournament.status -eq 'COMPLETED' -and $null -ne $finishedTournament.championEntryId) 'Tournament bracket did not complete.'
Add-Pass 'tournament' 'Create, entries, bracket generation, winner progression and champion'

$availabilityFrom = [DateTimeOffset]::UtcNow.AddDays(8)
$availabilityTo = $availabilityFrom.AddDays(2)
$availability = @(Invoke-PublicApi $owner POST "/api/availability/groups/$($badmintonGroup.id)/search" @{
    from = $availabilityFrom.ToString('yyyy-MM-ddTHH:mm:00Z')
    to = $availabilityTo.ToString('yyyy-MM-ddTHH:mm:00Z')
    durationMinutes = 60
    requiredMemberIds = @($owner.User.id, $memberA.User.id)
    minimumAttendance = 2
    strategy = 'EARLIEST'
})
Assert-Smoke ($availability.Count -ge 1) 'Availability search returned no candidates.'
$preferences = @(Invoke-PublicApi $owner GET '/api/notifications/preferences')
Invoke-PublicApi $owner PATCH '/api/notifications/preferences/SESSION_CONFIRMED' @{ enabled = $false } | Out-Null
$notifications = @(Invoke-PublicApi $owner GET '/api/notifications')
if ($notifications.Count -gt 0) { Invoke-PublicApi $owner PATCH "/api/notifications/$($notifications[0].id)/read" | Out-Null }
Add-Pass 'supporting' 'Availability, notifications and preferences'

$from = [uri]::EscapeDataString([DateTimeOffset]::UtcNow.AddDays(-1).ToString('o'))
$to = [uri]::EscapeDataString([DateTimeOffset]::UtcNow.AddDays(12).ToString('o'))
$calendarItems = @(Invoke-PublicApi $owner GET "/api/calendar/items?from=$from&to=$to")
Assert-Smoke (@($calendarItems | Where-Object { $_.sourceType -in @('MANUAL', 'STUDY', 'BADMINTON') }).Count -ge 3) 'Aggregated calendar is missing expected source types.'
Add-Pass 'derived-data' 'Aggregated calendar receives personal, study and badminton items'

[PSCustomObject]@{
    status = 'PUBLIC_SMOKE_PASS'
    publicUrl = $PublicUrl
    runId = $RunId
    ownerEmail = $owner.Email
    studyGroupId = $studyGroup.id
    badmintonGroupId = $badmintonGroup.id
    badmintonSessionId = $badminton.id
    tournamentId = $tournament.id
    checks = $results
} | ConvertTo-Json -Depth 6
