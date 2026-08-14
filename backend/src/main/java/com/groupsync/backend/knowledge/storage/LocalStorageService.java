package com.groupsync.backend.knowledge.storage;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LocalStorageService implements StorageService {
    private final Path storageRoot;
    public LocalStorageService(@Value("${knowledge.storage.local-root:./knowledgeos-storage}") String root) {
        this.storageRoot = Paths.get(root).toAbsolutePath().normalize();
    }
    @Override public StoredFile store(Long ownerId, String originalFilename, InputStream inputStream) throws IOException {
        String safeName = originalFilename == null ? "resource" : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = ownerId + "/" + UUID.randomUUID() + "-" + safeName;
        Path destination = resolve(key);
        Files.createDirectories(destination.getParent());
        MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
        long size = 0;
        try (InputStream source = inputStream; OutputStream output = Files.newOutputStream(destination, StandardOpenOption.CREATE_NEW)) {
            byte[] buffer = new byte[8192]; int read;
            while ((read = source.read(buffer)) >= 0) { output.write(buffer, 0, read); digest.update(buffer, 0, read); size += read; }
        } catch (IOException exception) { Files.deleteIfExists(destination); throw exception; }
        return new StoredFile(key, size, HexFormat.of().formatHex(digest.digest()));
    }
    @Override public InputStream open(String storageKey) throws IOException { return Files.newInputStream(resolve(storageKey)); }
    @Override public void delete(String storageKey) throws IOException { Files.deleteIfExists(resolve(storageKey)); }
    private Path resolve(String storageKey) {
        Path path = storageRoot.resolve(storageKey).normalize();
        if (!path.startsWith(storageRoot)) throw new SecurityException("Invalid storage key.");
        return path;
    }
}
