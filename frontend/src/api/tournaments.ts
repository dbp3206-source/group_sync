import { apiClient } from './client'

export type Tournament = { id: number; groupId: number; seasonId: number; sessionId: number; name: string; competitionMode: 'SINGLES' | 'DOUBLES'; status: string; maxEntries: number; championEntryId: number | null; entries: number }
export type TournamentEntryMember = { userId: number; displayName: string }
export type TournamentEntry = { id: number; displayName: string; seedNumber: number | null; createdAt: string; members: TournamentEntryMember[] }
export type Bracket = { id: number; stage: string; matchNumber: number; nextMatchNumber: number | null; entryA: TournamentEntry | null; entryB: TournamentEntry | null; winnerEntry: TournamentEntry | null; status: string }

export async function getTournaments(groupId: number) { return (await apiClient.get<Tournament[]>(`/tournaments/groups/${groupId}`)).data }
export async function createTournament(groupId: number, input: { name: string; seasonId: number; sessionId: number; competitionMode: 'SINGLES' | 'DOUBLES'; maxEntries: number }) { return (await apiClient.post<Tournament>(`/tournaments/groups/${groupId}`, input)).data }
export async function openTournament(id: number) { return (await apiClient.post<Tournament>(`/tournaments/${id}/open`)).data }
export async function startTournament(id: number) { return (await apiClient.post<Tournament>(`/tournaments/${id}/start`)).data }
export async function addTournamentEntry(id: number, input: { displayName?: string; memberIds: number[]; seedNumber?: number }) { return (await apiClient.post<TournamentEntry>(`/tournaments/${id}/entries`, input)).data }
export async function getTournamentEntries(id: number) { return (await apiClient.get<TournamentEntry[]>(`/tournaments/${id}/entries`)).data }
export async function getTournamentBracket(id: number) { return (await apiClient.get<Bracket[]>(`/tournaments/${id}/bracket`)).data }
export async function recordTournamentWinner(tournamentId: number, tournamentMatchId: number, winnerEntryId: number) { return (await apiClient.post<Bracket>(`/tournaments/${tournamentId}/matches/${tournamentMatchId}/winner`, { winnerEntryId })).data }
