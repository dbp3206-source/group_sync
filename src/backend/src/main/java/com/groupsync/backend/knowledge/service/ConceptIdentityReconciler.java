package com.groupsync.backend.knowledge.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.*;
import com.groupsync.backend.knowledge.rag.EmbeddingProvider;
import com.groupsync.backend.knowledge.service.CollectionLearningPathModel.*;
import org.springframework.stereotype.Component;

/** Conservative one-to-one identity matching in the production Gemini embedding space. */
@Component
public class ConceptIdentityReconciler {
    static final double EQUIVALENCE_THRESHOLD = 0.92d;
    static final double AMBIGUITY_MARGIN = 0.015d;
    private final EmbeddingProvider embeddings;

    public ConceptIdentityReconciler(EmbeddingProvider embeddings) {
        this.embeddings = embeddings;
    }

    public record Result(LearningPlan plan, Set<Long> retiredConceptIds, int matchedCount, int newCount) { }

    public Result reconcile(LearningPlan input, Snapshot snapshot) {
        Map<Long, Long> chunkResources = new HashMap<>();
        for (ResourceSnapshot resource : snapshot.resources()) {
            for (EvidenceChunk chunk : resource.chunks()) chunkResources.put(chunk.chunkId(), chunk.resourceId());
        }
        List<ConceptPlan> candidates = deduplicateExact(input.modules());
        List<String> labels = new ArrayList<>();
        for (ConceptPlan candidate : candidates) labels.add(candidate.title());
        for (ExistingConcept existing : snapshot.existingConcepts()) labels.add(existing.title());
        List<float[]> vectors = embeddings.embedSemanticTexts(labels);
        if (vectors.size() != labels.size()) throw new IllegalStateException("Concept embedding count mismatch.");

        deduplicateSemantic(input.modules(), candidates, vectors);
        candidates = distinctConcepts(input.modules());

        // Re-embed after semantic candidate merging so indexes remain explicit and deterministic.
        labels = new ArrayList<>();
        for (ConceptPlan candidate : candidates) labels.add(candidate.title());
        for (ExistingConcept existing : snapshot.existingConcepts()) labels.add(existing.title());
        vectors = embeddings.embedSemanticTexts(labels);
        if (vectors.size() != labels.size()) throw new IllegalStateException("Concept reconciliation embedding count mismatch.");

        Set<Long> usedExisting = new HashSet<>();
        int matched = 0;
        for (int candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
            ConceptPlan candidate = candidates.get(candidateIndex);
            ExistingConcept exact = snapshot.existingConcepts().stream()
                    .filter(existing -> !usedExisting.contains(existing.id()))
                    .filter(existing -> equivalentText(candidate.title(), existing.title())
                            || Objects.equals(stableKey(candidate.title()), existing.stableKey()))
                    .findFirst().orElse(null);
            ExistingConcept selected = exact;
            if (selected == null) {
                Set<Long> candidateResources = candidate.sourceChunkIds().stream()
                        .map(chunkResources::get).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
                List<ScoredExisting> scores = new ArrayList<>();
                for (int existingIndex = 0; existingIndex < snapshot.existingConcepts().size(); existingIndex++) {
                    ExistingConcept existing = snapshot.existingConcepts().get(existingIndex);
                    if (usedExisting.contains(existing.id())) continue;
                    double score = SemanticSimilarity.cosine(vectors.get(candidateIndex), vectors.get(candidates.size() + existingIndex));
                    if (score >= EQUIVALENCE_THRESHOLD) {
                        long sourceOverlap = candidateResources.stream().filter(existing.evidenceResourceIds()::contains).count();
                        scores.add(new ScoredExisting(existing, score, sourceOverlap));
                    }
                }
                scores.sort(Comparator.comparingDouble(ScoredExisting::score).reversed()
                        .thenComparing(score -> score.concept().id()));
                if (scores.size() == 1 || (scores.size() > 1 && scores.get(0).score() - scores.get(1).score() >= AMBIGUITY_MARGIN)) {
                    selected = scores.getFirst().concept();
                } else if (scores.size() > 1) {
                    double bestScore = scores.getFirst().score();
                    List<ScoredExisting> ambiguous = scores.stream()
                            .filter(score -> bestScore - score.score() < AMBIGUITY_MARGIN)
                            .sorted(Comparator.comparingLong(ScoredExisting::sourceOverlap).reversed()
                                    .thenComparing(Comparator.comparingDouble(ScoredExisting::score).reversed())
                                    .thenComparing(score -> score.concept().id()))
                            .toList();
                    if (ambiguous.getFirst().sourceOverlap() > 0
                            && ambiguous.getFirst().sourceOverlap() > ambiguous.get(1).sourceOverlap()) {
                        selected = ambiguous.getFirst().concept();
                    }
                }
            }
            if (selected != null) {
                candidate.setExistingId(selected.id());
                candidate.setStableKey(selected.stableKey() == null || selected.stableKey().isBlank()
                        ? stableKey(candidate.title()) : selected.stableKey());
                usedExisting.add(selected.id());
                matched++;
            } else {
                candidate.setStableKey(stableKey(candidate.title()));
            }
        }

        Set<Long> retired = new LinkedHashSet<>();
        for (ExistingConcept existing : snapshot.existingConcepts()) {
            if (!usedExisting.contains(existing.id())) retired.add(existing.id());
        }
        return new Result(input, Set.copyOf(retired), matched, candidates.size() - matched);
    }

