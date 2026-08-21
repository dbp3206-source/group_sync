package com.groupsync.backend.knowledge.dto;

import java.util.List;

public record ResourceKnowledgeMapResponse(List<Node> nodes, List<Edge> edges) {
    public ResourceKnowledgeMapResponse {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        edges = edges == null ? List.of() : List.copyOf(edges);
    }

    public record Node(String id, String type, String label, Long resourceId, Long collectionId, Long tagId) { }

    public record Edge(String source, String target, String relationType, String reason,
                       Double confidence, String provenance) { }
}
