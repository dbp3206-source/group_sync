package com.groupsync.backend.knowledge.rag;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.groupsync.backend.knowledge.model.ChatMessage;
import com.groupsync.backend.knowledge.model.ChatMessageRole;
import com.groupsync.backend.knowledge.model.ChatSession;
import com.groupsync.backend.knowledge.rag.GroundedPromptBuilder.ConversationTurn;
import com.groupsync.backend.knowledge.service.KnowledgeChatService;

class MultiTurnPromptTest {

    @Test
    void promptBuilderIncludesRecentHistoryTurnsInFormattedPrompt() {
        List<RetrievedChunk> chunks = List.of(
                new RetrievedChunk(1L, 10L, "Parent-Child Retrieval", 0, 1, "Overview",
                        "Parent-child retrieval indexes small child chunks for search while retrieving full parent chunks for synthesis.", 0.05d)
        );

        List<ConversationTurn> history = List.of(
                new ConversationTurn("USER", "What is chunking?"),
                new ConversationTurn("ASSISTANT", "Chunking splits large documents into smaller semantically cohesive segments [1].")
        );

        String prompt = GroundedPromptBuilder.build("How does it differ from parent-child retrieval?", chunks, history);

        assertTrue(prompt.contains("--- RECENT CONVERSATION HISTORY ---"), "Prompt must contain history section");
        assertTrue(prompt.contains("USER: What is chunking?"), "Prompt must contain prior user query");
        assertTrue(prompt.contains("ASSISTANT: Chunking splits large documents"), "Prompt must contain prior assistant response");
        assertTrue(prompt.contains("Current Question: How does it differ from parent-child retrieval?"), "Prompt must contain current question");
    }

    @Test
    void followUpContextualizationEnrichesEnglishRelativePronounQuestions() {
        ChatSession mockSession = org.mockito.Mockito.mock(ChatSession.class);
        List<ChatMessage> history = List.of(
                new ChatMessage(mockSession, ChatMessageRole.USER, "What is Parent-Child Retrieval?"),
                new ChatMessage(mockSession, ChatMessageRole.ASSISTANT, "Parent-child retrieval separates indexed chunks from context chunks.")
        );

        String question = "How is it better than the previous method?";
        String enrichedQuery = KnowledgeChatService.contextualizeSearchQuery(question, history);

        assertEquals("What is Parent-Child Retrieval? How is it better than the previous method?", enrichedQuery);
    }

    @Test
    void followUpContextualizationEnrichesVietnameseRelativeQuestions() {
        ChatSession mockSession = org.mockito.Mockito.mock(ChatSession.class);

        // Test 1: "nó"
        List<ChatMessage> history1 = List.of(
                new ChatMessage(mockSession, ChatMessageRole.USER, "Chuẩn hóa BCNF trong cơ sở dữ liệu quan hệ"),
                new ChatMessage(mockSession, ChatMessageRole.ASSISTANT, "BCNF là dạng chuẩn hóa mạnh hơn 3NF.")
        );
        assertEquals("Chuẩn hóa BCNF trong cơ sở dữ liệu quan hệ Nó có ưu điểm gì?",
                KnowledgeChatService.contextualizeSearchQuery("Nó có ưu điểm gì?", history1));

        // Test 2: "cách trên"
        List<ChatMessage> history2 = List.of(
                new ChatMessage(mockSession, ChatMessageRole.USER, "Kỹ thuật phòng thủ Prompt Injection"),
                new ChatMessage(mockSession, ChatMessageRole.ASSISTANT, "Sử dụng XML delimiters để phân lập dữ liệu.")
        );
        assertEquals("Kỹ thuật phòng thủ Prompt Injection Làm sao để áp dụng cách trên hiệu quả?",
                KnowledgeChatService.contextualizeSearchQuery("Làm sao để áp dụng cách trên hiệu quả?", history2));

        // Test 3: "phần đó"
        List<ChatMessage> history3 = List.of(
                new ChatMessage(mockSession, ChatMessageRole.USER, "Kiến trúc hệ thống KnowledgeOS"),
                new ChatMessage(mockSession, ChatMessageRole.ASSISTANT, "KnowledgeOS gồm frontend React, backend Spring Boot và PostgreSQL.")
        );
        assertEquals("Kiến trúc hệ thống KnowledgeOS Phần đó có hỗ trợ vector search không?",
                KnowledgeChatService.contextualizeSearchQuery("Phần đó có hỗ trợ vector search không?", history3));
    }

    @Test
    void unrelatedQuestionContainingSubstringsOfItDoesNotInheritPreviousContext() {
        ChatSession mockSession = org.mockito.Mockito.mock(ChatSession.class);
        List<ChatMessage> history = List.of(
                new ChatMessage(mockSession, ChatMessageRole.USER, "What is PostgreSQL pgvector?"),
                new ChatMessage(mockSession, ChatMessageRole.ASSISTANT, "pgvector is a vector similarity extension.")
        );

        // "git", "commit", "audit", "trait", "initial" have 'it' as a substring, but are NOT pronouns
        String standaloneQuestion1 = "Explain git commit history and audit trails";
        assertEquals(standaloneQuestion1, KnowledgeChatService.contextualizeSearchQuery(standaloneQuestion1, history));

        String standaloneQuestion2 = "What is an initial algorithm for graph search?";
        assertEquals(standaloneQuestion2, KnowledgeChatService.contextualizeSearchQuery(standaloneQuestion2, history));

        assertFalse(KnowledgeChatService.isFollowUpQuestion(standaloneQuestion1));
        assertFalse(KnowledgeChatService.isFollowUpQuestion(standaloneQuestion2));
    }
}
