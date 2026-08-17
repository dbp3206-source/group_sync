package com.groupsync.backend.knowledge.ingestion;

import java.io.*;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import com.groupsync.backend.knowledge.model.ResourceType;
@Component public class DocxResourceParser implements ResourceParser { public ResourceType supports() { return ResourceType.DOCX; } public ParsedResourceContent parse(InputStream input) throws IOException { try (var document = new XWPFDocument(input); var extractor = new XWPFWordExtractor(document)) { return new ParsedResourceContent(extractor.getText(), null, null); } } }
