# Cấu hình secret và rotate

Backend bắt buộc `JWT_SECRET`, `DB_USERNAME`, `DB_PASSWORD`; `DB_URL` có thể ghi đè JDBC URL mặc định. `SPRING_DATASOURCE_URL` vẫn là override chuẩn của Spring. Không sửa mật khẩu trong YAML, không đưa secret vào biến `VITE_*`. Spring không tự đọc `.env`; `.env.example` chỉ liệt kê khóa. Biến phải được cấp trong terminal/IDE chạy Java hoặc secret manager của môi trường triển khai.

## Windows PowerShell

Chạy tại `ppet/backend`, trong terminal riêng không ghi transcript:

```powershell
$qaKeyBytes = New-Object byte[] 64
$qaRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try { $qaRng.GetBytes($qaKeyBytes) } finally { $qaRng.Dispose() }
$env:JWT_SECRET = [Convert]::ToBase64String($qaKeyBytes)
$qaDbCredential = Get-Credential -Message 'Tài khoản SQL Server của môi trường này'
$env:DB_USERNAME = $qaDbCredential.UserName
$env:DB_PASSWORD = $qaDbCredential.GetNetworkCredential().Password
$env:DB_URL = 'jdbc:sqlserver://localhost:1433;databaseName=petcarehub;encrypt=true;trustServerCertificate=true'
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:BOOTSTRAP_ADMIN_ENABLED = 'false'
.\mvnw.cmd spring-boot:run
```

Không chạy lệnh startup này để test tự động trên DB đang dùng. Test của bản vá dùng H2 và không cần các biến trên. Không hiển thị biến secret ra màn hình. Với môi trường lâu dài, lưu khóa sinh ra trong secret manager để các instance dùng cùng phiên bản khóa; sinh khóa mới mỗi lần restart sẽ làm mất hiệu lực token cũ.

## Linux/macOS (Bash)

```bash
export JWT_SECRET="$(openssl rand -base64 64 | tr -d '\n')"
read -r -p 'DB username: ' DB_USERNAME
read -r -s -p 'DB password: ' DB_PASSWORD
printf '\n'
export DB_USERNAME DB_PASSWORD
export DB_URL='jdbc:sqlserver://localhost:1433;databaseName=petcarehub;encrypt=true;trustServerCertificate=true'
export SPRING_PROFILES_ACTIVE=dev BOOTSTRAP_ADMIN_ENABLED=false
./mvnw spring-boot:run
```

JWT thiếu/rỗng, placeholder, khóa từng công khai, hoặc dưới 32 byte UTF-8 đều làm context thất bại. Provider chọn HS256/384/512 theo độ dài khóa; hướng dẫn sinh 64 byte ngẫu nhiên ở trên đủ cho các thuật toán hiện có. Thông báo lỗi không chứa giá trị bị từ chối. Cấu hình test chỉ nằm trong `src/test/resources`, dùng tiền tố `test-only-` và phần ngẫu nhiên, không đóng gói vào runtime.

## Khởi tạo admin có chủ đích

`BOOTSTRAP_ADMIN_ENABLED` mặc định false; không tạo admin và log hướng dẫn ngắn. Khi cần tạo lần đầu, cấp đủ `BOOTSTRAP_ADMIN_USERNAME`, `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD`, bật enabled rồi khởi động. Mật khẩu tối thiểu 12 ký tự, tối đa 72 byte UTF-8, gồm chữ hoa, chữ thường, chữ số. Nhập qua prompt bảo mật/secret manager giống DB, không viết giá trị vào file nguồn hoặc lịch sử lệnh.

Bootstrap băm BCrypt và không log password/hash. Không reset mật khẩu, bật lại tài khoản hoặc tự nâng quyền tài khoản trùng tên. Sau lần đầu, tắt enabled và gỡ các biến bootstrap. DataSeeder danh mục chỉ hoạt động ở profile dev; không bật dev trong production.

## Checklist ROTATE do người vận hành thực hiện

- Coi mọi secret từng commit là đã lộ, kể cả file hiện tại đã sạch. Không rewrite history hoặc force push.
- Cấp JWT secret mới qua secret manager cho toàn bộ instance, restart có kế hoạch; mọi token cũ phải đăng nhập lại. Kiểm tra token ký bằng khóa cũ bị 401.
- Đổi mật khẩu tài khoản DB và cập nhật secret triển khai; kiểm quyền tối thiểu và nguồn kết nối. Không dùng tài khoản quản trị DB cho runtime nếu không cần.
- Đổi mật khẩu admin đã từng seed qua quy trình quản trị được xác thực. Bootstrap không phải cơ chế reset. Nếu hệ thống chưa có API đổi mật khẩu admin, dùng công cụ quản trị được review để ghi BCrypt mới; không ghi password thuần hoặc hash vào báo cáo/log.
- Rà soát tài khoản nhân viên tạo bằng public signup trước bản vá; đối chiếu nhật ký/phê duyệt trước khi thu hồi quyền. Việc hạ quyền mọi tài khoản nhân viên tự động có thể khóa nhân viên hợp lệ.
- Tắt bootstrap sau khởi tạo, loại bản sao secret khỏi local scripts/logs và kiểm lại `.gitignore`.

Workflow GitHub Pages hiện chỉ build frontend; URL API dùng repository variables, token deploy dùng GitHub Secrets đã có. Không cấp JWT/DB secret cho frontend job. Nếu thêm backend deployment, dùng `${{ secrets.JWT_SECRET }}` và `${{ secrets.DB_PASSWORD }}` ở job backend riêng, không ghi literal vào YAML.

Không bước rotate nào đã được thực hiện trên DB dev/production trong nhiệm vụ này.
