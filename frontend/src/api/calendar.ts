import { apiClient } from './client'

export type CalendarItem = { sourceType: 'MANUAL' | 'RECURRING' | 'STUDY'; sourceId: number; title: string; start: string; end: string; busy: boolean }
export type BusyEvent = { id: number; title: string; start: string; end: string }
export type WeeklySchedule = { id: number; title: string; weekdays: string[]; startTime: string; endTime: string; validFrom: string; validUntil: string; timezone: string }

export async function getCalendarItems(from: string, to: string) { return (await apiClient.get<CalendarItem[]>('/calendar/items', { params: { from, to } })).data }
export async function createBusyEvent(input: { title: string; start: string; end: string }) { return (await apiClient.post<BusyEvent>('/calendar/events', input)).data }
export async function deleteBusyEvent(id: number) { await apiClient.delete(`/calendar/events/${id}`) }
export async function getRecurringSchedules() { return (await apiClient.get<WeeklySchedule[]>('/calendar/recurring')).data }
export async function createRecurringSchedule(input: Omit<WeeklySchedule, 'id'>) { return (await apiClient.post<WeeklySchedule>('/calendar/recurring', input)).data }
