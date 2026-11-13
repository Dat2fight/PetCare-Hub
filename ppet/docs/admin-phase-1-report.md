# Giai đoạn 1 — nền tảng quản trị

Ngày: 2026-11-13. Nhánh `feature/admin-1-foundation`; nền `0fd70f1`.

| Hạng mục | Trạng thái trong giai đoạn 1 | Mã nguồn chính |
|---|---|---|
| N2-P0: tìm/lọc/phân trang, khóa/mở, role, nhân viên, xóa mềm | ĐÃ XONG | AdminAccountService, StaffAccountService, AdminController, AdminUsersPage |
| N2-P1: hồ sơ, thu hồi phiên | ĐÃ XONG | AdminAccountService.overview/revoke, AdminUserDetail, JWT filter/provider, STOMP interceptor |
| N9-P0: audit chỉ-thêm | ĐÃ XONG | AdminAuditService, AdminAuditLog, AdminAuditRepository, AdminRequestAuditFilter, AdminAuditPage |
| Khung admin lazy-loaded, sidebar theo API quyền, component dùng lại | ĐÃ XONG | PermissionService/PermissionContext, AdminPortal, AdminUI |
| N1 dashboard nâng cao; N3–N8 vận hành/catalog; N9 gửi nhóm/cấu hình | CHƯA TRIỂN KHAI trong giai đoạn này | Theo giai đoạn 2–5 đã chốt |

Không thêm dependency; không chỉnh SQL legacy; không áp dụng migration vào database thật; không push/merge/rebase. Thư mục `.worktrees/` có sẵn không thuộc thay đổi.

## Quyền endpoint sau triển khai

Tiền tố `/api/v1/admin`. Anonymous 401, CUSTOMER 403 tất cả. ADMIN được dùng tất cả endpoint dưới đây. SALES_STAFF, WAREHOUSE_STAFF, VETERINARIAN chỉ được gọi `GET /me/permissions` trong tập endpoint giai đoạn 1; các endpoint còn lại trả 403.

| Endpoint | Permission |
|---|---|
| GET /me/permissions | PORTAL_ACCESS |
| GET /dashboard; GET /revenue-report | REPORT_VIEW |
| GET /users; POST /staff | USER_MANAGE |
| PATCH /users/{userId}/role; PATCH /users/{userId}/status | USER_MANAGE |
| DELETE /users/{userId}; POST /users/{userId}/revoke-sessions | USER_MANAGE |
| GET /users/{userId}/overview | USER_MANAGE |
| GET /audit-logs | AUDIT_VIEW |

`GET /me/permissions` trả `{permissions:[...]}`:

- ADMIN: PORTAL_ACCESS, USER_MANAGE, REPORT_VIEW, AUDIT_VIEW, SYSTEM_MANAGE, ORDER_MANAGE, CATALOG_MANAGE, CATALOG_READ, INVENTORY_MANAGE, PROMOTION_MANAGE, SALE_PET_MANAGE, REVIEW_MANAGE, CONTENT_MANAGE, PACKAGE_MANAGE, APPOINTMENT_MANAGE, ASSIGNED_APPOINTMENT_MANAGE, ASSIGNED_MEDICAL_MANAGE.
- SALES_STAFF: PORTAL_ACCESS, ORDER_MANAGE, CATALOG_MANAGE, CATALOG_READ, INVENTORY_MANAGE, PROMOTION_MANAGE, SALE_PET_MANAGE, REVIEW_MANAGE, CONTENT_MANAGE, PACKAGE_MANAGE, ASSIGNED_APPOINTMENT_MANAGE.
- WAREHOUSE_STAFF: PORTAL_ACCESS, CATALOG_READ, INVENTORY_MANAGE.
- VETERINARIAN: PORTAL_ACCESS, ASSIGNED_APPOINTMENT_MANAGE, ASSIGNED_MEDICAL_MANAGE.
- CUSTOMER: endpoint trả 403. Tài khoản phải đổi mật khẩu tạm cũng bị chặn cho tới khi đổi xong.

Các quyền vận hành là nền cho giai đoạn sau; không đồng nghĩa các màn/API vận hành mới đã được xây dựng. URL nghiệp vụ cũ vẫn giữ quyền cũ.

## Migration / thay đổi hợp đồng

