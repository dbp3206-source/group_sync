package com.groupsync.backend.knowledge.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.groupsync.backend.knowledge.dto.DocumentUnderstandingResult;
import com.groupsync.backend.knowledge.dto.ResourceUnderstandingResponse;
import com.groupsync.backend.knowledge.rag.GeminiProperties;
import com.groupsync.backend.knowledge.rag.LanguageModelClient;
import com.groupsync.backend.knowledge.service.DocumentUnderstandingTransactionService.StoredUnderstanding;
import com.groupsync.backend.knowledge.service.DocumentUnderstandingTransactionService.UnderstandingSource;
import com.groupsync.backend.knowledge.service.RepresentativeEvidenceSelector.EvidenceChunk;

@Service
public class DocumentUnderstandingService {
    private static final Logger log = LoggerFactory.getLogger(DocumentUnderstandingService.class);
    public static final String UNDERSTANDING_VERSION = "du-v1";
    private final DocumentUnderstandingTransactionService transactions;
    private final RepresentativeEvidenceSelector selector = new RepresentativeEvidenceSelector();
    private final LanguageModelClient languageModel;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;

    public DocumentUnderstandingService(DocumentUnderstandingTransactionService transactions,
                                        LanguageModelClient languageModel, GeminiProperties properties,
                                        ObjectMapper objectMapper) {
        this.transactions = transactions;
        this.languageModel = languageModel;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public record Outcome(String status, DocumentUnderstandingResult result, boolean reused, List<String> warnings) { }

    public ResourceUnderstandingResponse readForWorkspace(Long ownerId, Long resourceId) {
        Optional<DocumentUnderstandingTransactionService.WorkspaceUnderstanding> stored =
                transactions.readForWorkspace(ownerId, resourceId);
        if (stored.isEmpty()) {
            return new ResourceUnderstandingResponse("NOT_AVAILABLE", null, null, List.of(), List.of(), 0, null);
        }
        var value = stored.get();
        return new ResourceUnderstandingResponse(value.status(), value.normalizedTitle(), value.summary(),
                parseList(value.keyIdeasJson()), parseList(value.broadThemesJson()), value.evidenceCount(), value.updatedAt());
    }

    public Outcome understand(Long ownerId, Long resourceId) {
        long started = System.nanoTime();
        UnderstandingSource source = transactions.readSource(ownerId, resourceId);
        String checksum = source.sourceChecksum() == null || source.sourceChecksum().isBlank()
                ? checksum(source.chunks()) : source.sourceChecksum();
        Optional<StoredUnderstanding> current = transactions.findCurrent(ownerId, resourceId, checksum,
                source.chunkingVersion(), properties.chatModel(), UNDERSTANDING_VERSION);
        if (current.isPresent()) {
            return new Outcome("CURRENT", fromStored(current.get()), true, List.of());
        }

        List<EvidenceChunk> evidence = selector.select(source.chunks());
        if (evidence.isEmpty() || evidence.stream().mapToInt(chunk -> chunk.content().length()).sum() < 80) {
            transactions.recordFailure(source, checksum, properties.chatModel(), UNDERSTANDING_VERSION,
                    "UNSUPPORTED", "Resource has too little readable text for grounded understanding.");
            return new Outcome("UNSUPPORTED", null, false, List.of("Not enough readable evidence."));
        }

        try {
            String raw = languageModel.answer(DocumentUnderstandingPromptBuilder.build(
                    source.title(), source.originalFilename(), evidence));
            DocumentUnderstandingResult parsed = objectMapper.readValue(stripCodeFence(raw), DocumentUnderstandingResult.class);
            DocumentUnderstandingResult validated = validate(source, evidence, parsed);
            transactions.saveCurrent(source, checksum, properties.chatModel(), UNDERSTANDING_VERSION, validated);
            log.info("Document understanding completed resourceId={} model={} durationMs={} evidenceCount={}",
                    resourceId, properties.chatModel(), (System.nanoTime() - started) / 1_000_000L,
                    validated.evidenceChunkIds().size());
            return new Outcome("CURRENT", validated, false, List.of());
        } catch (Exception exception) {
            String category = failureCategory(exception);
            transactions.recordFailure(source, checksum, properties.chatModel(), UNDERSTANDING_VERSION,
                    "FAILED", category + ": " + safeMessage(exception));
            log.warn("Document understanding failed resourceId={} model={} category={}", resourceId,
                    properties.chatModel(), category);
            return new Outcome("FAILED", null, false, List.of("Understanding failed: " + category));
        }
    }

    DocumentUnderstandingResult validate(UnderstandingSource source, List<EvidenceChunk> evidence,
                                         DocumentUnderstandingResult input) {
        if (input == null) throw new IllegalArgumentException("Gemini returned an empty understanding.");
        Set<Long> allowed = new HashSet<>(source.chunks().stream().map(EvidenceChunk::id).toList());
        List<Long> verified = input.evidenceChunkIds().stream().filter(allowed::contains).distinct().toList();
        int minimumEvidence = source.chunks().size() > 3 ? 2 : 1;
        if (verified.size() < minimumEvidence) {
            throw new IllegalArgumentException("Understanding did not retain sufficient verified evidence.");
        }
        String summary = clean(input.summary(), 1200);
        if (summary.length() < 40) throw new IllegalArgumentException("Understanding summary is missing or too short.");
        List<String> ideas = cleanList(input.keyIdeas(), 1, 8, 160);
        List<String> tags = SemanticLabelPolicy.usefulTags(input.candidateTags());
        if (tags.isEmpty()) throw new IllegalArgumentException("Understanding did not contain useful semantic tags.");
        List<String> themes = cleanList(input.broadThemes(), 0, 4, 120);
        String normalizedTitle = normalizeTitle(input.normalizedTitle(), source.title(), source.originalFilename());
        String level = input.difficultyOrLevel() == null ? null : clean(input.difficultyOrLevel(), 120);
        return new DocumentUnderstandingResult(normalizedTitle, summary, ideas, tags, themes,
                level == null || level.isBlank() ? null : level, verified);
    }

    private DocumentUnderstandingResult fromStored(StoredUnderstanding stored) {
        try {
            return new DocumentUnderstandingResult(stored.normalizedTitle(), stored.summary(),
                    objectMapper.readValue(stored.keyIdeasJson(), new TypeReference<List<String>>() { }),
                    objectMapper.readValue(stored.candidateTagsJson(), new TypeReference<List<String>>() { }),
                    objectMapper.readValue(stored.broadThemesJson(), new TypeReference<List<String>>() { }),
                    stored.difficultyLevel(), stored.evidenceChunkIds());
        } catch (Exception e) {
            throw new IllegalStateException("Stored document understanding is invalid.", e);
        }
    }

    private List<String> parseList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (Exception e) {
            log.warn("Stored document understanding list could not be read: {}", e.getMessage());
            return List.of();
        }
    }

