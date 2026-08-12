package com.groupsync.backend.dashboard.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.dashboard.dto.DashboardResponse;
import com.groupsync.backend.dashboard.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;
    public DashboardController(DashboardService service) { this.service = service; }
    @GetMapping("/groups/{groupId}") public DashboardResponse group(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long groupId, @RequestParam Long seasonId) { return service.get(actor, groupId, seasonId); }
}
