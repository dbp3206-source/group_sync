package com.groupsync.backend.knowledge.service;

import java.util.*;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.groupsync.backend.knowledge.dto.*;
import com.groupsync.backend.knowledge.model.*;
import com.groupsync.backend.knowledge.rag.*;
import com.groupsync.backend.knowledge.rag.HybridRetrievalStrategy.HybridExecutionDetails;
import com.groupsync.backend.knowledge.rag.ParentChildContextExpander.ExpandedContext;
import com.groupsync.backend.knowledge.repository.*;
import com.groupsync.backend.shared.exception.NotFoundException;

/**
 * Orchestrates KnowledgeOS RAG v2 chat queries.
 * Integrates multi-turn contextualization, KnowledgeQueryPlanner intent classification,
 * Structured relational execution, Semantic pgvector retrieval, Filtered Hybrid Retrieval,
 * Parent-Child context expansion, and deterministic system-level execution tracing.
 */
@Service
public class KnowledgeChatService {
    private static final String INSUFFICIENT_CONTEXT = "I don't have enough evidence in the selected KnowledgeOS sources to answer that yet.";
    private static final double MIN_RELEVANCE = 0.15d;

    private final KnowledgeChatTransactionService chatTransactionService;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final CitationRepository citationRepository;
    private final HybridRetrievalStrategy retrievalStrategy;
    private final SemanticRetrievalStrategy semanticRetrievalStrategy;
    private final KnowledgeQueryPlanner queryPlanner;
    private final StructuredKnowledgeQueryService structuredQueryService;
    private final ParentChildContextExpander parentChildExpander;
    private final LanguageModelClient languageModelClient;
    private final GeminiProperties geminiProperties;

    @Autowired
    public KnowledgeChatService(
            KnowledgeChatTransactionService chatTransactionService,
            ChatSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            CitationRepository citationRepository,
            @Qualifier("hybridRetrieval") HybridRetrievalStrategy retrievalStrategy,
            @Qualifier("semanticRetrieval") SemanticRetrievalStrategy semanticRetrievalStrategy,
            KnowledgeQueryPlanner queryPlanner,
            StructuredKnowledgeQueryService structuredQueryService,
            ParentChildContextExpander parentChildExpander,
            LanguageModelClient languageModelClient,
            GeminiProperties geminiProperties) {
        this.chatTransactionService = chatTransactionService;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.citationRepository = citationRepository;
        this.retrievalStrategy = retrievalStrategy;
        this.semanticRetrievalStrategy = semanticRetrievalStrategy;
        this.queryPlanner = queryPlanner;
        this.structuredQueryService = structuredQueryService;
        this.parentChildExpander = parentChildExpander;
        this.languageModelClient = languageModelClient;
        this.geminiProperties = Objects.requireNonNull(geminiProperties, "geminiProperties must not be null");
    }

    public AskKnowledgeResponse ask(Long ownerId, AskKnowledgeRequest request) {
        KnowledgeChatTransactionService.ChatPreparation prep =
                chatTransactionService.prepareConversation(ownerId, request);
        try {
            return askPrepared(ownerId, request, prep);
        } catch (Throwable error) {
            chatTransactionService.markUserFailed(prep.userMessageId(), AskFailureClassifier.classify(error));
            if (error instanceof RuntimeException runtime) throw runtime;
            if (error instanceof Error fatal) throw fatal;
            throw new IllegalStateException("KnowledgeOS Ask failed.", error);
        }
    }


