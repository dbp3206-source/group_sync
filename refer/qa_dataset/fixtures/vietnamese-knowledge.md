# Chu Trình Quản Trị Tri Thức & Kiến Trúc Tìm Kiếm Lai KnowledgeOS

## 1. Chi Tiết 6 Bước Trong Chu Trình Quản Trị Tri Thức của KnowledgeOS

Hệ điều hành tri thức cá nhân KnowledgeOS vận hành theo chu trình khép kín gồm 6 giai đoạn liên tục:

1. **Thu thập dữ liệu (Collect)**: Tiếp nhận các nguồn tài liệu đa định dạng (PDF học thuật, Word giáo trình, Markdown ghi chú, bài giảng). Hệ thống tự động phân tách cấu trúc văn bản qua các Parser đa hình, chia nhỏ thành các đoạn 500 ký tự (độ gối đầu 100 ký tự) và lưu trữ tệp nhị phân gốc bền vững vào cơ sở dữ liệu.
2. **Tổ chức tri thức (Organize)**: Phân loại tài liệu khoa học theo cây Thư mục môn học (Collections) và Thẻ chuyên đề (Tags). Tích hợp thuật toán Trí tuệ Nhân tạo thông minh (Smart Organization Suggestions) tự động phân tích vector nội dung để gợi ý nhãn dán tối ưu.
3. **Thấu hiểu nội dung (Understand)**: Cung cấp không gian đọc Reader tập trung cao độ, loại bỏ hoàn toàn xao nhãng, hỗ trợ ghi chép nhanh các phát hiện nghiên cứu (Session Notes) và tự động phát hiện các tài liệu liên quan thông qua khoảng cách vector Cosine.
4. **Truy xuất lai (Retrieve)**: Kích hoạt đồng thời 2 nhánh tìm kiếm: Nhánh Ngữ nghĩa (Semantic Search qua pgvector với chỉ mục HNSW) và Nhánh Từ khóa chính xác (Lexical Search qua PostgreSQL Full-Text Search). Hợp nhất kết quả bằng thuật toán Xếp hạng Tương hỗ (Reciprocal Rank Fusion - RRF k=60).
5. **Hỏi đáp thông minh (Ask)**: Tổng hợp câu trả lời ngôn ngữ tự nhiên từ Google Gemini dựa trên ngữ cảnh tài liệu được cung cấp (Grounding), loại bỏ hoàn toàn hiện tượng AI bịa đặt ảo giác và gắn nhãn trích dẫn nguồn [1], [2] có thể bấm kiểm tra văn bản gốc.
6. **Học tập & Thống kê (Learn)**: Rèn luyện tính kỷ luật học tập với đồng hồ bấm giờ Pomodoro 25 phút không xao nhãng (Focus Mode) và theo dõi trực quan biểu đồ tăng trưởng tri thức thông qua bảng điều khiển Insights Dashboard.

---

## 2. Tại Sao Tìm Kiếm Lai (Hybrid RAG) Lại Vượt Trội Hơn Tìm Kiếm Vector Truyền Thống?

Tìm kiếm lai (Hybrid Retrieval) vượt trội hơn hẳn so với tìm kiếm vector đơn thuần nhờ các ưu thế công nghệ sau:

1. **Khắc phục điểm mù của Vector Embeddings**: Mô hình vector thường bị "trôi ngữ nghĩa" và không phân biệt được các mã định danh kỹ thuật, chuỗi số hiệu (như CVE-2026-8819, RFC-9421) hay tên biến camelCase. Nhánh Full-Text Search (FTS) với chỉ mục GIN của PostgreSQL xử lý hoàn hảo các chuỗi ký tự chính xác này.
2. **Hiểu sâu ngữ nghĩa câu hỏi tự nhiên**: Nhánh Semantic sử dụng vector embedding 768 chiều từ Gemini giúp hiểu được các câu hỏi đồng nghĩa, câu hỏi diễn giải gián tiếp ngay cả khi không trùng khớp từ vựng.
3. **Thuật toán Hợp nhất RRF (k=60) cân bằng tối ưu**: Không cần hiệu chỉnh trọng số thủ công, RRF tự động ưu tiên những đoạn văn xuất hiện ở thứ hạng cao trên cả hai nhánh tìm kiếm, mang lại độ chính xác vượt trội cho cả tiếng Việt và tiếng Anh.
