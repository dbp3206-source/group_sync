import { apiClient } from './client'
import type { User } from './auth'

export type ProfileInput = {
  displayName: string
  timeZone: string
}

export async function updateProfile(input: ProfileInput) {
  const response = await apiClient.patch<User>('/users/me/profile', input)
  return response.data
}

export async function getProfile() {
  const response = await apiClient.get<User>('/users/me/profile')
  return response.data
}

export async function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  const response = await apiClient.post<User>('/users/me/avatar', formData)
  return response.data
}

export async function deleteAvatar() {
  await apiClient.delete('/users/me/avatar')
}

export async function changePassword(currentPassword: string, newPassword: string) {
  await apiClient.put('/users/me/password', { currentPassword, newPassword })
}
