import { apiClient } from './client'

export type PagedResponse<T> = {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
  hasNext: boolean
}

export type Resource = { id: number; title: string; description: string | null; resourceType: string; processingStatus: string; processingError?: string | null; favorite: boolean; priority: number; originalFilename?: string | null; sizeBytes?: number | null; createdAt: string; updatedAt?: string }
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
export type KnowledgeCollection = { id: number; name: string; description: string | null; createdAt?: string; updatedAt?: string; resourceCount?: number }
export type KnowledgeTag = { id: number; name: string; createdAt?: string }
export type ResourceNote = { id: number; content: string; createdAt: string; updatedAt: string }
export type ResourceActivity = { processingStatus: string; progressPercent: number; noteCount: number; createdAt: string; updatedAt: string; lastOpenedAt: string | null }
export type ResourceUnderstanding = {
  status: 'CURRENT' | 'FAILED' | 'UNSUPPORTED' | 'STALE' | 'NOT_AVAILABLE' | string
  normalizedTitle: string | null
  summary: string | null
  keyIdeas: string[]
  broadThemes: string[]
  evidenceCount: number
  updatedAt: string | null
}
export type AskTraceTechnicalDetails = {
  mode: string | null
  operation: string | null
  semanticCandidates: number | null
  lexicalCandidates: number | null
  totalCandidates: number | null
  selectedChildren: number | null
  parentsUsed: number | null
  charactersUsed: number | null
  maxCharactersBudget: number | null
  citationsVerified: number | null
  model: string | null
  failureCategory: string | null
}
export type AskTraceEvent = {
  attemptId: number
  sequence: number
  stage: string
  status: 'RUNNING' | 'COMPLETE' | 'FAILED'
  occurredAt: string
  durationMs: number
  beginnerMessage: string
  technicalSummary: string
  technicalDetails: AskTraceTechnicalDetails
}
export type AskAttempt = { attemptId: number; sessionId: number; userMessageId: number; status: 'PENDING' | 'RUNNING' | 'COMPLETE' | 'FAILED'; failureCategory: string | null; createdAt: string; completedAt: string | null }
export type AskPreflight = { heavy: boolean; estimatedInputTokens: number; estimatedContextCharacters: number; estimateBasis: string; providerQuotaVisible: boolean; providerQuotaState: string; resetAt: string | null }
export type AiUsage = { completedRequests: number; rateLimitCount: number; failedRequests: number; promptTokens: number; outputTokens: number; totalTokens: number; exactProviderQuotaVisible: boolean; providerQuotaState: string; resetAt: string | null; lastRecordedAt: string | null }
export type ResourceDeepDive = {
  available: boolean
  topicId: number | null
  topicTitle: string | null
  goal: string | null
  topicStatus: string | null
  conceptCount: number
  checkedCount: number
  reviewNeededCount: number
  learningCount: number
  notStartedCount: number
  updatedAt: string | null
}
export type ResourceKnowledgeMapNode = {
  id: string
  type: 'RESOURCE' | 'COLLECTION' | 'TAG' | string
  label: string
  resourceId: number | null
  collectionId: number | null
  tagId: number | null
}
export type ResourceKnowledgeMapEdge = {
  source: string
  target: string
  relationType: string
  reason: string
  confidence: number | null
  provenance: string
}
export type ResourceKnowledgeMap = { nodes: ResourceKnowledgeMapNode[]; edges: ResourceKnowledgeMapEdge[] }
export type RecentActivity = { type: 'RESOURCE_OPENED' | 'ASK_ACTIVITY' | 'FOCUS_ACTIVITY' | 'RECALL_ACTIVITY'; title: string; occurredAt: string; resumeUrl: string; context: string }
export type RelatedResource = { id: number; title: string; description: string | null; resourceType: string; processingStatus: string; relationType?: string; createdAt?: string }
export type ChatSession = { id: number; title: string; scope: string; collectionId: number; updatedAt: string }
export type ChatDetail = ChatSession & { resourceIds: number[]; messages: { id:number; role:'USER'|'ASSISTANT'; content:string; citations:Citation[]; status?:'PENDING'|'COMPLETE'|'FAILED'; failureCategory?:string|null }[] }
export type OrganizationTagSuggestion = { name: string; existingTagId: number; reason: string; confidence: number }
export type OrganizationCollectionSuggestion = { name: string; existingCollectionId: number; reason: string; confidence: number }
export type OrganizationRelatedSuggestion = { resourceId: number; title: string; reason: string; similarity: number }
export type OrganizationSuggestions = { resourceId: number; suggestedTags: OrganizationTagSuggestion[]; suggestedCollections: OrganizationCollectionSuggestion[]; suggestedRelatedResources: OrganizationRelatedSuggestion[] }
export type SemanticOrganizationResult = { resourceId: number; understandingStatus: string; tagsAssigned: string[]; collectionsAssigned: number[]; collectionSuggestions: OrganizationCollectionSuggestion[]; newCollectionSuggestions: OrganizationCollectionSuggestion[]; warnings: string[] }
export type OrganizationBatchResult = { processed: number; assigned: number; suggested: number; skipped: number; failed: number; results: SemanticOrganizationResult[] }

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
export async function preflightAsk(input: AskInput) { return (await apiClient.post<AskPreflight>('/ask/preflight', input)).data }
export async function startAskAttempt(input: AskInput) { return (await apiClient.post<AskAttempt>('/ask/attempts', input)).data }
export async function retryAskAttempt(attemptId: number) { return (await apiClient.post<AskAttempt>(`/ask/attempts/${attemptId}/retry`)).data }
export async function getAskUsage() { return (await apiClient.get<AiUsage>('/ask/usage')).data }
export function subscribeAskTrace(attemptId: number, onEvent: (event: AskTraceEvent) => void, onError: () => void) {
  const source = new EventSource(`/api/ask/attempts/${attemptId}/events`, { withCredentials: true })
  source.addEventListener('ask-trace', event => {
    try { onEvent(JSON.parse((event as MessageEvent).data) as AskTraceEvent) } catch { /* malformed telemetry is ignored by the UI */ }
  })
  source.onerror = onError
  return () => source.close()
}
export async function getCollections() { return (await apiClient.get<KnowledgeCollection[]>('/collections')).data }
export async function createCollection(name:string, description='') { return (await apiClient.post<KnowledgeCollection>('/collections', { name, description })).data }
export async function updateCollection(id:number, name:string, description='') { return (await apiClient.patch<KnowledgeCollection>(`/collections/${id}`, { name, description })).data }
export async function deleteCollection(id:number) { await apiClient.delete(`/collections/${id}`) }
export async function getTags() { return (await apiClient.get<KnowledgeTag[]>('/tags')).data }
export async function createTag(name:string) { return (await apiClient.post<KnowledgeTag>('/tags', { name })).data }
export async function getResourceTags(id:number) { return (await apiClient.get<KnowledgeTag[]>(`/resources/${id}/tags`)).data }
export async function getResourceCollections(id:number) { return (await apiClient.get<KnowledgeCollection[]>(`/resources/${id}/collections`)).data }
export async function getResourceUnderstanding(id:number) { return (await apiClient.get<ResourceUnderstanding>(`/resources/${id}/understanding`)).data }
export async function getResourceDeepDive(id:number) { return (await apiClient.get<ResourceDeepDive>(`/resources/${id}/deep-dive`)).data }
export async function getResourceKnowledgeMap(id:number) { return (await apiClient.get<ResourceKnowledgeMap>(`/resources/${id}/knowledge-map`)).data }
export async function assignTagToResource(resourceId:number, tagId:number) { await apiClient.put(`/resources/${resourceId}/tags/${tagId}`) }
export async function removeTagFromResource(resourceId:number, tagId:number) { await apiClient.delete(`/resources/${resourceId}/tags/${tagId}`) }
export async function assignResourceToCollection(collectionId:number, resourceId:number) { await apiClient.put(`/collections/${collectionId}/resources/${resourceId}`) }
export async function assignResourcesToCollection(collectionId:number, resourceIds:number[]) { return (await apiClient.post<{ requestedCount:number; affectedCount:number }>(`/collections/${collectionId}/resources/bulk`, { resourceIds })).data }
export async function removeResourceFromCollection(collectionId:number, resourceId:number) { await apiClient.delete(`/collections/${collectionId}/resources/${resourceId}`) }
export async function getCollectionResources(collectionId:number) { return (await apiClient.get<Resource[]>(`/collections/${collectionId}/resources`)).data }
export async function getChatSessions() { return (await apiClient.get<ChatSession[]>('/ask/sessions')).data }
export async function getChatSession(id:number) { return (await apiClient.get<ChatDetail>(`/ask/sessions/${id}`)).data }
export async function getResourceNotes(id:number) { return (await apiClient.get<ResourceNote[]>(`/resources/${id}/notes`)).data }
export async function createResourceNote(id:number, content:string) { return (await apiClient.post<ResourceNote>(`/resources/${id}/notes`, { content })).data }
export async function updateResourceNote(id:number,noteId:number,content:string) { return (await apiClient.patch<ResourceNote>(`/resources/${id}/notes/${noteId}`, { content })).data }
export async function deleteResourceNote(id:number,noteId:number) { await apiClient.delete(`/resources/${id}/notes/${noteId}`) }
export async function getRelatedResources(id:number) { return (await apiClient.get<RelatedResource[]>(`/resources/${id}/related`)).data }
export async function getResourceActivity(id:number) { return (await apiClient.get<ResourceActivity>(`/resources/${id}/activity`)).data }
export async function recordResourceOpened(id:number) { return (await apiClient.post<ResourceActivity>(`/resources/${id}/open`)).data }
export async function updateResourceProgress(id:number, progressPercent:number) { return (await apiClient.put<ResourceActivity>(`/resources/${id}/progress`, { progressPercent })).data }
export async function getRecentActivity() { return (await apiClient.get<RecentActivity[]>('/activity/recent')).data }
export async function getOrganizationSuggestions(id:number) { return (await apiClient.get<OrganizationSuggestions>(`/resources/${id}/organization/suggestions`)).data }
export async function applyOrganization(id:number, payload:{tagNames:string[]; collectionIds:number[]; newCollectionNames:string[]; relatedResourceIds:number[]}) { await apiClient.post(`/resources/${id}/organization/apply`, payload) }
export async function deleteResource(id:number) { await apiClient.delete(`/resources/${id}`) }
export async function retryResource(id:number) { return (await apiClient.post<Resource>(`/resources/${id}/retry`)).data }
export async function deleteResources(ids:number[]) { return (await apiClient.post<{ requestedCount:number; affectedCount:number }>('/resources/bulk-delete', { resourceIds: ids })).data }
export async function updateResourceFavorite(id:number, favorite:boolean) { return (await apiClient.patch<Resource>(`/resources/${id}`, { favorite })).data }
export async function autoOrganizeAll() { return (await apiClient.post<OrganizationBatchResult>('/resources/auto-organize-all')).data }
export async function autoOrganizeResource(id: number) { return (await apiClient.post<SemanticOrganizationResult>(`/resources/${id}/auto-organize`)).data }
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