    static String normalizeTitle(String proposed, String currentTitle, String filename) {
        String candidate = cleanNullable(proposed, 240);
        if (candidate != null && !candidate.matches("(?i).+\\.(pdf|docx|txt|md)$")) return candidate;
        String fallback = cleanNullable(currentTitle, 240);
        if (fallback != null && !fallback.matches("(?i).+\\.(pdf|docx|txt|md)$")) return fallback;
        String raw = filename != null && !filename.isBlank() ? filename : currentTitle;
        if (raw == null || raw.isBlank()) return "Untitled resource";
        String cleaned = raw.replaceFirst("(?i)\\.(pdf|docx|txt|md)$", "")
                .replaceAll("(?i)(?:[_-](?:final|copy|draft|v\\d+)(?:[_-]?\\d+)?)+$", "")
                .replaceAll("[_-]+", " ").replaceAll("\\s+", " ").trim();
        if (cleaned.isBlank()) return "Untitled resource";
        return Arrays.stream(cleaned.split(" ")).map(word -> word.isBlank() ? word
                : Character.toUpperCase(word.charAt(0)) + word.substring(1)).reduce((a, b) -> a + " " + b).orElse(cleaned);
    }

    private static List<String> cleanList(List<String> input, int min, int max, int length) {
        List<String> result = input == null ? List.of() : input.stream().map(value -> cleanNullable(value, length))
                .filter(Objects::nonNull).distinct().limit(max).toList();
        if (result.size() < min) throw new IllegalArgumentException("Understanding list field is incomplete.");
        return result;
    }

    private static String clean(String value, int max) {
        String result = cleanNullable(value, max);
        if (result == null) throw new IllegalArgumentException("Understanding text field is missing.");
        return result;
    }

    private static String cleanNullable(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String result = value.replaceAll("[\\r\\n]+", " ").replaceAll("\\s+", " ").trim();
        return result.substring(0, Math.min(max, result.length()));
    }

    private String checksum(List<EvidenceChunk> chunks) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (EvidenceChunk chunk : chunks) digest.update(chunk.content().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) { throw new IllegalStateException("Could not calculate source checksum.", e); }
    }

    private String stripCodeFence(String raw) {
        if (raw == null) return "";
        return raw.trim().replaceFirst("(?s)^```(?:json)?\\s*", "").replaceFirst("(?s)\\s*```$", "");
    }

    private String failureCategory(Exception exception) {
        String message = safeMessage(exception).toLowerCase(Locale.ROOT);
        if (message.contains("429") || message.contains("quota")) return "RATE_LIMIT";
        if (message.contains("timeout") || message.contains("timed out")) return "TIMEOUT";
        if (message.contains("json") || message.contains("parse")) return "INVALID_JSON";
        if (message.contains("evidence")) return "INVALID_EVIDENCE";
        return "GENERATION_ERROR";
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return "Semantic understanding failed.";
        return message.substring(0, Math.min(300, message.length()));
    }
}
