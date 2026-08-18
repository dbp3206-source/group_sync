package com.groupsync.backend.knowledge.rag;

import com.google.genai.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration providing a singleton, reusable Google GenAI Client instance.
 */
@Configuration
public class GeminiClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(GeminiClientConfiguration.class);

    @Bean
    public Client geminiGenAiClient(GeminiProperties properties) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            log.info("GEMINI_API_KEY is not set. Gemini client will not be initialized.");
            return null;
        }
        log.info("Initializing shared Gemini GenAI Client.");
        return Client.builder().apiKey(properties.apiKey()).build();
    }
}
