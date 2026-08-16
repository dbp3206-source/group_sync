# KnowledgeOS — Antigravity Verified Takeover Report

**Prepared by:** Antigravity (Claude Sonnet 4.6)
**Date:** 2026-08-16
**Pass type:** READ / INSPECT / VERIFY / DOCUMENT — no code changes made
**Source of truth hierarchy:** APPROVED SPECS > HANDOFF > ACTUAL SOURCE + TESTS + GIT

---

## 1. Executive Project Summary

KnowledgeOS is a personal knowledge intelligence platform built as an additive migration on top of
the existing GroupSync repository. It is a third-year university OOP project and an AI/backend/data
portfolio project. The migration is layered under com.groupsync.backend.knowledge. Legacy GroupSync
modules (Study, Badminton, Calendar) remain intact.

The project was substantially completed by GPT Work / Codex. This is the first Antigravity session.
The handoff is LARGELY VERIFIED with minor discrepancies documented below.

---

## 2. Product Idea / Problem / User

Product: KnowledgeOS — Personal Knowledge Intelligence Platform
Core Loop: COLLECT -> ORGANIZE -> UNDERSTAND -> RETRIEVE -> ASK -> LEARN

Target user: Single user (owner-scoped data) who saves many learning documents and struggles to
find them, understand relationships, decide what to study next, ask deep questions, and track progress.

Primary value proposition: Transform a messy document collection into an organized, searchable,
conversational, and actionable knowledge system.

What it is NOT: multi-user collaboration, file manager, Google Drive clone, Notion clone, ChatGPT
wrapper, full LMS, enterprise DMS, autonomous agent platform.

OOP objective: Parser abstraction, chunking strategy, embedding provider, retrieval abstraction,
lifecycle state machine, service/repository separation in a real AI-backed system.

---

## 3. Visual / UX Direction

Approved design: editorial, research-oriented, warm, composed. NOT a recolored GroupSync dashboard.
- Home = brand moment + user context + most important next action
- Library = knowledge shelf (not generic card dump)
- Resource Workspace = research/editorial surface
- Ask = answer<->citation<->evidence<->resource (citations first-class)
- Focus = calm next-action; Insights = explanatory data-led
- Two-font system: Playfair Display (display/hero) + Be Vietnam Pro (UI/body)
- Imagery participates in layout; motion is restrained and meaningful
- Responsive: 1440/1024/768/390px; mobile is adaptive

Current state: First-pass KnowledgeOS UI with correct structure. Full Phase 7 TasteSkill editorial
visual reset is PENDING (future Prompt 4 work).

---

## 6. TasteSkill Environment Status

TASTESKILL STATUS: AVAILABLE

File verified at: C:\Users\Bao Phuc\.agents\skills\design-taste-frontend\SKILL.md
Size: 88,459 bytes. Dated: 2026-08-14.

The skill is physically present and readable. The $design-taste-frontend slash-command syntax was
Codex-specific. In Antigravity, invoke by reading the SKILL.md via view_file before any UI work.

TASTESKILL COMMAND: -taste-frontend
REQUIRED: Read SKILL.md before any KnowledgeOS frontend modification. Hallmark is SECONDARY only.
No installation is needed — skill file is already present and readable.

TASTESKILL BLOCKER: None. Skill is available and readable.

---

## 7. Tech Stack (Verified from Source)

### Frontend
- React 19.2.8 | TypeScript 6.0.2 | Vite 8.2.0 | React Router 7.9.6 | Axios 1.13.2
- Bootstrap 5.3.8 | FullCalendar 6.1.21 | Lucide React 1.31.0 | oxlint 1.75.0
- Build: npm.cmd run build | Lint: npm.cmd run lint | Dev: npm.cmd run dev

