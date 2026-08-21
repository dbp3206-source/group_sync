package com.groupsync.backend.knowledge.rag;
import java.util.Optional;

public interface LanguageModelClient {
    String answer(String groundedPrompt);
    default Optional<TokenUsage> lastUsage() { return Optional.empty(); }
}
