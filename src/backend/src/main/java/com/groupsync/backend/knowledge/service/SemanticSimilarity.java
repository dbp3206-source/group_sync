package com.groupsync.backend.knowledge.service;

public final class SemanticSimilarity {
    private SemanticSimilarity() { }

    public static double cosine(float[] left, float[] right) {
        if (left == null || right == null || left.length == 0 || left.length != right.length) return -1d;
        double dot = 0d, leftNorm = 0d, rightNorm = 0d;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0d || rightNorm == 0d) return -1d;
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
