# Retrieval-Augmented Generation (RAG) Architecture

Retrieval-Augmented Generation enhances Large Language Model capabilities by grounding answers on private, domain-specific documents.

## Hybrid Retrieval Pipeline

1. **Ingestion & Parsing**: Documents (PDF, DOCX, Markdown, Text) are extracted into plain text.
2. **Chunking**: Text is partitioned into manageable segments (e.g. 500 characters with 100 character overlap) to preserve semantic coherence.
3. **Embedding**: Text chunks are converted into 768-dimensional dense vectors using `gemini-embedding-001`.
4. **Dual Retrieval**:
   - **Semantic Branch**: Vector cosine similarity search executed in PostgreSQL using `pgvector` and HNSW indexing.
   - **Lexical Branch**: Full-Text Search (FTS) executed in PostgreSQL using `to_tsvector('simple', ...)` and GIN indexing.
5. **Reciprocal Rank Fusion (RRF)**: Merges ranked results from both branches using formula: `Score = sum(1.0 / (60 + rank))`.
6. **Grounded Prompting**: Formats top evidence chunks with strict citations and anti-hallucination boundaries.
7. **Synthesis**: LLM generates the response with explicit source citations.
