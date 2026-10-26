# Chạy PetCare-Hub trên Windows

Tài liệu vận hành chính của repository. Các lệnh PowerShell bên dưới dùng checkout `D:\PetCare-Hub`; đổi đường dẫn nếu bạn clone ở nơi khác. Mỗi khối ghi rõ terminal/thư mục sử dụng.

**Đã kiểm chứng ngày 19/10/2026:** Java 25.0.2, Node 24.21.0, npm 11.19.0, Maven Wrapper 3.9.9, SQL Server 17.0.1145.4 Standard Developer Edition; backend dev và frontend chạy trên database có sẵn. Test/build/lint và xác thực API pass. [Báo cáo đầy đủ](docs/local-run-20261019.md).

Các mục tạo database mới, cài/bật SQL Server, bootstrap admin, seed mới và Bash là hướng dẫn từ cấu hình/mã nguồn, **chưa thực thi trong phiên này**. Không xem chúng là bằng chứng đã kiểm thử trên database mới. Luồng đặt lịch cần kiểm tra tay riêng.

## Mục lục

1. [Kiến trúc và thư mục](#1-kiến-trúc-và-thư-mục)
2. [Yêu cầu môi trường](#2-yêu-cầu-môi-trường)
3. [Lấy mã nguồn](#3-lấy-mã-nguồn)
4. [Chuẩn bị database](#4-chuẩn-bị-database)
5. [Biến môi trường và JWT](#5-biến-môi-trường-và-jwt)
6. [Chạy backend](#6-chạy-backend)
7. [Chạy frontend](#7-chạy-frontend)
8. [Kiểm tra nhanh](#8-kiểm-tra-nhanh)
9. [Test, build và lint](#9-test-build-và-lint)
10. [Khắc phục sự cố](#10-khắc-phục-sự-cố)
11. [Dừng dịch vụ](#11-dừng-dịch-vụ)
12. [Bảo mật](#12-bảo-mật)
13. [Tài liệu và trạng thái sửa lỗi](#13-tài-liệu-và-trạng-thái-sửa-lỗi)

## 1. Kiến trúc và thư mục

React 18/TypeScript/Vite 7 gọi REST API Spring Boot 3.3.13; backend dùng JPA kết nối SQL Server. Xác thực bằng JWT, thông báo dùng SockJS/STOMP.

| Thành phần | Cổng / URL mặc định |
|---|---|
| Frontend | http://localhost:5173/PetCare-Hub/ |
| Backend (context path /) | http://localhost:8081 |
| API | http://localhost:8081/api/v1 |
| SockJS | http://localhost:8081/ws |
| SQL Server | localhost:1433, database petcarehub |

```text
PetCare-Hub/
  README.md                 Điểm vào ngắn
  .github/workflows/deploy.yml
  ppet/
    README.md               Hướng dẫn này
    backend/
      mvnw.cmd              Maven Wrapper cho Windows
      pom.xml
      .env.example          Chỉ mô tả biến, không tự được nạp
      src/main/resources/
        application.yml
        application-dev.yml
        schema-sqlserver.sql
        data-sqlserver.sql
        reset-demo-sqlserver.sql
        migrations/         001 → 002 → 003
      src/test/resources/application-test.yml
    frontend/
      package.json
      vite.config.ts
      .env.example
      src/api/client.ts
    docs/
```

## 2. Yêu cầu môi trường

- JDK: pom target Java 17. Phiên này compile/run thành công bằng **Java 25.0.2**; không cần đổi JDK. Test Mockito/ByteBuddy trên Java 25 cần cờ ở mục 9.
- Node: Vite đang cài yêu cầu `^20.19.0 || >=22.12.0`; đã dùng **24.21.0**.
- npm đã dùng **11.19.0**; frontend đã có node_modules nên không cài lại dependency.
- Maven Wrapper đã tải Maven **3.9.9**; không bắt buộc Maven toàn cục. Lần đầu wrapper/dependency cần mạng.
- SQL Server đang chạy, TCP/IP 1433. Máy này dùng SQL Server **17.0.1145.4**; schema ghi mục tiêu SQL Server 2016+, chưa thử các phiên bản khác trong phiên này.
- `sqlcmd` và tùy chọn SSMS để quản trị database.

Kiểm tra từ thư mục gốc:

```powershell
Set-Location D:\PetCare-Hub
java -version
node -v
npm -v
.\ppet\backend\mvnw.cmd -v
Get-Service MSSQLSERVER
Test-NetConnection localhost -Port 1433
```

Nếu thiếu: cài JDK, Node/npm và SQL Server Developer/Express từ nhà cung cấp, thêm PATH rồi mở terminal mới. Không cần cài Maven riêng. Nếu SQL Server chưa bật TCP/IP: SQL Server Configuration Manager → SQL Server Network Configuration → Protocols for instance → TCP/IP → Enable; IPAll đặt TCP Port 1433, xóa Dynamic Ports nếu muốn cổng cố định; restart đúng dịch vụ SQL Server bằng quyền quản trị. Named instance có tên service khác MSSQLSERVER.

Backend hiện dùng SQL Authentication. Trong SSMS, bật “SQL Server and Windows Authentication mode” nếu cần và restart instance. Security → Logins → New Login: chọn login riêng, tự nhập mật khẩu mạnh, map user vào đúng database, cấp quyền ứng dụng cần dùng. Dùng tài khoản quản trị riêng để tạo schema/migration; không dùng sa cho triển khai thật. Phiên này dùng login sa đã có, không tạo login hay thay đổi thiết lập server.

## 3. Lấy mã nguồn

Máy đã có checkout thì chỉ chuyển thư mục và kiểm tra nhánh:

```powershell
Set-Location D:\PetCare-Hub
git branch --show-current
git status --short
```

Máy mới có thể clone (không chạy lại nếu thư mục đã tồn tại):

```powershell
Set-Location D:\
git clone https://github.com/Dat2fight/PetCare-Hub
Set-Location D:\PetCare-Hub
git switch fix/critical-jwt-secret
```

Lệnh clone chưa được thực thi lại trong phiên kiểm chứng. Không tự chuyển nhánh khi đang có thay đổi chưa lưu.

## 4. Chuẩn bị database

### Database có sẵn — cách đã dùng

Đọc bằng Windows Authentication, không truyền mật khẩu vào câu lệnh:

```powershell
Set-Location D:\PetCare-Hub
sqlcmd -S localhost,1433 -E -C -b -Q "SET NOCOUNT ON; SELECT name FROM sys.databases WHERE name='petcarehub';"
sqlcmd -S localhost,1433 -E -C -b -d petcarehub -Q "SET NOCOUNT ON; SELECT COUNT(*) AS table_count FROM sys.tables; SELECT COUNT(*) AS users_count FROM dbo.users; SELECT COUNT(*) AS species_count FROM dbo.species;"
```

`-E` dùng Windows login hiện tại; `-C` chấp nhận chứng chỉ local; `-b` trả lỗi khi SQL thất bại. Nếu Windows login không có quyền, nhờ người quản trị cấp quyền hoặc dùng SSMS với tài khoản được cấp. Backend dùng SQL login là một kết nối khác, không được suy ra rằng `-E` thành công thì backend chắc chắn đăng nhập được.

Phiên này có 32 bảng, 5 users trước smoke test, 6 species. Không chạy lại schema/seed/reset. Profile dev dùng `ddl-auto: validate`, không tự tạo bảng.

Database máy kiểm chứng còn `UQ_inventory_pet`, `UQ_inventory_product` không filter, chưa có filtered index của migrations 001–003. App vẫn validate/start được nhưng điều đó **không chứng minh** các ràng buộc inventory/cart đã đúng. Trước khi kiểm thử đầy đủ hoặc triển khai: backup, dừng writer, review dữ liệu và áp dụng migration có kiểm soát. Phiên này không sửa database đó.

### Database mới — thứ tự chuẩn bị, chưa thực thi trong phiên

Chỉ dùng database **mới/rỗng**. Ví dụ tạo database riêng để không đụng dữ liệu cũ:

```powershell
Set-Location D:\PetCare-Hub
sqlcmd -S localhost,1433 -E -C -b -Q "IF DB_ID(N'petcarehub_local_new') IS NOT NULL THROW 50000, 'Database exists; stop and inspect it', 1; CREATE DATABASE petcarehub_local_new;"
if ($LASTEXITCODE -ne 0) { throw 'Không tạo database; dừng tại đây' }
$dbName = 'petcarehub_local_new'
$sqlRoot = Join-Path (Get-Location) 'ppet\backend\src\main\resources'
$scripts = @(
    'schema-sqlserver.sql',
    'migrations\001_inventory_nullable_unique.sql',
    'migrations\002_cart_product_unique.sql',
    'migrations\003_inventory_generated_unique.sql'
)
foreach ($script in $scripts) {
    sqlcmd -S localhost,1433 -E -C -b -d $dbName -i (Join-Path $sqlRoot $script)
    if ($LASTEXITCODE -ne 0) { throw "SQL thất bại tại $script; không chạy tiếp" }
}
```

Đổi `databaseName` trong `SPRING_DATASOURCE_URL` ở mục 6 sang database vừa tạo. Không chạy cả `SQLQuery3.sql` và quy trình trên: SQLQuery3.sql là script tạo petcarehub riêng, không nằm trong quy trình này.

- `schema-sqlserver.sql`: tạo bảng, không phải công cụ nâng cấp database có sẵn; không chạy lại.
- `001`: filtered UNIQUE inventory và CHECK quantity không âm.
- `002`: filtered UNIQUE cart/product; dừng nếu có dòng trùng.
- `003`: thay các UNIQUE inventory không filter do Hibernate trước đây tạo; có DROP constraint/index, không DROP bảng.
- `reset-demo-sqlserver.sql`: **DROP TABLE, mất dữ liệu**; không dùng cho hướng dẫn chạy thường ngày.
- Database có dữ liệu: chỉ xem xét migration sau backup/review/dừng writer; không chạy schema/reset để “sửa nhanh”. Dữ liệu âm/trùng phải được đối soát, không tự xóa.

Chọn **một** cách seed:

1. Java `DataSeeder`: bật profile dev, khi species rỗng sẽ thêm roles, species/breeds, product categories, services, care packages. Không tạo admin, user mẫu hay sản phẩm. Species có dữ liệu thì bỏ qua toàn bộ seed; không có biến `SEED_DEMO`.
2. SQL demo tùy chọn: schema → 001 → 002 → 003 → `data-sqlserver.sql` → chạy backend/bootstrap. Cần SQLCMD mode, `APP_PROFILE=dev`, `DEMO_PASSWORD_HASH` là BCrypt của mật khẩu do bạn tự cấp; users/roles phải rỗng. Không tạo admin. Không dùng thêm SQL demo sau Java seed. Chưa chạy phương án này trong phiên; không có mật khẩu/hash mẫu để sao chép.

## 5. Biến môi trường và JWT

| Tên | Bắt buộc? | Ý nghĩa | Ví dụ |
|---|---|---|---|
| JWT_SECRET | Có, mọi profile chạy thật | Khóa ký JWT, ít nhất 32 byte UTF-8 | CHANGE_ME (cố ý không hợp lệ) |
| SPRING_PROFILES_ACTIVE | Có cho quy trình local này | Bật cấu hình validate và Java seed dev | dev |
| DB_USERNAME | Có | Tài khoản SQL Server được cấp quyền phù hợp | Cấp qua môi trường |
| DB_PASSWORD | Có | Mật khẩu SQL Server, không có fallback | Cấp qua môi trường |
| DB_URL / SPRING_DATASOURCE_URL | Khi đổi host/database | JDBC URL; Spring override vẫn được hỗ trợ | Xem backend/.env.example |
| APP_CORS_ALLOWED_ORIGINS | Khi frontend khác origin mặc định | Origin chính xác, phân cách dấu phẩy | http://localhost:5173 |
| BOOTSTRAP_ADMIN_ENABLED | Không, mặc định false | Bật tạo admin khi khởi động | false |
| BOOTSTRAP_ADMIN_USERNAME | Khi bật bootstrap | Username admin mới | CHANGE_ME |
| BOOTSTRAP_ADMIN_EMAIL | Khi bật bootstrap | Email chưa dùng | CHANGE_ME |
| BOOTSTRAP_ADMIN_PASSWORD | Khi bật bootstrap | Ít nhất 12 ký tự, tối đa 72 byte UTF-8 | CHANGE_ME |
| SPRING_JPA_SHOW_SQL | Không | Tắt SQL log khi smoke test | false |
| LOGGING_LEVEL_COM_PETCAREHUB | Không | Giảm debug local | INFO |
| LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY | Không | Giảm debug security | INFO |
| VITE_API_BASE_URL | Không với proxy local | HTTP API, được nhúng khi build | /api/v1 |
| VITE_WS_URL | Không với proxy local | SockJS HTTP URL | /ws |
| APP_PROFILE / DEMO_PASSWORD_HASH | Chỉ SQL demo | SQLCMD seed guard / hash do bạn cấp | dev / CHANGE_ME |

Mọi profile runtime bắt buộc cấp DB_USERNAME, DB_PASSWORD và JWT_SECRET. Không có mật khẩu fallback. Không sửa credential trực tiếp trong YAML; xem [hướng dẫn cấu hình và rotate](docs/qa-critical-001-007/SECRET-OPERATIONS.md).

### Khóa ký JWT bắt buộc

Sinh **64 byte ngẫu nhiên**, biểu diễn Base64, không in giá trị:

```powershell
$jwtBytes = New-Object byte[] 64
$jwtRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try { $jwtRng.GetBytes($jwtBytes) } finally { $jwtRng.Dispose() }
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
```

Bash tương đương, cần OpenSSL (chưa chạy trên máy Windows trong phiên này):

```bash
export JWT_SECRET="$(openssl rand -base64 64 | tr -d '\r\n')"
```

Code dùng byte UTF-8 của chuỗi, không decode Base64. Thiếu/rỗng/placeholder/khóa ngắn/khóa từng công khai bị từ chối và app cố ý dừng. Không dùng lại khóa cũ đã lộ. Sinh khóa mới sẽ làm token cũ không còn hợp lệ; đăng nhập lại.

**Spring Boot không tự đọc .env.** File .env.example chỉ mô tả; copy thành .env không làm backend nhận biến. Cần IDE/launcher nạp biến vào chính tiến trình Java, hoặc đặt trong cùng terminal chạy Maven. Không echo biến, không in môi trường, không đưa khóa vào VITE_*.

## 6. Chạy backend

Terminal A, PowerShell, tại gốc repository. Phải sinh JWT theo mục 5 **trong cùng terminal** trước lệnh Maven.

```powershell
Set-Location D:\PetCare-Hub
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:SPRING_DATASOURCE_URL = 'jdbc:sqlserver://localhost:1433;databaseName=petcarehub;encrypt=true;trustServerCertificate=true'
$env:DB_USERNAME = Read-Host 'SQL login'
$dbCredential = Read-Host 'Mật khẩu SQL login' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $dbCredential).Password
$env:APP_CORS_ALLOWED_ORIGINS = 'http://localhost:5173'
$env:BOOTSTRAP_ADMIN_ENABLED = 'false'
$env:SPRING_JPA_SHOW_SQL = 'false'
$env:LOGGING_LEVEL_COM_PETCAREHUB = 'INFO'
$env:LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY = 'INFO'
Set-Location .\ppet\backend
.\mvnw.cmd spring-boot:run
```

Không nhập mật khẩu bằng literal lưu trong history. Kiểm tra URL nếu chọn database mới. `Read-Host` là cách nhập cho người dùng; trong phiên kiểm chứng đã cấp credential cấu hình sẵn qua môi trường mà không hiển thị.

Dấu hiệu thành công đã quan sát:

```text
Tomcat started on port 8081 (http) with context path '/'
Started PetCareHubApplication in ... seconds
Database already seeded. Skipping.
```

Kiểm tra thêm API sau log Started, vì các runner seed/bootstrap có thể còn chạy:

```powershell
(Invoke-WebRequest 'http://localhost:8081/api/v1/products').StatusCode
```

Kết quả phải 200. Không mặc định có endpoint health/Actuator.

### Admin bootstrap tùy chọn

Phiên kiểm chứng **không bật bootstrap**, không tạo/reset admin. Nếu cần admin mới, trước khi chạy Maven ở trên:

```powershell
$env:BOOTSTRAP_ADMIN_ENABLED = 'true'
$env:BOOTSTRAP_ADMIN_USERNAME = Read-Host 'Username admin mới'
$env:BOOTSTRAP_ADMIN_EMAIL = Read-Host 'Email admin mới'
$adminCredential = Read-Host 'Mật khẩu admin (>=12 ký tự, <=72 byte UTF-8)' -AsSecureString
$env:BOOTSTRAP_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', $adminCredential).Password
```

Bootstrap không phụ thuộc profile dev; chạy trước Java seed. Nếu username đã là admin, không đổi mật khẩu/quyền/trạng thái. Username thuộc CUSTOMER hoặc email trùng bị từ chối. Sau lần đầu, dừng backend, tắt bootstrap/xóa biến rồi chạy lại:

```powershell
$env:BOOTSTRAP_ADMIN_ENABLED = 'false'
Remove-Item Env:BOOTSTRAP_ADMIN_USERNAME, Env:BOOTSTRAP_ADMIN_EMAIL, Env:BOOTSTRAP_ADMIN_PASSWORD -ErrorAction SilentlyContinue
```

Không có mật khẩu admin mặc định hợp lệ để hướng dẫn sử dụng; mật khẩu do người vận hành cấp qua BOOTSTRAP_ADMIN_PASSWORD.

### Chạy nền thay cho giữ terminal

Sau khi đã đặt các biến trong terminal A, có thể dùng lệnh này **thay** lệnh Maven foreground. Không chạy cả hai cùng lúc:

```powershell
$runDir = Join-Path $env:TEMP 'petcarehub-local'
New-Item -ItemType Directory -Force $runDir | Out-Null
$backendProcess = Start-Process cmd.exe -ArgumentList '/c mvnw.cmd spring-boot:run' -WorkingDirectory 'D:\PetCare-Hub\ppet\backend' -WindowStyle Hidden -RedirectStandardOutput "$runDir\backend.out.log" -RedirectStandardError "$runDir\backend.err.log" -PassThru
$backendProcess.Id
```

Tiến trình con kế thừa môi trường. Log nằm ngoài repository; chỉ đọc các dòng trạng thái/lỗi cần thiết, không chia sẻ log chưa rà soát.

## 7. Chạy frontend

Terminal B riêng:

```powershell
Set-Location D:\PetCare-Hub\ppet\frontend
if (-not (Test-Path node_modules)) { npm install }
$env:VITE_API_BASE_URL = '/api/v1'
$env:VITE_WS_URL = '/ws'
npm run dev -- --host localhost --strictPort
```

Đã dùng node_modules có sẵn, chưa chạy npm install trong phiên này. Mở **http://localhost:5173/PetCare-Hub/**. `--strictPort` ngăn Vite âm thầm đổi cổng gây lệch CORS.

Vite proxy `/api` và `/ws` tới localhost:8081; Axios thêm `/api/v1`. Base của SPA là `/PetCare-Hub/`, không thêm base SPA vào URL API. Vite tự đọc .env.local nếu bạn tạo; đây là hành vi khác Spring Boot. Môi trường tiến trình có ưu tiên cao hơn file env; restart Vite sau khi đổi biến.

Chạy nền tương đương, dùng thay lệnh dev foreground:

```powershell
$runDir = Join-Path $env:TEMP 'petcarehub-local'
New-Item -ItemType Directory -Force $runDir | Out-Null
$frontendProcess = Start-Process cmd.exe -ArgumentList '/c npm run dev -- --host localhost --strictPort' -WorkingDirectory 'D:\PetCare-Hub\ppet\frontend' -WindowStyle Hidden -RedirectStandardOutput "$runDir\frontend.out.log" -RedirectStandardError "$runDir\frontend.err.log" -PassThru
$frontendProcess.Id
```

`vite.config.ts` có `optimizeDeps.esbuildOptions.define.global = 'globalThis'` để SockJS không lỗi `global is not defined` trong dev. Đây là thay đổi cấu hình duy nhất của phiên vận hành.

Deploy khác origin: VITE_API_BASE_URL là HTTPS URL đầy đủ kết thúc /api/v1, VITE_WS_URL là HTTPS SockJS URL /ws (không phải wss://); APP_CORS_ALLOWED_ORIGINS phải là origin frontend không chứa path. Workflow deploy.yml yêu cầu hai GitHub repository Variables này khi build, rồi tạo 404.html cho Pages. Chưa deploy trong phiên này; tất cả VITE_* đều công khai trong bundle.

## 8. Kiểm tra nhanh

1. Mở trang chủ đúng base /PetCare-Hub/; giao diện phải render, không chỉ HTTP 200.
2. Đăng ký tài khoản riêng với email chưa dùng, mật khẩu ít nhất 8 ký tự và tối đa 72 byte UTF-8.
3. Đăng nhập; gọi /api/v1/auth/me với Bearer token phải 200, không token phải 401. Không copy token vào báo cáo/log.
4. Mở “Khám phá sản phẩm”: database máy kiểm chứng hiển thị 4 sản phẩm; database chỉ Java seed có thể rỗng. Kiểm tra Network không có CORS/404 cho API.
5. Kiểm tra tay đặt lịch: đăng nhập, vào Dịch vụ, chọn thông tin thú cưng/dịch vụ/thời gian hợp lệ, gửi và xác nhận lịch xuất hiện trong danh sách. **Chưa thực hiện bước này trong phiên**, không khẳng định toàn bộ nghiệp vụ đã pass.

Kiểm tra HTTP không tạo dữ liệu:

```powershell
(Invoke-WebRequest 'http://localhost:5173/PetCare-Hub/').StatusCode
(Invoke-WebRequest 'http://localhost:5173/api/v1/products').StatusCode
(Invoke-WebRequest 'http://localhost:8081/api/v1/products' -Headers @{Origin='http://localhost:5173'}).Headers['Access-Control-Allow-Origin']
```

Kỳ vọng 200, 200 và http://localhost:5173. Luồng register/login/me đã được kiểm thử thực tế bằng PowerShell; tài khoản thử ghi ở báo cáo, mật khẩu ngẫu nhiên không lưu.

## 9. Test, build và lint

Backend, Java 25 trên máy này:

```powershell
Set-Location D:\PetCare-Hub\ppet\backend
.\mvnw.cmd '-Dnet.bytebuddy.experimental=true' test
```

Đây là Maven test qua wrapper; cờ experimental dành cho ByteBuddy/Mockito trên Java 25, không phải cấu hình JWT. Profile test dùng H2 và khóa giả ngẫu nhiên; không dùng SQL Server nghiệp vụ. Kết quả: 172 test, 0 failure, 0 error, 3 skip. Các test SQL Server opt-in chưa chạy; không bật chúng lên database nghiệp vụ.

Frontend:

```powershell
Set-Location D:\PetCare-Hub\ppet\frontend
try {
    npm run build
    if ($LASTEXITCODE -ne 0) { throw 'Frontend build thất bại' }
} finally {
    git restore -- tsconfig.app.tsbuildinfo
}
npm run lint
```

Build gồm tsc -b và Vite, output ở dist. Build/lint pass; đã chạy lại sau thay đổi cấu hình Vite. Sau **mỗi** build luôn restore tsconfig.app.tsbuildinfo. Không commit dist/log/.env thật.

## 10. Khắc phục sự cố

| Hiện tượng | Kiểm tra / xử lý |
|---|---|
| Thiếu JWT_SECRET, app dừng ngay | Sinh khóa theo mục 5 trong đúng terminal/IDE; .env không tự được Spring nạp. Không thêm fallback. |
| Khóa CHANGE_ME/ngắn/đã lộ bị từ chối | Cấp khóa ngẫu nhiên mới ít nhất 32 byte; không đưa giá trị lỗi vào báo cáo. |
| Cổng 8081/5173 bị chiếm | Dùng Get-NetTCPConnection ở mục 11 xác định PID, chỉ dừng tiến trình của mình. Nếu đổi backend bằng SERVER_PORT, đổi cả target Vite proxy; đổi frontend phải đổi CORS. |
| SQL Server không kết nối | Kiểm tra service, TCP/IP, host/port, firewall, databaseName và SQL login/mixed mode. Không in DB_PASSWORD. `sqlcmd -E` không kiểm chứng mật khẩu SQL login. |
| Schema missing table/column | Dev chỉ validate. Database mới cần schema và migration đúng thứ tự; database có dữ liệu cần backup/review, không reset hoặc bật ddl-auto=update để che lỗi. |
| CORS | Dùng localhost thống nhất, origin gồm scheme + host + port, không path. Local dùng proxy; deploy cần origin chính xác trong APP_CORS_ALLOWED_ORIGINS. |
| 401 sau đổi JWT | Token cũ không hợp lệ; đăng nhập lại. Tất cả instance cùng môi trường cần cùng khóa mới. |
| Trang trắng, global is not defined | Giữ cấu hình dependency optimizer SockJS trong vite.config.ts; restart Vite và reload trang. |
| 404 API hoặc SPA | SPA dùng /PetCare-Hub/, API dùng /api/v1. Đừng trỏ API vào /PetCare-Hub/api/v1. |
| Java 25 cảnh báo native-access/Unsafe | Phiên này build/run vẫn pass; test cần cờ ByteBuddy ở mục 9. Không kết luận warning là lỗi build. |
| Cảnh báo Hibernate dialect/open-in-view | Đã thấy khi chạy, không chặn startup; chưa thay cấu hình này trong phiên. |
| npm audit/lint | Lint phiên này pass; chưa chạy npm audit. Nếu npm install báo audit, review riêng, không tự npm audit fix --force hoặc nâng dependency ngoài phạm vi. |

Khi backend lỗi, xem `Caused by` cuối cùng trong log, chỉ sửa cấu hình/môi trường nếu phù hợp. Lỗi nghiệp vụ cần báo riêng, không thay đổi code để ép smoke test pass.

## 11. Dừng dịch vụ

Chạy foreground: Ctrl+C ở mỗi terminal. Chạy nền: lưu PID launcher trả về lúc Start-Process, dừng cả cây tiến trình:

```powershell
taskkill /PID $backendProcess.Id /T /F
taskkill /PID $frontendProcess.Id /T /F
```

Nếu đã đóng terminal, đọc PID/cổng và đối chiếu tiến trình trước khi dừng:

```powershell
Get-NetTCPConnection -State Listen |
    Where-Object LocalPort -in 8081,5173 |
    Select-Object LocalAddress,LocalPort,OwningProcess
```

PID listener Java/Node khác PID launcher cmd; chỉ dừng listener có thể để Maven sống. Dùng PID launcher trong báo cáo của phiên hiện tại sau khi xác minh nó vẫn thuộc project. Không dừng SQL Server chung của máy. PID trong báo cáo chỉ đúng cho phiên đó.

Sau khi dừng backend, có thể xóa biến secret ở terminal của bạn:

```powershell
Remove-Item Env:JWT_SECRET, Env:DB_PASSWORD, Env:BOOTSTRAP_ADMIN_PASSWORD -ErrorAction SilentlyContinue
```

## 12. Bảo mật

Không commit .env thật, khóa, token, mật khẩu, hash demo hoặc log. .env.example chỉ có placeholder. Không bật tracing/echo secret; không gửi token qua URL. Khóa từng nằm trong Git coi là **lộ vĩnh viễn**, không tái sử dụng.

Đổi mật khẩu admin/SQL login legacy theo quy trình quản trị; bootstrap không reset admin cũ. Không cung cấp mật khẩu mặc định trong README. Triển khai thật cấp secret qua secret manager/service environment, TLS và SQL login ít quyền; không cấp credential backend qua VITE_*.

## 13. Tài liệu và trạng thái sửa lỗi

- [Báo cáo chạy local 19/10/2026](docs/local-run-20261019.md)
- [Yêu cầu](docs/requirements.md), [kiến trúc](docs/architecture.md), [database](docs/database.md), [API](docs/api.md)
- [Design patterns](docs/design-patterns.md), [roadmap](docs/development-roadmap.md)
- [Audit #25–58](docs/audit-25-58/REPORT.md)
- [Critical JWT](docs/critical-jwt-secret/REPORT.md)
- [High lượt 1](docs/high-round-1/REPORT.md), [lượt 2](docs/high-round-2/REPORT.md), [lượt 3](docs/high-round-3/REPORT.md)

Các báo cáo trước có giới hạn kiểm chứng và việc còn lại riêng; smoke test local không thay thế kiểm thử nghiệp vụ/concurrency/deploy.
