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
        assertTrue(cases >= 25);
        assertTrue(json.contains("VIETNAMESE"));
        assertTrue(json.contains("PROMPT_INJECTION"));
        assertTrue(json.contains("CONFLICTING_EVIDENCE"));
        assertTrue(json.contains("SCOPE_ISOLATION"));
    }
}
