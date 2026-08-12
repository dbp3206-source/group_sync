import { apiClient } from './client'

export type BadmintonSeason = { id: number; name: string; startsOn: string; endsOn: string | null; active: boolean }
export type Court = { id: number; name: string; active: boolean }
export type Venue = { id: number; name: string; address: string | null; courts: Court[] }
export type Registration = { id: number; userId: number; displayName: string; status: string; queuedAt: string | null; conflictWarning: boolean }
export type Responsibility = { id: number; itemName: string; status: string; assigneeId: number | null; assigneeName: string | null; note: string | null }
export type BadmintonSession = { id: number; groupId: number; title: string; start: string; end: string; registrationDeadline: string; capacity: number; status: string; venueId: number; venueName: string; seasonId: number; seasonName: string; courts: Court[]; registrations: Registration[]; responsibilities: Responsibility[] }
export type Profile = { membershipId: number; userId: number; displayName: string; skillLevel: string; bio: string | null }

export async function getBadmintonSeasons(groupId: number) { return (await apiClient.get<BadmintonSeason[]>(`/badminton/groups/${groupId}/seasons`)).data }
export async function getBadmintonVenues(groupId: number) { return (await apiClient.get<Venue[]>(`/badminton/groups/${groupId}/venues`)).data }
export async function createBadmintonVenue(groupId: number, input: { name: string; address: string }) { return (await apiClient.post<Venue>(`/badminton/groups/${groupId}/venues`, input)).data }
export async function createBadmintonCourt(groupId: number, venueId: number, name: string) { return (await apiClient.post<Court>(`/badminton/groups/${groupId}/venues/${venueId}/courts`, { name })).data }
export async function getBadmintonSessions(groupId: number) { return (await apiClient.get<BadmintonSession[]>(`/badminton/groups/${groupId}/sessions`)).data }
export async function createBadmintonSession(groupId: number, input: { title: string; start: string; end: string; registrationDeadline: string; capacity: number; seasonId: number; venueId: number; courtIds: number[] }) { return (await apiClient.post<BadmintonSession>(`/badminton/groups/${groupId}/sessions`, input)).data }
export async function openBadmintonSession(id: number) { return (await apiClient.post<BadmintonSession>(`/badminton/sessions/${id}/open`)).data }
export async function confirmBadmintonSession(id: number) { return (await apiClient.post<BadmintonSession>(`/badminton/sessions/${id}/confirm`)).data }
export async function cancelBadmintonSession(id: number) { return (await apiClient.post<BadmintonSession>(`/badminton/sessions/${id}/cancel`)).data }
export async function joinBadmintonSession(id: number) { return (await apiClient.post<BadmintonSession>(`/badminton/sessions/${id}/registrations`)).data }
export async function leaveBadmintonSession(id: number) { return (await apiClient.delete<BadmintonSession>(`/badminton/sessions/${id}/registrations/me`)).data }
