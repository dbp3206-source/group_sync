package com.groupsync.backend.knowledge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.groupsync.backend.knowledge.dto.AskKnowledgeRequest;
import com.groupsync.backend.knowledge.dto.AskKnowledgeResponse;
import com.groupsync.backend.knowledge.dto.CitationResponse;
import com.groupsync.backend.knowledge.model.*;
import com.groupsync.backend.knowledge.rag.*;
import com.groupsync.backend.knowledge.rag.ParentChildContextExpander.ExpandedContext;
import com.groupsync.backend.knowledge.repository.*;
import com.groupsync.backend.knowledge.service.KnowledgeChatService;
import com.groupsync.backend.knowledge.service.KnowledgeChatTransactionService;
import com.groupsync.backend.knowledge.service.KnowledgeWorkspaceService;
import com.groupsync.backend.knowledge.service.StructuredKnowledgeQueryService;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class KnowledgeChatCitationTest {

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
                citationRepository, retrievalStrategy, queryPlanner,
                structuredQueryService, parentChildExpander, languageModelClient
        );
    }

    @Test
    void askReturnsOnlyActuallyCitedChunksAndPreservesMarkerNumbersAcrossPersistenceAndReload() {
        Long ownerId = 1L;
        Long sessionId = 88L;
        UserAccount owner = new UserAccount("student@example.com", "hash", "Student");
        ChatSession session = new ChatSession(owner, "Test Chat", RetrievalScope.LIBRARY, null, Set.of());
        ReflectionTestUtils.setField(session, "id", sessionId);

        AskKnowledgeRequest request = new AskKnowledgeRequest(null, "Explain BCNF decomposition", RetrievalScope.LIBRARY, null, null, null, null);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(sessionRepository.save(any(ChatSession.class))).thenReturn(session);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage msg = inv.getArgument(0);
            ReflectionTestUtils.setField(msg, "id", 999L);
            return msg;
        });

        when(queryPlanner.plan(anyLong(), anyString(), any(), any(), any(), any()))
                .thenReturn(new QueryPlan(QueryMode.HYBRID, QueryOperation.SEARCH, "Explain BCNF decomposition", KnowledgeQueryFilters.empty(), "test"));

        // Simulate 4 retrieved chunks
        List<RetrievedChunk> retrieved = List.of(
                new RetrievedChunk(101L, 1L, "Database Systems", 0, 1, "Section 1", "Chunk 1 content on 1NF", 0.1d),
                new RetrievedChunk(102L, 1L, "Database Systems", 1, 2, "Section 2", "Chunk 2 content on 2NF", 0.12d),
                new RetrievedChunk(103L, 1L, "Database Systems", 2, 3, "Section 3", "Chunk 3 content on 3NF & BCNF", 0.15d),
                new RetrievedChunk(104L, 1L, "Database Systems", 3, 4, "Section 4", "Chunk 4 content on SQL triggers", 0.18d)
        );
        when(retrievalStrategy.retrieve(anyLong(), anyString(), any(), any(), any(), any(), any())).thenReturn(retrieved);
        when(parentChildExpander.expand(retrieved)).thenReturn(new ExpandedContext(retrieved, retrieved));

        // LLM output specifically cites only [1] and [3]
        String llmAnswer = "According to [1], normalization begins with 1NF. Further, [3] defines BCNF as every determinant being a superkey.";
        when(languageModelClient.answer(anyString())).thenReturn(llmAnswer);

        Resource res = new Resource(owner, "Database Systems", null, ResourceType.MARKDOWN, "db.md", "text/markdown", 100L, "1/db.md", "hash");
        DocumentChunk chunk1 = new DocumentChunk(res, 0, 1, "Section 1", "Chunk 1 content on 1NF");
        DocumentChunk chunk3 = new DocumentChunk(res, 2, 3, "Section 3", "Chunk 3 content on 3NF & BCNF");
        ReflectionTestUtils.setField(chunk1, "id", 101L);
        ReflectionTestUtils.setField(chunk3, "id", 103L);

        when(chunkRepository.findAllById(anyList())).thenReturn(List.of(chunk1, chunk3));

        AskKnowledgeResponse response = chatService.ask(ownerId, request);

        assertTrue(response.grounded());
        // Immediate response MUST contain 2 citations: marker 1 and marker 3
        assertEquals(2, response.citations().size());
        assertEquals(1, response.citations().get(0).citationOrder());
        assertEquals(3, response.citations().get(1).citationOrder());

        // Verify persisted Citation entities retain original marker index (1 and 3)
        ArgumentCaptor<Citation> citationCaptor = ArgumentCaptor.forClass(Citation.class);
        verify(citationRepository, times(2)).save(citationCaptor.capture());
        List<Citation> savedCitations = citationCaptor.getAllValues();
        assertEquals(1, savedCitations.get(0).getCitationOrder());
        assertEquals(3, savedCitations.get(1).getCitationOrder());

        // Test Session Reload: citationOrder MUST reload as 1 and 3 (matching answer text)
        when(sessionRepository.findByIdAndOwnerId(sessionId, ownerId)).thenReturn(Optional.of(session));
        ChatMessage savedAssistantMsg = new ChatMessage(session, ChatMessageRole.ASSISTANT, llmAnswer);
        ReflectionTestUtils.setField(savedAssistantMsg, "id", 999L);
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)).thenReturn(List.of(savedAssistantMsg));
        when(citationRepository.findByMessageIdOrderByCitationOrderAsc(999L)).thenReturn(savedCitations);

        Map<String, Object> loadedSession = chatService.session(ownerId, sessionId);
        assertNotNull(loadedSession);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) loadedSession.get("messages");
        assertEquals(1, messages.size());
        @SuppressWarnings("unchecked")
        List<CitationResponse> reloadedCitations = (List<CitationResponse>) messages.get(0).get("citations");
        assertEquals(2, reloadedCitations.size());
        assertEquals(1, reloadedCitations.get(0).citationOrder(), "Reloaded citation 1 order must match [1]");
        assertEquals(3, reloadedCitations.get(1).citationOrder(), "Reloaded citation 2 order must match [3]");
    }

    @Test
    void askWithNoCitationMarkersNeverFabricatesFallbackCitation() {
        Long ownerId = 1L;
        UserAccount owner = new UserAccount("student@example.com", "hash", "Student");
        ChatSession session = new ChatSession(owner, "Test Chat", RetrievalScope.LIBRARY, null, Set.of());

        AskKnowledgeRequest request = new AskKnowledgeRequest(null, "Summarize knowledge base", RetrievalScope.LIBRARY, null, null, null, null);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(sessionRepository.save(any(ChatSession.class))).thenReturn(session);
        when(sessionRepository.findById(any())).thenReturn(Optional.of(session));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        when(queryPlanner.plan(anyLong(), anyString(), any(), any(), any(), any()))
                .thenReturn(new QueryPlan(QueryMode.HYBRID, QueryOperation.SEARCH, "Summarize knowledge base", KnowledgeQueryFilters.empty(), "test"));

        List<RetrievedChunk> retrieved = List.of(
                new RetrievedChunk(101L, 1L, "Guide", 0, 1, "Intro", "General intro content", 0.1d)
        );
        when(retrievalStrategy.retrieve(anyLong(), anyString(), any(), any(), any(), any(), any())).thenReturn(retrieved);
        when(parentChildExpander.expand(retrieved)).thenReturn(new ExpandedContext(retrieved, retrieved));

        // LLM output provides general answer with NO [X] markers
        String llmAnswer = "This knowledge base covers software architecture, databases, and testing best practices.";
        when(languageModelClient.answer(anyString())).thenReturn(llmAnswer);
        when(chunkRepository.findAllById(anyList())).thenReturn(List.of());

        AskKnowledgeResponse response = chatService.ask(ownerId, request);

        assertTrue(response.grounded());
        // Citations MUST be empty - NO fabricated citation [1]
        assertTrue(response.citations().isEmpty(), "No citation markers in answer must result in 0 citations, not fake [1]");
        verify(citationRepository, never()).save(any(Citation.class));
    }
}
