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
import org.springframework.test.util.ReflectionTestUtils;
import com.groupsync.backend.knowledge.model.*;
import com.groupsync.backend.knowledge.repository.*;
import com.groupsync.backend.knowledge.service.RecallCheckTransactionService;
import com.groupsync.backend.knowledge.service.RecallCheckTransactionService.QuizEvidence;
import com.groupsync.backend.knowledge.service.RecallCheckTransactionService.QuizEvidenceChunk;
import com.groupsync.backend.knowledge.service.RecallCheckTransactionService.ValidatedQuizQuestion;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

/**
 * Regression test verifying that RecallCheckTransactionService.prepareEvidence(...)
 * produces pure, immutable DTO records (QuizEvidenceChunk) and does NOT leak detached
 * JPA entities across transaction boundaries, completely eliminating LazyInitializationException risk.
 */
@ExtendWith(MockitoExtension.class)
class RecallCheckLazyLoadingSafetyTest {

    @Mock private StudyTopicRepository topicRepository;
    @Mock private TopicConceptRepository conceptRepository;
    @Mock private DocumentChunkRepository chunkRepository;
    @Mock private QuizAttemptRepository attemptRepository;
    @Mock private UserAccountRepository userRepository;

    private RecallCheckTransactionService transactionService;
    private UserAccount owner;
    private StudyTopic topic;
    private Resource resource;
    private DocumentChunk chunk1;
    private DocumentChunk chunk2;
    private TopicConcept concept;

    @BeforeEach
    void setUp() {
        transactionService = new RecallCheckTransactionService(
                topicRepository, conceptRepository, chunkRepository,
                attemptRepository, userRepository
        );

        owner = new UserAccount("student@example.com", "hash", "Student");
        topic = new StudyTopic(owner, "Database Normalization", "Learn 1NF, 2NF, 3NF, BCNF");
        ReflectionTestUtils.setField(topic, "id", 10L);

        resource = new Resource(owner, "normalization.pdf", "desc", ResourceType.PDF, "normalization.pdf", "application/pdf", 1024L, "k", "c");
        ReflectionTestUtils.setField(resource, "id", 99L);
        resource.beginParsing(); resource.beginChunking(); resource.beginEmbedding(); resource.markReady();
        topic.addResource(resource);

        chunk1 = new DocumentChunk(resource, 0, 1, "Section 1", "Functional dependency X -> Y means X uniquely determines Y.");
        ReflectionTestUtils.setField(chunk1, "id", 101L);

        chunk2 = new DocumentChunk(resource, 1, 2, "Section 2", "BCNF requires every determinant to be a superkey.");
        ReflectionTestUtils.setField(chunk2, "id", 102L);

        concept = new TopicConcept(topic, "Boyce-Codd Normal Form", "BCNF rules", "Strict normal form", 1);
        ReflectionTestUtils.setField(concept, "id", 50L);
        concept.getSourceChunks().add(chunk1);
        concept.getSourceChunks().add(chunk2);
    }

    @Test
    void prepareEvidence_returnsImmutableDtoChunksWithoutDetachedJpaEntities() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(topicRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(topic));
        when(conceptRepository.findByIdAndTopicId(50L, 10L)).thenReturn(Optional.of(concept));

        // When prepareEvidence is called inside the read-only transaction:
        QuizEvidence evidence = transactionService.prepareEvidence(1L, 10L, 50L);

        // Then transaction closes. Verify returned object holds DTO records only:
        assertNotNull(evidence);
        assertEquals(10L, evidence.topicId());
        assertEquals("Database Normalization", evidence.topicTitle());
        assertEquals(50L, evidence.conceptId());
        assertEquals("Boyce-Codd Normal Form", evidence.conceptTitle());

        // Verify evidence chunks are QuizEvidenceChunk instances (not DocumentChunk JPA entities)
        List<QuizEvidenceChunk> chunks = evidence.allowedChunks();
        assertEquals(2, chunks.size());

        QuizEvidenceChunk chunkDto1 = chunks.stream().filter(c -> c.chunkId().equals(101L)).findFirst().orElseThrow();
        assertEquals(101L, chunkDto1.chunkId());
        assertEquals(99L, chunkDto1.resourceId());
        assertEquals("normalization.pdf", chunkDto1.resourceTitle());
        assertEquals("Functional dependency X -> Y means X uniquely determines Y.", chunkDto1.content());
        assertEquals(1, chunkDto1.pageNumber());
        assertEquals("Section 1", chunkDto1.sectionTitle());

        QuizEvidenceChunk chunkDto2 = chunks.stream().filter(c -> c.chunkId().equals(102L)).findFirst().orElseThrow();
        assertEquals(102L, chunkDto2.chunkId());
        assertEquals(99L, chunkDto2.resourceId());
        assertEquals("normalization.pdf", chunkDto2.resourceTitle());
        assertEquals("BCNF requires every determinant to be a superkey.", chunkDto2.content());

        // Prove map access operates on immutable DTO records
        assertTrue(evidence.allowedChunkMap().containsKey(101L));
        assertTrue(evidence.allowedChunkMap().containsKey(102L));
        assertEquals("normalization.pdf", evidence.allowedChunkMap().get(101L).resourceTitle());
    }

    @Test
    void persistQuizAttempt_reloadsEntitiesInsideWriteTransactionAndValidatesAllowedSet() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(topicRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(topic));
        when(conceptRepository.findByIdAndTopicId(50L, 10L)).thenReturn(Optional.of(concept));
        when(chunkRepository.findById(101L)).thenReturn(Optional.of(chunk1));
        when(attemptRepository.save(any(QuizAttempt.class))).thenAnswer(inv -> {
            QuizAttempt a = inv.getArgument(0);
            ReflectionTestUtils.setField(a, "id", 300L);
            return a;
        });

        Set<Long> allowedChunkIds = Set.of(101L, 102L);
        List<ValidatedQuizQuestion> questions = List.of(
                new ValidatedQuizQuestion(
                        "What is BCNF?",
                        List.of("Determinant is superkey", "3NF", "No nulls", "None"),
                        0,
                        "Every determinant must be a superkey in BCNF.",
                        101L,
                        "Snippet text"
                )
        );

        var response = transactionService.persistQuizAttempt(1L, 10L, 50L, allowedChunkIds, questions);

        assertNotNull(response);
        assertEquals(1, response.totalQuestions());
        verify(chunkRepository, times(1)).findById(101L);
        verify(attemptRepository, times(1)).save(any(QuizAttempt.class));
    }

    @Test
    void persistQuizAttempt_rejectsQuestionWithChunkIdOutsideAllowedSet() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(topicRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(topic));
        when(conceptRepository.findByIdAndTopicId(50L, 10L)).thenReturn(Optional.of(concept));

        Set<Long> allowedChunkIds = Set.of(101L);
        List<ValidatedQuizQuestion> questions = List.of(
                new ValidatedQuizQuestion(
                        "What is ungrounded question?",
                        List.of("A", "B", "C", "D"),
                        0,
                        "Explanation",
                        999L, // Not in allowed set
                        "Snippet"
                )
        );

        assertThrows(com.groupsync.backend.shared.exception.BadRequestException.class, () ->
                transactionService.persistQuizAttempt(1L, 10L, 50L, allowedChunkIds, questions)
        );

        verify(attemptRepository, never()).save(any());
    }
}
