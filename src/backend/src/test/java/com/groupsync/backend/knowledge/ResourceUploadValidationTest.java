package com.groupsync.backend.knowledge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import com.groupsync.backend.knowledge.ingestion.ResourceParserRegistry;
import com.groupsync.backend.knowledge.model.ResourceType;
import com.groupsync.backend.knowledge.repository.CitationRepository;
import com.groupsync.backend.knowledge.repository.DocumentChunkRepository;
import com.groupsync.backend.knowledge.repository.ResourceRepository;
import com.groupsync.backend.knowledge.service.ResourceService;
import com.groupsync.backend.knowledge.storage.StorageService;
import com.groupsync.backend.shared.exception.BadRequestException;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class ResourceUploadValidationTest {

    @Mock private ResourceRepository resourceRepository;
    @Mock private UserAccountRepository userRepository;
    @Mock private StorageService storageService;
    @Mock private ApplicationEventPublisher events;
    @Mock private CitationRepository citationRepository;
    @Mock private DocumentChunkRepository chunkRepository;
    @Mock private ResourceParserRegistry parserRegistry;

    private ResourceService resourceService;
    private final UserAccount owner = new UserAccount("test@example.com", "hash", "Tester");

    @BeforeEach
    void setUp() {
        // Configure with 10MB limit (10 * 1024 * 1024 = 10485760 bytes)
        long configuredLimit = 10L * 1024 * 1024;
        resourceService = new ResourceService(
                configuredLimit, resourceRepository, userRepository,
                storageService, events, citationRepository, chunkRepository, parserRegistry
        );
    }

    @Test
    void rejectsFileExceedingConfiguredMaxSize() {
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB exceeds 10MB
        MockMultipartFile file = new MockMultipartFile("file", "large.pdf", "application/pdf", largeContent);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                resourceService.upload(1L, file, "Large File", null)
        );
        assertTrue(ex.getMessage().contains("10 MB or smaller"));
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                resourceService.upload(1L, file, "Empty File", null)
        );
        assertTrue(ex.getMessage().contains("Choose a resource"));
    }

    @Test
    void rejectsMimeTypeMismatch_pdfWithImageMime() {
        MockMultipartFile file = new MockMultipartFile("file", "malicious.pdf", "image/png", "fake data".getBytes(StandardCharsets.UTF_8));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                resourceService.upload(1L, file, "Mismatch File", null)
        );
        assertTrue(ex.getMessage().contains("định dạng MIME không khớp"));
    }

    @Test
    void rejectsMimeTypeMismatch_docxWithPdfMime() {
        MockMultipartFile file = new MockMultipartFile("file", "document.docx", "application/pdf", "fake data".getBytes(StandardCharsets.UTF_8));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                resourceService.upload(1L, file, "Mismatch DOCX", null)
        );
        assertTrue(ex.getMessage().contains("định dạng MIME không khớp"));
    }

    @Test
    void rejectsUnsupportedFileExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "script.py", "text/x-python", "print('hello')".getBytes(StandardCharsets.UTF_8));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                resourceService.upload(1L, file, "Python Script", null)
        );
        assertTrue(ex.getMessage().contains("Supported files are PDF, DOCX, TXT, and Markdown"));
    }

    @Test
    void acceptsValidPdfWithPdfMime() throws Exception {
        byte[] content = "%PDF-1.4 valid".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", content);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(storageService.store(eq(1L), eq("test.pdf"), any())).thenReturn(
                new StorageService.StoredFile("1/test.pdf", (long) content.length, "sha256-hash")
        );
        when(resourceRepository.findByOwnerIdAndChecksumSha256(1L, "sha256-hash")).thenReturn(Optional.empty());
        when(resourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = resourceService.upload(1L, file, "Test PDF", "Description");
        assertNotNull(response);
        assertEquals("Test PDF", response.title());
        assertEquals(ResourceType.PDF, response.resourceType());
    }
}
