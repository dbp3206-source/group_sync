package com.groupsync.backend.knowledge.service;

import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupsync.backend.knowledge.dto.FocusStudioDto.*;
import com.groupsync.backend.knowledge.rag.LanguageModelClient;
import com.groupsync.backend.knowledge.service.RecallCheckTransactionService.QuizEvidence;
import com.groupsync.backend.knowledge.service.RecallCheckTransactionService.QuizEvidenceChunk;
import com.groupsync.backend.knowledge.service.RecallCheckTransactionService.ValidatedQuizQuestion;
import com.groupsync.backend.shared.exception.BadRequestException;

@Service
public class RecallCheckService {

    private static final Logger log = LoggerFactory.getLogger(RecallCheckService.class);
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final int TARGET_QUESTION_COUNT = 5;

    private final RecallCheckTransactionService transactionService;
    private final LanguageModelClient languageModelClient;

    public RecallCheckService(
            RecallCheckTransactionService transactionService,
            LanguageModelClient languageModelClient) {
        this.transactionService = transactionService;
        this.languageModelClient = languageModelClient;
    }

    public QuizAttemptResponse generateQuiz(Long ownerId, Long topicId, Long conceptId) {
        // Step 1: Prepare evidence in short read-only transaction (pure DTOs returned)
        QuizEvidence evidence = transactionService.prepareEvidence(ownerId, topicId, conceptId);

        // Step 2: Generate and strictly validate quiz questions with Gemini outside DB transaction
        List<ValidatedQuizQuestion> validQuestions = generateAndValidateQuestions(evidence);

        // Retry once if 0 valid questions were generated
        if (validQuestions.isEmpty()) {
            log.info("Initial Gemini quiz generation produced 0 valid questions. Retrying once...");
            validQuestions = generateAndValidateQuestions(evidence);
        }

        if (validQuestions.isEmpty()) {
            throw new BadRequestException("Không thể tạo bài kiểm tra ghi nhớ hợp lệ từ tài liệu đã chọn. Vui lòng thử lại sau.");
        }

        // Step 3: Persist QuizAttempt and valid QuizItems in short write transaction
        return transactionService.persistQuizAttempt(ownerId, topicId, conceptId, evidence.allowedChunkMap().keySet(), validQuestions);
    }

    public SubmitQuizAnswersResponse submitAnswers(Long ownerId, Long attemptId, SubmitQuizAnswersRequest request) {
        return transactionService.submitAnswers(ownerId, attemptId, request);
    }

    private List<ValidatedQuizQuestion> generateAndValidateQuestions(QuizEvidence evidence) {
        StringBuilder evidenceText = new StringBuilder();
        int limit = Math.min(evidence.allowedChunks().size(), 8);
        for (int i = 0; i < limit; i++) {
            QuizEvidenceChunk c = evidence.allowedChunks().get(i);
            evidenceText.append("[CHUNK_").append(c.chunkId()).append(" from ").append(c.resourceTitle()).append("]:\n")
                    .append(c.content()).append("\n\n");
        }

        String targetName = evidence.conceptTitle() != null ? evidence.conceptTitle() : evidence.topicTitle();

        String prompt = """
                You are a university professor creating an active-recall assessment. Based ONLY on the following source evidence, generate 5 multiple-choice questions testing conceptual recall and understanding for: "%s".
                
                SOURCE EVIDENCE:
                %s
                
                INSTRUCTIONS:
                1. Output ONLY a valid JSON array of 5 objects without markdown code fence blocks.
                2. Each object must have:
                   - "question": Direct, clear question in Vietnamese.
                   - "options": Array of 4 distinct, plausible choices in Vietnamese. All 4 choices must be mutually distinct.
                   - "correctOption": Integer index 0, 1, 2, or 3 of the correct choice.
                   - "explanation": Concise explanation of WHY the correct option is right based on the source evidence.
                   - "sourceChunkId": Exact integer CHUNK ID from the brackets above where the fact is stated.
                3. Ground all answers strictly in the provided evidence.
                """.formatted(targetName, evidenceText.toString());

        String raw = null;
        try {
            raw = languageModelClient.answer(prompt);
        } catch (Exception e) {
            log.warn("Gemini quiz generation call failed: {}", e.getMessage());
            return Collections.emptyList();
        }

        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }

