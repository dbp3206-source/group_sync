package com.groupsync.backend.knowledge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groupsync.backend.knowledge.dto.AskKnowledgeRequest;
import com.groupsync.backend.knowledge.dto.AskKnowledgeResponse;
import com.groupsync.backend.knowledge.model.*;
import com.groupsync.backend.knowledge.rag.*;
import com.groupsync.backend.knowledge.repository.*;
import com.groupsync.backend.knowledge.service.KnowledgeChatService;
import com.groupsync.backend.knowledge.service.KnowledgeWorkspaceService;
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
    @Mock private RetrievalStrategy retrievalStrategy;
    @Mock private LanguageModelClient languageModelClient;
    @Mock private KnowledgeWorkspaceService workspaceService;

    @InjectMocks private KnowledgeChatService chatService;

    @Test
    void askReturnsOnlyActuallyCitedChunksInsteadOfAllRetrievedChunks() {
        Long ownerId = 1L;
        UserAccount owner = new UserAccount("student@example.com", "hash", "Student");
        ChatSession session = new ChatSession(owner, "Test Chat", RetrievalScope.LIBRARY, null, Set.of());

        AskKnowledgeRequest request = new AskKnowledgeRequest(null, "Explain BCNF decomposition", RetrievalScope.LIBRARY, null, null, null, null);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(sessionRepository.save(any(ChatSession.class))).thenReturn(session);
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        // Simulate 4 retrieved chunks
        List<RetrievedChunk> retrieved = List.of(
                new RetrievedChunk(101L, 1L, "Database Systems", 0, 1, "Section 1", "Chunk 1 content on 1NF", 0.1d),
                new RetrievedChunk(102L, 1L, "Database Systems", 1, 2, "Section 2", "Chunk 2 content on 2NF", 0.12d),
                new RetrievedChunk(103L, 1L, "Database Systems", 2, 3, "Section 3", "Chunk 3 content on 3NF & BCNF", 0.15d),
                new RetrievedChunk(104L, 1L, "Database Systems", 3, 4, "Section 4", "Chunk 4 content on SQL triggers", 0.18d)
        );
        when(retrievalStrategy.retrieve(anyLong(), anyString(), any(), any(), any(), any())).thenReturn(retrieved);

        // LLM output specifically cites only [1] and [3]
        String llmAnswer = "According to [1], normalization begins with 1NF. Further, [3] defines BCNF as every determinant being a superkey.";
        when(languageModelClient.answer(anyString())).thenReturn(llmAnswer);

        Resource res = new Resource(owner, "Database Systems", null, ResourceType.MARKDOWN, "db.md", "text/markdown", 100L, "1/db.md", "hash");
        DocumentChunk chunk1 = new DocumentChunk(res, 0, 1, "Section 1", "Chunk 1 content on 1NF");
        DocumentChunk chunk3 = new DocumentChunk(res, 2, 3, "Section 3", "Chunk 3 content on 3NF & BCNF");
        org.springframework.test.util.ReflectionTestUtils.setField(chunk1, "id", 101L);
        org.springframework.test.util.ReflectionTestUtils.setField(chunk3, "id", 103L);

        when(chunkRepository.findAllById(anyList())).thenReturn(List.of(chunk1, chunk3));

        AskKnowledgeResponse response = chatService.ask(ownerId, request);

        assertTrue(response.grounded());
        // Citations returned MUST ONLY be 2 items: chunk 1 and chunk 3
        assertEquals(2, response.citations().size());
        assertEquals(1, response.citations().get(0).citationOrder());
        assertEquals(3, response.citations().get(1).citationOrder());
        verify(citationRepository, times(2)).save(any(Citation.class));
    }
}
