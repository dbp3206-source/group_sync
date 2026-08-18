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
import com.groupsync.backend.knowledge.service.RecallCheckService;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class RecallCheckServiceTest {

    @Mock private StudyTopicRepository topicRepository;
    @Mock private TopicConceptRepository conceptRepository;
    @Mock private DocumentChunkRepository chunkRepository;
    @Mock private QuizAttemptRepository attemptRepository;
    @Mock private UserAccountRepository userRepository;
    @Mock private LanguageModelClient languageModelClient;

    private RecallCheckService recallCheckService;
    private UserAccount owner;
    private StudyTopic topic;
    private Resource resource;
    private DocumentChunk chunk;
    private TopicConcept concept;

    @BeforeEach
    void setUp() {
        recallCheckService = new RecallCheckService(
                topicRepository, conceptRepository, chunkRepository,
                attemptRepository, userRepository, languageModelClient);

        owner = new UserAccount("owner@example.com", "hash", "Owner User");
        topic = new StudyTopic(owner, "RAG Topic", "Master RAG");
        resource = new Resource(owner, "rag.pdf", "desc", ResourceType.PDF, "rag.pdf", "application/pdf", 1000L, "k", "c");
        resource.beginParsing(); resource.beginChunking(); resource.beginEmbedding(); resource.markReady();
        topic.addResource(resource);

        chunk = new DocumentChunk(resource, 0, 1, "Section 1", "RRF combines rankings from different search lists.");
        concept = new TopicConcept(topic, "RRF Fusion", "Combines ranks", "Important for hybrid", 1);
        concept.getSourceChunks().add(chunk);
    }

    @Test
    void submitAnswers_scoresCorrectlyAndFlagsReviewOnWrongAnswer() {
        QuizAttempt attempt = new QuizAttempt(topic, owner, concept, 2);

        QuizItem item1 = new QuizItem(attempt, concept, "What is RRF?",
                "[\"Rank fusion\",\"Score norm\",\"Vector search\",\"None\"]",
                0, "RRF combines rankings.", resource, chunk, "RRF snippet");

        QuizItem item2 = new QuizItem(attempt, concept, "Why use RRF?",
                "[\"Fast\",\"Different score scales\",\"Cheaper\",\"None\"]",
                1, "Scores are on different scales.", resource, chunk, "RRF snippet");

        item1.setId(1001L);
        item2.setId(1002L);
        attempt.addItem(item1);
        attempt.addItem(item2);

        when(attemptRepository.findByIdAndOwnerId(100L, 1L)).thenReturn(Optional.of(attempt));
        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // User answers item1 correctly (0), item2 incorrectly (0 instead of 1)
        SubmitQuizAnswersRequest request = new SubmitQuizAnswersRequest(Map.of(
                1001L, 0,
                1002L, 0
        ));

        SubmitQuizAnswersResponse response = recallCheckService.submitAnswers(1L, 100L, request);

        assertEquals(1, response.scoreCorrect());
        assertEquals(2, response.totalQuestions());
        assertEquals(50.0, response.percentage());
        assertEquals(1, response.conceptsNeedingReview().size());
        assertEquals("RRF Fusion", response.conceptsNeedingReview().get(0).title());
        assertEquals("REVIEW_NEEDED", concept.getStudyStatus());
    }

    @Test
    void submitAnswers_marksConceptCheckedWhenAllAnswersPass() {
        QuizAttempt attempt = new QuizAttempt(topic, owner, concept, 1);

        QuizItem item1 = new QuizItem(attempt, concept, "What is RRF?",
                "[\"Rank fusion\",\"Score norm\",\"Vector search\",\"None\"]",
                0, "RRF combines rankings.", resource, chunk, "RRF snippet");
        item1.setId(1001L);
        attempt.addItem(item1);

        when(attemptRepository.findByIdAndOwnerId(101L, 1L)).thenReturn(Optional.of(attempt));
        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubmitQuizAnswersRequest request = new SubmitQuizAnswersRequest(Map.of(1001L, 0));
        SubmitQuizAnswersResponse response = recallCheckService.submitAnswers(1L, 101L, request);

        assertEquals(1, response.scoreCorrect());
        assertEquals(100.0, response.percentage());
        assertEquals(0, response.conceptsNeedingReview().size());
        assertEquals("CHECKED", concept.getStudyStatus());
    }
}
