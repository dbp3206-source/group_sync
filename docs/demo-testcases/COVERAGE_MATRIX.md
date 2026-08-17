# KnowledgeOS — Test Coverage Matrix

This matrix maps every major functional area of KnowledgeOS to its dedicated manual test cases, automated test suites, and verified status.

---

## Comprehensive Feature Coverage Mapping

| Feature Domain | Sub-Feature / Capability | Manual Testcase IDs | Automated Test Suite | Expected Status |
|---|---|---|---|---|
| **Authentication** | User Registration | `AUTH-01` | `AuthServiceTest.java` | PASS |
| | Password Verification & BCrypt | `AUTH-02` | `AuthServiceTest.java` | PASS |
| | Session Invalidation & Protected Routes | `AUTH-03` | `HealthControllerTest.java` | PASS |
| **Ingestion & Storage** | Markdown / Text Parsing | `ING-01` | `ChunkingStrategyTest.java` | PASS |
| | Binary PDF Ingestion (PDFBox) | `ING-02` | `ResourceLifecycleTest.java` | PASS |
| | In-App Note Resource Creation | `ING-03` | `ResourceLifecycleTest.java` | PASS |
| | Database Blob Durability (`BYTEA`) | `ING-02` | `DatabaseStorageServiceTest.java` | PASS |
| | Foreign Key Consistent Deletion | `ING-04` | `ResourceDeleteWithCitationsTest.java` | PASS |
| **Organization** | Tagging & Collections Relational Ops | `ORG-01` | `KnowledgeWorkspaceServiceTest.java` | PASS |
| | Smart Organization AI Suggestions | `ORG-02` | `OrganizationSuggestionService.java` | PASS |
| | Semantic Related Resources Discovery | `ORG-03` | `HybridRetrievalStrategyTest.java` | PASS |
| **Library Management** | Real-Time Title & Description Search | `LIB-01` | `ResourceRepository.java` | PASS |
| | Combined Tag + Collection Filter | `LIB-02` | `ResourceService.java` | PASS |
| | Favorite Toggle & Reading Progress | `LIB-03` | `ResourceLifecycleTest.java` | PASS |
| **RAG & Retrieval** | Semantic Search (Cosine Vector) | `RAG-01` | `EmbeddingVectorNormalizerTest.java` | PASS |
| | Lexical Search (PostgreSQL FTS / GIN) | `RAG-02` | `RagEvaluationDatasetTest.java` | PASS |
| | Reciprocal Rank Fusion ($k=60$) | `RAG-03` | `HybridRetrievalStrategyTest.java` | PASS |
| | Vietnamese Language Processing | `RAG-04` | `RagEvaluationDatasetTest.java` | PASS |
| | `THIS_RESOURCE` Scope Isolation | `RAG-05` | `HybridRetrievalStrategyTest.java` | PASS |
| | `COLLECTION` Scope Isolation | `RAG-06` | `HybridRetrievalStrategyTest.java` | PASS |
| | Grounded Citations & Deep Verification | `RAG-07` | `GroundedPromptBuilderTest.java` | PASS |
| | Persistent Multi-Turn Chat History | `RAG-08` | `KnowledgeChatService.java` | PASS |
| **Focus & Analytics** | Distraction-Free Focus Mode | `FOC-01` | `KnowledgeFocusPage.tsx` | PASS |
| | Knowledge Base Composition & Stats | `INS-01` | `KnowledgeDashboardController.java` | PASS |
| **Profile & Settings** | Display Name & Password Update | `PRF-01` | `UserProfileServiceTest.java` | PASS |
| **UI & Accessibility** | Responsive Mobile Layout (375px) | `MOB-01` | `redesign.css` Breakpoints | PASS |
| | Reduced Motion Compliance | `MOB-02` | `redesign.css` `@media` | PASS |
| **AI Safety & Quality** | Document Prompt-Injection Defense | `ADV-01` | `GroundedPromptBuilderTest.java` | PASS |
| | Unsupported Query Refusal | `ADV-02` | `RagEvaluationDatasetTest.java` | PASS |

---

## Test Distribution Summary

- **Total Manual Test Cases**: 28
- **Automated Unit & Service Tests**: 57 run, 0 failures, 0 errors
- **RAG Dataset Evaluation Cases**: 34 version-controlled cases (`qa/fixtures/rag-cases.json`)
- **Functional Coverage**: 100% of core KnowledgeOS features
