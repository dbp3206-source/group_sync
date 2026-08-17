import { apiClient } from './client'

export type Notification = {
  id: number
  type: string
  title: string
  message: string
  targetType: string | null
  targetId: number | null
  read: boolean
  createdAt: string
}

export async function getNotifications() {
  return (await apiClient.get<Notification[]>('/notifications')).data
}

export async function markNotificationRead(id: number) {
  await apiClient.patch(`/notifications/${id}/read`)
}
