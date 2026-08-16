# KnowledgeOS local performance report

The current checkpoint prioritizes correctness and explainability over premature infrastructure optimization.

- Backend package: PASS.
- Frontend production build: PASS.
- Live RAG benchmark: 5 cases completed in 52.24 seconds including Spring context startup, Neon connection, ingestion, embeddings, retrieval, and generation. This is an end-to-end test duration, not per-request latency.
- pgvector query path: HNSW-backed `vector(768)` storage is used by the semantic retrieval repository.
- Bundle warning: the main frontend JavaScript chunk is approximately 665 kB minified. This is a non-blocking optimization item for a later production performance pass; no functional regression was found.
- No latency average/P95 was captured in the live benchmark, so those values are intentionally not invented.
