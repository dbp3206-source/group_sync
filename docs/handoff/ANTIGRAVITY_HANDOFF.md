# KnowledgeOS — Antigravity Engineering Handoff

This document is the canonical transfer record for the current KnowledgeOS migration. It is
written for a new coding agent with no access to the preceding GPT Work conversation. Read it
before changing code. The repository is currently a safe migration checkpoint; do not restart
the architecture or discard the existing GroupSync baseline.

## A. Project identity

KnowledgeOS is a personal knowledge-intelligence platform for one user who saves learning
documents and needs to organize, understand, retrieve, question, and revisit them. Its loop is:

`COLLECT → ORGANIZE → UNDERSTAND → RETRIEVE → ASK → LEARN`

It is a focused knowledge library, resource workspace, grounded RAG assistant, and lightweight
learning/focus layer. It is not a generic file manager, Drive/Notion clone, ChatGPT wrapper,
enterprise DMS, LMS, autonomous-agent platform, or collaboration product.

The project is a third-year university OOP project, so code must stay readable, modular,
explainable, and testable. It should also be credible as an AI/backend/data portfolio project.
The old GroupSync study/badminton system remains as a working legacy baseline while KnowledgeOS
is migrated additively.

## B. Product features and actual status

### Implemented in the current source

- Auth: register, login, logout, current user, server-side session cookie, BCrypt, CSRF endpoint.
- Knowledge Home: Focus Next and recent knowledge context.
- Library: owned resources, note creation, multipart upload, title search, Vietnamese title
  search, tag filter, collection filter, combined AND filtering, reset/no-result states.
- Resource CRUD and protected content access; checksum-based exact duplicate rejection.
- Supported ingestion: note, text/markdown, PDF, and DOCX parser registrations.
- Processing lifecycle: `UPLOADED → PARSING → CHUNKING → EMBEDDING → READY`, with `FAILED` and retry.
- Collections and tags: owner-scoped CRUD, resource assignment, tag normalization, filtering.
- Resource relations: explicit related resources and semantic related suggestions.
- Notes: owner-validated create/list/update/delete APIs and real Workspace UI.
- Favorite, priority, progress, timestamps, processing status, and truthful activity data.
- Resource Workspace: Overview, Reader, Notes, Ask, Related, Activity/Progress, Organize.
- Ask/RAG scopes: `THIS_RESOURCE`, `SELECTED_RESOURCES`, `COLLECTION`, and `LIBRARY`.
- Grounded answers with persisted ChatSession, ChatMessage, Citation, evidence excerpts, and
  reloadable session history.
- Focus Next: deterministic explainable recommendation from existing resource data.
- Insights: truthful library/activity/progress/citation-oriented aggregate view.
- Profile and existing GroupSync pages remain available while migration is additive.
- Responsive first pass verified around 1440, 1024, 768, and 390px without horizontal overflow.
- Smart Organization: reviewed tag, collection, new-collection, and related-resource suggestions.
- Reusable RAG evaluation fixture/dataset and executable live benchmark.

### Partial or not yet production-complete

- Binary file-selector browser E2E was not completed because the protected browser harness
  rejected workspace-safe synthetic file paths. Multipart upload, parsers, storage, and note
  ingestion are implemented; a real browser file chooser should be retested in another QA
  environment.
- Semantic duplicate detection is not an automatic duplicate block. Exact checksum duplicates
  are blocked; semantic matches are exposed as related/organization suggestions and need a
  deliberate product decision if warning UX is expanded.
- The live benchmark executes seven checks (five answer cases plus unsupported-question and
  scope-isolation checks), while the versioned dataset has 25 cases. Full 25-case live provider
  execution is intentionally not run on every change.
- Average/P95 latency is not captured; the existing report records end-to-end benchmark time and
  the known frontend bundle warning.
- Production Render/Vercel delivery, repository rename, and production storage persistence are
  deliberately not finalized in this handoff.

### Approved deferred scope

