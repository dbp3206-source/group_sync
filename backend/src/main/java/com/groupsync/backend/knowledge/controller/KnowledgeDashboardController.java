package com.groupsync.backend.knowledge.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.knowledge.dto.FocusNextResponse;
import com.groupsync.backend.knowledge.dto.InsightOverviewResponse;
import com.groupsync.backend.knowledge.service.KnowledgeDashboardService;

@RestController
@RequestMapping("/api")
public class KnowledgeDashboardController {
    private final KnowledgeDashboardService dashboardService;
    public KnowledgeDashboardController(KnowledgeDashboardService dashboardService) { this.dashboardService = dashboardService; }
    @GetMapping("/focus/next")
    public ResponseEntity<FocusNextResponse> focusNext(@AuthenticationPrincipal AuthenticatedUser user) {
        return dashboardService.focusNext(user.getId()).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }
    @GetMapping("/insights/overview")
    public InsightOverviewResponse overview(@AuthenticationPrincipal AuthenticatedUser user) {
        return dashboardService.overview(user.getId());
    }
}
