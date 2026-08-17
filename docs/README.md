# MỤC LỤC & HỆ THỐNG TÀI LIỆU DỰ ÁN KNOWLEDGEOS

> **Hệ thống Quản lý Tri thức Cá nhân Thông minh kết hợp Tìm kiếm Lai (Hybrid RAG) và Cơ sở Dữ liệu Quan hệ**  
> *Đồ án Môn học Lập trình Hướng đối tượng (OOP) & Công nghệ Phần mềm — Trình độ Đại học Năm 3*

Chào mừng thầy cô, hội đồng chấm thi, nhà tuyển dụng và các bạn sinh viên đến với bộ tài liệu chính thức của dự án **KnowledgeOS**. Toàn bộ tài liệu được phân chia theo 6 danh mục chuyên biệt, có định danh rõ ràng giúp người đọc dễ dàng định vị tài liệu cần thiết cho từng mục đích (học tập, bảo vệ, tra cứu kỹ thuật, hoặc chạy thử nghiệm).

---

## 🧭 BẢN ĐỒ ĐỊNH VỊ TÀI LIỆU NHANH

| Danh mục | Tên tệp | Tiêu đề Tài liệu | Mục đích sử dụng | Định dạng |
|---|---|---|---|---|
| **01. Hướng dẫn** | [`KNOWLEDGEOS_GUIDE.md`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/01_guides/KNOWLEDGEOS_GUIDE.md) | **Sách Hướng dẫn Sản phẩm & Sổ tay Người dùng** | Hướng dẫn người dùng thao tác 39 bước từ A-Z trên giao diện web | Markdown |
| **01. Hướng dẫn** | [`KNOWLEDGEOS_GUIDE.pdf`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/01_guides/KNOWLEDGEOS_GUIDE.pdf) | **Sách Hướng dẫn Sản phẩm (Bản in PDF)** | Bản in PDF xuất bản chuẩn học thuật, định dạng Times New Roman | PDF |
| **02. Kỹ thuật** | [`TECHNICAL_REFERENCE.md`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/02_technical_reference/TECHNICAL_REFERENCE.md) | **Đặc tả Kỹ thuật Chuyên sâu & Catalog API** | Giải thích cấu trúc mã nguồn, mẫu thiết kế OOP, REST API, Database, RAG | Markdown |
| **03. Lộ trình** | [`BACKEND_ROADMAP_MAPPING.md`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/03_curriculum_mapping/BACKEND_ROADMAP_MAPPING.md) | **Đối chiếu Lộ trình Kỹ sư Backend (roadmap.sh)** | Phân tích 16 chủ đề Backend chuẩn theo công thức 7 bước ứng dụng | Markdown |
| **04. Vấn đáp** | [`COURSE_DEFENSE_GUIDE.md`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/04_course_defense/COURSE_DEFENSE_GUIDE.md) | **Cẩm nang 32 Câu hỏi Vấn đáp Bảo vệ Đồ án** | 32 câu hỏi và câu trả lời súc tích (30–90 giây) phục vụ bảo vệ miệng | Markdown |
| **05. Kiểm thử** | [`TEST_CASES.md`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/05_qa_and_demo/TEST_CASES.md) | **Bộ 28 Ca Kiểm thử Thủ công (Test Cards)** | 28 kịch bản kiểm thử từng bước (Cơ bản, Nâng cao, Tấn công biên) | Markdown |
| **05. Kiểm thử** | [`COVERAGE_MATRIX.md`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/05_qa_and_demo/COVERAGE_MATRIX.md) | **Ma trận Bao phủ Tính năng & Truy vết** | Bảng đối chiếu 100% tính năng với mã nguồn và kịch bản test | Markdown |
| **05. Kiểm thử** | [`LIVE_DEMO_SCRIPT.md`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/05_qa_and_demo/LIVE_DEMO_SCRIPT.md) | **Kịch bản Thuyết trình Demo Trực tiếp 12 Phút** | Lời thoại thuyết trình và thao tác click chuột trực tiếp trước hội đồng | Markdown |
| **05. Kiểm thử** | [`fixtures/`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/05_qa_and_demo/fixtures) | **Thư mục Dữ liệu Mẫu (11 Fixtures)** | 11 tệp tài liệu mẫu Markdown/Text phục vụ nạp và thử nghiệm | Tệp mẫu |
| **06. Lịch sử** | [`06_historical_reports/`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/06_historical_reports) | **Lưu trữ Báo cáo Các Giai đoạn Trước** | Báo cáo tích hợp, phân tích di chuyển kiến trúc qua các vòng lặp | Thư mục |

