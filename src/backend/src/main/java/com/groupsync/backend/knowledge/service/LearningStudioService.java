package com.groupsync.backend.knowledge.service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupsync.backend.knowledge.dto.FocusStudioDto.*;
import com.groupsync.backend.knowledge.dto.ResourceDeepDiveResponse;
import com.groupsync.backend.knowledge.model.*;
import com.groupsync.backend.knowledge.rag.LanguageModelClient;
import com.groupsync.backend.knowledge.repository.*;
import com.groupsync.backend.shared.exception.BadRequestException;
import com.groupsync.backend.shared.exception.NotFoundException;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@Service
public class LearningStudioService {

    private static final Logger log = LoggerFactory.getLogger(LearningStudioService.class);
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private final StudyTopicRepository topicRepository;
    private final TopicConceptRepository conceptRepository;
    private final ResourceRepository resourceRepository;
    private final DocumentChunkRepository chunkRepository;
    private final UserAccountRepository userRepository;
    private final LanguageModelClient languageModelClient;

    public LearningStudioService(
            StudyTopicRepository topicRepository,
            TopicConceptRepository conceptRepository,
            ResourceRepository resourceRepository,
            DocumentChunkRepository chunkRepository,
            UserAccountRepository userRepository,
            LanguageModelClient languageModelClient) {
        this.topicRepository = topicRepository;
        this.conceptRepository = conceptRepository;
        this.resourceRepository = resourceRepository;
        this.chunkRepository = chunkRepository;
        this.userRepository = userRepository;
        this.languageModelClient = languageModelClient;
    }

    @Transactional
    public StudyTopicDetailResponse createTopic(Long ownerId, CreateStudyTopicRequest request) {
        UserAccount owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("User not found."));

        if (request.title() == null || request.title().isBlank()) {
            throw new BadRequestException("Tên chủ đề học tập không được để trống.");
        }
        String goal = (request.goal() != null && !request.goal().isBlank())
                ? request.goal().trim()
                : "Tìm hiểu và làm chủ các khái niệm cốt lõi trong " + request.title().trim();

        StudyTopic topic = new StudyTopic(owner, request.title().trim(), goal);

        if (request.resourceIds() != null && !request.resourceIds().isEmpty()) {
            for (Long resId : request.resourceIds()) {
                Resource res = resourceRepository.findByIdAndOwnerId(resId, ownerId)
                        .orElseThrow(() -> new BadRequestException("Tài liệu không thuộc quyền sở hữu của bạn: " + resId));
                topic.addResource(res);
            }
        }

        StudyTopic saved = topicRepository.save(topic);

        // Try generating initial plan if there are READY resources
        try {
            generateLearningPlan(ownerId, saved.getId());
        } catch (Exception e) {
            log.info("Initial plan generation deferred for topic {}: {}", saved.getId(), e.getMessage());
        }

