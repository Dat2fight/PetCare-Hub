# PetCare Hub

A professional pet shop management platform that transforms traditional pet sales and care into a data-driven, responsible, and accessible ecosystem.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 17+, Spring Boot 3.3, Spring MVC, Spring Data JPA, Spring Security, WebSocket |
| **Frontend** | React 18, TypeScript, Tailwind CSS, Vite |
| **Database** | SQL Server (MSSQL) |
| **Testing** | JUnit 5, Mockito, Playwright |

## Project Structure

```
ppet/
├── backend/          # Spring Boot application
│   └── src/
│       ├── main/java/com/petcarehub/
│       └── main/resources/
├── frontend/         # React SPA
│   └── src/
│       ├── api/
│       ├── components/
│       ├── contexts/
│       ├── hooks/
│       ├── pages/
│       ├── services/
│       ├── types/
│       └── utils/
└── docs/             # Project documentation
    ├── requirements.md
    ├── architecture.md
    ├── database.md
    ├── api.md
    ├── design-patterns.md
    └── development-roadmap.md
```

## Prerequisites

- Java 17 or higher
- Maven 3.9+
- Node.js 18+ and npm
- SQL Server 2016+ (or SQL Server Express)

## Getting Started

### Database Setup

1. Open SQL Server Management Studio (SSMS) or Azure Data Studio.
2. Connect to your SQL Server instance (ensure TCP/IP is enabled on port 1433).
3. Create a new database named `petcarehub`:
   ```sql
   CREATE DATABASE petcarehub;
   ```
4. Open `backend/src/main/resources/application.yml` and `application-dev.yml`. Update the `password` field for the `sa` user to match your actual SQL Server `sa` password.

### Backend

Mặc định không còn bật profile `dev`. Để chạy local với cấu hình dev, đặt
`SPRING_PROFILES_ACTIVE=dev` trong môi trường rồi chạy lệnh bên dưới. Profile dev
chỉ seed danh mục/demo khi DB chưa có species; không tạo tài khoản admin.

Bootstrap admin riêng: cấp `BOOTSTRAP_ADMIN_ENABLED=true`, `BOOTSTRAP_ADMIN_USERNAME`,
`BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD` qua môi trường/secret store trước
khi khởi động. Mật khẩu tối thiểu 12 ký tự, tối đa 72 byte UTF-8; không có mặc định.
Sau lần đầu, bỏ các biến bootstrap. Chạy lại không đổi mật khẩu/quyền/trạng thái của
admin hiện có; username trùng CUSTOMER sẽ bị từ chối. Bootstrap không phụ thuộc dev.

`data-sqlserver.sql` chỉ là demo tùy chọn: dùng SQLCMD mode với `APP_PROFILE=dev`
và `DEMO_PASSWORD_HASH` (BCrypt của mật khẩu demo do bạn tự cấp) từ môi trường.
Script có `:On Error exit`, yêu cầu bảng users/roles rỗng, không tạo admin.
Nếu chọn SQL demo, thực hiện sau schema/migration và **trước** khi khởi động app/bootstrap;
nếu chọn Java demo thì không chạy thêm SQL demo. Không đưa mật khẩu/hash vào Git hay
tham số lệnh lưu lịch sử. Không chạy seed lên database nghiệp vụ.

API tạo nhân viên: ADMIN gọi `POST /api/v1/admin/staff` với các field đăng ký hiện có
và role `SALES_STAFF`, `WAREHOUSE_STAFF` hoặc `VETERINARIAN`. Public register luôn tạo CUSTOMER.

#### Khóa ký JWT bắt buộc

Trước khi chạy backend, cấp **`JWT_SECRET`** qua môi trường của tiến trình hoặc secret
store. Property duy nhất là `app.jwt.secret`; mọi profile chạy thật, kể cả dev, đều
không có fallback. Thiếu, rỗng, chỉ khoảng trắng, `CHANGE_ME`, dưới **32 byte UTF-8**
hoặc trùng một khóa đã từng công khai đều bị từ chối khi tạo JwtTokenProvider.

Code dùng nguyên byte UTF-8 của chuỗi, **không decode Base64**. JJWT chọn HS256 ở
32–47 byte, HS384 ở 48–63 byte, HS512 từ 64 byte. Khuyến nghị sinh **64 byte ngẫu nhiên**
rồi biểu diễn Base64 như bên dưới; không dùng mật khẩu tự nghĩ hoặc giá trị mẫu.

1. Sinh và gán khóa mà không in kết quả ra console. **PowerShell**:
   ```powershell
   $jwtBytes = New-Object byte[] 64
   $jwtRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
   try { $jwtRng.GetBytes($jwtBytes) } finally { $jwtRng.Dispose() }
   $env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
   ```
   **Bash với OpenSSL**:
   ```bash
   export JWT_SECRET="$(openssl rand -base64 64 | tr -d '\r\n')"
   ```
   Đây chỉ là lệnh hướng dẫn, không có khóa thật trong tài liệu. Giữ khóa ổn định
   trong secret store cho môi trường đó; chạy lệnh sinh lại sẽ thay khóa và hủy phiên cũ.
2. Chạy backend trong **cùng terminal/tiến trình được cấp biến** bằng lệnh bên dưới.
   Chọn `SPRING_PROFILES_ACTIVE=dev` riêng nếu cần dev. Spring Boot không tự đọc `.env`:
   `backend/.env.example` chỉ mô tả biến, `CHANGE_ME` cố ý không chạy được. Nếu dùng
   `.env`, phải có IDE/launcher nạp biến vào tiến trình; không commit file chứa khóa.
