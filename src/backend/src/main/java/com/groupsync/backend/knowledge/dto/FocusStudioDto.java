package com.groupsync.backend.knowledge.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class FocusStudioDto {

    public record CreateStudyTopicRequest(
        String title,
        String goal,
        List<Long> resourceIds
    ) {}

    public record UpdateStudyTopicRequest(
        String title,
        String goal,
        String status
    ) {}

    public record ConceptSourceDto(
        Long resourceId,
        String resourceTitle,
        Long chunkId,
        String snippet
    ) {}

    public record TopicConceptDto(
        Long id,
        String title,
        String summary,
        String whyItMatters,
        String studyStatus,
        int position,
        List<ConceptSourceDto> sources
    ) {}

    public record TopicResourceDto(
        Long id,
        String title,
        String resourceType,
        String processingStatus,
        int progressPercent
    ) {}

    public record StudyTopicResponse(
        Long id,
        String title,
        String goal,
        String status,
        int resourceCount,
        int conceptCount,
        int checkedCount,
        int reviewNeededCount,
        int learningCount,
        int notStartedCount,
        Instant createdAt,
        Instant updatedAt
    ) {}

    public record StudyTopicDetailResponse(
        Long id,
        String title,
        String goal,
        String status,
        List<TopicResourceDto> resources,
        List<TopicConceptDto> concepts,
        int checkedCount,
        int reviewNeededCount,
        int learningCount,
        int notStartedCount,
        Instant createdAt,
        Instant updatedAt
    ) {}

    public record QuizQuestionDto(
        Long id,
        Long conceptId,
        String question,
        List<String> options,
        Integer correctOption, // Nullable when taking quiz, revealed upon submission
        Integer userAnswer,
        String explanation,
        Long sourceResourceId,
        String sourceResourceTitle,
        Long sourceChunkId,
        String sourceSnippet
    ) {}

    public record QuizAttemptResponse(
        Long attemptId,
        Long topicId,
        Long conceptId,
        int scoreCorrect,
        int totalQuestions,
        boolean submitted,
        List<QuizQuestionDto> questions,
        Instant createdAt
    ) {}

    public record SubmitQuizAnswersRequest(
        Map<Long, Integer> answers // quizItemId -> selectedOption (0, 1, 2, 3)
    ) {}

    public record SubmitQuizAnswersResponse(
        Long attemptId,
        int scoreCorrect,
        int totalQuestions,
        double percentage,
        List<QuizQuestionDto> results,
        List<TopicConceptDto> conceptsNeedingReview
    ) {}

    public record ReviewQueueItemDto(
        Long conceptId,
        String conceptTitle,
        Long topicId,
        String topicTitle,
        String studyStatus,
        String summary,
        String whyItMatters,
        Instant updatedAt
    ) {}

    public record LearningAreaResponse(
        Long id,
        Long collectionId,
        String title,
        String goal,
        int sourceCount,
        int moduleCount,
        int conceptCount,
        int checkedCount,
        int reviewNeededCount,
        int learningCount,
        int notStartedCount,
        String refreshStatus,
        int newSourceCount,
        int currentVersion,
        String generationFailure,
        Instant updatedAt
    ) {}

    public record ModuleResourceDto(
        Long id,
        String title,
        String resourceType,
        String role
    ) {}

    public record LearningModuleResponse(
        Long id,
        int position,
        String stage,
        String title,
        String objective,
        int conceptCount,
        int checkedCount,
        int reviewNeededCount,
        List<ModuleResourceDto> primaryResources,
        List<ModuleResourceDto> supportingResources,
        List<TopicConceptDto> concepts
    ) {}

    public record LearningAreaDetailResponse(
        LearningAreaResponse area,
        List<TopicResourceDto> resources,
        List<LearningModuleResponse> modules,
        List<TopicConceptDto> retiredConcepts
    ) {}

    public record SourceMapNodeDto(
        String id,
        String type,
        String label,
        Long resourceId,
        Long collectionId,
        Long conceptId
    ) {}

    public record SourceMapEdgeDto(
        String source,
        String target,
        String relationType,
        String reason
    ) {}

    public record LearningAreaSourceMapResponse(
        Long learningAreaId,
        List<SourceMapNodeDto> nodes,
        List<SourceMapEdgeDto> edges,
        int selectedSourceCount,
        boolean bounded
    ) {}

    public record DeepDiveAreaDto(
        Long learningAreaId,
        Long collectionId,
        String title,
        String refreshStatus,
        Long moduleId,
        String moduleTitle,
        int conceptCount,
        int checkedCount,
        int reviewNeededCount
    ) {}
}
