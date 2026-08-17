import { apiClient } from './client'

export type Resource = { id: number; title: string; description: string | null; resourceType: string; processingStatus: string; favorite: boolean; priority: number; createdAt: string }
export type FocusNext = { resourceId: number; title: string; resourceType: string; priority: number; favorite: boolean; progressPercent: number; reason: string }
export type InsightOverview = { totalResources: number; readyResources: number; inProgressResources: number; completedResources: number; composition: { resourceType: string; count: number }[] }
export type Citation = { chunkId: number; resourceId: number; resourceTitle: string; pageNumber: number | null; section: string | null; citationOrder: number; relevanceScore: number; evidenceExcerpt: string }
export type AskResponse = { sessionId: number; answer: string; grounded: boolean; citations: Citation[] }
export type AskInput = { sessionId?: number; question: string; scope: 'THIS_RESOURCE' | 'SELECTED_RESOURCES' | 'COLLECTION' | 'LIBRARY'; resourceId?: number; resourceIds?: number[]; collectionId?: number; sessionTitle?: string }
export type KnowledgeCollection = { id: number; name: string; description: string | null }
export type KnowledgeTag = { id: number; name: string; created_at?: string }
export type ResourceNote = { id: number; content: string; created_at: string; updated_at: string }
export type ResourceActivity = { processing_status: string; progress_percent: number; note_count: number; created_at: string; updated_at: string; last_opened_at: string | null }
export type ChatSession = { id: number; title: string; scope: string; collectionId: number; updatedAt: string }
export type ChatDetail = ChatSession & { resourceIds: number[]; messages: { id:number; role:'USER'|'ASSISTANT'; content:string; citations:Citation[] }[] }
export type OrganizationTagSuggestion = { name: string; existingTagId: number; reason: string; confidence: number }
export type OrganizationCollectionSuggestion = { name: string; existingCollectionId: number; reason: string; confidence: number }
export type OrganizationRelatedSuggestion = { resourceId: number; title: string; reason: string; similarity: number }
export type OrganizationSuggestions = { resourceId: number; suggestedTags: OrganizationTagSuggestion[]; suggestedCollections: OrganizationCollectionSuggestion[]; suggestedRelatedResources: OrganizationRelatedSuggestion[] }

export async function getResources(q?: string, tagId?: number, collectionId?: number) { return (await apiClient.get<Resource[]>('/resources', { params: { ...(q ? { q } : {}), ...(tagId ? { tagId } : {}), ...(collectionId ? { collectionId } : {}) } })).data }
export async function getResource(id: number) { return (await apiClient.get<Resource>(`/resources/${id}`)).data }
export async function getResourceContent(id: number) { return (await apiClient.get<string>(`/resources/${id}/content`, { responseType: 'text' })).data }
export async function getFocusNext() { return (await apiClient.get<FocusNext | null>('/focus/next')).data }
export async function getInsights() { return (await apiClient.get<InsightOverview>('/insights/overview')).data }
export async function createNote(title: string, content: string) { return (await apiClient.post<Resource>('/resources/notes', { title, content })).data }
export async function uploadResource(file: File, title?: string) { const body = new FormData(); body.append('file', file); if (title) body.append('title', title); return (await apiClient.post<Resource>('/resources', body)).data }
export async function askKnowledge(input: AskInput) { return (await apiClient.post<AskResponse>('/ask', input)).data }
export async function getCollections() { return (await apiClient.get<KnowledgeCollection[]>('/collections')).data }
export async function createCollection(name:string, description='') { return (await apiClient.post<KnowledgeCollection>('/collections', { name, description })).data }
export async function getTags() { return (await apiClient.get<KnowledgeTag[]>('/tags')).data }
export async function createTag(name:string) { return (await apiClient.post<KnowledgeTag>('/tags', { name })).data }
export async function getResourceTags(id:number) { return (await apiClient.get<KnowledgeTag[]>(`/resources/${id}/tags`)).data }
export async function assignTagToResource(resourceId:number, tagId:number) { await apiClient.put(`/resources/${resourceId}/tags/${tagId}`) }
export async function removeTagFromResource(resourceId:number, tagId:number) { await apiClient.delete(`/resources/${resourceId}/tags/${tagId}`) }
export async function assignResourceToCollection(collectionId:number, resourceId:number) { await apiClient.put(`/collections/${collectionId}/resources/${resourceId}`) }
export async function getCollectionResources(collectionId:number) { return (await apiClient.get<Resource[]>(`/collections/${collectionId}/resources`)).data }
export async function getChatSessions() { return (await apiClient.get<ChatSession[]>('/ask/sessions')).data }
export async function getChatSession(id:number) { return (await apiClient.get<ChatDetail>(`/ask/sessions/${id}`)).data }
export async function getResourceNotes(id:number) { return (await apiClient.get<ResourceNote[]>(`/resources/${id}/notes`)).data }
export async function createResourceNote(id:number, content:string) { return (await apiClient.post<ResourceNote>(`/resources/${id}/notes`, { content })).data }
export async function updateResourceNote(id:number,noteId:number,content:string) { return (await apiClient.patch<ResourceNote>(`/resources/${id}/notes/${noteId}`, { content })).data }
export async function deleteResourceNote(id:number,noteId:number) { await apiClient.delete(`/resources/${id}/notes/${noteId}`) }
export async function getRelatedResources(id:number) { return (await apiClient.get<Resource[]>(`/resources/${id}/related`)).data }
export async function getResourceActivity(id:number) { return (await apiClient.get<ResourceActivity>(`/resources/${id}/activity`)).data }
export async function updateResourceProgress(id:number, progressPercent:number) { return (await apiClient.put<ResourceActivity>(`/resources/${id}/progress`, { progressPercent })).data }
export async function getOrganizationSuggestions(id:number) { return (await apiClient.get<OrganizationSuggestions>(`/resources/${id}/organization/suggestions`)).data }
export async function applyOrganization(id:number, payload:{tagNames:string[]; collectionIds:number[]; newCollectionNames:string[]; relatedResourceIds:number[]}) { await apiClient.post(`/resources/${id}/organization/apply`, payload) }
export async function deleteResource(id:number) { await apiClient.delete(`/resources/${id}`) }
export async function updateResourceFavorite(id:number, favorite:boolean) { return (await apiClient.patch<Resource>(`/resources/${id}`, { favorite })).data }
