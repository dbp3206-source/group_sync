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
}
