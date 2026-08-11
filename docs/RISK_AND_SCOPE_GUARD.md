# GroupSync — Risk and Scope Guard

> Mục đích: bảo vệ đồ án khỏi scope creep, overengineering và lỗi data-integrity trong các flow quan trọng.  
> Nhãn: **Core** = phải hoàn thành để đạt product thesis; **Advanced** = chỉ sau Core release; **Out-of-scope** = không triển khai trong đồ án hiện tại.

## 1. Core scope — phải làm, làm đủ sâu

### 1.1 Platform and shared core

- React + TypeScript + Vite responsive SPA.
- Spring Boot + Java 21 REST API.
- PostgreSQL + reproducible migrations.
- Simple Spring Security auth: register/login/logout/current user, BCrypt, USER/ADMIN.
- User profile cơ bản; avatar URL hoặc bỏ avatar upload nếu gây nhiễu.
- Group Core: STUDY/BADMINTON, Membership, Invitation, OWNER/ORGANIZER/MEMBER, permission checks.
- In-app notification DB: inbox, unread/read, safe target.

### 1.2 Calendar and availability shared core

- One-time personal busy event.
- Weekly recurring schedule, bounded date range.
- Calendar Aggregator với source-aware derived items.
- Free/busy and conflict detection.
- Availability search theo 30-minute grid.
- MaximumAttendance + EarliestPossible strategies.
- Required-member constraint.
- Privacy: user khác chỉ thấy BUSY/FREE.

### 1.3 Study Group — moderate vertical

- StudySession lifecycle.
- Create from manual time hoặc availability suggestion.
- Join/leave; optional capacity.
- Topic, goals checklist, material links.
- Attendance.
- Confirm/reschedule/cancel tự phản ánh trên calendar participant.
- Operational notification tối thiểu.

### 1.4 Badminton Group — flagship vertical

- Group-scoped Badminton profile.
- Minimal active Season data boundary.
- Venue/Court.
- Session lifecycle, selected courts, capacity, deadline.
- Registration, duplicate prevention, FIFO waitlist, transaction-safe promotion.
- Conflict warning.
- Calendar-derived participation.
- Manual check-in/no-show.
- Lightweight equipment/session responsibilities và cancellation recheck.
- Manual + sequential court allocation.
- Manual correction + Random/Balanced pairing.
- Doubles-first MatchSide/MatchParticipant model.
- Submit/confirm one-game result.
- Points ranking, group/season leaderboard.
- Played/win/loss/win rate/recent form/attendance/no-show.
- Ranking history snapshot.
- Manual announcements + bounded system news templates.
- Notifications cho change/cancel/promotion/result/responsibility.
- Home/group dashboard tối thiểu.

### 1.5 Core verification

- Unit tests cho recurrence/interval/strategies/state transitions.
- Integration/service tests cho permission, capacity/waitlist, calendar source và result-derived stats.
- Fresh database migration check.
- Backend package + frontend typecheck/build.
- Golden flow end-to-end trên browser.
- Demo seed không chứa real credentials.

## 2. Advanced/P1 scope — chỉ sau Core release xanh

Advanced item không được “làm ké” trong Core phase. Mỗi item cần acceptance criteria và owner rõ.

### 2.1 Recommended first

- Multi-season management UI, close/reopen season, season awards.
- Head-to-head statistics.
- Partner statistics.
- AvoidRecentPartner pairing.
- Better ranking history chart and export.
- Multi-set/best-of-three badminton scoring.
- Result correction + deterministic ranking rebuild admin use case.
- Simple admin user/group moderation.
- CSV export.

### 2.2 Later only if time remains

- Google Calendar integration (prefer one-way export before two-way sync).
- QR check-in.
- Tournament (group/knockout).
- Elo ranking as another `RankingStrategy`.
- Push notification.
- Study notes/summary.
- Announcement comments.
- Calendar exception dates or richer recurrence.
- Flexible multi-timezone support.

Advanced gate requires all of:

1. Phase 16 golden flow passes.
2. Critical backend tests and frontend build are green.
3. No open high-risk permission/data-integrity bug.
4. The item does not force a rewrite of Core data model.
5. Team agrees which existing backlog item will be dropped if time slips.

## 3. Out-of-scope — không đưa vào roadmap hiện tại

### Architecture/infrastructure

- Microservices.
- CQRS, event sourcing, hexagonal/clean-architecture ceremony.
- Kafka, RabbitMQ or other message broker.
- Redis distributed cache/lock.
- Kubernetes.
- Generic workflow/rule engine.
- Mandatory Dockerized full stack.
- GraphQL.
- Full realtime/WebSocket infrastructure.

### Product

- AI/ML scheduling, pairing, ranking or content generation.
- Payment gateway, wallet, subscriptions or court-booking payment.
- IoT court/equipment tracking.
- Native mobile app.
- Private/full chat.
- Social follower/feed recommendation.
- TikTok/video feed like PeSocTok.
- Football, Running, Boardgame hoặc generic OTHER vertical trong Core.
- Full LMS: grades, assignment submission, course management.
- Warehouse/inventory/asset depreciation system.
- Real venue booking provider integration.

