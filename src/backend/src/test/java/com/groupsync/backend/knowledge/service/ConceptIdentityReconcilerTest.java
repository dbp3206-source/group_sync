package com.groupsync.backend.knowledge.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.groupsync.backend.knowledge.rag.EmbeddingProvider;
import com.groupsync.backend.knowledge.service.CollectionLearningPathModel.*;

class ConceptIdentityReconcilerTest {
    private ConceptIdentityReconciler reconciler;

    @BeforeEach
    void setUp() {
        EmbeddingProvider embeddings = mock(EmbeddingProvider.class);
        when(embeddings.embedSemanticTexts(anyList())).thenAnswer(invocation -> {
            List<String> labels = invocation.getArgument(0);
            return labels.stream().map(this::vector).toList();
        });
        reconciler = new ConceptIdentityReconciler(embeddings);
    }

    @Test
    void ragAndRetrievalAugmentedGenerationAreOneConcept() {
        assertTrue(ConceptIdentityReconciler.equivalentText("RAG", "Retrieval-Augmented Generation"));
        assertEquals(1, reconcile(List.of(concept("RAG", 101L), concept("Retrieval-Augmented Generation", 201L)), List.of()).plan().modules().getFirst().concepts().size());
    }

    @Test
    void oopAndObjectOrientedProgrammingAreOneConcept() {
        assertTrue(ConceptIdentityReconciler.equivalentText("OOP", "Object-Oriented Programming"));
        assertEquals(1, reconcile(List.of(concept("OOP", 101L), concept("Object-Oriented Programming", 201L)), List.of()).plan().modules().getFirst().concepts().size());
    }

    @Test
    void normalizationAndBcnfRemainDifferent() {
        assertEquals(2, reconcile(List.of(concept("Normalization", 101L), concept("BCNF", 201L)), List.of()).plan().modules().getFirst().concepts().size());
    }

    @Test
    void sqlAndPostgresqlRemainDifferent() {
        assertEquals(2, reconcile(List.of(concept("SQL", 101L), concept("PostgreSQL", 201L)), List.of()).plan().modules().getFirst().concepts().size());
    }

    @Test
    void identicalConceptAcrossThreeResourcesKeepsAllEvidence() {
        ConceptIdentityReconciler.Result result = reconcile(List.of(concept("RAG", 101L), concept("RAG", 201L), concept("RAG", 301L)), List.of());
        ConceptPlan merged = result.plan().modules().getFirst().concepts().getFirst();
        assertEquals(Set.of(101L, 201L, 301L), merged.sourceChunkIds());
        assertEquals(1, result.newCount());
    }

    @Test
    void checkedConceptKeepsSameIdentity() {
        ExistingConcept existing = existing(11L, "Retrieval-Augmented Generation", "CHECKED");
        ConceptPlan candidate = concept("RAG fundamentals", 101L);
        ConceptIdentityReconciler.Result result = reconcile(List.of(candidate), List.of(existing));
        assertEquals(11L, result.plan().modules().getFirst().concepts().getFirst().existingId());
        assertEquals("CHECKED", existing.studyStatus());
    }

    @Test
    void reviewNeededConceptKeepsSameIdentity() {
        ExistingConcept existing = existing(12L, "OOP", "REVIEW_NEEDED");
        assertEquals(12L, reconcile(List.of(concept("Object-Oriented Programming", 101L)), List.of(existing)).plan().modules().getFirst().concepts().getFirst().existingId());
        assertEquals("REVIEW_NEEDED", existing.studyStatus());
    }

    @Test
    void learningConceptKeepsSameIdentity() {
        ExistingConcept existing = existing(13L, "RRF", "LEARNING");
        assertEquals(13L, reconcile(List.of(concept("Reciprocal Rank Fusion", 101L)), List.of(existing)).plan().modules().getFirst().concepts().getFirst().existingId());
        assertEquals("LEARNING", existing.studyStatus());
    }

    @Test
    void notStartedConceptKeepsSameIdentity() {
        ExistingConcept existing = existing(14L, "SQL", "NOT_STARTED");
        assertEquals(14L, reconcile(List.of(concept("SQL", 101L)), List.of(existing)).plan().modules().getFirst().concepts().getFirst().existingId());
    }

    @Test
    void newConceptHasNoExistingIdAndGetsStableKey() {
        ConceptPlan concept = reconcile(List.of(concept("BCNF", 101L)), List.of()).plan().modules().getFirst().concepts().getFirst();
        assertNull(concept.existingId());
        assertNotNull(concept.stableKey());
    }

    @Test
    void unmatchedOldConceptIsRetiredNotDeleted() {
        ConceptIdentityReconciler.Result result = reconcile(List.of(concept("RAG", 101L)), List.of(existing(22L, "Unrelated cryptography", "CHECKED")));
        assertEquals(Set.of(22L), result.retiredConceptIds());
    }

    @Test
    void oneOldConceptCannotMatchMultipleCandidates() {
        ConceptIdentityReconciler.Result result = reconcile(List.of(concept("RAG", 101L), concept("Retrieval pipeline", 201L)),
                List.of(existing(31L, "Retrieval-Augmented Generation", "CHECKED")));
        assertEquals(1, result.matchedCount());
        assertEquals(1, result.newCount());
    }

