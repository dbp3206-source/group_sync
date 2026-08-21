package com.groupsync.backend.knowledge.controller;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.knowledge.dto.AskKnowledgeRequest;
import com.groupsync.backend.knowledge.dto.AskKnowledgeResponse;
import com.groupsync.backend.knowledge.dto.ChatSessionDetailResponse;
import com.groupsync.backend.knowledge.dto.ChatSessionSummaryResponse;
import com.groupsync.backend.knowledge.dto.AskAttemptResponse;
import com.groupsync.backend.knowledge.dto.AskPreflightResponse;
import com.groupsync.backend.knowledge.dto.AiUsageResponse;
import com.groupsync.backend.knowledge.service.KnowledgeChatService;
import com.groupsync.backend.knowledge.service.AskAttemptService;
import com.groupsync.backend.knowledge.service.AskPreflightService;

@RestController
@RequestMapping("/api/ask")
public class KnowledgeChatController {

    private final KnowledgeChatService chatService;
    private final AskAttemptService attemptService;
    private final AskPreflightService preflightService;

    public KnowledgeChatController(KnowledgeChatService chatService, AskAttemptService attemptService, AskPreflightService preflightService) {
        this.chatService = chatService; this.attemptService = attemptService; this.preflightService = preflightService;
    }

    @PostMapping("/preflight")
    public AskPreflightResponse preflight(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody AskKnowledgeRequest request) {
        return preflightService.estimate(user.getId(), request);
    }

    @PostMapping("/attempts")
    public AskAttemptResponse startAttempt(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody AskKnowledgeRequest request) {
        return attemptService.start(user.getId(), request);
    }

    @PostMapping("/attempts/{attemptId}/retry")
    public AskAttemptResponse retryAttempt(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long attemptId) {
        return attemptService.retry(user.getId(), attemptId);
    }

    @GetMapping("/attempts/{attemptId}")
    public AskAttemptResponse attemptStatus(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long attemptId) {
        return attemptService.status(user.getId(), attemptId);
    }

    @GetMapping(value = "/attempts/{attemptId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter attemptEvents(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long attemptId,
                                    @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        return attemptService.subscribe(user.getId(), attemptId, lastEventId);
    }

    @GetMapping("/usage")
    public AiUsageResponse usage(@AuthenticationPrincipal AuthenticatedUser user) {
        return attemptService.usage(user.getId());
    }

    @PostMapping
    public AskKnowledgeResponse ask(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AskKnowledgeRequest request) {
        return chatService.ask(user.getId(), request);
    }

    @GetMapping("/sessions")
    public List<ChatSessionSummaryResponse> sessions(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return chatService.sessions(user.getId());
    }

    @GetMapping("/sessions/{sessionId}")
    public ChatSessionDetailResponse session(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long sessionId) {
        return chatService.session(user.getId(), sessionId);
    }
}
