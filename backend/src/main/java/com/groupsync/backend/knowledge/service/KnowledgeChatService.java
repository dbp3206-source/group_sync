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
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final CitationRepository citationRepository;
    private final DocumentChunkRepository chunkRepository;
    private final ResourceRepository resourceRepository;
    private final UserAccountRepository userRepository;
    private final SemanticRetrievalService retrievalService;
    private final LanguageModelClient languageModelClient;

    public KnowledgeChatService(ChatSessionRepository sessionRepository, ChatMessageRepository messageRepository,
            CitationRepository citationRepository, DocumentChunkRepository chunkRepository, ResourceRepository resourceRepository,
            UserAccountRepository userRepository, SemanticRetrievalService retrievalService, LanguageModelClient languageModelClient) {
        this.sessionRepository = sessionRepository; this.messageRepository = messageRepository; this.citationRepository = citationRepository;
        this.chunkRepository = chunkRepository; this.resourceRepository = resourceRepository; this.userRepository = userRepository;
        this.retrievalService = retrievalService; this.languageModelClient = languageModelClient;
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
        if (chunks.isEmpty()) {
            messageRepository.save(new ChatMessage(session, ChatMessageRole.ASSISTANT, INSUFFICIENT_CONTEXT));
            return new AskKnowledgeResponse(session.getId(), INSUFFICIENT_CONTEXT, false, List.of());
        }

        String answer = languageModelClient.answer(prompt(request.question(), chunks));
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

    private ChatSession resolveSession(Long ownerId, AskKnowledgeRequest request) {
        if (request.sessionId() != null) {
            return sessionRepository.findByIdAndOwnerId(request.sessionId(), ownerId)
                    .orElseThrow(() -> new NotFoundException("Chat session not found."));
        }
        if (request.scope() == null) throw new IllegalArgumentException("A retrieval scope is required for a new chat.");
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

    private String prompt(String question, List<RetrievedChunk> chunks) {
        StringBuilder prompt = new StringBuilder("You are KnowledgeOS. Answer only from the supplied evidence. ")
                .append("If the evidence is insufficient, say so plainly. Cite sources using [1], [2], and so on.\n\nEvidence:\n");
        for (int index = 0; index < chunks.size(); index++) {
            RetrievedChunk chunk = chunks.get(index);
            prompt.append('[').append(index + 1).append("] ").append(chunk.resourceTitle()).append(": ")
                    .append(chunk.content()).append("\n\n");
        }
        return prompt.append("Question: ").append(question.trim()).toString();
    }

    private String excerpt(String content) { return truncate(content, 500); }
    private String truncate(String content, int max) { return content.length() <= max ? content : content.substring(0, max - 1).trim() + "…"; }
}
