package com.groupsync.backend.knowledge.rag;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.stereotype.Component;

@Component
public class GeminiLanguageModelClient implements LanguageModelClient {
    private final GeminiProperties properties;

    public GeminiLanguageModelClient(GeminiProperties properties) {
        this.properties = properties;
    }

    @Override
    public String answer(String groundedPrompt) {
        if (groundedPrompt == null || groundedPrompt.isBlank()) {
            throw new IllegalArgumentException("A grounded prompt is required.");
        }
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY must be configured before answers can be generated.");
        }

        Client client = Client.builder().apiKey(properties.apiKey()).build();
        GenerateContentResponse response = client.models.generateContent(properties.chatModel(), groundedPrompt, null);
        String answer = response.text();
        if (answer == null || answer.isBlank()) {
            throw new IllegalStateException("Gemini did not return answer text.");
        }
        return answer.trim();
    }
}
