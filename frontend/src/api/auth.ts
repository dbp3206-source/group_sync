import { apiClient } from './client'

export type User = {
  id: number
  email: string
  displayName: string
  systemRole: string
}

export type RegisterInput = {
  email: string
  password: string
  displayName: string
}

export async function getCsrfToken() {
  const response = await apiClient.get<{ token: string }>('/auth/csrf')
  return response.data.token
}

export async function register(input: RegisterInput) {
  const response = await apiClient.post<User>('/auth/register', input)
  return response.data
}

export async function login(email: string, password: string) {
  const response = await apiClient.post<User>('/auth/login', { email, password })
  return response.data
}

export async function getCurrentUser() {
  const response = await apiClient.get<User>('/auth/me')
  return response.data
}

export async function logout() {
  await apiClient.post('/auth/logout')
}