---

## 📂 CHI TIẾT NỘI DUNG TỪNG DANH MỤC

### 1. Thư mục [`01_guides/`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/01_guides) — Hướng Dẫn Sử Dụng Sản Phẩm
- **Mục đích**: Cung cấp tài liệu đào tạo người dùng cuối (sinh viên, giảng viên, nghiên cứu sinh) cách sử dụng toàn bộ tính năng của KnowledgeOS.
- **Tài liệu chính**:
  1. [`KNOWLEDGEOS_GUIDE.md`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/01_guides/KNOWLEDGEOS_GUIDE.md): Bản thảo chi tiết gồm 2 phần:
     - **Phần I**: Giới thiệu hệ thống, vòng đời tri thức 6 bước và sơ đồ kiến trúc Modular Monolith.
     - **Phần II**: Hướng dẫn thực hành từng bước qua 39 quy trình: Đăng ký/Đăng nhập, Nạp tài liệu (PDF, DOCX, Markdown, Ghi chú), Không gian làm việc Reader/Note, Phân loại Thư mục (Collections) & Thẻ (Tags), Gợi ý thông minh (Smart Organization), Tìm kiếm đa tiêu chí, Hỏi đáp thông minh (RAG) qua 4 phạm vi truy xuất (`THIS_RESOURCE`, `SELECTED_RESOURCES`, `COLLECTION`, `LIBRARY`), Kiểm tra trích dẫn gốc, Chế độ tập trung Pomodoro, Thống kê tri thức Insights và Quản lý tài khoản cá nhân.
  2. [`KNOWLEDGEOS_GUIDE.pdf`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/01_guides/KNOWLEDGEOS_GUIDE.pdf): Bản in PDF xuất bản chuẩn học thuật, định dạng Times New Roman, canh lề chuẩn in ấn, hiển thị tiếng Việt trọn vẹn.

### 2. Thư mục [`02_technical_reference/`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/02_technical_reference) — Đặc Tả Kỹ Thuật Chuyên Sâu
- **Mục đích**: Tài liệu kiến trúc và kỹ thuật dành cho giảng viên hướng dẫn, lập trình viên backend/frontend và người duy trì hệ thống.
- **Tài liệu chính**:
  - [`TECHNICAL_REFERENCE.md`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/02_technical_reference/TECHNICAL_REFERENCE.md): Gồm 16 chương kỹ thuật chi tiết:
    1. Cấu trúc vật lý toàn dự án.
    2. Kiến trúc Frontend (React 19, TypeScript, Design Tokens).
    3. Kiến trúc Backend Modular Monolith (Spring Boot 4).
    4. Áp dụng Mẫu thiết kế Hướng đối tượng OOP (Strategy Pattern, Polymorphic Registry, DIP, Encapsulation).
    5. Danh mục toàn bộ API RESTful (Catalog chi tiết Request/Response DTO).
    6. Cơ sở dữ liệu quan hệ PostgreSQL & Sơ đồ Thực thể Liên kết (ER Diagram).
    7. Quản lý tiến hóa Database qua 13 bản Flyway Migration (V1–V13).
    8. Động cơ Tìm kiếm Lai (Hybrid RAG: Vector `pgvector` + Lexical FTS `tsvector`).
    9. Thuật toán Hợp nhất Xếp hạng Tương hỗ (Reciprocal Rank Fusion - RRF $k=60$).
    10. Bốn phạm vi truy xuất thông tin (Retrieval Scopes).
    11. Cơ chế Chống ảo giác (Grounding) và Trích dẫn nguồn gốc (Citations).
    12. Bảo mật, Quản lý Phiên (Session/Cookie) và Phòng thủ Prompt Injection.
    13. Trích vết luồng dữ liệu End-to-End từ UI đến Database.
    14. Chiến lược và kết quả Kiểm thử tự động.
    15. Từ điển Thuật ngữ Kỹ thuật.
    16. Lộ trình 8 cấp độ tự học và nghiên cứu mã nguồn.

