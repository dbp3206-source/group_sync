package com.groupsync.backend.knowledge.service;

import java.util.*;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Automatically organizes resources into relevant Collections and Tags
 * based on keyword, semantic, and topic extraction upon ingestion.
 */
@Service
public class AutoOrganizationService {
    private static final Pattern NON_ALPHANUM = Pattern.compile("[^\\p{L}\\p{N}]+", Pattern.UNICODE_CHARACTER_CLASS);

    private record TopicRule(String collectionName, String collectionDesc, List<String> tagNames, List<String> keywords) {}

    private static final List<TopicRule> TOPIC_RULES = List.of(
            new TopicRule(
                    "Database Systems",
                    "Relational database design, normalization, functional dependencies, SQL, and indexing.",
                    List.of("database", "functional-dependency", "normalization", "sql"),
                    List.of("functional dependency", "armstrong", "database", "normalization", "relation", "closure", "bcnf", "3nf", "sql", "superkey")
            ),
            new TopicRule(
                    "Software Engineering",
                    "OOP principles, design patterns, clean architecture, refactoring, and system design.",
                    List.of("oop", "design-patterns", "architecture", "software-engineering"),
                    List.of("oop", "encapsulation", "polymorphism", "inheritance", "abstraction", "strategy pattern", "design patterns", "clean architecture", "acid", "solid")
            ),
            new TopicRule(
                    "AI & Security",
                    "AI safety, prompt injection defense, vulnerability research, CVEs, and security standards.",
                    List.of("ai-security", "cve", "prompt-injection", "vulnerability"),
                    List.of("cve", "prompt injection", "security", "vulnerability", "cvss", "rfc-9421", "jailbreak", "adversarial", "anti-hallucination", "rag")
            ),
            new TopicRule(
                    "Macroeconomics & Finance",
                    "Economic indicators, GDP growth, inflation, monetary policy, and foreign direct investment.",
                    List.of("economics", "macroeconomics", "finance", "gdp"),
                    List.of("gdp", "inflation", "cpi", "fdi", "macroeconomic", "interest rate", "economic", "export", "import", "monetary")
            ),
            new TopicRule(
                    "Medical & Healthcare",
                    "Clinical trial protocols, cardiology, pharmacological therapies, and medical research.",
                    List.of("medical", "clinical-trials", "healthcare", "cardiology"),
                    List.of("clinical", "trial", "cardiotrex", "hfref", "ejection fraction", "cardiology", "placebo", "protocol", "medical", "patient")
            ),
            new TopicRule(
                    "Knowledge Management",
                    "Knowledge retrieval, RAG architectures, knowledge graph, and documentation lifecycle.",
                    List.of("knowledge-management", "rag", "vector-search", "embeddings"),
                    List.of("knowledge", "quản trị tri thức", "retrieval", "hybrid rag", "pgvector", "hnsw", "fts", "rrf")
            )
    );

    private final NamedParameterJdbcTemplate jdbc;
    private final KnowledgeWorkspaceService workspaceService;

    public AutoOrganizationService(NamedParameterJdbcTemplate jdbc, KnowledgeWorkspaceService workspaceService) {
        this.jdbc = jdbc;
        this.workspaceService = workspaceService;
    }

    /**
     * Automatically classifies a resource into 1 or more relevant collections and tags.
     */
    @Transactional
    public void autoOrganize(Long ownerId, Long resourceId) {
        if (ownerId == null || resourceId == null) {
            return;
        }

        // Fetch resource title, description, and concatenated chunk content
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT title, coalesce(description, '') as description FROM resources WHERE id = :id AND owner_id = :ownerId",
                Map.of("id", resourceId, "ownerId", ownerId)
        );
        if (rows.isEmpty()) {
            return;
        }

        Map<String, Object> resource = rows.get(0);
        String title = String.valueOf(resource.get("title"));
        String description = String.valueOf(resource.get("description"));

        String chunkContent = jdbc.queryForObject(
                "SELECT coalesce(string_agg(content, ' '), '') FROM document_chunks WHERE resource_id = :resourceId",
                Map.of("resourceId", resourceId),
                String.class
        );

        String fullCorpus = (title + " " + description + " " + (chunkContent != null ? chunkContent : "")).toLowerCase(Locale.ROOT);

        Set<String> matchedCollections = new LinkedHashSet<>();
        Set<String> matchedTags = new LinkedHashSet<>();

        for (TopicRule rule : TOPIC_RULES) {
            boolean matched = false;
            for (String kw : rule.keywords()) {
                if (fullCorpus.contains(kw)) {
                    matched = true;
                    break;
                }
            }
            if (matched) {
                matchedCollections.add(rule.collectionName());
                matchedTags.addAll(rule.tagNames());
            }
        }

        // Default fallback if no specific rule matched
        if (matchedCollections.isEmpty()) {
            matchedCollections.add("General Knowledge");
            matchedTags.add("general");
        }

        // Assign tags
        for (String tagName : matchedTags) {
            try {
                com.groupsync.backend.knowledge.dto.TagResponse tag = workspaceService.findOrCreateTag(ownerId, tagName);
                Long tagId = tag.id();
                workspaceService.assignTag(ownerId, resourceId, tagId);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(AutoOrganizationService.class).warn("Failed to assign tag {}: {}", tagName, e.getMessage(), e);
            }
        }

        // Assign collections
        for (String colName : matchedCollections) {
            try {
                TopicRule rule = TOPIC_RULES.stream().filter(r -> r.collectionName().equalsIgnoreCase(colName)).findFirst().orElse(null);
                String colDesc = rule != null ? rule.collectionDesc() : "Curated topic collection.";
                com.groupsync.backend.knowledge.dto.CollectionResponse collection = workspaceService.findOrCreateCollection(ownerId, colName, colDesc);
                Long colId = collection.id();
                workspaceService.assignResource(ownerId, colId, resourceId);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(AutoOrganizationService.class).warn("Failed to assign collection {}: {}", colName, e.getMessage(), e);
            }
        }
    }

    /**
     * Re-runs auto-organization for all resources of the given owner.
     */
    @Transactional
    public void autoOrganizeAll(Long ownerId) {
        List<Long> resourceIds = jdbc.queryForList(
                "SELECT id FROM resources WHERE owner_id = :ownerId",
                Map.of("ownerId", ownerId),
                Long.class
        );
        for (Long resId : resourceIds) {
            autoOrganize(ownerId, resId);
        }
    }
}
