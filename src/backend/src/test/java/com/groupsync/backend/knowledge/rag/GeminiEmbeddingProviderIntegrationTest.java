package com.groupsync.backend.knowledge.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class GeminiEmbeddingProviderIntegrationTest {
    @Test
    void createsANormalized768DimensionEmbeddingUsingGemini() {
        GeminiProperties properties = new GeminiProperties(
                System.getenv("GEMINI_API_KEY"),
                "gemini-3.5-flash-lite",
                "gemini-3.5-flash",
                "gemini-embedding-001",
                768,
                5,
                2,
                12,
                60,
                30000);

        float[] embedding = new GeminiEmbeddingProvider(properties, null)
                .embedDocument("KnowledgeOS stores learning resources for grounded retrieval.");

        assertEquals(768, embedding.length);
        assertTrue(Math.abs(squaredMagnitude(embedding) - 1.0d) < 0.001d);
    }

    private double squaredMagnitude(float[] vector) {
        double total = 0;
        for (float value : vector) {
            total += value * value;
        }
        return total;
    }
}
