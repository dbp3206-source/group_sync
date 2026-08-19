package com.groupsync.backend.knowledge.dto;

import com.groupsync.backend.knowledge.rag.QueryMode;
import com.groupsync.backend.knowledge.rag.QueryOperation;
import com.groupsync.backend.knowledge.rag.RetrievalScope;

public record PlannerTrace(
        QueryMode mode,
        QueryOperation operation,
        String semanticQuery,
        String explanation
) {}
