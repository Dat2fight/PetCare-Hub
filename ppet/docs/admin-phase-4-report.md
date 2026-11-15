# Admin Portal — báo cáo giai đoạn 4

Ngày 2026-11-15. Nhánh `feature/admin-4-insights`, nền `7768d41` giai đoạn 3. Hoàn thành dashboard/báo cáo, thông báo web theo lô, nhắc tự động và trạng thái hệ thống. Không push/merge/rebase, không áp migration trên database chính, không thêm dependency. `.worktrees/` có sẵn giữ ngoài commit.

## Tính năng

| Nhóm | Trạng thái | File chính | Commit |
|---|---|---|---|
| N1-P0: chỉ số/so sánh kỳ, doanh thu theo ngày/tháng, Clock, mốc trống | ĐÃ XONG | ReportPeriod, AdminInsightsService, InsightsDashboard | bbb8739 |
| N1-P1: top sản phẩm/dịch vụ, tỷ lệ hủy/hết hạn, cảnh báo mở hàng đợi | ĐÃ XONG | AdminInsightsService, AdminOrder/Appointment/InventoryOperations, OperationsUI | bbb8739 |
| N1-P2: CSV giới hạn, BOM/chống công thức, stream, audit, admin-only | ĐÃ XONG | AdminCsvExport, JwtAuthenticationFilter | bbb8739 |
| N9-P1: audience preview, mẫu, async batch, hạn tần suất/idempotency/lịch sử | ĐÃ XONG | AdminNotificationService, NotificationDispatch, NotificationText, InsightsNotifications | e7c7f57 |
| N9-P1: nhắc lịch/tiêm theo Clock, receipt chống trùng, bật/tắt/tần suất | ĐÃ XONG | CustomerReminders, NotificationJobs, ReminderReceipt | e7c7f57 |
| N9-P1: DB/migration/version, ba scheduled jobs, số đơn sắp hết hạn/lỗi thanh toán nội bộ | ĐÃ XONG | JobMonitor, AdminSystemService, OrderExpiryJob, InsightsSystem, pom build-info | e7c7f57 + c5ab3f4 |
| Guards, 13 mappings, ma trận/audit/tests, V20, hướng dẫn, bằng chứng | ĐÃ XONG | Controllers, SecurityConfig, AdminPortal, tests/docs | Commit tích hợp |
| Cổng thanh toán/webhook/email/SMS/social/quên mật khẩu | KHÔNG LÀM THEO QUYẾT ĐỊNH | Tiền mặt + thông báo web | Không áp dụng |

Các commit theo nhóm phụ thuộc nhau; triển khai toàn bộ nhánh cùng migration/frontend. Không cherry-pick riêng màn hình bỏ backend hoặc V20. Commits nhóm: bbb8739 (báo cáo), e7c7f57 (thông báo + schema/JobMonitor), c5ab3f4 (hệ thống). Commit tích hợp chứa chính báo cáo này.

## Ma trận quyền sau triển khai

Tiền tố `/api/v1/admin`. Thêm 13 mappings, 9 đọc và 4 ghi:

| Endpoint | ADMIN | SALES_STAFF | WAREHOUSE_STAFF | VETERINARIAN | CUSTOMER / anonymous |
|---|---|---|---|---|---|
| GET /dashboard/summary, /dashboard/alerts | Có | 403 | 403 | 403 | 403 / 401 |
| GET /reports/revenue, /reports/rankings, /reports/export | Có | 403 | 403 | 403 | 403 / 401 |
| GET /notifications/audience-preview, /notifications/history, /notifications/templates | Có | 403 | 403 | 403 | 403 / 401 |
| POST /notifications/send, /notifications/templates | Có | 403 | 403 | 403 | 403 / 401 |
| PUT /notifications/templates/{id}, DELETE /notifications/templates/{id} | Có | 403 | 403 | 403 | 403 / 401 |
| GET /system/status | Có | 403 | 403 | 403 | 403 / 401 |

REPORT_VIEW cho dashboard/reports, SYSTEM_MANAGE cho notification/system. Endpoint cũ giữ ma trận [giai đoạn 1](admin-phase-1-report.md), [giai đoạn 2](admin-phase-2-report.md), [giai đoạn 3](admin-phase-3-report.md). GET /orders thêm expiring, GET /appointments thêm actionable; kho low=true thống nhất chỉ sản phẩm đang bật.

