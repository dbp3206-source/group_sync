import { apiClient } from './client'

export type CalendarItem = { sourceType: 'MANUAL' | 'RECURRING' | 'STUDY' | 'BADMINTON'; sourceId: number; title: string; start: string; end: string; busy: boolean }
export type BusyEvent = { id: number; title: string; start: string; end: string; description?: string; category?: string; location?: string; visibility?: string; reminderMinutes?: number }
export type WeeklySchedule = { id: number; title: string; weekdays: string[]; startTime: string; endTime: string; validFrom: string; validUntil: string; timezone: string; description?: string; category?: string; location?: string; visibility?: string; reminderMinutes?: number; frequency?: string }

export async function getCalendarItems(from: string, to: string) { return (await apiClient.get<CalendarItem[]>('/calendar/items', { params: { from, to } })).data }
export async function getBusyEvents() { return (await apiClient.get<BusyEvent[]>('/calendar/events')).data }
export async function createBusyEvent(input: Omit<BusyEvent, 'id'>) { return (await apiClient.post<BusyEvent>('/calendar/events', input)).data }
export async function updateBusyEvent(id: number, input: Omit<BusyEvent, 'id'>) { return (await apiClient.patch<BusyEvent>(`/calendar/events/${id}`, input)).data }
export async function deleteBusyEvent(id: number) { await apiClient.delete(`/calendar/events/${id}`) }
export async function duplicateBusyEvent(id: number) { return (await apiClient.post<BusyEvent>(`/calendar/events/${id}/duplicate`)).data }
export async function getRecurringSchedules() { return (await apiClient.get<WeeklySchedule[]>('/calendar/recurring')).data }
export async function createRecurringSchedule(input: Omit<WeeklySchedule, 'id'>) { return (await apiClient.post<WeeklySchedule>('/calendar/recurring', input)).data }
export async function updateRecurringSchedule(id: number, input: Omit<WeeklySchedule, 'id'>) { return (await apiClient.patch<WeeklySchedule>(`/calendar/recurring/${id}`, input)).data }
export async function deleteRecurringSchedule(id: number) { await apiClient.delete(`/calendar/recurring/${id}`) }
export async function duplicateRecurringSchedule(id: number) { return (await apiClient.post<WeeklySchedule>(`/calendar/recurring/${id}/duplicate`)).data }
