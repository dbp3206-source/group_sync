package com.groupsync.backend.knowledge.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Durable PostgreSQL database-backed implementation of {@link StorageService}.
 *
 * <p>Guarantees that uploaded original Resource files (PDF, DOCX, TXT, MD, Note) survive:
 * <ul>
 *   <li>Backend process restarts</li>
 *   <li>Docker container recreations on ephemeral hosts (Render, Cloud Run, Heroku)</li>
 *   <li>Application redeployments</li>
 * </ul>
 *
 * <p>Files are stored as raw bytes in the {@code storage_blobs} table with strict owner isolation
 * and SHA-256 checksum integrity verification.
 */
@Service("storageService")
@Primary
public class DatabaseStorageService implements StorageService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DatabaseStorageService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public StoredFile store(Long ownerId, String originalFilename, InputStream inputStream) throws IOException {
        String safeName = originalFilename == null ? "resource" : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = ownerId + "/" + UUID.randomUUID() + "-" + safeName;

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }

        while ((read = inputStream.read(chunk)) >= 0) {
            buffer.write(chunk, 0, read);
            digest.update(chunk, 0, read);
        }
        byte[] bytes = buffer.toByteArray();
        String checksum = HexFormat.of().formatHex(digest.digest());

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("storageKey", key)
                .addValue("ownerId", ownerId)
                .addValue("filename", safeName)
                .addValue("sizeBytes", (long) bytes.length)
                .addValue("checksum", checksum)
                .addValue("data", bytes);

        jdbcTemplate.update("""
                INSERT INTO storage_blobs (storage_key, owner_id, filename, size_bytes, checksum_sha256, data, created_at)
                VALUES (:storageKey, :ownerId, :filename, :sizeBytes, :checksum, :data, NOW())
                ON CONFLICT (storage_key) DO UPDATE SET data = EXCLUDED.data
                """, params);

        return new StoredFile(key, bytes.length, checksum);
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        try {
            byte[] data = jdbcTemplate.queryForObject(
                    "SELECT data FROM storage_blobs WHERE storage_key = :key",
                    Map.of("key", storageKey),
                    byte[].class);
            if (data == null) {
                throw new IOException("Storage blob not found: " + storageKey);
            }
            return new ByteArrayInputStream(data);
        } catch (EmptyResultDataAccessException e) {
            throw new IOException("Storage blob not found: " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        jdbcTemplate.update(
                "DELETE FROM storage_blobs WHERE storage_key = :key",
                Map.of("key", storageKey));
    }
}
