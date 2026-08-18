package com.groupsync.backend.knowledge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.groupsync.backend.knowledge.dto.FocusStudioDto.*;
import com.groupsync.backend.knowledge.model.*;
import com.groupsync.backend.knowledge.rag.LanguageModelClient;
import com.groupsync.backend.knowledge.repository.*;
import com.groupsync.backend.knowledge.service.LearningStudioService;
import com.groupsync.backend.shared.exception.BadRequestException;
import com.groupsync.backend.shared.exception.NotFoundException;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class LearningStudioServiceTest {

    @Mock private StudyTopicRepository topicRepository;
    @Mock private TopicConceptRepository conceptRepository;
    @Mock private ResourceRepository resourceRepository;
    @Mock private DocumentChunkRepository chunkRepository;
    @Mock private UserAccountRepository userRepository;
    @Mock private LanguageModelClient languageModelClient;

    private LearningStudioService studioService;
    private UserAccount owner;
    private UserAccount otherUser;

    @BeforeEach
    void setUp() {
        studioService = new LearningStudioService(
                topicRepository, conceptRepository, resourceRepository,
                chunkRepository, userRepository, languageModelClient);

        owner = new UserAccount("owner@example.com", "hash", "Owner User");
        otherUser = new UserAccount("other@example.com", "hash", "Other User");
    }

    @Test
    void createTopic_validatesOwnershipAndSaves() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        Resource r1 = new Resource(owner, "Doc 1", "Desc", ResourceType.PDF, "doc1.pdf", "application/pdf", 1000L, "k1", "c1");
        r1.beginParsing(); r1.beginChunking(); r1.beginEmbedding(); r1.markReady();

        when(resourceRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(r1));
        when(topicRepository.save(any(StudyTopic.class))).thenAnswer(inv -> inv.getArgument(0));
        when(topicRepository.findByIdAndOwnerId(any(), eq(1L))).thenAnswer(inv -> Optional.of(new StudyTopic(owner, "RAG Architecture", "Master RAG")));
        when(conceptRepository.findByTopicIdOrderByPositionAsc(any())).thenReturn(Collections.emptyList());

        CreateStudyTopicRequest req = new CreateStudyTopicRequest("RAG Architecture", "Master RAG", List.of(10L));
        StudyTopicDetailResponse resp = studioService.createTopic(1L, req);

        assertNotNull(resp);
        assertEquals("RAG Architecture", resp.title());
    }

    @Test
    void createTopic_rejectsResourceFromDifferentOwner() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(resourceRepository.findByIdAndOwnerId(99L, 1L)).thenReturn(Optional.empty());

        CreateStudyTopicRequest req = new CreateStudyTopicRequest("Topic", "Goal", List.of(99L));
        assertThrows(BadRequestException.class, () -> studioService.createTopic(1L, req));
    }

    @Test
    void generateLearningPlan_failsIfNoResourcesAreReady() {
        StudyTopic topic = new StudyTopic(owner, "Topic 1", "Goal");
        Resource unready = new Resource(owner, "Draft", "Desc", ResourceType.PDF, "draft.pdf", "application/pdf", 1000L, "k2", "c2");
        topic.addResource(unready);

        when(topicRepository.findByIdAndOwnerId(5L, 1L)).thenReturn(Optional.of(topic));

        assertThrows(BadRequestException.class, () -> studioService.generateLearningPlan(1L, 5L));
    }

    @Test
    void updateConceptStatus_transitionsCorrectly() {
        StudyTopic topic = new StudyTopic(owner, "Topic 1", "Goal");
        TopicConcept concept = new TopicConcept(topic, "Hybrid RRF", "Summary", "Why", 1);

        when(topicRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(topic));
        when(conceptRepository.findByIdAndTopicId(10L, 1L)).thenReturn(Optional.of(concept));
        when(conceptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TopicConceptDto updated = studioService.updateConceptStatus(1L, 1L, 10L, "CHECKED");
        assertEquals("CHECKED", updated.studyStatus());
    }

    @Test
    void getReviewQueue_returnsOwnerActiveQueue() {
        StudyTopic topic = new StudyTopic(owner, "Topic 1", "Goal");
        TopicConcept c1 = new TopicConcept(topic, "Cosine Similarity", "Summary", "Why", 1);
        c1.markReviewNeeded();

        when(conceptRepository.findActiveQueueByOwnerId(1L)).thenReturn(List.of(c1));

        List<ReviewQueueItemDto> queue = studioService.getReviewQueue(1L);
        assertEquals(1, queue.size());
        assertEquals("Cosine Similarity", queue.get(0).conceptTitle());
        assertEquals("REVIEW_NEEDED", queue.get(0).studyStatus());
    }
}
