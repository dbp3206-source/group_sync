package com.groupsync.backend.knowledge.ingestion;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;
import com.groupsync.backend.knowledge.model.ResourceType;

@Component
public class DocxResourceParser implements ResourceParser {

    @Override
    public ResourceType supports() {
        return ResourceType.DOCX;
    }

    @Override
    public ParsedResourceContent parse(InputStream input) throws IOException {
        try (var document = new XWPFDocument(input); var extractor = new XWPFWordExtractor(document)) {
            return new ParsedResourceContent(extractor.getText(), null, null);
        }
    }

    @Override
    public ParsedDocument parseDocument(InputStream input) throws IOException {
        try (var document = new XWPFDocument(input)) {
            List<ParsedBlock> blocks = new ArrayList<>();
            StringBuilder fullText = new StringBuilder();
            String currentHeading = null;
            int order = 0;

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText().trim();
                if (text.isBlank()) continue;

                if (!fullText.isEmpty()) fullText.append("\n\n");
                fullText.append(text);

                String style = paragraph.getStyle();
                boolean isHeading = (style != null && style.toLowerCase().startsWith("heading"))
                        || (paragraph.getRuns().stream().allMatch(r -> r.isBold()) && text.length() < 100);

                if (isHeading) {
                    currentHeading = text;
                    blocks.add(new ParsedBlock(BlockType.HEADING, currentHeading, text, null, order++));
                } else {
                    blocks.add(new ParsedBlock(BlockType.PARAGRAPH, currentHeading, text, null, order++));
                }
            }

            return new ParsedDocument(null, fullText.toString(), blocks);
        }
    }
}
