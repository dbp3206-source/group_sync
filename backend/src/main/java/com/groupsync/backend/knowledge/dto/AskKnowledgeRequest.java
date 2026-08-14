package com.groupsync.backend.knowledge.dto;

import java.util.List;
import com.groupsync.backend.knowledge.rag.RetrievalScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AskKnowledgeRequest(Long sessionId, @NotBlank String question, RetrievalScope scope,
        Long resourceId, List<Long> resourceIds, Long collectionId, String sessionTitle) { }
