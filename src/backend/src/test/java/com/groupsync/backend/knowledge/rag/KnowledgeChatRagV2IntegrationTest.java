package com.groupsync.backend.knowledge.rag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
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
import com.groupsync.backend.knowledge.service.*;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

/**
 * Execution-path orchestration tests verifying KnowledgeOS RAG v2 Ask flow.
 * Proves that QueryPlanner, Validator, Structured Query Routing, Semantic pgvector retrieval,
 * Filtered Hybrid Retrieval, Parent-Child context expansion, Grounded Prompting, and RagExecutionTrace
 * are strictly wired into runtime without fake trace default values.
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
    @Mock private SemanticRetrievalStrategy semanticRetrievalStrategy;
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
                citationRepository, retrievalStrategy, semanticRetrievalStrategy,
                queryPlanner, structuredQueryService, parentChildExpander,
                languageModelClient, new GeminiProperties("", "gemini-3.5-flash-lite", "gemini-3.5-flash", "gemini-embedding-001", 768, 16, 5, 2, 12, 60, 30000)
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

        assertNotNull(response);
        assertEquals("You have 7 PDF resources in KnowledgeOS.", response.answer());
        assertTrue(response.citations().isEmpty());

        verify(structuredQueryService, times(1)).execute(anyLong(), any(), any(), any(), any(), any());
        verify(retrievalStrategy, never()).retrieve(anyLong(), anyString(), any(), any(), any(), any());
        verify(retrievalStrategy, never()).retrieveWithTrace(anyLong(), anyString(), any(), any(), any(), any(), any());
        verify(semanticRetrievalStrategy, never()).retrieve(anyLong(), anyString(), any(), any(), any(), any(), any(), anyInt());
        verify(parentChildExpander, never()).expand(anyList());
        verify(languageModelClient, never()).answer(anyString());

        assertNotNull(response.trace());
        assertEquals(QueryMode.STRUCTURED, response.trace().mode());
        assertEquals(QueryOperation.COUNT, response.trace().operation());
        assertNull(response.trace().retrieval(), "Structured query must not have retrieval trace");
        assertNull(response.trace().fusion(), "Structured query must not have fusion trace");
        assertNull(response.trace().parentChild(), "Structured query must not have parent-child trace");
        assertNull(response.trace().generation(), "Structured query must not have LLM generation trace");
    }

    @Test
    void ask_semanticPath_executesPurePgVectorWithoutFtsOrFusion() {
        AskKnowledgeRequest request = new AskKnowledgeRequest(
                null, "Find concept representations", RetrievalScope.LIBRARY, null, null, null, null
        );

        QueryPlan plan = new QueryPlan(QueryMode.SEMANTIC, QueryOperation.SEARCH, "concept representations",
                KnowledgeQueryFilters.empty(), "Pure semantic vector search");
        when(queryPlanner.plan(eq(ownerId), anyString(), eq(RetrievalScope.LIBRARY), isNull(), anyList(), isNull()))
                .thenReturn(plan);

        RetrievedChunk child = new RetrievedChunk(101L, 10L, "AI Concepts", 1, 1, "Sec 1", "Child vector content", 0.05d);
        when(semanticRetrievalStrategy.retrieve(eq(ownerId), eq("concept representations"), eq(RetrievalScope.LIBRARY), isNull(), anyList(), isNull(), any(), eq(6)))
                .thenReturn(List.of(child));

        RetrievedChunk parent = new RetrievedChunk(100L, 10L, "AI Concepts", 0, 1, "Sec 1", "Parent section context", 0.05d);
        ExpandedContext expanded = new ExpandedContext(List.of(parent), List.of(child), 1, 0, 120);
        when(parentChildExpander.expand(List.of(child))).thenReturn(expanded);
        when(parentChildExpander.getMaxContextChars()).thenReturn(6000);

        when(languageModelClient.answer(anyString())).thenReturn("Concept representations are defined in [1].");

        Resource resource = new Resource(owner, "AI Concepts", null, ResourceType.MARKDOWN, "ai.md", "text/markdown", 500L, "1/ai.md", "hash");
        DocumentChunk chunk100 = new DocumentChunk(resource, 0, 1, "Sec 1", "Parent section context");
        ReflectionTestUtils.setField(chunk100, "id", 100L);
        when(chunkRepository.findAllById(anyList())).thenReturn(List.of(chunk100));

        AskKnowledgeResponse response = chatService.ask(ownerId, request);

        assertNotNull(response);
        assertEquals(QueryMode.SEMANTIC, response.trace().mode());
        assertEquals(1, response.trace().retrieval().semanticCandidates());
        assertEquals(0, response.trace().retrieval().lexicalCandidates(), "Semantic path must have 0 lexical candidates");
        assertNull(response.trace().fusion(), "Semantic path must not execute RRF fusion");
        verify(retrievalStrategy, never()).retrieveWithTrace(anyLong(), anyString(), any(), any(), any(), any(), any());
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

        assertNotNull(response);
        assertTrue(response.grounded());
        assertEquals(1, response.citations().size());
        assertEquals(1, response.citations().getFirst().citationOrder());
        assertEquals(200L, response.citations().getFirst().chunkId());

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
    void ask_insufficientContext_stopsTraceWithoutFabricatedStages() {
        AskKnowledgeRequest request = new AskKnowledgeRequest(
                null, "Random unknown query", RetrievalScope.LIBRARY, null, null, null, null
        );

        QueryPlan plan = new QueryPlan(QueryMode.HYBRID, QueryOperation.SEARCH, "Random unknown query",
                KnowledgeQueryFilters.empty(), "Search");
        when(queryPlanner.plan(eq(ownerId), anyString(), eq(RetrievalScope.LIBRARY), isNull(), anyList(), isNull()))
                .thenReturn(plan);

        // Hybrid retrieval returns 0 candidates
        HybridExecutionDetails hybridDetails = new HybridExecutionDetails(List.of(), 0, 0, 0, 60);
        when(retrievalStrategy.retrieveWithTrace(eq(ownerId), anyString(), eq(RetrievalScope.LIBRARY), isNull(), anyList(), isNull(), any()))
                .thenReturn(hybridDetails);

        AskKnowledgeResponse response = chatService.ask(ownerId, request);

        assertNotNull(response);
        assertFalse(response.grounded());
        assertNotNull(response.trace().retrieval());
        assertNotNull(response.trace().fusion());
        assertNull(response.trace().parentChild(), "Insufficient context must not create fake parentChild trace");
        assertNull(response.trace().contextBudget(), "Insufficient context must not create fake contextBudget trace");
        assertNull(response.trace().generation(), "Insufficient context must not create fake generation trace");
        verify(parentChildExpander, never()).expand(anyList());
        verify(languageModelClient, never()).answer(anyString());
    }

    @Test
    void ask_impossibleFilter_returnsZeroCandidatesAndStopsTrace() {
        AskKnowledgeRequest request = new AskKnowledgeRequest(
                null, "Find markdown in foreign collection", RetrievalScope.LIBRARY, null, null, null, null
        );

        KnowledgeQueryFilters impossibleFilters = KnowledgeQueryFilters.impossibleFilter();
        QueryPlan plan = new QueryPlan(QueryMode.FILTERED_HYBRID, QueryOperation.SEARCH, "query",
                impossibleFilters, "Impossible filter");
        when(queryPlanner.plan(eq(ownerId), anyString(), eq(RetrievalScope.LIBRARY), isNull(), anyList(), isNull()))
                .thenReturn(plan);

        HybridExecutionDetails hybridDetails = new HybridExecutionDetails(List.of(), 0, 0, 0, 60);
        when(retrievalStrategy.retrieveWithTrace(eq(ownerId), anyString(), eq(RetrievalScope.LIBRARY), isNull(), anyList(), isNull(), eq(impossibleFilters)))
                .thenReturn(hybridDetails);

        AskKnowledgeResponse response = chatService.ask(ownerId, request);

        assertNotNull(response);
        assertFalse(response.grounded());
        assertNull(response.trace().parentChild());
        assertNull(response.trace().generation());
    }

    @Test
    void ask_emitsActualModeStagesAndKeepsPublicTraceFreeOfChainOfThought() {
        RetrievedChunk child = new RetrievedChunk(301L, 10L, "Trace Doc", 1, 1, "Trace", "Trace child", 0.05d);
        RetrievedChunk parent = new RetrievedChunk(300L, 10L, "Trace Doc", 0, 1, "Trace", "Trace parent", 0.05d);
        ExpandedContext expanded = new ExpandedContext(List.of(parent), List.of(child), 1, 0, 80);
        when(parentChildExpander.expand(anyList())).thenReturn(expanded);
        when(parentChildExpander.getMaxContextChars()).thenReturn(6000);
        when(languageModelClient.answer(anyString())).thenReturn("Evidence answer");
        when(semanticRetrievalStrategy.retrieve(anyLong(), anyString(), any(), any(), anyList(), any(), any(), anyInt()))
                .thenReturn(List.of(child));
        when(retrievalStrategy.retrieveWithTrace(anyLong(), anyString(), any(), any(), anyList(), any(), any()))
                .thenReturn(new HybridExecutionDetails(List.of(child), 3, 2, 5, 60));
        KnowledgeQueryFilters filtered = new KnowledgeQueryFilters(null, null, null, ResourceType.PDF, null, null, null);
        when(queryPlanner.plan(eq(ownerId), anyString(), eq(RetrievalScope.LIBRARY), isNull(), anyList(), isNull()))
                .thenAnswer(invocation -> switch ((String) invocation.getArgument(1)) {
                    case "structured trace" -> new QueryPlan(QueryMode.STRUCTURED, QueryOperation.COUNT, null, KnowledgeQueryFilters.empty(), "count");
                    case "semantic trace" -> new QueryPlan(QueryMode.SEMANTIC, QueryOperation.SEARCH, "semantic trace", KnowledgeQueryFilters.empty(), "semantic");
                    case "filtered trace" -> new QueryPlan(QueryMode.FILTERED_HYBRID, QueryOperation.SEARCH, "filtered trace", filtered, "filtered");
                    default -> new QueryPlan(QueryMode.HYBRID, QueryOperation.SEARCH, "hybrid trace", KnowledgeQueryFilters.empty(), "hybrid");
                });
        when(structuredQueryService.execute(anyLong(), any(), any(), any(), any(), any()))
                .thenReturn(new StructuredKnowledgeQueryService.StructuredResult("count", 1L, List.of()));
        Resource resource = new Resource(owner, "Trace Doc", null, ResourceType.PDF, "trace.pdf", "application/pdf", 100L, "10/trace.pdf", "hash");
        DocumentChunk persisted = new DocumentChunk(resource, 0, 1, "Trace", "Trace parent");
        ReflectionTestUtils.setField(persisted, "id", 300L);
        when(chunkRepository.findAllById(anyList())).thenReturn(List.of(persisted));

        List<AskTraceStage> structuredStages = stagesFor("structured trace");
        List<AskTraceStage> semanticStages = stagesFor("semantic trace");
        List<AskTraceStage> hybridStages = stagesFor("hybrid trace");
        List<AskTraceStage> filteredStages = stagesFor("filtered trace");

        assertTrue(structuredStages.contains(AskTraceStage.STRUCTURED_OPERATION_COMPLETE));
        assertFalse(structuredStages.contains(AskTraceStage.SEMANTIC_RETRIEVAL_COMPLETE));
        assertFalse(structuredStages.contains(AskTraceStage.LEXICAL_RETRIEVAL_COMPLETE));
        assertFalse(structuredStages.contains(AskTraceStage.RRF_COMPLETE));
        assertTrue(semanticStages.contains(AskTraceStage.SEMANTIC_RETRIEVAL_COMPLETE));
        assertFalse(semanticStages.contains(AskTraceStage.LEXICAL_RETRIEVAL_COMPLETE));
        assertFalse(semanticStages.contains(AskTraceStage.RRF_COMPLETE));
        assertTrue(hybridStages.containsAll(List.of(AskTraceStage.SEMANTIC_RETRIEVAL_COMPLETE, AskTraceStage.LEXICAL_RETRIEVAL_COMPLETE, AskTraceStage.RRF_COMPLETE)));
        assertTrue(filteredStages.indexOf(AskTraceStage.FILTERS_APPLIED) < filteredStages.indexOf(AskTraceStage.SEMANTIC_RETRIEVAL_COMPLETE));
        assertTrue(filteredStages.indexOf(AskTraceStage.CITATIONS_VERIFIED) > filteredStages.indexOf(AskTraceStage.GENERATION_COMPLETE));
        assertTrue(java.util.Arrays.stream(AskTraceTechnicalDetails.class.getDeclaredFields()).noneMatch(field -> field.getName().toLowerCase().contains("thought")));
    }

    private List<AskTraceStage> stagesFor(String question) {
        List<AskTraceStage> stages = new java.util.ArrayList<>();
        AskKnowledgeRequest request = new AskKnowledgeRequest(null, question, RetrievalScope.LIBRARY, null, null, null, null);
        try (var ignored = com.groupsync.backend.knowledge.service.AskTraceContext.open((stage, details) -> stages.add(stage))) {
            chatService.ask(ownerId, request);
        }
        return stages;
    }
}
