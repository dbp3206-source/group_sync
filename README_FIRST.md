# GroupSync — Codex Build Kit
## Lượt 1/3: Foundation Pack

Đây là bộ context nền tảng phải được đặt trong repository GroupSync **trước khi yêu cầu Codex viết nhiều code**.

Mục tiêu của lượt 1:
1. Giúp AI hiểu GroupSync là sản phẩm gì, dành cho ai và khác một app calendar CRUD ở đâu.
2. Khóa phạm vi kỹ thuật ở mức phù hợp sinh viên năm 3.
3. Cho Codex một `AGENTS.md` ngắn gọn để giữ quy tắc xuyên suốt repo.
4. Chốt roadmap và cấu trúc source gần với repo PeSoc: `controller -> service -> repository -> model`, nhưng tách React frontend và bổ sung DTO/strategy/event khi có lý do.
5. Cung cấp prompt đầu tiên cho Sol/Work lập plan và prompt thứ hai cho Luna High/Codex bootstrap project.

## Cách dùng bộ này

### Bước A — Tạo repo trống
Tạo repository tên `groupsync` và copy toàn bộ nội dung kit này vào repo.

Gợi ý ban đầu:

```text
groupsync/
├── AGENTS.md
├── docs/
├── prompts/
├── reference/
├── backend/          # Codex tạo ở Phase 0
└── frontend/         # Codex tạo ở Phase 0
```

### Bước B — Plan trước bằng Sol / ChatGPT Work
Mở ChatGPT Work hoặc một phiên Plan dùng Sol. Gửi repo/folder hoặc tối thiểu các file:
- `AGENTS.md`
- `docs/00_PROJECT_OVERVIEW.md`
- `docs/01_SCOPE_AND_FEATURES.md`
- `docs/02_ARCHITECTURE_AND_REPO_STRUCTURE.md`
- `docs/03_ROADMAP_AND_PHASES.md`
- `docs/04_CODING_LEVEL_AND_RULES.md`

Sau đó dùng prompt trong `prompts/00_SOL_PLAN_PROMPT.md`.

Mục tiêu của bước Plan **không phải viết code**. Plan phải rà lại dependency giữa các module, phát hiện điểm mơ hồ, rồi tạo một implementation plan thực tế. Nếu có nhiều cách đúng, model được phép tự chọn cách đơn giản nhất miễn không vi phạm phạm vi.

### Bước C — Build bằng Luna High / Codex
Mở repo trong Codex, chọn Luna High theo cấu hình bạn muốn dùng, rồi gửi prompt `prompts/01_LUNA_BOOTSTRAP_PROMPT.md`.

Task đầu tiên chỉ scaffold + verify project. Không yêu cầu Codex build 20 tính năng trong một task.

### Bước D — Sau bootstrap
Chỉ khi backend/frontend đều chạy được, mới đi module-by-module theo roadmap.

Các prompt module chi tiết sẽ nằm ở các lượt sau của kit.

## Nguyên tắc quan trọng nhất

> **Explain/inspect first -> implement a bounded feature -> run tests/build -> summarize -> commit/checkpoint -> next feature.**

Không prompt kiểu: `Hãy build toàn bộ GroupSync cho tôi`.

## Nguồn tham khảo PeSoc
Repo PeSoc chỉ dùng làm benchmark về:
- độ khó code;
- cách chia controller/service/repository/model dễ đọc;
- ý tưởng ranking/history/tournament/news/notification;
- sản phẩm cộng đồng có dữ liệu lịch sử để user muốn quay lại.

**Không copy code, schema, authentication, credential handling hay architecture một cách máy móc.** Xem `reference/PESOC_REFERENCE_NOTES.md`.