### Auth/security complexity

- OAuth/social login, SSO, MFA.
- Refresh-token rotation/device session suite.
- Custom remember-me cookie.
- Multi-tenant enterprise RBAC/ABAC framework.

### Calendar/optimization complexity

- Full RFC recurrence engine.
- Optimization solver for court/pairing.
- Automatic rescheduling without organizer confirmation.
- Two-way external calendar sync in Core.

Nếu stakeholder yêu cầu một mục Out-of-scope, phải cập nhật `PROJECT_DECISIONS.md`, đổi timeline và nêu rõ Core item nào bị cắt; không chỉ thêm task.

## 4. Risk register

| ID | Risk | Probability / Impact | Early warning | Mitigation / checkpoint |
|---|---|---|---|---|
| R-01 | Scope quá lớn cho team năm 3 | High / Critical | Nhiều module cùng “80%”, build đỏ kéo dài | Phase nhỏ, Definition of Done, freeze Advanced; golden flow là ưu tiên số 1. |
| R-02 | Vibe coding tạo code compile được nhưng rule sai | High / Critical | Controller set status trực tiếp, thiếu negative tests | Mỗi critical rule có test trước checkpoint; review use-case trace end-to-end. |
| R-03 | Package-by-feature biến thành nhiều abstraction khó hiểu | Medium / High | Interface chỉ có một implementation, generic base class | Chỉ giữ strategy/source có ≥2 behavior hoặc extension thật; xóa abstraction speculative. |
| R-04 | Recurrence/timezone/off-by-one | High / High | Slot thiếu/thừa ở boundary, frontend lệch ngày | Một timezone baseline, ISO offset, half-open intervals, boundary unit tests. |
| R-05 | Availability chậm hoặc combinatorial | Medium / High | Query quá dài, load tất cả lịch sử | Range ≤14 ngày, grid 30 phút, merge busy intervals, bounded member set, sanity benchmark. |
| R-06 | Privacy leak calendar title | Medium / Critical | API trả entity/title private cho organizer | DTO theo requester, source privacy tests, không expose entity. |
| R-07 | Calendar derived data bị drift | High / High nếu duplicate rows | Có “sync job” sửa calendar, reschedule cần update nhiều bảng | Derived view từ source registration/participation; không persist duplicate group calendar event. |
| R-08 | Capacity vượt giới hạn khi concurrent join | Medium / Critical | Test tuần tự xanh nhưng production có >capacity | Lock session row + same transaction + unique DB constraint + PostgreSQL concurrency test. |
| R-09 | Waitlist promotion sai FIFO/double promotion | Medium / Critical | Hai user cùng promoted, queuedAt null | Repository query ordered + lock + idempotent transition + cancel/promote integration test. |
| R-10 | Ranking/stats bị apply hai lần | Medium / Critical | Refresh/retry confirm tăng điểm lần nữa | Confirm idempotent, unique history source, stats update cùng transaction, double-confirm test. |
| R-11 | App event chain khó debug | Medium / High | Listener publish listener, correctness phụ thuộc scheduler | Critical derived data gọi trực tiếp; event chỉ one-level side effect; log source event ID. |
| R-12 | JPA recursion/N+1/lazy exception | High / Medium | Entity serialized, endpoint page load nhiều query | DTO projection/mapping, explicit fetch query, không Lombok `@Data` entity, query review ở hardening. |
| R-13 | Migration/schema không tái tạo được | Medium / Critical | Chỉ chạy được trên DB cũ, `ddl-auto=update` | Flyway từ đầu, fresh DB checkpoint, seed riêng migration/schema. |
| R-14 | Auth “đơn giản” thành insecure custom auth | Medium / Critical | Plain password, username cookie, CSRF disabled | Spring Security + BCrypt + session + CSRF; auth integration tests; secret scan. |
| R-15 | Frontend/backend contract drift | High / Medium | `any`, duplicated ad-hoc response map, 400 khó hiểu | DTO/types theo feature, stable error model, API client centralized, frontend typecheck each phase. |
| R-16 | Study/Badminton phụ thuộc lẫn nhau | Medium / High | Import giữa vertical packages | Shared contracts ở calendar/group/notification; dependency review before merge. |
| R-17 | Season thêm quá muộn làm vỡ ranking | High / High | Rating/stat nằm trên User/group only | Minimal Season + group-scoped profile có từ Badminton foundation. |
| R-18 | Pairing “balanced” khó giải thích/khó test | Medium / Medium | Random output thay đổi mọi lần, magic formula | Formula đơn giản, stable sort/seed, invariant tests, explanation field/preview. |
| R-19 | News/notification phình thành social network | High / Medium | Comment/like/follower/video xuất hiện trước Core complete | Bounded templates, polling DB, comments/push Advanced, PeSocTok explicitly excluded. |
| R-20 | PeSoc được copy máy móc | Medium / High | Global Elo on User, player1/player2 match, giant controller | Review conceptual only; doubles composition, group/season scope, service boundary and DTO rules. |
| R-21 | Test chỉ có contextLoads | High / Critical | Critical phase không có rule tests | Required test matrix in each phase; no checkpoint without critical tests. |
| R-22 | Team merge/migration conflict | High / Medium | Nhiều người sửa cùng entity/migration number | Single owner per table/migration, small PR/commit, integration contract review. |
| R-23 | Demo phụ thuộc external service/network | Medium / High | Firebase/Google/provider required | Core uses DB notification/local calendar only; demo works offline except local app/database. |
| R-24 | UI polish nuốt thời gian nghiệp vụ | High / Medium | Dashboard đẹp nhưng waitlist/result chưa chắc | Bootstrap/simple UI until golden flow; polish only Phase 15/16. |

