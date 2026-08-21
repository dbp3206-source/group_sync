package com.groupsync.backend.knowledge.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupsync.backend.knowledge.service.CollectionLearningPathModel.ResourceSnapshot;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class LearningPathTransactionServiceTest {

    private final LearningPathTransactionService service = new LearningPathTransactionService(
            mock(NamedParameterJdbcTemplate.class), new ObjectMapper());

    @Test
    void sourceSignatureIsStableAcrossResourceAndTagOrdering() {
        String first = service.sourceSignature(List.of(resource(2L, List.of("java", "oop")), resource(1L, List.of("sql"))));
        String second = service.sourceSignature(List.of(resource(1L, List.of("sql")), resource(2L, List.of("oop", "java"))));
        assertEquals(first, second);
    }

    @Test
    void semanticTagChangeInvalidatesSourceSignature() {
        String before = service.sourceSignature(List.of(resource(1L, List.of("sql"))));
        String after = service.sourceSignature(List.of(resource(1L, List.of("sql", "normalization"))));
        assertNotEquals(before, after);
    }

    private ResourceSnapshot resource(Long id, List<String> tags) {
        return new ResourceSnapshot(id, "Doc " + id, "PDF", "checksum-" + id, id, "v1", "Doc", "Summary",
                List.of("Idea"), List.of("Theme"), tags, Set.of(), List.of());
    }
}
