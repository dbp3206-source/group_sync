package com.groupsync.backend.knowledge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.groupsync.backend.knowledge.model.Resource;
import com.groupsync.backend.knowledge.model.ResourceType;
import com.groupsync.backend.knowledge.repository.CitationRepository;
import com.groupsync.backend.knowledge.repository.ResourceRepository;
import com.groupsync.backend.knowledge.service.ResourceService;
import com.groupsync.backend.knowledge.storage.StorageService;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

/**
 * Verifies that ResourceService.delete() deletes citations before the resource
 * to avoid the ON DELETE RESTRICT FK violation on citations.document_chunk_id.
 *
 * Historical chat sessions and messages are preserved; only citations that
 * reference the deleted resource's chunks are removed.
 */
@ExtendWith(MockitoExtension.class)
class ResourceDeleteWithCitationsTest {

    @Mock private ResourceRepository resourceRepository;
    @Mock private UserAccountRepository userRepository;
    @Mock private StorageService storageService;
    @Mock private ApplicationEventPublisher events;
    @Mock private CitationRepository citationRepository;
    @Mock private com.groupsync.backend.knowledge.repository.DocumentChunkRepository chunkRepository;
    @Mock private com.groupsync.backend.knowledge.ingestion.ResourceParserRegistry parserRegistry;

    private ResourceService resourceService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        resourceService = new ResourceService(
                org.springframework.util.unit.DataSize.ofMegabytes(25),
                resourceRepository, userRepository, storageService,
                events, citationRepository, chunkRepository, parserRegistry
        );
    }

    @Test
    void deleteCitationsBeforeResourceToAvoidFkViolation() throws IOException {
        Long ownerId = 1L;
        Long resourceId = 42L;
        UserAccount owner = new UserAccount("owner@example.com", "hash", "Owner");
        Resource resource = new Resource(owner, "Test Resource", null, ResourceType.MARKDOWN,
                "test.md", "text/markdown", 100L, "1/test.md", "abc123");

        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Optional.of(resource));

        resourceService.delete(ownerId, resourceId);

        // Citations for this resource's chunks MUST be deleted before the resource itself.
        // If the order is reversed, ON DELETE RESTRICT on citations.document_chunk_id causes FK violation.
        InOrder order = inOrder(citationRepository, storageService, resourceRepository);
        order.verify(citationRepository).deleteByChunkResourceId(resourceId);
        order.verify(storageService).delete(resource.getStorageKey());
        order.verify(resourceRepository).delete(resource);
    }

    @Test
    void deleteSucceedsWhenNoCitationsExist() throws IOException {
        Long ownerId = 1L;
        Long resourceId = 99L;
        UserAccount owner = new UserAccount("owner@example.com", "hash", "Owner");
        Resource resource = new Resource(owner, "Fresh Resource", null, ResourceType.NOTE,
                null, "text/markdown", 50L, "1/fresh.md", "def456");

        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Optional.of(resource));

        // deleteByChunkResourceId is a no-op when no citations exist — must not throw
        doNothing().when(citationRepository).deleteByChunkResourceId(resourceId);

        assertDoesNotThrow(() -> resourceService.delete(ownerId, resourceId));
        verify(citationRepository).deleteByChunkResourceId(resourceId);
        verify(resourceRepository).delete(resource);
    }
}
