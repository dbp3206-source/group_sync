package com.groupsync.backend.knowledge.service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.groupsync.backend.knowledge.dto.*;
import com.groupsync.backend.knowledge.model.*;
import com.groupsync.backend.knowledge.rag.LanguageModelClient;
import com.groupsync.backend.knowledge.rag.QueryMode;
import com.groupsync.backend.knowledge.rag.TokenUsage;
import com.groupsync.backend.knowledge.repository.*;
import com.groupsync.backend.shared.exception.NotFoundException;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@Service
public class AskAttemptService {
    private final KnowledgeChatService chatService;
    private final KnowledgeChatTransactionService transactionService;
    private final AskAttemptRepository attemptRepository;
    private final AiUsageEventRepository usageRepository;
    private final UserAccountRepository userRepository;
    private final LanguageModelClient languageModelClient;
    private final GeminiPropertiesAdapter modelConfig;
    private final LocalUsageClassifier usageClassifier;
    private final ConcurrentMap<Long, RuntimeAttempt> runtimes = new ConcurrentHashMap<>();

    public AskAttemptService(KnowledgeChatService chatService,
                             KnowledgeChatTransactionService transactionService,
                             AskAttemptRepository attemptRepository,
                             AiUsageEventRepository usageRepository,
                             UserAccountRepository userRepository,
                             LanguageModelClient languageModelClient,
                             com.groupsync.backend.knowledge.rag.GeminiProperties properties,
                             LocalUsageClassifier usageClassifier) {
        this.chatService = chatService; this.transactionService = transactionService; this.attemptRepository = attemptRepository;
        this.usageRepository = usageRepository; this.userRepository = userRepository; this.languageModelClient = languageModelClient;
        this.modelConfig = new GeminiPropertiesAdapter(properties.chatModel());
        this.usageClassifier = usageClassifier;
    }

    public AskAttemptResponse start(Long ownerId, AskKnowledgeRequest request) {
        KnowledgeChatTransactionService.ChatPreparation prep = transactionService.prepareConversation(ownerId, request);
        if (prep.userMessageId() == null) throw new IllegalStateException("The question turn could not be persisted.");
        UserAccount owner = userRepository.findById(ownerId).orElseThrow(() -> new NotFoundException("User not found."));
        AskAttempt attempt = attemptRepository.save(new AskAttempt(owner, prep.sessionId(), prep.userMessageId()));
        RuntimeAttempt runtime = new RuntimeAttempt(attempt.getId());
        runtimes.put(attempt.getId(), runtime);
        execute(ownerId, request, prep, attempt.getId(), runtime);
        return response(attempt);
    }

    public AskAttemptResponse retry(Long ownerId, Long attemptId) {
        AskAttempt attempt = attemptRepository.findByIdAndOwnerId(attemptId, ownerId)
                .orElseThrow(() -> new NotFoundException("Ask attempt not found."));
        if (attempt.getStatus() != AskAttemptStatus.FAILED) return response(attempt);

        int claimed = attemptRepository.claimRetry(attemptId, ownerId, AskAttemptStatus.FAILED, AskAttemptStatus.PENDING);
        if (claimed != 1) {
            return response(attemptRepository.findByIdAndOwnerId(attemptId, ownerId).orElse(attempt));
        }
        KnowledgeChatTransactionService.ChatPreparation prep = transactionService.prepareRetry(ownerId, attempt.getSessionId(), attempt.getUserMessageId());
        RuntimeAttempt runtime = new RuntimeAttempt(attemptId);
        runtimes.put(attemptId, runtime);
        AskKnowledgeRequest request = new AskKnowledgeRequest(attempt.getSessionId(), prepQuestion(prep, attempt), prep.scopeType(),
                prep.scopeType() == com.groupsync.backend.knowledge.rag.RetrievalScope.THIS_RESOURCE ? prep.thisResourceId() : null,
                prep.scopeType() == com.groupsync.backend.knowledge.rag.RetrievalScope.SELECTED_RESOURCES ? prep.selectedResourceIds() : List.of(),
                prep.collectionId(), null);
        execute(ownerId, request, prep, attemptId, runtime);
        return response(attemptRepository.findByIdAndOwnerId(attemptId, ownerId).orElse(attempt));
    }

