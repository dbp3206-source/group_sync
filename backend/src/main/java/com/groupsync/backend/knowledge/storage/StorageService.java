package com.groupsync.backend.knowledge.storage;

import java.io.IOException;
import java.io.InputStream;

public interface StorageService {
    StoredFile store(Long ownerId, String originalFilename, InputStream inputStream) throws IOException;
    InputStream open(String storageKey) throws IOException;
    void delete(String storageKey) throws IOException;
    record StoredFile(String key, long sizeBytes, String checksumSha256) { }
}
