package com.groupsync.backend.knowledge.service;

import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupsync.backend.knowledge.dto.FocusStudioDto.*;
import com.groupsync.backend.knowledge.model.*;
import com.groupsync.backend.knowledge.rag.LanguageModelClient;
import com.groupsync.backend.knowledge.repository.*;
import com.groupsync.backend.shared.exception.BadRequestException;
import com.groupsync.backend.shared.exception.NotFoundException;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@Service
public class RecallCheckService {

    private static final Logger log = LoggerFactory.getLogger(RecallCheckService.class);
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private final StudyTopicRepository topicRepository;
    private final TopicConceptRepository conceptRepository;
    private final DocumentChunkRepository chunkRepository;
    private final QuizAttemptRepository attemptRepository;
    private final UserAccountRepository userRepository;
    private final LanguageModelClient languageModelClient;

    public RecallCheckService(
            StudyTopicRepository topicRepository,
            TopicConceptRepository conceptRepository,
            DocumentChunkRepository chunkRepository,
            QuizAttemptRepository attemptRepository,
            UserAccountRepository userRepository,
            LanguageModelClient languageModelClient) {
        this.topicRepository = topicRepository;
        this.conceptRepository = conceptRepository;
        this.chunkRepository = chunkRepository;
        this.attemptRepository = attemptRepository;
        this.userRepository = userRepository;
        this.languageModelClient = languageModelClient;
    }

