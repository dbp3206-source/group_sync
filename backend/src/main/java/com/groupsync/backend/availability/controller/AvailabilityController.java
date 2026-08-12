package com.groupsync.backend.availability.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.availability.dto.AvailabilityCandidateResponse;
import com.groupsync.backend.availability.dto.AvailabilityRequest;
import com.groupsync.backend.availability.service.AvailabilityService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {
    private final AvailabilityService availabilityService;
    public AvailabilityController(AvailabilityService availabilityService) { this.availabilityService = availabilityService; }
    @PostMapping("/groups/{groupId}/search")
    public List<AvailabilityCandidateResponse> search(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long groupId, @Valid @RequestBody AvailabilityRequest request) { return availabilityService.find(actor, groupId, request); }
}
