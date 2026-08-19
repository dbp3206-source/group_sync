package com.groupsync.backend.knowledge.dto;

import java.util.List;

public record PagedResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        boolean hasNext
) {
    public static <T> PagedResponse<T> of(List<T> items, int page, int size, long totalItems) {
        int totalPages = size <= 0 ? 1 : (int) Math.ceil((double) totalItems / size);
        boolean hasNext = (long) (page + 1) * size < totalItems;
        return new PagedResponse<>(items, page, size, totalItems, totalPages, hasNext);
    }
}