    private List<ConceptPlan> deduplicateExact(List<ModulePlan> modules) {
        List<ConceptPlan> representatives = new ArrayList<>();
        Map<ConceptPlan, ConceptPlan> replacements = new IdentityHashMap<>();
        for (ModulePlan module : modules) {
            for (ConceptPlan candidate : module.concepts()) {
                ConceptPlan equivalent = representatives.stream()
                        .filter(existing -> equivalentText(existing.title(), candidate.title()))
                        .findFirst().orElse(null);
                if (equivalent == null) representatives.add(candidate);
                else {
                    equivalent.mergeEvidence(candidate);
                    replacements.put(candidate, equivalent);
                }
            }
        }
        pruneDuplicates(modules, replacements);
        return distinctConcepts(modules);
    }

    private void deduplicateSemantic(List<ModulePlan> modules, List<ConceptPlan> candidates, List<float[]> vectors) {
        Map<ConceptPlan, ConceptPlan> replacements = new IdentityHashMap<>();
        for (int i = 0; i < candidates.size(); i++) {
            if (replacements.containsKey(candidates.get(i))) continue;
            for (int j = i + 1; j < candidates.size(); j++) {
                ConceptPlan right = candidates.get(j);
                if (replacements.containsKey(right)) continue;
                double similarity = SemanticSimilarity.cosine(vectors.get(i), vectors.get(j));
                if (similarity >= EQUIVALENCE_THRESHOLD) {
                    candidates.get(i).mergeEvidence(right);
                    replacements.put(right, candidates.get(i));
                }
            }
        }
        pruneDuplicates(modules, replacements);
    }

    private void pruneDuplicates(List<ModulePlan> modules, Map<ConceptPlan, ConceptPlan> replacements) {
        Set<ConceptPlan> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ModulePlan module : modules) {
            module.concepts().removeIf(concept -> replacements.containsKey(concept) || !seen.add(concept));
        }
        modules.removeIf(module -> module.concepts().isEmpty());
    }

    private List<ConceptPlan> distinctConcepts(List<ModulePlan> modules) {
        return modules.stream().flatMap(module -> module.concepts().stream()).toList();
    }

    static boolean equivalentText(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        if (normalizedLeft.equals(normalizedRight)) return true;
        String leftAcronym = acronym(normalizedLeft);
        String rightAcronym = acronym(normalizedRight);
        return normalizedLeft.equals(rightAcronym) || normalizedRight.equals(leftAcronym)
                || (!leftAcronym.isBlank() && leftAcronym.equals(rightAcronym)
                    && (normalizedLeft.split(" ").length > 1 || normalizedRight.split(" ").length > 1));
    }

    static String normalize(String value) {
        if (value == null) return "";
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return ascii.toLowerCase(Locale.ROOT)
                .replace('&', ' ')
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\b(fundamentals?|architecture|concepts?|overview|introduction|intro)\\b", " ")
                .replaceAll("\\s+", " ").trim();
    }

    static String acronym(String normalized) {
        if (normalized == null || normalized.isBlank()) return "";
        String[] words = normalized.split(" ");
        if (words.length == 1) return words[0];
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!Set.of("and", "of", "the", "for", "to", "in").contains(word) && !word.isBlank()) result.append(word.charAt(0));
        }
        return result.toString();
    }

    static String stableKey(String title) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalize(title).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 40);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create concept identity.", exception);
        }
    }

    private record ScoredExisting(ExistingConcept concept, double score, long sourceOverlap) { }
}
