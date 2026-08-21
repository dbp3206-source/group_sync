package com.groupsync.backend.knowledge.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import com.groupsync.backend.knowledge.dto.*;

class AskTraceContextTest {
    @Test
    void contextPublishesOnlyTypedSystemStageDetails() {
        var stages = new ArrayList<AskTraceStage>();
        try (var ignored = AskTraceContext.open((stage, details) -> stages.add(stage))) {
            AskTraceContext.emit(AskTraceStage.PLAN_READY, new AskTraceTechnicalDetails("SEMANTIC", "SEARCH", 1, 0, 1, null, null, null, null, null, null, null));
            AskTraceContext.emit(AskTraceStage.CITATIONS_VERIFIED, new AskTraceTechnicalDetails("SEMANTIC", "SEARCH", 1, 0, 1, 1, 1, 40, 6000, 1, "gemini-3.5-flash-lite", null));
        }
        assertEquals(java.util.List.of(AskTraceStage.PLAN_READY, AskTraceStage.CITATIONS_VERIFIED), stages);
    }
}
