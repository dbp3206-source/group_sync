package com.groupsync.backend.knowledge.ingestion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import com.groupsync.backend.knowledge.model.ResourceType;

@Component
public class NoteResourceParser implements ResourceParser {

    @Override
    public ResourceType supports() {
        return ResourceType.NOTE;
    }

    @Override
    public ParsedResourceContent parse(InputStream input) throws IOException {
        String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        return new ParsedResourceContent(content, null, null);
    }

    @Override
    public ParsedDocument parseDocument(InputStream input) throws IOException {
        String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        List<ParsedBlock> blocks = new ArrayList<>();
        int order = 0;

        String[] paragraphs = content.split("(?:\\r?\\n){2,}");
        for (String para : paragraphs) {
            String clean = para.trim();
            if (!clean.isBlank()) {
                blocks.add(new ParsedBlock(BlockType.PARAGRAPH, null, clean, null, order++));
            }
        }

        return new ParsedDocument(null, content, blocks);
    }
}