    public AskKnowledgeResponse askPrepared(Long ownerId, AskKnowledgeRequest request,
                                            KnowledgeChatTransactionService.ChatPreparation prep) {
        long startMs = System.currentTimeMillis();
        AskTraceContext.emit(AskTraceStage.QUERY_RECEIVED, new AskTraceTechnicalDetails(null, null, null, null, null, null, null, null, null, null, null, null));

        // Multi-turn conversational contextualization
        String contextualQuestion = contextualizeSearchQuery(request.question().trim(), prep.previousMessages());

        // Step 2: Intelligent Query Planning (intent classification & schema filtering)
        QueryPlan plan = queryPlanner.plan(
                ownerId,
                contextualQuestion,
                prep.scopeType(),
                prep.thisResourceId(),
                prep.selectedResourceIds(),
                prep.collectionId()
        );

        PlannerTrace plannerTrace = new PlannerTrace(
                plan.mode(),
                plan.operation(),
                plan.semanticQuery(),
                plan.explanation()
        );

        FilterTrace filterTrace = new FilterTrace(
                prep.scopeType(),
                plan.filters() != null && plan.filters().resourceType() != null ? plan.filters().resourceType().name() : null,
                plan.filters() != null ? plan.filters().favorite() : null,
                plan.filters() != null && plan.filters().collectionIds() != null ? plan.filters().collectionIds().size() : null,
                plan.filters() != null && plan.filters().tagIds() != null ? plan.filters().tagIds().size() : null,
                plan.filters() != null && plan.filters().resourceIds() != null ? plan.filters().resourceIds().size() : null,
                plan.filters() != null && plan.filters().createdAfter() != null ? plan.filters().createdAfter().toString() : null,
                plan.filters() != null && plan.filters().createdBefore() != null ? plan.filters().createdBefore().toString() : null
        );
        AskTraceContext.emit(AskTraceStage.PLAN_READY, new AskTraceTechnicalDetails(plan.mode().name(), plan.operation().name(), null, null, null, null, null, null, null, null, null, null));
        if (plan.mode() == QueryMode.FILTERED_HYBRID && hasActualFilters(plan.filters())) {
            AskTraceContext.emit(AskTraceStage.FILTERS_APPLIED, new AskTraceTechnicalDetails(plan.mode().name(), plan.operation().name(), null, null, null, null, null, null, null, null, null, null));
        }

        // Step 3: Handle Structured Queries (COUNT, LIST) directly from PostgreSQL relational facts
        if (plan.mode() == QueryMode.STRUCTURED) {
            StructuredKnowledgeQueryService.StructuredResult result = structuredQueryService.execute(
                    ownerId, plan, prep.scopeType(), prep.thisResourceId(), prep.selectedResourceIds(), prep.collectionId()
            );
            AskKnowledgeResponse saved = chatTransactionService.persistAssistantResult(prep.sessionId(), prep.userMessageId(), result.textResponse(), List.of());
            AskTraceContext.emit(AskTraceStage.STRUCTURED_OPERATION_COMPLETE, new AskTraceTechnicalDetails(plan.mode().name(), plan.operation().name(), null, null, null, null, null, null, null, 0, null, null));
            long durationMs = System.currentTimeMillis() - startMs;
            RagExecutionTrace trace = RagExecutionTrace.forStructured(plannerTrace, filterTrace, durationMs);
            return new AskKnowledgeResponse(saved.sessionId(), saved.answer(), saved.grounded(), saved.citations(), trace);
        }

        // Step 4: Retrieval based on QueryMode
        List<RetrievedChunk> candidateChildren;
        RetrievalTrace retrievalTrace;
        FusionTrace fusionTrace;

        if (plan.mode() == QueryMode.SEMANTIC) {
            // Pure semantic retrieval branch (pgvector only, no FTS, no RRF)
            candidateChildren = semanticRetrievalStrategy.retrieve(
                    ownerId,
                    plan.semanticQuery(),
                    prep.scopeType(),
                    prep.thisResourceId(),
                    prep.selectedResourceIds(),
                    prep.collectionId(),
                    plan.filters(),
                    6
            );
            retrievalTrace = new RetrievalTrace(
                    candidateChildren.size(),
                    0,
                    candidateChildren.size()
            );
            fusionTrace = null;
            AskTraceContext.emit(AskTraceStage.SEMANTIC_RETRIEVAL_COMPLETE, new AskTraceTechnicalDetails(plan.mode().name(), plan.operation().name(), candidateChildren.size(), 0, candidateChildren.size(), null, null, null, null, null, null, null));
        } else {
            // Filtered Hybrid Retrieval (pgvector + FTS + RRF)
            HybridExecutionDetails hybridDetails = retrievalStrategy.retrieveWithTrace(
                    ownerId,
                    plan.semanticQuery(),
                    prep.scopeType(),
                    prep.thisResourceId(),
                    prep.selectedResourceIds(),
                    prep.collectionId(),
                    plan.filters()
            );
            candidateChildren = hybridDetails.fusedChunks();
            retrievalTrace = new RetrievalTrace(
                    hybridDetails.semanticCandidateCount(),
                    hybridDetails.keywordCandidateCount(),
                    hybridDetails.totalInputCandidates()
            );
            fusionTrace = new FusionTrace(
                    hybridDetails.totalInputCandidates(),
                    candidateChildren.size(),
                    hybridDetails.rrfK()
            );
            AskTraceContext.emit(AskTraceStage.SEMANTIC_RETRIEVAL_COMPLETE, new AskTraceTechnicalDetails(plan.mode().name(), plan.operation().name(), hybridDetails.semanticCandidateCount(), null, null, null, null, null, null, null, null, null));
            AskTraceContext.emit(AskTraceStage.LEXICAL_RETRIEVAL_COMPLETE, new AskTraceTechnicalDetails(plan.mode().name(), plan.operation().name(), null, hybridDetails.keywordCandidateCount(), null, null, null, null, null, null, null, null));
            AskTraceContext.emit(AskTraceStage.RRF_COMPLETE, new AskTraceTechnicalDetails(plan.mode().name(), plan.operation().name(), hybridDetails.semanticCandidateCount(), hybridDetails.keywordCandidateCount(), hybridDetails.totalInputCandidates(), hybridDetails.fusedChunks().size(), null, null, null, null, null, null));
        }

        if (candidateChildren.isEmpty() || candidateChildren.getFirst().distance() > 1.0 - MIN_RELEVANCE) {
            AskKnowledgeResponse saved = chatTransactionService.persistInsufficientContext(prep.sessionId(), prep.userMessageId());
            long durationMs = System.currentTimeMillis() - startMs;
            RagExecutionTrace trace = new RagExecutionTrace(
                    plan.mode(), plan.operation(), plannerTrace, filterTrace, retrievalTrace, fusionTrace,
                    null,
                    null,
                    null,
                    durationMs
            );
            return new AskKnowledgeResponse(saved.sessionId(), saved.answer(), saved.grounded(), saved.citations(), trace);
        }

        // Step 5: Parent Context Expansion & Deduplication with strict context budgeting
        ExpandedContext expanded = parentChildExpander.expand(candidateChildren);
        List<RetrievedChunk> promptChunks = expanded.promptContextChunks().isEmpty() ? candidateChildren : expanded.promptContextChunks();

        ParentChildTrace parentChildTrace = new ParentChildTrace(
                candidateChildren.size(),
                expanded.uniqueParentsFound(),
                expanded.duplicateParentsDeduplicated()
        );
        AskTraceContext.emit(AskTraceStage.PARENT_CONTEXT_COMPLETE, new AskTraceTechnicalDetails(plan.mode().name(), plan.operation().name(), null, null, null, candidateChildren.size(), expanded.uniqueParentsFound(), expanded.charactersUsed(), parentChildExpander.getMaxContextChars(), null, null, null));

        ContextBudgetTrace contextBudgetTrace = new ContextBudgetTrace(
                expanded.uniqueParentsFound(),
                expanded.charactersUsed(),
                parentChildExpander.getMaxContextChars()
        );
        AskTraceContext.emit(AskTraceStage.CONTEXT_READY, new AskTraceTechnicalDetails(plan.mode().name(), plan.operation().name(), null, null, null, promptChunks.size(), expanded.uniqueParentsFound(), expanded.charactersUsed(), parentChildExpander.getMaxContextChars(), null, null, null));

        // Step 6: Grounded Prompt Generation & Gemini LLM synthesis
        String groundedPrompt = GroundedPromptBuilder.build(request.question().trim(), promptChunks, prep.historyTurns());
        AskTraceContext.emit(AskTraceStage.GENERATION_STARTED, new AskTraceTechnicalDetails(plan.mode().name(), plan.operation().name(), null, null, null, promptChunks.size(), null, null, null, null, geminiProperties.chatModel(), null));
        String answer = languageModelClient.answer(groundedPrompt);
        AskTraceContext.emit(AskTraceStage.GENERATION_COMPLETE, new AskTraceTechnicalDetails(plan.mode().name(), plan.operation().name(), null, null, null, promptChunks.size(), null, null, null, null, geminiProperties.chatModel(), null));

        // Step 7: Persist assistant message and verified citations
        AskKnowledgeResponse saved = chatTransactionService.persistAssistantResult(prep.sessionId(), prep.userMessageId(), answer, promptChunks);
        AskTraceContext.emit(AskTraceStage.CITATIONS_VERIFIED, new AskTraceTechnicalDetails(plan.mode().name(), plan.operation().name(), null, null, null, promptChunks.size(), null, null, null, saved.citations().size(), geminiProperties.chatModel(), null));

        String modelName = geminiProperties.chatModel();
        GenerationTrace generationTrace = new GenerationTrace(
                modelName,
                promptChunks.size(),
                saved.citations().size()
        );

        long durationMs = System.currentTimeMillis() - startMs;
        RagExecutionTrace trace = new RagExecutionTrace(
                plan.mode(), plan.operation(), plannerTrace, filterTrace, retrievalTrace, fusionTrace,
                parentChildTrace, contextBudgetTrace, generationTrace, durationMs
        );

        return new AskKnowledgeResponse(saved.sessionId(), saved.answer(), saved.grounded(), saved.citations(), trace);
    }