    private String prepQuestion(KnowledgeChatTransactionService.ChatPreparation prep, AskAttempt attempt) {
        return transactionService.questionForMessage(attempt.getUserMessageId());
    }

    private void execute(Long ownerId, AskKnowledgeRequest request, KnowledgeChatTransactionService.ChatPreparation prep,
                         Long attemptId, RuntimeAttempt runtime) {
        CompletableFuture.runAsync(() -> {
            long start = System.currentTimeMillis();
            AskAttempt attempt = attemptRepository.findById(attemptId).orElse(null);
            if (attempt == null) return;
            if (attemptRepository.claimExecution(attemptId, AskAttemptStatus.PENDING, AskAttemptStatus.RUNNING) != 1) return;
            try (AskTraceContext.Scope ignored = AskTraceContext.open((stage, details) -> runtime.publish(stage, details, start))) {
                AskKnowledgeResponse response = chatService.askPrepared(ownerId, request, prep);
                QueryMode mode = response.trace() == null ? null : response.trace().mode();
                runtime.publish(AskTraceStage.COMPLETE, detailsFrom(response, mode), start);
                AskAttempt current = attemptRepository.findById(attemptId).orElse(attempt);
                current.markComplete(mode); attemptRepository.save(current);
                persistUsage(ownerId, attemptId, response, null, System.currentTimeMillis() - start);
            } catch (Throwable error) {
                AskFailureCategory category = AskFailureClassifier.classify(error);
                transactionService.markUserFailed(prep.userMessageId(), category);
                AskAttempt current = attemptRepository.findById(attemptId).orElse(attempt);
                current.markFailed(category, null); attemptRepository.save(current);
                runtime.publish(AskTraceStage.FAILED, new AskTraceTechnicalDetails(null, null, null, null, null, null, null, null, null, null, modelConfig.chatModel(), category.name()), start);
                persistUsage(ownerId, attemptId, null, category, System.currentTimeMillis() - start);
            }
        });
    }

    public SseEmitter subscribe(Long ownerId, Long attemptId, String lastEventId) {
        AskAttempt attempt = attemptRepository.findByIdAndOwnerId(attemptId, ownerId)
                .orElseThrow(() -> new NotFoundException("Ask attempt not found."));
        RuntimeAttempt runtime = runtimes.get(attemptId);
        SseEmitter emitter = new SseEmitter(0L);
        if (runtime == null) {
            AskAttempt recovered = recoverMissingRuntime(ownerId, attempt);
            try {
                if (parseLastEventId(lastEventId) < 1) {
                    emitter.send(SseEmitter.event().id("1").name("ask-trace").data(persistedTerminalEvent(attemptId, recovered)));
                }
                emitter.complete();
            }
            catch (Exception ignored) { emitter.completeWithError(ignored); }
            return emitter;
        }
        runtime.add(emitter, parseLastEventId(lastEventId));
        emitter.onCompletion(() -> runtime.remove(emitter));
        emitter.onTimeout(() -> runtime.remove(emitter));
        return emitter;
    }

    public AskAttemptResponse status(Long ownerId, Long attemptId) {
        return response(attemptRepository.findByIdAndOwnerId(attemptId, ownerId).orElseThrow(() -> new NotFoundException("Ask attempt not found.")));
    }

    public AiUsageResponse usage(Long ownerId) {
        Instant from = Instant.now().minus(Duration.ofHours(24));
        List<AiUsageEvent> events = usageRepository.findByOwnerIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(ownerId, from);
        long completed = events.stream().filter(e -> "COMPLETE".equals(e.getRequestStatus())).count();
        long rateLimits = events.stream().filter(e -> e.getFailureCategory() == AskFailureCategory.RATE_LIMIT).count();
        long failed = events.stream().filter(e -> "FAILED".equals(e.getRequestStatus())).count();
        long prompt = events.stream().filter(e -> e.getPromptTokens() != null).mapToLong(e -> e.getPromptTokens()).sum();
        long output = events.stream().filter(e -> e.getOutputTokens() != null).mapToLong(e -> e.getOutputTokens()).sum();
        long total = events.stream().filter(e -> e.getTotalTokens() != null).mapToLong(e -> e.getTotalTokens()).sum();
        LocalUsageStatus localStatus = usageClassifier.classify(events, Instant.now());
        return new AiUsageResponse(completed, rateLimits, failed, prompt, output, total, false, "UNKNOWN", null,
                events.isEmpty() ? null : events.getFirst().getCreatedAt(), localStatus,
                LocalUsageClassifier.WINDOW, "GENERATION_CALL_ONLY");
    }

