package com.groupsync.backend.knowledge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.groupsync.backend.knowledge.dto.FocusStudioDto.*;
import com.groupsync.backend.knowledge.model.*;
import com.groupsync.backend.knowledge.rag.LanguageModelClient;
import com.groupsync.backend.knowledge.repository.*;
import com.groupsync.backend.knowledge.service.RecallCheckService;
import com.groupsync.backend.knowledge.service.RecallCheckTransactionService;
import com.groupsync.backend.shared.exception.BadRequestException;
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

    private RecallCheckTransactionService transactionService;
    private RecallCheckService recallCheckService;

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
                attemptRepository, userRepository);
        recallCheckService = new RecallCheckService(transactionService, languageModelClient);

        owner = new UserAccount("owner@example.com", "hash", "Owner User");
        topic = new StudyTopic(owner, "RAG Topic", "Master RAG");
        resource = new Resource(owner, "rag.pdf", "desc", ResourceType.PDF, "rag.pdf", "application/pdf", 1000L, "k", "c");
        resource.beginParsing(); resource.beginChunking(); resource.beginEmbedding(); resource.markReady();
        topic.addResource(resource);

        chunk1 = new DocumentChunk(resource, 0, 1, "Section 1", "RRF combines rankings from different search lists.");
        ReflectionTestUtils.setField(chunk1, "id", 101L);
        chunk2 = new DocumentChunk(resource, 1, 2, "Section 2", "pgvector uses HNSW indexing for approximate nearest neighbors.");
        ReflectionTestUtils.setField(chunk2, "id", 102L);

        concept = new TopicConcept(topic, "RRF Fusion", "Combines ranks", "Important for hybrid", 1);
        ReflectionTestUtils.setField(concept, "id", 50L);
        concept.getSourceChunks().add(chunk1);
        concept.getSourceChunks().add(chunk2);
    }

    @Test
    void testA_invalidSourceId_isRejectedAndNeverReplacedWithFallback() {
        // Allowed chunks are chunk 101 and chunk 102.
        // LLM returns 1 question pointing to chunk 999 (NOT in allowed set) and 1 question pointing to 101.
        String llmJson = """
                [
                  {
                    "question": "What is hallucinated chunk?",
                    "options": ["A", "B", "C", "D"],
                    "correctOption": 0,
                    "explanation": "Fake source",
                    "sourceChunkId": 999
                  },
                  {
                    "question": "What is RRF fusion?",
                    "options": ["Rank fusion", "Vector index", "Cache", "None"],
                    "correctOption": 0,
                    "explanation": "RRF combines rankings.",
                    "sourceChunkId": 101
                  }
                ]
                """;

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(topicRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(topic));
        when(conceptRepository.findByIdAndTopicId(50L, 10L)).thenReturn(Optional.of(concept));
        when(languageModelClient.answer(anyString())).thenReturn(llmJson);
        when(chunkRepository.findById(101L)).thenReturn(Optional.of(chunk1));
        when(attemptRepository.save(any(QuizAttempt.class))).thenAnswer(inv -> {
            QuizAttempt a = inv.getArgument(0);
            ReflectionTestUtils.setField(a, "id", 200L);
            return a;
        });

        QuizAttemptResponse response = recallCheckService.generateQuiz(1L, 10L, 50L);

        // Verified: The question referencing fake chunk 999 is REJECTED.
        // Exactly 1 valid question referencing 101 is kept.
        assertEquals(1, response.totalQuestions());
        assertEquals(1, response.questions().size());
        assertEquals(101L, response.questions().get(0).sourceChunkId());
        assertEquals("What is RRF fusion?", response.questions().get(0).question());
    }

    @Test
    void testB_invalidCorrectOption_isRejected() {
        // Question 1 has correctOption = 7 (out of bounds).
        // Question 2 has correctOption = 1 (valid).
        String llmJson = """
                [
                  {
                    "question": "Question with bad index",
                    "options": ["A", "B", "C", "D"],
                    "correctOption": 7,
                    "explanation": "Bad index",
                    "sourceChunkId": 101
                  },
                  {
                    "question": "Valid pgvector question",
                    "options": ["A", "HNSW indexing", "C", "D"],
                    "correctOption": 1,
                    "explanation": "pgvector uses HNSW.",
                    "sourceChunkId": 102
                  }
                ]
                """;

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(topicRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(topic));
        when(conceptRepository.findByIdAndTopicId(50L, 10L)).thenReturn(Optional.of(concept));
        when(languageModelClient.answer(anyString())).thenReturn(llmJson);
        when(chunkRepository.findById(102L)).thenReturn(Optional.of(chunk2));
        when(attemptRepository.save(any(QuizAttempt.class))).thenAnswer(inv -> {
            QuizAttempt a = inv.getArgument(0);
            ReflectionTestUtils.setField(a, "id", 201L);
            return a;
        });

        QuizAttemptResponse response = recallCheckService.generateQuiz(1L, 10L, 50L);

        assertEquals(1, response.totalQuestions());
        assertEquals("Valid pgvector question", response.questions().get(0).question());
        assertEquals(102L, response.questions().get(0).sourceChunkId());
    }

    @Test
    void testC_insufficientValidQuestions_doesNotClaimFiveQuestions() {
        // Only 2 valid questions generated from Gemini
        String llmJson = """
                [
                  {
                    "question": "Question 1",
                    "options": ["A", "B", "C", "D"],
                    "correctOption": 0,
                    "explanation": "Exp 1",
                    "sourceChunkId": 101
                  },
                  {
                    "question": "Question 2",
                    "options": ["A", "B", "C", "D"],
                    "correctOption": 1,
                    "explanation": "Exp 2",
                    "sourceChunkId": 102
                  }
                ]
                """;

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(topicRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(topic));
        when(conceptRepository.findByIdAndTopicId(50L, 10L)).thenReturn(Optional.of(concept));
        when(languageModelClient.answer(anyString())).thenReturn(llmJson);
        when(chunkRepository.findById(101L)).thenReturn(Optional.of(chunk1));
        when(chunkRepository.findById(102L)).thenReturn(Optional.of(chunk2));

        ArgumentCaptor<QuizAttempt> attemptCaptor = ArgumentCaptor.forClass(QuizAttempt.class);
        when(attemptRepository.save(attemptCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        QuizAttemptResponse response = recallCheckService.generateQuiz(1L, 10L, 50L);

        // Database attempt entity and response MUST report totalQuestions = 2 (not 5)
        assertEquals(2, response.totalQuestions());
        assertEquals(2, attemptCaptor.getValue().getTotalQuestions());
        assertEquals(2, attemptCaptor.getValue().getItems().size());
    }

    @Test
    void testD_geminiFailure_throwsTruthfulErrorAndNeverFabricatesFallbackQuiz() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(topicRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(topic));
        when(conceptRepository.findByIdAndTopicId(50L, 10L)).thenReturn(Optional.of(concept));
        when(languageModelClient.answer(anyString())).thenThrow(new RuntimeException("Gemini quota exceeded (429)"));

        assertThrows(BadRequestException.class, () -> recallCheckService.generateQuiz(1L, 10L, 50L));

        // No attempt or fake item was saved
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void testE_groundingPreservation_everyPersistedItemLinksToAllowedChunk() {
        String llmJson = """
                [
                  {
                    "question": "Question 1",
                    "options": ["A", "B", "C", "D"],
                    "correctOption": 0,
                    "explanation": "Exp 1",
                    "sourceChunkId": 101
                  }
                ]
                """;

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(topicRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(topic));
        when(conceptRepository.findByIdAndTopicId(50L, 10L)).thenReturn(Optional.of(concept));
        when(languageModelClient.answer(anyString())).thenReturn(llmJson);
        when(chunkRepository.findById(101L)).thenReturn(Optional.of(chunk1));
        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        QuizAttemptResponse response = recallCheckService.generateQuiz(1L, 10L, 50L);

        assertEquals(1, response.questions().size());
        assertEquals(101L, response.questions().get(0).sourceChunkId());
        assertEquals("rag.pdf", response.questions().get(0).sourceResourceTitle());
    }

    @Test
    void testG_duplicateExactOptions_isRejected() {
        String llmJson = """
                [
                  {
                    "question": "Question with exact duplicate options",
                    "options": ["RRF", "RRF", "FTS", "Vector Search"],
                    "correctOption": 0,
                    "explanation": "Duplicate options",
                    "sourceChunkId": 101
                  },
                  {
                    "question": "Valid Question",
                    "options": ["RRF", "BM25", "Vector Search", "Metadata Filtering"],
                    "correctOption": 0,
                    "explanation": "Valid unique options",
                    "sourceChunkId": 101
                  }
                ]
                """;

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(topicRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(topic));
        when(conceptRepository.findByIdAndTopicId(50L, 10L)).thenReturn(Optional.of(concept));
        when(languageModelClient.answer(anyString())).thenReturn(llmJson);
        when(chunkRepository.findById(101L)).thenReturn(Optional.of(chunk1));
        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        QuizAttemptResponse response = recallCheckService.generateQuiz(1L, 10L, 50L);

        assertEquals(1, response.totalQuestions(), "Question with exact duplicate options must be rejected");
        assertEquals("Valid Question", response.questions().get(0).question());
    }

    @Test
    void testH_duplicateCaseInsensitiveOptions_isRejected() {
        String llmJson = """
                [
                  {
                    "question": "Question with case-insensitive duplicate options",
                    "options": ["RRF", "rrf", "FTS", "Vector Search"],
                    "correctOption": 0,
                    "explanation": "Duplicate case-insensitive options",
                    "sourceChunkId": 101
                  },
                  {
                    "question": "Valid Question",
                    "options": ["RRF", "BM25", "Vector Search", "Metadata Filtering"],
                    "correctOption": 0,
                    "explanation": "Valid unique options",
                    "sourceChunkId": 101
                  }
                ]
                """;

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(topicRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(topic));
        when(conceptRepository.findByIdAndTopicId(50L, 10L)).thenReturn(Optional.of(concept));
        when(languageModelClient.answer(anyString())).thenReturn(llmJson);
        when(chunkRepository.findById(101L)).thenReturn(Optional.of(chunk1));
        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        QuizAttemptResponse response = recallCheckService.generateQuiz(1L, 10L, 50L);

        assertEquals(1, response.totalQuestions(), "Question with case-insensitive duplicate options must be rejected");
        assertEquals("Valid Question", response.questions().get(0).question());
    }

    @Test
    void testI_validUniqueOptions_isAccepted() {
        String llmJson = """
                [
                  {
                    "question": "What is RRF fusion?",
                    "options": ["RRF", "BM25", "Vector Search", "Metadata Filtering"],
                    "correctOption": 0,
                    "explanation": "Valid unique choices",
                    "sourceChunkId": 101
                  }
                ]
                """;

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(topicRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(topic));
        when(conceptRepository.findByIdAndTopicId(50L, 10L)).thenReturn(Optional.of(concept));
        when(languageModelClient.answer(anyString())).thenReturn(llmJson);
        when(chunkRepository.findById(101L)).thenReturn(Optional.of(chunk1));
        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        QuizAttemptResponse response = recallCheckService.generateQuiz(1L, 10L, 50L);

        assertEquals(1, response.totalQuestions());
        assertEquals("What is RRF fusion?", response.questions().get(0).question());
        assertEquals(List.of("RRF", "BM25", "Vector Search", "Metadata Filtering"), response.questions().get(0).options());
    }

    @Test
    void submitAnswers_scoresCorrectlyAndFlagsReviewOnWrongAnswer() {
        QuizAttempt attempt = new QuizAttempt(topic, owner, concept, 2);

        QuizItem item1 = new QuizItem(attempt, concept, "What is RRF?",
                "[\"Rank fusion\",\"Score norm\",\"Vector search\",\"None\"]",
                0, "RRF combines rankings.", resource, chunk1, "RRF snippet");

        QuizItem item2 = new QuizItem(attempt, concept, "Why use RRF?",
                "[\"Fast\",\"Different score scales\",\"Cheaper\",\"None\"]",
                1, "Scores are on different scales.", resource, chunk1, "RRF snippet");

        ReflectionTestUtils.setField(item1, "id", 1001L);
        ReflectionTestUtils.setField(item2, "id", 1002L);
        attempt.addItem(item1);
        attempt.addItem(item2);

        when(attemptRepository.findByIdAndOwnerId(100L, 1L)).thenReturn(Optional.of(attempt));
        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

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
}
