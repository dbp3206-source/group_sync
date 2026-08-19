import { apiClient } from './client'

export type PagedResponse<T> = {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
  hasNext: boolean
}

export type Resource = { id: number; title: string; description: string | null; resourceType: string; processingStatus: string; favorite: boolean; priority: number; originalFilename?: string | null; sizeBytes?: number | null; createdAt: string }
export type FocusNext = { resourceId: number; title: string; resourceType: string; priority: number; favorite: boolean; progressPercent: number; reason: string }
export type InsightOverview = { totalResources: number; readyResources: number; inProgressResources: number; completedResources: number; composition: { resourceType: string; count: number }[] }
export type PlannerTrace = {
  mode: string
  operation: string
  semanticQuery: string
  explanation: string
}

export type FilterTrace = {
  scope: string
  resourceType: string | null
  favorite: boolean | null
  collectionCount: number | null
  tagCount: number | null
  eligibleResourceCount: number | null
  createdAfter: string | null
  createdBefore: string | null
}

export type RetrievalTrace = {
  semanticCandidates: number
  lexicalCandidates: number
  totalCandidates: number
}

export type FusionTrace = {
  inputCandidates: number
  selectedChildren: number
  rrfK: number
}

export type ParentChildTrace = {
  childChunksRetrieved: number
  uniqueParentsFound: number
  duplicateParentsDeduplicated: number
}

export type ContextBudgetTrace = {
  parentsUsed: number
  charactersUsed: number
  maxCharactersBudget: number
}

export type GenerationTrace = {
  model: string
  promptChunksCount: number
  verifiedCitationsCount: number
}

export type RagExecutionTrace = {
  mode: string
  operation: string
  planner?: PlannerTrace
  filter?: FilterTrace
  retrieval?: RetrievalTrace
  fusion?: FusionTrace
  parentChild?: ParentChildTrace
  contextBudget?: ContextBudgetTrace
  generation?: GenerationTrace
  durationMs: number
}

export type ResourceIngestionTrace = {
  resourceId: number
  resourceTitle: string
  resourceType: string
  processingStatus: string
  chunkingVersion: number
  parentChunkCount: number
  childChunkCount: number
  embeddingBatchCount: number
  embeddingModel: string
  embeddingDimensions: number
  semanticMetadataIncluded: boolean
}

export type Citation = { chunkId: number; resourceId: number; resourceTitle: string; pageNumber: number | null; section: string | null; citationOrder: number; relevanceScore: number; evidenceExcerpt: string }
export type AskResponse = { sessionId: number; answer: string; grounded: boolean; citations: Citation[]; trace?: RagExecutionTrace }
export type AskInput = { sessionId?: number; question: string; scope: 'THIS_RESOURCE' | 'SELECTED_RESOURCES' | 'COLLECTION' | 'LIBRARY'; resourceId?: number; resourceIds?: number[]; collectionId?: number; sessionTitle?: string }
export type KnowledgeCollection = { id: number; name: string; description: string | null; createdAt?: string; updatedAt?: string }
export type KnowledgeTag = { id: number; name: string; createdAt?: string }
export type ResourceNote = { id: number; content: string; createdAt: string; updatedAt: string }
export type ResourceActivity = { processingStatus: string; progressPercent: number; noteCount: number; createdAt: string; updatedAt: string; lastOpenedAt: string | null }
export type RelatedResource = { id: number; title: string; description: string | null; resourceType: string; processingStatus: string; relationType?: string; createdAt?: string }
export type ChatSession = { id: number; title: string; scope: string; collectionId: number; updatedAt: string }
export type ChatDetail = ChatSession & { resourceIds: number[]; messages: { id:number; role:'USER'|'ASSISTANT'; content:string; citations:Citation[] }[] }
export type OrganizationTagSuggestion = { name: string; existingTagId: number; reason: string; confidence: number }
export type OrganizationCollectionSuggestion = { name: string; existingCollectionId: number; reason: string; confidence: number }
export type OrganizationRelatedSuggestion = { resourceId: number; title: string; reason: string; similarity: number }
export type OrganizationSuggestions = { resourceId: number; suggestedTags: OrganizationTagSuggestion[]; suggestedCollections: OrganizationCollectionSuggestion[]; suggestedRelatedResources: OrganizationRelatedSuggestion[] }

