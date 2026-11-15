# Admin Portal — báo cáo giai đoạn 5

Ngày 2026-11-15. Nhánh `feature/admin-5-content-settings`, nền `74f2846` giai đoạn 4. Không push/merge/rebase, không chạy migration trên database chính, không thêm dependency. `.worktrees/` có sẵn giữ ngoài commit.

## Tính năng

| Nhóm | Trạng thái | File chính | Commit |
|---|---|---|---|
| N8-P1: CMS nháp/xuất bản/gỡ/khôi phục 20 bản; home nổi bật/chính sách/liên hệ | ĐÃ XONG | SiteContentService, SiteContent/ContentRevision, ContentManagement, SafeContent, HomePage/Footer | Nhóm CMS |
| N8-P2: bài viết/FAQ, slug, metadata title, public chỉ bản xuất bản | ĐÃ XONG | PublicContentController, PublicContent | Nhóm CMS |
| N9-P2: settings whitelist/biên/cache; quote/đơn/hủy lịch; loyalty cùng nguồn | ĐÃ XONG | SettingsService, CheckoutPricing, OrderServiceImpl, AppointmentServiceImpl, SettingsPage | Nhóm cấu hình/xuất dữ liệu |
| N9-P2: export async allowlist/owner-only/dùng một lần/hết hạn/audit | ĐÃ XONG | DataExportsService, DataExportWorker/Runner, DataExportsPage, JobMonitor | Nhóm cấu hình/xuất dữ liệu |
| N4-P2: khách yêu cầu trả; duyệt/nhận kho/ghi nhận hoàn tiền mặt từng bước | ĐÃ XONG | ReturnsService, ReturnRequest, AdminOrderOperations, ReturnManagement, Returns | Nhóm trả hàng |
| Guards, V21, ma trận/audit/concurrency, tài liệu/bằng chứng | ĐÃ XONG | Controllers, SecurityConfig, AdminPortal/App, tests/docs | Commit tích hợp |
| N9-P2: runbook backup/restore ngoài ứng dụng | ĐÃ XONG / NEEDS DECISION vận hành | backup-restore-runbook.md; cần chốt lịch/retention/RPO/RTO | Commit tích hợp |
| N3-P2 biến thể, N5-P2 nhận nuôi, hạn mức gói legacy | NEEDS DECISION giữ từ giai đoạn 3 | admin-phase-3-design-decisions.md | Không thay đổi trong giai đoạn 5 |
| Payment gateway/email/SMS/social/quên mật khẩu | KHÔNG LÀM THEO QUYẾT ĐỊNH | Tiền mặt + thông báo web | Không áp dụng |

Các nhóm commit phụ thuộc nhau; triển khai toàn bộ nhánh cùng V21 và frontend. Không cherry-pick UI bỏ service/schema. Commit nhóm: **ab35f87** (CMS + schema/entities V21), **f272c50** (cấu hình/xuất dữ liệu), **9e15f8d** (trả hàng). Commit tích hợp chứa guards, kiểm thử, tài liệu và chính báo cáo này.

## Ma trận quyền

Tiền tố `/api/v1/admin`. **16 mappings mới: 6 GET, 10 write**; guard ở matcher và controller/service:

| Endpoint | ADMIN | SALES_STAFF | WAREHOUSE_STAFF | VETERINARIAN | CUSTOMER / anonymous |
|---|---|---|---|---|---|
| GET /content, /content/{id}, /content/{id}/revisions | Có | Có | 403 | 403 | 403 / 401 |
| POST /content; PUT /content/{id}; DELETE /content/{id} | Có | Có | 403 | 403 | 403 / 401 |
| POST /content/{id}/publish, /unpublish, /restore | Có | Có | 403 | 403 | 403 / 401 |
| GET /settings; PUT /settings | Có | 403 | 403 | 403 | 403 / 401 |
| GET /data-exports; POST /data-exports | Có, chỉ lịch sử mình | 403 | 403 | 403 | 403 / 401 |
| POST /data-exports/{id}/download | Chỉ người tạo | 403 | 403 | 403 | 403 / 401 |
| GET /returns; POST /returns/{id}/transition | Có | Có | 403 | 403 | 403 / 401 |