    private void persistUsage(Long ownerId, Long attemptId, AskKnowledgeResponse response, AskFailureCategory failure, long durationMs) {
        UserAccount owner = userRepository.findById(ownerId).orElse(null); if (owner == null) return;
        TokenUsage tokens = languageModelClient.lastUsage().orElse(null);
        QueryMode mode = response == null || response.trace() == null ? null : response.trace().mode();
        usageRepository.save(new AiUsageEvent(owner, attemptId, "Gemini", modelConfig.chatModel(), failure == null ? "COMPLETE" : "FAILED", mode,
                tokens == null ? null : tokens.promptTokens(), tokens == null ? null : tokens.outputTokens(), tokens == null ? null : tokens.totalTokens(),
                response == null || response.trace() == null || response.trace().contextBudget() == null ? null : response.trace().contextBudget().charactersUsed(),
                response == null || response.trace() == null || response.trace().retrieval() == null ? null : response.trace().retrieval().totalCandidates(), durationMs, failure));
    }

    private AskTraceTechnicalDetails detailsFrom(AskKnowledgeResponse response, QueryMode mode) {
        RagExecutionTrace trace = response.trace();
        return new AskTraceTechnicalDetails(mode == null ? null : mode.name(), trace == null ? null : trace.operation().name(),
                trace == null || trace.retrieval() == null ? null : trace.retrieval().semanticCandidates(),
                trace == null || trace.retrieval() == null ? null : trace.retrieval().lexicalCandidates(),
                trace == null || trace.retrieval() == null ? null : trace.retrieval().totalCandidates(),
                trace == null || trace.fusion() == null ? null : trace.fusion().selectedChildren(),
                trace == null || trace.contextBudget() == null ? null : trace.contextBudget().parentsUsed(),
                trace == null || trace.contextBudget() == null ? null : trace.contextBudget().charactersUsed(),
                trace == null || trace.contextBudget() == null ? null : trace.contextBudget().maxCharactersBudget(),
                response.citations().size(), modelConfig.chatModel(), null);
    }

    private AskAttemptResponse response(AskAttempt attempt) { return new AskAttemptResponse(attempt.getId(), attempt.getSessionId(), attempt.getUserMessageId(), attempt.getStatus(), attempt.getFailureCategory(), attempt.getCreatedAt(), attempt.getCompletedAt()); }
    private long parseLastEventId(String value) { try { return value == null ? 0 : Long.parseLong(value); } catch (NumberFormatException ignored) { return 0; } }
    private AskAttempt recoverMissingRuntime(Long ownerId, AskAttempt attempt) {
        if (attempt.getStatus() == AskAttemptStatus.PENDING || attempt.getStatus() == AskAttemptStatus.RUNNING) {
            int recovered = attemptRepository.markInterruptedIfActive(attempt.getId(), ownerId,
                    AskAttemptStatus.PENDING, AskAttemptStatus.RUNNING, AskAttemptStatus.FAILED,
                    AskFailureCategory.INTERRUPTED);
            if (recovered == 1) transactionService.markUserFailed(attempt.getUserMessageId(), AskFailureCategory.INTERRUPTED);
        }
        return attemptRepository.findByIdAndOwnerId(attempt.getId(), ownerId).orElse(attempt);
    }

