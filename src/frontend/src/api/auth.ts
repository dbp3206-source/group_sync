import { apiClient } from './client'

export type User = {
  id: number
  email: string
  displayName: string
  systemRole: string
  timeZone: string
  profileCompleted: boolean
  avatarUrl: string | null
}

export type RegisterInput = {
  email: string
  password: string
  displayName: string
}

function parseUser(value: unknown): User {
  if (!value || typeof value !== 'object' || typeof (value as Partial<User>).id !== 'number' || typeof (value as Partial<User>).displayName !== 'string') {
    throw new Error('KnowledgeOS server is not available yet.')
  }
  return value as User
}

export async function getCsrfToken() {
  const response = await apiClient.get<{ token: string }>('/auth/csrf')
  if (!response.data || typeof response.data.token !== 'string' || !response.data.token) {
    throw new Error('KnowledgeOS server is not available yet.')
  }
  return response.data.token
}

export async function register(input: RegisterInput) {
  const response = await apiClient.post<User>('/auth/register', input)
  return parseUser(response.data)
}

export async function login(email: string, password: string) {
  const response = await apiClient.post<User>('/auth/login', { email, password })
  return parseUser(response.data)
}

export async function getCurrentUser() {
  const response = await apiClient.get<User>('/auth/me')
  return parseUser(response.data)
}

export async function logout() {
  await apiClient.post('/auth/logout')
}
