package com.groupsync.backend.knowledge.rag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groupsync.backend.knowledge.dto.*;
import com.groupsync.backend.knowledge.model.*;
import com.groupsync.backend.knowledge.rag.HybridRetrievalStrategy.HybridExecutionDetails;
import com.groupsync.backend.knowledge.rag.ParentChildContextExpander.ExpandedContext;
import com.groupsync.backend.knowledge.repository.*;
import com.groupsync.backend.knowledge.service.KnowledgeChatService;
import com.groupsync.backend.knowledge.service.KnowledgeChatTransactionService;
import com.groupsync.backend.knowledge.service.KnowledgeWorkspaceService;
import com.groupsync.backend.knowledge.service.StructuredKnowledgeQueryService;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class RagExecutionTraceTest {

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

    @BeforeEach
    void setUp() {
        chatTransactionService = new KnowledgeChatTransactionService(
                sessionRepository, messageRepository, citationRepository,
                chunkRepository, resourceRepository, userRepository, workspaceService
        );
        chatService = new KnowledgeChatService(
                chatTransactionService, sessionRepository, messageRepository,
                citationRepository, retrievalStrategy, null, queryPlanner,
                structuredQueryService, parentChildExpander, languageModelClient,
                new com.groupsync.backend.knowledge.rag.GeminiProperties("", "gemini-3.5-flash-lite", "gemini-3.5-flash", "gemini-embedding-001", 768, 16, 5, 2, 12, 60, 30000)
        );
    }

    @Test
    void ask_structuredQueryProducesTruthfulStructuredTraceWithoutRetrievalStages() {
        Long ownerId = 1L;
        Long sessionId = 10L;
        UserAccount owner = new UserAccount("u@test.com", "hash", "User");
        ChatSession session = new ChatSession(owner, "Count Query", RetrievalScope.LIBRARY, null, Set.of());
        ReflectionTestUtils.setField(session, "id", sessionId);

        AskKnowledgeRequest request = new AskKnowledgeRequest(null, "How many PDF documents do I have?", RetrievalScope.LIBRARY, null, null, null, null);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(sessionRepository.save(any(ChatSession.class))).thenReturn(session);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        KnowledgeQueryFilters filters = new KnowledgeQueryFilters(null, null, null, ResourceType.PDF, null, null, null);
        QueryPlan plan = new QueryPlan(QueryMode.STRUCTURED, QueryOperation.COUNT, null, filters, "Counting PDF resources");
        when(queryPlanner.plan(anyLong(), anyString(), any(), any(), any(), any())).thenReturn(plan);

        when(structuredQueryService.execute(anyLong(), any(), any(), any(), any(), any()))
                .thenReturn(new StructuredKnowledgeQueryService.StructuredResult("You have 5 PDF resources in KnowledgeOS.", 5L, List.of()));

        AskKnowledgeResponse response = chatService.ask(ownerId, request);

        assertNotNull(response.trace(), "Structured query must produce an execution trace");
        assertEquals(QueryMode.STRUCTURED, response.trace().mode());
        assertEquals(QueryOperation.COUNT, response.trace().operation());
        assertEquals("PDF", response.trace().filter().resourceType());

        // Truthful constraint: retrieval, fusion, parentChild, contextBudget must be NULL for structured query
        assertNull(response.trace().retrieval(), "Structured query must not report retrieval stage");
        assertNull(response.trace().fusion(), "Structured query must not report fusion stage");
        assertNull(response.trace().parentChild(), "Structured query must not report parentChild stage");
        assertNull(response.trace().contextBudget(), "Structured query must not report contextBudget stage");
        assertNull(response.trace().generation(), "Structured query must not report LLM generation stage");
    }

    @Test
    void ask_hybridQueryProducesTruthfulRetrievalAndParentChildTrace() {
        Long ownerId = 1L;
        Long sessionId = 20L;
        UserAccount owner = new UserAccount("u@test.com", "hash", "User");
        ChatSession session = new ChatSession(owner, "Hybrid Query", RetrievalScope.LIBRARY, null, Set.of());
        ReflectionTestUtils.setField(session, "id", sessionId);

        AskKnowledgeRequest request = new AskKnowledgeRequest(null, "Explain BCNF decomposition", RetrievalScope.LIBRARY, null, null, null, null);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(sessionRepository.save(any(ChatSession.class))).thenReturn(session);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        QueryPlan plan = new QueryPlan(QueryMode.HYBRID, QueryOperation.SEARCH, "Explain BCNF decomposition", KnowledgeQueryFilters.empty(), "General search");
        when(queryPlanner.plan(anyLong(), anyString(), any(), any(), any(), any())).thenReturn(plan);

        RetrievedChunk child1 = new RetrievedChunk(101L, 1L, "DB", 0, 1, "Sec 1", "Content 1", 0.05d);
        RetrievedChunk child2 = new RetrievedChunk(102L, 1L, "DB", 1, 1, "Sec 1", "Content 2", 0.08d);
        HybridExecutionDetails hybridDetails = new HybridExecutionDetails(List.of(child1, child2), 8, 4, 12, 60);
        when(retrievalStrategy.retrieveWithTrace(anyLong(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(hybridDetails);

        RetrievedChunk parent = new RetrievedChunk(100L, 1L, "DB", 0, 1, "Sec 1", "Parent section content", 0.05d);
        ExpandedContext expanded = new ExpandedContext(List.of(parent), List.of(child1, child2), 1, 1, 120);
        when(parentChildExpander.expand(anyList())).thenReturn(expanded);
        when(parentChildExpander.getMaxContextChars()).thenReturn(6000);

        when(languageModelClient.answer(anyString())).thenReturn("According to [1], BCNF eliminates anomalies.");

        Resource res = new Resource(owner, "DB", null, ResourceType.MARKDOWN, "db.md", "text/markdown", 100L, "1/db.md", "hash");
        DocumentChunk chunk100 = new DocumentChunk(res, 0, 1, "Sec 1", "Parent section content");
        ReflectionTestUtils.setField(chunk100, "id", 100L);
        when(chunkRepository.findAllById(anyList())).thenReturn(List.of(chunk100));

        AskKnowledgeResponse response = chatService.ask(ownerId, request);

        assertNotNull(response.trace());
        assertEquals(QueryMode.HYBRID, response.trace().mode());
        assertEquals(8, response.trace().retrieval().semanticCandidates());
        assertEquals(4, response.trace().retrieval().lexicalCandidates());
        assertEquals(12, response.trace().fusion().inputCandidates());
        assertEquals(2, response.trace().fusion().selectedChildren());
        assertEquals(2, response.trace().parentChild().childChunksRetrieved());
        assertEquals(1, response.trace().parentChild().uniqueParentsFound());
        assertEquals(1, response.trace().parentChild().duplicateParentsDeduplicated());
        assertEquals(1, response.trace().contextBudget().parentsUsed());
        assertEquals(120, response.trace().contextBudget().charactersUsed());
        assertEquals("gemini-3.5-flash-lite", response.trace().generation().model());
        assertEquals(1, response.trace().generation().verifiedCitationsCount());
        assertTrue(response.trace().durationMs() >= 0);
    }
}
