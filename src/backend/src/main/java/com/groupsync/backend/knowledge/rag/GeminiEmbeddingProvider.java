package com.groupsync.backend.knowledge.rag;

import com.google.genai.Client;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GeminiEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingProvider.class);
    private static final int MAX_RETRIES = 2;
    private static final long INITIAL_BACKOFF_MS = 500L;

    private final GeminiProperties properties;
    private final Client sharedClient;

    public GeminiEmbeddingProvider(GeminiProperties properties, @Autowired(required = false) Client sharedClient) {
        this.properties = properties;
        this.sharedClient = sharedClient;
    }

    @Override
    public float[] embedDocument(String content) {
        return embed(content, "RETRIEVAL_DOCUMENT");
    }

    @Override
    public float[] embedQuery(String content) {
        return embed(content, "RETRIEVAL_QUERY");
    }

    private Client getClient() {
        if (sharedClient != null) {
            return sharedClient;
        }
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY must be configured before embeddings can be created.");
        }
        return Client.builder().apiKey(properties.apiKey()).build();
    }

    private float[] embed(String content, String taskType) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content is required to create an embedding.");
        }
        Client client = getClient();
        EmbedContentConfig config = EmbedContentConfig.builder()
                .outputDimensionality(properties.embeddingDimensions())
                .taskType(taskType)
                .build();

        EmbedContentResponse response = executeWithRetry(() ->
                client.models.embedContent(properties.embeddingModel(), content, config)
        );

        if (response.embeddings().isEmpty()) {
            throw new IllegalStateException("Gemini did not return an embedding.");
        }
        return EmbeddingVectorNormalizer.normalize(
                response.embeddings().get().getFirst().values().orElseThrow(
                        () -> new IllegalStateException("Gemini did not return embedding values.")),
                properties.embeddingDimensions());
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
                    throw new IllegalStateException("Gemini embedding request failed: " + ex.getMessage(), ex);
                }
                log.warn("Transient error calling Gemini embedding API (attempt {}/{}). Retrying in {}ms: {}",
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
