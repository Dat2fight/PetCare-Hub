# PetCare-Hub 🐾

Hệ thống quản lý và chăm sóc thú cưng toàn diện (Pet Shop & Pet Care Management Platform). Dự án kết hợp kiến trúc Backend hướng dịch vụ với Frontend SPA hiện đại, phân quyền đa vai trò cho Quản trị viên, Bác sĩ thú y, Nhân viên bán hàng, Nhân viên kho và Khách hàng.

---

## 🛠 Công nghệ sử dụng

| Tầng (Layer) | Công nghệ chính |
| :--- | :--- |
| **Backend** | Java 17+, Spring Boot 3.3.5, Spring Security, Spring Data JPA, WebSocket, JWT (JJWT 0.12.6), Lombok |
| **Frontend** | React 18, TypeScript, Vite 5, Tailwind CSS, Axios, React Router v6 |
| **Cơ sở dữ liệu** | Microsoft SQL Server (MSSQL 2016+) |
| **Tài liệu API** | OpenAPI 3.0 / Swagger UI |

---

## 📁 Cấu trúc thư mục dự án

```text
PetCare-Hub/
├── ppet/
│   ├── backend/                     # Mã nguồn Backend Spring Boot
│   │   ├── mvnw & mvnw.cmd          # Maven Wrapper
│   │   ├── pom.xml
│   │   └── src/main/
│   │       ├── java/com/petcarehub/ # Controllers, Services, Entities, Security
│   │       └── resources/
│   │           ├── application.yml
│   │           ├── application-dev.yml
│   │           ├── schema-sqlserver.sql  # Script tạo 32 bảng và quan hệ
│   │           └── data-sqlserver.sql    # Script dữ liệu mẫu (Roles, Users, Products...)
│   ├── frontend/                    # Mã nguồn Frontend React SPA
│   │   ├── package.json
│   │   ├── vite.config.ts
│   │   └── src/                     # Components, Pages, Services, Contexts
│   └── docs/                        # Tài liệu đặc tả hệ thống
└── README.md
```

---

## 📋 Yêu cầu môi trường (Prerequisites)

1. **Java JDK 17** (Khuyến nghị: JDK 17 hoặc JDK 21 LTS).
   > **Lưu ý:** Dự án sử dụng Lombok với Spring Boot 3.3.5, tương thích tốt nhất với **JDK 17/21**. Nếu máy tính của bạn cài đặt phiên bản Java quá mới (như JDK 25, 27) làm mặc định trong `PATH`, hãy cấu hình biến môi trường `JAVA_HOME` trỏ về JDK 17 khi biên dịch backend.
2. **Node.js** phiên bản **18.x** trở lên và **npm** (đi kèm Node.js).
3. **Microsoft SQL Server** (2016 trở lên, SQL Server Express hoặc Developer Edition) đang chạy trên cổng mặc định **1433**.
4. **SQL Server Management Studio (SSMS)**, **Azure Data Studio**, hoặc công cụ `sqlcmd` để quản lý database.

---

## 🚀 Hướng dẫn khởi động & kết nối hệ thống (Từng bước)

### Bước 1: Khởi tạo Cơ sở dữ liệu (Microsoft SQL Server)

1. Mở **SSMS**, **Azure Data Studio** hoặc cửa sổ dòng lệnh có `sqlcmd`.
2. Kết nối vào SQL Server instance của bạn (`localhost:1433`).
3. Tạo cơ sở dữ liệu `petcarehub`:
   ```sql
   CREATE DATABASE petcarehub;
   GO
   ```
4. Mở và thực thi lần lượt 2 file script theo đúng thứ tự:
   - **File 1 (Tạo bảng):** `ppet/backend/src/main/resources/schema-sqlserver.sql`
   - **File 2 (Nạp dữ liệu mẫu):** `ppet/backend/src/main/resources/data-sqlserver.sql`

   *(Nếu dùng `sqlcmd` trên Windows PowerShell, bạn có thể chạy nhanh:)*
   ```powershell
   sqlcmd -S localhost -E -C -d petcarehub -i "ppet\backend\src\main\resources\schema-sqlserver.sql"
   sqlcmd -S localhost -E -C -d petcarehub -i "ppet\backend\src\main\resources\data-sqlserver.sql"
   ```

5. Kiểm tra thông tin đăng nhập trong file `ppet/backend/src/main/resources/application-dev.yml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:sqlserver://localhost:1433;databaseName=petcarehub;encrypt=true;trustServerCertificate=true
       username: ${DB_USERNAME:sa}
       password: ${DB_PASSWORD:1804}
   ```
   > Nếu tài khoản `sa` trên máy bạn có mật khẩu khác `1804`, bạn có thể sửa trực tiếp trong file trên hoặc đặt biến môi trường `$env:DB_PASSWORD="mat_khau_cua_ban"` trước khi chạy backend.

---

### Bước 2: Khởi động Backend (Spring Boot)