    private AskTraceEvent persistedTerminalEvent(Long attemptId, AskAttempt attempt) {
        boolean complete = attempt.getStatus() == AskAttemptStatus.COMPLETE;
        AskTraceStage stage = complete ? AskTraceStage.COMPLETE : AskTraceStage.FAILED;
        AskTraceStatus status = complete ? AskTraceStatus.COMPLETE : AskTraceStatus.FAILED;
        String message = complete ? "Lần hỏi đã hoàn tất." : "Lượt hỏi chưa hoàn tất; câu hỏi vẫn được giữ lại để thử lại.";
        return new AskTraceEvent(attemptId, 1, stage, status, Instant.now(), 0, message,
                "persisted_terminal_state", new AskTraceTechnicalDetails(null, null, null, null, null, null, null, null, null, null,
                modelConfig.chatModel(), attempt.getFailureCategory() == null ? null : attempt.getFailureCategory().name()));
    }

    private record GeminiPropertiesAdapter(String chatModel) { }

    static final class RuntimeAttempt {
        private final Long attemptId; private final AtomicLong sequence = new AtomicLong(); private final List<AskTraceEvent> events = new CopyOnWriteArrayList<>(); private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
        RuntimeAttempt(Long attemptId) { this.attemptId = attemptId; }
        void publish(AskTraceStage stage, AskTraceTechnicalDetails details, long startedAt) {
            AskTraceStatus status = stage == AskTraceStage.FAILED ? AskTraceStatus.FAILED : stage == AskTraceStage.COMPLETE ? AskTraceStatus.COMPLETE : AskTraceStatus.RUNNING;
            String beginner = switch (stage) {
                case QUERY_RECEIVED -> "Đã nhận câu hỏi.";
                case PLAN_READY -> "Đã chọn cách tìm kiếm phù hợp.";
                case FILTERS_APPLIED -> "Đã lọc đúng phạm vi nguồn trước khi tìm.";
                case STRUCTURED_OPERATION_COMPLETE -> "Đã đọc dữ kiện dạng bảng trực tiếp.";
                case SEMANTIC_RETRIEVAL_COMPLETE -> "Đã tìm các đoạn có ý nghĩa gần nhất.";
                case LEXICAL_RETRIEVAL_COMPLETE -> "Đã tìm thêm theo từ khóa.";
                case RRF_COMPLETE -> "Đã gộp các kết quả tìm kiếm.";
                case PARENT_CONTEXT_COMPLETE -> "Đã mở rộng đoạn nhỏ thành ngữ cảnh đầy đủ hơn.";
                case CONTEXT_READY -> "Đã chuẩn bị ngữ cảnh có thể kiểm chứng.";
                case GENERATION_STARTED -> "Đang viết câu trả lời dựa trên bằng chứng.";
                case GENERATION_COMPLETE -> "Đã nhận câu trả lời từ mô hình.";
                case CITATIONS_VERIFIED -> "Đã đối chiếu các trích dẫn với nguồn.";
                case COMPLETE -> "Hoàn tất lượt hỏi.";
                case FAILED -> "Lượt hỏi chưa hoàn tất; câu hỏi vẫn được giữ lại để thử lại.";
            };
            AskTraceEvent event = new AskTraceEvent(attemptId, sequence.incrementAndGet(), stage, status, Instant.now(), System.currentTimeMillis() - startedAt, beginner, stage.name().toLowerCase(Locale.ROOT), details);
            events.add(event);
            for (SseEmitter emitter : emitters) try { emitter.send(SseEmitter.event().id(String.valueOf(event.sequence())).name("ask-trace").data(event)); if (status != AskTraceStatus.RUNNING) { emitter.complete(); emitters.remove(emitter); } } catch (Exception ignored) { emitters.remove(emitter); }
        }
        void add(SseEmitter emitter, long after) { emitters.add(emitter); for (AskTraceEvent event : events) if (event.sequence() > after) try { emitter.send(SseEmitter.event().id(String.valueOf(event.sequence())).name("ask-trace").data(event)); } catch (Exception ignored) { emitters.remove(emitter); } if (!events.isEmpty() && events.getLast().status() != AskTraceStatus.RUNNING) { emitter.complete(); emitters.remove(emitter); } }
        void remove(SseEmitter emitter) { emitters.remove(emitter); }
    }
}
