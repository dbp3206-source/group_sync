package com.groupsync.backend.knowledge.service;

import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupsync.backend.knowledge.rag.LanguageModelClient;
import com.groupsync.backend.knowledge.service.CollectionLearningPathModel.*;
import com.groupsync.backend.shared.exception.BadRequestException;
import org.springframework.stereotype.Component;

/** One bounded structured generation call. Validation remains deterministic and source-owned. */
@Component
public class CollectionCurriculumPlanner {
    private static final Set<String> STAGES = Set.of("FOUNDATION", "CORE", "APPLICATION", "ADVANCED");
    private static final int MAX_MODULES = 16;
    private static final int MAX_CONCEPTS_PER_MODULE = 12;
    static final int MAX_PROMPT_RESOURCES = 64;
    private final LanguageModelClient languageModel;
    private final ObjectMapper objectMapper;

    public CollectionCurriculumPlanner(LanguageModelClient languageModel, ObjectMapper objectMapper) {
        this.languageModel = languageModel;
        this.objectMapper = objectMapper;
    }

    public LearningPlan generate(Snapshot snapshot, List<EvidenceChunk> evidence) {
        String response = languageModel.answer(prompt(snapshot, evidence));
        return parseAndValidate(response, snapshot, evidence);
    }

    LearningPlan parseAndValidate(String raw, Snapshot snapshot, List<EvidenceChunk> evidence) {
        if (raw == null || raw.isBlank()) throw new BadRequestException("Gemini did not return a learning path.");
        Set<Long> resourceIds = snapshot.resources().stream().map(ResourceSnapshot::id).collect(java.util.stream.Collectors.toSet());
        Map<Long, EvidenceChunk> allowedChunks = evidence.stream().collect(java.util.stream.Collectors.toMap(
                EvidenceChunk::chunkId, chunk -> chunk, (left, right) -> left));
        try {
            JsonNode root = objectMapper.readTree(stripFence(raw));
            JsonNode modulesNode = root.path("modules");
            if (!modulesNode.isArray() || modulesNode.isEmpty() || modulesNode.size() > MAX_MODULES) {
                throw new BadRequestException("The generated learning path has an invalid module count.");
            }
            List<ModulePlan> modules = new ArrayList<>();
            Set<String> moduleKeys = new HashSet<>();
            for (JsonNode moduleNode : modulesNode) {
                String title = text(moduleNode, "title");
                String stage = text(moduleNode, "stage").toUpperCase(Locale.ROOT);
                String objective = text(moduleNode, "objective");
                if (title.isBlank() || objective.isBlank() || !STAGES.contains(stage)) {
                    throw new BadRequestException("The generated learning path contains an invalid module.");
                }
                if (!moduleKeys.add(stage + ":" + SemanticLabelPolicy.normalize(title))) {
                    throw new BadRequestException("The generated learning path contains duplicate modules.");
                }
                List<Long> primary = validResourceIds(moduleNode.path("primaryResourceIds"), resourceIds);
                List<Long> supporting = validResourceIds(moduleNode.path("supportingResourceIds"), resourceIds).stream()
                        .filter(id -> !primary.contains(id)).toList();
                if (primary.isEmpty()) throw new BadRequestException("Every module needs a verified primary source.");
                Set<Long> expectedResourceIds = new HashSet<>(primary);
                expectedResourceIds.addAll(supporting);

                JsonNode conceptsNode = moduleNode.path("concepts");
                if (!conceptsNode.isArray() || conceptsNode.isEmpty() || conceptsNode.size() > MAX_CONCEPTS_PER_MODULE) {
                    throw new BadRequestException("The generated learning path contains an empty or oversized module.");
                }
                List<ConceptPlan> concepts = new ArrayList<>();
                for (JsonNode conceptNode : conceptsNode) {
                    String conceptTitle = text(conceptNode, "title");
                    String summary = text(conceptNode, "summary");
                    String why = text(conceptNode, "whyItMatters");
                    LinkedHashSet<Long> chunkIds = new LinkedHashSet<>();
                    JsonNode chunkNode = conceptNode.path("sourceChunkIds");
                    if (chunkNode.isArray()) {
                        for (JsonNode idNode : chunkNode) {
                            long id = idNode.asLong(-1L);
                            EvidenceChunk chunk = allowedChunks.get(id);
                            if (chunk != null && expectedResourceIds.contains(chunk.resourceId()) && chunk.content() != null && !chunk.content().isBlank()) {
                                chunkIds.add(id);
                            }
                        }
                    }
                    // Unsupported concepts are rejected. Evidence is never fabricated or replaced.
                    if (!conceptTitle.isBlank() && !summary.isBlank() && !chunkIds.isEmpty()) {
                        concepts.add(new ConceptPlan(conceptTitle, summary, why, chunkIds));
                    }
                }
                if (!concepts.isEmpty()) modules.add(new ModulePlan(title, stage, objective, primary, supporting, concepts));
            }
            if (modules.isEmpty()) throw new BadRequestException("No source-grounded concepts were generated.");
            modules.sort(Comparator.comparingInt(module -> stageOrder(module.stage())));
            return new LearningPlan(text(root, "title").isBlank() ? snapshot.title() : text(root, "title"), modules);
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("The generated learning path could not be validated.");
        }
    }

