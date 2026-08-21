package com.groupsync.backend.knowledge.service;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class Phase5ArchitectureContractTest {
    @Test
    void migrationAddsCollectionBackedUniquenessAndKeepsLegacyRows() { String sql = migration();
        assertTrue(sql.contains("collection_id BIGINT REFERENCES collections(id) ON DELETE SET NULL"));
        assertTrue(sql.contains("uk_study_topics_active_collection"));
        assertTrue(sql.contains("learning_area_type VARCHAR(30) NOT NULL DEFAULT 'LEGACY'")); }

    @Test
    void migrationAddsOrderedModulesAndPrimarySupportingSources() { String sql = migration();
        assertTrue(sql.contains("CREATE TABLE learning_modules")); assertTrue(sql.contains("uk_learning_module_version_position"));
        assertTrue(sql.contains("'PRIMARY', 'SUPPORTING'")); }

    @Test
    void migrationAddsVersionAndCurrentLookup() { String sql = migration();
        assertTrue(sql.contains("CREATE TABLE learning_path_versions")); assertTrue(sql.contains("uk_learning_path_current_topic")); }

    @Test
    void migrationPreservesConceptRowsAndAddsRetirement() { String sql = migration();
        assertTrue(sql.contains("lifecycle_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'"));
        assertTrue(sql.contains("ON DELETE RESTRICT")); assertFalse(sql.contains("DELETE FROM topic_concepts")); }

    @Test
    void collectionAdditionAndRemovalOnlyMarkPathStale() { String sql = migration();
        assertTrue(sql.contains("trg_collection_learning_area_membership")); assertTrue(sql.contains("NEW_KNOWLEDGE_AVAILABLE"));
        assertFalse(sql.contains("DELETE FROM quiz_attempts")); }

    @Test
    void resourceBecomingReadyMarksAreasWithoutGemini() { String sql = migration();
        assertTrue(sql.contains("AFTER UPDATE OF processing_status ON resources")); assertFalse(sql.toLowerCase().contains("gemini")); }

    @Test
    void priorMigrationsAreNotModifiedByPhase5Migration() { String sql = migration();
        assertFalse(sql.contains("ALTER MIGRATION")); assertFalse(sql.contains("DROP TABLE study_topics")); }

    @Test
    void importPipelineDoesNotCreateStandaloneStudyTopic() { String source = source("/com/groupsync/backend/knowledge/service/ResourceIngestionService.java");
        assertFalse(source.contains("StudyTopic")); assertFalse(source.contains("createTopic(")); }

    @Test
    void plannerRejectsUnsupportedConceptInsteadOfFallback() { String source = source("/com/groupsync/backend/knowledge/service/CollectionCurriculumPlanner.java");
        assertTrue(source.contains("Unsupported concepts are rejected")); assertFalse(source.contains("chunks.get(0)")); }

    @Test
    void oldLearningStudioNoLongerContainsFirstTwelveOrFallbackDeletion() { String source = source("/com/groupsync/backend/knowledge/service/LearningStudioService.java");
        assertFalse(source.contains("chunkLimit")); assertFalse(source.contains("deleteAll(existing)")); assertFalse(source.contains("nearest chunk")); }

    @Test
    void getEndpointsAreReadOnlyAndDoNotGenerate() { String controller = source("/com/groupsync/backend/knowledge/controller/LearningStudioController.java");
        assertTrue(controller.contains("@GetMapping(\"/learning-areas\")")); assertTrue(controller.contains("@PostMapping(\"/learning-areas/{id}/build\")"));
        assertFalse(controller.contains("@GetMapping(\"/learning-areas/{id}/build\")")); }

    @Test
    void recallSchemaKeepsHistoricalConceptReferencesSafe() { String migration = migration();
        assertFalse(migration.contains("ALTER TABLE quiz_attempts")); assertFalse(migration.contains("ALTER TABLE quiz_items"));
        assertTrue(migration.contains("topic_concepts(id) ON DELETE RESTRICT")); }

    @Test
    void refreshAddsEvidenceAndOnlyPrunesSourcesThatAreNoLongerValid() {
        String source = source("/com/groupsync/backend/knowledge/service/LearningPathTransactionService.java");
        assertTrue(source.contains("on conflict do nothing"));
        assertTrue(source.contains("and not exists("));
        assertTrue(source.contains("r.processing_status='READY'"));
        assertFalse(source.contains("delete from topic_concept_sources where concept_id=:concept"));
    }

    private String migration() { return resource("/db/migration/V18__collection_learning_paths.sql"); }
    private String source(String name) {
        try { return Files.readString(Path.of("src/main/java" + name), StandardCharsets.UTF_8); }
        catch (Exception exception) { throw new AssertionError(exception); }
    }
    private String resource(String name) {
        try (var stream = Phase5ArchitectureContractTest.class.getResourceAsStream(name)) {
            assertNotNull(stream, name); return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception exception) { throw new AssertionError(exception); }
    }
}
