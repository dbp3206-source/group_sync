import { apiClient } from './client'

export type GroupType = 'STUDY' | 'BADMINTON'
export type GroupRole = 'OWNER' | 'ORGANIZER' | 'MEMBER'

export type GroupSummary = {
  id: number
  name: string
  description: string | null
  type: GroupType
  role: GroupRole
}

export type GroupMember = {
  userId: number
  displayName: string
  role: GroupRole
}

export type GroupDetail = {
  id: number
  name: string
  description: string | null
  type: GroupType
  members: GroupMember[]
}

export type Invitation = {
  id: number
  groupId: number
  groupName: string
  inviteeEmail: string
  inviterDisplayName: string
  status: string
}

export async function getGroups() {
  const response = await apiClient.get<GroupSummary[]>('/groups')
  return response.data
}

export async function createGroup(input: { name: string; description: string; type: GroupType }) {
  const response = await apiClient.post<GroupDetail>('/groups', input)
  return response.data
}

export async function getGroup(groupId: number) {
  const response = await apiClient.get<GroupDetail>(`/groups/${groupId}`)
  return response.data
}

export async function getPendingInvitations() {
  const response = await apiClient.get<Invitation[]>('/groups/invitations/pending')
  return response.data
}

export async function acceptInvitation(invitationId: number) {
  const response = await apiClient.post<Invitation>(`/groups/invitations/${invitationId}/accept`)
  return response.data
}

export async function inviteUser(groupId: number, email: string) {
  const response = await apiClient.post<Invitation>(`/groups/${groupId}/invitations`, { email })
  return response.data
}

export async function changeMemberRole(groupId: number, userId: number, role: GroupRole) {
  const response = await apiClient.patch<GroupDetail>(`/groups/${groupId}/members/${userId}/role`, { role })
  return response.data
}
