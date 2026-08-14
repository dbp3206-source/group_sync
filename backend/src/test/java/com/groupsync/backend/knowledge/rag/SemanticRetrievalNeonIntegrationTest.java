package com.groupsync.backend.knowledge.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.groupsync.backend.knowledge.model.DocumentChunk;
import com.groupsync.backend.knowledge.model.Resource;
import com.groupsync.backend.knowledge.model.ResourceType;
import com.groupsync.backend.knowledge.repository.DocumentChunkRepository;
import com.groupsync.backend.knowledge.repository.ResourceRepository;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = ".+")
class SemanticRetrievalNeonIntegrationTest {
    @Autowired private UserAccountRepository userRepository;
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private DocumentChunkRepository chunkRepository;
    @Autowired private SemanticRetrievalRepository retrievalRepository;

    @Test
    void persistsAndRetrieves768DimensionVectorsUsingPgvector() {
        UserAccount owner = userRepository.save(new UserAccount("vector-" + UUID.randomUUID() + "@example.test", "not-a-login", "Vector Test"));
        Resource resource = resourceRepository.save(new Resource(owner, "Vector retrieval", null, ResourceType.NOTE,
                null, "text/markdown", 0, "test/vector", UUID.randomUUID().toString().replace("-", "")));
        DocumentChunk chunk = new DocumentChunk(resource, 0, null, "Test", "Semantic retrieval should find this source.");
        chunk.embed(unitVector(0), "gemini-embedding-001");
        chunkRepository.saveAndFlush(chunk);

        List<RetrievedChunk> results = retrievalRepository.findNearest(owner.getId(), unitVector(0),
                RetrievalScope.THIS_RESOURCE, resource.getId(), List.of(resource.getId()), null, 3);

        assertFalse(results.isEmpty());
        assertEquals(chunk.getId(), results.getFirst().chunkId());
        assertEquals(0.0d, results.getFirst().distance(), 0.0001d);
    }

    private float[] unitVector(int index) {
        float[] vector = new float[768];
        vector[index] = 1f;
        return vector;
    }
}