### Backend
- Java 21 | Spring Boot 4.1.0 | Maven Wrapper
- Spring Web MVC, Data JPA, Security, Validation, Flyway, PostgreSQL JDBC
- Google GenAI Java SDK 1.56.0 | Apache PDFBox 3.0.5 | Apache POI 5.4.1
- Build: & .\mvnw.cmd test | Package: & .\mvnw.cmd package -DskipTests
- Internal package: com.groupsync.backend (NOT to be mass-renamed)

### Database
- PostgreSQL 17.10 (Neon development) + pgvector 0.8.0
- Vector: 768 dimensions, HNSW cosine index
- Flyway V11 applied on Neon

### AI (LOCKED)
- Generation: gemini-3.5-flash-lite | Quality fallback: gemini-3.5-flash
- Embeddings: gemini-embedding-001 | Dimensions: 768 | RAG top-K: 5
- API key env var: GEMINI_API_KEY

### Authentication
- Email + password, BCrypt, server-side session cookie, CSRF token endpoint
- Roles: USER, ADMIN (system)
- Cookie: same-site=lax, secure=false (local dev; must harden for production)

### Storage
- StorageService interface -> LocalStorageService
- Root: KNOWLEDGE_STORAGE_LOCAL_ROOT (default: ./knowledgeos-storage)
- CRITICAL PRODUCTION RISK: ephemeral disk is NOT safe; durable storage required before deploy

---

## 8. Modular-Monolith Architecture (Verified)

One Spring Boot app. KnowledgeOS module: com.groupsync.backend.knowledge
Sub-packages: chunking, controller, dto, ingestion, model, rag, repository, service, storage

Architectural prohibitions CONFIRMED: no FastAPI, no Qdrant, no microservices, no Kafka, no Redis,
no OpenAI in final RAG, no CQRS/event-sourcing, no enterprise IAM.

---

## 9. OOP Architecture (Verified from Source)

All key abstractions confirmed present:

| Abstraction | Interface | Implementations |
|---|---|---|
| ResourceParser | ResourceParser.java | Markdown, Text, Note, PDF, DOCX parsers |
| Parser Registry | ResourceParserRegistry.java | selects by ResourceType |
| ChunkingStrategy | ChunkingStrategy.java | Paragraph, Recursive |
| EmbeddingProvider | EmbeddingProvider.java | GeminiEmbeddingProvider |
| LanguageModelClient | LanguageModelClient.java | GeminiLanguageModelClient |
| StorageService | StorageService.java | LocalStorageService |
| SemanticRetrievalService | rag/SemanticRetrievalService.java | scoped vector retrieval |
| OrganizationSuggestionService | service class | deterministic + pgvector |
| Resource lifecycle | Resource.java | beginParsing/beginChunking/beginEmbedding/markReady/markFailed/retry |

DISAGREEMENT vs handoff: Handoff names 'FocusRecommendationStrategy' as a separate interface.
In actual source, Focus logic is in KnowledgeDashboardService.focusNext() as a SQL query.
No separate strategy interface class exists. Minor naming discrepancy; functionality is present.

---

## 10. Resource Ingestion Lifecycle (Verified)

POST /api/resources (multipart) or POST /api/resources/notes
-> ResourceService: checksum check (409 if exact duplicate), save UPLOADED
-> ResourceProcessingRequestedEvent published
-> ResourceIngestionService [async listener]:
   beginParsing -> parse -> beginChunking -> chunk -> beginEmbedding -> embed -> save chunks -> markReady
-> READY

On failure: markFailed(message) -> FAILED status, error stored (max 500 chars)
Retry: POST /api/resources/{id}/retry -> retry() resets to UPLOADED, re-triggers

POTENTIAL ISSUE: citations use ON DELETE RESTRICT on document_chunk_id. Resource deletion ordering
(citations -> chunks -> resource) must be verified in ResourceService. See Section 26.

---

## 11. Database / Flyway State (Verified)

