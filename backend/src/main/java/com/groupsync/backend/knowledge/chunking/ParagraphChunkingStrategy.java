package com.groupsync.backend.knowledge.chunking;

import java.util.*;
import org.springframework.stereotype.Component;
@Component public class ParagraphChunkingStrategy implements ChunkingStrategy { public List<String> chunk(String content) { if (content == null || content.isBlank()) return List.of(); return Arrays.stream(content.split("(?:\\r?\\n){2,}")).map(String::trim).filter(value -> !value.isBlank()).toList(); } }
