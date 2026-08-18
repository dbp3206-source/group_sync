package com.groupsync.backend.knowledge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groupsync.backend.knowledge.chunking.RecursiveChunkingStrategy;
import com.groupsync.backend.knowledge.ingestion.ParsedResourceContent;
import com.groupsync.backend.knowledge.ingestion.ResourceParser;
import com.groupsync.backend.knowledge.ingestion.ResourceParserRegistry;
import com.groupsync.backend.knowledge.model.DocumentChunk;
import com.groupsync.backend.knowledge.model.Resource;
import com.groupsync.backend.knowledge.model.ResourceProcessingStatus;
import com.groupsync.backend.knowledge.model.ResourceType;
import com.groupsync.backend.knowledge.rag.EmbeddingProvider;
import com.groupsync.backend.knowledge.rag.GeminiProperties;
import com.groupsync.backend.knowledge.repository.DocumentChunkRepository;
import com.groupsync.backend.knowledge.repository.ResourceRepository;
import com.groupsync.backend.knowledge.service.AutoOrganizationService;
import com.groupsync.backend.knowledge.service.ResourceIngestionService;
import com.groupsync.backend.knowledge.service.ResourceIngestionTransactionService;
import com.groupsync.backend.knowledge.storage.StorageService;
import com.groupsync.backend.user.model.UserAccount;

@ExtendWith(MockitoExtension.class)
class ResourceIngestionConcurrencyTest {

    @Mock private ResourceRepository resourceRepository;
    @Mock private DocumentChunkRepository chunkRepository;
    @Mock private ResourceParserRegistry parserRegistry;
    @Mock private RecursiveChunkingStrategy chunkingStrategy;
    @Mock private StorageService storageService;
    @Mock private EmbeddingProvider embeddingProvider;
    @Mock private GeminiProperties geminiProperties;
    @Mock private AutoOrganizationService autoOrganizationService;
    @Mock private ResourceIngestionTransactionService transactionService;

    @InjectMocks private ResourceIngestionService ingestionService;

    @Test
    void transactionServiceClaimReturnsTrueOnFirstAttemptAndFalseOnSubsequentAttempt() {
        ResourceIngestionTransactionService txService = new ResourceIngestionTransactionService(
                resourceRepository, chunkRepository, geminiProperties, autoOrganizationService);

        Long resourceId = 100L;

        // First attempt: status is UPLOADED -> updated rows = 1 -> claim succeeds
        when(resourceRepository.updateStatusIfMatches(resourceId, ResourceProcessingStatus.UPLOADED, ResourceProcessingStatus.PARSING))
                .thenReturn(1)
                .thenReturn(0); // Second attempt: status already PARSING -> updated rows = 0 -> claim fails

        boolean firstClaim = txService.claim(resourceId);
        boolean secondClaim = txService.claim(resourceId);

        assertTrue(firstClaim, "First claim attempt on UPLOADED resource must succeed");
        assertFalse(secondClaim, "Second concurrent claim attempt must return false");
        verify(resourceRepository, times(2)).updateStatusIfMatches(resourceId, ResourceProcessingStatus.UPLOADED, ResourceProcessingStatus.PARSING);
    }

    @Test
    void twoProcessAttemptsResultInOnlyOneActualIngestionWithoutDuplicateProcessing() throws IOException {
        Long resourceId = 200L;
        UserAccount owner = new UserAccount("user@example.com", "hash", "User");
        Resource resource = new Resource(owner, "Concurrent Guide", null, ResourceType.MARKDOWN,
                "guide.md", "text/markdown", 500L, "200/guide.md", "hash200");

        // First claim succeeds, second claim fails
        when(transactionService.claim(resourceId)).thenReturn(true).thenReturn(false);
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));

        when(storageService.open("200/guide.md")).thenReturn(new ByteArrayInputStream("# Title\nContent".getBytes()));
        ResourceParser mockParser = mock(ResourceParser.class);
        when(parserRegistry.forType(ResourceType.MARKDOWN)).thenReturn(mockParser);
        when(mockParser.parse(any())).thenReturn(new ParsedResourceContent("Extracted content text", null, "Title"));
        when(chunkingStrategy.chunk("Extracted content text")).thenReturn(List.of("Chunk 1", "Chunk 2"));
        when(embeddingProvider.embedDocument(anyString())).thenReturn(new float[768]);

        // Attempt 1
        ingestionService.process(resourceId);
        // Attempt 2 (concurrent / retry)
        ingestionService.process(resourceId);

        // Verification: parse, chunk, embed, and saveReady were called EXACTLY ONCE
        verify(transactionService, times(2)).claim(resourceId);
        verify(storageService, times(1)).open(anyString());
        verify(embeddingProvider, times(2)).embedDocument(anyString()); // 2 chunks, 1 time each
        verify(transactionService, times(1)).saveReady(eq(resourceId), anyList(), anyList(), any());
    }

    @Test
    void saveReadyDeletesExistingChunksFirstToPreventDuplicateChunks() {
        ResourceIngestionTransactionService txService = new ResourceIngestionTransactionService(
                resourceRepository, chunkRepository, geminiProperties, autoOrganizationService);

        Long resourceId = 300L;
        UserAccount owner = new UserAccount("user@example.com", "hash", "User");
        Resource resource = new Resource(owner, "Guide", null, ResourceType.MARKDOWN,
                "guide.md", "text/markdown", 500L, "300/guide.md", "hash300");

        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(geminiProperties.embeddingModel()).thenReturn("gemini-embedding-001");
        resource.beginParsing();

        List<String> pieces = List.of("Section 1", "Section 2");
        List<float[]> embeddings = List.of(new float[768], new float[768]);
        ParsedResourceContent parsed = new ParsedResourceContent("Section 1 Section 2", 1, "Intro");

        txService.saveReady(resourceId, pieces, embeddings, parsed);

        // MUST delete previous chunks before saving new chunks
        org.mockito.InOrder order = inOrder(chunkRepository, resourceRepository);
        order.verify(chunkRepository).deleteByResourceId(resource.getId());
        order.verify(chunkRepository).saveAll(anyList());
        order.verify(resourceRepository).save(resource);
        assertEquals(ResourceProcessingStatus.READY, resource.getProcessingStatus());
    }
}
