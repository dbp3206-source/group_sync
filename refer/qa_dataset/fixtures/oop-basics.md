# Lập Trình Hướng Đối Tượng (OOP) & Các Mẫu Thiết Kế Phần Mềm

## 1. Bốn Trụ Cột của Lập Trình Hướng Đối Tượng

1. **Tính Đóng Gói (Encapsulation)**: Gom nhóm dữ liệu và các phương thức xử lý dữ liệu vào trong một đơn vị đối tượng, đồng thời che giấu trạng thái nội bộ bằng quyền truy cập `private`/`protected`. Trong KnowledgeOS, thực thể `Resource.java` bảo vệ máy trạng thái qua các hàm `beginParsing()`, `beginEmbedding()`, `markReady()`.
2. **Tính Kế Thừa (Inheritance)**: Cho phép một lớp con kế thừa lại các thuộc tính và hành vi của lớp cha nhằm tái sử dụng mã nguồn.
3. **Tính Đa Hình (Polymorphism)**: Khả năng các đối tượng khác nhau phản ứng với cùng một thông điệp theo các cách khác nhau. Thể hiện qua giao diện `ResourceParser` với các lớp triển khai `PdfResourceParser`, `DocxResourceParser`.
4. **Tính Trừu Tượng (Abstraction)**: Tập trung vào các hành vi cốt lõi của đối tượng và ẩn đi các chi tiết cài đặt phức tạp thông qua Interface hoặc Abstract Class.

---

## 2. Mẫu Thiết Kế Chiến Lược (Strategy Pattern) Trong RAG

Giao diện `RetrievalStrategy` định nghĩa phương thức tìm kiếm trừu tượng `retrieve()`. Có 3 chiến lược cụ thể:
- `SemanticRetrievalStrategy`: Tìm kiếm vector ngữ nghĩa với pgvector.
- `KeywordRetrievalStrategy`: Tìm kiếm từ khóa chính xác bằng PostgreSQL Full-Text Search.
- `HybridRetrievalStrategy`: Kết hợp cả hai nhánh và hợp nhất bằng Reciprocal Rank Fusion (RRF k=60).

Ưu điểm: Tuân thủ nguyên lý **Open/Closed Principle (OCP)**, cho phép mở rộng thuật toán mới mà không cần sửa đổi mã nguồn gọi dịch vụ.
