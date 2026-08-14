package com.groupsync.backend.knowledge.dto;

import java.util.List;

public record AskKnowledgeResponse(Long sessionId, String answer, boolean grounded, List<CitationResponse> citations) { }
