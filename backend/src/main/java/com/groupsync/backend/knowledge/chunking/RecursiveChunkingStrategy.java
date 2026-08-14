package com.groupsync.backend.knowledge.chunking;

import java.util.*;
import org.springframework.stereotype.Component;
@Component public class RecursiveChunkingStrategy implements ChunkingStrategy { private static final int SIZE = 1000, OVERLAP = 180; public List<String> chunk(String content) { if (content == null || content.isBlank()) return List.of(); List<String> chunks = new ArrayList<>(); String normalized = content.replace("\r\n", "\n").trim(); for (int start = 0; start < normalized.length(); ) { int end = Math.min(normalized.length(), start + SIZE); if (end < normalized.length()) { int split = Math.max(normalized.lastIndexOf("\n", end), normalized.lastIndexOf(" ", end)); if (split > start + SIZE / 2) end = split; } chunks.add(normalized.substring(start, end).trim()); if (end >= normalized.length()) break; start = Math.max(end - OVERLAP, start + 1); } return chunks.stream().filter(value -> !value.isBlank()).toList(); } }
