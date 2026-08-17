import { apiClient } from './client'

export type AvailabilityCandidate = { start: string; end: string; attendance: number; availableMemberIds: number[] }
export async function searchAvailability(groupId: number, input: { from: string; to: string; durationMinutes: number; requiredMemberIds: number[]; minimumAttendance: number; strategy: string }) { return (await apiClient.post<AvailabilityCandidate[]>(`/availability/groups/${groupId}/search`, input)).data }
