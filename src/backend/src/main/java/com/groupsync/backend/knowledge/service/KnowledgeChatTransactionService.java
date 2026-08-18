package com.groupsync.backend.knowledge.service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.groupsync.backend.knowledge.dto.*;
import com.groupsync.backend.knowledge.model.*;
import com.groupsync.backend.knowledge.rag.GroundedPromptBuilder;
import com.groupsync.backend.knowledge.rag.RetrievalScope;
import com.groupsync.backend.knowledge.rag.RetrievedChunk;
import com.groupsync.backend.knowledge.repository.*;
import com.groupsync.backend.shared.exception.NotFoundException;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

/**
 * Dedicated transactional boundary service for Knowledge Chat persistence operations.
 * Isolates short DB write transactions from long-running external RAG retrieval and Gemini API calls.
 */
@Service
public class KnowledgeChatTransactionService {

    private static final String INSUFFICIENT_CONTEXT = "I don't have enough evidence in the selected KnowledgeOS sources to answer that yet.";
    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[(\\d+)\\]");

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final CitationRepository citationRepository;
    private final DocumentChunkRepository chunkRepository;
    private final ResourceRepository resourceRepository;
    private final UserAccountRepository userRepository;
    private final KnowledgeWorkspaceService workspaceService;

    public KnowledgeChatTransactionService(
            ChatSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            CitationRepository citationRepository,
            DocumentChunkRepository chunkRepository,
            ResourceRepository resourceRepository,
            UserAccountRepository userRepository,
            KnowledgeWorkspaceService workspaceService) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.citationRepository = citationRepository;
        this.chunkRepository = chunkRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.workspaceService = workspaceService;
    }

    public record ChatPreparation(
            Long sessionId,
            RetrievalScope scopeType,
            Long collectionId,
            List<Long> selectedResourceIds,
            Long thisResourceId,
            List<GroundedPromptBuilder.ConversationTurn> historyTurns,
            List<ChatMessage> previousMessages
    ) {}

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatPreparation prepareConversation(Long ownerId, AskKnowledgeRequest request) {
        ChatSession session = resolveSession(ownerId, request);

        // Fetch previous session message history for multi-turn conversational context
        List<ChatMessage> previousMessages = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        List<GroundedPromptBuilder.ConversationTurn> historyTurns = new ArrayList<>();
        int startIdx = Math.max(0, previousMessages.size() - 4);
        for (int i = startIdx; i < previousMessages.size(); i++) {
            ChatMessage msg = previousMessages.get(i);
            historyTurns.add(new GroundedPromptBuilder.ConversationTurn(msg.getRole().name(), msg.getContent()));
        }

        // Save current user message inside this short transaction
        messageRepository.save(new ChatMessage(session, ChatMessageRole.USER, request.question().trim()));

        List<Long> selectedIds = session.getResources().stream().map(Resource::getId).toList();
        Long thisResourceId = session.getScopeType() == RetrievalScope.THIS_RESOURCE
                ? selectedIds.stream().findFirst().orElse(null) : null;

        return new ChatPreparation(
                session.getId(),
                session.getScopeType(),
                session.getCollectionId(),
                selectedIds,
                thisResourceId,
                historyTurns,
                previousMessages
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AskKnowledgeResponse persistInsufficientContext(Long sessionId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Chat session not found."));
        messageRepository.save(new ChatMessage(session, ChatMessageRole.ASSISTANT, INSUFFICIENT_CONTEXT));
        return new AskKnowledgeResponse(session.getId(), INSUFFICIENT_CONTEXT, false, List.of());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AskKnowledgeResponse persistAssistantResult(Long sessionId, String answer, List<RetrievedChunk> chunks) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Chat session not found."));
        ChatMessage assistantMessage = messageRepository.save(new ChatMessage(session, ChatMessageRole.ASSISTANT, answer));

        Set<Integer> referencedIndices = extractCitationIndices(answer, chunks.size());

        Map<Long, DocumentChunk> persistedChunks = new HashMap<>();
        chunkRepository.findAllById(chunks.stream().map(RetrievedChunk::chunkId).toList())
                .forEach(chunk -> persistedChunks.put(chunk.getId(), chunk));

        List<CitationResponse> citations = new ArrayList<>();
        for (Integer index : referencedIndices) {
            RetrievedChunk chunk = chunks.get(index - 1);
            DocumentChunk persisted = persistedChunks.get(chunk.chunkId());
            if (persisted == null) continue;
            // Preserve the original 1-based evidence marker number in citationOrder
            citationRepository.save(new Citation(assistantMessage, persisted, index, 1 - chunk.distance(), excerpt(chunk.content())));
            citations.add(new CitationResponse(chunk.chunkId(), chunk.resourceId(), chunk.resourceTitle(), chunk.pageNumber(),
                    chunk.section(), index, 1 - chunk.distance(), excerpt(chunk.content())));
        }

        return new AskKnowledgeResponse(session.getId(), answer, true, citations);
    }

    private ChatSession resolveSession(Long ownerId, AskKnowledgeRequest request) {
        if (request.sessionId() != null) {
            return sessionRepository.findByIdAndOwnerId(request.sessionId(), ownerId)
                    .orElseThrow(() -> new NotFoundException("Chat session not found."));
        }
        if (request.scope() == null) throw new IllegalArgumentException("A retrieval scope is required for a new chat.");
        if (request.scope() == RetrievalScope.COLLECTION) {
            if (request.collectionId() == null) throw new IllegalArgumentException("COLLECTION chats require a collection.");
            workspaceService.requireCollection(ownerId, request.collectionId());
        }
        Set<Resource> resources = selectedResources(ownerId, request);
        UserAccount owner = userRepository.findById(ownerId).orElseThrow(() -> new NotFoundException("User not found."));
        String title = request.sessionTitle() == null || request.sessionTitle().isBlank()
                ? truncate(request.question(), 240) : truncate(request.sessionTitle(), 240);
        return sessionRepository.save(new ChatSession(owner, title, request.scope(), request.collectionId(), resources));
    }

    private Set<Resource> selectedResources(Long ownerId, AskKnowledgeRequest request) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (request.resourceId() != null) ids.add(request.resourceId());
        if (request.resourceIds() != null) ids.addAll(request.resourceIds());
        if (request.scope() == RetrievalScope.THIS_RESOURCE && ids.size() != 1) {
            throw new IllegalArgumentException("THIS_RESOURCE chats require exactly one resource.");
        }
        if (request.scope() == RetrievalScope.SELECTED_RESOURCES && ids.isEmpty()) {
            throw new IllegalArgumentException("SELECTED_RESOURCES chats require at least one resource.");
        }
        List<Resource> resources = resourceRepository.findAllById(ids);
        if (resources.size() != ids.size() || resources.stream().anyMatch(resource -> !resource.getOwner().getId().equals(ownerId))) {
            throw new NotFoundException("One or more selected resources were not found.");
        }
        return new LinkedHashSet<>(resources);
    }

    private Set<Integer> extractCitationIndices(String answer, int totalChunks) {
        Set<Integer> indices = new LinkedHashSet<>();
        if (answer == null || totalChunks <= 0) return indices;
        Matcher matcher = CITATION_PATTERN.matcher(answer);
        while (matcher.find()) {
            try {
                int idx = Integer.parseInt(matcher.group(1));
                if (idx >= 1 && idx <= totalChunks) {
                    indices.add(idx);
                }
            } catch (NumberFormatException ignored) {}
        }
        return indices;
    }

    private String excerpt(String content) { return truncate(content, 500); }
    private String truncate(String content, int max) { return content.length() <= max ? content : content.substring(0, max - 1).trim() + "…"; }
}
