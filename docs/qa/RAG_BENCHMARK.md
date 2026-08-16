# KnowledgeOS RAG benchmark

The benchmark is executable in `backend/src/test/java/com/groupsync/backend/knowledge/rag/RagBenchmarkIntegrationTest.java` and is enabled only with `KNOWLEDGEOS_RAG_EVAL=true`. It uses the real resource parser, `gemini-embedding-001`, 768-dimensional normalized vectors in pgvector, retrieval scopes, `GroundedPromptBuilder`, Gemini generation, and persisted citation mapping. Temporary Neon records and local files are cleaned after the test.

## Measured run

Run date: 2026-08-16. Environment: Neon development PostgreSQL 17.10 with pgvector 0.8.0, Gemini provider configured through ignored `.env.local`.

| Metric | Measured value |
|---|---:|
| Live checks | 7 (5 answer cases + unsupported-question + scope-isolation checks) |
| Passed | 7 |
| Recall@5 | 1.000 |
| MRR | 1.000 |
| Citation validity | 100% |
| Grounded answer rate | 100% |
| Scope leakage cases | 0 |
| Unsupported hallucinations | 0 |
| Vietnamese case | PASS |
| Prompt-injection case | PASS |
| Average/P95 latency | Not captured by this run |

The version-controlled dataset contains 25 cases in `qa/fixtures/rag-cases.json`, including direct facts, paraphrase, cross-document, Vietnamese, unsupported, distractor, scope isolation, conflicting evidence, and prompt-injection categories. The live run uses five answer-generating cases for provider-cost control and two direct `KnowledgeChatService` safety checks; it is not presented as a 25-case execution. Recall/MRR/citation/grounding are calculated over the five answer cases.
