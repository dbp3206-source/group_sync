package com.groupsync.backend.knowledge.storage;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class DatabaseStorageServiceTest {

    private NamedParameterJdbcTemplate jdbcTemplate;
    private DatabaseStorageService storageService;

    @BeforeEach
    void setUp() {
        jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        storageService = new DatabaseStorageService(jdbcTemplate);
    }

    @Test
    void storesBlobAndComputesChecksum() throws IOException {
        String content = "Hello KnowledgeOS durable storage";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        StorageService.StoredFile stored = storageService.store(1L, "notes.md", new ByteArrayInputStream(bytes));

        assertNotNull(stored.key());
        assertTrue(stored.key().startsWith("1/"));
        assertTrue(stored.key().endsWith("-notes.md"));
        assertEquals(bytes.length, stored.sizeBytes());
        assertNotNull(stored.checksumSha256());
        assertEquals(64, stored.checksumSha256().length());

        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        Mockito.verify(jdbcTemplate).update(Mockito.anyString(), captor.capture());
        MapSqlParameterSource params = captor.getValue();
        assertEquals(1L, params.getValue("ownerId"));
        assertEquals("notes.md", params.getValue("filename"));
        assertEquals((long) bytes.length, params.getValue("sizeBytes"));
        assertArrayEquals(bytes, (byte[]) params.getValue("data"));
    }

    @Test
    void opensBlobSuccessfully() throws IOException {
        byte[] expectedData = "persisted resource content".getBytes(StandardCharsets.UTF_8);
        Mockito.when(jdbcTemplate.queryForObject(Mockito.anyString(), Mockito.anyMap(), Mockito.eq(byte[].class)))
                .thenReturn(expectedData);

        InputStream stream = storageService.open("1/test-key-resource.pdf");

        assertNotNull(stream);
        byte[] actualData = stream.readAllBytes();
        assertArrayEquals(expectedData, actualData);
    }

    @Test
    void throwsIOExceptionWhenBlobNotFound() {
        Mockito.when(jdbcTemplate.queryForObject(Mockito.anyString(), Mockito.anyMap(), Mockito.eq(byte[].class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        assertThrows(IOException.class, () -> storageService.open("non-existent-key"));
    }

    @Test
    void deletesBlobSuccessfully() throws IOException {
        storageService.delete("1/to-delete.pdf");

        Mockito.verify(jdbcTemplate).update(
                Mockito.contains("DELETE FROM storage_blobs"),
                Mockito.argThat((java.util.Map<String, ?> map) -> "1/to-delete.pdf".equals(map.get("key"))));
    }
}
