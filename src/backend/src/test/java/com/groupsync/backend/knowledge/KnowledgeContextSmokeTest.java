package com.groupsync.backend.knowledge;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.groupsync.backend.user.repository.UserAccountRepository;
import com.groupsync.backend.knowledge.chunking.StructureAwareChunkingStrategy;
import com.groupsync.backend.knowledge.ingestion.ResourceParserRegistry;
import com.groupsync.backend.knowledge.rag.EmbeddingProvider;
import com.groupsync.backend.knowledge.rag.EmbeddingTextBuilder;
import com.groupsync.backend.knowledge.rag.GeminiProperties;
import com.groupsync.backend.knowledge.rag.HybridRetrievalStrategy;
import com.groupsync.backend.knowledge.rag.KeywordRetrievalStrategy;
import com.groupsync.backend.knowledge.rag.KnowledgeQueryPlanner;
import com.groupsync.backend.knowledge.rag.LanguageModelClient;
import com.groupsync.backend.knowledge.rag.ParentChildContextExpander;
import com.groupsync.backend.knowledge.rag.QueryPlanValidator;
import com.groupsync.backend.knowledge.rag.SemanticRetrievalRepository;
import com.groupsync.backend.knowledge.rag.SemanticRetrievalStrategy;
import com.groupsync.backend.knowledge.repository.ChatMessageRepository;
import com.groupsync.backend.knowledge.repository.ChatSessionRepository;
import com.groupsync.backend.knowledge.repository.CitationRepository;
import com.groupsync.backend.knowledge.repository.DocumentChunkRepository;
import com.groupsync.backend.knowledge.repository.ResourceRepository;
import com.groupsync.backend.knowledge.service.KnowledgeChatService;
import com.groupsync.backend.knowledge.service.KnowledgeChatTransactionService;
import com.groupsync.backend.knowledge.service.AutoOrganizationService;
import com.groupsync.backend.knowledge.service.ResourceIngestionService;
import com.groupsync.backend.knowledge.service.ResourceIngestionTransactionService;
import com.groupsync.backend.knowledge.service.ResourceService;
import com.groupsync.backend.knowledge.service.StructuredKnowledgeQueryService;
import com.groupsync.backend.knowledge.storage.StorageService;

