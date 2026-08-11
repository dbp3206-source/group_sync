# GroupSync — Project Decisions

> Trạng thái: **baseline đã chốt cho giai đoạn implementation**  
> Phạm vi tài liệu: quyết định product/architecture, không chứa implementation code.  
> Khi tài liệu cũ khác với file này, ưu tiên theo thứ tự: yêu cầu hiện tại của chủ dự án → file này → `RISK_AND_SCOPE_GUARD.md` → `IMPLEMENTATION_PLAN.md` → Foundation/A-to-Z prompts → PeSoc reference.

## 1. Product baseline

GroupSync là **Personal & Group Activity Management Platform**. Calendar cá nhân không phải một tính năng đứng riêng; nó là dữ liệu đầu vào chung để Study Group và Badminton Group vận hành.

Chuỗi giá trị chính:

```text
Personal schedule
→ Calendar Aggregator
→ Availability / conflict detection
→ Group activity
→ Registration / participation
→ Derived calendar, notification, history, ranking and feed
```

Hai vertical được triển khai:

- **Study Group:** vertical vừa phải để chứng minh shared core dùng được ngoài thể thao.
- **Badminton Group:** flagship vertical, đi hết vòng đời session → registration/waitlist → check-in → court/pairing → match/result → ranking/history/statistics → news/notification.

Nguyên tắc product: **Input once, derive many**. Người dùng chỉ nhập fact mà hệ thống không tự biết; dữ liệu tổng hợp, conflict, promotion, statistics và notification được suy ra từ nguồn thật.

## 2. Các điểm mâu thuẫn/không thống nhất đã phát hiện và cách chốt

| Điểm lệch giữa tài liệu | Quyết định cuối cùng | Lý do |
|---|---|---|
| Foundation chỉ có `STUDY/BADMINTON`, prompt A-to-Z có thêm `OTHER` | Core chỉ có `STUDY` và `BADMINTON` | `OTHER` tạo một vertical không có nghiệp vụ, UI hay acceptance criteria; trái scope “hai vertical sâu”. Có thể thêm bằng migration sau. |
| Season được ghi lúc là P1, lúc nằm trong Badminton Core | **Season tối thiểu là Core data boundary**; quản trị nhiều season/awards/archive là Advanced | Nếu thêm Season sau ranking sẽ phải đổi khóa và lịch sử. Mỗi Badminton Group tự có một active season mặc định; UI quản trị nâng cao để sau. |
| Roadmap Foundation chia 15 phase nhỏ, A-to-Z gom thành 4–5 prompt lớn | `IMPLEMENTATION_PLAN.md` dùng phase nhỏ làm source of truth; prompt A-to-Z chỉ là milestone bundle | Task nhỏ dễ build/test/debug và giảm rủi ro vibe coding chạm quá nhiều module cùng lúc. |
| Auth cho phép JWT hoặc server session | Chọn **Spring Security server-side session + HttpOnly cookie** | Không cần tự viết JWT filter/refresh flow; phù hợp modular monolith. Dùng Vite proxy khi dev và CSRF protection theo Spring Security. Nếu deployment bắt buộc cross-site tách domain, chỉ đổi quyết định sau khi có bằng chứng. |
| Calendar nói “system-created event/link”, nhưng cũng yêu cầu aggregator | Không lưu bản sao calendar event cho Study/Badminton; aggregator trả **derived calendar item** từ participation nguồn | Reschedule/cancel tự phản ánh, không cần đồng bộ hai bảng và không có dữ liệu drift. `StudySessionParticipant`/`BadmintonRegistration` chính là link nguồn. |
| `RequiredMembersStrategy` được gọi là strategy ở một chỗ; chỗ khác coi là rule | Required members là **constraint/filter**; strategy Core gồm `MaximumAttendance` và `EarliestPossible` | Hai strategy đủ để thể hiện polymorphism thật; required-member không phải thuật toán xếp hạng độc lập. |
| Ranking Core có thể là points hoặc Elo; A-to-Z nhắc Elo sau | Core dùng **PointsRanking**; Elo là Advanced | Points dễ giải thích, test, debug cho doubles. Không để Elo chặn golden flow. |
| Partner/head-to-head lúc “P1 nếu dễ”, lúc nằm trong danh sách statistics rộng | Core chỉ có played/win/loss/win rate/points/recent form/attendance/no-show; partner và head-to-head là Advanced | Giữ đúng flagship mà không nổ số lượng query và edge case. |
| News có comments “optional”, PeSoc có social/video/feed lớn | Core có announcement thủ công + một số system news template; comments và social/video feed không thuộc Core | News phục vụ group operation, không biến GroupSync thành mạng xã hội. |
| Court allocation có thể balanced hoặc sequential | Core có manual + deterministic sequential allocation; balanced court allocation là Advanced. Pairing Core có random + balanced | Phân biệt rõ “xếp người vào sân” với “chia hai đội”; giữ thuật toán dễ bảo vệ. |
| Spring Boot docs ghi 4.1.x, PeSoc pom dùng 4.0.6 | GroupSync dùng Spring Boot **4.1.x stable** + Java 21 | Spring Boot 4.1.0 đã là stable và hỗ trợ Java 21; PeSoc chỉ là reference, không quyết định version. |
| PostgreSQL được ghi 18.x nhưng nhà cung cấp deploy có thể khác | Target local là PostgreSQL 18; chỉ dùng feature SQL phổ thông để có thể deploy PostgreSQL 17 nếu provider bắt buộc | Không để minor/provider chặn đồ án; exact version được ghi trong setup report. |