CONTENT_MANAGE cho CMS; SYSTEM_MANAGE cho settings/export; ORDER_MANAGE cho return queue. Ngoài admin: GET `/api/v1/content`, `/content/featured`, `/content/{slug}` công khai chỉ bản xuất bản; GET/POST `/api/v1/returns` chỉ CUSTOMER, theo ownership. Endpoint mới đều có trong EndpointSecurityMatrixTest; thiếu khai báo route mới sẽ fail. Ma trận endpoint cũ giữ nguyên trong báo cáo [1](admin-phase-1-report.md), [2](admin-phase-2-report.md), [3](admin-phase-3-report.md), [4](admin-phase-4-report.md).

`GET /admin/me/permissions` vẫn lấy duy nhất từ PermissionService:

| Role | Permissions |
|---|---|
| ADMIN | PORTAL_ACCESS, USER_MANAGE, REPORT_VIEW, AUDIT_VIEW, SYSTEM_MANAGE, ORDER_MANAGE, CATALOG_MANAGE, CATALOG_READ, INVENTORY_MANAGE, PROMOTION_MANAGE, SALE_PET_MANAGE, REVIEW_MANAGE, CONTENT_MANAGE, PACKAGE_MANAGE, APPOINTMENT_MANAGE, ASSIGNED_APPOINTMENT_MANAGE, ASSIGNED_MEDICAL_MANAGE |
| SALES_STAFF | PORTAL_ACCESS, ORDER_MANAGE, CATALOG_MANAGE, CATALOG_READ, INVENTORY_MANAGE, PROMOTION_MANAGE, SALE_PET_MANAGE, REVIEW_MANAGE, CONTENT_MANAGE, PACKAGE_MANAGE, ASSIGNED_APPOINTMENT_MANAGE |
| WAREHOUSE_STAFF | PORTAL_ACCESS, CATALOG_READ, INVENTORY_MANAGE |
| VETERINARIAN | PORTAL_ACCESS, ASSIGNED_APPOINTMENT_MANAGE, ASSIGNED_MEDICAL_MANAGE |
| CUSTOMER | Không có quyền portal |

## Audit và xác nhận

Mỗi write admin thành công đúng một SUCCESS, cùng transaction: CONTENT_DRAFT_SAVED, CONTENT_PUBLISHED, CONTENT_UNPUBLISHED, CONTENT_RESTORED, CONTENT_DELETED, SYSTEM_SETTINGS_UPDATED, DATA_EXPORT_REQUESTED, DATA_EXPORT_DOWNLOADED, RETURN_APPROVED, RETURN_REJECTED, RETURN_RECEIVED, RETURN_REFUNDED. Replay có audit cho request, không lặp nhập kho/hoàn tiền. Filter hiện hữu ghi DENIED/FAILED riêng khi request bị từ chối; worker dùng job_runs, không audit từng dòng. Audit chỉ chứa trạng thái, quantity/count, cờ reasonProvided/replay và thay đổi số cấu hình trước → sau; không body CMS/CSV/lý do tự do/keyword/secret.

UI xác nhận thao tác lưu, publish/gỡ/restore/xóa, cấu hình, export và xử lý trả. Xóa CMS và mọi bước xử lý trả cần lý do; refund thêm số tiền và mã biên nhận. Không thêm bước nhập lại mật khẩu trong giai đoạn này; thao tác nhạy cảm về tài khoản giữ yêu cầu giai đoạn 1. DOWNLOAD audit nghĩa là máy chủ chấp nhận tiêu thụ file, không bảo đảm máy khách tải hoàn tất.

## Migration, API và cấu hình

