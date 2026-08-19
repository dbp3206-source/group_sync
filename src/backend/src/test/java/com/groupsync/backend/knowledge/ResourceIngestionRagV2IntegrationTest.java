package com.groupsync.backend.knowledge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groupsync.backend.knowledge.chunking.StructureAwareChunkingStrategy;
import com.groupsync.backend.knowledge.chunking.StructureAwareChunkingStrategy.HierarchicalChunk;
import com.groupsync.backend.knowledge.ingestion.*;
import com.groupsync.backend.knowledge.model.*;
import com.groupsync.backend.knowledge.rag.EmbeddingProvider;
import com.groupsync.backend.knowledge.rag.EmbeddingTextBuilder;
import com.groupsync.backend.knowledge.rag.EmbeddingTextBuilder.SemanticMetadata;
import com.groupsync.backend.knowledge.repository.ResourceRepository;
import com.groupsync.backend.knowledge.service.ResourceIngestionService;
import com.groupsync.backend.knowledge.service.ResourceIngestionTransactionService;
import com.groupsync.backend.knowledge.storage.StorageService;
import com.groupsync.backend.user.model.UserAccount;

/**
 * Execution-path integration test verifying KnowledgeOS RAG v2 Ingestion orchestration.
 * Proves that StructureAwareChunkingStrategy, EmbeddingTextBuilder, embedDocuments batching,
 * and Parent-Child persistence are executed on the real ingestion path.
 */
@ExtendWith(MockitoExtension.class)
class ResourceIngestionRagV2IntegrationTest {

    @Mock private ResourceRepository resourceRepository;
    @Mock private ResourceParserRegistry parserRegistry;
    @Mock private StructureAwareChunkingStrategy chunkingStrategy;
    @Mock private StorageService storageService;
    @Mock private EmbeddingProvider embeddingProvider;
    @Mock private EmbeddingTextBuilder embeddingTextBuilder;
    @Mock private ResourceIngestionTransactionService transactionService;
    @Mock private ResourceParser resourceParser;

    private ResourceIngestionService ingestionService;

    private final Long resourceId = 42L;
    private Resource testResource;
    private UserAccount owner;

    @BeforeEach
    void setUp() {
        ingestionService = new ResourceIngestionService(
                resourceRepository, parserRegistry, chunkingStrategy,
                storageService, embeddingProvider, embeddingTextBuilder, transactionService
        );

        owner = new UserAccount("author@knowledgeos.io", "hash", "Author");
        testResource = new Resource(
                owner, "Advanced AI Architecture", "Comprehensive guide",
                ResourceType.MARKDOWN, "architecture.md", "text/markdown",
                2048L, "42/architecture.md", "hash123"
        );
        ReflectionTestUtils.setField(testResource, "id", resourceId);
    }

    @Test
    void process_fullV2Pipeline_executesStructureAwareChunkingRichEmbeddingAndHierarchicalSave() throws Exception {
        when(transactionService.claim(resourceId)).thenReturn(true);
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(testResource));
        when(storageService.open("42/architecture.md")).thenReturn(new ByteArrayInputStream("# Advanced AI Architecture\nContent".getBytes(StandardCharsets.UTF_8)));
        when(parserRegistry.forType(ResourceType.MARKDOWN)).thenReturn(resourceParser);

        ParsedDocument parsedDoc = new ParsedDocument(
                "Advanced AI Architecture",
                "Advanced AI Architecture\nParent and child chunks provide optimal precision.",
                List.of(
                        new ParsedBlock(BlockType.HEADING, "Advanced AI Architecture", "Advanced AI Architecture", 1, 0),
                        new ParsedBlock(BlockType.PARAGRAPH, "Advanced AI Architecture", "Parent and child chunks provide optimal precision.", 1, 1)
                )
        );
        when(resourceParser.parseDocument(any(InputStream.class))).thenReturn(parsedDoc);

        HierarchicalChunk parentChunk = new HierarchicalChunk(0, ChunkLevel.PARENT, null, 1, "Advanced AI Architecture", "Parent section full text context.");
        HierarchicalChunk childChunk = new HierarchicalChunk(1, ChunkLevel.CHILD, 0, 1, "Advanced AI Architecture", "Child precision chunk content.");
        when(chunkingStrategy.chunkDocument(parsedDoc)).thenReturn(List.of(parentChunk, childChunk));

        SemanticMetadata resourceMeta = new SemanticMetadata("Advanced AI Architecture", List.of("AI Core"), List.of("RAG", "LLM"), null);
        when(transactionService.fetchSemanticMetadata(resourceId)).thenReturn(resourceMeta);

        when(embeddingTextBuilder.build(any(SemanticMetadata.class), eq("Child precision chunk content.")))
                .thenReturn("Document: Advanced AI Architecture\nCollection: AI Core\nTags: RAG, LLM\nSection: Advanced AI Architecture\n\nChild precision chunk content.");

