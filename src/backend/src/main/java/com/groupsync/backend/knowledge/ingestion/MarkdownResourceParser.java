package com.groupsync.backend.knowledge.ingestion;

import java.io.*; import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import com.groupsync.backend.knowledge.model.ResourceType;
@Component public class MarkdownResourceParser implements ResourceParser { public ResourceType supports() { return ResourceType.MARKDOWN; } public ParsedResourceContent parse(InputStream input) throws IOException { String content = new String(input.readAllBytes(), StandardCharsets.UTF_8); String section = content.lines().filter(line -> line.startsWith("#")).findFirst().map(line -> line.replaceFirst("^#+\\s*", "")).orElse(null); return new ParsedResourceContent(content, null, section); } }