Google Calendar sync, external reminder delivery, real-time/WebSocket infrastructure, complex
analytics/mastery models, advanced charting, external email invitations, and enterprise IAM are
out of scope for this stage.

## C. Product and design direction

The product identity is KnowledgeOS — “Personal Knowledge Intelligence Platform”. The visual
reset is editorial, research-oriented, warm, and intentionally composed. It must not be a
recolored GroupSync dashboard or a generic “sidebar + header + KPI cards” SaaS template.

- Home is the flagship: a strong brand moment paired with the user’s real next action, such as
  Focus Next, Continue Reading, a recent resource, or an Ask entry point.
- Library behaves like an editorial knowledge shelf: search and filters support discovery,
  with varied composition rather than repeated cards.
- Resource Workspace is a research surface. Reader, notes, evidence, related materials, and
  activity should feel like one connected document context.
- Ask must visually preserve `Answer ↔ Citation ↔ Evidence ↔ Resource`, making source evidence
  prominent rather than hiding it beneath a generic chatbot transcript.
- Focus is a calm next-action surface; Insights is explanatory and data-led, not a decorative
  KPI wall.
- Auth should feel part of the KnowledgeOS identity, not a leftover GroupSync screen.
- Typography direction is a deliberate two-font system: expressive display type with Vietnamese
  support (Playfair Display was the starting direction) and a sharp UI/body sans (Be Vietnam Pro
  was the starting direction). Verify actual font loading before replacing it.
- Imagery participates in composition: document previews, crop, overlap, texture, layered
  atmospheric backgrounds, and meaningful SVG/bitmap artwork. Do not add random AI blobs/glows.
- Motion is restrained and meaningful, with lighter motion and touch-friendly controls on mobile.
- Responsive checkpoints are approximately 1440 / 1024 / 768 / 390px. Mobile is adaptive, not a
  shrunken desktop: use full-width sections, horizontal resource strips, drawers/bottom sheets,
  and reduced motion where appropriate.

The accepted first-pass visual work is in the KnowledgeOS pages, hero asset, and
`knowledgeos-responsive.css`. Future visual work must use `$design-taste-frontend` as the
primary creative direction; Hallmark is secondary only for quality/accessibility review.

## D. Final tech stack

Frontend: React 19, TypeScript 6, Vite 8, React Router 7, Axios 1, Bootstrap 5 dependency,
FullCalendar 6, Lucide React, and `oxlint`. The production command is `npm.cmd run build`;
lint is `npm.cmd run lint`.

Backend: Java source/target 21, Spring Boot 4.1.0, Maven Wrapper, Spring Web MVC, Spring Data
JPA, Spring Security, validation, Flyway, PostgreSQL JDBC driver, and Google GenAI Java SDK
`com.google.genai:google-genai:1.56.0`. The package still uses the internal technical root
`com.groupsync.backend`; do not mass-rename it just because user-facing branding changed.

Database: PostgreSQL with pgvector. Development currently uses a separate Neon DEVELOPMENT
database, not the old GroupSync production database. The earlier portable Windows PostgreSQL
runtime is preserved and was not modified to compile pgvector. Production direction remains
Render PostgreSQL + pgvector.

AI configuration is locked:

- Generation: `gemini-3.5-flash-lite`
- Optional quality model: `gemini-3.5-flash`
- Embedding: `gemini-embedding-001`
- Embedding dimension: `768`
- API key environment variable: `GEMINI_API_KEY`

Document storage is `StorageService` with the current local implementation in
`backend/src/main/java/com/groupsync/backend/knowledge/storage/LocalStorageService.java`, rooted
by `KNOWLEDGE_STORAGE_LOCAL_ROOT` and defaulting to `./knowledgeos-storage`. Production needs a
persistent storage decision before deployment; ephemeral container disk is not a safe final
production upload store.

## E. Architecture