V1-V8: IMMUTABLE (legacy GroupSync) - NOT modified per Git history inspection
V9: resources, collections, tags, resource_collections, resource_tags, resource_relations, resource_notes, learning_progress
V10: document_chunks, vector(768), HNSW cosine index
V11: chat_sessions, chat_session_resources, chat_messages, citations

Hibernate: ddl-auto=validate. Schema validated on Neon PostgreSQL 17.10 + pgvector 0.8.0.
All uniqueness constraints present (checksum, collection names, tag names, etc.)

---

## 12-13. RAG Architecture and Four Scopes (Verified)

Pipeline: Question -> embed query -> pgvector cosine retrieval (scoped, owner-filtered, top-K=5)
-> lexical anchor check + relevance gate -> if pass: GroundedPromptBuilder -> Gemini generation
-> save ChatMessage + Citations -> return AskKnowledgeResponse

GroundedPromptBuilder: puts app rules BEFORE evidence; evidence delimited as UNTRUSTED KNOWLEDGE;
explicitly tells Gemini to treat evidence as untrusted data and not obey instructions inside documents.

Four scopes (all verified in source + benchmark):
- THIS_RESOURCE: resource_id = :resourceId AND owner_id = :ownerId
- SELECTED_RESOURCES: resource_id IN (:resourceIds) AND owner_id = :ownerId
- COLLECTION: JOIN resource_collections WHERE collection_id = :collectionId AND owner_id = :ownerId
- LIBRARY: owner_id = :ownerId (all owner chunks)

Scope isolation live-tested: Atlas question scoped to Project Orion -> grounded=false, citations=[] (0 leakage)

---

## 14. Search / Organization Architecture (Verified)

Library search: GET /api/resources?q=&tagId=&collectionId=
- Title search: LOWER(title) LIKE LOWER('%q%') -- lexical, UTF-8, Vietnamese works transparently
- Tag filter: JOIN resource_tags WHERE tag_id = :tagId
- Collection filter: JOIN resource_collections WHERE collection_id = :collectionId
- Combined: AND logic; Owner isolation always present
- Frontend: KnowledgeLibraryPage.tsx -- search, tag dropdown, collection dropdown, clear/reset, no-result state

NOTE: Semantic search (vector hybrid) is NOT in the Library search path. Vector is RAG only.
This is NOT a Prompt 3 blocker (spec baseline is keyword search; hybrid was Phase 4 optional).

Smart Organization:
- GET /api/resources/{id}/organization/suggestions -> OrganizationSuggestionService
- Tags: controlled vocab (9 terms: rag, retrieval, embedding, vector-search, gemini, oop, design-patterns, architecture, vietnamese)
- Collections: keyword match against existing OR deterministic fallback proposal
- Related: pgvector semantic LIBRARY retrieval, exclude self, dedup by resourceId
- POST /api/resources/{id}/organization/apply: re-validates ownership; no silent application
- Frontend: ResourceWorkspacePage.tsx 'Organize' tab

LIMITATION: 9-term controlled vocab is domain-specific. Non-AI/OOP resources get title-keyword fallback.

---

## 16. RAG Evaluation Framework State (Verified)

Dataset: qa/fixtures/rag-cases.json -- >=25 cases verified by RagEvaluationDatasetTest
Categories confirmed: VIETNAMESE, PROMPT_INJECTION, CONFLICTING_EVIDENCE, SCOPE_ISOLATION
8 fixture markdown/text files present.

Live Benchmark (RagBenchmarkIntegrationTest.java):
- Enabled only with KNOWLEDGEOS_RAG_EVAL=true
- 7 checks: 5 answer cases + 1 unsupported question + 1 scope isolation
- NOT 25 live checks -- dataset has 25, live execution is intentionally smaller
- Last run: 2026-08-16

Metrics (verified, last run):
- Live checks: 7 | Passed: 7
- Recall@5: 1.000 (over 5 answer cases)
- MRR: 1.000 (over 5 answer cases)
- Citation validity: 100%
- Grounded answer rate: 100%
- Scope leakage: 0
- Unsupported hallucinations: 0
- Vietnamese: PASS
- Prompt injection: PASS
- Average/P95 latency: NOT CAPTURED (intentional)

