package com.groupsync.backend.knowledge.service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.groupsync.backend.knowledge.dto.*;
import com.groupsync.backend.knowledge.model.*;
import com.groupsync.backend.knowledge.rag.*;
import com.groupsync.backend.knowledge.repository.*;
import com.groupsync.backend.shared.exception.NotFoundException;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@Service
public class KnowledgeChatService {
    private static final String INSUFFICIENT_CONTEXT = "I don't have enough evidence in the selected KnowledgeOS sources to answer that yet.";
    /** Minimum fused relevance score (1 - distance). Hybrid RRF scores are lower than cosine similarity; 0.15 is calibrated to reject truly empty evidence. */
    private static final double MIN_RELEVANCE = 0.15d;
    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[(\\d+)\\]");

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final CitationRepository citationRepository;
    private final DocumentChunkRepository chunkRepository;
    private final ResourceRepository resourceRepository;
    private final UserAccountRepository userRepository;
    private final RetrievalStrategy retrievalStrategy;
    private final LanguageModelClient languageModelClient;
    private final KnowledgeWorkspaceService workspaceService;

    public KnowledgeChatService(ChatSessionRepository sessionRepository, ChatMessageRepository messageRepository,
            CitationRepository citationRepository, DocumentChunkRepository chunkRepository, ResourceRepository resourceRepository,
            UserAccountRepository userRepository, @Qualifier("hybridRetrieval") RetrievalStrategy retrievalStrategy,
            LanguageModelClient languageModelClient, KnowledgeWorkspaceService workspaceService) {
        this.sessionRepository = sessionRepository; this.messageRepository = messageRepository; this.citationRepository = citationRepository;
        this.chunkRepository = chunkRepository; this.resourceRepository = resourceRepository; this.userRepository = userRepository;
        this.retrievalStrategy = retrievalStrategy; this.languageModelClient = languageModelClient; this.workspaceService = workspaceService;
    }

    @Transactional
    public AskKnowledgeResponse ask(Long ownerId, AskKnowledgeRequest request) {
        ChatSession session = resolveSession(ownerId, request);

        // Fetch previous session message history for multi-turn conversational context
        List<ChatMessage> previousMessages = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        List<GroundedPromptBuilder.ConversationTurn> historyTurns = new ArrayList<>();
        int startIdx = Math.max(0, previousMessages.size() - 4);
        for (int i = startIdx; i < previousMessages.size(); i++) {
            ChatMessage msg = previousMessages.get(i);
            historyTurns.add(new GroundedPromptBuilder.ConversationTurn(msg.getRole().name(), msg.getContent()));
        }

        // Save current user message
        messageRepository.save(new ChatMessage(session, ChatMessageRole.USER, request.question().trim()));

        // Formulate search query: enrich with recent topic context if this is a follow-up turn
        String searchQuery = contextualizeSearchQuery(request.question().trim(), previousMessages);

        List<Long> selectedIds = session.getResources().stream().map(Resource::getId).toList();
        Long thisResourceId = session.getScopeType() == RetrievalScope.THIS_RESOURCE
                ? selectedIds.stream().findFirst().orElse(null) : null;
        List<RetrievedChunk> chunks = retrievalStrategy.retrieve(ownerId, searchQuery, session.getScopeType(),
                thisResourceId, selectedIds, session.getCollectionId());

        // After hybrid RRF, distance is 1 - rrfScore (lower = better). Reject when best match has distance > (1 - MIN_RELEVANCE).
        if (chunks.isEmpty() || chunks.getFirst().distance() > 1.0 - MIN_RELEVANCE) {
            messageRepository.save(new ChatMessage(session, ChatMessageRole.ASSISTANT, INSUFFICIENT_CONTEXT));
            return new AskKnowledgeResponse(session.getId(), INSUFFICIENT_CONTEXT, false, List.of());
        }

        // Build prompt with untrusted knowledge and recent conversation history
        String groundedPrompt = GroundedPromptBuilder.build(request.question().trim(), chunks, historyTurns);
        String answer = languageModelClient.answer(groundedPrompt);
        ChatMessage assistantMessage = messageRepository.save(new ChatMessage(session, ChatMessageRole.ASSISTANT, answer));

        // P0.4: Parse citation markers from generated answer output and validate indices against available chunks
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

    private String excerpt(String content) { return truncate(content, 500); }
    private String truncate(String content, int max) { return content.length() <= max ? content : content.substring(0, max - 1).trim() + "…"; }
}
