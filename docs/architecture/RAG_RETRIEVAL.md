# KnowledgeOS — RAG Retrieval Architecture

## 1. Overview

KnowledgeOS uses a **Hybrid Retrieval** architecture combining semantic dense vector search and PostgreSQL full-text search (FTS), fused via **Reciprocal Rank Fusion (RRF)**.

```
                             User Question
                                   │
                 ┌─────────────────┴─────────────────┐
                 │                                   │
          Semantic Branch                     Lexical Branch
      (SemanticRetrievalStrategy)        (KeywordRetrievalStrategy)
       • Gemini Embedding (768-dim)       • PostgreSQL FTS (simple dict)
       • pgvector HNSW cosine <=>         • plainto_tsquery('simple', ...)
       • Top K×2 candidate pool           • Top K×2 candidate pool
       • Strict scope & owner isolation   • Strict scope & owner isolation
                 │                                   │
                 └─────────────────┬─────────────────┘
                                   │
                        Reciprocal Rank Fusion
                       (HybridRetrievalStrategy)
                       • score = Σ 1/(60 + rank_i)
                       • Chunk deduplication by ID
                       • Fused Top-K ranking
                                   │
                        KnowledgeChatService
                       • GroundedPromptBuilder
                       • Gemini 3.5 Flash Lite
                       • Grounded citations persisted
```

---

## 2. Component Responsibilities

1. **`RetrievalStrategy` Interface**: Standard contract for scoped document chunk retrieval.
2. **`SemanticRetrievalStrategy`**: Invokes `GeminiEmbeddingProvider` (`gemini-embedding-001`, 768 dimensions) and queries `document_chunks` using pgvector's cosine distance operator (`<=>`) and HNSW index.
3. **`KeywordRetrievalStrategy`**: Queries PostgreSQL's built-in `fts_content` generated `tsvector` column using the `simple` configuration and a GIN index.
4. **`HybridRetrievalStrategy`**: Dispatches in parallel to both branches, deduplicates chunks by ID, and computes the fused RRF score using $k = 60$.
5. **`GroundedPromptBuilder`**: Constructs an injection-resistant context containing numbered chunk citations and explicit grounding rules.
6. **`KnowledgeChatService`**: Orchestrates chat sessions, message persistence, grounded answer generation via `GeminiLanguageModelClient` (`gemini-3.5-flash-lite`), and citation storage.

---

## 3. Four Retrieval Scopes

All retrieval branches strictly mirror the following isolation boundaries:

- **`THIS_RESOURCE`**: Only chunks belonging to the single active resource are queried (`dc.resource_id = :resourceId`).
- **`SELECTED_RESOURCES`**: Only chunks belonging to the explicitly selected set of resources (`dc.resource_id IN (:resourceIds)`).
- **`COLLECTION`**: Only chunks belonging to resources assigned to the collection (`EXISTS (SELECT 1 FROM resource_collections ...)`).
- **`LIBRARY`**: All resources owned by the authenticated user (`r.owner_id = :ownerId`).