### 3. Thư mục [`03_curriculum_mapping/`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/03_curriculum_mapping) — Cầu Nối Lộ Trình Backend
- **Mục đích**: Giúp sinh viên nắm vững cách áp dụng kiến thức lý thuyết từ [roadmap.sh/backend](https://roadmap.sh/backend) vào một dự án phần mềm thực tế.
- **Tài liệu chính**:
  - [`BACKEND_ROADMAP_MAPPING.md`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/03_curriculum_mapping/BACKEND_ROADMAP_MAPPING.md): Phân tích 16 chủ đề cốt lõi (Java 21, Spring Boot, RESTful API, PostgreSQL, HNSW Vector Index, FTS, Flyway, BCrypt Auth, Modular Architecture, v.v.) theo chuẩn 7 câu hỏi:
    1. Chủ đề là gì? (What)
    2. Vì sao dự án cần nó? (Why)
    3. Dự án áp dụng nó như thế nào? (How)
    4. Đường dẫn mã nguồn cụ thể (Source Path)
    5. Luồng xử lý một Request thực tế (Request Flow)
    6. Lý do thiết kế kiến trúc (Design Rationale)
    7. Đánh đổi kỹ thuật & Giải pháp thay thế (Trade-offs & Alternatives)

### 4. Thư mục [`04_course_defense/`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/04_course_defense) — Vấn Đáp Bảo Vệ Đồ Án
- **Mục đích**: Chuẩn bị cho sinh viên bước vào buổi bảo vệ đồ án miệng trước Hội đồng Giám khảo.
- **Tài liệu chính**:
  - [`COURSE_DEFENSE_GUIDE.md`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/04_course_defense/COURSE_DEFENSE_GUIDE.md): Tập hợp 32 câu hỏi vấn đáp trọng tâm được chia thành 10 nhóm chủ đề:
    - Nhóm 1: Tổng quan Kiến trúc & Phạm vi Đồ án.
    - Nhóm 2: Lập trình Hướng đối tượng OOP & Mẫu Thiết kế.
    - Nhóm 3: Khung ứng dụng Spring Boot & Luồng Dữ liệu.
    - Nhóm 4: Thiết kế Cơ sở Dữ liệu Quan hệ & Lưu trữ Nhị phân (BYTEA).
    - Nhóm 5: Động cơ Hybrid RAG & Vector Embeddings.
    - Nhóm 6: Thuật toán Hợp nhất RRF & Trích dẫn Nguồn.
    - Nhóm 7: Giao diện Người dùng Frontend & Khả năng Truy cập.
    - Nhóm 8: Bảo mật, Quản lý Phiên & Chống Tấn công Prompt Injection.
    - Nhóm 9: Đánh giá Chất lượng, Kiểm thử & Thống kê Hiệu năng.
    - Nhóm 10: Giới hạn của Phiên bản v1 & Định hướng Phát triển v2.
    *(Mỗi câu hỏi đều có hướng dẫn trả lời trực diện, súc tích trong 30–90 giây kèm dẫn chứng file mã nguồn)*.

### 5. Thư mục [`05_qa_and_demo/`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/05_qa_and_demo) — Kiểm Thử Thủ Công & Kịch Bản Demo
- **Mục đích**: Cung cấp công cụ và kịch bản thực tế để kiểm tra chất lượng phần mềm và trình diễn trực tiếp không gặp sự cố.
- **Tài liệu chính**:
  - [`TEST_CASES.md`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/05_qa_and_demo/TEST_CASES.md): 28 Test Card chi tiết được chuẩn hóa gồm Mục tiêu, Tiền điều kiện, Các bước thực hiện, Kết quả mong đợi và Kết quả thực tế.
  - [`COVERAGE_MATRIX.md`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/05_qa_and_demo/COVERAGE_MATRIX.md): Bảng ma trận đối chiếu 100% tính năng của hệ thống với các bài kiểm thử tự động và thủ công.
  - [`LIVE_DEMO_SCRIPT.md`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/05_qa_and_demo/LIVE_DEMO_SCRIPT.md): Kịch bản trình diễn trực tiếp 12 phút hoàn chỉnh từ khâu chuẩn bị, nạp dữ liệu, kiểm tra tìm kiếm lai, kiểm tra trích dẫn, thử nghiệm tấn công Prompt Injection và phương án dự phòng khi mất kết nối mạng.
  - [`fixtures/`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/05_qa_and_demo/fixtures): 11 tệp tài liệu mẫu tổng hợp (Markdown/Text) để người dùng tải lên và thử nghiệm ngay lập tức.

### 6. Thư mục [`06_historical_reports/`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/06_historical_reports) — Báo Cáo Lịch Sử & Nhật Ký Phát Triển
- **Mục đích**: Lưu trữ các báo cáo kiểm thử, ghi chú quyết định kiến trúc và biên bản bàn giao từ các giai đoạn phát triển ban đầu của dự án nhằm phục vụ tra cứu lịch sử khi cần thiết.

---

## 🎯 HƯỚNG DẪN BẮT ĐẦU ĐỌC THEO VAI TRÒ

- **Dành cho Giảng viên / Người chấm thi**:
  1. Đọc [`04_course_defense/COURSE_DEFENSE_GUIDE.md`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/04_course_defense/COURSE_DEFENSE_GUIDE.md) để đánh giá mức độ hiểu sâu kiến trúc OOP và kỹ thuật của sinh viên.
  2. Đọc [`02_technical_reference/TECHNICAL_REFERENCE.md`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/02_technical_reference/TECHNICAL_REFERENCE.md) để kiểm tra tính đúng đắn của thiết kế cơ sở dữ liệu, API và thuật toán RAG.
  3. Tham khảo [`05_qa_and_demo/COVERAGE_MATRIX.md`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/05_qa_and_demo/COVERAGE_MATRIX.md) để xem ma trận kiểm thử.

- **Dành cho Sinh viên / Người thực hiện Demo**:
  1. Mở [`05_qa_and_demo/LIVE_DEMO_SCRIPT.md`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/05_qa_and_demo/LIVE_DEMO_SCRIPT.md) và làm theo từng bước trong 12 phút.
  2. Lấy dữ liệu mẫu từ [`05_qa_and_demo/fixtures/`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/05_qa_and_demo/fixtures) để tải lên hệ thống.
  3. Luyện tập các câu trả lời ngắn gọn trong [`04_course_defense/COURSE_DEFENSE_GUIDE.md`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/04_course_defense/COURSE_DEFENSE_GUIDE.md).

- **Dành cho Người dùng cuối / Người trải nghiệm**:
  1. Đọc [`01_guides/KNOWLEDGEOS_GUIDE.md`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/01_guides/KNOWLEDGEOS_GUIDE.md) hoặc xem bản PDF [`01_guides/KNOWLEDGEOS_GUIDE.pdf`](file:///C:/Users/Bao%20Phuc/Documents/GroupSync_Build/docs/01_guides/KNOWLEDGEOS_GUIDE.pdf).
  2. Làm theo 39 bước hướng dẫn thực hành trên trang web.