    private boolean hasActualFilters(KnowledgeQueryFilters filters) {
        return filters != null && (filters.resourceType() != null || filters.favorite() != null
                || (filters.collectionIds() != null && !filters.collectionIds().isEmpty())
                || (filters.tagIds() != null && !filters.tagIds().isEmpty())
                || (filters.resourceIds() != null && !filters.resourceIds().isEmpty())
                || filters.createdAfter() != null || filters.createdBefore() != null);
    }

    private static final Pattern FOLLOW_UP_PATTERN = Pattern.compile(
            "\\b(it|this|that|these|those|they|them|its|their|the above|previous|earlier)\\b" +
            "|(?iu)\\b(nó|chúng|chúng nó|cách trên|phương pháp trên|phần đó|ở trên|điều này|vấn đề này|cái này|đoạn trên)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    public static boolean isFollowUpQuestion(String question) {
        if (question == null || question.isBlank()) return false;
        return FOLLOW_UP_PATTERN.matcher(question).find();
    }

    public static String contextualizeSearchQuery(String currentQuestion, List<ChatMessage> history) {
        if (history == null || history.isEmpty() || currentQuestion == null || currentQuestion.isBlank()) {
            return currentQuestion != null ? currentQuestion.trim() : "";
        }
        ChatMessage lastUserMsg = null;
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).getRole() == ChatMessageRole.USER) {
                lastUserMsg = history.get(i);
                break;
            }
        }
        if (lastUserMsg == null || lastUserMsg.getContent() == null || lastUserMsg.getContent().isBlank()) {
            return currentQuestion.trim();
        }

        String prevText = lastUserMsg.getContent().trim();
        if (isFollowUpQuestion(currentQuestion)) {
            return prevText + " " + currentQuestion.trim();
        }
        return currentQuestion.trim();
    }

    @Transactional(readOnly = true)
    public List<ChatSessionSummaryResponse> sessions(Long ownerId) {
        return sessionRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId).stream().map(session ->
                new ChatSessionSummaryResponse(
                        session.getId(),
                        session.getTitle(),
                        session.getScopeType().name(),
                        session.getCollectionId() == null ? 0L : session.getCollectionId(),
                        session.getUpdatedAt()
                )
        ).toList();
    }

    @Transactional(readOnly = true)
    public ChatSessionDetailResponse session(Long ownerId, Long sessionId) {
        ChatSession session = sessionRepository.findByIdAndOwnerId(sessionId, ownerId)
                .orElseThrow(() -> new NotFoundException("Chat session not found."));
        List<ChatMessageDto> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream().map(message -> {
            List<CitationResponse> citations = citationRepository.findByMessageIdOrderByCitationOrderAsc(message.getId()).stream().map(citation ->
                    new CitationResponse(
                            citation.getChunk().getId(),
                            citation.getChunk().getResource().getId(),
                            citation.getChunk().getResource().getTitle(),
                            citation.getChunk().getPageNumber(),
                            citation.getChunk().getSection(),
                            citation.getCitationOrder(),
                            citation.getRelevanceScore(),
                            citation.getEvidenceExcerpt()
                    )
            ).toList();
            return new ChatMessageDto(
                    message.getId(),
                    message.getRole().name(),
                    message.getContent(),
                    message.getCreatedAt(),
                    citations,
                    message.getStatus(),
                    message.getFailureCategory()
            );
        }).toList();
        return new ChatSessionDetailResponse(
                session.getId(),
                session.getTitle(),
                session.getScopeType().name(),
                session.getCollectionId() == null ? 0L : session.getCollectionId(),
                session.getResources().stream().map(Resource::getId).toList(),
                messages
        );
    }
}
