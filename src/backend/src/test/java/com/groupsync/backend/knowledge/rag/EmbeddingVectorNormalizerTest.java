package com.groupsync.backend.knowledge.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmbeddingVectorNormalizerTest {
    @Test
    void normalizesAValidVector() {
        float[] result = EmbeddingVectorNormalizer.normalize(List.of(3f, 4f), 2);

        assertEquals(0.6f, result[0], 0.0001f);
        assertEquals(0.8f, result[1], 0.0001f);
    }

    @Test
    void rejectsAnUnexpectedDimension() {
        assertThrows(IllegalStateException.class,
                () -> EmbeddingVectorNormalizer.normalize(Collections.singletonList(1f), 2));
    }
}
