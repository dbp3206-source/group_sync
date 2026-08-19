package com.groupsync.backend.knowledge.ingestion;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import com.groupsync.backend.knowledge.model.ResourceType;

@Component
public class PdfResourceParser implements ResourceParser {

    @Override
    public ResourceType supports() {
        return ResourceType.PDF;
    }

    @Override
    public ParsedResourceContent parse(InputStream input) throws IOException {
        try (PDDocument document = Loader.loadPDF(input.readAllBytes())) {
            String text = new PDFTextStripper().getText(document);
            return new ParsedResourceContent(text, null, null);
        }
    }

    @Override
    public ParsedDocument parseDocument(InputStream input) throws IOException {
        try (PDDocument document = Loader.loadPDF(input.readAllBytes())) {
            int totalPages = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            List<ParsedBlock> blocks = new ArrayList<>();
            StringBuilder fullText = new StringBuilder();
            int order = 0;

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document);
                if (pageText != null && !pageText.isBlank()) {
                    if (!fullText.isEmpty()) fullText.append("\n\n");
                    fullText.append(pageText.trim());

                    String[] paragraphs = pageText.split("(?:\\r?\\n){2,}");
                    for (String para : paragraphs) {
                        String clean = para.trim();
                        if (!clean.isBlank()) {
                            blocks.add(new ParsedBlock(BlockType.PARAGRAPH, null, clean, page, order++));
                        }
                    }
                }
            }

            return new ParsedDocument(null, fullText.toString(), blocks);
        }
    }
}