/**
 * Spring ApplicationContext smoke test verifying that the core RAG v2 bean graph
 * is successfully wired and instantiated by the Spring container in an offline environment.
 * Proves constructor injection, @Autowired selection, and ConfigurationProperties binding.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        KnowledgeContextSmokeTest.TestConfig.class,
        ResourceService.class,
        ResourceIngestionService.class,
        KnowledgeChatService.class,
        KnowledgeQueryPlanner.class,
        QueryPlanValidator.class,
        HybridRetrievalStrategy.class,
        SemanticRetrievalStrategy.class,
        ParentChildContextExpander.class,
        StructuredKnowledgeQueryService.class,
        StructureAwareChunkingStrategy.class,
        com.groupsync.backend.knowledge.chunking.RecursiveChunkingStrategy.class,
        EmbeddingTextBuilder.class
})
@EnableConfigurationProperties(GeminiProperties.class)
@TestPropertySource(properties = {
        "gemini.chat-model=gemini-3.5-flash-lite",
        "gemini.quality-model=gemini-3.5-flash",
        "gemini.embedding-model=gemini-embedding-001",
        "gemini.embedding-dimensions=768",
        "gemini.embedding-batch-size=16",
        "gemini.rag-top-k=5",
        "gemini.timeout-millis=30000",
        "knowledge.upload.max-size=25MB"
})
class KnowledgeContextSmokeTest {

    @Configuration
    static class TestConfig {
        @Bean
        static org.springframework.beans.factory.config.CustomEditorConfigurer customEditorConfigurer() {
            org.springframework.beans.factory.config.CustomEditorConfigurer configurer = new org.springframework.beans.factory.config.CustomEditorConfigurer();
            configurer.setPropertyEditorRegistrars(new org.springframework.beans.PropertyEditorRegistrar[]{
                    registry -> registry.registerCustomEditor(
                            org.springframework.util.unit.DataSize.class,
                            new java.beans.PropertyEditorSupport() {
                                @Override
                                public void setAsText(String text) {
                                    setValue(org.springframework.util.unit.DataSize.parse(text));
                                }
                            }
                    )
            });
            return configurer;
        }

        @Bean ResourceRepository resourceRepository() { return mock(ResourceRepository.class); }
        @Bean UserAccountRepository userAccountRepository() { return mock(UserAccountRepository.class); }
        @Bean StorageService storageService() { return mock(StorageService.class); }
        @Bean CitationRepository citationRepository() { return mock(CitationRepository.class); }
        @Bean DocumentChunkRepository documentChunkRepository() { return mock(DocumentChunkRepository.class); }
        @Bean ResourceParserRegistry resourceParserRegistry() { return mock(ResourceParserRegistry.class); }
        @Bean EmbeddingProvider embeddingProvider() { return mock(EmbeddingProvider.class); }
        @Bean AutoOrganizationService autoOrganizationService() { return mock(AutoOrganizationService.class); }
        @Bean ResourceIngestionTransactionService resourceIngestionTransactionService() { return mock(ResourceIngestionTransactionService.class); }
        @Bean KnowledgeChatTransactionService knowledgeChatTransactionService() { return mock(KnowledgeChatTransactionService.class); }
        @Bean ChatSessionRepository chatSessionRepository() { return mock(ChatSessionRepository.class); }
        @Bean ChatMessageRepository chatMessageRepository() { return mock(ChatMessageRepository.class); }
        @Bean LanguageModelClient languageModelClient() { return mock(LanguageModelClient.class); }
        @Bean NamedParameterJdbcTemplate namedParameterJdbcTemplate() { return mock(NamedParameterJdbcTemplate.class); }
        @Bean SemanticRetrievalRepository semanticRetrievalRepository() { return mock(SemanticRetrievalRepository.class); }
        @Bean(name = "keywordRetrieval") KeywordRetrievalStrategy keywordRetrievalStrategy() { return mock(KeywordRetrievalStrategy.class); }
    }

    @Autowired private ApplicationContext applicationContext;
    @Autowired private GeminiProperties geminiProperties;
    @Autowired private ResourceService resourceService;
    @Autowired private ResourceIngestionService resourceIngestionService;
    @Autowired private KnowledgeChatService knowledgeChatService;
    @Autowired private KnowledgeQueryPlanner queryPlanner;
    @Autowired private HybridRetrievalStrategy hybridRetrievalStrategy;
    @Autowired private SemanticRetrievalStrategy semanticRetrievalStrategy;
    @Autowired private ParentChildContextExpander parentChildContextExpander;

    @Test
    void springContainerInstantiatesCoreRagV2BeanGraph() {
        assertNotNull(applicationContext, "Spring ApplicationContext must be created");
        assertNotNull(geminiProperties, "GeminiProperties must be instantiated and bound");
        assertNotNull(resourceService, "ResourceService bean must be instantiated via Spring constructor injection");
        assertNotNull(resourceIngestionService, "ResourceIngestionService bean must be instantiated via Spring constructor injection");
        assertNotNull(knowledgeChatService, "KnowledgeChatService bean must be instantiated via Spring constructor injection");
        assertNotNull(queryPlanner, "KnowledgeQueryPlanner bean must be instantiated");
        assertNotNull(hybridRetrievalStrategy, "HybridRetrievalStrategy bean must be instantiated");
        assertNotNull(semanticRetrievalStrategy, "SemanticRetrievalStrategy bean must be instantiated");
        assertNotNull(parentChildContextExpander, "ParentChildContextExpander bean must be instantiated");
    }

    @Test
    void geminiPropertiesConfigurationBindingIsValid() {
        assertEquals("gemini-3.5-flash-lite", geminiProperties.chatModel());
        assertEquals("gemini-embedding-001", geminiProperties.embeddingModel());
        assertEquals(768, geminiProperties.embeddingDimensions());
        assertEquals(16, geminiProperties.embeddingBatchSize());
        assertEquals(5, geminiProperties.ragTopK());
        assertEquals(30000, geminiProperties.timeoutMillis());
    }

    @Test
    void productionServicesReceiveConfiguredProperties() {
        assertEquals(25L * 1024 * 1024, resourceService.getMaxUploadBytes(), "ResourceService must bind maxUploadSize property");
        assertNotNull(geminiProperties, "GeminiProperties must be non-null");
        assertEquals(16, geminiProperties.embeddingBatchSize());
        assertEquals(768, geminiProperties.embeddingDimensions());
    }
}