`GET /admin/me/permissions` vẫn lấy từ PermissionService:

| Role | Permissions |
|---|---|
| ADMIN | PORTAL_ACCESS, USER_MANAGE, REPORT_VIEW, AUDIT_VIEW, SYSTEM_MANAGE, ORDER_MANAGE, CATALOG_MANAGE, CATALOG_READ, INVENTORY_MANAGE, PROMOTION_MANAGE, SALE_PET_MANAGE, REVIEW_MANAGE, CONTENT_MANAGE, PACKAGE_MANAGE, APPOINTMENT_MANAGE, ASSIGNED_APPOINTMENT_MANAGE, ASSIGNED_MEDICAL_MANAGE |
| SALES_STAFF | PORTAL_ACCESS, ORDER_MANAGE, CATALOG_MANAGE, CATALOG_READ, INVENTORY_MANAGE, PROMOTION_MANAGE, SALE_PET_MANAGE, REVIEW_MANAGE, CONTENT_MANAGE, PACKAGE_MANAGE, ASSIGNED_APPOINTMENT_MANAGE |
| WAREHOUSE_STAFF | PORTAL_ACCESS, CATALOG_READ, INVENTORY_MANAGE |
| VETERINARIAN | PORTAL_ACCESS, ASSIGNED_APPOINTMENT_MANAGE, ASSIGNED_MEDICAL_MANAGE |
| CUSTOMER | Không truy cập endpoint admin |

## Audit và xác nhận

Mỗi write mapping mới thành công có đúng một SUCCESS: NOTIFICATION_QUEUED (kể cả replay), NOTIFICATION_TEMPLATE_SAVED, NOTIFICATION_TEMPLATE_DELETED. GET export ghi CSV_EXPORT_PREPARED; đợt đã tạo và thông báo worker dùng cùng transaction ở từng bước, không audit 500 lần theo người nhận. DENIED/FAILED do filter hiện hữu; job dùng job_runs. Audit không có body thông báo/tên/email/token; từ khóa tên chỉ ghi keywordProvided để tránh lưu PII.

ConfirmDialog cho lưu/xóa mẫu, gửi và xuất file; xóa yêu cầu lý do. Không thêm nhập lại mật khẩu trong giai đoạn này; quy tắc tài khoản của giai đoạn 1 giữ nguyên. CSV chuẩn bị xong không đồng nghĩa tải thành công; UI không báo giao thông báo thành công khi chỉ mới nhận 202.

## Migration, cấu hình và API