    @Transactional
    public QuizAttemptResponse generateQuiz(Long ownerId, Long topicId, Long conceptId) {
        UserAccount owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("User not found."));
        StudyTopic topic = topicRepository.findByIdAndOwnerId(topicId, ownerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề học tập."));

        TopicConcept targetConcept = null;
        if (conceptId != null && conceptId > 0) {
            targetConcept = conceptRepository.findByIdAndTopicId(conceptId, topicId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy khái niệm."));
        }

        // Collect available chunks from topic
        List<DocumentChunk> chunks = new ArrayList<>();
        if (targetConcept != null && !targetConcept.getSourceChunks().isEmpty()) {
            chunks.addAll(targetConcept.getSourceChunks());
        } else {
            List<Resource> readyResources = topic.getResources().stream()
                    .filter(r -> r.getProcessingStatus() == ResourceProcessingStatus.READY)
                    .toList();
            for (Resource r : readyResources) {
                chunks.addAll(chunkRepository.findByResourceIdOrderByChunkIndex(r.getId()));
            }
        }

        if (chunks.isEmpty()) {
            throw new BadRequestException("Chưa có tài liệu hoặc phân đoạn tri thức khả dụng trong chủ đề này để tạo bài kiểm tra ghi nhớ.");
        }

        // Create initial QuizAttempt
        QuizAttempt attempt = new QuizAttempt(topic, owner, targetConcept, 5);

        boolean generatedByAi = false;
        try {
            generatedByAi = generateQuestionsWithAi(attempt, targetConcept, chunks);
        } catch (Exception e) {
            log.warn("Gemini quiz generation failed, falling back to deterministic questions: {}", e.getMessage());
        }

        if (!generatedByAi || attempt.getItems().isEmpty()) {
            generateQuestionsDeterministically(attempt, targetConcept, chunks);
        }

        QuizAttempt saved = attemptRepository.save(attempt);

        return mapToQuizAttemptResponse(saved, false);
    }

    private boolean generateQuestionsWithAi(QuizAttempt attempt, TopicConcept concept, List<DocumentChunk> chunks) {
        StringBuilder evidence = new StringBuilder();
        int limit = Math.min(chunks.size(), 8);
        for (int i = 0; i < limit; i++) {
            DocumentChunk c = chunks.get(i);
            evidence.append("[CHUNK_").append(c.getId()).append(" from ").append(c.getResource().getTitle()).append("]:\n")
                    .append(c.getContent()).append("\n\n");
        }

        String targetName = concept != null ? concept.getTitle() : attempt.getTopic().getTitle();

        String prompt = """
                You are a university professor creating an active-recall assessment. Based ONLY on the following source evidence, generate 5 multiple-choice questions testing conceptual recall and understanding for: "%s".
                
                SOURCE EVIDENCE:
                %s
                
                INSTRUCTIONS:
                1. Output ONLY a valid JSON array of 5 objects without markdown code fence blocks.
                2. Each object must have:
                   - "question": Direct, clear question in Vietnamese.
                   - "options": Array of 4 plausible choices in Vietnamese.
                   - "correctOption": Integer index 0, 1, 2, or 3 of the correct choice.
                   - "explanation": Concise explanation of WHY the correct option is right based on the source evidence.
                   - "sourceChunkId": Exact integer CHUNK ID from the brackets above where the fact is stated.
                3. Ground all answers strictly in the provided evidence.
                """.formatted(targetName, evidence.toString());

        String raw = languageModelClient.answer(prompt);
        if (raw == null || raw.isBlank()) return false;

        String cleanJson = raw.trim();
        if (cleanJson.startsWith("```json")) cleanJson = cleanJson.substring(7);
        if (cleanJson.startsWith("```")) cleanJson = cleanJson.substring(3);
        if (cleanJson.endsWith("```")) cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
        cleanJson = cleanJson.trim();

        try {
            JsonNode root = jsonMapper.readTree(cleanJson);
            if (!root.isArray() || root.isEmpty()) return false;

            Map<Long, DocumentChunk> chunkMap = chunks.stream()
                    .collect(Collectors.toMap(DocumentChunk::getId, c -> c, (a, b) -> a));

            for (JsonNode qNode : root) {
                String question = qNode.path("question").asText("").trim();
                JsonNode optsNode = qNode.path("options");
                int correctOpt = qNode.path("correctOption").asInt(0);
                String explanation = qNode.path("explanation").asText("").trim();
                Long sourceChunkId = qNode.path("sourceChunkId").asLong();

                if (question.isBlank() || !optsNode.isArray() || optsNode.size() != 4 || explanation.isBlank()) {
                    continue;
                }

                List<String> options = new ArrayList<>();
                for (JsonNode opt : optsNode) {
                    options.add(opt.asText().trim());
                }

                DocumentChunk sourceChunk = chunkMap.get(sourceChunkId);
                if (sourceChunk == null && !chunks.isEmpty()) {
                    sourceChunk = chunks.get(0);
                }

                Resource res = sourceChunk != null ? sourceChunk.getResource() : null;
                String snippet = sourceChunk != null && sourceChunk.getContent() != null
                        ? (sourceChunk.getContent().length() > 200 ? sourceChunk.getContent().substring(0, 200) + "…" : sourceChunk.getContent())
                        : "";

                QuizItem item = new QuizItem(attempt, concept, question, jsonMapper.writeValueAsString(options),
                        correctOpt, explanation, res, sourceChunk, snippet);
                attempt.getItems().add(item);
            }

            return !attempt.getItems().isEmpty();
        } catch (Exception e) {
            log.warn("Failed to parse quiz AI json: {}", e.getMessage());
            return false;
        }
    }

    private void generateQuestionsDeterministically(QuizAttempt attempt, TopicConcept concept, List<DocumentChunk> chunks) {
        int count = Math.min(chunks.size(), 5);
        for (int i = 0; i < count; i++) {
            DocumentChunk chk = chunks.get(i);
            String text = chk.getContent() != null ? chk.getContent().trim() : "";
            String snippet = text.length() > 180 ? text.substring(0, 180) + "…" : text;

            String question = "Khái niệm hoặc luận điểm cốt lõi nào được đề cập trong trích đoạn từ " + chk.getResource().getTitle() + "?";
            List<String> options = List.of(
                    snippet.length() > 80 ? snippet.substring(0, 80) + "…" : snippet,
                    "Phương pháp kiểm thử hiệu năng mở rộng quy mô lớn",
                    "Cấu hình hệ thống cân bằng tải máy chủ phân tán",
                    "Giao thức mã hóa dữ liệu truyền thông đa luồng"
            );

            String explanation = "Trích xuất trực tiếp từ phân đoạn tài liệu " + chk.getResource().getTitle() + ": " + snippet;

            try {
                QuizItem item = new QuizItem(attempt, concept, question, jsonMapper.writeValueAsString(options),
                        0, explanation, chk.getResource(), chk, snippet);
                attempt.getItems().add(item);
            } catch (Exception ignored) {}
        }
    }

    @Transactional
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

    private QuizAttemptResponse mapToQuizAttemptResponse(QuizAttempt attempt, boolean revealAnswers) {
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

    private List<QuizQuestionDto> mapToQuestionDtos(List<QuizItem> items, boolean revealAnswers) {
        return items.stream().map(item -> {
            List<String> options = new ArrayList<>();
            try {
                JsonNode optsNode = jsonMapper.readTree(item.getOptionsJson());
                if (optsNode.isArray()) {
                    for (JsonNode n : optsNode) options.add(n.asText());
                }
            } catch (Exception e) {
                options = List.of("Lựa chọn A", "Lựa chọn B", "Lựa chọn C", "Lựa chọn D");
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
