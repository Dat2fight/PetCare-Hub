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

```bash
cd backend
./mvnw spring-boot:run
# hoặc trên Windows: .\mvnw.cmd spring-boot:run
```

The API will be available at `http://localhost:8081` (Swagger UI at `http://localhost:8081/swagger-ui.html`).

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
