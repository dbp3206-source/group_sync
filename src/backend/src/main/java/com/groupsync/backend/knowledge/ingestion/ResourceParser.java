package com.groupsync.backend.knowledge.ingestion;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import com.groupsync.backend.knowledge.model.ResourceType;

public interface ResourceParser {

    ResourceType supports();

    ParsedResourceContent parse(InputStream input) throws IOException;

    default ParsedDocument parseDocument(InputStream input) throws IOException {
        ParsedResourceContent legacy = parse(input);
        String text = legacy != null ? legacy.content() : "";
        List<ParsedBlock> blocks = text.isBlank() ? List.of() : List.of(
                new ParsedBlock(BlockType.PARAGRAPH, legacy != null ? legacy.section() : null, text, legacy != null ? legacy.pageNumber() : null, 0)
        );
        return new ParsedDocument(legacy != null ? legacy.section() : null, text, blocks);
    }
}