3. Trên server, cấp `JWT_SECRET` qua secret manager/cấu hình môi trường của service;
   không đưa vào command-line argument, image, Git, hoặc biến `VITE_*`. Các replica
   cùng môi trường cần cùng khóa. Dừng/restart **tất cả** instance khi đổi khóa;
   người dùng đăng nhập lại, không giữ instance cũ tiếp tục chấp nhận khóa đã lộ.
4. CI hiện chỉ build frontend nên không cần secret JWT. Maven test dùng khóa giả ngẫu
   nhiên tại `src/test/resources/application-test.yml`, không cần production secret.
   Nếu thêm job chạy backend thật, cấp env từ GitHub Actions Secrets:
   ```yaml
   env:
     JWT_SECRET: ${{ secrets.JWT_SECRET }}
   ```
   Không echo biến, không bật shell tracing. Artifact chạy thật không chứa profile test.
5. Kiểm tra fail-fast trên môi trường thử: bỏ biến JWT_SECRET hoặc đặt `CHANGE_ME`,
   khởi động phải thất bại và thông báo tên biến, không hiện giá trị. Cấp khóa hợp lệ
   rồi khởi động lại, đăng nhập và thử token trước khi đổi khóa: token cũ phải bị từ chối.

Khóa đã có trong lịch sử Git phải coi là lộ vĩnh viễn. Không tái sử dụng; code không
tự thay secret trên server hay kiểm tra việc triển khai trước đây có bị lạm dụng.
Chi tiết và checklist: [báo cáo Critical #1](docs/critical-jwt-secret/REPORT.md).

```bash
cd backend
./mvnw spring-boot:run
# hoặc trên Windows: .\mvnw.cmd spring-boot:run
```

The API will be available at `http://localhost:8081` (Swagger UI at `http://localhost:8081/swagger-ui.html`).

WebSocket thông báo dùng SockJS `/ws`. Cấp danh sách origin chính xác, phân cách bằng
dấu phẩy qua `APP_CORS_ALLOWED_ORIGINS` (mặc định `http://localhost:5173`); không dùng
wildcard. STOMP CONNECT phải gửi header `Authorization: Bearer <JWT>`, sau đó subscribe
`/user/queue/notifications`. Topic `/topic/notifications/{id}` cũ và mọi SEND từ client
đều bị từ chối. Frontend trang thông báo dùng hook đã cập nhật, vẫn giữ polling dự phòng.
Khi triển khai khác origin, reverse proxy cần chuyển `/api` và `/ws` tới backend.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The app will be available at `http://localhost:5173/PetCare-Hub/`.


## Documentation

See the [docs/](docs/) directory for:
- [Requirements Specification](docs/requirements.md)
- [System Architecture](docs/architecture.md)
- [Database Schema](docs/database.md)
- [REST API Specification](docs/api.md)
- [Design Patterns](docs/design-patterns.md)
- [Development Roadmap](docs/development-roadmap.md)

## License

This project is for educational purposes.


## Audit #25–58: vận hành và kiểm chứng

Chi tiết từng lỗi, ma trận quyền và checklist: [báo cáo audit](docs/audit-25-58/REPORT.md).

- Backend: `cd backend` rồi `.\mvnw.cmd test` (Java 17+). Profile test dùng H2, không dùng database nghiệp vụ.
- Frontend: `cd frontend`, `npm ci`, `npm run lint`, `npx tsc --noEmit`, `npm run build`. Node phải đáp ứng engines của Vite 7 (20.19+ hoặc 22.12+). Đã kiểm tra dev server trên localhost.
- Deploy giữ BrowserRouter, basename lấy từ Vite base `/PetCare-Hub/`; workflow copy `dist/index.html` thành `dist/404.html`. Khi đổi tên repo/host phải đổi base. Refresh URL con trên GitHub Pages vẫn cần smoke test sau deploy; phản hồi HTTP của Pages có thể là 404 dù SPA hiển thị đúng.
- SQL Server lần đầu: chọn đúng database rỗng; review rồi chạy `backend/src/main/resources/schema-sqlserver.sql`, tiếp theo `migrations/001_inventory_nullable_unique.sql`. Schema chỉ tạo đối tượng chưa có; không nâng cấp cấu trúc bảng đã tồn tại. Các thay đổi cấu trúc sau này phải nằm trong migration riêng được review/backup, không dùng Hibernate update cho production.
- Migration 001 thay UNIQUE nullable bằng filtered unique index và thêm CHECK tồn kho không âm, không xóa hàng; dữ liệu âm làm migration dừng. Không script SQL nào đã được chạy lên database thật trong audit này.
- `reset-demo-sqlserver.sql` chỉ dành cho dữ liệu demo có thể bỏ: tên DB phải kết thúc `_demo` và session context `allow_petcare_reset=1`. Script chỉ reset; sau đó chạy schema và migration lại. Không dùng trên dữ liệu cần giữ.
- Test SQL Server tùy chọn: Docker phải chạy, đặt `$env:RUN_SQLSERVER_TESTS='true'` rồi `.\mvnw.cmd -Dtest=SqlServerConstraintTest test`. Testcontainers tải image Microsoft SQL Server 2022 và chạy schema/migration hai lần trong container riêng; `acceptLicense()` là cấu hình test nên người vận hành cần đảm bảo đủ quyền dùng image. Test không kết nối SQL Server localhost. Không có Docker thì test bị skip, không phải PASS.
- Seed pets: cấp `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `API_BASE_URL` qua môi trường rồi chạy `node seed_pets.js` từ `ppet`. `ADMIN_EMAIL` hiện phải chứa **username** của admin do contract login hiện tại dùng username. Không đưa giá trị thật vào script, lệnh lưu trong lịch sử hay Git. Thiếu biến thì script dừng; token không được log.
- Giữ package gốc vì còn script seed/deploy; audit cả `ppet/` lẫn `ppet/frontend/`.
