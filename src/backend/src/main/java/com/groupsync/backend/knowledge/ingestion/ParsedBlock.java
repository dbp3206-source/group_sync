package com.groupsync.backend.knowledge.ingestion;

public record ParsedBlock(
        BlockType type,
        String heading,
        String content,
        Integer pageNumber,
        int order
) {
    public ParsedBlock {
        if (content == null) content = "";
    }
}
