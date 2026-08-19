package com.groupsync.backend.knowledge.ingestion;

import java.util.List;

public record ParsedDocument(
        String title,
        String fullText,
        List<ParsedBlock> blocks
) {
    public ParsedDocument {
        if (fullText == null) fullText = "";
        if (blocks == null) blocks = List.of();
    }
}
