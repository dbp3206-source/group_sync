package com.groupsync.backend.knowledge.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.knowledge.dto.FocusStudioDto.*;
import com.groupsync.backend.knowledge.service.LearningStudioService;
import com.groupsync.backend.knowledge.service.RecallCheckService;

@RestController
@RequestMapping("/api/focus")
public class LearningStudioController {

    private final LearningStudioService studioService;
    private final RecallCheckService recallCheckService;

    public LearningStudioController(LearningStudioService studioService, RecallCheckService recallCheckService) {
        this.studioService = studioService;
        this.recallCheckService = recallCheckService;
    }

    @GetMapping("/topics")
    public List<StudyTopicResponse> listTopics(@AuthenticationPrincipal AuthenticatedUser user) {
        return studioService.listTopics(user.getId());
    }

    @PostMapping("/topics")
    public StudyTopicDetailResponse createTopic(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody CreateStudyTopicRequest request) {
        return studioService.createTopic(user.getId(), request);
    }

    @GetMapping("/topics/{id}")
    public StudyTopicDetailResponse getTopic(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        return studioService.getTopicDetail(user.getId(), id);
    }

    @DeleteMapping("/topics/{id}")
    public ResponseEntity<Void> deleteTopic(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        studioService.deleteTopic(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/topics/{id}/sources/{resourceId}")
    public StudyTopicDetailResponse addSource(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id,
            @PathVariable Long resourceId) {
        return studioService.addSourceToTopic(user.getId(), id, resourceId);
    }

    @DeleteMapping("/topics/{id}/sources/{resourceId}")
    public StudyTopicDetailResponse removeSource(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id,
            @PathVariable Long resourceId) {
        return studioService.removeSourceFromTopic(user.getId(), id, resourceId);
    }

    @PostMapping("/topics/{id}/plan")
    public StudyTopicDetailResponse generatePlan(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        return studioService.generateLearningPlan(user.getId(), id);
    }

    @PatchMapping("/topics/{id}/concepts/{conceptId}/status")
    public TopicConceptDto updateConceptStatus(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id,
            @PathVariable Long conceptId,
            @RequestBody Map<String, String> body) {
        return studioService.updateConceptStatus(user.getId(), id, conceptId, body.get("status"));
    }

    @PostMapping("/topics/{id}/quiz")
    public QuizAttemptResponse generateQuiz(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id,
            @RequestParam(required = false) Long conceptId) {
        return recallCheckService.generateQuiz(user.getId(), id, conceptId);
    }

    @PostMapping("/quiz/attempts/{attemptId}/answers")
    public SubmitQuizAnswersResponse submitQuizAnswers(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long attemptId,
            @RequestBody SubmitQuizAnswersRequest request) {
        return recallCheckService.submitAnswers(user.getId(), attemptId, request);
    }

    @GetMapping("/review-queue")
    public List<ReviewQueueItemDto> getReviewQueue(@AuthenticationPrincipal AuthenticatedUser user) {
        return studioService.getReviewQueue(user.getId());
    }
}
