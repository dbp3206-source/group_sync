package com.groupsync.backend.knowledge.ingestion;

import java.io.*; import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import com.groupsync.backend.knowledge.model.ResourceType;
@Component public class NoteResourceParser implements ResourceParser { public ResourceType supports() { return ResourceType.NOTE; } public ParsedResourceContent parse(InputStream input) throws IOException { return new ParsedResourceContent(new String(input.readAllBytes(), StandardCharsets.UTF_8), null, null); } }
