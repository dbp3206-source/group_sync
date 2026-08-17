import { apiClient } from './client'

export type StudyParticipant = { userId: number; displayName: string; email: string; attendance: string }
export type StudySession = { id: number; groupId: number; topic: string; goal: string | null; location: string | null; start: string; end: string; capacity: number | null; status: string; participants: StudyParticipant[]; materials: { id: number; title: string; url: string }[]; goals: { id: number; description: string; completed: boolean }[] }
export async function getStudySessions(groupId: number) { return (await apiClient.get<StudySession[]>('/study/sessions', { params: { groupId } })).data }
export async function createStudySession(groupId: number, input: { topic: string; goal: string; location: string; start: string; end: string; capacity: number | null }) { return (await apiClient.post<StudySession>(`/study/groups/${groupId}/sessions`, input)).data }
export async function joinStudySession(id: number) { return (await apiClient.post<StudySession>(`/study/sessions/${id}/join`)).data }
export async function confirmStudySession(id: number) { return (await apiClient.post<StudySession>(`/study/sessions/${id}/confirm`)).data }
export async function cancelStudySession(id: number) { return (await apiClient.post<StudySession>(`/study/sessions/${id}/cancel`)).data }
export async function rescheduleStudySession(id: number, start: string, end: string) { return (await apiClient.patch<StudySession>(`/study/sessions/${id}/schedule`, { start, end })).data }