**Bắt buộc cấp `JWT_SECRET` trước khi chạy**, kể cả profile dev. Không còn khóa mặc
định; thiếu/khóa ngắn/`CHANGE_ME`/khóa từng công khai sẽ làm ứng dụng dừng khởi động.
Xem [cách sinh và cấp khóa JWT](ppet/README.md#khóa-ký-jwt-bắt-buộc) và
[báo cáo Critical #1](ppet/docs/critical-jwt-secret/REPORT.md). Không đặt khóa trong frontend.

1. Mở một terminal mới tại thư mục gốc của dự án (`PetCare-Hub`).
2. Di chuyển vào thư mục backend:
   ```bash
   cd ppet/backend
   ```
3. *(Tùy chọn)* Nếu máy bạn cài nhiều JDK và `java -version` không phải là JDK 17/21, hãy gán `JAVA_HOME`:
   - **Windows PowerShell:**
     ```powershell
     $env:JAVA_HOME = "C:\Users\<Tên_User>\.jdks\ms-17.0.20.1"  # Hoặc đường dẫn JDK 17 trên máy bạn
     $env:Path = "$env:JAVA_HOME\bin;$env:Path"
     ```
   - **Linux / macOS:**
     ```bash
     export JAVA_HOME=/path/to/jdk-17
     export PATH=$JAVA_HOME/bin:$PATH
     ```
4. Khởi chạy ứng dụng bằng Maven Wrapper:
   - **Windows:**
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```
   - **Linux / macOS:**
     ```bash
     ./mvnw spring-boot:run
     ```
5. Khi thấy thông báo:
   ```text
   Tomcat started on port 8081 (http) with context path '/'
   Started PetCareHubApplication in ... seconds
   ```
   Backend đã sẵn sàng!
   - **Cổng Backend:** `http://localhost:8081`
   - **Tài liệu Swagger UI:** `http://localhost:8081/swagger-ui.html`

---

### Bước 3: Khởi động Frontend (React / Vite)

1. Mở một terminal **khác** tại thư mục gốc của dự án (`PetCare-Hub`).
2. Di chuyển vào thư mục frontend:
   ```bash
   cd ppet/frontend
   ```
3. Cài đặt các gói phụ thuộc (chỉ cần làm ở lần đầu tiên):
   ```bash
   npm install
   ```
4. Khởi chạy máy chủ phát triển Vite:
   ```bash
   npm run dev
   ```
5. Mở trình duyệt và truy cập theo đường dẫn:
   👉 **`http://localhost:5173/PetCare-Hub/`**

   > **Lưu ý quan trọng về URL:** Do cấu hình `base: '/PetCare-Hub/'` trong `vite.config.ts`, bạn cần truy cập đúng đường dẫn `http://localhost:5173/PetCare-Hub/`.
   > Trình duyệt sẽ tự động kết nối API với backend thông qua Vite Proxy (`/api` -> `http://localhost:8081`).

---

## 👥 Tài khoản thử nghiệm (Mặc định)

Tất cả các tài khoản mẫu dưới đây đều có mật khẩu mặc định là: **`admin123`**

| Tên đăng nhập (Username) | Email | Vai trò (Role) | Chức năng chính |
| :--- | :--- | :--- | :--- |
| **`admin`** | `admin@petcarehub.com` | **ADMIN** | Quản trị toàn quyền hệ thống |
| **`sales01`** | `sales@petcarehub.com` | **SALES_STAFF** | Tư vấn bán hàng, quản lý đơn hàng |
| **`warehouse01`** | `warehouse@petcarehub.com` | **WAREHOUSE_STAFF** | Quản lý kho, tồn kho, xuất nhập |
| **`vet01`** | `vet@petcarehub.com` | **VETERINARIAN** | Khám bệnh, lịch tiêm chủng, hồ sơ y tế |
| **`customer01`** | `customer@gmail.com` | **CUSTOMER** | Mua sắm sản phẩm, đặt lịch dịch vụ, quản lý thú cưng |

---

## 🔧 Xử lý sự cố thường gặp (Troubleshooting)

1. **Lỗi `cannot find symbol` liên quan đến Getters/Setters khi chạy backend:**
   - **Nguyên nhân:** Phiên bản Java mặc định trong terminal quá mới (ví dụ JDK 25 hoặc 27) chưa tương thích hoàn toàn với bộ xử lý annotation của Lombok.
   - **Cách khắc phục:** Cài đặt JDK 17 (khuyên dùng Microsoft OpenJDK 17 hoặc Eclipse Temurin 17) và trỏ `JAVA_HOME` đến JDK 17 trước khi chạy `mvnw`.

2. **Lỗi `SchemaManagementException: Schema-validation: missing table...`:**
   - **Nguyên nhân:** File cấu hình `application-dev.yml` đang bật `ddl-auto: validate`, yêu cầu các bảng trong database phải tồn tại sẵn.
   - **Cách khắc phục:** Chạy đầy đủ 2 file `schema-sqlserver.sql` và `data-sqlserver.sql` vào database `petcarehub` trước khi khởi động Spring Boot.

3. **Lỗi kết nối SQL Server (`Login failed for user 'sa'` hoặc cổng 1433 không kết nối được):**
   - Đảm bảo service **SQL Server (MSSQLSERVER)** đang chạy trong `services.msc`.
   - Mở **SQL Server Configuration Manager** -> **SQL Server Network Configuration** -> **Protocols for MSSQLSERVER** -> Bật **TCP/IP** (Enabled: Yes) và đảm bảo cổng là 1433.
   - Đảm bảo SQL Server đang ở chế độ **SQL Server and Windows Authentication mode**.
   - Kiểm tra lại mật khẩu user `sa` trong `application-dev.yml`.

4. **Trang web trắng tinh hoặc báo lỗi 404 khi mở `http://localhost:5173`:**
   - Hãy kiểm tra xem bạn đã thêm tiền tố `/PetCare-Hub/` vào URL chưa: `http://localhost:5173/PetCare-Hub/`.
