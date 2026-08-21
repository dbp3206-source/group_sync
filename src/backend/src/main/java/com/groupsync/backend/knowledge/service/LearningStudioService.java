package com.groupsync.backend.knowledge.service;

import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.groupsync.backend.knowledge.dto.FocusStudioDto.*;
import com.groupsync.backend.knowledge.dto.ResourceDeepDiveResponse;
import com.groupsync.backend.knowledge.model.*;
import com.groupsync.backend.knowledge.repository.*;
import com.groupsync.backend.shared.exception.BadRequestException;
import com.groupsync.backend.shared.exception.NotFoundException;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@Service
public class LearningStudioService {

    private final StudyTopicRepository topicRepository;
    private final TopicConceptRepository conceptRepository;
    private final ResourceRepository resourceRepository;
    private final UserAccountRepository userRepository;

    public LearningStudioService(
            StudyTopicRepository topicRepository,
            TopicConceptRepository conceptRepository,
            ResourceRepository resourceRepository,
            UserAccountRepository userRepository) {
        this.topicRepository = topicRepository;
        this.conceptRepository = conceptRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
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
        return getTopicDetail(ownerId, saved.getId());
    }

    @Transactional(readOnly = true)
    public List<StudyTopicResponse> listTopics(Long ownerId) {
        List<StudyTopic> topics = topicRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId);
        return topics.stream().filter(topic -> "LEGACY".equals(topic.getLearningAreaType()))
                .map(this::mapToTopicResponse).toList();
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
                concepts.size(), checked, review, learning, notStarted, topic.getUpdatedAt(), List.of());
    }

    @Transactional
    public void deleteTopic(Long ownerId, Long topicId) {
        StudyTopic topic = topicRepository.findByIdAndOwnerId(topicId, ownerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề học tập."));
        if (topic.getCollectionId() != null) {
            throw new BadRequestException("Collection-backed Learning Areas are preserved with their Collection and cannot be deleted here.");
        }
        topicRepository.delete(topic);
    }

    @Transactional
    public StudyTopicDetailResponse addSourceToTopic(Long ownerId, Long topicId, Long resourceId) {
        StudyTopic topic = topicRepository.findByIdAndOwnerId(topicId, ownerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề học tập."));
        requireLegacyTopic(topic);
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
        requireLegacyTopic(topic);
        Resource res = resourceRepository.findByIdAndOwnerId(resourceId, ownerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài liệu."));

        topic.removeResource(res);
        topicRepository.save(topic);
        return getTopicDetail(ownerId, topicId);
    }

    @Transactional(readOnly = true)
    public StudyTopicDetailResponse generateLearningPlan(Long ownerId, Long topicId) {
        StudyTopic topic = topicRepository.findByIdAndOwnerId(topicId, ownerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề học tập."));
        if (topic.getResources().stream().noneMatch(resource -> resource.getProcessingStatus() == ResourceProcessingStatus.READY)) {
            throw new BadRequestException("Chủ đề chưa có tài liệu nào ở trạng thái sẵn sàng (READY) để tạo lộ trình học.");
        }
        throw new BadRequestException("Legacy Topic vẫn có thể học và Recall, nhưng không còn tái tạo lộ trình theo cơ chế cũ. Hãy mở một Collection trong Focus để xây Learning Area có version và giữ mastery.");
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

    private void requireLegacyTopic(StudyTopic topic) {
        if (topic.getCollectionId() != null) {
            throw new BadRequestException("Nguồn của Learning Area được kế thừa từ Collection. Hãy quản lý nguồn trong Library.");
        }
    }
}
