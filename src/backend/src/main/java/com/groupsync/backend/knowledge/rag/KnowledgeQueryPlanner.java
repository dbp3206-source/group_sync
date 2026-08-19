package com.groupsync.backend.knowledge.rag;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupsync.backend.knowledge.model.ResourceType;

/**
 * Intelligent Query Planner for KnowledgeOS RAG v2.
 * Classifies user intent into typed execution plans (Structured, Semantic, Hybrid, Filtered Hybrid)
 * with schema awareness, collection/tag entity resolution, and strict fallback safety.
 */
@Component
public class KnowledgeQueryPlanner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeQueryPlanner.class);
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```", Pattern.CASE_INSENSITIVE);

    private final LanguageModelClient languageModelClient;
    private final QueryPlanValidator validator;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KnowledgeQueryPlanner(LanguageModelClient languageModelClient,
                                 QueryPlanValidator validator,
                                 NamedParameterJdbcTemplate jdbcTemplate) {
        this.languageModelClient = languageModelClient;
        this.validator = validator;
        this.jdbcTemplate = jdbcTemplate;
    }

    public QueryPlan plan(Long ownerId, String question, RetrievalScope scope,
                          Long thisResourceId, List<Long> selectedResourceIds, Long collectionId) {
        if (question == null || question.isBlank()) {
            return QueryPlan.defaultHybrid("");
        }

        try {
            // Step 1: Collect compact user schema context
            UserMetadataContext userContext = loadUserMetadataContext(ownerId);

            // Step 2: Build compact planning prompt
            String planningPrompt = buildPlanningPrompt(question.trim(), userContext);

            // Step 3: Call LLM for structured intent classification
            String llmResponse = languageModelClient.answer(planningPrompt);

            // Step 4: Parse JSON to raw QueryPlan
            QueryPlan rawPlan = parseJsonToPlan(llmResponse, question.trim(), userContext);

            // Step 5: Validate and constrain scope
            return validator.validateAndSanitize(ownerId, rawPlan, scope, thisResourceId, selectedResourceIds, collectionId);

        } catch (Exception ex) {
            log.warn("[query_planner_fallback] Planning failed for question '{}': {}. Falling back to default HYBRID search.",
                    question, ex.getMessage());
            return QueryPlan.defaultHybrid(question.trim());
        }
    }

    private record UserMetadataContext(
            Map<String, Long> collectionsByName,
            Map<String, Long> tagsByName
    ) {}

    private UserMetadataContext loadUserMetadataContext(Long ownerId) {
        MapSqlParameterSource params = new MapSqlParameterSource("ownerId", ownerId);

        Map<String, Long> collections = new HashMap<>();
        jdbcTemplate.query(
                "SELECT id, name FROM collections WHERE owner_id = :ownerId",
                params,
                (rs, rowNum) -> Map.entry(rs.getString("name").toLowerCase(Locale.ROOT), rs.getLong("id"))
        ).forEach(entry -> collections.put(entry.getKey(), entry.getValue()));

        Map<String, Long> tags = new HashMap<>();
        jdbcTemplate.query(
                "SELECT id, name FROM tags WHERE owner_id = :ownerId",
                params,
                (rs, rowNum) -> Map.entry(rs.getString("name").toLowerCase(Locale.ROOT), rs.getLong("id"))
        ).forEach(entry -> tags.put(entry.getKey(), entry.getValue()));

        return new UserMetadataContext(collections, tags);
    }

    private String buildPlanningPrompt(String question, UserMetadataContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are the KnowledgeOS Query Planner. Your job is to classify the user question into a typed query plan.\n");
        sb.append("Allowed Query Modes:\n");
        sb.append("- STRUCTURED: purely relational metadata questions such as counting documents, listing files, favorites.\n");
        sb.append("- SEMANTIC: pure conceptual/semantic text search with no filters.\n");
        sb.append("- HYBRID: standard text search combining semantic and lexical retrieval.\n");
        sb.append("- FILTERED_HYBRID: search containing explicit metadata constraints (e.g. only PDFs, within a specific collection, with a specific tag, favorite only).\n\n");

        sb.append("Allowed Operations: SEARCH, COUNT, LIST.\n");
        sb.append("Allowed Resource Types: PDF, DOCX, TEXT, MARKDOWN, NOTE.\n");

        if (!context.collectionsByName().isEmpty()) {
            sb.append("User's Available Collections: ").append(String.join(", ", context.collectionsByName().keySet())).append("\n");
        }
        if (!context.tagsByName().isEmpty()) {
            sb.append("User's Available Tags: ").append(String.join(", ", context.tagsByName().keySet())).append("\n");
        }

        sb.append("\nReturn ONLY a JSON object with this exact structure:\n");
        sb.append("{\n");
        sb.append("  \"mode\": \"STRUCTURED\" | \"SEMANTIC\" | \"HYBRID\" | \"FILTERED_HYBRID\",\n");
        sb.append("  \"operation\": \"SEARCH\" | \"COUNT\" | \"LIST\",\n");
        sb.append("  \"semanticQuery\": \"search query keywords/concepts\",\n");
        sb.append("  \"resourceType\": \"PDF\" | \"DOCX\" | \"TEXT\" | \"MARKDOWN\" | \"NOTE\" | null,\n");
        sb.append("  \"collectionName\": \"collection name\" | null,\n");
        sb.append("  \"tagName\": \"tag name\" | null,\n");
        sb.append("  \"favorite\": true | false | null,\n");
        sb.append("  \"createdAfter\": \"YYYY-MM-DD\" | null,\n");
        sb.append("  \"createdBefore\": \"YYYY-MM-DD\" | null,\n");
        sb.append("  \"explanation\": \"short rationale\"\n");
        sb.append("}\n\n");
        sb.append("User Question: ").append(question);

        return sb.toString();
    }

    private QueryPlan parseJsonToPlan(String rawJson, String originalQuestion, UserMetadataContext context) {
        String cleanJson = rawJson.trim();
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(cleanJson);
        if (matcher.find()) {
            cleanJson = matcher.group(1).trim();
        }

        try {
            JsonNode node = objectMapper.readTree(cleanJson);

            String modeStr = node.path("mode").asText("HYBRID").toUpperCase(Locale.ROOT);
            QueryMode mode;
            try {
                mode = QueryMode.valueOf(modeStr);
            } catch (Exception e) {
                mode = QueryMode.HYBRID;
            }

            String opStr = node.path("operation").asText("SEARCH").toUpperCase(Locale.ROOT);
            QueryOperation operation;
            try {
                operation = QueryOperation.valueOf(opStr);
            } catch (Exception e) {
                operation = QueryOperation.SEARCH;
            }

            String semanticQuery = node.path("semanticQuery").asText(originalQuestion);
            if (semanticQuery == null || semanticQuery.isBlank() || "null".equalsIgnoreCase(semanticQuery)) {
                semanticQuery = originalQuestion;
            }

            boolean impossible = false;

            // Parse Filters
            ResourceType resourceType = null;
            if (node.hasNonNull("resourceType")) {
                String rawType = node.path("resourceType").asText().trim();
                if (!rawType.isBlank() && !"null".equalsIgnoreCase(rawType)) {
                    try {
                        resourceType = ResourceType.valueOf(rawType.toUpperCase(Locale.ROOT));
                    } catch (Exception ex) {
                        impossible = true;
                    }
                }
            }

            Boolean favorite = node.hasNonNull("favorite") ? node.path("favorite").asBoolean() : null;

            LocalDateTime createdAfter = null;
            if (node.hasNonNull("createdAfter")) {
                String rawDate = node.path("createdAfter").asText().trim();
                if (!rawDate.isBlank() && !"null".equalsIgnoreCase(rawDate)) {
                    try {
                        createdAfter = LocalDate.parse(rawDate).atStartOfDay();
                    } catch (Exception ex) {
                        impossible = true;
                    }
                }
            }

            LocalDateTime createdBefore = null;
            if (node.hasNonNull("createdBefore")) {
                String rawDate = node.path("createdBefore").asText().trim();
                if (!rawDate.isBlank() && !"null".equalsIgnoreCase(rawDate)) {
                    try {
                        createdBefore = LocalDate.parse(rawDate).atTime(23, 59, 59);
                    } catch (Exception ex) {
                        impossible = true;
                    }
                }
            }

            // Resolve Collection Name
            Set<Long> collectionIds = null;
            if (node.hasNonNull("collectionName")) {
                String collName = node.path("collectionName").asText().trim();
                if (!collName.isBlank() && !"null".equalsIgnoreCase(collName)) {
                    Long collId = context.collectionsByName().get(collName.toLowerCase(Locale.ROOT));
                    if (collId != null) {
                        collectionIds = Set.of(collId);
                    } else {
                        impossible = true;
                        collectionIds = Set.of(-1L);
                    }
                }
            }

            // Resolve Tag Name
            Set<Long> tagIds = null;
            if (node.hasNonNull("tagName")) {
                String tagName = node.path("tagName").asText().trim();
                if (!tagName.isBlank() && !"null".equalsIgnoreCase(tagName)) {
                    Long tagId = context.tagsByName().get(tagName.toLowerCase(Locale.ROOT));
                    if (tagId != null) {
                        tagIds = Set.of(tagId);
                    } else {
                        impossible = true;
                        tagIds = Set.of(-1L);
                    }
                }
            }

            KnowledgeQueryFilters filters = new KnowledgeQueryFilters(
                    null, collectionIds, tagIds, resourceType, favorite, createdAfter, createdBefore, impossible
            );

            String explanation = node.path("explanation").asText("Planned by KnowledgeOS");

            return new QueryPlan(mode, operation, semanticQuery, filters, explanation);

        } catch (Exception ex) {
            log.warn("Failed to parse JSON query plan: {}", ex.getMessage());
            return QueryPlan.defaultHybrid(originalQuestion);
        }
    }
}
