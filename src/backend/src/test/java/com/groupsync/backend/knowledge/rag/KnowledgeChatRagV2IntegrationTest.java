package com.groupsync.backend.knowledge.rag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groupsync.backend.knowledge.dto.*;
import com.groupsync.backend.knowledge.model.*;
import com.groupsync.backend.knowledge.rag.HybridRetrievalStrategy.HybridExecutionDetails;
import com.groupsync.backend.knowledge.rag.ParentChildContextExpander.ExpandedContext;
import com.groupsync.backend.knowledge.repository.*;
import com.groupsync.backend.knowledge.service.*;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

/**
 * Execution-path integration test verifying KnowledgeOS RAG v2 Ask orchestration.
 * Proves that QueryPlanner, Validator, Structured Query Routing, Filtered Hybrid Retrieval,
 * Parent-Child context expansion, Grounded Prompting, and RagExecutionTrace are strictly wired into runtime.
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeChatRagV2IntegrationTest {

    @Mock private ChatSessionRepository sessionRepository;
    @Mock private ChatMessageRepository messageRepository;
    @Mock private CitationRepository citationRepository;
    @Mock private DocumentChunkRepository chunkRepository;
    @Mock private ResourceRepository resourceRepository;
    @Mock private UserAccountRepository userRepository;
    @Mock private HybridRetrievalStrategy retrievalStrategy;
    @Mock private KnowledgeQueryPlanner queryPlanner;
    @Mock private StructuredKnowledgeQueryService structuredQueryService;
    @Mock private ParentChildContextExpander parentChildExpander;
    @Mock private LanguageModelClient languageModelClient;
    @Mock private KnowledgeWorkspaceService workspaceService;

    private KnowledgeChatTransactionService chatTransactionService;
    private KnowledgeChatService chatService;

    private final Long ownerId = 1L;
    private final Long sessionId = 100L;
    private UserAccount owner;
    private ChatSession session;

    @BeforeEach
    void setUp() {
        chatTransactionService = new KnowledgeChatTransactionService(
                sessionRepository, messageRepository, citationRepository,
                chunkRepository, resourceRepository, userRepository, workspaceService
        );
        chatService = new KnowledgeChatService(
                chatTransactionService, sessionRepository, messageRepository,
                citationRepository, retrievalStrategy, queryPlanner,
                structuredQueryService, parentChildExpander, languageModelClient
        );

        owner = new UserAccount("user@knowledgeos.io", "hash", "User");
        session = new ChatSession(owner, "RAG v2 Test Session", RetrievalScope.LIBRARY, null, Set.of());
        ReflectionTestUtils.setField(session, "id", sessionId);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(sessionRepository.save(any(ChatSession.class))).thenReturn(session);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage msg = inv.getArgument(0);
            ReflectionTestUtils.setField(msg, "id", 500L);
            return msg;
        });
    }

    @Test
    void ask_structuredPath_executesRelationalMetadataQueryWithoutRetrievalOrParentChild() {
        AskKnowledgeRequest request = new AskKnowledgeRequest(
                null, "How many PDF documents are in my library?", RetrievalScope.LIBRARY, null, null, null, null
        );

        KnowledgeQueryFilters filters = new KnowledgeQueryFilters(null, null, null, ResourceType.PDF, null, null, null);
        QueryPlan plan = new QueryPlan(QueryMode.STRUCTURED, QueryOperation.COUNT, null, filters, "Counting PDF resources");
        when(queryPlanner.plan(eq(ownerId), anyString(), eq(RetrievalScope.LIBRARY), isNull(), anyList(), isNull()))
                .thenReturn(plan);

        when(structuredQueryService.execute(eq(ownerId), eq(plan), eq(RetrievalScope.LIBRARY), isNull(), anyList(), isNull()))
                .thenReturn(new StructuredKnowledgeQueryService.StructuredResult("You have 7 PDF resources in KnowledgeOS.", 7L, List.of()));

        AskKnowledgeResponse response = chatService.ask(ownerId, request);

        // Verification of business logic
        assertNotNull(response);
        assertEquals("You have 7 PDF resources in KnowledgeOS.", response.answer());
        assertTrue(response.citations().isEmpty());

        // Verification that unstructured RAG components were NOT called
        verify(structuredQueryService, times(1)).execute(anyLong(), any(), any(), any(), any(), any());
        verify(retrievalStrategy, never()).retrieve(anyLong(), anyString(), any(), any(), any(), any());
        verify(retrievalStrategy, never()).retrieveWithTrace(anyLong(), anyString(), any(), any(), any(), any(), any());
        verify(parentChildExpander, never()).expand(anyList());
        verify(languageModelClient, never()).answer(anyString());

        // Verification of truthful execution trace
        assertNotNull(response.trace());
        assertEquals(QueryMode.STRUCTURED, response.trace().mode());
        assertEquals(QueryOperation.COUNT, response.trace().operation());
        assertNull(response.trace().retrieval(), "Structured query must not have retrieval trace");
        assertNull(response.trace().fusion(), "Structured query must not have fusion trace");
        assertNull(response.trace().parentChild(), "Structured query must not have parent-child trace");
        assertNull(response.trace().generation(), "Structured query must not have LLM generation trace");
    }

    @Test
    void ask_hybridPath_executesHybridRetrievalAndParentChildExpansion() {
        AskKnowledgeRequest request = new AskKnowledgeRequest(
                null, "Explain hierarchical parent-child retrieval chunking", RetrievalScope.LIBRARY, null, null, null, null
        );

        QueryPlan plan = new QueryPlan(QueryMode.HYBRID, QueryOperation.SEARCH, "Explain hierarchical parent-child retrieval chunking",
                KnowledgeQueryFilters.empty(), "General search");
        when(queryPlanner.plan(eq(ownerId), anyString(), eq(RetrievalScope.LIBRARY), isNull(), anyList(), isNull()))
                .thenReturn(plan);

        RetrievedChunk child1 = new RetrievedChunk(201L, 10L, "RAG Guide", 1, 1, "Sec 1", "Child 1 content", 0.05d);
        RetrievedChunk child2 = new RetrievedChunk(202L, 10L, "RAG Guide", 2, 1, "Sec 1", "Child 2 content", 0.08d);
        HybridExecutionDetails hybridDetails = new HybridExecutionDetails(List.of(child1, child2), 6, 4, 10, 60);
        when(retrievalStrategy.retrieveWithTrace(eq(ownerId), anyString(), eq(RetrievalScope.LIBRARY), isNull(), anyList(), isNull(), any()))
                .thenReturn(hybridDetails);

        RetrievedChunk parent = new RetrievedChunk(200L, 10L, "RAG Guide", 0, 1, "Sec 1", "Full Parent Section Context", 0.05d);
        ExpandedContext expanded = new ExpandedContext(List.of(parent), List.of(child1, child2), 1, 1, 200);
        when(parentChildExpander.expand(List.of(child1, child2))).thenReturn(expanded);
        when(parentChildExpander.getMaxContextChars()).thenReturn(6000);

        when(languageModelClient.answer(anyString())).thenReturn("According to [1], parent-child retrieval indexes child chunks for search and expands to parent chunks.");

        Resource resource = new Resource(owner, "RAG Guide", null, ResourceType.MARKDOWN, "rag.md", "text/markdown", 500L, "1/rag.md", "hash");
        DocumentChunk chunk200 = new DocumentChunk(resource, 0, 1, "Sec 1", "Full Parent Section Context");
        ReflectionTestUtils.setField(chunk200, "id", 200L);
        when(chunkRepository.findAllById(anyList())).thenReturn(List.of(chunk200));

        AskKnowledgeResponse response = chatService.ask(ownerId, request);

        // Verification of business logic
        assertNotNull(response);
        assertTrue(response.grounded());
        assertEquals(1, response.citations().size());
        assertEquals(1, response.citations().getFirst().citationOrder());
        assertEquals(200L, response.citations().getFirst().chunkId());

        // Verification of orchestration calls
        verify(queryPlanner, times(1)).plan(anyLong(), anyString(), any(), any(), any(), any());
        verify(retrievalStrategy, times(1)).retrieveWithTrace(anyLong(), anyString(), any(), any(), any(), any(), any());
        verify(parentChildExpander, times(1)).expand(anyList());
        verify(languageModelClient, times(1)).answer(anyString());
        verify(structuredQueryService, never()).execute(anyLong(), any(), any(), any(), any(), any());

        // Verification of truthful execution trace
        assertNotNull(response.trace());
        assertEquals(QueryMode.HYBRID, response.trace().mode());
        assertEquals(6, response.trace().retrieval().semanticCandidates());
        assertEquals(4, response.trace().retrieval().lexicalCandidates());
        assertEquals(10, response.trace().fusion().inputCandidates());
        assertEquals(2, response.trace().fusion().selectedChildren());
        assertEquals(2, response.trace().parentChild().childChunksRetrieved());
        assertEquals(1, response.trace().parentChild().uniqueParentsFound());
        assertEquals(1, response.trace().parentChild().duplicateParentsDeduplicated());
        assertEquals(200, response.trace().contextBudget().charactersUsed());
        assertEquals("gemini-3.5-flash-lite", response.trace().generation().model());
        assertEquals(1, response.trace().generation().verifiedCitationsCount());
    }

    @Test
    void ask_filteredHybridPath_appliesValidatedFiltersAndPassesToRetrieval() {
        AskKnowledgeRequest request = new AskKnowledgeRequest(
                null, "Find markdown notes about memory compression", RetrievalScope.LIBRARY, null, null, null, null
        );

        KnowledgeQueryFilters filters = new KnowledgeQueryFilters(null, null, Set.of(5L), ResourceType.MARKDOWN, true, null, null);
        QueryPlan plan = new QueryPlan(QueryMode.FILTERED_HYBRID, QueryOperation.SEARCH, "memory compression", filters, "Search with markdown and tag filters");
        when(queryPlanner.plan(eq(ownerId), anyString(), eq(RetrievalScope.LIBRARY), isNull(), anyList(), isNull()))
                .thenReturn(plan);

        RetrievedChunk child = new RetrievedChunk(301L, 20L, "Memory Note", 1, 1, "Memory", "Child content on compression", 0.04d);
        HybridExecutionDetails hybridDetails = new HybridExecutionDetails(List.of(child), 3, 2, 5, 60);
        when(retrievalStrategy.retrieveWithTrace(eq(ownerId), eq("memory compression"), eq(RetrievalScope.LIBRARY), isNull(), anyList(), isNull(), eq(filters)))
                .thenReturn(hybridDetails);

        RetrievedChunk parent = new RetrievedChunk(300L, 20L, "Memory Note", 0, 1, "Memory", "Parent Memory Note Full Context", 0.04d);
        ExpandedContext expanded = new ExpandedContext(List.of(parent), List.of(child), 1, 0, 150);
        when(parentChildExpander.expand(List.of(child))).thenReturn(expanded);
        when(parentChildExpander.getMaxContextChars()).thenReturn(6000);

        when(languageModelClient.answer(anyString())).thenReturn("Memory compression is detailed in [1].");

        Resource resource = new Resource(owner, "Memory Note", null, ResourceType.MARKDOWN, "mem.md", "text/markdown", 300L, "1/mem.md", "hash");
        DocumentChunk chunk300 = new DocumentChunk(resource, 0, 1, "Memory", "Parent Memory Note Full Context");
        ReflectionTestUtils.setField(chunk300, "id", 300L);
        when(chunkRepository.findAllById(anyList())).thenReturn(List.of(chunk300));

        AskKnowledgeResponse response = chatService.ask(ownerId, request);

        // Verification
        assertNotNull(response);
        assertTrue(response.grounded());
        assertEquals("MARKDOWN", response.trace().filter().resourceType());
        assertEquals(Boolean.TRUE, response.trace().filter().favorite());
        assertEquals(1, response.trace().filter().tagCount());
        assertEquals(QueryMode.FILTERED_HYBRID, response.trace().mode());
    }
}
