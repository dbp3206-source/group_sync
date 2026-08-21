# Embedding 2 feasibility benchmark

Production remains `gemini-embedding-001` at 768 dimensions. This benchmark does not update
application configuration, PostgreSQL vectors, or production indexes.

The live-only test builds two isolated in-memory indexes, one per model, from the controlled RAG
fixtures. It measures Recall@5, Recall@10, MRR, document embedding latency, average query latency,
and provider errors for direct fact, semantic paraphrase, Vietnamese, cross-document, exact
identifier, distractor, and scope-filter cases.

Run from `src/backend` only when a live Gemini call is intended:

```powershell
$env:KNOWLEDGEOS_EMBEDDING_BENCHMARK='true'
$env:GEMINI_API_KEY='<configured outside Git>'
.\mvnw.cmd -Dtest=EmbeddingModelComparisonBenchmarkTest test
```

Normal test and CI runs skip the live class. Deterministic isolation and scope checks still run.

Official Google references checked on 2026-08-21:

- https://ai.google.dev/gemini-api/docs/embeddings
- https://ai.google.dev/gemini-api/docs/models/gemini-embedding-2
- https://ai.google.dev/gemini-api/docs/deprecations
- https://ai.google.dev/gemini-api/docs/pricing

Key migration facts: Embedding 2 is stable, supports 128-3072 dimensions with 768 recommended,
does not accept the Embedding 1 `task_type` field, aggregates multiple parts unless inputs are
separated, and uses an embedding space incompatible with Embedding 001. A production migration
would therefore require a complete isolated re-embedding and retrieval evaluation.