        float[] mockVector = new float[768];
        mockVector[0] = 0.42f;
        when(embeddingProvider.embedDocuments(anyList())).thenReturn(List.of(mockVector));

        ingestionService.process(resourceId);

        // Verification of entire v2 call chain
        verify(transactionService, times(1)).claim(resourceId);
        verify(chunkingStrategy, times(1)).chunkDocument(parsedDoc);
        verify(transactionService, times(1)).fetchSemanticMetadata(resourceId);
        verify(embeddingTextBuilder, times(1)).build(any(SemanticMetadata.class), eq("Child precision chunk content."));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> richTextsCaptor = ArgumentCaptor.forClass(List.class);
        verify(embeddingProvider, times(1)).embedDocuments(richTextsCaptor.capture());
        assertEquals(1, richTextsCaptor.getValue().size());
        assertTrue(richTextsCaptor.getValue().getFirst().contains("Collection: AI Core"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<HierarchicalChunk>> chunksCaptor = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Integer, float[]>> embeddingsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(transactionService, times(1)).saveReadyHierarchical(eq(resourceId), chunksCaptor.capture(), embeddingsCaptor.capture());

        List<HierarchicalChunk> savedChunks = chunksCaptor.getValue();
        assertEquals(2, savedChunks.size());
        assertEquals(ChunkLevel.PARENT, savedChunks.get(0).level());
        assertEquals(ChunkLevel.CHILD, savedChunks.get(1).level());
        assertEquals(0, savedChunks.get(1).parentIndex());

        Map<Integer, float[]> savedEmbeddings = embeddingsCaptor.getValue();
        assertNull(savedEmbeddings.get(0), "Parent chunks must not have search embedding");
        assertNotNull(savedEmbeddings.get(1), "Child chunks must have search embedding");
        assertEquals(0.42f, savedEmbeddings.get(1)[0]);
    }

    @Test
    void process_embeddingBatchFailure_marksResourceFailedWithoutCorruptReadyState() throws Exception {
        when(transactionService.claim(resourceId)).thenReturn(true);
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(testResource));
        when(storageService.open("42/architecture.md")).thenReturn(new ByteArrayInputStream("Test text".getBytes(StandardCharsets.UTF_8)));
        when(parserRegistry.forType(ResourceType.MARKDOWN)).thenReturn(resourceParser);

        ParsedDocument parsedDoc = new ParsedDocument("Doc", "Test", List.of(new ParsedBlock(BlockType.PARAGRAPH, "Intro", "Test", 1, 0)));
        when(resourceParser.parseDocument(any(InputStream.class))).thenReturn(parsedDoc);

        HierarchicalChunk child = new HierarchicalChunk(0, ChunkLevel.CHILD, null, 1, "Intro", "Test");
        when(chunkingStrategy.chunkDocument(parsedDoc)).thenReturn(List.of(child));
        when(transactionService.fetchSemanticMetadata(resourceId)).thenReturn(new SemanticMetadata("Doc", List.of(), List.of(), null));
        when(embeddingTextBuilder.build(any(), anyString())).thenReturn("Rich text");

        when(embeddingProvider.embedDocuments(anyList())).thenThrow(new IllegalStateException("Gemini embedding quota exhausted (429)"));

        ingestionService.process(resourceId);

        verify(transactionService, times(1)).markFailed(eq(resourceId), contains("Gemini embedding quota exhausted (429)"));
        verify(transactionService, never()).saveReadyHierarchical(anyLong(), anyList(), anyMap());
    }

    @Test
    void reindex_usesV2PipelineWithClaimForReindex() throws Exception {
        when(transactionService.claimForReindex(resourceId)).thenReturn(true);
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(testResource));
        when(storageService.open("42/architecture.md")).thenReturn(new ByteArrayInputStream("Reindex text".getBytes(StandardCharsets.UTF_8)));
        when(parserRegistry.forType(ResourceType.MARKDOWN)).thenReturn(resourceParser);

        ParsedDocument parsedDoc = new ParsedDocument("Doc", "Reindex", List.of(new ParsedBlock(BlockType.PARAGRAPH, "Sec", "Reindex", 1, 0)));
        when(resourceParser.parseDocument(any(InputStream.class))).thenReturn(parsedDoc);

        HierarchicalChunk child = new HierarchicalChunk(0, ChunkLevel.CHILD, null, 1, "Sec", "Reindex");
        when(chunkingStrategy.chunkDocument(parsedDoc)).thenReturn(List.of(child));
        when(transactionService.fetchSemanticMetadata(resourceId)).thenReturn(new SemanticMetadata("Doc", List.of(), List.of(), null));
        when(embeddingTextBuilder.build(any(), anyString())).thenReturn("Rich text");
        when(embeddingProvider.embedDocuments(anyList())).thenReturn(List.of(new float[768]));

        ingestionService.reindex(resourceId);

        verify(transactionService, times(1)).claimForReindex(resourceId);
        verify(transactionService, times(1)).saveReadyHierarchical(eq(resourceId), anyList(), anyMap());
    }
}
