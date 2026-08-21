package com.groupsync.backend.knowledge.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.annotation.Transactional;
import com.groupsync.backend.knowledge.dto.FocusStudioDto.*;
import com.groupsync.backend.knowledge.service.CollectionLearningPathModel.*;
import com.groupsync.backend.shared.exception.BadRequestException;

class CollectionLearningPathServiceTest {
    private LearningPathTransactionService transactions;
    private CollectionCurriculumPlanner planner;
    private ConceptIdentityReconciler reconciler;
    private CollectionLearningPathService service;

    @BeforeEach
    void setUp() {
        transactions = mock(LearningPathTransactionService.class);
        planner = mock(CollectionCurriculumPlanner.class);
        reconciler = mock(ConceptIdentityReconciler.class);
        service = new CollectionLearningPathService(transactions, planner, reconciler);
    }

    @Test
    void collectionCanInitializeOneLearningArea() {
        when(transactions.initialize(1L, 2L)).thenReturn(3L);
        when(transactions.detail(1L, 3L)).thenReturn(detail(3L, 2L, 0, "NOT_BUILT"));
        assertEquals(3L, service.initialize(1L, 2L).area().id());
    }

    @Test
    void duplicateInitializationUsesTransactionLevelIdentity() {
        when(transactions.initialize(1L, 2L)).thenReturn(3L);
        when(transactions.detail(1L, 3L)).thenReturn(detail(3L, 2L, 0, "NOT_BUILT"));
        assertEquals(service.initialize(1L, 2L).area().id(), service.initialize(1L, 2L).area().id());
        verify(transactions, times(2)).initialize(1L, 2L);
    }

    @Test
    void ownerIsolationIsDelegatedToEveryTransactionCall() {
        service.list(77L);
        verify(transactions).listAreas(77L);
        service.sourceMap(77L, 4L, List.of(9L));
        verify(transactions).sourceMap(77L, 4L, List.of(9L));
    }

    @Test
    void refreshWithNoSemanticChangesDoesNotCallGeminiOrEmbedding() {
        Snapshot snapshot = snapshot(1, "same");
        when(transactions.snapshot(1L, 10L)).thenReturn(snapshot);
        when(transactions.sourceSignature(snapshot.resources())).thenReturn("same");
        when(transactions.detail(1L, 10L)).thenReturn(detail(10L, 20L, 1, "CURRENT"));
        assertEquals("CURRENT", service.buildOrRefresh(1L, 10L).area().refreshStatus());
        verify(transactions).markCurrentWithoutChange(1L, 10L);
        verifyNoInteractions(planner, reconciler);
    }

    @Test
    void initialBuildRequiresReadySources() {
        Snapshot empty = new Snapshot(10L, 20L, 1L, "Area", "Goal", 0, null, "NOT_BUILT", List.of(), List.of(), Instant.now());
        when(transactions.snapshot(1L, 10L)).thenReturn(empty);
        assertThrows(BadRequestException.class, () -> service.buildOrRefresh(1L, 10L));
        verify(transactions, never()).markGenerating(anyLong(), anyLong(), anyBoolean());
    }

    @Test
    void failedRefreshKeepsOldCurrentPathAndRecordsFailure() {
        Snapshot snapshot = snapshot(1, "old");
        when(transactions.snapshot(1L, 10L)).thenReturn(snapshot);
        when(transactions.sourceSignature(snapshot.resources())).thenReturn("new");
        when(planner.generate(eq(snapshot), anyList())).thenThrow(new RuntimeException("provider failed"));
        assertThrows(BadRequestException.class, () -> service.buildOrRefresh(1L, 10L));
        verify(transactions).markFailure(1L, 10L, "provider failed");
        verify(transactions, never()).persist(anyLong(), any(), any(), anyString());
    }

    @Test
    void validGenerationPersistsOnlyAfterPlanningAndReconciliation() {
        Snapshot snapshot = snapshot(0, null);
        LearningPlan plan = plan();
        ConceptIdentityReconciler.Result result = new ConceptIdentityReconciler.Result(plan, Set.of(), 0, 1);
        when(transactions.snapshot(1L, 10L)).thenReturn(snapshot);
        when(transactions.sourceSignature(snapshot.resources())).thenReturn("new");
        when(planner.generate(eq(snapshot), anyList())).thenReturn(plan);
        when(reconciler.reconcile(plan, snapshot)).thenReturn(result);
        when(transactions.detail(1L, 10L)).thenReturn(detail(10L, 20L, 1, "CURRENT"));
        service.buildOrRefresh(1L, 10L);
        InOrder order = inOrder(transactions, planner, reconciler);
        order.verify(transactions).markGenerating(1L, 10L, false);
        order.verify(planner).generate(eq(snapshot), anyList());
        order.verify(reconciler).reconcile(plan, snapshot);
        order.verify(transactions).persist(1L, snapshot, result, "new");
    }

    @Test
    void geminiOrchestratorHasNoTransactionalAnnotation() throws Exception {
        assertNull(CollectionLearningPathService.class.getMethod("buildOrRefresh", Long.class, Long.class).getAnnotation(Transactional.class));
        assertNotNull(LearningPathTransactionService.class.getMethod("snapshot", Long.class, Long.class).getAnnotation(Transactional.class));
        assertNotNull(LearningPathTransactionService.class.getMethod("persist", Long.class, Snapshot.class,
                ConceptIdentityReconciler.Result.class, String.class).getAnnotation(Transactional.class));
    }

    @Test
    void refreshMarksRefreshingInsteadOfResettingMastery() {
        Snapshot snapshot = snapshot(2, "old");
        when(transactions.snapshot(1L, 10L)).thenReturn(snapshot);
        when(transactions.sourceSignature(snapshot.resources())).thenReturn("new");
        when(planner.generate(eq(snapshot), anyList())).thenThrow(new BadRequestException("stop"));
        assertThrows(BadRequestException.class, () -> service.buildOrRefresh(1L, 10L));
        verify(transactions).markGenerating(1L, 10L, true);
    }

    private Snapshot snapshot(int version, String signature) {
        EvidenceChunk chunk = new EvidenceChunk(101L, 1L, 0, "S", "Evidence");
        ResourceSnapshot resource = new ResourceSnapshot(1L, "Doc", "PDF", "checksum", 1L, "v1", "Doc", "Summary",
                List.of("Idea"), List.of("Theme"), List.of("semantic-tag"), Set.of(101L), List.of(chunk));
        return new Snapshot(10L, 20L, 1L, "Area", "Goal", version, signature, "CURRENT", List.of(resource), List.of(), Instant.now());
    }

    private LearningPlan plan() {
        ConceptPlan concept = new ConceptPlan("RAG", "Summary", "Why", List.of(101L));
        return new LearningPlan("Area", new ArrayList<>(List.of(new ModulePlan("Module", "FOUNDATION", "Objective", List.of(1L), List.of(), List.of(concept)))));
    }

    private LearningAreaDetailResponse detail(Long id, Long collectionId, int version, String status) {
        LearningAreaResponse area = new LearningAreaResponse(id, collectionId, "Area", "Goal", 1, version > 0 ? 1 : 0,
                version > 0 ? 1 : 0, 0, 0, 0, version > 0 ? 1 : 0, status, 0, version, null, Instant.now());
        return new LearningAreaDetailResponse(area, List.of(), version > 0 ? List.of(new LearningModuleResponse(1L, 1,
                "FOUNDATION", "Module", "Objective", 1, 0, 0, List.of(), List.of(), List.of())) : List.of(), List.of());
    }
}
