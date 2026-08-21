package com.groupsync.backend.knowledge.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.groupsync.backend.knowledge.dto.FocusStudioDto.*;
import com.groupsync.backend.knowledge.dto.ResourceDeepDiveResponse;
import com.groupsync.backend.knowledge.service.CollectionLearningPathModel.*;
import com.groupsync.backend.shared.exception.BadRequestException;

/**
 * Coordinates provider work outside transactions:
 * snapshot, Gemini planning, semantic reconciliation, then atomic persistence.
 */
@Service
public class CollectionLearningPathService {
    private static final Logger log = LoggerFactory.getLogger(CollectionLearningPathService.class);
    private final LearningPathTransactionService transactions;
    private final RepresentativeLearningEvidenceSelector evidenceSelector;
    private final CollectionCurriculumPlanner planner;
    private final ConceptIdentityReconciler reconciler;

    public CollectionLearningPathService(LearningPathTransactionService transactions,
                                         CollectionCurriculumPlanner planner,
                                         ConceptIdentityReconciler reconciler) {
        this.transactions = transactions;
        this.planner = planner;
        this.reconciler = reconciler;
        this.evidenceSelector = new RepresentativeLearningEvidenceSelector();
    }

    public List<LearningAreaResponse> list(Long ownerId) { return transactions.listAreas(ownerId); }

    public LearningAreaDetailResponse initialize(Long ownerId, Long collectionId) {
        Long areaId = transactions.initialize(ownerId, collectionId);
        return transactions.detail(ownerId, areaId);
    }

    public LearningAreaDetailResponse detail(Long ownerId, Long areaId) { return transactions.detail(ownerId, areaId); }

    public LearningModuleResponse module(Long ownerId, Long areaId, Long moduleId) {
        return transactions.module(ownerId, areaId, moduleId);
    }

    public LearningAreaSourceMapResponse sourceMap(Long ownerId, Long areaId, List<Long> resourceIds) {
        return transactions.sourceMap(ownerId, areaId, resourceIds);
    }

    public ResourceDeepDiveResponse resourceDeepDive(Long ownerId, Long resourceId) {
        return transactions.resourceDeepDive(ownerId, resourceId);
    }

    public LearningAreaDetailResponse buildOrRefresh(Long ownerId, Long areaId) {
        Snapshot snapshot = transactions.snapshot(ownerId, areaId);
        if (snapshot.resources().isEmpty()) {
            throw new BadRequestException("Collection này chưa có nguồn READY để xây lộ trình học.");
        }
        String signature = transactions.sourceSignature(snapshot.resources());
        if (snapshot.currentVersion() > 0 && signature.equals(snapshot.currentSignature())) {
            transactions.markCurrentWithoutChange(ownerId, areaId);
            return transactions.detail(ownerId, areaId);
        }

        boolean refresh = snapshot.currentVersion() > 0;
        transactions.markGenerating(ownerId, areaId, refresh);
        long started = System.nanoTime();
        try {
            List<EvidenceChunk> evidence = evidenceSelector.select(snapshot.resources());
            if (evidence.isEmpty()) throw new BadRequestException("Không có bằng chứng đã xác minh để xây lộ trình học.");
            LearningPlan generated = planner.generate(snapshot, evidence);
            ConceptIdentityReconciler.Result reconciled = reconciler.reconcile(generated, snapshot);
            if (reconciled.plan().modules().isEmpty()) {
                throw new BadRequestException("Không có module hợp lệ sau khi đối chiếu khái niệm.");
            }
            transactions.persist(ownerId, snapshot, reconciled, signature);
            log.info("Learning path persisted learningAreaId={} collectionId={} sourceCount={} curriculumVersion={} candidateConceptCount={} matchedConceptCount={} newConceptCount={} retiredConceptCount={} generationDurationMs={}",
                    areaId, snapshot.collectionId(), snapshot.resources().size(), snapshot.currentVersion() + 1,
                    reconciled.matchedCount() + reconciled.newCount(), reconciled.matchedCount(), reconciled.newCount(),
                    reconciled.retiredConceptIds().size(), (System.nanoTime() - started) / 1_000_000L);
        } catch (RuntimeException exception) {
            transactions.markFailure(ownerId, areaId, exception.getMessage());
            log.warn("Learning path generation failed learningAreaId={} collectionId={} sourceCount={} failureCategory={} generationDurationMs={}",
                    areaId, snapshot.collectionId(), snapshot.resources().size(), exception.getClass().getSimpleName(),
                    (System.nanoTime() - started) / 1_000_000L);
            if (exception instanceof BadRequestException badRequest) throw badRequest;
            throw new BadRequestException("Không thể xây lộ trình học đã xác minh. Lộ trình hiện tại vẫn được giữ nguyên.");
        }
        return transactions.detail(ownerId, areaId);
    }
}