- **V20__admin_insights.sql**: năm bảng templates/batches/deliveries/reminder_receipts/job_runs, unique/idempotency/FK/index và ba mẫu mặc định Unicode; chỉ mục doanh thu paid_at và ngày tiêm. Không sửa SQL legacy/migration cũ. [Chi tiết/cách áp](database.md#admin-insights-v20-2026-11-15), [runbook](migrations.md).
- Biến optional: APP_NOTIFICATIONS_ENABLED=true, INTERVAL_MS=5000, INITIAL_DELAY_MS=30000; APP_REMINDERS_ENABLED=true, INTERVAL_MS=3600000, INITIAL_DELAY_MS=60000 (tên đầy đủ trong backend/.env.example). Không thêm secret. Profile test tắt hai job mới, Chrome proof bật dispatch trên DB tạm.
- API contract, payload, quy tắc doanh thu, cap/rate/audience và safe error: [API](api.md#admin-insights--giai-đoạn-4-2026-11-15). Menu lấy permissions, route lazy load; bảng thay cho biểu đồ và dialog hỗ trợ bàn phím. Maven build-info cung cấp version; chạy IDE thiếu metadata thì development.

## Kiểm chứng

- `tools/test-sqlserver.ps1` chạy toàn bộ `mvn test`: **295 tests, 0 failures, 0 errors, 0 skipped**. Trong đó các integration test SQL Server dùng database tên ngẫu nhiên `petcarehub_test_*`, lifecycle tự dọn; không Docker, không mutation database ứng dụng. Flyway V20 đạt schema mới/nâng cấp legacy/checksum/Hibernate validate. Các bài nghiệp vụ/API/queue chạy H2; không tuyên bố mọi bài đều chạy SQL Server.
- AdminInsightsTest (6 bài): inventory 13 routes + method guards/mọi role; số liệu tính tay paid/pending/cancel/refund/ngoài kỳ/pet, khoảng nửa mở/kỳ trước/0/366 ngày/Clock; CSV BOM/escape/audit; đủ 4 write mappings một audit; audience tier/cap501/template an toàn; replay/rate/worker; reminder chống trùng; trạng thái hệ thống che lỗi.
- AdminCsvHttpTest (1 bài): servlet thật cổng ngẫu nhiên + JWT + download hoàn tất và audit đúng một lần, anonymous 401. Bắt lỗi ASYNC redispatch mà principal giả của MockMvc che khuất; JwtAuthenticationFilter nay phục hồi principal ở dispatch này.
- NotificationConcurrencyTest (1 bài): hai worker cạnh tranh batch đã commit, tổng 8 thông báo, không trùng, gọi lại không thêm. NotificationJobFailureTest (2 bài): lỗi worker/reminder chỉ ghi mã an toàn và trạng thái thất bại. EndpointSecurityMatrixTest kiểm mọi mapping toàn app, endpoint mới thiếu khai báo sẽ fail.
- `npx tsc --noEmit`, `npm run lint`, `npm run build`: **exit 0**, không thêm thư viện.
- Chrome thật headless + backend H2 tạm + build frontend: login, dashboard/chart/table, tải CSV tiếng Việt, link cảnh báo lọc kho, tạo template, preview/send đến COMPLETED và count1, trạng thái DB/job. Kiểm 5 màn ở **320/360/390/1366px**, không tràn ngang; Tab trong dialog/Escape đóng; không pageerror. [Bằng chứng](admin-phase-4-evidence/README.md). Đây là môi trường cách ly, không phải xác nhận production đã triển khai.
- Quét service admin không có `findAll()` không giới hạn; query scalar/page/cap. Frontend không `dangerouslySetInnerHTML`. `mockImages` còn là URL ảnh trình bày cũ ngoài admin, không giả dữ liệu nghiệp vụ. `LocalDateTime.now()` không Clock chỉ còn timestamp chẩn đoán health/error cũ; phần mới dùng Clock. Không có endpoint admin mới trả 500 cho anonymous trong ma trận.

## Giới hạn và việc triển khai

- CSV tối đa 5000 dòng; dữ liệu được giới hạn và chuẩn bị trong bộ nhớ rồi ghi từng dòng. Nhóm thông báo tối đa 500, worker100/tick, một đợt/phút/admin; chưa load-test lưu lượng lớn hoặc mọi interleaving trên SQL Server. Lịch sử/receipt không tự xóa; cần kế hoạch retention khi quy mô tăng. Job giữ 100 bản ghi đã hoàn tất mỗi loại, RUNNING dở dang có thể còn sau crash.
- Nhắc tự động 100 mỗi loại/tick, không nhắc bù lịch đã qua; backlog lớn cần điều chỉnh tần suất. Lỗi 5 lần batch FAILED dừng; chưa có UI resume, không gửi lại cả batch đã giao một phần. Mã lỗi an toàn giúp đối chiếu vận hành, không có exception thô trong UI.
- NEEDS DECISION từ giai đoạn 3 vẫn giữ: biến thể/nhận nuôi/quota gói cũ, ảnh chỉ URL allowlist. Xem [thiết kế](admin-phase-3-design-decisions.md). Không có quyết định mới chặn giai đoạn 4; CSV thay XLSX, ADMIN-only theo mặc định đã chốt.
- Trước production: sao lưu và kiểm khôi phục; thử Flyway trên bản sao rồi V20/ddl-auto=validate; duyệt mẫu nhắc và timezone; cấp admin an toàn theo bootstrap runbook, rà lại quyền nội bộ, ảnh/nội dung pháp lý của cửa hàng. Các câu SQL chỉ đọc rà staff/active admin/giỏ sản phẩm tắt/pet PENDING không đơn/voucher vượt lượt nằm nguyên trong [admin guide](admin-guide.md#kiểm-tra-dữ-liệu-trước-vận-hành-chỉ-đọc-tự-chạy-khi-cần), chưa chạy trên database chính.
- Các thay đổi đều thuộc giai đoạn 4. `.worktrees/` ngoài phạm vi không đưa vào commit. Không push, merge, rebase hoặc chạy migration thật.
