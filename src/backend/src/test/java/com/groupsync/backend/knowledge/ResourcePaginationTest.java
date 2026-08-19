package com.groupsync.backend.knowledge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.unit.DataSize;
import com.groupsync.backend.knowledge.dto.PagedResponse;
import com.groupsync.backend.knowledge.dto.ResourceResponse;
import com.groupsync.backend.knowledge.ingestion.ResourceParserRegistry;
import com.groupsync.backend.knowledge.model.Resource;
import com.groupsync.backend.knowledge.model.ResourceType;
import com.groupsync.backend.knowledge.rag.GeminiProperties;
import com.groupsync.backend.knowledge.repository.CitationRepository;
import com.groupsync.backend.knowledge.repository.DocumentChunkRepository;
import com.groupsync.backend.knowledge.repository.ResourceRepository;
import com.groupsync.backend.knowledge.service.ResourceService;
import com.groupsync.backend.knowledge.storage.StorageService;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class ResourcePaginationTest {

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
        resourceService = new ResourceService(
                DataSize.ofMegabytes(25),
                resourceRepository, userRepository, storageService,
                events, citationRepository, chunkRepository, parserRegistry,
                new GeminiProperties("", "gemini-3.5-flash-lite", "gemini-3.5-flash", "gemini-embedding-001", 768, 16, 5, 2, 12, 60, 30000)
        );
    }

    @Test
    void listPaged_boundsNegativePageAndExcessiveSize() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<String> sortCaptor = ArgumentCaptor.forClass(String.class);
        when(resourceRepository.searchPaged(eq(1L), isNull(), isNull(), isNull(), sortCaptor.capture(), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of(), Pageable.unpaged(), 0));

        // Negative page (-5) and excessive size (500)
        resourceService.listPaged(1L, null, null, null, -5, 500, "updated_desc");

        Pageable captured = pageableCaptor.getValue();
        assertEquals(0, captured.getPageNumber(), "Negative page must be clamped to 0");
        assertEquals(100, captured.getPageSize(), "Size exceeding 100 must be clamped to max 100");
        assertEquals("updated_desc", sortCaptor.getValue());
    }

    @Test
    void listPaged_normalizesSortTokensAndFallsBackSafely() {
        ArgumentCaptor<String> sortCaptor = ArgumentCaptor.forClass(String.class);
        when(resourceRepository.searchPaged(anyLong(), any(), any(), any(), sortCaptor.capture(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.unpaged(), 0));

        resourceService.listPaged(1L, "rag", 2L, 3L, 0, 24, "created_desc");
        assertEquals("created_desc", sortCaptor.getValue());

        resourceService.listPaged(1L, "rag", 2L, 3L, 0, 24, "title_asc");
        assertEquals("title_asc", sortCaptor.getValue());

        resourceService.listPaged(1L, "rag", 2L, 3L, 0, 24, "title_desc");
        assertEquals("title_desc", sortCaptor.getValue());

        // Unknown sort token falls back to updated_desc
        resourceService.listPaged(1L, "rag", 2L, 3L, 0, 24, "DROP TABLE resources;--");
        assertEquals("updated_desc", sortCaptor.getValue(), "Unknown or malicious sort token must safely fall back to updated_desc");
    }

    @Test
    void listPaged_returnsCalculatedPaginationMetadata() {
        Resource r1 = new Resource(owner, "Doc 1", "Desc", ResourceType.MARKDOWN, "doc1.md", "text/markdown", 100L, "k1", "c1");
        Resource r2 = new Resource(owner, "Doc 2", "Desc", ResourceType.MARKDOWN, "doc2.md", "text/markdown", 200L, "k2", "c2");
        org.springframework.test.util.ReflectionTestUtils.setField(r1, "id", 101L);
        org.springframework.test.util.ReflectionTestUtils.setField(r2, "id", 102L);

        Page<Resource> pageResult = new PageImpl<>(List.of(r1, r2), org.springframework.data.domain.PageRequest.of(0, 2), 5L);
        when(resourceRepository.searchPaged(eq(1L), eq("ai"), isNull(), isNull(), eq("updated_desc"), any(Pageable.class)))
                .thenReturn(pageResult);

        PagedResponse<ResourceResponse> response = resourceService.listPaged(1L, "ai", null, null, 0, 2, "updated_desc");

        assertEquals(2, response.items().size());
        assertEquals(0, response.page());
        assertEquals(2, response.size());
        assertEquals(5L, response.totalItems());
        assertEquals(3, response.totalPages());
        assertTrue(response.hasNext());
        assertEquals("Doc 1", response.items().get(0).title());
    }
}
