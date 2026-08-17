package com.groupsync.backend.knowledge.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.knowledge.dto.AskKnowledgeRequest;
import com.groupsync.backend.knowledge.dto.AskKnowledgeResponse;
import com.groupsync.backend.knowledge.service.KnowledgeChatService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ask")
public class KnowledgeChatController {
    private final KnowledgeChatService chatService;
    public KnowledgeChatController(KnowledgeChatService chatService) { this.chatService = chatService; }
    @PostMapping
    public AskKnowledgeResponse ask(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody AskKnowledgeRequest request) {
        return chatService.ask(user.getId(), request);
    }
    @GetMapping("/sessions") public List<Map<String,Object>> sessions(@AuthenticationPrincipal AuthenticatedUser user) { return chatService.sessions(user.getId()); }
    @GetMapping("/sessions/{sessionId}") public Map<String,Object> session(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long sessionId) { return chatService.session(user.getId(), sessionId); }
}
