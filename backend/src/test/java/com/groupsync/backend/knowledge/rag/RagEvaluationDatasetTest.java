package com.groupsync.backend.knowledge.rag;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class RagEvaluationDatasetTest {
    @Test
    void controlledDatasetContainsBroadGroundedAndSafetyCoverage() throws Exception {
        String json = Files.readString(Path.of("..", "qa", "fixtures", "rag-cases.json"));
        long cases = Pattern.compile("\\\"testId\\\"").matcher(json).results().count();
        assertTrue(cases >= 34, "Expected at least 34 controlled cases (25 original + 9 hybrid-specific); found: " + cases);
        assertTrue(json.contains("VIETNAMESE"));
        assertTrue(json.contains("PROMPT_INJECTION"));
        assertTrue(json.contains("CONFLICTING_EVIDENCE"));
        assertTrue(json.contains("SCOPE_ISOLATION"));
        // Hybrid-specific categories
        assertTrue(json.contains("EXACT_IDENTIFIER"), "Dataset must include EXACT_IDENTIFIER cases for CVE/code precision testing");
        assertTrue(json.contains("EXACT_STANDARD"), "Dataset must include EXACT_STANDARD cases for RFC/standard precision testing");
        assertTrue(json.contains("OUT_OF_SCOPE_EXACT_MATCH"), "Dataset must include OUT_OF_SCOPE_EXACT_MATCH to verify zero lexical leakage");
        assertTrue(json.contains("SEMANTIC_PARAPHRASE"), "Dataset must include SEMANTIC_PARAPHRASE cases for semantic branch validation");
    }
}