The application is a single Spring Boot modular monolith with familiar controller → service →
repository → model layering. Existing GroupSync modules remain in the same backend. KnowledgeOS
is grouped under `com.groupsync.backend.knowledge` and has controllers, DTOs, models, repositories,
ingestion, chunking, RAG, storage, and services.

Important OOP abstractions already present:

- `ResourceParser` with Markdown, text/note, PDF, and DOCX parser implementations, selected by
  `ResourceParserRegistry`.
- `ChunkingStrategy` with paragraph and recursive chunking implementations.
- `EmbeddingProvider` implemented by `GeminiEmbeddingProvider`.
- `LanguageModelClient` implemented by `GeminiLanguageModelClient`.
- `SemanticRetrievalRepository` and `SemanticRetrievalService` for scoped vector retrieval.
- `FocusRecommendationStrategy`/dashboard service for explainable next-resource selection.
- `StorageService` implemented by `LocalStorageService`.
- `OrganizationSuggestionService` for deterministic, reviewable organization proposals.
- Resource lifecycle methods (`beginParsing`, `beginChunking`, `beginEmbedding`, `markReady`,
  `markFailed`, `retry`) encapsulate state transitions in the entity.

Do not add inheritance merely to demonstrate OOP. Prefer composition, interfaces for real
variable algorithms, enums for lifecycle, and explicit service responsibilities.

Data flow:

`Resource → parser → extracted text/page/section → chunking → Gemini embedding → pgvector →
scoped retrieval → grounded context → Gemini generation → persisted answer/citations`.

Architectural prohibitions: no production FastAPI service, microservices, Kafka, Redis,
Kubernetes, second vector database, OpenAI RAG path, CQRS/event-sourcing ceremony, or heavy
enterprise IAM.

## F. Database and Flyway

V1-V8 are the original GroupSync foundation/application/calendar/study/badminton/profile/
tournament migrations. They are immutable and must never be edited or rewritten. KnowledgeOS
starts at V9:

- V9: resources, collections, tags, resource-collection/tag links, relations, notes, progress,
  ownership and checksum-related fields.
- V10: `document_chunks`, `vector(768)` embeddings, embedding metadata, and vector retrieval
  indexing (HNSW where supported).
- V11: chat sessions, chat messages, citations, scope/resource links.

Hibernate runs with `ddl-auto=validate`; migrations are the schema authority. Important safety
rules are owner-scoped queries, unique links/constraints, resource checksum duplicate protection,
chunk cleanup on resource deletion, and citation links back to persisted chunks/resources.
Current verified development schema is Flyway version 11 on Neon PostgreSQL 17.10 with pgvector
0.8.0. Never use the old production GroupSync database for development.

## G. Current Git state

Repository: `https://github.com/dbp3206-source/group_sync.git`

- Branch at inspection: `codex/knowledgeos-migration`
- HEAD at inspection: `abf3bd1f496cf5f0fcb9185104a0474ccf885c86`
- Remote branch at inspection: same SHA
- `origin/main` at inspection: `01c129787f5687f6979c049a29446cffa103bcf6`
- The migration branch is ahead of main and must not be merged yet.
- Intended later repository identity is `dbp3206-source/knowledgeos`; rename is a later release
  operation only, after safety and Vercel/Render integration checks.

Important checkpoints:

- `01c1297`: verified GroupSync baseline.
- `ce1c4f7`: KnowledgeOS resource foundation/storage.
- `21614da`: parser registry and chunking strategies.
- `e86547a`: pgvector schema and chunk mapping.
- `614708f`: Gemini provider configuration contract.
- `84ed49e`: Gemini retrieval and grounded chat.
- `5e28bf9`: KnowledgeOS workspace routes/UI.
- `1909595`: collections and persistent history workspace flows.
- `f44f5dc`: Step Two workspace flows.
- `abf3bd1`: acceptance organization, filters, and RAG QA checkpoint.

