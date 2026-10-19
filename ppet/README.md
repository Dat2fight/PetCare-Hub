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
- SQL Server lần đầu: chọn đúng database rỗng; review rồi chạy `backend/src/main/resources/schema-sqlserver.sql`, tiếp theo các migration `001_inventory_nullable_unique.sql`, `002_cart_product_unique.sql`, `003_inventory_generated_unique.sql` trong `backend/src/main/resources/migrations/`, theo thứ tự. Database đã có dữ liệu: dừng writer, backup và review rồi chỉ chạy các migration, không dùng schema/reset để nâng cấp. Không dùng Hibernate update cho production; dùng `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` sau khi áp dụng schema/migration.
- Migration 001 thay UNIQUE nullable bằng filtered unique index và thêm CHECK tồn kho không âm, không xóa hàng; dữ liệu âm làm migration dừng. Không script SQL nào đã được chạy lên database thật trong audit này.
- Migration 002 tạo unique index `(cart_id, product_id) WHERE product_id IS NOT NULL`; có dòng trùng thì rollback/dừng để đối soát thủ công, không tự gộp/xóa. Migration 003 thay cả UNIQUE một cột có tên do Hibernate sinh trên `inventory.pet_id/product_id`, giữ filtered indexes và dữ liệu. Không thêm cột version. Chạy trước khi seed demo hoặc khởi động bản mới; hướng dẫn/giới hạn kiểm chứng tại [báo cáo High lượt 2](docs/high-round-2/REPORT.md).
- `reset-demo-sqlserver.sql` chỉ dành cho dữ liệu demo có thể bỏ: tên DB phải kết thúc `_demo` và session context `allow_petcare_reset=1`. Script chỉ reset; sau đó chạy schema và migration lại. Không dùng trên dữ liệu cần giữ.
- Test SQL Server tùy chọn: Docker phải chạy, đặt `$env:RUN_SQLSERVER_TESTS='true'` rồi `.\mvnw.cmd -Dtest=SqlServerConstraintTest test`. Testcontainers tải image Microsoft SQL Server 2022 và chạy schema/migration hai lần trong container riêng; `acceptLicense()` là cấu hình test nên người vận hành cần đảm bảo đủ quyền dùng image. Test không kết nối SQL Server localhost. Không có Docker thì test bị skip, không phải PASS.
- Seed pets: cấp `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `API_BASE_URL` qua môi trường rồi chạy `node seed_pets.js` từ `ppet`. `ADMIN_EMAIL` hiện phải chứa **username** của admin do contract login hiện tại dùng username. Không đưa giá trị thật vào script, lệnh lưu trong lịch sử hay Git. Thiếu biến thì script dừng; token không được log.
- Giữ package gốc vì còn script seed/deploy; audit cả `ppet/` lẫn `ppet/frontend/`.


## High #23 — cấu hình frontend/API khi triển khai

- Local: sao chép `frontend/.env.example` thành `frontend/.env.local` nếu cần. Mặc định HTTP `/api/v1` và SockJS `/ws`; Vite dev proxy tới backend cổng **8081**. Khởi động lại Vite sau khi đổi môi trường.
- Deploy: đặt `VITE_API_BASE_URL` là URL HTTPS đầy đủ kết thúc `/api/v1`, `VITE_WS_URL` là URL HTTPS SockJS kết thúc `/ws` (không dùng `wss://`). Ví dụ tên miền tài liệu `api.example.invalid` chỉ là chỗ giữ chỗ, phải thay bằng host của nhóm. Biến được nhúng lúc **build**, đổi biến trên server sau build không đổi bundle.
- GitHub Pages: Settings → Secrets and variables → Actions → **Variables**, tạo hai biến trên rồi chạy workflow hiện tại. Workflow dừng nếu thiếu URL HTTPS; giữ bước copy `404.html` và base `/PetCare-Hub/`. Chưa chạy deploy từ phiên sửa này.
- Backend: đặt `APP_CORS_ALLOWED_ORIGINS` bằng origin frontend chính xác (scheme + host + port, không path `/PetCare-Hub/`), nhiều origin phân cách dấu phẩy. HTTP và WebSocket dùng chung property; không dùng wildcard. Reverse proxy phải hỗ trợ `/ws` và nâng cấp WebSocket.
- Tất cả `VITE_*` đều công khai trong bundle. Không đặt JWT_SECRET, mật khẩu database hoặc credential vào các biến này. JWT_SECRET vẫn chỉ được cấp cho tiến trình backend theo hướng dẫn Critical #1; không đổi profile mặc định.
- Kiểm tra sau build/deploy: đăng nhập, gọi `/cart`, refresh `/orders/{id}`, nhận thông báo qua `/user/queue/notifications`; origin khác phải bị từ chối. CONNECT vẫn gửi Bearer token theo hook đã sửa ở lượt 1.


## High #24 — bản vá và mật khẩu

Backend dùng Spring Boot3.3.13 / Spring Security6.3.10 (Java target17). Register/tạo nhân viên: ít nhất8 ký tự, tối đa72 byte UTF-8; login cùng giới hạn trên. Không cắt mật khẩu. Người dùng có mật khẩu legacy vượt giới hạn cần đặt lại qua quy trình quản trị phù hợp. JWT_SECRET và profile giữ nguyên như Critical1. Boot3.3 đã hết OSS support; lên dòng được hỗ trợ là kế hoạch riêng. Kết quả, ma trận quyền và checklist đầy đủ: [High lượt3](docs/high-round-3/REPORT.md).