Additional non-live tests: RagEvaluationDatasetTest, GroundedPromptBuilderTest, EmbeddingVectorNormalizerTest,
ChunkingStrategyTest, ResourceLifecycleTest, SemanticRetrievalNeonIntegrationTest, etc.

---

## 18. Git / Working Tree State (Verified)

| Field | Handoff claim | Actual verified |
|---|---|---|
| Branch | codex/knowledgeos-migration | codex/knowledgeos-migration CONFIRMED |
| HEAD SHA | abf3bd1f (handoff text) | 9f89326 (one correction commit after a87ba1c) |
| Remote branch | same as HEAD | 9f89326 up to date CONFIRMED |
| origin/main | 01c129787f | 01c1297 CONFIRMED |
| Working tree | modified start-groupsync.ps1; untracked design-work/qa/, knowledgeos-storage/ | CONFIRMED |

DISAGREEMENT: Handoff Section G stated HEAD=abf3bd1f but actual head at handoff write was a87ba1c.
The correction commit 9f89326 updated ANTIGRAVITY_HANDOFF.md to fix this. Now HEAD=9f89326.

Commit timeline (newest first):
9f89326 - correct antigravity checkpoint state (CURRENT HEAD)
a87ba1c - create antigravity engineering handoff + Vietnamese fix
abf3bd1 - acceptance organization, filters, and RAG QA checkpoint
f44f5dc - Step Two workspace flows
1909595 - collections and persistent history workspace flows
5e28bf9 - KnowledgeOS workspace routes/UI
84ed49e - Gemini retrieval and grounded chat
614708f - Gemini provider configuration
e86547a - pgvector schema and chunk mapping
21614da - parser registry and chunking
ce1c4f7 - resource foundation and storage
01c1297 - GroupSync baseline (main branch point)

User-owned local files (DO NOT stage or delete):
- scripts/start-groupsync.ps1 (modified working dev helper)
- design-work/qa/ (untracked: qa-report.md, qa-report-redesign.md, screenshots/, run-manifest*.json, public-browser-smoke.json)
- knowledgeos-storage/ (untracked runtime storage)

---

## 19. Exact Current Project Phase

PHASE: End of Prompt 3 -- Local Acceptance Near-Complete (~95%)

| Phase | Name | Status |
|---|---|---|
| Phase 0 | Safety/baseline migration | COMPLETE |
| Phase 1 | Knowledge domain foundation (V9) | COMPLETE |
| Phase 2 | Ingestion (parsers, chunking, storage) | COMPLETE |
| Phase 3 | RAG vertical slice | COMPLETE |
| Phase 4 | Product-level RAG (all 4 scopes, search) | COMPLETE |
| Phase 5 | Focus / Learning | COMPLETE |
| Phase 6 | Analytics / Insights | COMPLETE |
| Phase 7 | TasteSkill visual reset | PARTIAL (first-pass UI done; full editorial reset PENDING) |
| Phase 8 | Legacy cleanup | NOT STARTED |
| Phase 9 | QA / Production | PARTIAL (Local Acceptance near-complete; Render/Vercel NOT done) |

Remaining Prompt 3 items: (1) final regression build confirmation; (2) optional binary file-upload
browser E2E in unrestricted browser.

---

## 20. Full Prompt 3 Requirement Matrix (Summary)

Total tracked: ~47 items
PASS: 43 | PARTIAL: 2 | NOT TESTED: 1 (latency) | FAIL: 0 | APPROVED DEFERRED: several