- V16__admin_accounts.sql: token_version, must_change_password, chỉ mục trạng thái, staff_id nullable. Entity User và Appointment khớp schema.
- V17__admin_audit_log.sql: bảng/index/trigger chặn sửa/xóa audit.
- DTO người dùng quản trị thêm deletedAt/mustChangePassword, không trả entity User. Auth DTO thêm mustChangePassword. JWT thêm tv; token cũ thiếu claim phải đăng nhập lại.
- API đổi role chuyển query param sang JSON có reason/currentPassword; API staff trả user và temporaryPassword một lần. Chi tiết payload trong api.md.
- Không có biến môi trường mới bắt buộc. Hướng dẫn áp dụng, truy vấn rà dữ liệu và quyết định nghiệp vụ nằm trong admin-guide.md.

## Audit / xác nhận

SUCCESS cùng transaction: STAFF_CREATED, USER_STATUS_CHANGED, USER_ROLE_CHANGED, USER_DELETED, USER_SESSIONS_REVOKED, USER_OVERVIEW_READ, USER_PASSWORD_CHANGED. DENIED/FAILED của yêu cầu admin được ghi riêng dưới ADMIN_REQUEST_REJECTED. Không ghi body/password/token/PII; lý do tự do không lưu nguyên văn trong audit.

Yêu cầu lý do: khóa, đổi quyền, xóa mềm, thu hồi phiên. Yêu cầu mật khẩu: đổi từ/sang ADMIN, khóa ADMIN, xóa nhân viên/admin; đổi mật khẩu phải biết mật khẩu hiện tại. Không cấp role nhân viên qua API đổi role.

## Kiểm chứng

Kết quả cuối: `tools/test-sqlserver.ps1` chạy Maven **272 tests, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS**. `npx tsc --noEmit`, `npm run lint`, `npm run build` đều exit 0. Script trình duyệt `target/admin-proof.cjs` exit 0. `git diff --check` không có lỗi whitespace.

Commit backend: `36b2a99` (N9 audit), `8afba3b` (N2 governance/JWT/tests). Commit giao diện và tài liệu theo sau, tên `feat(admin-1): ship permission-driven portal and operating guide`.

- AdminFoundationTest: ma trận role cho mọi endpoint admin, method guard, kiểm mọi endpoint ghi có đúng một audit thành công, đọc hồ sơ có audit; khóa bản thân, khóa/mở rồi token cũ vẫn vô hiệu; thu hồi phiên, mật khẩu tạm/đổi mật khẩu, sai mật khẩu không đổi dữ liệu nhưng ghi DENIED; tìm kiếm/phân trang/xóa mềm; hai admin đồng thời không khóa được cả hai.
- StaffRegistrationTest giữ chống role injection ở đăng ký công khai, kiểm hợp đồng mật khẩu tạm mới. EndpointSecurityMatrixTest kiểm tồn kho endpoint; test pagination/JWT cập nhật DTO/claim mới có chủ đích.
- FlywayMigrationTest kiểm database SQL Server trống/legacy, bảo toàn dữ liệu, validate entity, mặc định V16, trigger audit không sửa/xóa. Cập nhật số migration mong đợi vì thêm V16/V17.
- Trình duyệt Chrome headless trên backend H2 độc lập: đăng nhập thật, tạo nhân viên qua form, đọc hồ sơ, đọc audit, đổi mật khẩu tạm và đăng xuất. Trang người dùng không tràn ngang ở 320/360/390/1366; không có lỗi JavaScript. Ảnh và script kiểm chứng trong backend/target/admin-* (không commit dữ liệu thử nghiệm).
- Chưa kiểm thủ công bằng screen reader; chưa kiểm toàn bộ thao tác bằng bàn phím trên từng màn hình. Modal dùng dialog native, input có label, các nút dùng button thật.
- Flyway hiện cảnh báo SQL Server 17 mới hơn phạm vi hỗ trợ đã công bố của phiên bản thư viện đang dùng; không tự nâng dependency. Bộ test tích hợp chạy trên SQL Server cài sẵn, không Docker.

## Việc trước khi đưa vào sử dụng thật

Sao lưu/kiểm tra phục hồi, kiểm migration trên bản sao; rà danh sách ADMIN/nhân viên; áp dụng migration có kiểm soát; đăng nhập lại; thử với tài khoản thật có quyền tương ứng. Giai đoạn 2 mới bổ sung màn điều phối các lịch đã bỏ phân công. Không có tính năng khôi phục tài khoản đã xóa hoặc cấp lại mật khẩu tạm trong phạm vi này.
