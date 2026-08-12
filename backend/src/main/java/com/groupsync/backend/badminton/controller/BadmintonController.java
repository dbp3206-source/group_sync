package com.groupsync.backend.badminton.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.badminton.dto.AssignResponsibilityRequest;
import com.groupsync.backend.badminton.dto.BadmintonResponses;
import com.groupsync.backend.badminton.dto.CreateCourtRequest;
import com.groupsync.backend.badminton.dto.CreateResponsibilityRequest;
import com.groupsync.backend.badminton.dto.CreateSessionRequest;
import com.groupsync.backend.badminton.dto.CreateSeasonRequest;
import com.groupsync.backend.badminton.dto.CreateVenueRequest;
import com.groupsync.backend.badminton.dto.CheckinResponses;
import com.groupsync.backend.badminton.service.CheckinService;
import com.groupsync.backend.badminton.dto.ProfileRequest;
import com.groupsync.backend.badminton.dto.RescheduleSessionRequest;
import com.groupsync.backend.badminton.service.BadmintonService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/badminton")
public class BadmintonController {
    private final BadmintonService service;
    private final CheckinService checkinService;
    public BadmintonController(BadmintonService service, CheckinService checkinService) { this.service = service; this.checkinService = checkinService; }

    @GetMapping("/groups/{groupId}/seasons") public List<BadmintonResponses.SeasonResponse> seasons(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long groupId) { return service.seasons(actor, groupId); }
    @PostMapping("/groups/{groupId}/seasons") @ResponseStatus(HttpStatus.CREATED) public BadmintonResponses.SeasonResponse createSeason(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long groupId, @Valid @RequestBody CreateSeasonRequest request) { return service.createSeason(actor, groupId, request); }
    @PostMapping("/seasons/{seasonId}/activate") public BadmintonResponses.SeasonResponse activateSeason(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long seasonId) { return service.activateSeason(actor, seasonId); }
    @PostMapping("/seasons/{seasonId}/deactivate") public BadmintonResponses.SeasonResponse deactivateSeason(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long seasonId) { return service.deactivateSeason(actor, seasonId); }
    @GetMapping("/groups/{groupId}/venues") public List<BadmintonResponses.VenueResponse> venues(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long groupId) { return service.venues(actor, groupId); }
    @PostMapping("/groups/{groupId}/venues") @ResponseStatus(HttpStatus.CREATED) public BadmintonResponses.VenueResponse createVenue(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long groupId, @Valid @RequestBody CreateVenueRequest request) { return service.createVenue(actor, groupId, request); }
    @PostMapping("/groups/{groupId}/venues/{venueId}/courts") @ResponseStatus(HttpStatus.CREATED) public BadmintonResponses.CourtResponse createCourt(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long groupId, @PathVariable Long venueId, @Valid @RequestBody CreateCourtRequest request) { return service.createCourt(actor, groupId, venueId, request); }
    @GetMapping("/groups/{groupId}/profile") public BadmintonResponses.ProfileResponse myProfile(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long groupId) { return service.profile(actor, groupId, actor.getId()); }
    @GetMapping("/groups/{groupId}/profile/{userId}") public BadmintonResponses.ProfileResponse profile(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long groupId, @PathVariable Long userId) { return service.profile(actor, groupId, userId); }
    @PutMapping("/groups/{groupId}/profile") public BadmintonResponses.ProfileResponse updateProfile(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long groupId, @Valid @RequestBody ProfileRequest request) { return service.updateProfile(actor, groupId, request); }
    @GetMapping("/groups/{groupId}/sessions") public List<BadmintonResponses.SessionResponse> sessions(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long groupId) { return service.sessions(actor, groupId); }
    @GetMapping("/sessions/{id}") public BadmintonResponses.SessionResponse session(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { return service.session(actor, id); }
    @PostMapping("/groups/{groupId}/sessions") @ResponseStatus(HttpStatus.CREATED) public BadmintonResponses.SessionResponse createSession(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long groupId, @Valid @RequestBody CreateSessionRequest request) { return service.createSession(actor, groupId, request); }
    @PostMapping("/sessions/{id}/open") public BadmintonResponses.SessionResponse open(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { return service.open(actor, id); }
    @PostMapping("/sessions/{id}/confirm") public BadmintonResponses.SessionResponse confirm(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { return service.confirm(actor, id); }
    @PostMapping("/sessions/{id}/start") public BadmintonResponses.SessionResponse start(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { return service.start(actor, id); }
    @PostMapping("/sessions/{id}/cancel") public BadmintonResponses.SessionResponse cancel(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { return service.cancel(actor, id); }
    @PostMapping("/sessions/{id}/complete") public BadmintonResponses.SessionResponse complete(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { return service.complete(actor, id); }
    @PatchMapping("/sessions/{id}/schedule") public BadmintonResponses.SessionResponse reschedule(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id, @Valid @RequestBody RescheduleSessionRequest request) { return service.reschedule(actor, id, request); }
    @PostMapping("/sessions/{id}/registrations") public BadmintonResponses.SessionResponse join(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { return service.join(actor, id); }
    @DeleteMapping("/sessions/{id}/registrations/me") public BadmintonResponses.SessionResponse leave(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { return service.cancelRegistration(actor, id); }
    @PostMapping("/sessions/{id}/registrations/{userId}/check-in") public BadmintonResponses.SessionResponse checkIn(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id, @PathVariable Long userId) { return service.checkIn(actor, id, userId, false); }
    @PostMapping("/sessions/{id}/registrations/{userId}/no-show") public BadmintonResponses.SessionResponse noShow(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id, @PathVariable Long userId) { return service.checkIn(actor, id, userId, true); }
    @PostMapping("/sessions/{id}/responsibilities") @ResponseStatus(HttpStatus.CREATED) public BadmintonResponses.ResponsibilityResponse addResponsibility(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id, @Valid @RequestBody CreateResponsibilityRequest request) { return service.addResponsibility(actor, id, request); }
    @PatchMapping("/sessions/{id}/responsibilities/{responsibilityId}") public BadmintonResponses.ResponsibilityResponse assignResponsibility(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id, @PathVariable Long responsibilityId, @Valid @RequestBody AssignResponsibilityRequest request) { return service.assignResponsibility(actor, id, responsibilityId, request); }
    @DeleteMapping("/responsibilities/{responsibilityId}/assignee") public BadmintonResponses.ResponsibilityResponse unassignResponsibility(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long responsibilityId) { return service.unassignResponsibility(actor, responsibilityId); }
    @PostMapping("/sessions/{id}/responsibilities/assign-round-robin") public List<BadmintonResponses.ResponsibilityResponse> assignResponsibilitiesRoundRobin(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { return service.assignResponsibilitiesRoundRobin(actor, id); }
    @PostMapping("/sessions/{id}/checkin-token") public CheckinResponses.Token generateCheckinToken(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) { return checkinService.generate(actor, id); }
    @PostMapping("/check-in") public CheckinResponses.Result checkIn(@AuthenticationPrincipal AuthenticatedUser actor, @RequestBody CheckinResponses.CheckinRequest request) { return checkinService.checkIn(actor, request.token()); }
}
