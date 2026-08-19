package com.groupsync.backend.knowledge.dto;

public record ParentChildTrace(
        int childChunksRetrieved,
        int uniqueParentsFound,
        int duplicateParentsDeduplicated
) {}