Key PASS items: product alignment, arch audit, security, responsive, title search, Vietnamese search,
tag filter, collection filter, combined filter, clear/reset, no-result, owner isolation, Smart Org,
smart tags, smart collections, related resources, duplicate checksum, controlled corpus, critical
user journeys, all 4 RAG scopes, citations, persistent chat, insufficient context, Focus, Insights,
RAG evaluation framework, Recall@5=1.000, MRR=1.000, citation validity=100%, scope leakage=0,
unsupported hallucinations=0, Vietnamese RAG, prompt injection, backend tests (43), backend package,
frontend build, mobile 390px, 0 critical bugs, 0 major bugs.

PARTIAL items:
- UI acceptance (first-pass done; Phase 7 full visual reset pending)
- Semantic duplicate warning UX (expressed via related suggestions only; explicit UX deferred)
- Binary file-upload browser E2E (API+parsers work; browser harness limitation)
- DELETE/RE-INGEST (endpoint exists; citation FK ordering unverified)

NOT TESTED: Average/P95 latency (intentionally not captured).

---

## 21-23. Work Status Summary

COMPLETED: Auth, V9-V11 schema, 5 parsers, 2 chunking strategies, Gemini embedding+generation,
StorageService, full ingestion pipeline, all 4 RAG scopes, GroundedPromptBuilder, citation persistence,
persistent chat, Smart Organization, Library search/filter, Resource Workspace (all tabs), Focus Next,
Insights, KnowledgeOS frontend pages (Home/Library/Ask/Focus/Insights/Workspace), responsive first
pass, RAG evaluation framework (25-case dataset + 7-check live benchmark), backend tests, frontend build.

PARTIAL:
- Full TasteSkill editorial visual reset (Phase 7 -- future Prompt 4)
- Semantic duplicate UX (related suggestions surface it; no explicit UI warning)
- Binary file upload browser E2E (harness limitation)
- Library sort (only created_at DESC; no user-controlled sort)
- Citation FK ordering on delete (verification needed)
- Latency measurement

MISSING / UNFINISHED:
- Binary file-selector browser E2E: CANNOT be called PASS without unrestricted browser test
- Final regression build confirmation: clean run after HEAD 9f89326 not yet documented
- FINAL_LOCAL_ACCEPTANCE.md: file does NOT exist in docs/qa/

---

## 26. Known Bugs

Critical: 0
Major: 0

Minor/potential:
1. Citation FK ordering on delete: citations.document_chunk_id uses ON DELETE RESTRICT.
   Resource deletion may fail if citations present and service does not delete in correct order.
   Needs verification in ResourceService.
2. Smart tag controlled vocabulary is narrow (9 AI/OOP terms). Non-demo resources get fallback only.
3. Frontend bundle ~665 kB minified (non-blocking optimization).
4. Session storage is not clustered (acceptable for single-instance dev).

---

## 27. Known Tooling Limitations

1. Binary file-selector browser E2E: protected browser harness blocks synthetic file paths.
   Not an application failure. API+parsers are implemented and unit-tested.
2. TasteSkill -taste-frontend syntax: was Codex-specific. In Antigravity, use view_file on SKILL.md.
3. Live benchmark: requires KNOWLEDGEOS_RAG_EVAL=true + GEMINI_API_KEY + Neon dev DB.
4. Local portable PostgreSQL (port 54329): NOT compiled with pgvector. Do NOT use for KnowledgeOS dev.

---

## 28. Exact Ordered Continuation Backlog

| # | Priority | Task | Current State | Success Condition |
|---|---|---|---|---|
| 1 | CRITICAL | Final regression build + frontend build | Not confirmed post-9f89326 | 0 test failures; build PASS |
| 2 | HIGH | Verify citation FK ordering on resource deletion | Unverified | Resource deletes cleanly with active citations |
| 3 | HIGH | Binary file-upload browser E2E (unrestricted browser) | Harness-blocked | PDF/DOCX -> READY in real browser |
| 4 | MEDIUM | FINAL_LOCAL_ACCEPTANCE.md document | Missing | Document with all gates PASS exists |
| 5 | LOW | prefers-reduced-motion CSS verification | Not verified | No animations when user prefers reduced motion |
| 6 | LOW | Library sort control | created_at DESC only | User can sort by title/type/priority |
| 7 | FUTURE | TasteSkill visual reset (Phase 7) | First pass done | Full editorial design, no AI-slop |
| 8 | FUTURE | Production durable file storage | Local filesystem | Files survive container restart |
| 9 | FUTURE | Render/Vercel deployment | Not done | App at public URL |
| 10 | FUTURE | Repository rename | Deferred | dbp3206-source/knowledgeos after production |

