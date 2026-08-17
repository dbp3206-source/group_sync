package com.groupsync.backend.knowledge.rag;

import com.google.genai.Client;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;
import org.springframework.stereotype.Component;

@Component
public class GeminiEmbeddingProvider implements EmbeddingProvider {
    private final GeminiProperties properties;

    public GeminiEmbeddingProvider(GeminiProperties properties) {
        this.properties = properties;
    }

    @Override
    public float[] embedDocument(String content) {
        return embed(content, "RETRIEVAL_DOCUMENT");
    }

    @Override
    public float[] embedQuery(String content) {
        return embed(content, "RETRIEVAL_QUERY");
    }

    private float[] embed(String content, String taskType) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content is required to create an embedding.");
        }
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY must be configured before embeddings can be created.");
        }

        Client client = Client.builder().apiKey(properties.apiKey()).build();
        EmbedContentConfig config = EmbedContentConfig.builder()
                .outputDimensionality(properties.embeddingDimensions())
                .taskType(taskType)
                .build();
        EmbedContentResponse response = client.models.embedContent(properties.embeddingModel(), content, config);
        if (response.embeddings().isEmpty()) {
            throw new IllegalStateException("Gemini did not return an embedding.");
        }
        return EmbeddingVectorNormalizer.normalize(
                response.embeddings().get().getFirst().values().orElseThrow(
                        () -> new IllegalStateException("Gemini did not return embedding values.")),
                properties.embeddingDimensions());
    }
}