export async function getResources(q?: string, tagId?: number, collectionId?: number, page = 0, size = 24, sort = 'updated_desc') {
  return (await apiClient.get<PagedResponse<Resource>>('/resources', { params: { ...(q ? { q } : {}), ...(tagId ? { tagId } : {}), ...(collectionId ? { collectionId } : {}), page, size, sort } })).data
}
export async function getResource(id: number) { return (await apiClient.get<Resource>(`/resources/${id}`)).data }
export async function getResourceContent(id: number) { return (await apiClient.get<string>(`/resources/${id}/text`, { responseType: 'text' })).data }
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
export async function getRelatedResources(id:number) { return (await apiClient.get<RelatedResource[]>(`/resources/${id}/related`)).data }
export async function getResourceActivity(id:number) { return (await apiClient.get<ResourceActivity>(`/resources/${id}/activity`)).data }
export async function updateResourceProgress(id:number, progressPercent:number) { return (await apiClient.put<ResourceActivity>(`/resources/${id}/progress`, { progressPercent })).data }
export async function getOrganizationSuggestions(id:number) { return (await apiClient.get<OrganizationSuggestions>(`/resources/${id}/organization/suggestions`)).data }
export async function applyOrganization(id:number, payload:{tagNames:string[]; collectionIds:number[]; newCollectionNames:string[]; relatedResourceIds:number[]}) { await apiClient.post(`/resources/${id}/organization/apply`, payload) }
export async function deleteResource(id:number) { await apiClient.delete(`/resources/${id}`) }
export async function updateResourceFavorite(id:number, favorite:boolean) { return (await apiClient.patch<Resource>(`/resources/${id}`, { favorite })).data }
export async function autoOrganizeAll() { return (await apiClient.post<{ message: string }>('/resources/auto-organize-all')).data }
export async function autoOrganizeResource(id: number) { return (await apiClient.post<{ message: string }>(`/resources/${id}/auto-organize`)).data }
export async function getResourceIngestionTrace(id: number) { return (await apiClient.get<ResourceIngestionTrace>(`/resources/${id}/rag-trace`)).data }

// ==========================================
// Focus Topic Deepdive Learning Studio Types & API
// ==========================================

export type ConceptSource = {
  resourceId: number
  resourceTitle: string
  chunkId: number
  snippet: string
}

export type TopicConcept = {
  id: number
  title: string
  summary: string
  whyItMatters: string
  studyStatus: 'NOT_STARTED' | 'LEARNING' | 'REVIEW_NEEDED' | 'CHECKED'
  position: number
  sources: ConceptSource[]
}

export type TopicResource = {
  id: number
  title: string
  resourceType: string
  processingStatus: string
  progressPercent: number
}

export type StudyTopic = {
  id: number
  title: string
  goal: string
  status: string
  resourceCount: number
  conceptCount: number
  checkedCount: number
  reviewNeededCount: number
  learningCount: number
  notStartedCount: number
  createdAt: string
  updatedAt: string
}

export type StudyTopicDetail = {
  id: number
  title: string
  goal: string
  status: string
  resources: TopicResource[]
  concepts: TopicConcept[]
  checkedCount: number
  reviewNeededCount: number
  learningCount: number
  notStartedCount: number
  createdAt: string
  updatedAt: string
}

export type QuizQuestion = {
  id: number
  conceptId: number | null
  question: string
  options: string[]
  correctOption: number | null
  userAnswer: number | null
  explanation: string | null
  sourceResourceId: number | null
  sourceResourceTitle: string | null
  sourceChunkId: number | null
  sourceSnippet: string | null
}

export type QuizAttemptResponse = {
  attemptId: number
  topicId: number
  conceptId: number | null
  scoreCorrect: number
  totalQuestions: number
  submitted: boolean
  questions: QuizQuestion[]
  createdAt: string
}

export type SubmitQuizAnswersResponse = {
  attemptId: number
  scoreCorrect: number
  totalQuestions: number
  percentage: number
  results: QuizQuestion[]
  conceptsNeedingReview: TopicConcept[]
}

export type ReviewQueueItem = {
  conceptId: number
  conceptTitle: string
  topicId: number
  topicTitle: string
  studyStatus: string
  summary: string
  whyItMatters: string
  updatedAt: string
}

export async function getStudyTopics() {
  return (await apiClient.get<StudyTopic[]>('/focus/topics')).data
}

export async function createStudyTopic(title: string, goal: string, resourceIds: number[] = []) {
  return (await apiClient.post<StudyTopicDetail>('/focus/topics', { title, goal, resourceIds })).data
}

export async function getStudyTopicDetail(id: number) {
  return (await apiClient.get<StudyTopicDetail>(`/focus/topics/${id}`)).data
}

export async function deleteStudyTopic(id: number) {
  await apiClient.delete(`/focus/topics/${id}`)
}

export async function addTopicSource(topicId: number, resourceId: number) {
  return (await apiClient.post<StudyTopicDetail>(`/focus/topics/${topicId}/sources/${resourceId}`)).data
}

export async function removeTopicSource(topicId: number, resourceId: number) {
  return (await apiClient.delete<StudyTopicDetail>(`/focus/topics/${topicId}/sources/${resourceId}`)).data
}

export async function generateTopicPlan(topicId: number) {
  return (await apiClient.post<StudyTopicDetail>(`/focus/topics/${topicId}/plan`)).data
}

export async function updateConceptStatus(topicId: number, conceptId: number, status: string) {
  return (await apiClient.patch<TopicConcept>(`/focus/topics/${topicId}/concepts/${conceptId}/status`, { status })).data
}

export async function generateTopicQuiz(topicId: number, conceptId?: number) {
  return (await apiClient.post<QuizAttemptResponse>(`/focus/topics/${topicId}/quiz`, null, {
    params: conceptId ? { conceptId } : {},
  })).data
}

export async function submitQuizAnswers(attemptId: number, answers: Record<number, number>) {
  return (await apiClient.post<SubmitQuizAnswersResponse>(`/focus/quiz/attempts/${attemptId}/answers`, { answers })).data
}

export async function getReviewQueue() {
  return (await apiClient.get<ReviewQueueItem[]>('/focus/review-queue')).data
}