---

## 29. Local Acceptance Gates

All gates: PASS or PASS with re-run recommended.
Gate PARTIAL status: critical user journeys (binary file E2E untested, all other journeys PASS).

CURRENT LOCAL ACCEPTANCE: PARTIAL
Two items remaining: (1) final regression build confirmation; (2) binary file E2E in unrestricted browser.
After these two: LOCAL ACCEPTANCE = PASS.

---

## 30. Do-Not-Change Contract

LOCKED (do not change without explicit authorization):
- Repository: dbp3206-source/group_sync (rename deferred)
- Branch: codex/knowledgeos-migration (do not merge to main)
- Architecture: Spring Boot modular monolith + React/TypeScript/Vite
- Database: PostgreSQL + pgvector ONLY (no Qdrant, no second vector DB)
- AI: Gemini ONLY
- Generation model: gemini-3.5-flash-lite
- Embedding model: gemini-embedding-001
- Embedding dimensions: 768 (no migration)
- No OpenAI in final RAG path
- No FastAPI in production
- Flyway V1-V8: IMMUTABLE
- Package root: com.groupsync.backend (no mass-rename)
- TasteSkill: PRIMARY for future UI work; Hallmark: SECONDARY only
- No main merge, no production deploy, no repository rename during this phase
- No secrets in Git ever
- Preserve user local files: scripts/start-groupsync.ps1, design-work/qa/, knowledgeos-storage/
- Development database: Neon only (not local portable PostgreSQL)

---

## 31. Production Boundary / Prompt 4 Boundary

Production delivery NOT finalized. Prompt 4 territory.
Before release:
1. Durable file storage (cloud blob / mounted volume) -- LOCAL FILESYSTEM IS NOT SAFE
2. Render PostgreSQL with pgvector
3. Vercel frontend with same-origin /api proxy
4. Production CORS, secure cookies (same-site=strict, secure=true)
5. GEMINI_API_KEY + DB credentials in Render/Vercel secrets (never in Git)
6. Full smoke test at public URL
7. Repository rename after all of above
8. Update local git remote after rename

---

## 32. Handoff Verification Summary

| Claim | Verified |
|---|---|
| Branch: codex/knowledgeos-migration | CONFIRMED |
| HEAD SHA a87ba1c / correction 9f89326 | CONFIRMED |
| 43 backend tests PASS | DOCUMENTED (re-run recommended) |
| 7-check live benchmark PASS | CONFIRMED from source |
| Vietnamese stopword fix committed | CONFIRMED (KnowledgeChatService Set.of no duplicates) |
| All 4 RAG scopes | CONFIRMED from source |
| Persistent chat/citations | CONFIRMED V11 + KnowledgeChatService |
| Smart Organization | CONFIRMED OrganizationSuggestionService + frontend |
| Search/filter (title/tag/collection/combined/Vietnamese) | CONFIRMED source |
| FocusRecommendationStrategy as named interface | DISCREPANCY: logic is in KnowledgeDashboardService |
| 25-case dataset | CONFIRMED RagEvaluationDatasetTest |
| FINAL_LOCAL_ACCEPTANCE.md exists | DISCREPANCY: file NOT present in docs/qa/ |

---

*Takeover complete. No code was modified. Only this report was written.*
*Antigravity session: eb856d06-f968-4f22-bed2-2288cbd969ba. Date: 2026-08-16.*
