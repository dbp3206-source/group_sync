package com.groupsync.backend.knowledge.rag;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class GeminiLanguageModelClientIntegrationTest {
    @Test
    void returnsAnAnswerForAGroundedPrompt() {
        GeminiProperties properties = new GeminiProperties(
                System.getenv("GEMINI_API_KEY"),
                "gemini-3.5-flash-lite",
                "gemini-3.5-flash",
                "gemini-embedding-001", 768, 5, 2, 12, 60);
        String answer = new GeminiLanguageModelClient(properties, null).answer("""
                You answer only from the evidence below. If it is insufficient, say so.
                Evidence: KnowledgeOS organizes a personal library of learning resources.
                Question: What does KnowledgeOS organize?
                """);
        assertFalse(answer.isBlank());
    }
}
