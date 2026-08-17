# GroupSync — Bản bàn giao cho account mới

Ngày bàn giao: 14/08/2026  
Commit production hiện tại: `e49cbfb7313a48f8a41fa1885c94681eebcf0a3f`  
Branch production: `main`

## 1. Trạng thái hiện tại

GroupSync là web app quản lý lịch cá nhân và hoạt động nhóm. Hai vertical đang có:

- Study Group: nhóm học, lịch chung, buổi học và trạng thái xác nhận.
- Badminton Group: nhóm cầu lông, địa điểm, sân, buổi chơi, check-in/kết quả và tournament workspace.

Nguyên tắc sản phẩm: **Input once, derive many** — nhập dữ liệu một lần, lịch và thông tin liên quan được suy ra từ đó.

Phiên bản hiện tại đã deploy public và đã smoke-test production. Account mới cần tiếp quản GitHub, Vercel, Render và database; không cần dựng lại giao diện.

## 2. URL và tài nguyên

| Thành phần | URL / vị trí | Vai trò |
|---|---|---|
| GitHub | <https://github.com/dbp3206-source/group_sync> | Source of truth, branch `main` |
| Frontend production | <https://group-sync-khaki.vercel.app/> | URL người dùng |
| Backend production | <https://groupsync-backend-h68s.onrender.com> | Spring Boot API |
| Backend health | <https://groupsync-backend-h68s.onrender.com/api/health> | Phải trả HTTP 200 và `UP` |
| Local project | `C:\Users\Bao Phuc\Documents\GroupSync_Build` | Bản làm việc hiện tại |
| Vercel project | `group-sync` | Build/deploy frontend |
| Render resources | Blueprint `group_sync`, service `groupsync-backend`, DB `groupsync-db` | Backend + PostgreSQL |

Render Free có thể spin down khi không hoạt động; request đầu tiên sau thời gian nghỉ có thể chậm khoảng vài chục giây.

## 3. Kiến trúc triển khai

```text
Browser -> Vercel React/Vite frontend
             |
             | /api/* rewrite trong frontend/vercel.json
             v
          Render Spring Boot REST API
             |
             v
          Render PostgreSQL groupsync-db
```

Frontend dùng `baseURL: '/api'` trong `frontend/src/api/client.ts`. Vercel rewrite `/api/:path*` tới Render. Local Vite cũng proxy `/api` tới `http://127.0.0.1:8080`. Đây là lý do session và CSRF cookie đi qua cùng frontend origin.

## 4. Chuyển sang account mới

### GitHub

1. Transfer ownership repository sang account/team mới, hoặc mời account mới làm collaborator có quyền ghi.
2. Giữ repository name `group_sync`, branch `main` nếu có thể.
3. Nếu đổi URL owner/repository, cập nhật Git connection trong Vercel và Render.
4. Không commit `.env`, password, JWT secret, database URL chứa credential hoặc token.

### Vercel

1. Account mới chọn **Add New Project / Import Git Repository** và import `group_sync`.
2. Kiểm tra:
   - Root Directory: `frontend`
   - Build Command: `npm run build`
   - Output Directory: `dist`
   - Production Branch: `main`
3. Giữ `frontend/vercel.json`; file này chứa API rewrite và React Router fallback.
4. Production hiện tại không cần `VITE_API_URL`; frontend dùng relative `/api`. Kiểm tra và bỏ biến cũ nếu còn.
5. Deploy từ `main`, mở domain mới và thử `/login`, `/register`, dashboard.
6. Nếu đổi domain, cập nhật Render `APP_CORS_ORIGINS` thành origin mới, ví dụ `https://ten-moi.vercel.app`.

Nếu muốn giữ `group-sync-khaki.vercel.app`, domain phải được chuyển/gỡ khỏi project cũ theo cơ chế của Vercel.

### Render và PostgreSQL

Ưu tiên transfer Blueprint/service/database hiện tại. Không xóa database cũ trước khi backup hoặc xác nhận dữ liệu không cần giữ.

Thông số hiện tại:

- Web service: `groupsync-backend`
- Database: `groupsync-db`
- Runtime: Docker
- Region: Singapore
- Health check: `/api/health`
- Auto deploy theo commit branch được kết nối

Nếu phải tạo lại:

1. Tạo PostgreSQL 17, database `groupsync`, user `groupsync`.
2. Tạo Docker web service từ `backend/Dockerfile`, context `backend`.
3. Giữ health check `/api/health`.
4. Kết nối biến database Render cấp vào service.
5. Đặt `APP_CORS_ORIGINS` bằng origin Vercel chính xác.
6. Giữ:
   - `APP_TIMEZONE=Asia/Bangkok`
   - `APP_COOKIE_SAME_SITE=none`
   - `APP_COOKIE_SECURE=true`
   - `FLYWAY_ENABLED=true`