At handoff creation, the working tree also contains legitimate source changes to be checkpointed:
the duplicate-safe Vietnamese lexical-anchor fix in `KnowledgeChatService`, the expanded live
RAG benchmark safety checks, and the user-owned QA/runtime artefacts listed in the final receipt.
Do not discard them. Do not stage `.env.local` or unrelated runtime files.

## H. Current Prompt 3 / Local Acceptance status

Latest evidence is source-backed and should supersede older numbers in pre-existing reports.

| Requirement | Status | Evidence / continuation note |
|---|---|---|
| Search by title | PASS | `GET /api/resources?q=` and Library UI |
| Search by tag | PASS | owner-scoped tag filter exercised |
| Collection filter | PASS | owner-scoped collection filter exercised |
| Combined search/filter | PASS | title + tag + collection AND query |
| Vietnamese search | PASS | UTF-8 title search exercised |
| Smart Organization | PASS | review/apply flow in Workspace |
| Smart tag suggestions | PASS | normalized controlled suggestions |
| Smart collection suggestions | PASS | existing or reviewable new collection |
| Related resource suggestions | PASS | pgvector-backed, owner/self filtered |
| Duplicate awareness | PASS/PARTIAL | exact checksum block PASS; semantic warning UX is suggestion-only |
| Critical user journeys | PASS/PARTIAL | real note journey PASS; protected binary chooser remains harness-limited |
| RAG evaluation framework | PASS | executable test plus 25-case fixture dataset |
| Live RAG cases | PASS | 7 checks: 5 answer cases + unsupported + scope isolation |
| Recall@5 / MRR | PASS | 1.000 / 1.000 on the five answer cases |
| Citation validity | PASS | 100% on live benchmark |
| Grounding | PASS | 100% expected-answer rate on five answer cases |
| Scope isolation | PASS | 0 leakage cases |
| Unsupported behavior | PASS | 0 unsupported hallucinations; explicit insufficient-context response |
| Vietnamese RAG | PASS | live controlled case |
| Prompt injection | PASS | evidence delimiters/policy and live case |
| Persistent chat | PASS | sessions/messages/citations reload APIs and UI |
| Backend tests | PASS with caveat | prior full suite: 44 tests, 0 failures, 4 skipped; latest live benchmark PASS after lexical-anchor fix |
| Backend package | PASS | packaged JAR was produced before final handoff-only checkpoint |
| Frontend build | PASS | TypeScript + Vite production build |
| Responsive | PASS | approximately 1440/1024/768/390, no overflow reported |
| Critical bugs | 0 known | no unresolved critical defect recorded |
| Major bugs | 0 known | browser file chooser is an environment limitation, not a known app failure |

The current code change that was discovered during the final live benchmark was a real bug:
`Set.of` contained duplicate Vietnamese stopword `là`, causing an exception on some unsupported
questions. It is fixed in the working tree. The benchmark now reports:

`total=7 recallAt5=1.000 mrr=1.000 citationValidity=1.000 groundedAnswerRate=1.000 scopeLeakage=0 unsupportedHallucinations=0 vietnamese=PASS promptInjection=PASS`.

## I. File-by-file work map

Backend KnowledgeOS:

- `backend/src/main/java/com/groupsync/backend/knowledge/model/`: Resource, DocumentChunk,
  ChatSession, ChatMessage, Citation, lifecycle/type models.
- `.../ingestion/`: parser interface/registry and text, Markdown, PDF, DOCX, note adapters.
- `.../chunking/`: paragraph and recursive chunking strategies.
- `.../rag/`: Gemini clients/config, normalizer, retrieval scope/repository/service, prompt builder.
- `.../storage/`: `StorageService` and local filesystem implementation.
- `.../service/ResourceService.java`: CRUD, upload, ownership, checksum, content, lifecycle entry.
- `.../service/ResourceIngestionService.java`: parse/chunk/embed/persist pipeline.
- `.../service/KnowledgeChatService.java`: persistent chat, scoped retrieval, insufficient context,
  citations; current uncommitted fix is here.
