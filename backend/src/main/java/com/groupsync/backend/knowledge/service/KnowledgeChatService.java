package com.groupsync.backend.knowledge.service;

import java.util.*;
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
    private static final double MIN_RELEVANCE = 0.25d;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final CitationRepository citationRepository;
    private final DocumentChunkRepository chunkRepository;
    private final ResourceRepository resourceRepository;
    private final UserAccountRepository userRepository;
    private final SemanticRetrievalService retrievalService;
    private final LanguageModelClient languageModelClient;
    private final KnowledgeWorkspaceService workspaceService;

    public KnowledgeChatService(ChatSessionRepository sessionRepository, ChatMessageRepository messageRepository,
            CitationRepository citationRepository, DocumentChunkRepository chunkRepository, ResourceRepository resourceRepository,
            UserAccountRepository userRepository, SemanticRetrievalService retrievalService, LanguageModelClient languageModelClient, KnowledgeWorkspaceService workspaceService) {
        this.sessionRepository = sessionRepository; this.messageRepository = messageRepository; this.citationRepository = citationRepository;
        this.chunkRepository = chunkRepository; this.resourceRepository = resourceRepository; this.userRepository = userRepository;
        this.retrievalService = retrievalService; this.languageModelClient = languageModelClient; this.workspaceService = workspaceService;
    }

    @Transactional
    public AskKnowledgeResponse ask(Long ownerId, AskKnowledgeRequest request) {
        ChatSession session = resolveSession(ownerId, request);
        messageRepository.save(new ChatMessage(session, ChatMessageRole.USER, request.question().trim()));
        List<Long> selectedIds = session.getResources().stream().map(Resource::getId).toList();
        Long thisResourceId = session.getScopeType() == RetrievalScope.THIS_RESOURCE
                ? selectedIds.stream().findFirst().orElse(null) : null;
        List<RetrievedChunk> chunks = retrievalService.retrieve(ownerId, request.question(), session.getScopeType(),
                thisResourceId, selectedIds, session.getCollectionId());
        if (chunks.isEmpty() || chunks.getFirst().distance() > 1 - MIN_RELEVANCE
                || (!hasLexicalAnchor(request.question(), chunks.getFirst()) && chunks.getFirst().distance() > 0.25d)) {
            messageRepository.save(new ChatMessage(session, ChatMessageRole.ASSISTANT, INSUFFICIENT_CONTEXT));
            return new AskKnowledgeResponse(session.getId(), INSUFFICIENT_CONTEXT, false, List.of());
        }

        String answer = languageModelClient.answer(GroundedPromptBuilder.build(request.question(), chunks));
        ChatMessage assistantMessage = messageRepository.save(new ChatMessage(session, ChatMessageRole.ASSISTANT, answer));
        Map<Long, DocumentChunk> persistedChunks = new HashMap<>();
        chunkRepository.findAllById(chunks.stream().map(RetrievedChunk::chunkId).toList())
                .forEach(chunk -> persistedChunks.put(chunk.getId(), chunk));
        List<CitationResponse> citations = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            RetrievedChunk chunk = chunks.get(index);
            DocumentChunk persisted = persistedChunks.get(chunk.chunkId());
            if (persisted == null) continue;
            citationRepository.save(new Citation(assistantMessage, persisted, index + 1, 1 - chunk.distance(), excerpt(chunk.content())));
            citations.add(new CitationResponse(chunk.chunkId(), chunk.resourceId(), chunk.resourceTitle(), chunk.pageNumber(),
                    chunk.section(), index + 1, 1 - chunk.distance(), excerpt(chunk.content())));
        }
        return new AskKnowledgeResponse(session.getId(), answer, true, citations);
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
    private boolean hasLexicalAnchor(String question, RetrievedChunk chunk) {
        Set<String> ignored = Set.of("what", "when", "where", "which", "who", "why", "how", "does", "did", "the", "is", "are", "was", "this", "that", "from", "with", "for", "and", "or", "là", "gì", "bao", "nhiêu", "nào", "của", "ở", "trong", "từ", "và", "là");
        Set<String> evidence = tokens(chunk.resourceTitle() + " " + chunk.content(), ignored);
        return tokens(question, ignored).stream().anyMatch(evidence::contains);
    }
    private Set<String> tokens(String value, Set<String> ignored) {
        Set<String> result = new HashSet<>();
        for (String token : value.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (token.length() >= 3 && !ignored.contains(token)) result.add(token);
        }
        return result;
    }
}
