package com.groupsync.backend.knowledge.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.groupsync.backend.knowledge.dto.*;
import com.groupsync.backend.knowledge.model.*;
import com.groupsync.backend.knowledge.rag.*;
import com.groupsync.backend.knowledge.repository.*;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class AskAttemptServiceLifecycleTest {
    @Mock private KnowledgeChatService chatService;
    @Mock private KnowledgeChatTransactionService transactionService;
    @Mock private AskAttemptRepository attemptRepository;
    @Mock private AiUsageEventRepository usageRepository;
    @Mock private UserAccountRepository userRepository;
    @Mock private LanguageModelClient languageModelClient;

    private final UserAccount owner = new UserAccount("ask@test.com", "hash", "Ask User");
    private AskAttemptService service;
    private KnowledgeChatTransactionService.ChatPreparation preparation;
    private AskKnowledgeRequest request;

    @BeforeEach
    void setUp() {
        service = new AskAttemptService(chatService, transactionService, attemptRepository, usageRepository,
                userRepository, languageModelClient,
                new GeminiProperties("", "gemini-3.5-flash-lite", "gemini-3.5-flash", "gemini-embedding-001", 768, 16, 5, 2, 12, 60, 30000),
                new LocalUsageClassifier());
        request = new AskKnowledgeRequest(null, "Explain the stored question", RetrievalScope.LIBRARY, null, List.of(), null, null);
        preparation = new KnowledgeChatTransactionService.ChatPreparation(10L, 20L, RetrievalScope.LIBRARY,
                null, List.of(), null, List.of(), List.of());
        lenient().when(attemptRepository.save(any(AskAttempt.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        lenient().when(languageModelClient.lastUsage()).thenReturn(Optional.empty());
        lenient().when(usageRepository.save(any(AiUsageEvent.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void successfulStartClaimsOnceAndCompletesWithoutCreatingASecondAssistantInAttemptService() throws Exception {
        AskAttempt attempt = attempt(30L, AskAttemptStatus.PENDING);
        AtomicBoolean preparedBeforeGeneration = new AtomicBoolean(false);
        CountDownLatch finished = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(1);
        when(transactionService.prepareConversation(1L, request)).thenAnswer(inv -> { preparedBeforeGeneration.set(true); return preparation; });
        when(attemptRepository.save(any(AskAttempt.class))).thenAnswer(inv -> { AskAttempt value = inv.getArgument(0); ReflectionTestUtils.setField(value, "id", 30L); if (value.getStatus() == AskAttemptStatus.COMPLETE) completed.countDown(); return value; });
        when(attemptRepository.findById(30L)).thenReturn(Optional.of(attempt));
        when(attemptRepository.claimExecution(30L, AskAttemptStatus.PENDING, AskAttemptStatus.RUNNING)).thenReturn(1);
        when(chatService.askPrepared(1L, request, preparation)).thenAnswer(inv -> { assertTrue(preparedBeforeGeneration.get()); finished.countDown(); return new AskKnowledgeResponse(10L, "answer", true, List.of()); });

        service.start(1L, request);

        assertTrue(finished.await(3, TimeUnit.SECONDS));
        assertTrue(completed.await(3, TimeUnit.SECONDS));
        assertEquals(AskAttemptStatus.COMPLETE, attempt.getStatus());
        verify(attemptRepository).claimExecution(30L, AskAttemptStatus.PENDING, AskAttemptStatus.RUNNING);
        verify(transactionService, never()).markUserFailed(anyLong(), any());
    }

    @Test
    void failed429RetainsTurnAndDoesNotPersistAssistant() throws Exception {
        runFailure(new RuntimeException("HTTP 429 RESOURCE_EXHAUSTED"), AskFailureCategory.RATE_LIMIT);
    }

    @Test
    void failedTimeoutRetainsTurnAsTimeout() throws Exception {
        runFailure(new RuntimeException("provider timed out"), AskFailureCategory.TIMEOUT);
    }

    @Test
    void retrievalFailureIsNotRateLimit() throws Exception {
        runFailure(new RuntimeException("vector database retrieval failed"), AskFailureCategory.RETRIEVAL);
    }

    @Test
    void retryUsesSameUserMessageAndOnlyAFailedAttemptCanClaim() throws Exception {
        AskAttempt attempt = attempt(31L, AskAttemptStatus.FAILED);
        KnowledgeChatTransactionService.ChatPreparation retryPreparation = new KnowledgeChatTransactionService.ChatPreparation(
                10L, 20L, RetrievalScope.LIBRARY, null, List.of(), null, List.of(), List.of());
        CountDownLatch finished = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(1);
        when(attemptRepository.findByIdAndOwnerId(31L, 1L)).thenReturn(Optional.of(attempt));
        when(attemptRepository.claimRetry(31L, 1L, AskAttemptStatus.FAILED, AskAttemptStatus.PENDING)).thenReturn(1);
        when(transactionService.prepareRetry(1L, 10L, 20L)).thenReturn(retryPreparation);
        when(transactionService.questionForMessage(20L)).thenReturn("the retained question");
        when(attemptRepository.findById(31L)).thenReturn(Optional.of(attempt));
        when(attemptRepository.claimExecution(31L, AskAttemptStatus.PENDING, AskAttemptStatus.RUNNING)).thenReturn(1);
        when(attemptRepository.save(any(AskAttempt.class))).thenAnswer(inv -> { AskAttempt value = inv.getArgument(0); if (value.getStatus() == AskAttemptStatus.COMPLETE) completed.countDown(); return value; });
        when(chatService.askPrepared(eq(1L), any(AskKnowledgeRequest.class), eq(retryPreparation)))
                .thenAnswer(inv -> { finished.countDown(); return new AskKnowledgeResponse(10L, "retried answer", true, List.of()); });

        service.retry(1L, 31L);

        assertTrue(finished.await(3, TimeUnit.SECONDS));
        assertTrue(completed.await(3, TimeUnit.SECONDS));
        verify(transactionService).prepareRetry(1L, 10L, 20L);
        verify(transactionService).questionForMessage(20L);
        verify(transactionService, never()).prepareConversation(anyLong(), any());
        assertEquals(AskAttemptStatus.COMPLETE, attempt.getStatus());
    }

    @Test
    void completeOrRunningRetryDoesNotExecuteAgain() {
        for (AskAttemptStatus status : List.of(AskAttemptStatus.COMPLETE, AskAttemptStatus.RUNNING)) {
            AskAttempt attempt = attempt(40L + status.ordinal(), status);
            when(attemptRepository.findByIdAndOwnerId(attempt.getId(), 1L)).thenReturn(Optional.of(attempt));
            service.retry(1L, attempt.getId());
            verify(attemptRepository, never()).claimRetry(anyLong(), anyLong(), any(), any());
            verify(transactionService, never()).prepareRetry(anyLong(), anyLong(), anyLong());
        }
        verifyNoInteractions(chatService);
    }

    @Test
    void concurrentRetryClaimsHaveExactlyOneWinner() throws Exception {
        AtomicInteger claims = new AtomicInteger();
        AtomicInteger winners = new AtomicInteger();
        when(attemptRepository.findByIdAndOwnerId(50L, 1L)).thenAnswer(inv -> Optional.of(attempt(50L, AskAttemptStatus.FAILED)));
        when(attemptRepository.claimRetry(50L, 1L, AskAttemptStatus.FAILED, AskAttemptStatus.PENDING))
                .thenAnswer(inv -> { int call = claims.getAndIncrement(); if (call == 0) winners.incrementAndGet(); return call == 0 ? 1 : 0; });
        when(transactionService.prepareRetry(1L, 10L, 20L)).thenReturn(preparation);
        when(attemptRepository.findByIdAndOwnerId(50L, 1L)).thenReturn(Optional.of(attempt(50L, AskAttemptStatus.FAILED)));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> service.retry(1L, 50L));
            Future<?> second = executor.submit(() -> service.retry(1L, 50L));
            first.get(3, TimeUnit.SECONDS);
            second.get(3, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        assertEquals(2, claims.get(), "both concurrent callers attempt the conditional claim");
        assertEquals(1, winners.get(), "the conditional DB claim has exactly one winner");
        verify(transactionService, times(1)).prepareRetry(1L, 10L, 20L);
    }

    @Test
    void persistedTerminalStatesAreTruthfulAndOrphansBecomeInterrupted() throws Exception {
        for (AskAttemptStatus status : List.of(AskAttemptStatus.COMPLETE, AskAttemptStatus.FAILED)) {
            AskAttempt attempt = attempt(60L + status.ordinal(), status);
            when(attemptRepository.findByIdAndOwnerId(attempt.getId(), 1L)).thenReturn(Optional.of(attempt));
            try (MockedConstruction<SseEmitter> construction = mockConstruction(SseEmitter.class)) {
                service.subscribe(1L, attempt.getId(), null);
                SseEmitter emitter = construction.constructed().getFirst();
                verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
                verify(emitter).complete();
            }
            verify(attemptRepository, never()).markInterruptedIfActive(anyLong(), anyLong(), any(), any(), any(), any());
        }

        AskAttempt orphan = attempt(70L, AskAttemptStatus.RUNNING);
        when(attemptRepository.findByIdAndOwnerId(70L, 1L)).thenReturn(Optional.of(orphan));
        when(attemptRepository.markInterruptedIfActive(70L, 1L, AskAttemptStatus.PENDING, AskAttemptStatus.RUNNING,
                AskAttemptStatus.FAILED, AskFailureCategory.INTERRUPTED)).thenAnswer(inv -> {
                    orphan.markFailed(AskFailureCategory.INTERRUPTED, null);
                    return 1;
                });
        try (MockedConstruction<SseEmitter> construction = mockConstruction(SseEmitter.class)) {
            service.subscribe(1L, 70L, null);
            verify(construction.constructed().getFirst()).complete();
        }
        verify(transactionService).markUserFailed(20L, AskFailureCategory.INTERRUPTED);
        assertEquals(AskFailureCategory.INTERRUPTED, orphan.getFailureCategory());
    }

    @Test
    void ownerIsolationAppliesToStatusRetrySubscribeAndUsage() {
        when(attemptRepository.findByIdAndOwnerId(80L, 2L)).thenReturn(Optional.empty());
        assertThrows(com.groupsync.backend.shared.exception.NotFoundException.class, () -> service.status(2L, 80L));
        assertThrows(com.groupsync.backend.shared.exception.NotFoundException.class, () -> service.retry(2L, 80L));
        assertThrows(com.groupsync.backend.shared.exception.NotFoundException.class, () -> service.subscribe(2L, 80L, null));
        when(usageRepository.findByOwnerIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(eq(2L), any(Instant.class))).thenReturn(List.of());
        service.usage(2L);
        verify(usageRepository).findByOwnerIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(eq(2L), any(Instant.class));
    }

    @Test
    void runtimeReplayHonorsLastSequenceAndTerminalClosesEmitter() throws Exception {
        AskAttemptService.RuntimeAttempt runtime = new AskAttemptService.RuntimeAttempt(90L);
        SseEmitter emitter = mock(SseEmitter.class);
        runtime.add(emitter, 0);
        runtime.publish(AskTraceStage.QUERY_RECEIVED, null, System.currentTimeMillis());
        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        runtime.publish(AskTraceStage.COMPLETE, null, System.currentTimeMillis());
        verify(emitter).complete();
        SseEmitter replayAfterTerminal = mock(SseEmitter.class);
        runtime.add(replayAfterTerminal, 2);
        verify(replayAfterTerminal).complete();
        verify(replayAfterTerminal, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    private void runFailure(Throwable error, AskFailureCategory expected) throws Exception {
        AskAttempt attempt = attempt(30L, AskAttemptStatus.PENDING);
        CountDownLatch finished = new CountDownLatch(1);
        CountDownLatch failurePersisted = new CountDownLatch(1);
        CountDownLatch failedAttemptPersisted = new CountDownLatch(1);
        when(transactionService.prepareConversation(1L, request)).thenReturn(preparation);
        when(attemptRepository.save(any(AskAttempt.class))).thenAnswer(inv -> {
            AskAttempt value = inv.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 30L);
            if (value.getStatus() == AskAttemptStatus.FAILED) failedAttemptPersisted.countDown();
            return value;
        });
        when(attemptRepository.findById(30L)).thenReturn(Optional.of(attempt));
        when(attemptRepository.claimExecution(30L, AskAttemptStatus.PENDING, AskAttemptStatus.RUNNING)).thenReturn(1);
        doAnswer(inv -> { failurePersisted.countDown(); return null; }).when(transactionService).markUserFailed(20L, expected);
        doAnswer(inv -> { finished.countDown(); throw error; }).when(chatService).askPrepared(1L, request, preparation);

        service.start(1L, request);

        assertTrue(finished.await(3, TimeUnit.SECONDS));
        assertTrue(failurePersisted.await(3, TimeUnit.SECONDS));
        assertTrue(failedAttemptPersisted.await(3, TimeUnit.SECONDS));
        verify(transactionService).markUserFailed(20L, expected);
        verify(transactionService, never()).persistAssistantResult(anyLong(), anyLong(), anyString(), anyList());
        assertEquals(AskAttemptStatus.FAILED, attempt.getStatus());
        assertEquals(expected, attempt.getFailureCategory());
    }

    private AskAttempt attempt(Long id, AskAttemptStatus status) {
        AskAttempt attempt = new AskAttempt(owner, 10L, 20L);
        ReflectionTestUtils.setField(attempt, "id", id);
        if (status == AskAttemptStatus.FAILED) attempt.markFailed(AskFailureCategory.PROVIDER, null);
        else if (status == AskAttemptStatus.COMPLETE) attempt.markComplete(null);
        else if (status == AskAttemptStatus.RUNNING) attempt.markRunning();
        return attempt;
    }
}
