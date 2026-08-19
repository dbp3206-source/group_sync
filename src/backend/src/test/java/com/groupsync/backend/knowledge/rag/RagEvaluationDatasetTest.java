package com.groupsync.backend.knowledge.rag;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class RagEvaluationDatasetTest {
    private static Path resolveFixturePath(String filename) {
        Path[] candidates = new Path[] {
            Path.of("..", "..", "refer", "qa_dataset", "fixtures", filename),
            Path.of("..", "refer", "qa_dataset", "fixtures", filename),
            Path.of("refer", "qa_dataset", "fixtures", filename),
            Path.of("..", "qa", "fixtures", filename),
            Path.of("qa", "fixtures", filename)
        };
        for (Path p : candidates) {
            if (Files.exists(p)) return p;
        }
        return candidates[0];
    }

    @Test
    void controlledDatasetContainsBroadGroundedAndSafetyCoverage() throws Exception {
        Path path = resolveFixturePath("rag-cases.json");
        String json = Files.readString(path);
        long cases = Pattern.compile("\\\"testId\\\"").matcher(json).results().count();
        assertTrue(cases >= 37, "Expected at least 37 controlled cases; found: " + cases);
        assertTrue(json.contains("VIETNAMESE"));
        assertTrue(json.contains("PROMPT_INJECTION"));
        assertTrue(json.contains("CONFLICTING_EVIDENCE"));
        assertTrue(json.contains("SCOPE_ISOLATION"));
        // Hybrid and RAG v2 categories
        assertTrue(json.contains("EXACT_IDENTIFIER"), "Dataset must include EXACT_IDENTIFIER cases for CVE/code precision testing");
        assertTrue(json.contains("EXACT_STANDARD"), "Dataset must include EXACT_STANDARD cases for RFC/standard precision testing");
        assertTrue(json.contains("OUT_OF_SCOPE_EXACT_MATCH"), "Dataset must include OUT_OF_SCOPE_EXACT_MATCH to verify zero lexical leakage");
        assertTrue(json.contains("SEMANTIC_PARAPHRASE"), "Dataset must include SEMANTIC_PARAPHRASE cases for semantic branch validation");
        assertTrue(json.contains("STRUCTURED"), "Dataset must include STRUCTURED cases for RAG v2 relational operations");
        assertTrue(json.contains("FILTERED_SEMANTIC"), "Dataset must include FILTERED_SEMANTIC cases for RAG v2 metadata filtering");
        assertTrue(json.contains("PARENT_CHILD"), "Dataset must include PARENT_CHILD cases for RAG v2 hierarchical expansion");
    }
}
