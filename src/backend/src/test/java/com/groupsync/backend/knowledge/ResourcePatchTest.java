package com.groupsync.backend.knowledge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.groupsync.backend.knowledge.dto.ResourceResponse;
import com.groupsync.backend.knowledge.dto.UpdateResourceRequest;
import com.groupsync.backend.knowledge.ingestion.ResourceParserRegistry;
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
class ResourcePatchTest {

    @Mock private ResourceRepository resourceRepository;
    @Mock private UserAccountRepository userRepository;
    @Mock private StorageService storageService;
    @Mock private ApplicationEventPublisher events;
    @Mock private CitationRepository citationRepository;
    @Mock private DocumentChunkRepository chunkRepository;
    @Mock private ResourceParserRegistry parserRegistry;

    private ResourceService resourceService;

    @BeforeEach
    void setUp() {
        resourceService = new ResourceService(
                org.springframework.util.unit.DataSize.ofMegabytes(25),
                resourceRepository, userRepository, storageService,
                events, citationRepository, chunkRepository, parserRegistry
        );
    }

    @Test
    void partialPatchOnlyUpdatesSuppliedFieldsPreservingOthers() {
        Long ownerId = 1L;
        Long resourceId = 55L;
        UserAccount owner = new UserAccount("test@example.com", "hash", "Tester");
        Resource resource = new Resource(
                owner, "Original Title", "Original Description",
                ResourceType.MARKDOWN, "test.md", "text/markdown",
                200L, "1/test.md", "checksum"
        );
        resource.updateMetadata("Original Title", "Original Description", false, 1);
        ReflectionTestUtils.setField(resource, "id", resourceId);

        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Optional.of(resource));

        // Partial request with ONLY favorite=true (title, description, priority are null)
        UpdateResourceRequest patchRequest = new UpdateResourceRequest(null, null, true, null);

        ResourceResponse response = resourceService.update(ownerId, resourceId, patchRequest);

        assertTrue(response.favorite(), "Favorite should be updated to true");
        assertEquals("Original Title", response.title(), "Title should be preserved");
        assertEquals("Original Description", response.description(), "Description should be preserved");
        assertEquals(1, response.priority(), "Priority should be preserved");
    }

    @Test
    void partialPatchCanUpdatePriorityOnly() {
        Long ownerId = 1L;
        Long resourceId = 55L;
        UserAccount owner = new UserAccount("test@example.com", "hash", "Tester");
        Resource resource = new Resource(
                owner, "Doc Title", "Doc Description",
                ResourceType.PDF, "doc.pdf", "application/pdf",
                5000L, "1/doc.pdf", "checksum"
        );
        resource.updateMetadata("Doc Title", "Doc Description", true, 0);
        ReflectionTestUtils.setField(resource, "id", resourceId);

        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Optional.of(resource));

        UpdateResourceRequest patchRequest = new UpdateResourceRequest(null, null, null, 4);

        ResourceResponse response = resourceService.update(ownerId, resourceId, patchRequest);

        assertEquals(4, response.priority(), "Priority should be updated to 4");
        assertTrue(response.favorite(), "Favorite should remain true");
        assertEquals("Doc Title", response.title());
    }
}
