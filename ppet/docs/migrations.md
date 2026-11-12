# Migration SQL Server bằng Flyway

Giai đoạn 3, cập nhật 12/11/2026. Flyway là đường nâng cấp schema chính từ bản này. Không chạy schema/reset/seed tự động qua Spring SQL init. Runtime mặc định và dev giữ Hibernate validate; test H2 tắt Flyway và dùng create-drop.

## Phiên bản và dữ liệu

V1–V14 trong backend/src/main/resources/db/migration là bản sao nguyên nội dung SQL cũ. Bảng ánh xạ đầy đủ ở [kế hoạch](hardening/MIGRATION-PLAN.md). legacy-sha256.txt ghi SHA-256 chuẩn hóa xuống dòng LF để kiểm tra trên Windows/Linux; test so sánh thêm byte của bản sao và nguồn trong cùng checkout. Không thay đổi các script đã áp dụng.

V15__unicode_descriptions.sql mở rộng roles.description và loyalty_transactions.description thành NVARCHAR(MAX), giữ nguyên giá trị. Entity dùng Nationalized cho chuỗi Unicode, Length.LONG32 cho nội dung dài, decimal precision 18 cho tiền theo schema, URL 500 ký tự, checkout key 36 và fingerprint 64. Không sửa ngày lịch sử, không suy diễn paid_at, không xóa bảng/cột ngoài ORM.

Script V2–V4 và V6–V14 tự quản lý transaction nên có sidecar .sql.conf với executeInTransaction=false. V1, V5 và V15 để Flyway quản lý transaction. GO vẫn được SQL Server parser của Flyway xử lý. Không bật group/mixed hoặc bỏ sidecar khi đóng gói.

## Database trống

1. Người quản trị tạo database riêng và cấp quyền DDL cần thiết cho tài khoản migration. Flyway không tạo database SQL Server; dbo đã tồn tại.
2. Cấp DB_URL, DB_USERNAME, DB_PASSWORD và JWT_SECRET qua terminal/IDE/secret manager. Spring Boot không tự đọc .env. DB_URL phải trỏ đúng database đích.
3. Giữ FLYWAY_ENABLED=true, FLYWAY_BASELINE_ON_MIGRATE=false. Khởi động backend theo README: Flyway chạy V1–V15 trước khi Hibernate validate. Nếu schema thực sự trống, không baseline (baseline 1 sẽ bỏ qua việc tạo bảng).
4. Kiểm flyway_schema_history: 15 migration thành công, không có baseline. Kiểm schema, API và luồng mua/đặt lịch trước khi mở traffic.

Mặc định không seed catalog. Profile dev vẫn có Java DataSeeder cũ, thêm catalog khi species rỗng; vì vậy không dùng profile dev cho production hoặc kiểm chứng giữ nguyên dữ liệu. Admin bootstrap là bước độc lập, mặc định tắt.

## Database đã tồn tại

Baseline chỉ đánh dấu lịch sử, KHÔNG sửa schema. Tuyệt đối không bật baseline-on-migrate cố định cho mọi database.

1. Dừng writer/job, sao lưu và kiểm restore sang database tách biệt. Lưu schema, số dòng, PK/FK/index/CHECK, giá trị tiền và Unicode quan trọng trước nâng cấp.
2. Đối chiếu bản sao với V1 và các migration đã chạy; dùng [kiểm kê](hardening/SCHEMA-INVENTORY.md) và [truy vấn đối soát](database.md). Không chỉ dựa vào Hibernate validate: Hibernate không xác minh đầy đủ filtered unique, CHECK, precision và nullability.
3. Baseline 1 chỉ khi đủ toàn bộ schema nền V1. V2–V14 sẽ chạy lại có kiểm soát để xử lý DB đã chạy một phần migration thủ công. Baseline 14 chỉ khi có bằng chứng đã áp đủ V1–V14; khi đó chỉ V15 chạy. DB thiếu bảng nền/có schema do ORM tự tạo bị lệch cần migration đối soát riêng trước onboarding, không tự đổi kiểu hàng loạt hoặc xóa dữ liệu để ép chạy.
4. Trên bản sao đã xác minh, bật FLYWAY_BASELINE_ON_MIGRATE=true và FLYWAY_BASELINE_VERSION=1 chỉ cho một lượt khởi động. Sau khi thành công, dừng ứng dụng, bỏ override hoặc đặt lại false rồi khởi động lại. Có thể dùng lệnh baseline explicit của Flyway CLI với cùng phiên bản/dependency thay thế; dự án chưa cấu hình Maven Flyway plugin.
5. So sánh dữ liệu trước/sau; kiểm history có baseline 1 và V2–V15 thành công. Khởi động lần hai không còn migration pending. Chạy đầy đủ kiểm thử nghiệp vụ trên bản sao.
6. Chỉ sau khi kiểm chứng và có lịch triển khai mới thực hiện cùng quy trình trên database đích đã sao lưu; triển khai frontend/backend tương thích cùng đợt. Phiên phát triển này không thực thi bước đó.

