package com.groupsync.backend.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.groupsync.backend.knowledge.model.Resource;
import com.groupsync.backend.knowledge.model.ResourceProcessingStatus;
import com.groupsync.backend.knowledge.model.ResourceType;
import com.groupsync.backend.user.model.UserAccount;

class ResourceLifecycleTest {
    @Test
    void resourceMovesThroughTheControlledProcessingLifecycle() {
        Resource resource = new Resource(new UserAccount("reader@example.com", "hash", "Reader"), "Notes", null,
            ResourceType.TEXT, "notes.txt", "text/plain", 12, "1/notes.txt", "checksum");

        resource.beginParsing();
        resource.beginChunking();
        resource.beginEmbedding();
        resource.markReady();

        assertEquals(ResourceProcessingStatus.READY, resource.getProcessingStatus());
        assertThrows(IllegalStateException.class, resource::beginParsing);
    }

    @Test
    void failedResourceCanBeResetForRetry() {
        Resource resource = new Resource(new UserAccount("reader@example.com", "hash", "Reader"), "Notes", null,
            ResourceType.TEXT, "notes.txt", "text/plain", 12, "1/notes.txt", "checksum");

        resource.markFailed("Parser error");
        resource.retry();

        assertEquals(ResourceProcessingStatus.UPLOADED, resource.getProcessingStatus());
        assertEquals(null, resource.getProcessingError());
    }
}