- **V21__admin_content_settings_returns.sql**: 5 bảng system_settings/site_content/content_revisions/return_requests/data_export_jobs, FK/unique/check/index và orders.return_window_days/returned_amount. Không sửa migration cũ. [Schema](database.md#admin-contentsettingsreturns-v21-2026-11-15), [runbook](backup-restore-runbook.md): backup → verify/restore thử DB riêng → thử Flyway/validate → triển khai cặp backend/frontend. Chưa áp lên DB chính.
- Optional env: APP_CHECKOUT_SHIPPING_FEE=30000, APP_CHECKOUT_FREE_SHIPPING_FROM=500000, APP_RETURNS_WINDOW_DAYS=7, APP_EXPORTS_ENABLED=true, APP_EXPORTS_INTERVAL_MS=5000, APP_EXPORTS_INITIAL_DELAY_MS=30000. Hold/cancellation dùng fallback cấu hình hiện có; không VITE secret hay dependency mới. Cache local xóa sau commit, các tiến trình khác tối đa 5 giây.
- [API đầy đủ](admin-phase-5-api.md): payload/giới hạn/quyền/lỗi/audit. OrderDTO thêm returnedAmount/returnUntil, item thêm itemId chỉ đọc. Màn admin lazy-load/menu theo permission. Public home/policy/contact/articles/FAQ lấy snapshot; draft không lộ. Các policy fallback chỉ thông báo liên hệ, chưa thay nội dung pháp lý của cửa hàng.
- Đơn mới snapshot phí/expiry/thời hạn trả; đơn cũ không tính lại. Hủy lịch đọc cấu hình hiện hành. Loyalty thresholds tiếp tục nguồn loyalty_rules giai đoạn 3. Doanh thu trừ returnedAmount tại kỳ paidAt gốc; top sản phẩm còn là bán gộp trước trả. Chặn legacy full refund nếu có yêu cầu trả chưa bị từ chối để tránh hoàn hai lần.

## Kiểm chứng

- `tools/test-sqlserver.ps1` → `mvn test`: **303 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. SQL Server integration dùng database ngẫu nhiên `petcarehub_test_*`, tự dọn; kiểm schema mới/nâng cấp legacy/checksum/Hibernate validate tới V21. Các bài nghiệp vụ/API/concurrency chạy H2; không tuyên bố tất cả 303 chạy SQL Server. Không Docker hoặc thay đổi database ứng dụng.
- AdminContentSettingsTest **7 bài**: kiểm đủ 16 routes/guard/các role/đúng 10 write audit; draft privacy/publish/gỡ/restore/slug trùng; featured lọc hàng tắt/20 revision; biên settings/audit/snapshot đơn mới và cũ/hold/return/cancellation; export owner-only/one-time/expired; ownership/trạng thái/số lượng/trần tiền/chặn hoàn toàn bộ/doanh thu; quá hạn và PET bị từ chối.
- PhaseFiveConcurrencyTest **1 bài**: hai thread dùng fixture đã commit cùng nhận hàng → kho +1; cùng hoàn → returnedAmount đúng và một payment event; cùng tải → đúng một lần thành công. Không coi đây là load test hoặc kiểm hết mọi interleaving SQL Server.
- Sau sửa câu giải thích doanh thu cho khớp hoàn từng phần, chạy lại AdminInsightsTest + AdminContentSettingsTest bằng Java 17: **13 bài đạt, Maven exit 0**; không thay logic sau bộ 303 bài. Lần thử bổ sung trong sandbox dùng Java mặc định bị treo ở khởi tạo test và đã dừng; kết quả nêu ở đây là lần chạy Java 17 hoàn tất ngoài sandbox.
- `npx tsc --noEmit`, `npm run lint`, `npm run build`: **exit 0**. Không thêm thư viện. `git diff --check` sạch.
- Chrome headless thật + backend H2 tạm + frontend build: login, tạo nháp/public404, publish, script literal không chạy, lưu settings, enqueue → READY → tải CSV một lần, customer tạo yêu cầu từ đơn thật → staff duyệt/nhận hàng/hoàn tiền mặt → khách thấy returnedAmount. Năm màn admin ở **320/360/390/1366px**, không tràn ngang; public articles/article/FAQ/terms ở 320px; Tab/Escape dialog settings; không pageerror. Luồng khách dùng browser context riêng. [Ảnh và phạm vi](admin-phase-5-evidence/README.md). Đây là môi trường cách ly, chưa chứng nhận production.
- Quét phần admin mới không có `findAll()` không giới hạn, không `dangerouslySetInnerHTML`. Query scalar/page/cap. `mockImages` còn ở ảnh trình bày cũ (auth/dịch vụ/gói), không giả dữ liệu admin. `LocalDateTime.now()` không Clock còn ở health/error timestamp cũ; nghiệp vụ mới dùng Clock. Endpoint anonymous/role sai được kiểm bởi ma trận, không trả 500 do thiếu principal.

## Giới hạn, NEEDS DECISION và bước vận hành

- **Backup — NEEDS DECISION**: chủ hệ thống chốt RPO/RTO, lịch/retention, lưu ngoài máy, mã hóa/quản lý khóa và người có quyền restore. Đã viết runbook bảo thủ, chưa tự thực thi lên DB chính. CSV không thay backup. Sau restore cần đối soát giao nhận/tiền mặt và job export có thể quay lại trạng thái chưa tiêu thụ.
- Export tạo CSV trong bộ nhớ có trần, lưu tạm DB; không dành cho xuất toàn bộ dữ liệu không giới hạn. Worker tắt thì payload hết hạn chưa được dọn đến khi chạy lại, nhưng download từ chối theo deadline. Metadata/history/idempotency chưa tự xóa; cần retention khi quy mô tăng. Một lần tải được tính tại lúc server chấp nhận, lỗi mạng phải tạo bản xuất mới.
- Trả chỉ sản phẩm của đơn đã giao/thu tiền, không pet/gói. Đơn legacy không deliveredAt không tự cho trả. Đổi hàng bằng trả rồi mua mới. Nhập kho chỉ sau nhân viên xác nhận hàng còn bán được; chưa có kho cách ly/hàng hỏng. Tiền mặt chỉ đánh dấu thủ công, không dịch chuyển tiền. Không tự hoàn phí giao hàng/thuế/voucher/điểm; nếu cần đổi chính sách, bổ sung quy tắc kế toán và điều chỉnh có audit, không sửa snapshot đơn cũ. Thu hồi điểm bằng luồng điều chỉnh thủ công hiện có sau đối soát.
- NEEDS DECISION cũ về biến thể/nhận nuôi/quota gói/ảnh URL vẫn theo [giai đoạn 3](admin-phase-3-design-decisions.md); không tự mở rộng mô hình trong giai đoạn 5.
- Trước ra mắt: duyệt nội dung chính sách/liên hệ/ảnh, backup/restore thử và migration V21 trên bản sao, kiểm quyền nội bộ/bootstrap ADMIN theo runbook, smoke test trên hạ tầng đích. Ảnh/UI đã kiểm bằng Chrome thật tại môi trường test, không thay kiểm sau deployment.
- Các truy vấn chỉ đọc đối soát tài khoản nhân viên/ADMIN đáng ngờ, active admins, giỏ chứa sản phẩm tắt, pet PENDING thiếu đơn, voucher vượt lượt nằm nguyên trong [admin guide](admin-guide.md#kiểm-tra-dữ-liệu-trước-vận-hành-chỉ-đọc-tự-chạy-khi-cần); **chưa tự chạy trên DB chính**.
- Tất cả thay đổi thuộc giai đoạn 5; `.worktrees/` có sẵn không đưa vào commit. Không push/merge/rebase hoặc chạy migration thật.
