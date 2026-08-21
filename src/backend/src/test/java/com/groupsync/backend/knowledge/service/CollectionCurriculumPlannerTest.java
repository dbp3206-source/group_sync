package com.groupsync.backend.knowledge.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.groupsync.backend.knowledge.rag.LanguageModelClient;
import com.groupsync.backend.knowledge.service.CollectionLearningPathModel.*;
import com.groupsync.backend.shared.exception.BadRequestException;

class CollectionCurriculumPlannerTest {
    private LanguageModelClient languageModel;
    private CollectionCurriculumPlanner planner;
    private Snapshot snapshot;
    private List<EvidenceChunk> evidence;

    @BeforeEach
    void setUp() {
        languageModel = mock(LanguageModelClient.class);
        planner = new CollectionCurriculumPlanner(languageModel, new ObjectMapper());
        EvidenceChunk a = new EvidenceChunk(101L, 1L, 0, "A", "RAG uses retrieval before generation.");
        EvidenceChunk b = new EvidenceChunk(201L, 2L, 5, "B", "RRF combines ranked lists.");
        snapshot = new Snapshot(10L, 20L, 30L, "AI Engineering", "Learn", 0, null, "NOT_BUILT",
                List.of(resource(1L, a), resource(2L, b)), List.of(), Instant.now());
        evidence = List.of(a, b);
    }

    @Test
    void validTypedPlanKeepsOrderedStagesAndSources() {
        LearningPlan plan = planner.parseAndValidate(validJson(), snapshot, evidence);
        assertEquals(List.of("FOUNDATION", "CORE"), plan.modules().stream().map(ModulePlan::stage).toList());
        assertEquals(List.of(1L), plan.modules().getFirst().primaryResourceIds());
    }

    @Test
    void invalidResourceIdIsRejected() {
        String json = validJson().replace("\"primaryResourceIds\":[1]", "\"primaryResourceIds\":[999]");
        assertThrows(BadRequestException.class, () -> planner.parseAndValidate(json, snapshot, evidence));
    }

    @Test
    void crossResourceChunkIdIsRejectedRatherThanReplaced() {
        String json = validJson().replace("\"supportingResourceIds\":[2]", "\"supportingResourceIds\":[]").replace("[101]", "[201]");
        LearningPlan plan = planner.parseAndValidate(json, snapshot, evidence);
        assertEquals(1, plan.modules().size());
        assertEquals("RRF", plan.modules().getFirst().concepts().getFirst().title());
    }

    @Test
    void unsupportedConceptIsRemoved() {
        String json = validJson().replace("[101]", "[]");
        LearningPlan plan = planner.parseAndValidate(json, snapshot, evidence);
        assertTrue(plan.modules().stream().flatMap(module -> module.concepts().stream()).noneMatch(concept -> concept.title().equals("RAG")));
    }

    @Test
    void noArbitraryFallbackChunkIsEverAdded() {
        String json = validJson().replace("[101]", "[999]");
        LearningPlan plan = planner.parseAndValidate(json, snapshot, evidence);
        assertTrue(plan.modules().stream().flatMap(module -> module.concepts().stream())
                .flatMap(concept -> concept.sourceChunkIds().stream()).noneMatch(id -> id == 101L));
    }

    @Test
    void emptyModulesAreRejected() {
        String json = "{\"title\":\"X\",\"modules\":[{\"title\":\"M\",\"stage\":\"CORE\",\"objective\":\"O\",\"primaryResourceIds\":[1],\"supportingResourceIds\":[],\"concepts\":[]}]}";
        assertThrows(BadRequestException.class, () -> planner.parseAndValidate(json, snapshot, evidence));
    }

    @Test
    void unsupportedStageIsRejected() {
        assertThrows(BadRequestException.class, () -> planner.parseAndValidate(validJson().replace("FOUNDATION", "EXPERT"), snapshot, evidence));
    }

    @Test
    void supportingSourceCannotDuplicatePrimarySource() {
        String json = validJson().replace("\"supportingResourceIds\":[2]", "\"supportingResourceIds\":[1,2]");
        assertEquals(List.of(2L), planner.parseAndValidate(json, snapshot, evidence).modules().getFirst().supportingResourceIds());
    }

    @Test
    void generationPromptIncludesSemanticTagsAndEvidenceSections() {
        when(languageModel.answer(anyString())).thenReturn(validJson());
        planner.generate(snapshot, evidence);
        var prompt = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(languageModel).answer(prompt.capture());
        assertTrue(prompt.getValue().contains("Semantic tags: semantic-tag"));
        assertTrue(prompt.getValue().contains("SECTION=A"));
        assertTrue(prompt.getValue().contains("SECTION=B"));
    }

    @Test
    void largeCollectionPromptIsBoundedAndSpansWholeCollection() {
        List<ResourceSnapshot> resources = java.util.stream.LongStream.rangeClosed(1, 100)
                .mapToObj(id -> resource(id, new EvidenceChunk(id * 100, id, 0, "S", "Evidence"))).toList();
        Snapshot large = new Snapshot(10L, 20L, 30L, "Large Area", "Learn", 0, null, "NOT_BUILT",
                resources, List.of(), Instant.now());
        when(languageModel.answer(anyString())).thenReturn(validJson());
        planner.generate(large, evidence);
        var prompt = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(languageModel).answer(prompt.capture());
        List<String> resourceLines = prompt.getValue().lines().filter(line -> line.startsWith("[RESOURCE_")).toList();
        assertEquals(CollectionCurriculumPlanner.MAX_PROMPT_RESOURCES, resourceLines.size());
        assertTrue(resourceLines.stream().anyMatch(line -> line.startsWith("[RESOURCE_1]")));
        assertTrue(resourceLines.stream().anyMatch(line -> line.startsWith("[RESOURCE_100]")));
        assertTrue(resourceLines.stream().anyMatch(line -> line.matches("\\[RESOURCE_(4[8-9]|5[0-2])] .*")));
    }

    private ResourceSnapshot resource(Long id, EvidenceChunk chunk) {
        return new ResourceSnapshot(id, "Doc " + id, "PDF", "sum" + id, id, "v1", "Doc", "Summary",
                List.of("Idea"), List.of("Theme"), List.of("semantic-tag"), Set.of(chunk.chunkId()), List.of(chunk));
    }

    private String validJson() {
        return """
                {"title":"AI Engineering","modules":[
                  {"title":"Retrieval foundations","stage":"FOUNDATION","objective":"Understand retrieval","primaryResourceIds":[1],"supportingResourceIds":[2],
                   "concepts":[{"title":"RAG","summary":"Retrieval precedes generation.","whyItMatters":"Grounding","sourceChunkIds":[101]}]},
                  {"title":"Rank fusion","stage":"CORE","objective":"Combine rankings","primaryResourceIds":[2],"supportingResourceIds":[],
                   "concepts":[{"title":"RRF","summary":"Combines ranked lists.","whyItMatters":"Hybrid search","sourceChunkIds":[201]}]}
                ]}
                """;
    }
}