        return getTopicDetail(ownerId, saved.getId());
    }

    @Transactional(readOnly = true)
    public List<StudyTopicResponse> listTopics(Long ownerId) {
        List<StudyTopic> topics = topicRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId);
        return topics.stream().map(this::mapToTopicResponse).toList();
    }

    @Transactional(readOnly = true)
    public StudyTopicDetailResponse getTopicDetail(Long ownerId, Long topicId) {
        StudyTopic topic = topicRepository.findByIdAndOwnerId(topicId, ownerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề học tập."));

        List<TopicConcept> concepts = conceptRepository.findByTopicIdOrderByPositionAsc(topicId);

        List<TopicResourceDto> resourceDtos = topic.getResources().stream()
                .map(r -> new TopicResourceDto(r.getId(), r.getTitle(), r.getResourceType().name(), r.getProcessingStatus().name(), 0))
                .sorted(Comparator.comparing(TopicResourceDto::title))
                .toList();

        List<TopicConceptDto> conceptDtos = concepts.stream().map(c -> {
            List<ConceptSourceDto> sources = c.getSourceChunks().stream().map(chk -> {
                Resource r = chk.getResource();
                String snippet = chk.getContent() != null && chk.getContent().length() > 180
                        ? chk.getContent().substring(0, 180) + "…"
                        : (chk.getContent() != null ? chk.getContent() : "");
                return new ConceptSourceDto(r.getId(), r.getTitle(), chk.getId(), snippet);
            }).toList();

            return new TopicConceptDto(c.getId(), c.getTitle(), c.getSummary(), c.getWhyItMatters(),
                    c.getStudyStatus(), c.getPosition(), sources);
        }).toList();

        int checked = 0, review = 0, learning = 0, notStarted = 0;
        for (TopicConcept c : concepts) {
            switch (c.getStudyStatus()) {
                case "CHECKED" -> checked++;
                case "REVIEW_NEEDED" -> review++;
                case "LEARNING" -> learning++;
                default -> notStarted++;
            }
        }

        return new StudyTopicDetailResponse(topic.getId(), topic.getTitle(), topic.getGoal(), topic.getStatus(),
                resourceDtos, conceptDtos, checked, review, learning, notStarted, topic.getCreatedAt(), topic.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public ResourceDeepDiveResponse getResourceDeepDive(Long ownerId, Long resourceId) {
        resourceRepository.findByIdAndOwnerId(resourceId, ownerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài liệu."));
        List<StudyTopic> topics = topicRepository.findByOwnerIdAndResourceId(ownerId, resourceId);
        if (topics.isEmpty()) return ResourceDeepDiveResponse.unavailable();

        StudyTopic topic = topics.stream()
                .max(Comparator.comparing(StudyTopic::getUpdatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElseThrow();
        List<TopicConcept> concepts = conceptRepository.findByTopicIdOrderByPositionAsc(topic.getId());
        int checked = 0, review = 0, learning = 0, notStarted = 0;
        for (TopicConcept concept : concepts) {
            switch (concept.getStudyStatus()) {
                case "CHECKED" -> checked++;
                case "REVIEW_NEEDED" -> review++;
                case "LEARNING" -> learning++;
                default -> notStarted++;
            }
        }
        return new ResourceDeepDiveResponse(true, topic.getId(), topic.getTitle(), topic.getGoal(), topic.getStatus(),
                concepts.size(), checked, review, learning, notStarted, topic.getUpdatedAt());
    }

    @Transactional
    public void deleteTopic(Long ownerId, Long topicId) {
        StudyTopic topic = topicRepository.findByIdAndOwnerId(topicId, ownerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề học tập."));
        topicRepository.delete(topic);
    }

    @Transactional
    public StudyTopicDetailResponse addSourceToTopic(Long ownerId, Long topicId, Long resourceId) {
        StudyTopic topic = topicRepository.findByIdAndOwnerId(topicId, ownerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề học tập."));
        Resource res = resourceRepository.findByIdAndOwnerId(resourceId, ownerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài liệu."));

        topic.addResource(res);
        topicRepository.save(topic);
        return getTopicDetail(ownerId, topicId);
    }

    @Transactional
    public StudyTopicDetailResponse removeSourceFromTopic(Long ownerId, Long topicId, Long resourceId) {
        StudyTopic topic = topicRepository.findByIdAndOwnerId(topicId, ownerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề học tập."));
        Resource res = resourceRepository.findByIdAndOwnerId(resourceId, ownerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài liệu."));

        topic.removeResource(res);
        topicRepository.save(topic);
        return getTopicDetail(ownerId, topicId);
    }

    @Transactional
    public StudyTopicDetailResponse generateLearningPlan(Long ownerId, Long topicId) {
        StudyTopic topic = topicRepository.findByIdAndOwnerId(topicId, ownerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề học tập."));

        List<Resource> readyResources = topic.getResources().stream()
                .filter(r -> r.getProcessingStatus() == ResourceProcessingStatus.READY)
                .toList();

        if (readyResources.isEmpty()) {
            throw new BadRequestException("Chủ đề chưa có tài liệu nào ở trạng thái sẵn sàng (READY) để tạo lộ trình học.");
        }

        // Collect available chunks from topic resources
        List<DocumentChunk> allChunks = new ArrayList<>();
        for (Resource res : readyResources) {
            allChunks.addAll(chunkRepository.findByResourceIdOrderByChunkIndex(res.getId()));
        }

        if (allChunks.isEmpty()) {
            throw new BadRequestException("Chưa có phân đoạn tri thức (chunks) khả dụng để tạo lộ trình.");
        }

        // Try AI-powered source-grounded concept extraction
        boolean generatedByAi = false;
        try {
            generatedByAi = extractConceptsWithAi(topic, allChunks);
        } catch (Exception e) {
            log.warn("Gemini concept extraction failed for topic {}, falling back to deterministic extraction: {}", topicId, e.getMessage());
        }

        if (!generatedByAi) {
            extractConceptsDeterministically(topic, allChunks);
        }

        return getTopicDetail(ownerId, topicId);
    }

    private boolean extractConceptsWithAi(StudyTopic topic, List<DocumentChunk> chunks) {
        // Build evidence text with chunk ID tags
        StringBuilder evidence = new StringBuilder();
        int chunkLimit = Math.min(chunks.size(), 12);
        for (int i = 0; i < chunkLimit; i++) {
            DocumentChunk c = chunks.get(i);
            evidence.append("[CHUNK_").append(c.getId()).append(" from ").append(c.getResource().getTitle()).append("]:\n")
                    .append(c.getContent()).append("\n\n");
        }

        String prompt = """
                You are an expert curriculum designer. Based ONLY on the following source evidence, construct a concise, high-value learning path consisting of 4 to 6 core study concepts for the goal: "%s".
                
                SOURCE EVIDENCE:
                %s
                
                INSTRUCTIONS:
                1. Output ONLY a valid JSON array of objects without Markdown code fence blocks if possible.
                2. Each object must have exactly these keys:
                   - "title": Concise concept name (in Vietnamese).
                   - "summary": Clear 2-3 sentence explanation of the concept grounded in the evidence.
                   - "whyItMatters": Why understanding this concept is essential for the learning goal.
                   - "sourceChunkIds": Array of valid integer CHUNK IDs from the evidence brackets above.
                3. Order concepts logically from foundational to advanced.
                """.formatted(topic.getGoal(), evidence.toString());

        String rawResponse = languageModelClient.answer(prompt);
        if (rawResponse == null || rawResponse.isBlank()) return false;

        String cleanJson = rawResponse.trim();
        if (cleanJson.startsWith("```json")) cleanJson = cleanJson.substring(7);
        if (cleanJson.startsWith("```")) cleanJson = cleanJson.substring(3);
        if (cleanJson.endsWith("```")) cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
        cleanJson = cleanJson.trim();

        try {
            JsonNode root = jsonMapper.readTree(cleanJson);
            if (!root.isArray() || root.isEmpty()) return false;

            // Map of valid chunk IDs
            Map<Long, DocumentChunk> chunkMap = chunks.stream()
                    .collect(Collectors.toMap(DocumentChunk::getId, c -> c, (a, b) -> a));

            List<TopicConcept> newConcepts = new ArrayList<>();
            int pos = 1;
            for (JsonNode node : root) {
                String title = node.path("title").asText("").trim();
                String summary = node.path("summary").asText("").trim();
                String whyItMatters = node.path("whyItMatters").asText("").trim();

                if (title.isBlank() || summary.isBlank()) continue;

                TopicConcept concept = new TopicConcept(topic, title, summary, whyItMatters, pos++);

                JsonNode chunkIdsNode = node.path("sourceChunkIds");
                if (chunkIdsNode.isArray()) {
                    for (JsonNode idNode : chunkIdsNode) {
                        Long chunkId = idNode.asLong();
                        DocumentChunk targetChunk = chunkMap.get(chunkId);
                        if (targetChunk != null) {
                            concept.getSourceChunks().add(targetChunk);
                        }
                    }
                }

                // If no valid chunk was linked, link at least the nearest chunk
                if (concept.getSourceChunks().isEmpty() && !chunks.isEmpty()) {
                    concept.getSourceChunks().add(chunks.get((pos - 2) % chunks.size()));
                }

                newConcepts.add(concept);
            }

            if (newConcepts.isEmpty()) return false;

            // Clear old concepts and save new ones
            List<TopicConcept> existing = conceptRepository.findByTopicIdOrderByPositionAsc(topic.getId());
            conceptRepository.deleteAll(existing);
            conceptRepository.saveAll(newConcepts);
            return true;
        } catch (Exception e) {
            log.warn("Failed to parse AI concept json: {}", e.getMessage());
            return false;
        }
    }

    private void extractConceptsDeterministically(StudyTopic topic, List<DocumentChunk> chunks) {
        List<TopicConcept> newConcepts = new ArrayList<>();
        int count = Math.min(chunks.size(), 5);

        for (int i = 0; i < count; i++) {
            DocumentChunk chk = chunks.get(i);
            String chunkContent = chk.getContent() != null ? chk.getContent().trim() : "";
            String title = extractFirstHeadlineOrTitle(chunkContent, "Khái niệm " + (i + 1) + " (" + chk.getResource().getTitle() + ")");
            String summary = chunkContent.length() > 220 ? chunkContent.substring(0, 220) + "…" : chunkContent;
            String whyItMatters = "Kiến thức nền tảng trích xuất trực tiếp từ tài liệu " + chk.getResource().getTitle();

            TopicConcept concept = new TopicConcept(topic, title, summary, whyItMatters, i + 1);
            concept.getSourceChunks().add(chk);
            newConcepts.add(concept);
        }

        List<TopicConcept> existing = conceptRepository.findByTopicIdOrderByPositionAsc(topic.getId());
        conceptRepository.deleteAll(existing);
        conceptRepository.saveAll(newConcepts);
    }

    private String extractFirstHeadlineOrTitle(String text, String fallback) {
        if (text == null || text.isBlank()) return fallback;
        String[] lines = text.split("\n");
        for (String line : lines) {
            String trimmed = line.trim().replaceAll("^[#\\-*•0-9.\\s]+", "").trim();
            if (trimmed.length() >= 5 && trimmed.length() <= 80) {
                return trimmed;
            }
        }
        return fallback;
    }

    @Transactional
    public TopicConceptDto updateConceptStatus(Long ownerId, Long topicId, Long conceptId, String status) {
        topicRepository.findByIdAndOwnerId(topicId, ownerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề học tập."));
        TopicConcept concept = conceptRepository.findByIdAndTopicId(conceptId, topicId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khái niệm."));

        String normalized = status != null ? status.toUpperCase().trim() : "NOT_STARTED";
        if (!List.of("NOT_STARTED", "LEARNING", "REVIEW_NEEDED", "CHECKED").contains(normalized)) {
            throw new BadRequestException("Trạng thái học tập không hợp lệ: " + status);
        }

        concept.setStudyStatus(normalized);
        TopicConcept saved = conceptRepository.save(concept);

        List<ConceptSourceDto> sources = saved.getSourceChunks().stream().map(chk -> {
            Resource r = chk.getResource();
            String snippet = chk.getContent() != null && chk.getContent().length() > 180
                    ? chk.getContent().substring(0, 180) + "…"
                    : (chk.getContent() != null ? chk.getContent() : "");
            return new ConceptSourceDto(r.getId(), r.getTitle(), chk.getId(), snippet);
        }).toList();

        return new TopicConceptDto(saved.getId(), saved.getTitle(), saved.getSummary(), saved.getWhyItMatters(),
                saved.getStudyStatus(), saved.getPosition(), sources);
    }

    @Transactional(readOnly = true)
    public List<ReviewQueueItemDto> getReviewQueue(Long ownerId) {
        List<TopicConcept> queue = conceptRepository.findActiveQueueByOwnerId(ownerId);
        return queue.stream().map(c -> new ReviewQueueItemDto(
                c.getId(),
                c.getTitle(),
                c.getTopic().getId(),
                c.getTopic().getTitle(),
                c.getStudyStatus(),
                c.getSummary(),
                c.getWhyItMatters(),
                c.getUpdatedAt()
        )).toList();
    }

    private StudyTopicResponse mapToTopicResponse(StudyTopic t) {
        List<TopicConcept> concepts = conceptRepository.findByTopicIdOrderByPositionAsc(t.getId());
        int checked = 0, review = 0, learning = 0, notStarted = 0;
        for (TopicConcept c : concepts) {
            switch (c.getStudyStatus()) {
                case "CHECKED" -> checked++;
                case "REVIEW_NEEDED" -> review++;
                case "LEARNING" -> learning++;
                default -> notStarted++;
            }
        }
        return new StudyTopicResponse(t.getId(), t.getTitle(), t.getGoal(), t.getStatus(),
                t.getResources().size(), concepts.size(), checked, review, learning, notStarted, t.getCreatedAt(), t.getUpdatedAt());
    }
}
