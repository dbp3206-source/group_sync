package com.groupsync.backend.knowledge.service;

import org.springframework.stereotype.Service;
import com.groupsync.backend.knowledge.dto.AskPreflightResponse;
import com.groupsync.backend.knowledge.dto.AskKnowledgeRequest;
import com.groupsync.backend.knowledge.rag.RetrievalScope;

@Service
public class AskPreflightService {
    public AskPreflightResponse estimate(AskKnowledgeRequest request) {
        int questionChars = request.question() == null ? 0 : request.question().trim().length();
        int contextChars = switch (request.scope() == null ? RetrievalScope.LIBRARY : request.scope()) {
            case THIS_RESOURCE -> 2600;
            case SELECTED_RESOURCES -> Math.max(2600, Math.min(9000, 1500 * Math.max(1, request.resourceIds() == null ? 0 : request.resourceIds().size())));
            case COLLECTION -> 4200;
            case LIBRARY -> 6000;
        };
        int inputTokens = Math.max(1, (int) Math.ceil((questionChars + contextChars) / 4.0));
        boolean heavy = questionChars > 600 || inputTokens >= 1400;
        return new AskPreflightResponse(heavy, inputTokens, contextChars, "LOCAL_HEURISTIC_FROM_SCOPE_AND_TEXT_LENGTH", false, "UNKNOWN", null);
    }
}
