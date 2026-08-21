package com.groupsync.backend.knowledge.rag;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GeminiLanguageModelClient implements LanguageModelClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiLanguageModelClient.class);
    private static final int MAX_RETRIES = 2;
    private static final long INITIAL_BACKOFF_MS = 500L;

    private final GeminiProperties properties;
    private final Client sharedClient;
    private final ThreadLocal<TokenUsage> lastUsage = new ThreadLocal<>();

    public GeminiLanguageModelClient(GeminiProperties properties, @Autowired(required = false) Client sharedClient) {
        this.properties = properties;
        this.sharedClient = sharedClient;
    }

    private Client getClient() {
        if (sharedClient != null) {
            return sharedClient;
        }
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY must be configured before answers can be generated.");
        }
        return Client.builder()
                .apiKey(properties.apiKey())
                .httpOptions(com.google.genai.types.HttpOptions.builder().timeout(properties.timeoutMillis()).build())
                .build();
    }

    @Override
    public String answer(String groundedPrompt) {
        if (groundedPrompt == null || groundedPrompt.isBlank()) {
            throw new IllegalArgumentException("A grounded prompt is required.");
        }
        lastUsage.remove();
        Client client = getClient();

        GenerateContentResponse response = executeWithRetry(() ->
                client.models.generateContent(properties.chatModel(), groundedPrompt, null)
        );

        response.usageMetadata().ifPresent(usage -> lastUsage.set(new TokenUsage(
                usage.promptTokenCount().orElse(null),
                usage.candidatesTokenCount().orElse(null),
                usage.totalTokenCount().orElse(null)
        )));

        String answer = response.text();
        if (answer == null || answer.isBlank()) {
            throw new IllegalStateException("Gemini did not return answer text.");
        }
        return answer.trim();
    }

    @Override
    public java.util.Optional<TokenUsage> lastUsage() {
        return java.util.Optional.ofNullable(lastUsage.get());
    }

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get() throws Exception;
    }

    private <T> T executeWithRetry(SupplierWithException<T> action) {
        int attempts = 0;
        long backoff = INITIAL_BACKOFF_MS;
        while (true) {
            try {
                attempts++;
                return action.get();
            } catch (Exception ex) {
                if (attempts > MAX_RETRIES || !isTransientError(ex)) {
                    if (ex instanceof RuntimeException re) throw re;
                    throw new IllegalStateException("Gemini generation request failed: " + ex.getMessage(), ex);
                }
                log.warn("Transient error calling Gemini content generation API (attempt {}/{}). Retrying in {}ms: {}",
                        attempts, MAX_RETRIES, backoff, ex.getMessage());
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted during retry backoff", ie);
                }
                backoff *= 2;
            }
        }
    }

    private boolean isTransientError(Throwable ex) {
        if (ex == null) return false;
        String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        if (msg.contains("429") || msg.contains("quota") || msg.contains("resource_exhausted")
                || msg.contains("502") || msg.contains("503") || msg.contains("504")
                || msg.contains("timeout") || msg.contains("timed out") || msg.contains("connection reset")) {
            return true;
        }
        return isTransientError(ex.getCause());
    }
}