- `.../service/KnowledgeWorkspaceService.java`: collections, tags, notes, relations, progress,
  activity and ownership checks.
- `.../service/OrganizationSuggestionService.java`: deterministic suggestions and explicit apply.
- `.../controller/`: resource, workspace, chat, dashboard HTTP contracts.

Frontend:

- `frontend/src/pages/KnowledgeHomePage.tsx`: flagship Home.
- `KnowledgeLibraryPage.tsx`: search, tag/collection filters, import/note creation.
- `ResourceWorkspacePage.tsx`: resource tabs, notes, activity, related, Ask link, Organize review.
- `KnowledgeAskPage.tsx`: scope selection, session list/reload, answer/citations.
- `KnowledgeFocusPage.tsx`, `KnowledgeInsightsPage.tsx`: learning surfaces.
- `frontend/src/api/knowledge.ts`: typed REST adapter for KnowledgeOS.
- `frontend/src/styles/knowledgeos-responsive.css`: current responsive/design layer.
- `frontend/src/assets/knowledgeos-hero.png`: accepted hero visual.
- `frontend/src/App.tsx`: KnowledgeOS routes plus preserved legacy routes.

Schema/QA:

- `backend/src/main/resources/db/migration/V1__...` through `V8__...`: immutable legacy.
- `V9__knowledge_foundation.sql`, `V10__document_chunks_and_vectors.sql`, `V11__persistent_chat_and_citations.sql`.
- `qa/fixtures/`: reviewable Markdown/text fixtures and `rag-cases.json`.
- `backend/src/test/java/.../RagBenchmarkIntegrationTest.java`: live test; current working-tree expansion.
- `backend/src/test/java/.../RagEvaluationDatasetTest.java`: 25-case dataset shape check.
- `docs/qa/`: matrix, benchmark, journeys, performance report.

Untracked QA screenshots/manifests in `design-work/qa/` and runtime `knowledgeos-storage/` are
preserved as local evidence; do not accidentally commit secrets or large runtime content.

## J. API map

Auth: `GET /api/auth/csrf`, `POST /api/auth/register`, `POST /api/auth/login`,
`GET /api/auth/me` and `/current-user`, `POST /api/auth/logout`.

Resources: `POST /api/resources` multipart, `POST /api/resources/notes`,
`GET /api/resources?q=&tagId=&collectionId=`, `GET/PATCH/DELETE /api/resources/{id}`,
`POST /api/resources/{id}/retry`, `GET /api/resources/{id}/content`.

Collections: `GET/POST /api/collections`, `PATCH/DELETE /api/collections/{id}`,
`GET /api/collections/{id}/resources`, `PUT/DELETE /api/collections/{id}/resources/{resourceId}`.

Tags: `GET/POST /api/tags`, `PATCH/DELETE /api/tags/{id}`,
`GET /api/resources/{resourceId}/tags`, `PUT/DELETE /api/resources/{resourceId}/tags/{tagId}`.

Workspace: notes at `GET/POST /api/resources/{id}/notes`, `PATCH/DELETE .../notes/{noteId}`;
`GET /api/resources/{id}/related`; `GET /api/resources/{id}/activity`;
`PUT /api/resources/{id}/progress`.

Organization: `GET /api/resources/{id}/organization/suggestions`,
`POST /api/resources/{id}/organization/apply` with explicitly selected tag names, collection IDs,
new collection names, and related resource IDs.

RAG: `POST /api/ask` using `AskKnowledgeRequest`; `GET /api/ask/sessions` and
`GET /api/ask/sessions/{sessionId}`. Dashboard: `GET /api/focus/next` and
`GET /api/insights/overview`. Existing GroupSync API modules remain for groups, calendar,
badminton, notifications, profile, and tournament routes.

Known contract limitation: the REST layer uses compact `Map<String,Object>` responses for some
workspace endpoints. Do not expand this into a large API redesign during Local Acceptance.

