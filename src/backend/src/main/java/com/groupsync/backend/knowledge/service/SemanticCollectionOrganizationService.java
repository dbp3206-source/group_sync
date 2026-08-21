package com.groupsync.backend.knowledge.service;

import java.util.*;
import com.groupsync.backend.knowledge.dto.CollectionResponse;
import com.groupsync.backend.knowledge.dto.DocumentUnderstandingResult;
import com.groupsync.backend.knowledge.dto.OrganizationCollectionSuggestionResponse;
import com.groupsync.backend.knowledge.rag.EmbeddingProvider;
import org.springframework.stereotype.Service;

@Service
public class SemanticCollectionOrganizationService {
    static final double STRONG_THRESHOLD = 0.80d;
    static final double POSSIBLE_THRESHOLD = 0.62d;
    static final double NEW_SUGGESTION_THRESHOLD = 0.82d;
    private final KnowledgeWorkspaceService workspace;
    private final EmbeddingProvider embeddings;

    public SemanticCollectionOrganizationService(KnowledgeWorkspaceService workspace, EmbeddingProvider embeddings) {
        this.workspace = workspace;
        this.embeddings = embeddings;
    }

    public record CollectionPlan(List<OrganizationCollectionSuggestionResponse> strongMatches,
                                 List<OrganizationCollectionSuggestionResponse> possibleMatches,
                                 List<OrganizationCollectionSuggestionResponse> newSuggestions) { }

    public CollectionPlan plan(Long ownerId, DocumentUnderstandingResult understanding) {
        List<CollectionResponse> collections = workspace.collections(ownerId);
        List<String> themes = understanding.broadThemes().stream().filter(this::isBroadTheme).limit(4).toList();
        String document = semanticDocument(understanding);
        List<String> texts = new ArrayList<>();
        texts.add(document);
        collections.forEach(collection -> texts.add(collection.name() + ". " + Objects.toString(collection.description(), "")));
        texts.addAll(themes);
        List<float[]> vectors = embeddings.embedSemanticTexts(texts);
        if (vectors.size() != texts.size()) throw new IllegalStateException("Semantic collection embedding count mismatch.");

        List<OrganizationCollectionSuggestionResponse> strong = new ArrayList<>();
        List<OrganizationCollectionSuggestionResponse> possible = new ArrayList<>();
        for (int i = 0; i < collections.size(); i++) {
            CollectionResponse collection = collections.get(i);
            double score = SemanticSimilarity.cosine(vectors.get(0), vectors.get(i + 1));
            OrganizationCollectionSuggestionResponse response = new OrganizationCollectionSuggestionResponse(
                    collection.name(), collection.id(), score >= STRONG_THRESHOLD ? "Strong semantic match" : "Possible semantic match", score);
            if (score >= STRONG_THRESHOLD) strong.add(response);
            else if (score >= POSSIBLE_THRESHOLD) possible.add(response);
        }
        Comparator<OrganizationCollectionSuggestionResponse> byConfidence =
                Comparator.comparingDouble(OrganizationCollectionSuggestionResponse::confidence).reversed();
        strong.sort(byConfidence);
        possible.sort(byConfidence);

        List<OrganizationCollectionSuggestionResponse> proposed = new ArrayList<>();
        if (strong.isEmpty()) {
            int themeOffset = 1 + collections.size();
            for (int i = 0; i < themes.size(); i++) {
                String theme = themes.get(i);
                double score = SemanticSimilarity.cosine(vectors.get(0), vectors.get(themeOffset + i));
                boolean existingEquivalent = collections.stream().anyMatch(collection ->
                        SemanticLabelPolicy.normalize(collection.name()).equals(SemanticLabelPolicy.normalize(theme)));
                if (!existingEquivalent && score >= NEW_SUGGESTION_THRESHOLD) {
                    proposed.add(new OrganizationCollectionSuggestionResponse(theme, 0L,
                            "Broad semantic theme requiring review", score));
                }
            }
        }
        proposed.sort(byConfidence);
        return new CollectionPlan(strong.stream().limit(2).toList(), possible.stream().limit(3).toList(),
                proposed.stream().limit(2).toList());
    }

    private String semanticDocument(DocumentUnderstandingResult understanding) {
        return understanding.normalizedTitle() + ". " + understanding.summary() + ". Key ideas: "
                + String.join("; ", understanding.keyIdeas()) + ". Tags: " + String.join("; ", understanding.candidateTags());
    }

    private boolean isBroadTheme(String theme) {
        String normalized = SemanticLabelPolicy.normalize(theme);
        if (normalized.isBlank() || normalized.matches(".*\\b(chapter|file|final|notes|lecture)\\b.*")) return false;
        int words = normalized.split(" ").length;
        return words >= 1 && words <= 5 && normalized.length() <= 80;
    }
}
