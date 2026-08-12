import { apiClient } from './client'
export type Tournament = { id: number; groupId: number; seasonId: number; sessionId: number; name: string; format: string; status: string; maxParticipants: number; championId: number | null; participants: number }
export type TournamentParticipant = { userId: number; displayName: string; seedNumber: number | null; registeredAt: string | null }
export type Bracket = { id: number; stage: string; matchNumber: number; nextMatchNumber: number | null; matchId: number; status: string; winnerId: number | null }
export async function getTournaments(groupId: number) { return (await apiClient.get<Tournament[]>(`/tournaments/groups/${groupId}`)).data }
export async function createTournament(groupId: number, input: { name: string; seasonId: number; sessionId: number; format: string; maxParticipants: number }) { return (await apiClient.post<Tournament>(`/tournaments/groups/${groupId}`, input)).data }
export async function openTournament(id: number) { return (await apiClient.post<Tournament>(`/tournaments/${id}/open`)).data }
export async function startTournament(id: number) { return (await apiClient.post<Tournament>(`/tournaments/${id}/start`)).data }
export async function completeTournament(id: number, championId: number) { return (await apiClient.post<Tournament>(`/tournaments/${id}/complete?championId=${championId}`)).data }
export async function registerTournamentParticipant(id: number, userId: number) { return (await apiClient.post<TournamentParticipant>(`/tournaments/${id}/participants`, { userId })).data }
export async function getTournamentParticipants(id: number) { return (await apiClient.get<TournamentParticipant[]>(`/tournaments/${id}/participants`)).data }
export async function getTournamentBracket(id: number) { return (await apiClient.get<Bracket[]>(`/tournaments/${id}/bracket`)).data }
