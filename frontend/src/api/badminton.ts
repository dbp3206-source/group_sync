import { apiClient } from './client'

export type BadmintonSeason = { id: number; name: string; startsOn: string; endsOn: string | null; active: boolean }
export type Court = { id: number; name: string; active: boolean }
export type Venue = { id: number; name: string; address: string | null; courts: Court[] }
export type Registration = { id: number; userId: number; displayName: string; status: string; queuedAt: string | null; conflictWarning: boolean }
export type Responsibility = { id: number; itemName: string; status: string; assigneeId: number | null; assigneeName: string | null; note: string | null }
export type BadmintonSession = { id: number; groupId: number; title: string; start: string; end: string; registrationDeadline: string; capacity: number; status: string; venueId: number; venueName: string; seasonId: number; seasonName: string; courts: Court[]; registrations: Registration[]; responsibilities: Responsibility[] }
export type Profile = { membershipId: number; userId: number; displayName: string; skillLevel: string; bio: string | null }
export type Allocation = { id: number; courtId: number; courtName: string; roundNumber: number; status: string; players: { userId: number; displayName: string; position: number }[] }
export type Pairing = { courtId: number; courtName: string; roundNumber: number; strategy: string; sideA: { userId: number; displayName: string }[]; sideB: { userId: number; displayName: string }[]; unassigned: { userId: number; displayName: string }[] }
export type Match = { id: number; sessionId: number; courtId: number; courtName: string; roundNumber: number; status: string; scoreA: number | null; scoreB: number | null; winnerSide: string | null; sides: { code: string; participants: { userId: number; displayName: string }[] }[] }
export type Stat = { userId: number; displayName: string; matches: number; wins: number; losses: number; points: number; attended: number; noShows: number; winRate: number; recentForm: string }
export type News = { id: number; type: string; title: string; content: string; targetId: number | null; createdAt: string }
export type RankingHistory = { id: number; matchId: number; userId: number; pointsAfter: number; winsAfter: number; matchesAfter: number; createdAt: string }

export async function getBadmintonSeasons(groupId: number) { return (await apiClient.get<BadmintonSeason[]>(`/badminton/groups/${groupId}/seasons`)).data }
export async function getBadmintonVenues(groupId: number) { return (await apiClient.get<Venue[]>(`/badminton/groups/${groupId}/venues`)).data }
export async function createBadmintonVenue(groupId: number, input: { name: string; address: string }) { return (await apiClient.post<Venue>(`/badminton/groups/${groupId}/venues`, input)).data }
export async function createBadmintonCourt(groupId: number, venueId: number, name: string) { return (await apiClient.post<Court>(`/badminton/groups/${groupId}/venues/${venueId}/courts`, { name })).data }
export async function getBadmintonSessions(groupId: number) { return (await apiClient.get<BadmintonSession[]>(`/badminton/groups/${groupId}/sessions`)).data }
export async function getBadmintonSession(id: number) { return (await apiClient.get<BadmintonSession>(`/badminton/sessions/${id}`)).data }
export async function createBadmintonSession(groupId: number, input: { title: string; start: string; end: string; registrationDeadline: string; capacity: number; seasonId: number; venueId: number; courtIds: number[] }) { return (await apiClient.post<BadmintonSession>(`/badminton/groups/${groupId}/sessions`, input)).data }
export async function openBadmintonSession(id: number) { return (await apiClient.post<BadmintonSession>(`/badminton/sessions/${id}/open`)).data }
export async function confirmBadmintonSession(id: number) { return (await apiClient.post<BadmintonSession>(`/badminton/sessions/${id}/confirm`)).data }
export async function cancelBadmintonSession(id: number) { return (await apiClient.post<BadmintonSession>(`/badminton/sessions/${id}/cancel`)).data }
export async function joinBadmintonSession(id: number) { return (await apiClient.post<BadmintonSession>(`/badminton/sessions/${id}/registrations`)).data }
export async function leaveBadmintonSession(id: number) { return (await apiClient.delete<BadmintonSession>(`/badminton/sessions/${id}/registrations/me`)).data }
export async function checkInBadminton(sessionId: number, userId: number) { return (await apiClient.post<BadmintonSession>(`/badminton/sessions/${sessionId}/registrations/${userId}/check-in`)).data }
export async function noShowBadminton(sessionId: number, userId: number) { return (await apiClient.post<BadmintonSession>(`/badminton/sessions/${sessionId}/registrations/${userId}/no-show`)).data }
export async function startBadmintonSession(id: number) { return (await apiClient.post<BadmintonSession>(`/badminton/sessions/${id}/start`)).data }
export async function completeBadmintonSession(id: number) { return (await apiClient.post<BadmintonSession>(`/badminton/sessions/${id}/complete`)).data }
export async function generateCheckinToken(id: number) { return (await apiClient.post<{ sessionId: number; sessionTitle: string; token: string; checkInUrl: string; expiresAt: string }>(`/badminton/sessions/${id}/checkin-token`)).data }
export async function checkInWithToken(token: string) { return (await apiClient.post<{ sessionId: number; sessionTitle: string; status: string; alreadyCheckedIn: boolean }>('/badminton/check-in', { token })).data }
export async function generateAllocation(id: number, round = 1) { return (await apiClient.post<Allocation[]>(`/badminton/sessions/${id}/allocations/generate?round=${round}`)).data }
export async function getPairings(id: number, strategy = 'BALANCED', round = 1) { return (await apiClient.get<Pairing[]>(`/badminton/sessions/${id}/pairings?strategy=${strategy}&round=${round}&seed=42`)).data }
export async function createMatch(sessionId: number, input: { courtId: number; roundNumber: number; sideAUserIds: number[]; sideBUserIds: number[] }) { return (await apiClient.post<Match>(`/badminton/sessions/${sessionId}/matches`, input)).data }
export async function getMatches(groupId: number) { return (await apiClient.get<Match[]>(`/badminton/groups/${groupId}/matches`)).data }
export async function startMatch(id: number) { return (await apiClient.post<Match>(`/badminton/matches/${id}/start`)).data }
export async function submitMatchScore(id: number, scoreA: number, scoreB: number) { return (await apiClient.post<Match>(`/badminton/matches/${id}/result`, { scoreA, scoreB })).data }
export async function confirmMatch(id: number) { return (await apiClient.post<Match>(`/badminton/matches/${id}/confirm`)).data }
export async function getLeaderboard(groupId: number, seasonId: number) { return (await apiClient.get<Stat[]>(`/badminton/groups/${groupId}/leaderboard?seasonId=${seasonId}`)).data }
export async function getPlayerStats(groupId: number, userId: number, seasonId: number) { return (await apiClient.get<Stat>(`/badminton/groups/${groupId}/players/${userId}/stats?seasonId=${seasonId}`)).data }
export async function getRankingHistory(groupId: number, userId: number, seasonId: number) { return (await apiClient.get<RankingHistory[]>(`/badminton/groups/${groupId}/players/${userId}/ranking-history?seasonId=${seasonId}`)).data }
export async function getNews(groupId: number) { return (await apiClient.get<News[]>(`/badminton/groups/${groupId}/news`)).data }