## K. RAG implementation details

`GeminiEmbeddingProvider` calls `gemini-embedding-001`, normalizes the returned vector, and
requires exactly 768 dimensions. `DocumentChunk.embedding` is a pgvector `vector(768)` field;
metadata includes model and dimensions, plus resource, chunk index, page number, section, and
content.

Semantic retrieval uses top-K from `RAG_TOP_K` (default 5), owner filtering, and the requested
scope: one resource, selected resource IDs, one collection, or the complete owner library.
Results retain resource title/id, chunk id, page/section, content, and distance. Chat maps
retrieved chunks to persistent Citation rows with relevance and evidence excerpt.

`GroundedPromptBuilder` separates trusted application instructions from retrieved text delimited
as untrusted evidence. It asks Gemini to use only supplied evidence, avoid secrets, not obey
instructions inside documents, not invent citations, acknowledge conflict/insufficient evidence,
and answer in the question language where practical. `KnowledgeChatService` also gates weak
retrieval and lexical mismatch, returning an explicit insufficient-context response without
calling generation. The current Vietnamese duplicate-stopword fix must be committed.

Deleting a resource removes its owned content/chunks and related persisted knowledge data through
the service/schema relationships; owner checks are required for every retrieval and workspace
operation. The benchmark uses temporary owner/resources and cleans them after each run.

## L. Smart Organization

`OrganizationSuggestionService` extracts deterministic title/content signals, proposes a limited
controlled tag vocabulary, matches an existing collection or proposes a reviewable collection,
and uses semantic retrieval to propose related owner resources excluding the current resource.
Suggestions are not silently applied. The UI lets the user check individual proposals and sends
only the confirmed IDs/names to `/organization/apply`; service code revalidates ownership.

Exact checksum duplicates are rejected at import. Semantic similarity is not a destructive or
automatic duplicate decision; it is a related/smart-organization signal. Remaining work is only
optional warning UX and a decision about whether to add it to Local Acceptance; do not invent a
large autonomous organization system.

## M. QA and testing

Backend full suite: from `backend`, `./mvnw.cmd test` (PowerShell: `& .\mvnw.cmd test`). The
previous complete run passed 44 tests with 0 failures and 4 skipped. Network dependency access
may require the approved elevated execution environment.

Package: `./mvnw.cmd package -DskipTests`.

Frontend: from `frontend`, `npm.cmd run build`; lint if desired with `npm.cmd run lint`.

Live benchmark (uses real Neon/Gemini and must never print env values): load ignored `.env.local`
into the process, set `KNOWLEDGEOS_RAG_EVAL=true`, then from `backend` run
`./mvnw.cmd -Dtest=RagBenchmarkIntegrationTest test`. The latest run passed and produced the
seven-check metrics recorded above. Never commit benchmark secrets.

Fixtures are in `qa/fixtures/`: `project-orion.md`, `project-orion-revision.md`, `atlas.md`,
`knowledge-policy.md`, `meeting-notes.txt`, `vietnamese-learning.md`, `irrelevant.md`,
`prompt-injection.md`, and `rag-cases.json`. They contain controlled facts for direct retrieval,
Vietnamese, conflicting evidence, scope isolation, unsupported questions, and injection tests.

Read alongside this handoff:

- [Requirements matrix](../qa/REQUIREMENTS_MATRIX.md)
- [RAG benchmark](../qa/RAG_BENCHMARK.md)
- [User journeys](../qa/USER_JOURNEYS.md)
- [Performance report](../qa/PERFORMANCE_REPORT.md)
- [Implementation status](../IMPLEMENTATION_STATUS.md)
- `docs/qa/FINAL_LOCAL_ACCEPTANCE.md` if it exists in a later checkpoint.

Do not claim browser PASS from source inspection alone. Browser evidence previously covered the
real note journey, search/filter, Smart Organization, Ask, chat reload, Focus, Insights, profile,
and responsive widths. Binary file chooser remained blocked by the protected harness.

