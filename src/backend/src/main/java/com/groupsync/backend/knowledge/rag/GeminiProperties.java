package com.groupsync.backend.knowledge.rag;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(String apiKey, String chatModel, String qualityModel, String embeddingModel, int embeddingDimensions, int ragTopK) { }
