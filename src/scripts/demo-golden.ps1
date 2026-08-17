param(
    [string]$BackendUrl = $(if ($env:GROUPSYNC_BACKEND_URL) { $env:GROUPSYNC_BACKEND_URL } else { 'http://127.0.0.1:8080' })
)

$ErrorActionPreference = 'Stop'
if (-not $env:GROUPSYNC_DEMO_PASSWORD) { throw 'Set GROUPSYNC_DEMO_PASSWORD before running the golden demo.' }

function Assert-Demo([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw "GOLDEN_DEMO_FAIL: $Message" }
}

function New-DemoSession([string]$Email) {
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    Invoke-WebRequest -UseBasicParsing -WebSession $session -Uri "$BackendUrl/api/auth/csrf" | Out-Null
    $token = $session.Cookies.GetCookies($BackendUrl)['XSRF-TOKEN'].Value
    $body = @{ email = $Email; password = $env:GROUPSYNC_DEMO_PASSWORD } | ConvertTo-Json
    Invoke-RestMethod -UseBasicParsing -WebSession $session -Uri "$BackendUrl/api/auth/login" -Method Post -Headers @{ 'X-XSRF-TOKEN' = $token } -ContentType 'application/json' -Body $body | Out-Null
    return [PSCustomObject]@{ Session = $session; Csrf = $token; Email = $Email }
}

function Invoke-DemoApi($Client, [string]$Method, [string]$Path, $Body = $null) {
    $params = @{ UseBasicParsing = $true; WebSession = $Client.Session; Uri = "$BackendUrl$Path"; Method = $Method; Headers = @{ 'X-XSRF-TOKEN' = $Client.Csrf } }
    if ($null -ne $Body) { $params.ContentType = 'application/json'; $params.Body = ($Body | ConvertTo-Json -Depth 6) }
    if ($Method -eq 'GET') { $params.Remove('Headers') | Out-Null }
    $result = Invoke-RestMethod @params
    if ($result.PSObject.Properties.Name -contains 'value' -and $result.PSObject.Properties.Name -contains 'Count') {
        return @($result.value)
    }
    return $result
}

Invoke-RestMethod -UseBasicParsing "$BackendUrl/api/health" | Out-Null
$organizer = New-DemoSession 'demo.organizer@groupsync.local'
$member17 = New-DemoSession 'demo.member17@groupsync.local'
$member01 = New-DemoSession 'demo.member01@groupsync.local'

$groups = @(Invoke-DemoApi $organizer GET '/api/groups')
$badmintonMatches = @($groups | Where-Object { $_.name -eq 'Demo Badminton Group' })
Assert-Demo ($badmintonMatches.Count -eq 1) 'Demo Badminton Group was not found or was duplicated.'
$badminton = $badmintonMatches[0]

$sessions = @(Invoke-DemoApi $organizer GET ("/api/badminton/groups/{0}/sessions" -f $badminton.id))
$goldenMatches = @($sessions | Where-Object { $_.title -eq 'Demo Golden Session' })
Assert-Demo ($goldenMatches.Count -eq 1) 'Demo Golden Session was not found or was duplicated. Run reset-demo.ps1 first.'
$golden = $goldenMatches[0]
Assert-Demo ($golden.status -eq 'CONFIRMED') 'Golden session is not CONFIRMED. Run reset-demo.ps1 first.'
Assert-Demo ($golden.capacity -eq 16) 'Golden session capacity is not 16.'

# Real waitlist flow: member17 joins at capacity, then member01 leaves and member17 is promoted.
Invoke-DemoApi $member17 POST ("/api/badminton/sessions/{0}/registrations" -f $golden.id) | Out-Null
$afterWaitlist = Invoke-DemoApi $organizer GET ("/api/badminton/sessions/{0}" -f $golden.id)
$member17User = Invoke-DemoApi $member17 GET '/api/auth/me'
$member01User = Invoke-DemoApi $member01 GET '/api/auth/me'
$member17Registration = @($afterWaitlist.registrations | Where-Object { $_.userId -eq $member17User.id }) | Select-Object -First 1
Assert-Demo ($null -ne $member17Registration -and $member17Registration.status -eq 'WAITLISTED') 'member17 did not enter the waitlist.'

Invoke-DemoApi $member01 DELETE ("/api/badminton/sessions/{0}/registrations/me" -f $golden.id) | Out-Null
$afterPromotion = Invoke-DemoApi $organizer GET ("/api/badminton/sessions/{0}" -f $golden.id)
$member17Registration = @($afterPromotion.registrations | Where-Object { $_.userId -eq $member17User.id }) | Select-Object -First 1
Assert-Demo ($null -ne $member17Registration -and $member17Registration.status -eq 'REGISTERED') 'member17 was not auto-promoted.'
$member17Notifications = @(Invoke-DemoApi $member17 GET '/api/notifications')
$promotionNotifications = @($member17Notifications | Where-Object { $_.type -eq 'WAITLIST_PROMOTED' })
Assert-Demo ($promotionNotifications.Count -ge 1) 'Promotion notification was not created.'

