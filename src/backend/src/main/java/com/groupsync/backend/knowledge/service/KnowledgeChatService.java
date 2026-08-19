package com.groupsync.backend.knowledge.service;

import java.util.*;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.groupsync.backend.knowledge.dto.*;
import com.groupsync.backend.knowledge.model.*;
import com.groupsync.backend.knowledge.rag.*;
import com.groupsync.backend.knowledge.rag.ParentChildContextExpander.ExpandedContext;
import com.groupsync.backend.knowledge.repository.*;
import com.groupsync.backend.shared.exception.NotFoundException;

/**
 * Orchestrates KnowledgeOS RAG v2 chat queries.
 * Integrates multi-turn contextualization, KnowledgeQueryPlanner intent classification,
 * Structured relational execution, Filtered Hybrid Retrieval, and Parent-Child context expansion.
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
    private final KnowledgeQueryPlanner queryPlanner;
    private final StructuredKnowledgeQueryService structuredQueryService;
    private final ParentChildContextExpander parentChildExpander;
    private final LanguageModelClient languageModelClient;

    public KnowledgeChatService(
            KnowledgeChatTransactionService chatTransactionService,
            ChatSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            CitationRepository citationRepository,
            @Qualifier("hybridRetrieval") HybridRetrievalStrategy retrievalStrategy,
            KnowledgeQueryPlanner queryPlanner,
            StructuredKnowledgeQueryService structuredQueryService,
            ParentChildContextExpander parentChildExpander,
            LanguageModelClient languageModelClient) {
        this.chatTransactionService = chatTransactionService;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.citationRepository = citationRepository;
        this.retrievalStrategy = retrievalStrategy;
        this.queryPlanner = queryPlanner;
        this.structuredQueryService = structuredQueryService;
        this.parentChildExpander = parentChildExpander;
        this.languageModelClient = languageModelClient;
    }

    public AskKnowledgeResponse ask(Long ownerId, AskKnowledgeRequest request) {
        // Step 1: Prepare session and save user message in a short, isolated transaction
        KnowledgeChatTransactionService.ChatPreparation prep =
                chatTransactionService.prepareConversation(ownerId, request);

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

        // Step 3: Handle Structured Queries (COUNT, LIST) directly from PostgreSQL relational facts
        if (plan.mode() == QueryMode.STRUCTURED) {
            StructuredKnowledgeQueryService.StructuredResult result = structuredQueryService.execute(
                    ownerId, plan, prep.scopeType(), prep.thisResourceId(), prep.selectedResourceIds(), prep.collectionId()
            );
            return chatTransactionService.persistAssistantResult(prep.sessionId(), result.textResponse(), List.of());
        }

        // Step 4: Filtered Hybrid Retrieval (pgvector + FTS + RRF) on child chunks
        List<RetrievedChunk> candidateChildren = retrievalStrategy.retrieve(
                ownerId,
                plan.semanticQuery(),
                prep.scopeType(),
                prep.thisResourceId(),
                prep.selectedResourceIds(),
                prep.collectionId(),
                plan.filters()
        );

        if (candidateChildren.isEmpty() || candidateChildren.getFirst().distance() > 1.0 - MIN_RELEVANCE) {
            return chatTransactionService.persistInsufficientContext(prep.sessionId());
        }

        // Step 5: Parent Context Expansion & Deduplication with strict context budgeting
        ExpandedContext expanded = parentChildExpander.expand(candidateChildren);
        List<RetrievedChunk> promptChunks = expanded.promptContextChunks().isEmpty() ? candidateChildren : expanded.promptContextChunks();

        // Step 6: Grounded Prompt Generation & Gemini LLM synthesis
        String groundedPrompt = GroundedPromptBuilder.build(request.question().trim(), promptChunks, prep.historyTurns());
        String answer = languageModelClient.answer(groundedPrompt);

        // Step 7: Persist assistant message and verified citations
        return chatTransactionService.persistAssistantResult(prep.sessionId(), answer, promptChunks);
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
    public List<Map<String, Object>> sessions(Long ownerId) {
        return sessionRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId).stream().map(session -> Map.<String,Object>of(
                "id", session.getId(), "title", session.getTitle(), "scope", session.getScopeType().name(),
                "collectionId", session.getCollectionId() == null ? 0L : session.getCollectionId(), "updatedAt", session.getUpdatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> session(Long ownerId, Long sessionId) {
        ChatSession session = sessionRepository.findByIdAndOwnerId(sessionId, ownerId).orElseThrow(() -> new NotFoundException("Chat session not found."));
        List<Map<String,Object>> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream().map(message -> {
            List<CitationResponse> citations = citationRepository.findByMessageIdOrderByCitationOrderAsc(message.getId()).stream().map(citation -> new CitationResponse(
                citation.getChunk().getId(), citation.getChunk().getResource().getId(), citation.getChunk().getResource().getTitle(), citation.getChunk().getPageNumber(), citation.getChunk().getSection(), citation.getCitationOrder(), citation.getRelevanceScore(), citation.getEvidenceExcerpt())).toList();
            return Map.<String,Object>of("id", message.getId(), "role", message.getRole().name(), "content", message.getContent(), "createdAt", message.getCreatedAt(), "citations", citations);
        }).toList();
        return Map.of("id",session.getId(),"title",session.getTitle(),"scope",session.getScopeType().name(),"collectionId",session.getCollectionId()==null?0L:session.getCollectionId(),"resourceIds",session.getResources().stream().map(Resource::getId).toList(),"messages",messages);
    }
}
