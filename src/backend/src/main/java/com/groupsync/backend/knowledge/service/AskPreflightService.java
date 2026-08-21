package com.groupsync.backend.knowledge.service;

import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.groupsync.backend.knowledge.dto.AskPreflightResponse;
import com.groupsync.backend.knowledge.dto.AskKnowledgeRequest;
import com.groupsync.backend.knowledge.dto.LocalUsageStatus;
import com.groupsync.backend.knowledge.rag.RetrievalScope;
import com.groupsync.backend.knowledge.model.AiUsageEvent;
import com.groupsync.backend.knowledge.repository.AiUsageEventRepository;

@Service
public class AskPreflightService {
    private final AiUsageEventRepository usageRepository;
    private final LocalUsageClassifier usageClassifier;

    public AskPreflightService() {
        this.usageRepository = null;
        this.usageClassifier = new LocalUsageClassifier();
    }

    @Autowired
    public AskPreflightService(AiUsageEventRepository usageRepository, LocalUsageClassifier usageClassifier) {
        this.usageRepository = usageRepository;
        this.usageClassifier = usageClassifier;
    }

    public AskPreflightResponse estimate(AskKnowledgeRequest request) {
        return estimate(null, request);
    }

    public AskPreflightResponse estimate(Long ownerId, AskKnowledgeRequest request) {
        int questionChars = request.question() == null ? 0 : request.question().trim().length();
        int contextChars = switch (request.scope() == null ? RetrievalScope.LIBRARY : request.scope()) {
            case THIS_RESOURCE -> 2600;
            case SELECTED_RESOURCES -> Math.max(2600, Math.min(9000, 1500 * Math.max(1, request.resourceIds() == null ? 0 : request.resourceIds().size())));
            case COLLECTION -> 4200;
            case LIBRARY -> 6000;
        };
        int inputTokens = Math.max(1, (int) Math.ceil((questionChars + contextChars) / 4.0));
        boolean heavy = questionChars > 600 || inputTokens >= 1400;
        List<AiUsageEvent> events = ownerId == null || usageRepository == null
                ? List.of()
                : usageRepository.findByOwnerIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        ownerId, Instant.now().minus(java.time.Duration.ofHours(24)));
        LocalUsageStatus status = ownerId == null ? LocalUsageStatus.UNKNOWN : usageClassifier.classify(events, Instant.now());
        String warningLevel = warningLevel(heavy, status);
        return new AskPreflightResponse(heavy, inputTokens, contextChars,
                "LOCAL_HEURISTIC_FROM_SCOPE_AND_TEXT_LENGTH", false, "UNKNOWN", null,
                status, LocalUsageClassifier.WINDOW, warningLevel);
    }

    private String warningLevel(boolean heavy, LocalUsageStatus status) {
        if (!heavy) return "NONE";
        return switch (status) {
            case RATE_LIMITED, LOW -> "STRONG";
            case COMFORTABLE -> "SOFT";
            case MODERATE, UNKNOWN -> "STANDARD";
        };
    }
}