    private String prompt(Snapshot snapshot, List<EvidenceChunk> evidence) {
        StringBuilder sources = new StringBuilder();
        for (ResourceSnapshot resource : representativeResources(snapshot.resources())) {
            sources.append("[RESOURCE_").append(resource.id()).append("] ").append(resource.title()).append('\n')
                    .append("Normalized title: ").append(Objects.toString(resource.normalizedTitle(), resource.title())).append('\n')
                    .append("Summary: ").append(bounded(resource.summary(), 1000, "Not available")).append('\n')
                    .append("Key ideas: ").append(boundedList(resource.keyIdeas(), 8)).append('\n')
                    .append("Themes: ").append(boundedList(resource.broadThemes(), 8)).append('\n')
                    .append("Semantic tags: ").append(boundedList(resource.semanticTags(), 12)).append("\n\n");
        }
        for (EvidenceChunk chunk : evidence) {
            sources.append("[CHUNK_").append(chunk.chunkId()).append(" RESOURCE_").append(chunk.resourceId())
                    .append(" SECTION=").append(bounded(chunk.section(), 160, "Unlabeled")).append("]\n")
                    .append(bounded(chunk.content(), 1800, "")).append("\n\n");
        }
        return """
                You are designing a source-grounded curriculum for the learning area "%s".
                Use only the supplied document understanding and evidence. Return one JSON object, no Markdown.

                JSON shape:
                {"title":"...","modules":[{"title":"...","stage":"FOUNDATION|CORE|APPLICATION|ADVANCED","objective":"...","primaryResourceIds":[1],"supportingResourceIds":[2],"concepts":[{"title":"...","summary":"...","whyItMatters":"...","sourceChunkIds":[10]}]}]}

                Requirements:
                - Order modules from broad foundations toward application and advanced material.
                - Use only RESOURCE and CHUNK IDs shown below.
                - Every concept needs at least one exact supporting CHUNK ID.
                - Prefer concepts supported across documents without duplicating equivalent labels.
                - Preserve canonical English technical terms when translation would be awkward.
                - Return a compact path. Do not force all four stages when the evidence does not support them.

                SOURCES:
                %s
                """.formatted(snapshot.title(), sources);
    }

    private List<ResourceSnapshot> representativeResources(List<ResourceSnapshot> resources) {
        List<ResourceSnapshot> ordered = resources.stream().sorted(Comparator.comparing(ResourceSnapshot::id)).toList();
        if (ordered.size() <= MAX_PROMPT_RESOURCES) return ordered;
        List<ResourceSnapshot> represented = new ArrayList<>(MAX_PROMPT_RESOURCES);
        for (int index = 0; index < MAX_PROMPT_RESOURCES; index++) {
            int sourceIndex = (int) Math.round(index * (ordered.size() - 1d) / (MAX_PROMPT_RESOURCES - 1d));
            represented.add(ordered.get(sourceIndex));
        }
        return represented;
    }

    private String boundedList(List<String> values, int limit) {
        if (values == null || values.isEmpty()) return "";
        return values.stream().filter(Objects::nonNull).map(value -> bounded(value, 220, ""))
                .filter(value -> !value.isBlank()).limit(limit).collect(java.util.stream.Collectors.joining("; "));
    }

    private String bounded(String value, int limit, String fallback) {
        String safe = value == null || value.isBlank() ? fallback : value.trim();
        return safe.substring(0, Math.min(limit, safe.length()));
    }

    private List<Long> validResourceIds(JsonNode node, Set<Long> allowed) {
        if (!node.isArray()) return List.of();
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        for (JsonNode item : node) {
            long id = item.asLong(-1L);
            if (!allowed.contains(id)) throw new BadRequestException("The generated path referenced a resource outside this Collection.");
            result.add(id);
        }
        return List.copyOf(result);
    }

    private int stageOrder(String stage) {
        return switch (stage) {
            case "FOUNDATION" -> 0;
            case "CORE" -> 1;
            case "APPLICATION" -> 2;
            default -> 3;
        };
    }

    private String text(JsonNode node, String name) { return node.path(name).asText("").trim(); }
    private String stripFence(String raw) {
        String value = raw.trim();
        if (value.startsWith("```json")) value = value.substring(7);
        else if (value.startsWith("```")) value = value.substring(3);
        if (value.endsWith("```")) value = value.substring(0, value.length() - 3);
        return value.trim();
    }
}
