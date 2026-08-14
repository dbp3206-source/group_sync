package com.groupsync.backend.knowledge.ingestion;

import java.io.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import com.groupsync.backend.knowledge.model.ResourceType;
@Component public class PdfResourceParser implements ResourceParser { public ResourceType supports() { return ResourceType.PDF; } public ParsedResourceContent parse(InputStream input) throws IOException { try (var document = Loader.loadPDF(input.readAllBytes())) { return new ParsedResourceContent(new PDFTextStripper().getText(document), null, null); } } }
