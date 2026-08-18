package com.groupsync.backend.knowledge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
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
import com.groupsync.backend.knowledge.model.*;
import com.groupsync.backend.knowledge.rag.LanguageModelClient;
import com.groupsync.backend.knowledge.rag.RetrievalScope;
import com.groupsync.backend.knowledge.rag.RetrievalStrategy;
import com.groupsync.backend.knowledge.rag.RetrievedChunk;
import com.groupsync.backend.knowledge.repository.*;
import com.groupsync.backend.knowledge.service.KnowledgeChatService;
import com.groupsync.backend.knowledge.service.KnowledgeChatTransactionService;
import com.groupsync.backend.knowledge.service.KnowledgeWorkspaceService;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class KnowledgeChatTransactionTest {

    @Mock private ChatSessionRepository sessionRepository;
    @Mock private ChatMessageRepository messageRepository;
    @Mock private CitationRepository citationRepository;
    @Mock private DocumentChunkRepository chunkRepository;
    @Mock private ResourceRepository resourceRepository;
    @Mock private UserAccountRepository userRepository;
    @Mock private RetrievalStrategy retrievalStrategy;
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
                citationRepository, retrievalStrategy, languageModelClient
        );
    }

    @Test
    void failedLlmCallDoesNotPersistMalformedAssistantMessage() {
        Long ownerId = 1L;
        Long sessionId = 42L;
        UserAccount owner = new UserAccount("user@test.com", "hash", "User");
        ChatSession session = new ChatSession(owner, "Failed Chat", RetrievalScope.LIBRARY, null, Set.of());
        ReflectionTestUtils.setField(session, "id", sessionId);

        AskKnowledgeRequest request = new AskKnowledgeRequest(null, "Why is my query failing?", RetrievalScope.LIBRARY, null, null, null, null);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(sessionRepository.save(any(ChatSession.class))).thenReturn(session);
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        List<RetrievedChunk> retrieved = List.of(
                new RetrievedChunk(1L, 10L, "Doc", 0, 1, "Sec 1", "Content 1", 0.05d)
        );
        when(retrievalStrategy.retrieve(anyLong(), anyString(), any(), any(), any(), any())).thenReturn(retrieved);

        // Simulate LLM throwing network exception or timeout
        when(languageModelClient.answer(anyString())).thenThrow(new IllegalStateException("Gemini API connection timeout"));

        // Expect the exception to propagate cleanly
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> chatService.ask(ownerId, request));
        assertEquals("Gemini API connection timeout", ex.getMessage());

        // Verify only ONE message (the user message) was saved, NO assistant message was saved
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageRepository, times(1)).save(messageCaptor.capture());
        assertEquals(ChatMessageRole.USER, messageCaptor.getValue().getRole());
        assertEquals("Why is my query failing?", messageCaptor.getValue().getContent());
    }

    @Test
    void insufficientContextPersistsClearAssistantMessageWithoutLlmCall() {
        Long ownerId = 1L;
        Long sessionId = 42L;
        UserAccount owner = new UserAccount("user@test.com", "hash", "User");
        ChatSession session = new ChatSession(owner, "No Context", RetrievalScope.LIBRARY, null, Set.of());
        ReflectionTestUtils.setField(session, "id", sessionId);

        AskKnowledgeRequest request = new AskKnowledgeRequest(null, "Random question with no matches", RetrievalScope.LIBRARY, null, null, null, null);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(sessionRepository.save(any(ChatSession.class))).thenReturn(session);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        // Retrieval returns empty list
        when(retrievalStrategy.retrieve(anyLong(), anyString(), any(), any(), any(), any())).thenReturn(List.of());

        AskKnowledgeResponse response = chatService.ask(ownerId, request);

        assertFalse(response.grounded());
        assertTrue(response.answer().contains("I don't have enough evidence"));
        assertEquals(0, response.citations().size());

        // Verify LLM was never called
        verify(languageModelClient, never()).answer(anyString());

        // Verify two messages were saved: 1 user, 1 assistant with insufficient context
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageRepository, times(2)).save(messageCaptor.capture());
        List<ChatMessage> saved = messageCaptor.getAllValues();
        assertEquals(ChatMessageRole.USER, saved.get(0).getRole());
        assertEquals(ChatMessageRole.ASSISTANT, saved.get(1).getRole());
        assertTrue(saved.get(1).getContent().contains("I don't have enough evidence"));
    }
}