7. Deploy và kiểm tra health trả `UP`.

### Lỗi Render đã từng xử lý

Đã từng gặp:

```text
JDBC URL contains too many / characters
Driver org.postgresql.Driver claims to not accept jdbcUrl
```

Nguyên nhân là `DATABASE_URL` dạng URI của Render bị đưa thẳng vào Spring datasource như JDBC URL. `backend/render-start.sh` hiện chuẩn hóa authority/host/database thành URL JDBC hợp lệ trước khi chạy JAR. Không thay script này bằng nối chuỗi thủ công trong dashboard.

Nếu lỗi quay lại, kiểm tra `DATABASE_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_NAME` và runtime `DB_URL` phải có dạng `jdbc:postgresql://host:port/database`. Không ghi password vào GitHub hoặc tài liệu.

## 5. Chạy local

Cách khuyến nghị từ root:

```powershell
powershell.exe -ExecutionPolicy Bypass -File .\scripts\start-groupsync.ps1
```

Dừng process do script tạo:

```powershell
powershell.exe -ExecutionPolicy Bypass -File .\scripts\stop-groupsync.ps1
```

Chạy thủ công backend:

```powershell
cd backend
./mvnw.cmd spring-boot:run
```

Backend mặc định là `http://localhost:8080`; health là `http://localhost:8080/api/health`.

Frontend ở terminal khác:

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

Vite sẽ in URL local và proxy `/api` sang backend.

Kiểm tra trước khi push:

```powershell
cd backend
./mvnw.cmd test
./mvnw.cmd package

cd ..\frontend
npm.cmd install
npm.cmd run lint
npm.cmd run build
```

## 6. Cấu trúc repo

- `backend/`: Java 21, Spring Boot REST API, PostgreSQL, Flyway, Spring Security.
- `frontend/`: React + TypeScript + Vite SPA.
- `frontend/src/api/`: API wrapper.
- `frontend/src/auth/`: AuthContext/session.
- `frontend/src/components/`: shared UI, ProtectedRoute, WorkspaceTabs.
- `frontend/src/pages/`: màn hình chính.
- `frontend/src/styles/redesign.css`, `frontend/tokens.css`: redesign responsive và design tokens.
- `frontend/vercel.json`: rewrite API + SPA fallback.
- `render.yaml`, `backend/Dockerfile`, `backend/render-start.sh`: Render deployment.
- `docs/`: product, architecture, deployment, demo, status và QA.
- `AGENTS.md`: quy tắc kiến trúc, scope và chất lượng.
- `DESIGN.md`: design direction và refinement log.

## 7. Thay đổi production đã hoàn tất

Các commit chính:

- `7f6aaa2` — redesign GroupSync operational workspace.
- `b18d350` — profile/avatar setup không còn chặn người dùng nếu chưa có avatar.
- `07cb3b1` — tinh gọn Badminton organizer workspace.
- `e49cbfb` — hoàn thiện Vietnamese production polish.

Frontend đã có:

- Sidebar desktop, mobile header và bottom navigation.
- Contextual workspace tabs cho group, study và badminton.
- Dashboard command center với lịch, việc cần làm và hoạt động.
- Calendar/availability responsive.
- Badminton venue/court/session operations, lifecycle stepper và organizer toolbox.
- Tournament visual deep green/gold.
- Lucide icons, focus state, reduced-motion và xử lý overflow ngang.
- Đăng ký không ép avatar/profile setup để tránh lỗi permission ở lần dùng đầu.

**Backend files changed trong đợt redesign: 0.** Backend và database logic được giữ nguyên để giảm rủi ro deployment.

## 8. Kiểm thử đã thực hiện

Production flow đã đi qua:

- Mở public URL, login/register.
- Đăng ký và đăng nhập QA account.
- Profile setup và dashboard.
- Tạo Study group, mở group detail, availability.
- Tạo và confirm Study session.
- Tạo Badminton group, venue và court.
- Mở Badminton, Tournament, Notifications và Profile.
- Kiểm tra responsive tại 390, 768, 1024 và 1440px.
- Kiểm tra horizontal overflow, console và network.

Kết quả cuối:

- Vercel production deployment: Ready.
- Render backend: health `UP`.
- `/api/auth/csrf`: HTTP 200.
- `/api/auth/me`: HTTP 200.
- `/api/notifications`: HTTP 200.
- Console smoke test cuối: không có message.
- Frontend build: pass.
- Frontend lint: không có error.

Trong database production hiện có dữ liệu QA do smoke test tạo (Study group và Badminton group). Đây không phải dữ liệu người dùng thật. Nếu cần làm sạch, backup trước; không chạy `reset-demo.ps1` trên production. Không ghi password QA vào bản bàn giao.

## 9. Giới hạn còn biết

- Lint không có error nhưng còn một số warning hook dependency/fast-refresh.
- Vite cảnh báo bundle chính lớn hơn 500 kB; có thể tối ưu lazy route/chunking ở vòng sau.
- Native `datetime-local` của Badminton chưa được xác nhận end-to-end bằng công cụ browser hiện tại; UI khóa submit đúng khi ngày giờ chưa hợp lệ.
- Backend chưa refactor trong đợt redesign. Khi đổi business logic, đọc `AGENTS.md` và viết test backend trước.
- Render Free có cold start.
- Khi đổi domain/account, phải kiểm tra lại Vercel domain ownership và Render CORS.

## 10. Trạng thái local cần bảo toàn

Tại thời điểm bàn giao, Git có thay đổi ngoài commit production:

```text
M scripts/start-groupsync.ps1
```

Đây là thay đổi có sẵn của người dùng, chưa được đưa vào commit redesign. Ngoài ra có QA artifacts chưa track trong `design-work/qa/`. Mở diff trước khi commit; không dùng `git reset --hard` hoặc `git clean -fd` nếu chưa backup.

## 11. Checklist bàn giao

- [ ] Account mới có quyền GitHub repository.
- [ ] Account mới có quyền Vercel project/domain.
- [ ] Account mới có quyền Render Blueprint, service và database.
- [ ] Đã xác nhận database cần giữ hay có thể tạo mới.
- [ ] Secrets được lưu trong password/secret manager, không lưu trong Git.
- [ ] Vercel Root Directory là `frontend`, branch là `main`.
- [ ] `frontend/vercel.json` trỏ đúng backend.
- [ ] Render `APP_CORS_ORIGINS` trỏ đúng Vercel origin.
- [ ] Render health trả `UP`.
- [ ] Register/login và các route chính chạy được desktop/mobile.
- [ ] Đã thay hoặc thu hồi token/mật khẩu account cũ nếu cần.

## 12. Prompt tiếp tục công việc

> Bạn đang tiếp quản dự án GroupSync. Hãy đọc `AGENTS.md`, `README.md`, `docs/HANDOFF_NEW_ACCOUNT.md`, `docs/02_ARCHITECTURE_AND_REPO_STRUCTURE.md`, `docs/DEPLOYMENT_GUIDE.md` và `DESIGN.md` trước khi sửa. Source of truth là branch `main` của GitHub. Frontend deploy trên Vercel, backend + PostgreSQL trên Render. Giữ nguyên kiến trúc React/Vite + Spring Boot/PostgreSQL; không refactor backend hoặc mở rộng scope nếu chưa được yêu cầu. Trước khi code, kiểm tra Git và environment variables; không xóa thay đổi local, không commit secret. Khi đổi frontend, chạy lint/build và kiểm tra responsive cùng primary flow trên public URL. Khi đổi backend/database, chạy test/build backend và kiểm tra health, CORS, CSRF, login/register. Chỉ nói “verified” khi đã kiểm tra thực tế.

## 13. An toàn

- Không đưa password, access token, database URL có password, JWT secret hoặc cookie secret vào chat, GitHub, ảnh chụp hay tài liệu.
- Ưu tiên transfer quyền và giữ database hiện tại trước khi recreate.
- Không chạy `reset-demo.ps1` trên production.
- Không sửa migration Flyway đã applied; tạo migration mới nếu schema cần đổi.
- Sau mỗi deployment, kiểm tra health, CSRF, đăng nhập, một route có dữ liệu và một thao tác ghi dữ liệu.

## 14. Tài liệu liên quan

- [README](../README.md)
- [Deployment Guide](DEPLOYMENT_GUIDE.md)
- [Architecture](02_ARCHITECTURE_AND_REPO_STRUCTURE.md)
- [Implementation Status](IMPLEMENTATION_STATUS.md)
- [Production Smoke Report](PRODUCTION_SMOKE_REPORT.md)
- [Demo Guide](DEMO_GUIDE.md)
- [Design notes](../DESIGN.md)
- [Repository rules](../AGENTS.md)

