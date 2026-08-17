package com.groupsync.backend.knowledge.rag;

import java.util.List;

final class EmbeddingVectorNormalizer {
    private EmbeddingVectorNormalizer() { }

    static float[] normalize(List<Float> source, int requiredDimensions) {
        if (source == null || source.size() != requiredDimensions) {
            throw new IllegalStateException("Gemini returned an embedding with an unexpected dimension.");
        }

        double sumOfSquares = 0;
        float[] normalized = new float[requiredDimensions];
        for (int index = 0; index < source.size(); index++) {
            Float value = source.get(index);
            if (value == null || !Float.isFinite(value)) {
                throw new IllegalStateException("Gemini returned an invalid embedding value.");
            }
            normalized[index] = value;
            sumOfSquares += value * value;
        }

        double magnitude = Math.sqrt(sumOfSquares);
        if (magnitude == 0) {
            throw new IllegalStateException("Gemini returned a zero embedding vector.");
        }
        for (int index = 0; index < normalized.length; index++) {
            normalized[index] /= (float) magnitude;
        }
        return normalized;
    }
}
