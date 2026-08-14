package com.groupsync.backend.knowledge.chunking;

import java.util.List;
public interface ChunkingStrategy { List<String> chunk(String content); }
