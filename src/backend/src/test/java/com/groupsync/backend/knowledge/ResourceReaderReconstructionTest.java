package com.groupsync.backend.knowledge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.groupsync.backend.knowledge.ingestion.MarkdownResourceParser;
import com.groupsync.backend.knowledge.ingestion.ResourceParserRegistry;
import com.groupsync.backend.knowledge.model.DocumentChunk;
import com.groupsync.backend.knowledge.model.Resource;
import com.groupsync.backend.knowledge.model.ResourceType;
import com.groupsync.backend.knowledge.repository.CitationRepository;
import com.groupsync.backend.knowledge.repository.DocumentChunkRepository;
import com.groupsync.backend.knowledge.repository.ResourceRepository;
import com.groupsync.backend.knowledge.service.ResourceService;
import com.groupsync.backend.knowledge.storage.StorageService;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class ResourceReaderReconstructionTest {

    @Mock private ResourceRepository resourceRepository;
    @Mock private UserAccountRepository userRepository;
    @Mock private StorageService storageService;
    @Mock private ApplicationEventPublisher events;
    @Mock private CitationRepository citationRepository;
    @Mock private DocumentChunkRepository chunkRepository;

    private ResourceParserRegistry parserRegistry;
    private ResourceService resourceService;

    @BeforeEach
    void setUp() {
        parserRegistry = new ResourceParserRegistry(List.of(new MarkdownResourceParser()));
        resourceService = new ResourceService(
                resourceRepository, userRepository, storageService,
                events, citationRepository, chunkRepository, parserRegistry
        );
    }

    @Test
    void readerLoadsCanonicalDocumentTextWithoutChunkOverlapDuplication() throws Exception {
        Long ownerId = 1L;
        Long resourceId = 99L;
        UserAccount owner = new UserAccount("test@example.com", "hash", "Tester");
        Resource resource = new Resource(
                owner, "Architecture Overview", "Summary",
                ResourceType.MARKDOWN, "arch.md", "text/markdown",
                500L, "1/arch.md", "sha256-hash"
        );
        ReflectionTestUtils.setField(resource, "id", resourceId);

        // Canonical full text in storage
        String canonicalContent = "# Architecture\n\nSection A explains modular design.\n\nSection B details vector search.";
        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Optional.of(resource));
        when(storageService.open("1/arch.md")).thenReturn(new ByteArrayInputStream(canonicalContent.getBytes(StandardCharsets.UTF_8)));

        String resultText = resourceService.extractedText(ownerId, resourceId);

        // Result MUST match canonical content exactly and MUST NOT contain duplicated text from chunk overlap
        assertEquals(canonicalContent, resultText);
        // Ensure "Section A explains modular design." occurs exactly once in reader output
        int count = 0;
        int idx = 0;
        while ((idx = resultText.indexOf("Section A explains modular design.", idx)) != -1) {
            count++;
            idx += 10;
        }
        assertEquals(1, count, "Canonical reader content must not duplicate text from overlapping chunks");
    }
}