$from = [uri]::EscapeDataString([DateTimeOffset]::UtcNow.AddDays(-1).ToString('o'))
$to = [uri]::EscapeDataString([DateTimeOffset]::UtcNow.AddDays(8).ToString('o'))
$calendar = @(Invoke-DemoApi $member17 GET ("/api/calendar/items?from={0}&to={1}" -f $from, $to))
$badmintonCalendar = @($calendar | Where-Object { $_.sourceType -eq 'BADMINTON' -and $_.title -eq 'Badminton: Demo Golden Session' })
Assert-Demo ($badmintonCalendar.Count -ge 1) 'Promoted member calendar was not updated.'

# Check in the 16 registered players, start the session, then use the real allocation/pairing/match APIs.
$registered = @($afterPromotion.registrations | Where-Object { $_.status -eq 'REGISTERED' })
Assert-Demo ($registered.Count -eq 16) ("Expected 16 registered players, found {0}." -f $registered.Count)
foreach ($registration in $registered) {
    Invoke-DemoApi $organizer POST ("/api/badminton/sessions/{0}/registrations/{1}/check-in" -f $golden.id, $registration.userId) | Out-Null
}
Invoke-DemoApi $organizer POST ("/api/badminton/sessions/{0}/start" -f $golden.id) | Out-Null

$allocations = @(Invoke-DemoApi $organizer POST ("/api/badminton/sessions/{0}/allocations/generate?round=1" -f $golden.id))
$allocationPlayers = @($allocations | ForEach-Object { @($_.players) }).Count
Assert-Demo ($allocations.Count -eq 4 -and $allocationPlayers -eq 16) ("Expected 4 courts and 16 allocated players, found {0} courts/{1} players." -f $allocations.Count, $allocationPlayers)

$pairings = @(Invoke-DemoApi $organizer GET ("/api/badminton/sessions/{0}/pairings?round=1&strategy=BALANCED&seed=42" -f $golden.id))
Assert-Demo ($pairings.Count -eq 4) ("Expected 4 pairings, found {0}." -f $pairings.Count)
$firstPairing = $pairings | Select-Object -First 1
Assert-Demo (@($firstPairing.sideA).Count -eq 2 -and @($firstPairing.sideB).Count -eq 2) 'The first pairing is not a doubles pairing.'

$matchBody = @{ courtId = $firstPairing.courtId; roundNumber = 1; sideAUserIds = @($firstPairing.sideA | ForEach-Object { $_.userId }); sideBUserIds = @($firstPairing.sideB | ForEach-Object { $_.userId }) }
$match = Invoke-DemoApi $organizer POST ("/api/badminton/sessions/{0}/matches" -f $golden.id) $matchBody
Invoke-DemoApi $organizer POST ("/api/badminton/matches/{0}/start" -f $match.id) | Out-Null
Invoke-DemoApi $organizer POST ("/api/badminton/matches/{0}/result" -f $match.id) @{ scoreA = 21; scoreB = 17 } | Out-Null
$confirmed = Invoke-DemoApi $organizer POST ("/api/badminton/matches/{0}/confirm" -f $match.id)
Assert-Demo ($confirmed.status -eq 'CONFIRMED' -and $confirmed.scoreA -eq 21 -and $confirmed.scoreB -eq 17 -and $confirmed.winnerSide -eq 'A') 'Match confirmation did not derive the expected result.'

$seasonMatches = @((Invoke-DemoApi $organizer GET ("/api/badminton/groups/{0}/seasons" -f $badminton.id)) | Where-Object { $_.name -eq 'Season 1' })
Assert-Demo ($seasonMatches.Count -eq 1) 'Season 1 was not found or was duplicated.'
$season = $seasonMatches[0]
$leaderboard = @(Invoke-DemoApi $organizer GET ("/api/badminton/groups/{0}/leaderboard?seasonId={1}" -f $badminton.id, $season.id))
$firstStats = Invoke-DemoApi $organizer GET ("/api/badminton/groups/{0}/players/{1}/stats?seasonId={2}" -f $badminton.id, $firstPairing.sideA[0].userId, $season.id)
$history = @(Invoke-DemoApi $organizer GET ("/api/badminton/groups/{0}/players/{1}/ranking-history?seasonId={2}" -f $badminton.id, $firstPairing.sideA[0].userId, $season.id))
$news = @(Invoke-DemoApi $organizer GET ("/api/badminton/groups/{0}/news" -f $badminton.id))
$dashboard = Invoke-DemoApi $organizer GET ("/api/dashboard/groups/{0}?seasonId={1}" -f $badminton.id, $season.id)
Assert-Demo ($firstStats.matches -ge 1 -and $firstStats.wins -ge 1) 'Winner statistics were not updated.'
Assert-Demo ($leaderboard.Count -ge 1 -and $history.Count -ge 1 -and $news.Count -ge 1 -and @($dashboard.recentResults).Count -ge 1) 'Ranking/history/news/dashboard data was not available.'

Write-Output ("GOLDEN_DEMO_PASS groupId={0} sessionId={1} waitlistPromoted={2} registered={3} allocations={4} pairings={5} doubles=2v2 matchId={6} score=21-17 leaderboard={7} history={8} news={9}" -f $badminton.id, $golden.id, $member17User.email, $registered.Count, $allocations.Count, $pairings.Count, $match.id, $leaderboard.Count, $history.Count, $news.Count)
