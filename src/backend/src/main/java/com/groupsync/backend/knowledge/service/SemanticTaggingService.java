package com.groupsync.backend.knowledge.service;

import java.util.*;
import com.groupsync.backend.knowledge.dto.DocumentUnderstandingResult;
import com.groupsync.backend.knowledge.dto.TagResponse;
import com.groupsync.backend.knowledge.rag.EmbeddingProvider;
import org.springframework.stereotype.Service;

@Service
public class SemanticTaggingService {
    static final double EQUIVALENT_THRESHOLD = 0.88d;
    private final KnowledgeWorkspaceService workspace;
    private final EmbeddingProvider embeddings;

    public SemanticTaggingService(KnowledgeWorkspaceService workspace, EmbeddingProvider embeddings) {
        this.workspace = workspace;
        this.embeddings = embeddings;
    }

    public record TagDecision(String candidate, Long existingTagId, String canonicalLabel, double confidence) { }

    public List<TagDecision> plan(Long ownerId, DocumentUnderstandingResult understanding) {
        List<String> candidates = SemanticLabelPolicy.usefulTags(understanding.candidateTags());
        if (candidates.isEmpty()) return List.of();
        List<TagResponse> existing = workspace.tags(ownerId);
        List<TagDecision> decisions = new ArrayList<>();

        for (String candidate : candidates) {
            TagResponse exactOrAlias = existing.stream().filter(tag ->
                    SemanticLabelPolicy.equivalenceKey(tag.name()).equals(SemanticLabelPolicy.equivalenceKey(candidate)))
                    .findFirst().orElse(null);
            if (exactOrAlias != null) {
                decisions.add(new TagDecision(candidate, exactOrAlias.id(), exactOrAlias.name(), 1d));
            } else {
                decisions.add(new TagDecision(candidate, null, candidate, 0.84d));
            }
        }

        List<Integer> unmatched = new ArrayList<>();
        for (int i = 0; i < decisions.size(); i++) if (decisions.get(i).existingTagId() == null) unmatched.add(i);
        if (!unmatched.isEmpty() && !existing.isEmpty()) {
            List<String> texts = new ArrayList<>();
            unmatched.forEach(index -> texts.add(decisions.get(index).candidate()));
            existing.forEach(tag -> texts.add(tag.name()));
            List<float[]> vectors = embeddings.embedSemanticTexts(texts);
            if (vectors.size() != texts.size()) throw new IllegalStateException("Semantic tag embedding count mismatch.");
            for (int position = 0; position < unmatched.size(); position++) {
                int decisionIndex = unmatched.get(position);
                double best = -1d;
                TagResponse bestTag = null;
                for (int tagIndex = 0; tagIndex < existing.size(); tagIndex++) {
                    double score = SemanticSimilarity.cosine(vectors.get(position), vectors.get(unmatched.size() + tagIndex));
                    if (score > best) { best = score; bestTag = existing.get(tagIndex); }
                }
                if (bestTag != null && best >= EQUIVALENT_THRESHOLD) {
                    TagDecision original = decisions.get(decisionIndex);
                    decisions.set(decisionIndex, new TagDecision(original.candidate(), bestTag.id(), bestTag.name(), best));
                }
            }
        }

        LinkedHashMap<String, TagDecision> deduplicated = new LinkedHashMap<>();
        for (TagDecision decision : decisions) {
            String key = decision.existingTagId() != null ? "id:" + decision.existingTagId()
                    : "name:" + SemanticLabelPolicy.equivalenceKey(decision.canonicalLabel());
            deduplicated.putIfAbsent(key, decision);
        }
        return deduplicated.values().stream().limit(6).toList();
    }
}