Ví dụ biến một lần trong PowerShell, sau khi đã chọn đúng DB và kiểm kê:

```powershell
$env:FLYWAY_BASELINE_ON_MIGRATE = 'true'
$env:FLYWAY_BASELINE_VERSION = '1'
# Khởi động backend theo README, kiểm history, rồi dừng ứng dụng.
# Trước mọi lần khởi động tiếp theo:
$env:FLYWAY_BASELINE_ON_MIGRATE = 'false'
```

Không đưa mật khẩu vào script/log hoặc command line. Có thể tách tài khoản migration bằng SPRING_FLYWAY_USER/PASSWORD/URL trong secret manager; nếu không cấu hình, Flyway dùng datasource hiện tại. Tài khoản runtime cần DML, tài khoản migration cần DDL. Tắt Flyway ở runtime chỉ sau khi một bước migration độc lập đã chạy và được kiểm chứng; Hibernate validate vẫn phải bật.

## Viết migration tiếp theo

- Dùng V16__mo_ta.sql trở đi, phiên bản duy nhất. Không đổi tên/nội dung V1–V15 hoặc nguồn lịch sử.
- Thêm cột nullable hoặc có default hợp lệ, backfill có điều kiện sau đối soát; không thu hẹp dữ liệu hoặc đoán giá trị lịch sử.
- Idempotent khi có thể; nếu dữ liệu vi phạm unique/CHECK thì dừng và báo đối soát, không tự xóa.
- Ưu tiên transaction Flyway cho script mới; chỉ thêm executeInTransaction=false khi script có lý do phải tự quản lý transaction và có test failure/rollback.
- Thêm test nâng cấp có dữ liệu, trường hợp lỗi, chạy lần hai và khởi động Hibernate validate. Kiểm không mất dữ liệu ngoài ORM.
- Không dùng ddl-auto update để che drift; không chạy schema-sqlserver.sql hoặc SQLQuery3.sql vào DB đã được Flyway quản lý.

## Kiểm thử

Từ ppet/backend, test mặc định chạy H2 và kiểm mapping/checksum ngoại tuyến:

```powershell
.\mvnw.cmd test
```

Máy có Docker Linux containers đang chạy, đủ RAM và được phép tải SQL Server image:

```powershell
$env:RUN_SQLSERVER_TESTS = 'true'
try {
    .\mvnw.cmd '-Dtest=FlywayMigrationTest,SqlServerConstraintTest' test
    if ($LASTEXITCODE -ne 0) { throw 'SQL Server integration failed' }
} finally {
    Remove-Item Env:RUN_SQLSERVER_TESTS -ErrorAction SilentlyContinue
}
```

FlywayMigrationTest chỉ tạo URL từ MSSQLServerContainer; không đọc DB_URL để thử. Bao phủ database trống + ứng dụng validate + round-trip Unicode/giá lớn/URL dài, baseline DB cũ một phần/toàn bộ, dữ liệu và bảng ngoài ORM còn nguyên, chạy lại không thay đổi, checksum lỗi phải chặn, clean bị cấm, duplicate cart chặn migration và phục hồi sau đối soát. Opt-in nhưng thiếu Docker sẽ FAIL, tránh hiểu nhầm là đã chạy thành công. SQL Server tests không opt-in được skip trong bộ test thường.

## Sự cố và hồi phục

- Database không rỗng chưa có history: dừng, kiểm đích/backup/schema rồi mới baseline; không tự bật baseline để vượt lỗi.
- Migration lỗi: dừng writer, xem log/history và trạng thái schema. Script tự transaction có thể để failed history; xác minh rollback và dữ liệu trước khi sửa nguyên nhân. Chỉ dùng repair để xóa failed entry sau khi đã đối soát; không dùng repair để chấp nhận checksum bị đổi. Test retry chỉ thực hiện repair trên container dùng một lần.
- Migration đã thành công: sửa bằng phiên bản mới (forward-only), không chỉnh file cũ. Nếu không khắc phục an toàn được, dừng ghi và restore backup đã thử cùng phiên bản ứng dụng tương thích. Ghi nhận nguy cơ mất các ghi phát sinh sau backup.
- clean-disabled=true luôn giữ; không dùng clean/reset demo để hồi phục. reset-demo-sqlserver.sql chỉ dành cho DB demo tách biệt có thể bỏ đi và không thuộc lịch sử Flyway.
- NEEDS DECISION: lịch sử DATETIME2(0) có default UTC nhưng app ghi giờ kinh doanh Việt Nam. Không chuyển timezone hoặc đổi precision dữ liệu trong giai đoạn này. Kiểm container/đối soát nguồn ghi trước mọi thay đổi lịch sử.
