package com.groupsync.backend.knowledge.rag;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import com.google.genai.Client;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Explicit live-only comparison. Each model owns an isolated in-memory index and query vectors
 * are never compared with vectors produced by the other model.
 */
@EnabledIfEnvironmentVariable(named = "KNOWLEDGEOS_EMBEDDING_BENCHMARK", matches = "true")
class EmbeddingModelComparisonBenchmarkTest {
    private static final int DIMENSIONS = 768;

    @Test
    void comparesEmbedding001AndEmbedding2OnControlledRetrievalCases() throws Exception {
        String apiKey = System.getenv("GEMINI_API_KEY");
        assertNotNull(apiKey, "GEMINI_API_KEY is required for the live embedding benchmark.");
        assertFalse(apiKey.isBlank(), "GEMINI_API_KEY is required for the live embedding benchmark.");
        Client client = Client.builder().apiKey(apiKey).build();
        List<BenchmarkDocument> documents = loadDocuments();
        List<BenchmarkCase> cases = List.of(
                new BenchmarkCase("direct-fact", "Who owns Project Orion?", "project-orion.md", Set.of()),
                new BenchmarkCase("semantic-paraphrase", "How does vector search find conceptually close passages?", "security-notes.md", Set.of()),
                new BenchmarkCase("vietnamese", "Người học nên xem lại tài liệu sau khoảng thời gian nào?", "vietnamese-learning.md", Set.of()),
                new BenchmarkCase("cross-document", "Who owns the launch workstream?", "meeting-notes.txt", Set.of()),
                new BenchmarkCase("exact-identifier", "Where is CVE-2026-12345 discussed?", "security-notes.md", Set.of()),
                new BenchmarkCase("distractor", "What does the greenhouse need each morning?", "irrelevant.md", Set.of()),
                new BenchmarkCase("scope-filter", "What is the Atlas accuracy?", "atlas.md", Set.of("atlas.md")));

        for (String model : List.of("gemini-embedding-001", "gemini-embedding-2")) {
            ModelAdapter adapter = new ModelAdapter(client, model);
            long embeddingStarted = System.nanoTime();
            List<IndexedVector> isolatedIndex = new ArrayList<>();
            int errors = 0;
            for (BenchmarkDocument document : documents) {
                for (String chunk : chunks(document.content())) {
                    try {
                        isolatedIndex.add(new IndexedVector(model, document.filename(),
                                adapter.embedDocument(document.title(), chunk)));
                    } catch (RuntimeException exception) { errors++; }
                }
            }
            long embeddingLatencyMs = (System.nanoTime() - embeddingStarted) / 1_000_000L;

            int recall5 = 0, recall10 = 0;
            double reciprocalRank = 0d;
            long queryLatencyMs = 0L;
            for (BenchmarkCase benchmarkCase : cases) {
                try {
                    long queryStarted = System.nanoTime();
                    float[] query = adapter.embedQuery(benchmarkCase.question());
                    List<String> ranking = rank(model, query, isolatedIndex, benchmarkCase.allowedFiles());
                    queryLatencyMs += (System.nanoTime() - queryStarted) / 1_000_000L;
                    int rank = ranking.indexOf(benchmarkCase.expectedFile()) + 1;
                    if (rank > 0 && rank <= 5) recall5++;
                    if (rank > 0 && rank <= 10) recall10++;
                    if (rank > 0) reciprocalRank += 1d / rank;
                } catch (RuntimeException exception) { errors++; }
            }
            double denominator = cases.size();
            System.out.printf(Locale.ROOT,
                    "EMBEDDING_BENCHMARK model=%s cases=%d recallAt5=%.3f recallAt10=%.3f mrr=%.3f embeddingLatencyMs=%d avgQueryLatencyMs=%.1f errors=%d%n",
                    model, cases.size(), recall5 / denominator, recall10 / denominator,
                    reciprocalRank / denominator, embeddingLatencyMs, queryLatencyMs / denominator, errors);
            assertEquals(0, errors, "Live embedding benchmark encountered provider errors for " + model);
        }
    }

