# Kiến Trúc Hệ Thống Học Máy và Trí Tuệ Nhân Tạo

Hệ thống Học máy (Machine Learning System) hiện đại đòi hỏi sự kết hợp chặt chẽ giữa lưu trữ dữ liệu lớn và thuật toán suy luận nhanh.

## Thành Phần Chính

1. **Thu thập dữ liệu (Data Ingestion)**: Tiếp nhận tài liệu, chuyển đổi các định dạng văn bản thành định dạng chuẩn hóa.
2. **Biểu diễn véc-tơ (Vector Embeddings)**: Chuyển đổi ngôn ngữ tự nhiên thành không gian véc-tơ nhiều chiều (768 chiều), giữ lại ngữ nghĩa của câu hỏi và tài liệu.
3. **Cơ sở dữ liệu Véc-tơ (pgvector)**: Cho phép thực hiện tìm kiếm độ tương đồng Cosine (Cosine Similarity) trực tiếp trong hệ quản trị cơ sở dữ liệu PostgreSQL.
4. **Truy xuất lai (Hybrid Retrieval)**: Kết hợp tìm kiếm theo từ khóa (Lexical Search) và tìm kiếm ngữ nghĩa (Semantic Search) để đảm bảo độ chính xác cao nhất cho tiếng Việt.
