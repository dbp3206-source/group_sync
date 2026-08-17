package com.groupsync.backend.study.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.study.dto.*;
import com.groupsync.backend.study.service.StudyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/study")
public class StudyController {
    private final StudyService studyService;
    public StudyController(StudyService studyService) { this.studyService = studyService; }

    @GetMapping("/sessions") public List<StudySessionResponse> list(@AuthenticationPrincipal AuthenticatedUser actor, @RequestParam Long groupId) { return studyService.list(actor, groupId); }
    @GetMapping("/sessions/{sessionId}") public StudySessionResponse get(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long sessionId) { return studyService.get(actor, sessionId); }
    @PostMapping("/groups/{groupId}/sessions") public ResponseEntity<StudySessionResponse> create(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long groupId, @Valid @RequestBody CreateStudySessionRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(studyService.create(actor, groupId, request)); }
    @PostMapping("/sessions/{sessionId}/join") public StudySessionResponse join(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long sessionId) { return studyService.join(actor, sessionId); }
    @DeleteMapping("/sessions/{sessionId}/participants/me") public ResponseEntity<Void> leave(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long sessionId) { studyService.leave(actor, sessionId); return ResponseEntity.noContent().build(); }
    @PostMapping("/sessions/{sessionId}/confirm") public StudySessionResponse confirm(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long sessionId) { return studyService.confirm(actor, sessionId); }
    @PatchMapping("/sessions/{sessionId}/schedule") public StudySessionResponse reschedule(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long sessionId, @Valid @RequestBody RescheduleStudySessionRequest request) { return studyService.reschedule(actor, sessionId, request); }
    @PostMapping("/sessions/{sessionId}/cancel") public StudySessionResponse cancel(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long sessionId) { return studyService.cancel(actor, sessionId); }
    @PostMapping("/sessions/{sessionId}/complete") public StudySessionResponse complete(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long sessionId) { return studyService.complete(actor, sessionId); }
    @PostMapping("/sessions/{sessionId}/materials") public StudySessionResponse addMaterial(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long sessionId, @Valid @RequestBody CreateStudyMaterialRequest request) { return studyService.addMaterial(actor, sessionId, request); }
    @PostMapping("/sessions/{sessionId}/goals") public StudySessionResponse addGoal(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long sessionId, @Valid @RequestBody CreateStudyGoalRequest request) { return studyService.addGoal(actor, sessionId, request); }
    @PostMapping("/sessions/{sessionId}/goals/{goalId}/toggle") public StudySessionResponse toggleGoal(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long sessionId, @PathVariable Long goalId) { return studyService.toggleGoal(actor, sessionId, goalId); }
    @PatchMapping("/sessions/{sessionId}/participants/{userId}/attendance") public StudySessionResponse attendance(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long sessionId, @PathVariable Long userId, @Valid @RequestBody AttendanceRequest request) { return studyService.markAttendance(actor, sessionId, userId, request); }
}