    static List<String> rank(String model, float[] query, List<IndexedVector> index, Set<String> allowedFiles) {
        Map<String, Double> bestByFile = new HashMap<>();
        for (IndexedVector vector : index) {
            if (!model.equals(vector.model())) throw new IllegalArgumentException("Embedding spaces must remain isolated.");
            if (!allowedFiles.isEmpty() && !allowedFiles.contains(vector.filename())) continue;
            bestByFile.merge(vector.filename(), SemanticSimilarityForBenchmark.cosine(query, vector.vector()), Math::max);
        }
        return bestByFile.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey).toList();
    }

    private static List<BenchmarkDocument> loadDocuments() throws Exception {
        return List.of(
                load("Project Orion", "project-orion.md"), load("Atlas research", "atlas.md"),
                load("Security notes", "security-notes.md"), load("Vietnamese learning", "vietnamese-learning.md"),
                load("Meeting notes", "meeting-notes.txt"), load("Greenhouse distractor", "irrelevant.md"));
    }

    private static BenchmarkDocument load(String title, String filename) throws Exception {
        Path path = resolveFixture(filename);
        return new BenchmarkDocument(title, filename, Files.readString(path, StandardCharsets.UTF_8));
    }

    private static Path resolveFixture(String filename) {
        for (Path candidate : List.of(Path.of("..", "..", "refer", "qa_dataset", "fixtures", filename),
                Path.of("..", "refer", "qa_dataset", "fixtures", filename),
                Path.of("refer", "qa_dataset", "fixtures", filename))) {
            if (Files.exists(candidate)) return candidate;
        }
        throw new IllegalStateException("Benchmark fixture not found: " + filename);
    }

    private static List<String> chunks(String content) {
        List<String> result = new ArrayList<>();
        for (String paragraph : content.split("\\R\\s*\\R")) {
            String value = paragraph.trim();
            if (!value.isBlank()) result.add(value.length() > 1600 ? value.substring(0, 1600) : value);
        }
        return result;
    }

    private static final class ModelAdapter {
        private final Client client;
        private final String model;
        private ModelAdapter(Client client, String model) { this.client = client; this.model = model; }

        float[] embedDocument(String title, String text) { return embed(title, text, false); }
        float[] embedQuery(String query) { return embed(null, query, true); }

        private float[] embed(String title, String text, boolean query) {
            boolean embedding2 = "gemini-embedding-2".equals(model);
            String input = embedding2
                    ? (query ? "task: search result | query: " + text : "title: " + Objects.toString(title, "none") + " | text: " + text)
                    : text;
            EmbedContentConfig.Builder config = EmbedContentConfig.builder().outputDimensionality(DIMENSIONS);
            if (!embedding2) config.taskType(query ? "RETRIEVAL_QUERY" : "RETRIEVAL_DOCUMENT");
            EmbedContentResponse response = client.models.embedContent(model, input, config.build());
            List<ContentEmbedding> embeddings = response.embeddings().orElseThrow(
                    () -> new IllegalStateException("No embedding returned for " + model));
            if (embeddings.size() != 1) throw new IllegalStateException("Expected one isolated embedding for " + model);
            return EmbeddingVectorNormalizer.normalize(embeddings.getFirst().values().orElseThrow(), DIMENSIONS);
        }
    }

    record BenchmarkDocument(String title, String filename, String content) { }
    record BenchmarkCase(String id, String question, String expectedFile, Set<String> allowedFiles) { }
    record IndexedVector(String model, String filename, float[] vector) { }

    static final class SemanticSimilarityForBenchmark {
        static double cosine(float[] left, float[] right) {
            double dot=0,leftNorm=0,rightNorm=0;
            for(int i=0;i<left.length;i++){dot+=left[i]*right[i];leftNorm+=left[i]*left[i];rightNorm+=right[i]*right[i];}
            return dot/(Math.sqrt(leftNorm)*Math.sqrt(rightNorm));
        }
    }
}