## N. Local runtime/development runbook

The repository helper is `scripts/start-groupsync.ps1`. It loads local environment values,
starts/reuses the backend, waits for `GET http://127.0.0.1:8080/api/health`, builds/serves the
frontend, and uses a same-origin preview proxy on port 4173. Do not overwrite this helper: it is
an existing user-owned working-tree change.

Manual backend: from `backend`, load local env names into the process and run
`./mvnw.cmd spring-boot:run` or the packaged JAR. Frontend: from `frontend`, `npm.cmd run dev`
or use the helper/preview. Expected ports are backend 8080 and frontend 4173 (Vite may choose a
nearby port if occupied).

Required environment variable names are `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `GEMINI_API_KEY`,
`KNOWLEDGE_STORAGE_LOCAL_ROOT`, and optional Gemini/app settings in
`backend/src/main/resources/application.properties`. `.env.local` at repository root is ignored;
it is a local development mechanism, not a Spring dependency and never belongs in Git.

Neon uses a direct SSL JDBC URL, not the pooler endpoint, for Flyway/schema work. Health, Flyway
version, and runtime startup should be checked without echoing credentials. The old portable
PostgreSQL 17 runtime/data was not modified for pgvector and must not be deleted or reused as the
development database.

Windows history: a backend JAR lock was cleared by a full restart, not by killing an unverified
process. If a future JAR lock occurs, identify the exact project process before stopping it.

## O. Production state

Production delivery has NOT been finalized. The intended architecture is Render PostgreSQL with
pgvector, persistent resource storage, and a Vercel-style frontend integration as appropriate.
Before release, verify storage persistence, Render/Vercel environment secrets, CORS/cookie
settings, health, migrations, and rollback. Rename `dbp3206-source/group_sync` to
`dbp3206-source/knowledgeos` only after local acceptance, deployment integration, permissions,
and push/pull checks are proven. Update the local remote only after the rename. Do not deploy,
merge main, or rename during this handoff.

## P. Ordered next work

The new agent should not restart Phase 0 or add a major feature. The safe continuation order is:

1. Read this handoff, inspect `git status`, and preserve all listed user-owned files.
2. Commit the current coherent source checkpoint, including the Vietnamese lexical-anchor fix
   and expanded benchmark, plus this handoff/manifest and corrected QA numbers. Push the migration
   branch; do not stage `.env.local`, runtime storage, or untracked browser evidence unless a
   later explicit evidence policy permits it.
3. Run the ordinary full backend test/package and frontend build once after that checkpoint if
   the environment is available; compare results to the recorded PASS evidence.
4. If Local Acceptance requires a final browser pass, retest the binary file chooser in a browser
   that permits safe disposable fixtures; otherwise record the protected harness limitation.
5. Stop after Local Acceptance documentation is truthful. Production Prompt 4 is a separate stage.

Remaining task count at transfer: **3** — final checkpoint commit/push, one final regression/build
confirmation, and optional binary-upload browser E2E recheck. No architecture rewrite is pending.

## Q. Do-not-change contract

- Keep Spring Boot modular monolith with React/TypeScript frontend.
- Keep PostgreSQL + pgvector; do not add Qdrant or another vector database.
- Keep Gemini; generation `gemini-3.5-flash-lite`, embeddings `gemini-embedding-001`, 768 dims.
- Do not restore OpenAI in the final RAG path.
- Do not edit or rewrite Flyway V1-V8.
- Do not mass-rename the internal `com.groupsync.backend` package without a low-risk, verified reason.
- Do not merge `main`, rename the repository, or deploy production in this stage.
- Do not expose or commit secrets.
- Preserve the accepted KnowledgeOS visual direction and use `$design-taste-frontend` for future UI work.
- Preserve legitimate user working-tree changes and runtime QA evidence.
- Avoid unnecessary enterprise infrastructure and opaque LLM-only recommendations.
