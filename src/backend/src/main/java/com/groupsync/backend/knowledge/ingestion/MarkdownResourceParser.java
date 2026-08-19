package com.groupsync.backend.knowledge.ingestion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.groupsync.backend.knowledge.model.ResourceType;

@Component
public class MarkdownResourceParser implements ResourceParser {

    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$");

    @Override
    public ResourceType supports() {
        return ResourceType.MARKDOWN;
    }

    @Override
    public ParsedResourceContent parse(InputStream input) throws IOException {
        String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        String section = content.lines()
                .filter(line -> line.startsWith("#"))
                .findFirst()
                .map(line -> line.replaceFirst("^#+\\s*", ""))
                .orElse(null);
        return new ParsedResourceContent(content, null, section);
    }

    @Override
    public ParsedDocument parseDocument(InputStream input) throws IOException {
        String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        List<ParsedBlock> blocks = new ArrayList<>();

        String currentHeading = null;
        StringBuilder currentParagraph = new StringBuilder();
        int order = 0;

        String documentTitle = null;

        for (String line : content.lines().toList()) {
            var matcher = HEADING_PATTERN.matcher(line.trim());
            if (matcher.matches()) {
                // Flush preceding paragraph
                if (!currentParagraph.isEmpty()) {
                    String pText = currentParagraph.toString().trim();
                    if (!pText.isBlank()) {
                        blocks.add(new ParsedBlock(BlockType.PARAGRAPH, currentHeading, pText, null, order++));
                    }
                    currentParagraph.setLength(0);
                }

                currentHeading = matcher.group(2).trim();
                if (documentTitle == null) {
                    documentTitle = currentHeading;
                }
                blocks.add(new ParsedBlock(BlockType.HEADING, currentHeading, line.trim(), null, order++));
            } else if (line.trim().isBlank()) {
                if (!currentParagraph.isEmpty()) {
                    String pText = currentParagraph.toString().trim();
                    if (!pText.isBlank()) {
                        blocks.add(new ParsedBlock(BlockType.PARAGRAPH, currentHeading, pText, null, order++));
                    }
                    currentParagraph.setLength(0);
                }
            } else {
                if (!currentParagraph.isEmpty()) {
                    currentParagraph.append("\n");
                }
                currentParagraph.append(line);
            }
        }

        if (!currentParagraph.isEmpty()) {
            String pText = currentParagraph.toString().trim();
            if (!pText.isBlank()) {
                blocks.add(new ParsedBlock(BlockType.PARAGRAPH, currentHeading, pText, null, order++));
            }
        }

        return new ParsedDocument(documentTitle, content, blocks);
    }
}