    @Test
    void modulePositionChangeDoesNotAffectConceptIdentity() {
        ConceptPlan candidate = concept("SQL", 101L);
        ModulePlan advanced = new ModulePlan("Advanced", "ADVANCED", "Use SQL", List.of(1L), List.of(), List.of(candidate));
        LearningPlan plan = new LearningPlan("Path", new ArrayList<>(List.of(advanced)));
        ConceptIdentityReconciler.Result result = reconciler.reconcile(plan, snapshot(List.of(existing(40L, "SQL", "CHECKED"))));
        assertEquals(40L, result.plan().modules().getFirst().concepts().getFirst().existingId());
    }

    @Test
    void unrelatedConceptsRemainSeparate() {
        assertEquals(3, reconcile(List.of(concept("RAG", 101L), concept("BCNF", 201L), concept("PostgreSQL", 301L)), List.of()).newCount());
    }

    @Test
    void ambiguousSemanticMatchDoesNotMergeDestructively() {
        ExistingConcept one = existing(51L, "Ambiguous Alpha", "CHECKED");
        ExistingConcept two = existing(52L, "Ambiguous Beta", "REVIEW_NEEDED");
        ConceptIdentityReconciler.Result result = reconcile(List.of(concept("Ambiguous Candidate", 101L)), List.of(one, two));
        assertNull(result.plan().modules().getFirst().concepts().getFirst().existingId());
        assertEquals(Set.of(51L, 52L), result.retiredConceptIds());
    }

    @Test
    void verifiedSourceOverlapResolvesOtherwiseAmbiguousSemanticMatch() {
        ConceptPlan candidate = concept("Ambiguous Candidate", 101L);
        ModulePlan module = new ModulePlan("Module", "CORE", "Objective", List.of(1L), List.of(), List.of(candidate));
        ExistingConcept sameSource = new ExistingConcept(61L, "Ambiguous Alpha", "alpha", "CHECKED", Set.of(1L));
        ExistingConcept otherSource = new ExistingConcept(62L, "Ambiguous Beta", "beta", "REVIEW_NEEDED", Set.of(2L));
        EvidenceChunk evidence = new EvidenceChunk(101L, 1L, 0, "A", "Grounded evidence");
        ResourceSnapshot resource = new ResourceSnapshot(1L, "Doc", "PDF", "sum", 1L, "v1", "Doc", "Summary",
                List.of(), List.of(), List.of(), Set.of(101L), List.of(evidence));
        Snapshot withEvidence = new Snapshot(1L, 2L, 3L, "Area", "Goal", 1, "sig", "CURRENT",
                List.of(resource), List.of(sameSource, otherSource), Instant.now());
        ConceptIdentityReconciler.Result result = reconciler.reconcile(
                new LearningPlan("Path", new ArrayList<>(List.of(module))), withEvidence);
        assertEquals(61L, result.plan().modules().getFirst().concepts().getFirst().existingId());
        assertEquals(Set.of(62L), result.retiredConceptIds());
    }

    @Test
    void stableKeyIsDeterministicAcrossFormatting() {
        assertEquals(ConceptIdentityReconciler.stableKey("Retrieval-Augmented Generation"),
                ConceptIdentityReconciler.stableKey("retrieval augmented generation"));
    }

    private ConceptIdentityReconciler.Result reconcile(List<ConceptPlan> concepts, List<ExistingConcept> existing) {
        ModulePlan module = new ModulePlan("Module", "CORE", "Objective", List.of(1L), List.of(2L), concepts);
        return reconciler.reconcile(new LearningPlan("Path", new ArrayList<>(List.of(module))), snapshot(existing));
    }

    private Snapshot snapshot(List<ExistingConcept> existing) {
        return new Snapshot(1L, 2L, 3L, "Area", "Goal", 1, "sig", "CURRENT", List.of(), existing, Instant.now());
    }

    private ConceptPlan concept(String title, Long chunkId) { return new ConceptPlan(title, "Summary", "Why", List.of(chunkId)); }
    private ExistingConcept existing(Long id, String title, String status) {
        return new ExistingConcept(id, title, ConceptIdentityReconciler.stableKey(title), status, Set.of(1L));
    }

    private float[] vector(String label) {
        String normalized = ConceptIdentityReconciler.normalize(label);
        if (normalized.contains("ambiguous")) return new float[]{1f, 1f, 1f, 1f};
        if (normalized.equals("rag") || normalized.contains("retrieval augmented generation")) return new float[]{1f, 0f, 0f, 0f};
        if (normalized.equals("oop") || normalized.contains("object oriented programming")) return new float[]{0f, 1f, 0f, 0f};
        if (normalized.equals("rrf") || normalized.contains("reciprocal rank fusion")) return new float[]{.7f, .7f, 0f, 0f};
        if (normalized.contains("normalization")) return new float[]{0f, 0f, 1f, 0f};
        if (normalized.equals("bcnf")) return new float[]{0f, 0f, .6f, .8f};
        if (normalized.equals("sql")) return new float[]{0f, 0f, 0f, 1f};
        if (normalized.contains("postgresql")) return new float[]{.5f, 0f, 0f, .866f};
        if (normalized.contains("retrieval pipeline")) return new float[]{.75f, .66f, 0f, 0f};
        return new float[]{.25f, .35f, .45f, .55f};
    }
}
