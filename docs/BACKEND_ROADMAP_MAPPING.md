# KnowledgeOS — Backend Roadmap Mapping
> Mapping KnowledgeOS Architecture against [roadmap.sh/backend](https://roadmap.sh/backend)

---

## 1. Overview and Purpose

This document provides an educational bridge between the industry-standard **roadmap.sh/backend** learning curriculum and the concrete codebase of **KnowledgeOS**. 

KnowledgeOS is designed as an authored, academically defensible third-year university Object-Oriented Programming (OOP) and Backend Engineering project. Rather than introducing artificial complexity (such as distributed message queues or multi-tier microservices), KnowledgeOS implements a **robust, single-process modular monolith** using modern Java 21, Spring Boot 4, PostgreSQL with `pgvector`, and Google Gemini.

### Conceptual Mapping Table

| Roadmap Area | Used? | KnowledgeOS Technology | Actual Codebase Reference | Learning Depth |
|---|---|---|---|---|
| **Internet & Web Basics** | Yes | HTTP/1.1, TLS, JSON, CORS | `SecurityConfig.java`, `client.ts` | Fundamental |
| **Java Language** | Yes | Java 21 (Records, Sealed/Interfaces, Streams, Enums) | `Resource.java`, `DocumentChunk.java` | Core |
| **Build & Dependency Mgmt** | Yes | Apache Maven + Maven Wrapper (`mvnw`) | `backend/pom.xml` | Fundamental |
| **Relational Database** | Yes | PostgreSQL 16/17 (Neon / Render) | `backend/src/main/resources/db/migration/` | Core / Advanced |
| **Database Migrations** | Yes | Flyway (Migrations V1 through V13) | `V9__knowledge_foundation.sql` .. `V13__storage_blobs.sql` | Core |
| **Object-Relational Mapping** | Yes | Spring Data JPA + Hibernate + JDBC | `ResourceRepository.java`, `KnowledgeWorkspaceService.java` | Core |
| **Web Framework** | Yes | Spring Boot 4 (`@RestController`, `@Service`) | `ResourceController.java`, `ResourceService.java` | Core |
| **API Design** | Yes | RESTful HTTP + JSON + Multipart Form | `ResourceController.java`, `KnowledgeChatController.java` | Core |
| **Authentication & Security** | Yes | Spring Security + Session Cookies + BCrypt | `SecurityConfig.java`, `AuthService.java` | Core |
| **File / Binary Storage** | Yes | PostgreSQL BYTEA Blobs (`DatabaseStorageService`) | `DatabaseStorageService.java`, `V13__storage_blobs.sql` | Applied |
| **Vector Database & Search** | Yes | PostgreSQL `pgvector` (768-dim cosine + HNSW) | `V10__document_chunks_and_vectors.sql`, `SemanticRetrievalStrategy.java` | Advanced |
| **Full-Text Search (FTS)** | Yes | PostgreSQL FTS (`tsvector`, `tsquery`, GIN) | `V12__lexical_fts_index.sql`, `KeywordRetrievalStrategy.java` | Advanced |
| **Hybrid Retrieval & RRF** | Yes | Reciprocal Rank Fusion ($k=60$) | `HybridRetrievalStrategy.java` | Advanced |
| **External AI / LLM APIs** | Yes | Google Gemini (`gemini-3.5-flash-lite`, `gemini-embedding-001`) | `GeminiEmbeddingProvider.java`, `GeminiLanguageModelClient.java` | Applied |
| **Automated Testing** | Yes | JUnit 5, Mockito, SpringBootTest | `backend/src/test/java/...` | Core |
| **CI / Deployment** | Yes | Vercel (Frontend) + Render (Backend) + Git | `render.yaml`, `vercel.json` | Practical |

---

## 2. Deep Dive by Backend Topic

Every section below follows the seven-step analysis formula:
1. **What is it?**
2. **Why does it exist?**
3. **How does KnowledgeOS use it?**
4. **Where is it in source?**
5. **How does a real request flow through it?**
6. **Why was this design chosen?**
7. **Trade-off / Limitation.**

---

### Topic 1: Internet, Client-Server & HTTP

#### 1. What is it?
The client-server model is an architecture where client devices (such as web browsers) send requests across the Internet to a centralized server, which processes the request and returns a structured response using the Hypertext Transfer Protocol (HTTP/HTTPS).

#### 2. Why does it exist?
It decouples the user interface from data persistence and heavy computation, allowing multiple clients to access shared state securely without running server-side logic locally.

#### 3. How does KnowledgeOS use it?
KnowledgeOS deploys a React Single Page Application (SPA) on Vercel that interacts with a Spring Boot REST API hosted on Render over HTTPS. Request payloads and responses are encoded in JSON, while binary file uploads use `multipart/form-data`.

#### 4. Where is it in source?
- Frontend client: [`frontend/src/api/client.ts`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/frontend/src/api/client.ts)
- Backend CORS and Security: [`backend/src/main/java/com/groupsync/backend/auth/security/SecurityConfig.java`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/java/com/groupsync/backend/auth/security/SecurityConfig.java)

#### 5. How does a real request flow through it?
1. User clicks **"Ask"** in the browser.
2. React triggers `client.post('/api/knowledge/chat', { question, scope: 'LIBRARY' })`.
3. Browser transmits an HTTPS POST request with an HTTP-only session cookie.
4. Render's reverse proxy terminates TLS and forwards the HTTP request to Spring Boot port 8080.
5. Spring Boot parses the JSON payload, runs retrieval and AI inference, and streams back a JSON response.

#### 6. Why was this design chosen?
Separating the frontend into a static SPA and backend into an API allows independent deployment, caching of static assets on CDN edges, and standard REST contracts.

#### 7. Trade-off / Limitation
Network latency exists between the client and server. If the server is offline or experiencing cold starts on Render's free tier, the client UI must display graceful loading and error states.

---

### Topic 2: Java 21 & Object-Oriented Programming

#### 1. What is it?
Java 21 is a strongly-typed, class-based, object-oriented programming language running on the Java Virtual Machine (JVM). It features modern syntax additions such as records, pattern matching, enhanced switch expressions, and virtual threads.

#### 2. Why does it exist?
Java provides memory safety, automatic garbage collection, platform independence ("write once, run anywhere"), and a mature enterprise ecosystem with strong compile-time type guarantees.

#### 3. How does KnowledgeOS use it?
KnowledgeOS leverages Java 21 as the primary backend language. Domain models encapsulate state and lifecycle transitions (`Resource.beginParsing()`, `Resource.markReady()`). Polymorphic interfaces define contracts for file parsing (`ResourceParser`), chunking (`ChunkingStrategy`), vector embedding (`EmbeddingProvider`), and retrieval (`RetrievalStrategy`).

#### 4. Where is it in source?
- Entity behavior: [`backend/src/main/java/com/groupsync/backend/knowledge/model/Resource.java`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/java/com/groupsync/backend/knowledge/model/Resource.java)
- Polymorphic interfaces: [`backend/src/main/java/com/groupsync/backend/knowledge/rag/RetrievalStrategy.java`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/java/com/groupsync/backend/knowledge/rag/RetrievalStrategy.java)
- Strategy implementations: [`HybridRetrievalStrategy.java`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/java/com/groupsync/backend/knowledge/rag/HybridRetrievalStrategy.java), [`SemanticRetrievalStrategy.java`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/java/com/groupsync/backend/knowledge/rag/SemanticRetrievalStrategy.java), [`KeywordRetrievalStrategy.java`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/java/com/groupsync/backend/knowledge/rag/KeywordRetrievalStrategy.java)

#### 5. How does a real request flow through it?
When a query arrives, `KnowledgeChatService` calls `RetrievalStrategy.retrieve(query, scope)`. Spring injects the `@Primary` bean (`HybridRetrievalStrategy`), which in turn invokes `SemanticRetrievalStrategy` and `KeywordRetrievalStrategy` via polymorphism, fusing their outputs without `KnowledgeChatService` needing to know how each retrieval branch works internally.

#### 6. Why was this design chosen?
It demonstrates the **Strategy Pattern** and **Open/Closed Principle (OCP)**. New retrieval algorithms (such as dense rerankers) can be added by implementing `RetrievalStrategy` without modifying existing chat service logic.

#### 7. Trade-off / Limitation
Creating explicit interfaces and strategy classes requires more boilerplate than writing procedural scripts in Python or Node.js. However, it yields vastly superior testability and long-term maintainability.

---

### Topic 3: Spring Boot & Dependency Injection

#### 1. What is it?
Spring Boot is an opinionated framework that simplifies building production-ready Spring applications by providing auto-configuration, an embedded Tomcat server, and an Inversion of Control (IoC) container for Dependency Injection (DI).

#### 2. Why does it exist?
Without DI, classes must instantiate their own dependencies using the `new` operator, tightly coupling classes together and making unit testing with mock objects virtually impossible.

#### 3. How does KnowledgeOS use it?
KnowledgeOS uses constructor injection across all components. Spring manages the lifecycle of controllers (`@RestController`), services (`@Service`), and repositories (`@Repository`). Configuration properties are bound via `@Configuration` and `@Value`.

#### 4. Where is it in source?
- Service injection: [`backend/src/main/java/com/groupsync/backend/knowledge/service/ResourceService.java`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/java/com/groupsync/backend/knowledge/service/ResourceService.java)
- Controller injection: [`backend/src/main/java/com/groupsync/backend/knowledge/controller/ResourceController.java`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/java/com/groupsync/backend/knowledge/controller/ResourceController.java)

#### 5. How does a real request flow through it?
1. At application startup, the Spring IoC container instantiates `DatabaseStorageService`, `DocumentChunkRepository`, and `ResourceRepository`.
2. Spring passes these dependencies into `ResourceService`'s constructor.
3. Spring passes `ResourceService` into `ResourceController`'s constructor.
4. When an HTTP request hits `ResourceController`, the fully-wired instance executes the method seamlessly.

#### 6. Why was this design chosen?
Constructor injection guarantees that objects are never created in an uninitialized state and allows unit tests to instantiate services directly by passing mock dependencies without starting the Spring container.

#### 7. Trade-off / Limitation
Spring Boot has a moderate memory footprint and startup time compared to lightweight Go or Rust runtimes.

---

### Topic 4: Relational Databases & PostgreSQL

#### 1. What is it?
PostgreSQL is an open-source, object-relational database management system (ORDBMS) emphasizing extensibility, ACID transaction compliance, and standards conformance.

#### 2. Why does it exist?
Relational databases store structured data in normalized tables with strict types, primary keys, and foreign keys to ensure data integrity, prevent anomalies, and enable expressive SQL queries.

#### 3. How does KnowledgeOS use it?
PostgreSQL serves as the single source of truth for:
- User accounts and authentication state (`users`, `user_profiles`)
- Resource metadata (`resources`, `resource_tags`, `resource_collections`)
- Extracted text chunks and 768-dimensional embeddings (`document_chunks`)
- Persistent chat sessions, messages, and grounded citations (`chat_sessions`, `chat_messages`, `citations`)
- Binary file storage blobs (`storage_blobs`)

#### 4. Where is it in source?
- Schema migrations: [`backend/src/main/resources/db/migration/`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/resources/db/migration)
- Repositories: [`ResourceRepository.java`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/java/com/groupsync/backend/knowledge/repository/ResourceRepository.java), [`DocumentChunkRepository.java`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/java/com/groupsync/backend/knowledge/repository/DocumentChunkRepository.java)

#### 5. How does a real request flow through it?
When a user deletes a resource, `ResourceService` initiates a database transaction. It queries foreign key references in `citations`, removes associated records, clears `document_chunks`, removes binary data from `storage_blobs`, and finally deletes the `resources` row. If any step fails, the transaction rolls back completely.

#### 6. Why was this design chosen?
Using PostgreSQL for both relational metadata and vector embeddings (via `pgvector`) eliminates the operational complexity of managing a separate vector database (such as Pinecone or Milvus). ACID transactions guarantee relational consistency between document records and vector chunks.

#### 7. Trade-off / Limitation
Storing binary blobs directly in PostgreSQL (`storage_blobs`) increases database disk usage. For high-scale enterprise systems, offloading to object storage (like AWS S3) is preferred in v2.

---

### Topic 5: Database Migrations with Flyway

#### 1. What is it?
Flyway is an open-source database migration tool that versions database schema changes using sequential SQL scripts (`V1__...sql`, `V2__...sql`).

#### 2. Why does it exist?
Without migration versioning, synchronizing database schema changes across developer machines, CI test databases, and production servers leads to manual errors, schema drift, and unrepeatable deployments.

#### 3. How does KnowledgeOS use it?
KnowledgeOS uses 13 sequential Flyway migrations from initial baseline (`V1`) to knowledge foundation (`V9`), vector chunks (`V10`), chat citations (`V11`), lexical FTS (`V12`), and storage blobs (`V13`).

#### 4. Where is it in source?
- Migration directory: [`backend/src/main/resources/db/migration/`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/resources/db/migration)
- Storage blob migration: [`V13__storage_blobs.sql`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/resources/db/migration/V13__storage_blobs.sql)

#### 5. How does a real request flow through it?
On application startup, Flyway checks the `flyway_schema_history` table in PostgreSQL. If new migration files exist in `classpath:db/migration/`, Flyway applies each script in order within a database transaction, recording the checksum and execution time.

#### 6. Why was this design chosen?
It guarantees that any developer or deployment environment can spin up a complete, identical database schema from scratch by running `mvn spring-boot:run`.

#### 7. Trade-off / Limitation
Once a migration has been applied in production, its SQL script must never be edited or reordered. Subsequent changes require creating a new versioned migration file (`V14__...sql`).

---

### Topic 6: Vector Database, pgvector & Embeddings

#### 1. What is it?
`pgvector` is a PostgreSQL extension that adds support for vector data types, exact nearest-neighbor search, and approximate nearest neighbor (ANN) indexing methods like HNSW (Hierarchical Navigable Small World).

#### 2. Why does it exist?
Traditional keyword searches cannot capture semantic meaning or contextual synonyms. Converting text into dense mathematical vectors (embeddings) allows finding conceptually related text by calculating cosine distance.

#### 3. How does KnowledgeOS use it?
During document ingestion, text chunks are converted into 768-dimensional float arrays using Google's `gemini-embedding-001`. The embeddings are stored in the `vector(768)` column of `document_chunks`.

#### 4. Where is it in source?
- Vector column definition: [`V10__document_chunks_and_vectors.sql`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/resources/db/migration/V10__document_chunks_and_vectors.sql)
- Semantic query: [`SemanticRetrievalStrategy.java`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/java/com/groupsync/backend/knowledge/rag/SemanticRetrievalStrategy.java)

#### 5. How does a real request flow through it?
1. User enters: *"How does memory isolation work?"*
2. `GeminiEmbeddingProvider` generates a 768-dim query vector.
3. `SemanticRetrievalStrategy` executes a native SQL query:
   ```sql
   SELECT chunk_id, content, 1 - (embedding <=> :queryVector) AS similarity
   FROM document_chunks WHERE owner_id = :ownerId
   ORDER BY embedding <=> :queryVector LIMIT 10;
   ```
4. PostgreSQL uses vector cosine distance (`<=>`) to return the closest matching chunks.

#### 6. Why was this design chosen?
It provides sub-second semantic retrieval directly inside PostgreSQL without adding external third-party infrastructure.

#### 7. Trade-off / Limitation
Creating HNSW indexes on large vector datasets consumes significant RAM. In free-tier cloud environments with limited memory, index creation parameters (`m=16, ef_construction=64`) must be balanced against available resources.

---

### Topic 7: PostgreSQL Full-Text Search (FTS) & Lexical Retrieval

#### 1. What is it?
PostgreSQL Full-Text Search parses text into normalized lexemes (`tsvector`) and matches them against search queries (`tsquery`) using linguistic stemming and Generalized Inverted Index (GIN) indexing.

#### 2. Why does it exist?
Semantic vector search can fail to find exact technical identifiers, error codes, CVE numbers, or specific function names (e.g. `CVE-2026-8819`, `RFC-9421`, `blankToNull`). Lexical search guarantees exact keyword matching.

#### 3. How does KnowledgeOS use it?
KnowledgeOS maintains a generated `tsvector` column `tsv` on `document_chunks` indexed with a GIN index. `KeywordRetrievalStrategy` performs fast lexical retrieval using `websearch_to_tsquery('simple', :query)`.

#### 4. Where is it in source?
- Schema & GIN index: [`V12__lexical_fts_index.sql`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/resources/db/migration/V12__lexical_fts_index.sql)
- Lexical query: [`KeywordRetrievalStrategy.java`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/java/com/groupsync/backend/knowledge/rag/KeywordRetrievalStrategy.java)

#### 5. How does a real request flow through it?
1. User queries: *"What does CVE-2026-8819 fix?"*
2. `KeywordRetrievalStrategy` formats the search query with `plainto_tsquery('simple', 'CVE-2026-8819')`.
3. PostgreSQL queries the GIN index on `document_chunks.tsv` and scores results using `ts_rank_cd()`.
4. The exact matching chunk is returned as Rank 1.

#### 6. Why was this design chosen?
Using `'simple'` dictionary avoids aggressive English-only stemming, ensuring exact preservation of numbers, hyphens, and Vietnamese characters.

#### 7. Trade-off / Limitation
FTS does not recognize semantic synonyms (e.g., searching "car" will not match "automobile"). This is why KnowledgeOS combines FTS with Vector search via Hybrid RRF.

---

### Topic 8: Reciprocal Rank Fusion (RRF) & Hybrid RAG

#### 1. What is it?
Reciprocal Rank Fusion (RRF) is an algorithm that combines the ranked results of multiple independent search systems into a single unified score without requiring score normalization.

#### 2. Why does it exist?
Cosine similarity scores (ranging from -1.0 to 1.0) and `ts_rank_cd` scores (ranging from 0.0 to 1.0+) cannot be directly added together because their underlying distributions are fundamentally different. RRF solves this by scoring candidates based purely on their relative position (rank).

#### 3. How does KnowledgeOS use it?
KnowledgeOS implements RRF with standard smoothing constant $k=60$:
$$\text{RRF Score} = \sum_{m \in \{\text{semantic}, \text{lexical}\}} \frac{1}{60 + \text{rank}_m(d)}$$

#### 4. Where is it in source?
- Implementation: [`backend/src/main/java/com/groupsync/backend/knowledge/rag/HybridRetrievalStrategy.java`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/java/com/groupsync/backend/knowledge/rag/HybridRetrievalStrategy.java)

#### 5. How does a real request flow through it?
1. `SemanticRetrievalStrategy` retrieves top 10 chunks: $[C_1, C_2, C_3]$.
2. `KeywordRetrievalStrategy` retrieves top 10 chunks: $[C_3, C_1, C_4]$.
3. `HybridRetrievalStrategy` merges them:
   - $C_1$: $\frac{1}{60+1} + \frac{1}{60+2} = 0.01639 + 0.01613 = 0.03252$
   - $C_3$: $\frac{1}{60+3} + \frac{1}{60+1} = 0.01587 + 0.01639 = 0.03226$
   - $C_2$: $\frac{1}{60+2} = 0.01613$
   - $C_4$: $\frac{1}{60+3} = 0.01587$
4. Chunks ranking high in both branches receive top priority in the LLM prompt context.

#### 6. Why was this design chosen?
It produces significantly higher Recall@K and MRR across both semantic questions and exact technical identifiers than either strategy alone.

#### 7. Trade-off / Limitation
Executing two database queries per user question increases retrieval latency by approximately 15–25ms, which is negligible compared to the 800–1200ms LLM generation time.

---

### Topic 9: REST API Design, JSON & DTOs

#### 1. What is it?
Representational State Transfer (REST) is an architectural style for networked applications. It utilizes standard HTTP methods (`GET`, `POST`, `PATCH`, `DELETE`), resource-oriented URIs (`/api/resources/{id}`), and JSON payloads.

#### 2. Why does it exist?
REST provides a uniform, predictable interface that decouples frontend client implementations from backend database schema representations.

#### 3. How does KnowledgeOS use it?
KnowledgeOS exposes cleanly versioned REST controllers returning JSON Data Transfer Objects (DTOs) and proper HTTP status codes (`200 OK`, `201 Created`, `202 Accepted`, `204 No Content`, `400 Bad Request`, `401 Unauthorized`, `404 Not Found`).

#### 4. Where is it in source?
- DTO contracts: [`backend/src/main/java/com/groupsync/backend/knowledge/dto/`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/java/com/groupsync/backend/knowledge/dto)
- Controllers: [`ResourceController.java`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/java/com/groupsync/backend/knowledge/controller/ResourceController.java), [`KnowledgeChatController.java`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/java/com/groupsync/backend/knowledge/controller/KnowledgeChatController.java)

#### 5. How does a real request flow through it?
1. Client sends `PATCH /api/resources/42` with JSON `{"title": "Updated Title", "favorite": true}`.
2. Spring maps JSON to `UpdateResourceRequest` DTO and executes Bean Validation (`@Size(max=255)`).
3. `ResourceService` updates the entity and returns a `ResourceResponse` DTO.
4. Jackson serializes the DTO to JSON and returns `200 OK`.

#### 6. Why was this design chosen?
DTOs prevent over-posting vulnerabilities and decouple API contracts from database column changes.

#### 7. Trade-off / Limitation
Maintaining separate request and response DTO classes requires explicit mapping methods, but guarantees strong contract stability.

---

### Topic 10: Authentication, Authorization & Security

#### 1. What is it?
Authentication verifies *who* the user is; authorization verifies *what* resources they have permission to access.

#### 2. Why does it exist?
To protect sensitive user documents, maintain privacy, and prevent unauthorized cross-tenant data access.

#### 3. How does KnowledgeOS use it?
- Passwords hashed using standard BCrypt (`strength=10`).
- Stateful server-side sessions with secure HTTP-only cookies (`JSESSIONID`).
- Strict owner isolation: every database query filters by `owner_id = :authenticatedUserId`.
- CSRF protection enabled for browser-initiated mutations.

#### 4. Where is it in source?
- Security configuration: [`backend/src/main/java/com/groupsync/backend/auth/security/SecurityConfig.java`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/java/com/groupsync/backend/auth/security/SecurityConfig.java)
- Auth service: [`backend/src/main/java/com/groupsync/backend/auth/service/AuthService.java`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/backend/src/main/java/com/groupsync/backend/auth/service/AuthService.java)

#### 5. How does a real request flow through it?
1. User submits `POST /api/auth/login` with email and password.
2. `AuthService` verifies BCrypt password hash against `users.password_hash`.
3. Spring Security establishes an `HttpSession` and sets a `Set-Cookie: JSESSIONID=...; HttpOnly; SameSite=Lax` header.
4. Subsequent requests pass `JSESSIONID`; Spring's `SecurityContextHolder` extracts `AuthenticatedUser`.
5. Service methods verify `resource.getOwnerId().equals(user.getId())`.

#### 6. Why was this design chosen?
Server-side sessions with HTTP-only cookies protect against Cross-Site Scripting (XSS) token theft and simplify logout/revocation.

#### 7. Trade-off / Limitation
Server sessions require memory on the backend instance. For horizontal scaling across multiple servers, session clustering or Redis session storage would be added in v2.

---

## 3. Unused Backend Roadmap Topics in KnowledgeOS v1

| Roadmap Topic | What is it? (1 Sentence) | Why not used in KnowledgeOS v1? (1–2 Sentences) |
|---|---|---|
| **Redis / Distributed Cache** | An in-memory key-value data store used for sub-millisecond caching and distributed locks. | Single-node PostgreSQL with buffer cache and connection pooling is fast enough for the target single-user / student workload without extra infrastructure. |
| **Message Brokers (Kafka / RabbitMQ)** | Asynchronous distributed pub-sub messaging platforms for high-throughput event streaming. | Document ingestion in KnowledgeOS runs synchronously within background threads; distributed event brokers would introduce unnecessary operational overhead. |
| **Microservices Architecture** | Architectural pattern splitting an application into independent, network-communicating services. | KnowledgeOS is intentionally built as an explainable **modular monolith**, eliminating distributed transaction and network partitioning complexities. |
| **GraphQL / gRPC** | Alternative API query languages and binary RPC protocols over HTTP/2. | RESTful JSON over HTTP is the universal standard for web applications and aligns directly with third-year university course requirements. |
| **Kubernetes / Docker Swarm** | Container orchestration platforms for managing multi-host container clusters. | The application runs cleanly as a single container on Render and static build on Vercel, making Kubernetes excessive for current scale. |
