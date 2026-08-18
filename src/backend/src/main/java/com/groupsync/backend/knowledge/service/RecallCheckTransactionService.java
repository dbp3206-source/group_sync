package com.groupsync.backend.knowledge.service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupsync.backend.knowledge.dto.FocusStudioDto.*;
import com.groupsync.backend.knowledge.model.*;
import com.groupsync.backend.knowledge.repository.*;
import com.groupsync.backend.shared.exception.BadRequestException;
import com.groupsync.backend.shared.exception.NotFoundException;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

/**
 * Dedicated transactional boundary service for Recall Check / Quiz persistence operations.
 * Isolates short DB transactions from external Gemini LLM calls and returns immutable DTOs
 * to prevent LazyInitializationException outside transaction boundaries.
 */
@Service
public class RecallCheckTransactionService {

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private final StudyTopicRepository topicRepository;
    private final TopicConceptRepository conceptRepository;
    private final DocumentChunkRepository chunkRepository;
    private final QuizAttemptRepository attemptRepository;
    private final UserAccountRepository userRepository;

    public RecallCheckTransactionService(
            StudyTopicRepository topicRepository,
            TopicConceptRepository conceptRepository,
            DocumentChunkRepository chunkRepository,
            QuizAttemptRepository attemptRepository,
            UserAccountRepository userRepository) {
        this.topicRepository = topicRepository;
        this.conceptRepository = conceptRepository;
        this.chunkRepository = chunkRepository;
        this.attemptRepository = attemptRepository;
        this.userRepository = userRepository;
    }

    public record QuizEvidenceChunk(
            Long chunkId,
            Long resourceId,
            String resourceTitle,
            String content,
            Integer pageNumber,
            String sectionTitle
    ) {}

    public record QuizEvidence(
            Long topicId,
            String topicTitle,
            Long conceptId,
            String conceptTitle,
            List<QuizEvidenceChunk> allowedChunks,
            Map<Long, QuizEvidenceChunk> allowedChunkMap
    ) {}

    public record ValidatedQuizQuestion(
            String question,
            List<String> options,
            int correctOption,
            String explanation,
            Long sourceChunkId,
            String sourceSnippet
    ) {}

