import { apiClient } from './client'

export type Resource = { id: number; title: string; description: string | null; resourceType: string; processingStatus: string; favorite: boolean; priority: number; createdAt: string }
export type FocusNext = { resourceId: number; title: string; resourceType: string; priority: number; favorite: boolean; progressPercent: number; reason: string }
export type InsightOverview = { totalResources: number; readyResources: number; inProgressResources: number; completedResources: number; composition: { resourceType: string; count: number }[] }
export type Citation = { chunkId: number; resourceId: number; resourceTitle: string; pageNumber: number | null; section: string | null; citationOrder: number; relevanceScore: number; evidenceExcerpt: string }
export type AskResponse = { sessionId: number; answer: string; grounded: boolean; citations: Citation[] }
export type AskInput = { sessionId?: number; question: string; scope: 'THIS_RESOURCE' | 'SELECTED_RESOURCES' | 'COLLECTION' | 'LIBRARY'; resourceId?: number; resourceIds?: number[]; collectionId?: number; sessionTitle?: string }

export async function getResources(q?: string) { return (await apiClient.get<Resource[]>('/resources', { params: q ? { q } : undefined })).data }
export async function getResource(id: number) { return (await apiClient.get<Resource>(`/resources/${id}`)).data }
export async function getResourceContent(id: number) { return (await apiClient.get<string>(`/resources/${id}/content`, { responseType: 'text' })).data }
export async function getFocusNext() { return (await apiClient.get<FocusNext | null>('/focus/next')).data }
export async function getInsights() { return (await apiClient.get<InsightOverview>('/insights/overview')).data }
export async function createNote(title: string, content: string) { return (await apiClient.post<Resource>('/resources/notes', { title, content })).data }
export async function askKnowledge(input: AskInput) { return (await apiClient.post<AskResponse>('/ask', input)).data }
