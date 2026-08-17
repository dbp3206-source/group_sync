# CẦU NỐI LỘ TRÌNH KỸ SƯ BACKEND & PHÂN TÍCH KIẾN TRÚC KNOWLEDGEOS
> **Tài liệu**: Ánh xạ Kiến trúc KnowledgeOS theo Khung Chuẩn [roadmap.sh/backend](https://roadmap.sh/backend)  
> **Mã định danh**: `docs/03_curriculum_mapping/BACKEND_ROADMAP_MAPPING.md`  
> **Mục đích**: Cung cấp tài liệu học thuật đối chiếu kiến thức lý thuyết từ lộ trình kỹ sư backend chuẩn quốc tế với mã nguồn thực tế của dự án, phục vụ sinh viên tự học và giảng viên thẩm định.

---

## 1. Tổng quan & Mục tiêu Tài liệu

Tài liệu này tạo nên một cây cầu nối giáo dục giữa lộ trình đào tạo kỹ sư backend công nghiệp (**roadmap.sh/backend**) và hệ thống mã nguồn thực tế của **KnowledgeOS**. 

Dự án được thiết kế chuẩn mực theo cấp độ **Đồ án Môn học Lập trình Hướng đối tượng (OOP) và Công nghệ Phần mềm năm thứ 3 đại học**. Thay vì đưa vào các công nghệ phân tán phức tạp một cách khiên cưỡng (như Message Brokers, Microservices, Kubernetes), KnowledgeOS tập trung hiện thực hóa một kiến trúc **Modular Monolith vững chắc, dễ hiểu, dễ kiểm thử và có thể giải thích trọn vẹn trong buổi bảo vệ đồ án** bằng Java 21, Spring Boot 4, PostgreSQL (kết hợp `pgvector` & FTS) và Google Gemini API.

### Bảng Ánh xạ Khái niệm Chuẩn Backend

| Nhóm Chủ đề Roadmap | Áp dụng? | Công nghệ trong KnowledgeOS | Vị trí Mã nguồn Cụ thể | Cấp độ Đào tạo |
|---|---|---|---|---|
| **Internet & Web Basics** | Có | HTTP/1.1, TLS, JSON, CORS, Cookies | `SecurityConfig.java`, `client.ts` | Cơ bản |
| **Ngôn ngữ Java Hiện đại** | Có | Java 21 (Records, Sealed Types, Streams, Enums) | `Resource.java`, `DocumentChunk.java` | Cốt lõi |
| **Quản lý Build & Dependency** | Có | Apache Maven + Maven Wrapper (`mvnw`) | `src/backend/pom.xml` | Cơ bản |
| **Cơ sở Dữ liệu Quan hệ** | Có | PostgreSQL 17 (Bảng quan hệ, Khóa ngoại, ACID) | `src/backend/src/main/resources/db/migration/` | Nâng cao |
| **Quản lý Di chuyển DB (Migrations)** | Có | Flyway (13 Migrations từ V1 đến V13) | `V9__knowledge_foundation.sql` .. `V13__storage_blobs.sql` | Cốt lõi |
| **Ánh xạ Quan hệ Thực thể (ORM)** | Có | Spring Data JPA + Hibernate + JDBC Native Queries | `ResourceRepository.java`, `DocumentChunkRepository.java` | Cốt lõi |
| **Khung ứng dụng Web (Framework)** | Có | Spring Boot 4 (`@RestController`, `@Service`, `@Transactional`) | `ResourceController.java`, `ResourceService.java` | Cốt lõi |
| **Thiết kế API Chuẩn** | Có | RESTful HTTP + JSON DTOs + Multipart Uploads | `ResourceController.java`, `KnowledgeChatController.java` | Cốt lõi |
| **Xác thực & An toàn Bảo mật** | Có | Spring Security + Session Cookies (`JSESSIONID`) + BCrypt | `SecurityConfig.java`, `AuthService.java` | Cốt lõi |
| **Lưu trữ Tệp Nhị phân (Binary Blobs)** | Có | PostgreSQL `BYTEA` Storage (`DatabaseStorageService`) | `DatabaseStorageService.java`, `V13__storage_blobs.sql` | Ứng dụng |
| **Cơ sở Dữ liệu Vector & Tìm kiếm** | Có | PostgreSQL `pgvector` (Vector 768d + Khoảng cách Cosine + HNSW) | `V10__document_chunks_and_vectors.sql`, `SemanticRetrievalStrategy.java` | Nâng cao |
| **Tìm kiếm Toàn văn (Full-Text Search)** | Có | PostgreSQL FTS (`tsvector`, `tsquery`, Chỉ mục GIN, Từ điển `simple`) | `V12__lexical_fts_index.sql`, `KeywordRetrievalStrategy.java` | Nâng cao |
| **Thuật toán Tìm kiếm Lai & Hợp nhất** | Có | Reciprocal Rank Fusion (RRF với hằng số $k=60$) | `HybridRetrievalStrategy.java` | Nâng cao |
| **Tích hợp Hạ tầng Trí tuệ Nhân tạo** | Có | Google Gemini Cloud (`gemini-3.5-flash-lite`, `gemini-embedding-001`) | `GeminiEmbeddingProvider.java`, `GeminiLanguageModelClient.java` | Ứng dụng |
| **Kiểm thử Phần mềm Tự động** | Có | JUnit 5, Mockito, SpringBootTest | `src/backend/src/test/java/...` | Cốt lõi |
| **Đóng gói & Triển khai Cloud** | Có | Vercel (Frontend SPA) + Render (Spring Boot Docker Container) | `render.yaml`, `src/backend/Dockerfile` | Thực hành |

---

## 2. Phân tích 16 Chủ đề Trọng tâm theo Công thức 7 Bước

Mỗi chủ đề bên dưới được phân tích nhất quán theo 7 câu hỏi chuẩn mực:
1. **Khái niệm là gì? (What)**
2. **Vì sao hệ thống cần nó? (Why)**
3. **KnowledgeOS áp dụng như thế nào? (How)**
4. **Vị trí cụ thể trong mã nguồn (Source Path)**
5. **Luồng xử lý Request thực tế (Request Flow)**
6. **Lý do lựa chọn thiết kế này? (Design Rationale)**
7. **Đánh đổi kỹ thuật & Giới hạn (Trade-offs & Limitations)**

---

### Chủ đề 1: Giao thức HTTP, Internet & Mô hình Client-Server

- **1. Là gì?**: Mô hình kiến trúc phân tách giữa máy khách (trình duyệt web) và máy chủ (Spring Boot API), giao tiếp qua giao thức truyền tải siêu văn bản (HTTP/HTTPS) có bảo mật TLS.
- **2. Vì sao cần?**: Tách rời hoàn toàn giao diện hiển thị khỏi logic lưu trữ và xử lý tính toán, cho phép người dùng truy cập từ mọi thiết bị mà không cần cài đặt môi trường máy chủ.
- **3. Cách KnowledgeOS áp dụng**: React SPA gửi các Request JSON/Multipart qua HTTPS đến Render Backend, xử lý tiêu đề CORS và nhận về mã trạng thái HTTP chuẩn (200, 201, 202, 204, 400, 401, 403, 404, 500).
- **4. Vị trí mã nguồn**: `src/frontend/src/api/client.ts`, `src/backend/src/main/java/com/groupsync/backend/config/SecurityConfig.java`.
- **5. Luồng xử lý Request**: `Trình duyệt -> Axios Interceptor -> HTTPS Request -> Reverse Proxy Render -> Spring TestDispatcherServlet -> Controller`.
- **6. Lý do thiết kế**: Đơn giản, chuẩn hóa công nghiệp, tương thích với mọi nền tảng trình duyệt.
- **7. Đánh đổi**: Giao thức không trạng thái (stateless) đòi hỏi phải có cơ chế quản lý phiên (Session Cookie) kèm theo.

---

### Chủ đề 2: Ngôn ngữ Java Hiện đại (Java 21)

- **1. Là gì?**: Phiên bản hỗ trợ dài hạn (LTS) của ngôn ngữ Java với nhiều cải tiến mạnh mẽ về cú pháp: Record classes, Sealed interfaces, Pattern matching, Text blocks và Sequence Collections.
- **2. Vì sao cần?**: Giúp mã nguồn ngắn gọn, an toàn kiểu dữ liệu (Type-safety), loại bỏ mã mẫu thừa (boilerplate code) và tối ưu hóa hiệu năng bộ nhớ.
- **3. Cách KnowledgeOS áp dụng**: Sử dụng Java 21 Records cho các DTO bất biến (`LoginRequest`, `ChatRequest`, `ResourceResponse`), sử dụng Streams API để xử lý mảng embedding và chuỗi vector.
- **4. Vị trí mã nguồn**: `src/backend/src/main/java/com/groupsync/backend/knowledge/dto/`.
- **5. Luồng xử lý**: Request JSON được Jackson ánh xạ trực tiếp thành Java 21 Record với các thuộc tính `final` bất biến và xác thực Bean Validation.
- **6. Lý do thiết kế**: Bảo vệ tính toàn vẹn dữ liệu trong suốt luồng xử lý của luồng (thread).
- **7. Đánh đổi**: Yêu cầu môi trường chạy Java 21 trở lên (OpenJDK 21+).

---

### Chủ đề 3: Công cụ Quản lý Dự án & Đóng gói (Apache Maven)

- **1. Là gì?**: Công cụ tự động hóa build và quản lý thư viện phụ thuộc phổ biến nhất trong hệ sinh thái Java.
- **2. Vì sao cần?**: Quản lý tập trung các thư viện ngoài (Spring Boot, PDFBox, POI, pgvector), đảm bảo khả năng tái tạo bản build đồng nhất trên mọi máy tính qua Maven Wrapper (`mvnw`).
- **3. Cách KnowledgeOS áp dụng**: `pom.xml` khai báo rõ ràng các phụ thuộc, plugin kiểm thử Surefire và cấu hình đóng gói JAR thực thi độc lập.
- **4. Vị trí mã nguồn**: `src/backend/pom.xml`, `src/backend/mvnw`, `src/backend/mvnw.cmd`.
- **5. Luồng xử lý**: `./mvnw test` $\to$ `./mvnw package` $\to$ Sinh tệp `groupsync-backend.jar`.
- **6. Lý do thiết kế**: Maven Wrapper giúp sinh viên hoặc giám khảo chạy dự án ngay lập tức mà không cần cài đặt Maven toàn cục.
- **7. Đánh đổi**: Tệp `pom.xml` dài hơn Gradle nhưng có tính ổn định cao và cú pháp XML tường minh.

---

### Chủ đề 4: Cơ sở Dữ liệu Quan hệ & Toàn vẹn ACID (PostgreSQL 17)

- **1. Là gì?**: Hệ quản trị cơ sở dữ liệu quan hệ mã nguồn mở mạnh mẽ, hỗ trợ đầy đủ các thuộc tính ACID (Nguyên tử, Nhất quán, Cô lập, Bền vững).
- **2. Vì sao cần?**: Đảm bảo dữ liệu người dùng, tài liệu, ghi chú và phiên trò chuyện không bị mất mát hay xung đột khi có nhiều thao tác đồng thời.
- **3. Cách KnowledgeOS áp dụng**: Tổ chức 13 bảng quan hệ với các ràng buộc khóa chính (`PRIMARY KEY`), khóa ngoại (`FOREIGN KEY`), và chỉ mục tối ưu hóa truy vấn.
- **4. Vị trí mã nguồn**: `src/backend/src/main/resources/db/migration/`.
- **5. Luồng xử lý**: Mọi thao tác ghi dữ liệu (ví dụ nạp tài liệu) được bao bọc trong giao dịch `@Transactional`, tự động rollback nếu gặp sự cố.
- **6. Lý do thiết kế**: PostgreSQL là cơ sở dữ liệu duy nhất hỗ trợ hoàn hảo cả dữ liệu quan hệ, dữ liệu vector (`pgvector`) và tìm kiếm văn bản toàn diện (FTS).
- **7. Đánh đổi**: Yêu cầu cấu hình kết nối JDBC và tài nguyên RAM lớn hơn SQLite.

---

### Chủ đề 5: Quản lý Di chuyển Schema Cơ sở Dữ liệu (Flyway)

- **1. Là gì?**: Công cụ kiểm soát phiên bản cơ sở dữ liệu bằng các tệp mã lệnh SQL có đánh số tuần tự.
- **2. Vì sao cần?**: Ngăn chặn tình trạng sai lệch cấu trúc bảng giữa máy tính lập trình viên và máy chủ Cloud Production.
- **3. Cách KnowledgeOS áp dụng**: Quản lý 13 bản di chuyển cấu trúc từ `V1` đến `V13__storage_blobs.sql`. Khi backend khởi động, Flyway tự động kiểm tra bảng `flyway_schema_history` và áp dụng các bản cập nhật mới nhất.
- **4. Vị trí mã nguồn**: `src/backend/src/main/resources/db/migration/V1__init_schema.sql` $\to$ `V13__storage_blobs.sql`.
- **5. Luồng xử lý**: `Spring Boot Bootstrapping -> Flyway Validation -> Execute Unapplied SQL -> DB Ready`.
- **6. Lý do thiết kế**: Tránh việc sửa database thủ công bằng tay (Manual DDL), đảm bảo tính tái lập 100%.
- **7. Đánh đổi**: Các migration đã chạy vào production tuyệt đối không được phép chỉnh sửa nội dung cũ.

---

### Chủ đề 6: Ánh xạ Quan hệ Thực thể & JPA (Spring Data JPA)

- **1. Là gì?**: Tầng trừu tượng hóa truy cập dữ liệu, ánh xạ các bảng PostgreSQL thành các lớp Java Entity thông qua Hibernate.
- **2. Vì sao cần?**: Giúp lập trình viên thao tác với cơ sở dữ liệu qua các đối tượng Java mà không cần viết các câu lệnh SQL lặp đi lặp lại.
- **3. Cách KnowledgeOS áp dụng**: Các Repository kế thừa `JpaRepository<Resource, Long>` để cung cấp sẵn các hàm CRUD chuẩn và định nghĩa các truy vấn tùy biến an toàn.
- **4. Vị trí mã nguồn**: `src/backend/src/main/java/com/groupsync/backend/knowledge/repository/`.
- **5. Luồng xử lý**: `Service -> ResourceRepository.findByIdAndOwnerId() -> Hibernate generate SQL -> JDBC -> PostgreSQL`.
- **6. Lý do thiết kế**: Tăng tốc độ phát triển và giảm thiểu tối đa các lỗi sai cú pháp SQL.
- **7. Đánh đổi**: Cần chú ý vấn đề truy vấn N+1 khi sử dụng các quan hệ lười nạp (`FetchType.LAZY`).

---

### Chủ đề 7: Khung Ứng dụng Backend Hiện đại (Spring Boot 4)

- **1. Là gì?**: Khung ứng dụng Java phổ biến nhất thế giới, cung cấp cơ chế Đảo ngược Điều khiển (IoC), Tiêm Phụ thuộc (Dependency Injection) và Tự động Cấu hình (Auto-configuration).
- **2. Vì sao cần?**: Cung cấp kiến trúc phần mềm chuẩn mực, tách biệt rõ ràng các thành phần nghiệp vụ và kiểm soát vòng đời của các Bean.
- **3. Cách KnowledgeOS áp dụng**: Áp dụng Constructor Injection trên toàn bộ Service/Controller; sử dụng `@Service`, `@RestController`, `@Repository`, `@Transactional`.
- **4. Vị trí mã nguồn**: Toàn bộ thư mục `src/backend/src/main/java/com/groupsync/backend/`.
- **5. Luồng xử lý**: Spring Context khởi tạo các Singleton Bean khi ứng dụng khởi động và tự động tiêm các phụ thuộc phù hợp.
- **6. Lý do thiết kế**: Tiêu chuẩn công nghiệp hàng đầu cho lập trình hướng đối tượng và công nghệ phần mềm.
- **7. Đánh đổi**: Thời gian khởi động ban đầu lâu hơn các framework siêu nhẹ (như Go hay Node.js).

---

### Chủ đề 8: Thiết kế Giao diện Lập trình Ứng dụng Chuẩn (REST API)

- **1. Là gì?**: Phong cách kiến trúc mạng định nghĩa các nguyên tắc truyền thông qua các động từ HTTP chuẩn (`GET`, `POST`, `PUT`, `DELETE`).
- **2. Vì sao cần?**: Cung cấp giao diện giao tiếp đồng nhất, rõ ràng và dễ dàng tích hợp giữa Frontend và Backend.
- **3. Cách KnowledgeOS áp dụng**: Thiết kế danh mục endpoint rõ ràng (`/api/knowledge/resources`, `/api/knowledge/chat`, `/api/auth`), phân định rõ ràng giữa truy vấn dữ liệu (`GET`) và biến đổi trạng thái (`POST/PUT/DELETE`).
- **4. Vị trí mã nguồn**: `src/backend/src/main/java/com/groupsync/backend/knowledge/controller/`.
- **5. Luồng xử lý**: Request HTTP $\to$ DispatcherServlet $\to$ ResourceController $\to$ Response Entity với HTTP Status Code chuẩn.
- **6. Lý do thiết kế**: Giúp hệ thống dễ dàng mở rộng thêm ứng dụng Mobile hoặc tích hợp bên thứ ba.
- **7. Đánh đổi**: Không hỗ trợ thời gian thực hai chiều như WebSocket (tuy nhiên hoàn toàn đủ đáp ứng cho nghiệp vụ RAG).

---

### Chủ đề 9: Xác thực & Bảo mật (Spring Security + BCrypt)

- **1. Là gì?**: Phân hệ bảo mật kiểm soát quyền truy cập, xác thực danh tính người dùng và bảo vệ dữ liệu nhạy cảm.
- **2. Vì sao cần?**: Ngăn chặn truy cập trái phép, bảo vệ thông tin mật khẩu và cách ly tuyệt đối dữ liệu giữa các người dùng.
- **3. Cách KnowledgeOS áp dụng**: Mã hóa mật khẩu một chiều bằng BCrypt với Salt ngẫu nhiên; quản lý phiên qua Cookie `JSESSIONID` an toàn (`HttpOnly`, `SameSite=Lax`); áp dụng lọc bảo vệ trên mọi API private.
- **4. Vị trí mã nguồn**: `src/backend/src/main/java/com/groupsync/backend/config/SecurityConfig.java`, `AuthService.java`.
- **5. Luồng xử lý**: Client gửi `POST /api/auth/login` $\to$ BCrypt kiểm tra mật khẩu $\to$ Cấp Cookie `JSESSIONID` $\to$ Các Request tiếp theo tự động mang Cookie để xác thực danh tính.
- **6. Lý do thiết kế**: Bảo mật phiên theo Cookie an toàn hơn lưu Token trong `localStorage` (chống lộ lọt qua XSS).
- **7. Đánh đổi**: Cần cấu hình CORS và SameSite cẩn thận khi triển khai Frontend và Backend trên 2 domain khác nhau.

---

### Chủ đề 10: Lưu trữ Tệp Nhị phân Bền vững (PostgreSQL BYTEA Blobs)

- **1. Là gì?**: Kỹ thuật lưu trữ mảng byte dữ liệu nhị phân (Binary Large Objects) trực tiếp vào bảng cơ sở dữ liệu quan hệ.
- **2. Vì sao cần?**: Trong môi trường Cloud Serverless (như Render Container), hệ thống tệp cục bộ (Local Disk) sẽ bị xóa trắng mỗi khi container khởi động lại. Lưu trữ tệp vào database giúp tệp tồn tại vĩnh viễn mà không tốn chi phí thuê thêm dịch vụ lưu trữ ngoài (như AWS S3).
- **3. Cách KnowledgeOS áp dụng**: Lớp `DatabaseStorageService` lưu tệp nhị phân vào bảng `storage_blobs` kèm mã băm SHA-256 để chống trùng lặp.
- **4. Vị trí mã nguồn**: `src/backend/src/main/java/com/groupsync/backend/knowledge/storage/DatabaseStorageService.java`, `V13__storage_blobs.sql`.
- **5. Luồng xử lý**: `Upload File -> Read InputStream -> Save BYTEA to storage_blobs -> Return storageKey`.
- **6. Lý do thiết kế**: Đơn giản hóa kiến trúc, giúp đồ án sinh viên hoàn toàn tự chứa (Self-contained) trong 1 cơ sở dữ liệu duy nhất.
- **7. Đánh đổi**: Dung lượng cơ sở dữ liệu sẽ tăng lên khi lưu trữ các tệp có kích thước hàng trăm Megabyte.

---

### Chủ đề 11: Cơ sở Dữ liệu Vector & Khoảng cách Cosine (`pgvector`)

- **1. Là gì?**: Tiện ích mở rộng cho phép PostgreSQL lưu trữ các vector đa chiều và thực hiện phép toán tìm kiếm láng giềng gần nhất (Nearest Neighbor Search).
- **2. Vì sao cần?**: Giúp hệ thống hiểu được ý nghĩa ngữ nghĩa của câu hỏi người dùng, tìm ra các đoạn văn có nội dung tương đồng ngay cả khi không trùng khớp từ ngữ.
- **3. Cách KnowledgeOS áp dụng**: Lưu vector embedding 768 chiều vào cột `embedding vector(768)` trên bảng `document_chunks` và tạo chỉ mục tăng tốc HNSW (`vector_cosine_ops`).
- **4. Vị trí mã nguồn**: `src/backend/src/main/resources/db/migration/V10__document_chunks_and_vectors.sql`, `SemanticRetrievalStrategy.java`.
- **5. Luồng xử lý**: `Câu hỏi -> Gemini Embedding (768d) -> SQL WHERE owner_id = :id ORDER BY embedding <=> :queryVector LIMIT 10`.
- **6. Lý do thiết kế**: Tích hợp trực tiếp vào PostgreSQL giúp tránh việc phải cài đặt thêm một Vector Database chuyên biệt (như Pinecone hay Milvus).
- **7. Đánh đổi**: Chỉ mục HNSW tiêu tốn thêm một phần bộ nhớ RAM để duy trì đồ thị liên kết vector.

---

### Chủ đề 12: Tìm kiếm Toàn văn (PostgreSQL Full-Text Search)

- **1. Là gì?**: Tính năng tìm kiếm văn bản dựa trên phân tích từ vựng (Tokenization), chuẩn hóa từ gốc (Stemming) và lập chỉ mục đảo GIN (Generalized Inverted Index).
- **2. Vì sao cần?**: Tìm kiếm vector thường gặp khó khăn với các mã định danh kỹ thuật chính xác (như `CVE-2026-8819`, `RFC-9421`, tên hàm `DatabaseStorageService`). Tìm kiếm FTS giải quyết triệt để bài toán này.
- **3. Cách KnowledgeOS áp dụng**: Tạo cột `tsv tsvector GENERATED ALWAYS AS (to_tsvector('simple', content))` kết hợp chỉ mục GIN trên bảng `document_chunks`.
- **4. Vị trí mã nguồn**: `src/backend/src/main/resources/db/migration/V12__lexical_fts_index.sql`, `KeywordRetrievalStrategy.java`.
- **5. Luồng xử lý**: `Câu hỏi -> to_tsquery('simple', :tokens) -> SQL WHERE tsv @@ query LIMIT 10`.
- **6. Lý do thiết kế**: Từ điển `simple` giữ nguyên từng ký tự đặc biệt và chữ số, hoàn hảo cho tài liệu kỹ thuật và tiếng Việt.
- **7. Đánh đổi**: Không hiểu được các từ đồng nghĩa hoặc câu hỏi diễn giải gián tiếp (cần kết hợp với nhánh Semantic).

---

### Chủ đề 13: Thuật toán Tìm kiếm Lai & Hợp nhất Xếp hạng (RRF)

- **1. Là gì?**: Thuật toán kết hợp các danh sách kết quả tìm kiếm độc lập dựa trên vị trí thứ bậc tương đối của từng phần tử.
- **2. Vì sao cần?**: Điểm số khoảng cách Cosine (0.0 $\to$ 1.0) và điểm xếp hạng từ khóa `ts_rank` có thang đo hoàn toàn khác nhau, không thể cộng trực tiếp. RRF là giải pháp chuẩn công nghiệp để chuẩn hóa và hợp nhất hai nhánh tìm kiếm.
- **3. Cách KnowledgeOS áp dụng**: Lớp `HybridRetrievalStrategy` tính toán điểm số cho từng đoạn văn: $\text{Score}_{\text{RRF}}(d) = \sum \frac{1}{60 + \text{rank}(d)}$.
- **4. Vị trí mã nguồn**: `src/backend/src/main/java/com/groupsync/backend/knowledge/rag/HybridRetrievalStrategy.java`.
- **5. Luồng xử lý**: `Kích hoạt song song Semantic + Lexical -> Lấy Top 10 mỗi nhánh -> Tính toán RRF Score -> Trả về Top 5 đoạn văn cao nhất`.
- **6. Lý do thiết kế**: Chi phí tính toán cực nhẹ, không mất phí tài nguyên và có độ chính xác cao vượt trội.
- **7. Đánh đổi**: Hằng số $k=60$ là hằng số tiêu chuẩn thực nghiệm, cần giữ cố định để đảm bảo tính ổn định.

---

### Chủ đề 14: Tích hợp Dịch vụ Trí tuệ Nhân tạo Cloud (Google Gemini)

- **1. Là gì?**: Dịch vụ đám mây cung cấp các mô hình nền tảng ngôn ngữ và nhúng vector thông qua giao tiếp API RESTful.
- **2. Vì sao cần?**: Cung cấp khả năng hiểu ngôn ngữ tự nhiên và tạo sinh văn bản tiếng Việt chuẩn mực mà không đòi hỏi máy chủ phải trang bị GPU đắt đỏ.
- **3. Cách KnowledgeOS áp dụng**: Sử dụng `gemini-embedding-001` (vector 768 chiều) để tạo vector nhúng và `gemini-3.5-flash-lite` để tổng hợp câu trả lời dựa trên tài liệu bằng chứng.
- **4. Vị trí mã nguồn**: `src/backend/src/main/java/com/groupsync/backend/knowledge/rag/GeminiEmbeddingProvider.java`, `GeminiLanguageModelClient.java`.
- **5. Luồng xử lý**: `Prompt Builder -> HTTP POST đến Google AI Endpoint -> Nhận chuỗi phản hồi -> Phân tích và gắn trích dẫn`.
- **6. Lý do thiết kế**: Mô hình `gemini-3.5-flash-lite` có tốc độ phản hồi cực nhanh, hỗ trợ tiếng Việt xuất sắc và tối ưu chi phí.
- **7. Đánh đổi**: Phụ thuộc vào kết nối mạng Internet và khóa API Key hợp lệ.

---

### Chủ đề 15: Kiến trúc Kiểm thử Tự động (Testing & Verification)

- **1. Là gì?**: Bộ công cụ viết và chạy các bài kiểm tra tự động nhằm đảm bảo mã nguồn hoạt động chính xác và không phát sinh lỗi hồi quy (Regression).
- **2. Vì sao cần?**: Tự động hóa kiểm tra tính đúng đắn của logic thuật toán (cắt đoạn, RRF, cách ly phạm vi, xóa khóa ngoại) mà không cần thao tác tay.
- **3. Cách KnowledgeOS áp dụng**: Xây dựng 57 bài kiểm thử đơn vị và tích hợp bằng JUnit 5 & Mockito; kiểm tra 34 ca RAG Benchmark qua `RagEvaluationDatasetTest.java`.
- **4. Vị trí mã nguồn**: `src/backend/src/test/java/com/groupsync/backend/`.
- **5. Luồng xử lý**: `./mvnw test` kích hoạt toàn bộ 57 bài test $\to$ Báo cáo `BUILD SUCCESS` (0 failures, 0 errors).
- **6. Lý do thiết kế**: Đảm bảo sinh viên có thể chứng minh tính đúng đắn của phần mềm một cách khoa học trước hội đồng bảo vệ.
- **7. Đánh đổi**: Cần đầu tư thời gian viết và bảo trì mã kiểm thử song song với mã nguồn nghiệp vụ.

---

### Chủ đề 16: Đóng gói Container & Triển khai Đám mây (Docker & Cloud)

- **1. Là gì?**: Kỹ thuật đóng gói toàn bộ mã nguồn, thư viện và môi trường chạy vào một hình ảnh Container độc lập và phát hành lên máy chủ Cloud.
- **2. Vì sao cần?**: Loại bỏ hoàn toàn lỗi *"chạy được trên máy em nhưng không chạy được trên máy chủ"* (It works on my machine).
- **3. Cách KnowledgeOS áp dụng**: Tệp `src/backend/Dockerfile` sử dụng kỹ thuật Multi-stage build để biên dịch JAR trên môi trường Maven và chạy trên nền OpenJDK 21 tối giản, triển khai tự động lên Render; Frontend triển khai tự động lên Vercel CDN.
- **4. Vị trí mã nguồn**: `src/backend/Dockerfile`, `render.yaml`, `src/frontend/vercel.json`.
- **5. Luồng xử lý**: `Git Push main -> Render Webhook -> Docker Build -> Container Deploy -> Healthcheck /api/health -> LIVE`.
- **6. Lý do thiết kế**: Cho phép bất kỳ ai cũng có thể trải nghiệm trực tiếp sản phẩm qua đường dẫn public mà không cần cài đặt phức tạp.
- **7. Đánh đổi**: Gói miễn phí của Render có thể chuyển sang trạng thái ngủ (Sleep) sau một thời gian không có truy cập và cần khoảng 30–50 giây để khởi động lại.