## 5. Scope decision rules during implementation

### 5.1 Add a dependency only when

- Nó trực tiếp phục vụ Core feature hiện tại.
- Team có thể giải thích vì sao built-in/JDK/Spring hiện có không đủ.
- License và maintenance phù hợp.
- Nó không kéo theo một framework hoặc runtime mới cho một việc nhỏ.

Không thêm dependency chỉ vì reference repo hoặc generated code đề xuất.

### 5.2 Create an interface/strategy only when

- Có ít nhất hai behavior thật trong Core; hoặc
- Có một implementation Core và một Advanced implementation đã được chấp thuận, với API ổn định; hoặc
- Nó phá dependency cycle rõ ràng như `CalendarSource`.

Nếu không thỏa, dùng class/service cụ thể trước.

### 5.3 Persist derived data only when

- Query derive trực tiếp quá tốn kém hoặc không thể audit; và
- Có source reference + idempotency/unique key; và
- Có test chứng minh update/cancel/retry không làm drift.

Calendar item không persist duplicate. Ranking history được persist vì cần audit theo thời gian; recent form được derive.

### 5.4 Stop and review when

- Một task cần sửa từ ba module domain trở lên ngoài integration contract dự kiến.
- Phải đổi identity/foreign key của Core entity đã có data.
- Permission rule không thể mô tả bằng một bảng actor/action đơn giản.
- Cần background processing để đạt correctness.
- Một class service/controller vượt khoảng 250–300 dòng và chứa nhiều use case khác nhau.
- Một phase chưa xanh nhưng team muốn bắt đầu phase sau.

## 6. OOP defense guard

OOP phải thể hiện ở behavior thật, không phải số lượng interface.

### Evidence Core cần có

- **Encapsulation:** `SessionRegistration.cancel`, session lifecycle transition, match result confirmation guard.
- **Abstraction/polymorphism:** scheduling strategies, pairing strategies; ranking strategy nếu có Points + later implementation.
- **Composition:** Group–Membership; Match–MatchSide–Participant; Session–Court/Registration/Responsibility.
- **Single responsibility:** controller HTTP only; service use-case transaction; repository persistence only.
- **Dependency direction:** vertical implements shared CalendarSource without shared core import vertical entity.

### Anti-pattern fail conditions

- Entity chỉ toàn setter trong khi state transition có rule.
- Controller tính ranking, waitlist hoặc permission.
- Một `GodService` xử lý session + registration + match + notification + upload.
- Deep inheritance tree cho activity/group/session.
- Generic `BaseController/BaseService/BaseRepository` làm trace khó hơn.
- Strategy interface được tạo chỉ để “có polymorphism” nhưng behavior không thay đổi.

## 7. Core release gate

Không gọi project là Core-complete nếu thiếu bất kỳ mục nào:

- [ ] Auth hash password và permission server-side.
- [ ] Group membership/role tests.
- [ ] Recurring schedule + availability boundary tests.
- [ ] Study confirmed/rescheduled/cancelled calendar behavior.
- [ ] Badminton capacity 16 / user 17 waitlist / cancel → FIFO promotion.
- [ ] PostgreSQL concurrency guard cho registration.
- [ ] Check-in → allocation → valid doubles pairing.
- [ ] Result confirmed exactly once → group/season stats/ranking/history.
- [ ] Notification/news không duplicate trong retry path chính.
- [ ] Fresh DB migration + demo seed.
- [ ] Backend tests/package, frontend typecheck/build.
- [ ] Golden flow smoke trên mobile-width và desktop-width browser.
- [ ] Không external API cần cho golden demo.
- [ ] Known limitations được ghi, không che giấu bằng score/claim.

## 8. Explicit reference boundary for PeSoc

PeSoc chỉ được dùng để tham khảo:

- độ dài/độ khó mà sinh viên có thể đọc;
- giá trị của history, ranking, upcoming/past matches, news và dashboard;
- lợi ích của classic Spring layering.

Không lấy từ PeSoc:

- code hoặc HTML/CSS;
- package/schema/entity names;
- custom auth/remember-me/plain password;
- Firebase credential/push setup;
- global User Elo;
- one-v-one `player1/player2` match model;
- tournament/video/social/WebSocket scope;
- giant controller/service hoặc field injection style.