Nguồn version chính thức: [Spring Boot 4.1](https://spring.io/projects/spring-boot/), [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html), [React 19.2](https://react.dev/blog/2025/10/01/react-19-2).

## 3. Architecture

### D-01 — Deployment shape

- Một React + TypeScript SPA.
- Một Spring Boot REST API.
- Một PostgreSQL database.
- Giao tiếp synchronous REST/JSON.
- Không microservice, broker hoặc distributed workflow.

Đây là **modular monolith**, không phải microservices trong cùng repo.

### D-02 — Backend package structure

Chọn **package-by-feature, layers inside each feature**:

```text
com.groupsync.backend
├── shared/                 # config, exception, time, common API primitives
├── auth/                   # controller, dto, service, security
├── user/                   # model, repository, service, dto
├── group/                  # model, repository, service, controller, dto
├── calendar/               # personal + aggregation contracts
├── availability/           # interval logic + scheduling strategies
├── study/                  # Study vertical
├── badminton/              # Badminton vertical, chia subpackage theo nghiệp vụ khi cần
├── notification/           # in-app notifications + scheduler
├── news/                   # group announcements/system news
└── dashboard/              # read-oriented composition, không chứa write rule
```

Trong mỗi module vẫn theo luồng quen thuộc:

```text
controller → service → repository/model
```

Không dùng Spring Modulith, ArchUnit, hexagonal ports/adapters ceremony hoặc generic base service trong Core. Package boundaries được giữ bằng dependency rules, review và test.

### D-03 — Dependency direction

- `shared` không phụ thuộc domain module.
- `user/auth` không phụ thuộc Study/Badminton.
- `group` phụ thuộc `user`.
- `calendar` phụ thuộc `user`; định nghĩa `CalendarSource` extension point.
- `availability` phụ thuộc `calendar` + `group`.
- `study` và `badminton` phụ thuộc shared core; không phụ thuộc lẫn nhau.
- `notification/news` nhận command/event từ domain; domain không gọi controller/UI.
- `dashboard` được phép đọc nhiều module nhưng không module nào phụ thuộc ngược vào dashboard.

### D-04 — API boundary

- Public REST API chỉ nhận/trả DTO; không serialize JPA entity trực tiếp.
- Validation cú pháp ở request DTO; business validation ở service/domain behavior.
- Một global exception handler trả error shape ổn định: `code`, `message`, `timestamp`, optional `fieldErrors`.
- API versioning chưa cần `/v1`; giữ `/api/...` trong đồ án.

## 4. Authentication and authorization

### D-05 — Authentication

- Email là định danh đăng nhập duy nhất trong Core; display name không cần unique.
- Password hash bằng BCrypt qua Spring Security `PasswordEncoder`.
- Server-side session, cookie `HttpOnly`, `SameSite=Lax`, `Secure` ở production.
- CSRF không bị tắt tùy tiện; frontend lấy/gửi CSRF token theo cấu hình Spring Security.
- Không “remember me” tự chế, không lưu username/password trong cookie.
- System role chỉ `USER`, `ADMIN`.

### D-06 — Group authorization

- Group role nằm trên `Membership`: `OWNER`, `ORGANIZER`, `MEMBER`.
- Permission được kiểm tra trong service cho mọi write use case, không chỉ ẩn nút frontend.
- Group luôn phải còn ít nhất một OWNER.
- OWNER có thể chuyển quyền owner trước khi rời; không auto chọn owner mơ hồ.
- Core invite chỉ dành cho user đã có tài khoản; invitation có `PENDING/ACCEPTED/DECLINED/CANCELLED`.

## 5. Persistence and data integrity

### D-07 — Database and migrations

- PostgreSQL là source of truth.
- Dùng Flyway từ phase có bảng domain đầu tiên; schema thay đổi bằng migration có thứ tự.
- Production/dev không dựa vào `ddl-auto=update`; dùng `validate` sau migration.
- Test thuật toán thuần không cần database; repository/service integration test có profile test và phải có ít nhất một PostgreSQL smoke run trước hardening.
- Không dùng JSON column cho domain chính khi relational table thể hiện rõ hơn.

### D-08 — IDs, audit and enums

- Entity dùng surrogate numeric ID.
- Entity quan trọng có `createdAt`, `updatedAt`; chỉ tạo audited base class nhỏ nếu thật sự giảm lặp.
- Lifecycle status dùng enum persisted as string.
- Không dùng Lombok `@Data` trên JPA entity; tránh `equals/hashCode/toString` đi xuyên relationship.
- Database constraints bảo vệ unique membership, invitation, registration, season stats và ranking history invariants phù hợp.

### D-09 — Time policy

- MVP vận hành trong timezone `Asia/Bangkok` (UTC+7).
- Actual session/event timestamps truyền qua API ở ISO-8601 có offset và lưu bằng kiểu timestamp có timezone/Instant phù hợp.
- Weekly recurrence lưu `DayOfWeek`, `LocalTime`, `validFrom`, `validUntil`, timezone.
- Core không hỗ trợ event recurring qua đêm; `endTime` phải sau `startTime` trong cùng ngày.
- Availability request bị giới hạn range (mặc định tối đa 14 ngày) để thuật toán dễ kiểm soát.

## 6. Calendar and availability

### D-10 — Personal calendar source

Core có hai loại input do user tạo:

1. One-time busy event.
2. Weekly recurring schedule, một hoặc nhiều weekday, có date range.

Không triển khai full RFC recurrence, exception dates phức tạp hoặc external calendar sync trong Core.

### D-11 — Calendar aggregation

`CalendarAggregator` tạo unified view từ các `CalendarSource`:

- personal one-time events;
- recurring occurrences được expand theo query range;
- confirmed Study participation;
- confirmed Badminton registration/check-in.

Mỗi item có `sourceType`, `sourceId`, `start`, `end`, `busy`, và title được mask theo quyền xem. Group organizer chỉ cần biết BUSY/FREE của member khác, không xem title private.

Waitlisted registration không làm user bận. Session `DRAFT/OPEN` chỉ hiển thị dạng tentative ở màn liên quan; chỉ `CONFIRMED/PLAYING/COMPLETED` + active participation đóng góp busy interval.

### D-12 — Availability algorithm

- Merge overlapping busy intervals trước khi tính.
- Candidate grid 30 phút; duration là bội số 30 phút.
- Search window do organizer cung cấp; default daily window có thể là 07:00–22:00.
- `RequiredMembers` là hard constraint.
- `MaximumAttendanceStrategy`: nhiều available member nhất, hòa thì slot sớm hơn.
- `EarliestPossibleStrategy`: slot sớm nhất thỏa minimum attendance/required members.
- Kết quả là suggestion, không tự tạo session.

## 7. Study vertical

### D-13 — Study scope

- `StudySession`: topic/title, time, location/link, status, optional capacity.
- `StudyParticipant`: join/leave + attendance.
- Goals là checklist nhỏ; Materials chỉ link + title.
- Session confirmed/rescheduled/cancelled được aggregator phản ánh tự động.
- Không file upload, grade, assignment submission, course catalog hay LMS workflow trong Core.

## 8. Badminton vertical

### D-14 — Group profile and season

- `BadmintonMemberProfile` thuộc một Membership, không thuộc global User.
- Mỗi Badminton Group có một active `Season` mặc định ngay khi tạo group/khởi tạo vertical.
- `PlayerSeasonStat` unique theo membership + season; ranking tuyệt đối không lưu trên `User`.

### D-15 — Venue, court and session

- Venue thuộc Badminton Group; Court thuộc Venue.
- Session chọn một hoặc nhiều court qua relationship riêng.
- Lifecycle: `DRAFT → OPEN → CONFIRMED → PLAYING → COMPLETED`; `CANCELLED` là nhánh kết thúc.
- Mọi transition đi qua behavior/service có guard; controller không set status trực tiếp.

### D-16 — Registration, capacity and waitlist

- Một row registration cho một user/session, có unique constraint.
- Active statuses: `REGISTERED`, `WAITLISTED`, `CHECKED_IN`; terminal/operational: `CANCELLED`, `NO_SHOW`.
- Join khi còn chỗ → `REGISTERED`; hết chỗ → `WAITLISTED` với `queuedAt`.
- Conflict chỉ cảnh báo, không block trong Core.
- Cancel một registered slot → promote oldest active waitlist FIFO trong cùng transaction.
- Service lock session row khi join/cancel/promote; unique constraint là lớp bảo vệ thứ hai.
- Rejoin sau cancel được phép trước deadline bằng cách tái kích hoạt row và xếp lại cuối queue nếu cần.

### D-17 — Check-in and responsibilities

- Organizer check-in thủ công; không QR trong Core.
- Chỉ `CHECKED_IN` được đưa vào allocation/pairing.
- Responsibility là lightweight session assignment, không phải inventory.
- Nếu assignee cancel, assignment trở lại `NEEDED/UNASSIGNED` và organizer nhận notification.

### D-18 — Court allocation and pairing

- Manual court assignment luôn có.
- Automatic Core: deterministic sequential distribution theo court order.
- Pairing Core có `RandomPairingStrategy` và `BalancedPairingStrategy` dựa trên current season points/initial skill; seed/order được lưu hoặc trả về để test lặp lại được.
- Manual pairing là use case chỉnh assignment, không cần giả làm strategy.
- Không ghép người chưa check-in; một player không xuất hiện hai lần trong cùng round.

### D-19 — Match/result model

- `BadmintonMatch` có hai `MatchSide`; mỗi side có 1 hoặc 2 `MatchParticipant`.
- Doubles là first-class; singles được model cho phép nhưng không cần UI hoàn chỉnh trong Core.
- Core score là một game score A/B đơn giản, không hòa; best-of-three/multi-set là Advanced.
- Lifecycle: `SCHEDULED → PLAYING → RESULT_SUBMITTED → CONFIRMED`; cancel trước confirm nếu cần.
- Participant hoặc organizer có thể submit; organizer confirm.
- Chỉ `CONFIRMED` result ảnh hưởng official stats/ranking.
- Result đã confirmed là immutable trong Core để tránh double-apply; correction/rebuild là Advanced/admin repair path.

### D-20 — Ranking, history and stats

- Core `PointsRankingStrategy`: mỗi participant bên thắng +3, bên thua +1.
- Tie order: points → wins → matches played ít hơn → display name; đây là presentation tie-break, không phát sinh điểm giả.
- Confirm result gọi trực tiếp Ranking/Stats service trong cùng transaction; sau đó mới publish synchronous application event cho notification/system news.
- `RankingHistory` append snapshot sau mỗi confirmed match cho từng participant.
- Recent form được derive từ confirmed matches, không lưu chuỗi W/L riêng.
- Attendance/no-show cập nhật khi session completed; không trộn với match win/loss.

## 9. Automation, notification and news

### D-21 — Automation style

- Business-critical derived data (capacity, promotion, ranking, stats) chạy trực tiếp trong transactional service để dễ trace.
- Spring application event chỉ dùng cho side effect ngắn: in-app notification và system news.
- Event chain tối đa một tầng; listener không publish một chuỗi event mới.
- Spring Scheduler chỉ dùng reminder/registration deadline housekeeping; không là nguồn duy nhất đảm bảo correctness.

### D-22 — Notification/news

- In-app DB notification với `READ/UNREAD`, created time, target type/id hoặc safe relative link.
- Frontend poll/refetch; không WebSocket bắt buộc.
- Core manual group announcement và system templates có ích: invitation, session change/cancel, waitlist promotion, responsibility unassigned, result confirmed, leader changed.
- Không Firebase/push/comment/feed algorithm trong Core.

## 10. Frontend and testing

### D-23 — Frontend

- React 19.2 + TypeScript + Vite + React Router + Axios + Bootstrap 5.
- FullCalendar chỉ dùng như calendar renderer; domain rule không đặt trong component.
- State: local state, AuthContext và feature hooks; không Redux.
- API/types theo feature; page compose component nhỏ; có loading/error/empty/disabled states.
- Mobile-first cho member, desktop thoải mái cho organizer.

### D-24 — Test strategy

- Unit test cho interval/recurrence/strategies/state transitions.
- Service/integration test cho permission, waitlist transaction, calendar source, match confirmation.
- API smoke test cho golden flow chính.
- Frontend ưu tiên typecheck/build; component test cho form/flow có rule quan trọng nếu thời gian cho phép.
- Không theo đuổi 100% coverage; phase không đạt checkpoint nếu critical rule chưa có test.

## 11. Bài học lấy từ PeSoc và giới hạn tham khảo

Đã rà source PeSoc gồm entity/repository/service/controller, cấu hình, Thymeleaf templates và test footprint. Chỉ giữ các lesson:

- sản phẩm cộng đồng có giá trị quay lại nhờ history, upcoming activity, ranking và news;
- classic Spring layer dễ đọc cho sinh viên;
- match/result có thể kéo theo ranking/history/notification;
- dashboard nên tổng hợp dữ liệu có ích thay vì chỉ là CRUD menu.

GroupSync chủ động không lặp lại các vấn đề thấy trong reference:

- plaintext/default password và custom remember-me cookie;
- credential file trong resources;
- controller/service quá lớn, field injection, duplicated notification logic;
- JPA entity trả thẳng ra UI/API;
- string status/type và entity gần như chỉ có setter;
- one-v-one match cứng trên `player1/player2`;
- global rating trên User;
- WebSocket/Firebase/video/social scope;
- `ddl-auto=update` làm schema strategy;
- chỉ có context-load test.

Không copy code, package names, schema, HTML/CSS, auth flow hoặc tournament design từ PeSoc.
