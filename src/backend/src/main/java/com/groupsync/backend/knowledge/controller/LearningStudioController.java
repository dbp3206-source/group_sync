package com.groupsync.backend.knowledge.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.knowledge.dto.FocusStudioDto.*;
import com.groupsync.backend.knowledge.dto.UpdateConceptStatusRequest;
import com.groupsync.backend.knowledge.service.LearningStudioService;
import com.groupsync.backend.knowledge.service.CollectionLearningPathService;
import com.groupsync.backend.knowledge.service.RecallCheckService;

@RestController
@RequestMapping("/api/focus")
public class LearningStudioController {

    private final LearningStudioService studioService;
    private final RecallCheckService recallCheckService;
    private final CollectionLearningPathService learningPathService;

    public LearningStudioController(LearningStudioService studioService, RecallCheckService recallCheckService,
                                    CollectionLearningPathService learningPathService) {
        this.studioService = studioService;
        this.recallCheckService = recallCheckService;
        this.learningPathService = learningPathService;
    }

    @GetMapping("/learning-areas")
    public List<LearningAreaResponse> listLearningAreas(@AuthenticationPrincipal AuthenticatedUser user) {
        return learningPathService.list(user.getId());
    }

    @PostMapping("/collections/{collectionId}/initialize")
    public LearningAreaDetailResponse initializeLearningArea(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long collectionId) {
        return learningPathService.initialize(user.getId(), collectionId);
    }

    @GetMapping("/learning-areas/{id}")
    public LearningAreaDetailResponse getLearningArea(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return learningPathService.detail(user.getId(), id);
    }

    @PostMapping("/learning-areas/{id}/build")
    public LearningAreaDetailResponse buildLearningArea(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return learningPathService.buildOrRefresh(user.getId(), id);
    }

    @PostMapping("/learning-areas/{id}/refresh")
    public LearningAreaDetailResponse refreshLearningArea(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return learningPathService.buildOrRefresh(user.getId(), id);
    }

    @GetMapping("/learning-areas/{id}/modules/{moduleId}")
    public LearningModuleResponse getLearningModule(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id, @PathVariable Long moduleId) {
        return learningPathService.module(user.getId(), id, moduleId);
    }

    @GetMapping("/learning-areas/{id}/source-map")
    public LearningAreaSourceMapResponse sourceMap(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
            @RequestParam(required = false) List<Long> resourceIds) {
        return learningPathService.sourceMap(user.getId(), id, resourceIds);
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
            @jakarta.validation.Valid @RequestBody UpdateConceptStatusRequest request) {
        return studioService.updateConceptStatus(user.getId(), id, conceptId, request.status());
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
