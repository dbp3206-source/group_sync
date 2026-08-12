package com.groupsync.backend.notification.controller;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.badminton.dto.BadmintonResponses.NotificationResponse;
import com.groupsync.backend.notification.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }
    @GetMapping public List<NotificationResponse> list(@AuthenticationPrincipal AuthenticatedUser actor) { return service.list(actor); }
    @PatchMapping("/{id}/read") public void markRead(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { service.markRead(actor, id); }
}
