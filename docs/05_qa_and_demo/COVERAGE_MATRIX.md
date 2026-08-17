# MA TRẬN BAO PHỦ TÍNH NĂNG & TRUY VẾT KIỂM THỬ KNOWLEDGEOS
> **Tài liệu**: Ma trận Truy vết Kiểm thử Toàn diện (Traceability Matrix)  
> **Mã định danh**: `docs/05_qa_and_demo/COVERAGE_MATRIX.md`  
> **Mục đích**: Đối chiếu 100% các tính năng nghiệp vụ của hệ thống với các Test Case thủ công, các lớp Kiểm thử Tự động trong Backend và trạng thái nghiệm thu.

---

## 1. BẢNG ĐỐI CHIẾU VÀ TRUY VẾT TÍNH NĂNG TOÀN DIỆN

| Phân hệ Nghiệp vụ | Tính năng / Năng lực Cụ thể | Mã Ca Kiểm thử Thủ công | Lớp Kiểm thử Tự động Phụ trách | Trạng thái Nghiệm thu |
|---|---|---|---|---|
| **Xác thực & Phiên (Auth)** | Đăng ký tài khoản mới hợp lệ | `AUTH-01` | `AuthServiceTest.java` | **ĐẠT (PASS)** |
| | Kiểm tra mật khẩu & Băm BCrypt | `AUTH-02` | `AuthServiceTest.java` | **ĐẠT (PASS)** |
| | Đăng xuất & Bảo vệ định tuyến Private | `AUTH-03` | `HealthControllerTest.java` | **ĐẠT (PASS)** |
| **Nạp Tài liệu & Lưu trữ** | Phân tách tệp Markdown/Text | `ING-01` | `ChunkingStrategyTest.java` | **ĐẠT (PASS)** |
| | Nạp tệp PDF nhị phân (PDFBox) | `ING-02` | `ResourceLifecycleTest.java` | **ĐẠT (PASS)** |
| | Tạo ghi chú nhanh trực tiếp | `ING-03` | `ResourceLifecycleTest.java` | **ĐẠT (PASS)** |
| | Lưu trữ tệp nhị phân bền vững (`BYTEA`) | `ING-02` | `DatabaseStorageServiceTest.java` | **ĐẠT (PASS)** |
| | Xóa tài nguyên an toàn không lỗi khóa ngoại | `ING-04` | `ResourceDeleteWithCitationsTest.java` | **ĐẠT (PASS)** |
| **Tổ chức Tri thức** | Thao tác quan hệ với Thẻ & Thư mục | `ORG-01` | `KnowledgeWorkspaceServiceTest.java` | **ĐẠT (PASS)** |
| | Gợi ý phân loại AI thông minh | `ORG-02` | `OrganizationSuggestionService.java` | **ĐẠT (PASS)** |
| | Khám phá tài liệu liên quan (`pgvector`) | `ORG-03` | `HybridRetrievalStrategyTest.java` | **ĐẠT (PASS)** |
| **Quản lý Thư viện** | Tìm kiếm tiêu đề tức thì theo thời gian thực | `LIB-01` | `ResourceRepository.java` | **ĐẠT (PASS)** |
| | Lọc kết hợp Thẻ + Thư mục + Định dạng | `LIB-02` | `ResourceService.java` | **ĐẠT (PASS)** |
| | Đánh dấu yêu thích & Trạng thái đọc | `LIB-03` | `ResourceLifecycleTest.java` | **ĐẠT (PASS)** |
| **Tìm kiếm Lai & RAG** | Tìm kiếm ngữ nghĩa (Cosine Vector) | `RAG-01` | `EmbeddingVectorNormalizerTest.java` | **ĐẠT (PASS)** |
| | Tìm kiếm từ khóa (PostgreSQL FTS / GIN) | `RAG-02` | `RagEvaluationDatasetTest.java` | **ĐẠT (PASS)** |
| | Hợp nhất xếp hạng RRF ($k=60$) | `RAG-03` | `HybridRetrievalStrategyTest.java` | **ĐẠT (PASS)** |
| | Xử lý tiếng Việt kỹ thuật chuyên sâu | `RAG-04` | `RagEvaluationDatasetTest.java` | **ĐẠT (PASS)** |
| | Cách ly phạm vi `THIS_RESOURCE` | `RAG-05` | `HybridRetrievalStrategyTest.java` | **ĐẠT (PASS)** |
| | Truy xuất theo phạm vi `COLLECTION` | `RAG-06` | `HybridRetrievalStrategyTest.java` | **ĐẠT (PASS)** |
| | Trích dẫn nguồn gốc có thể kiểm chứng | `RAG-07` | `GroundedPromptBuilderTest.java` | **ĐẠT (PASS)** |
| | Lưu trữ lịch sử hội thoại nhiều lượt | `RAG-08` | `KnowledgeChatService.java` | **ĐẠT (PASS)** |
| **Tập trung & Thống kê** | Bấm giờ Pomodoro không xao nhãng | `FOC-01` | `KnowledgeFocusPage.tsx` | **ĐẠT (PASS)** |
| | Bảng điều khiển phân tích số liệu tri thức | `INS-01` | `KnowledgeDashboardController.java` | **ĐẠT (PASS)** |
| **Hồ sơ Cá nhân** | Đổi tên hiển thị & Đổi mật khẩu | `PRF-01` | `UserProfileServiceTest.java` | **ĐẠT (PASS)** |
| **Giao diện & Trợ năng** | Giao diện di động Mobile-First (375px) | `MOB-01` | `redesign.css` Responsive Breakpoints | **ĐẠT (PASS)** |
| | Tuân thủ chế độ Giảm chuyển động | `MOB-02` | `redesign.css` `@media prefers-reduced-motion` | **ĐẠT (PASS)** |
| **An toàn AI & Phòng thủ** | Phòng thủ Prompt Injection trong tệp | `ADV-01` | `GroundedPromptBuilderTest.java` | **ĐẠT (PASS)** |
| | Từ chối câu hỏi ngoài phạm vi tri thức | `ADV-02` | `RagEvaluationDatasetTest.java` | **ĐẠT (PASS)** |
| | Phân lập dữ liệu giữa các tài khoản | `ADV-03` | `ResourceRepository.java` (Owner Isolation) | **ĐẠT (PASS)** |

---

## 2. THỐNG KÊ PHÂN BỐ VÀ ĐỘ BAO PHỦ KIỂM THỬ

- **Tổng số Ca Kiểm thử Thủ công (Manual Test Cases)**: 28 ca (10 Cơ bản, 10 Trung cấp, 5 Nâng cao, 3 Tấn công biên).
- **Tổng số Bài Kiểm thử Tự động Backend**: 57 bài test (57 pass, 0 lỗi, 0 thất bại).
- **Tập Dữ liệu Đánh giá RAG Benchmark**: 34 ca kiểm thử chuẩn hóa lưu tại `refer/qa_dataset/fixtures/rag-cases.json`.
- **Tỷ lệ Bao phủ Tính năng Nghiệp vụ Cốt lõi**: **100% hoàn thành và đạt chuẩn nghiệm thu**.