    @Transactional(readOnly = true)
    public QuizEvidence prepareEvidence(Long ownerId, Long topicId, Long conceptId) {
        userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("User not found."));
        StudyTopic topic = topicRepository.findByIdAndOwnerId(topicId, ownerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề học tập."));

        TopicConcept targetConcept = null;
        if (conceptId != null && conceptId > 0) {
            targetConcept = conceptRepository.findByIdAndTopicId(conceptId, topicId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy khái niệm."));
        }

        List<QuizEvidenceChunk> evidenceChunks = new ArrayList<>();
        if (targetConcept != null && !targetConcept.getSourceChunks().isEmpty()) {
            for (DocumentChunk chunk : targetConcept.getSourceChunks()) {
                evidenceChunks.add(toEvidenceChunk(chunk));
            }
        } else {
            List<Resource> readyResources = topic.getResources().stream()
                    .filter(r -> r.getProcessingStatus() == ResourceProcessingStatus.READY)
                    .toList();
            for (Resource r : readyResources) {
                for (DocumentChunk chunk : chunkRepository.findByResourceIdOrderByChunkIndex(r.getId())) {
                    evidenceChunks.add(toEvidenceChunk(chunk));
                }
            }
        }

        if (evidenceChunks.isEmpty()) {
            throw new BadRequestException("Chưa có tài liệu hoặc phân đoạn tri thức khả dụng trong chủ đề này để tạo bài kiểm tra ghi nhớ.");
        }

        Map<Long, QuizEvidenceChunk> chunkMap = evidenceChunks.stream()
                .collect(Collectors.toMap(QuizEvidenceChunk::chunkId, c -> c, (a, b) -> a));

        return new QuizEvidence(
                topic.getId(),
                topic.getTitle(),
                targetConcept != null ? targetConcept.getId() : null,
                targetConcept != null ? targetConcept.getTitle() : null,
                evidenceChunks,
                chunkMap
        );
    }

    private QuizEvidenceChunk toEvidenceChunk(DocumentChunk chunk) {
        Resource res = chunk.getResource();
        return new QuizEvidenceChunk(
                chunk.getId(),
                res != null ? res.getId() : null,
                res != null ? res.getTitle() : null,
                chunk.getContent(),
                chunk.getPageNumber(),
                chunk.getSection()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public QuizAttemptResponse persistQuizAttempt(Long ownerId, Long topicId, Long conceptId, Set<Long> allowedChunkIds, List<ValidatedQuizQuestion> validQuestions) {
        if (validQuestions == null || validQuestions.isEmpty()) {
            throw new BadRequestException("Không thể tạo bài kiểm tra ghi nhớ: không có câu hỏi hợp lệ nào được sinh ra.");
        }

        UserAccount owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("User not found."));
        StudyTopic topic = topicRepository.findByIdAndOwnerId(topicId, ownerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề học tập."));

        TopicConcept targetConcept = null;
        if (conceptId != null && conceptId > 0) {
            targetConcept = conceptRepository.findByIdAndTopicId(conceptId, topicId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy khái niệm."));
        }

        // Exact totalQuestions equals the valid accepted question count
        QuizAttempt attempt = new QuizAttempt(topic, owner, targetConcept, validQuestions.size());

        for (ValidatedQuizQuestion q : validQuestions) {
            if (allowedChunkIds != null && !allowedChunkIds.contains(q.sourceChunkId())) {
                throw new BadRequestException("Phân đoạn nguồn không thuộc danh mục cho phép: " + q.sourceChunkId());
            }

            DocumentChunk sourceChunk = chunkRepository.findById(q.sourceChunkId())
                    .orElseThrow(() -> new BadRequestException("Phân đoạn nguồn không tồn tại: " + q.sourceChunkId()));
            Resource sourceRes = sourceChunk.getResource();

            String optionsJson;
            try {
                optionsJson = jsonMapper.writeValueAsString(q.options());
            } catch (Exception e) {
                throw new IllegalStateException("Không thể chuyển đổi danh sách lựa chọn thành JSON.", e);
            }

            QuizItem item = new QuizItem(
                    attempt,
                    targetConcept,
                    q.question(),
                    optionsJson,
                    q.correctOption(),
                    q.explanation(),
                    sourceRes,
                    sourceChunk,
                    q.sourceSnippet()
            );
            attempt.addItem(item);
        }

        QuizAttempt saved = attemptRepository.save(attempt);
        return mapToQuizAttemptResponse(saved, false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SubmitQuizAnswersResponse submitAnswers(Long ownerId, Long attemptId, SubmitQuizAnswersRequest request) {
        QuizAttempt attempt = attemptRepository.findByIdAndOwnerId(attemptId, ownerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra ghi nhớ."));

        Map<Long, Integer> answers = request.answers() != null ? request.answers() : Collections.emptyMap();

        int score = 0;
        Set<TopicConcept> reviewNeededConcepts = new HashSet<>();
        Set<TopicConcept> checkedConcepts = new HashSet<>();

        for (QuizItem item : attempt.getItems()) {
            Integer userAns = answers.get(item.getId());
            item.setUserAnswer(userAns);

            boolean isCorrect = item.isCorrect();
            if (isCorrect) {
                score++;
                if (item.getConcept() != null) {
                    checkedConcepts.add(item.getConcept());
                }
            } else {
                if (item.getConcept() != null) {
                    item.getConcept().markReviewNeeded();
                    conceptRepository.save(item.getConcept());
                    reviewNeededConcepts.add(item.getConcept());
                }
            }
        }

        // For concepts where all questions were correct and none failed
        for (TopicConcept c : checkedConcepts) {
            if (!reviewNeededConcepts.contains(c)) {
                c.markChecked();
                conceptRepository.save(c);
            }
        }

        attempt.setScoreCorrect(score);
        attemptRepository.save(attempt);

        List<QuizQuestionDto> questionResults = mapToQuestionDtos(attempt.getItems(), true);

        List<TopicConceptDto> conceptsNeedingReview = reviewNeededConcepts.stream().map(c ->
                new TopicConceptDto(c.getId(), c.getTitle(), c.getSummary(), c.getWhyItMatters(),
                        c.getStudyStatus(), c.getPosition(), Collections.emptyList())
        ).toList();

        double percentage = attempt.getTotalQuestions() > 0
                ? Math.round(((double) score / attempt.getTotalQuestions()) * 100.0)
                : 0;

        return new SubmitQuizAnswersResponse(attempt.getId(), score, attempt.getTotalQuestions(),
                percentage, questionResults, conceptsNeedingReview);
    }

    public QuizAttemptResponse mapToQuizAttemptResponse(QuizAttempt attempt, boolean revealAnswers) {
        List<QuizQuestionDto> dtos = mapToQuestionDtos(attempt.getItems(), revealAnswers);
        return new QuizAttemptResponse(
                attempt.getId(),
                attempt.getTopic().getId(),
                attempt.getConcept() != null ? attempt.getConcept().getId() : null,
                attempt.getScoreCorrect(),
                attempt.getTotalQuestions(),
                revealAnswers,
                dtos,
                attempt.getCreatedAt()
        );
    }

    public List<QuizQuestionDto> mapToQuestionDtos(List<QuizItem> items, boolean revealAnswers) {
        return items.stream().map(item -> {
            List<String> options = new ArrayList<>();
            try {
                var optsNode = jsonMapper.readTree(item.getOptionsJson());
                if (optsNode.isArray()) {
                    for (var n : optsNode) options.add(n.asText());
                }
            } catch (Exception e) {
                options = List.of();
            }

            Resource res = item.getSourceResource();
            DocumentChunk chk = item.getSourceChunk();

            return new QuizQuestionDto(
                    item.getId(),
                    item.getConcept() != null ? item.getConcept().getId() : null,
                    item.getQuestion(),
                    options,
                    revealAnswers ? item.getCorrectOption() : null,
                    item.getUserAnswer(),
                    revealAnswers ? item.getExplanation() : null,
                    res != null ? res.getId() : null,
                    res != null ? res.getTitle() : null,
                    chk != null ? chk.getId() : null,
                    revealAnswers ? item.getSourceSnippet() : null
            );
        }).toList();
    }
}