        return parseAndValidateQuestions(raw, evidence.allowedChunkMap());
    }

    public List<ValidatedQuizQuestion> parseAndValidateQuestions(String rawJson, Map<Long, QuizEvidenceChunk> allowedChunkMap) {
        String cleanJson = rawJson.trim();
        if (cleanJson.startsWith("```json")) cleanJson = cleanJson.substring(7);
        if (cleanJson.startsWith("```")) cleanJson = cleanJson.substring(3);
        if (cleanJson.endsWith("```")) cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
        cleanJson = cleanJson.trim();

        List<ValidatedQuizQuestion> result = new ArrayList<>();
        Set<String> seenQuestions = new HashSet<>();

        try {
            JsonNode root = jsonMapper.readTree(cleanJson);
            if (!root.isArray() || root.isEmpty()) {
                return Collections.emptyList();
            }

            for (JsonNode qNode : root) {
                String question = qNode.path("question").asText("").trim();
                JsonNode optsNode = qNode.path("options");
                int correctOpt = qNode.has("correctOption") ? qNode.path("correctOption").asInt(-1) : -1;
                String explanation = qNode.path("explanation").asText("").trim();
                Long sourceChunkId = qNode.has("sourceChunkId") ? qNode.path("sourceChunkId").asLong() : null;

                // 1. Validate question text
                if (question.isBlank() || seenQuestions.contains(question.toLowerCase(Locale.ROOT))) {
                    log.debug("Rejecting quiz question: blank or duplicate '{}'", question);
                    continue;
                }

                // 2. Validate options (must have exactly 4 non-empty options)
                if (!optsNode.isArray() || optsNode.size() != 4) {
                    log.debug("Rejecting quiz question '{}': options size is not 4", question);
                    continue;
                }
                List<String> options = new ArrayList<>();
                boolean allOptsValid = true;
                for (JsonNode opt : optsNode) {
                    String optText = opt.asText("").trim();
                    if (optText.isBlank()) {
                        allOptsValid = false;
                        break;
                    }
                    options.add(optText);
                }
                if (!allOptsValid || options.size() != 4) {
                    log.debug("Rejecting quiz question '{}': contains blank options", question);
                    continue;
                }

                // 2b. Validate option uniqueness (case-insensitive & trimmed comparison)
                Set<String> normalizedOptions = options.stream()
                        .map(String::trim)
                        .map(value -> value.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toSet());
                if (normalizedOptions.size() != 4) {
                    log.warn("Rejecting quiz question '{}': contains duplicate options {}", question, options);
                    continue;
                }

                // 3. Validate correctOption index bounds [0, 3]
                if (correctOpt < 0 || correctOpt > 3) {
                    log.debug("Rejecting quiz question '{}': correctOption {} out of bounds [0, 3]", question, correctOpt);
                    continue;
                }

                // 4. Validate explanation
                if (explanation.isBlank()) {
                    log.debug("Rejecting quiz question '{}': explanation is blank", question);
                    continue;
                }

                // 5. Strict Source Grounding: sourceChunkId MUST belong to the allowed chunk map
                // NEVER silently fallback to chunks.get(0) or fabricate a source chunk.
                if (sourceChunkId == null || !allowedChunkMap.containsKey(sourceChunkId)) {
                    log.warn("Rejecting quiz question '{}': sourceChunkId {} is not in allowed evidence chunk set {}",
                            question, sourceChunkId, allowedChunkMap.keySet());
                    continue;
                }

                QuizEvidenceChunk sourceChunk = allowedChunkMap.get(sourceChunkId);
                String snippet = sourceChunk != null && sourceChunk.content() != null
                        ? (sourceChunk.content().length() > 200 ? sourceChunk.content().substring(0, 200) + "…" : sourceChunk.content())
                        : "";

                seenQuestions.add(question.toLowerCase(Locale.ROOT));
                result.add(new ValidatedQuizQuestion(question, options, correctOpt, explanation, sourceChunkId, snippet));

                if (result.size() >= TARGET_QUESTION_COUNT) {
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse quiz questions JSON: {}", e.getMessage());
            return Collections.emptyList();
        }

        return result;
    }
}
