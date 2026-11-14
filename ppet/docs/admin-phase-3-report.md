# Admin Portal — báo cáo giai đoạn 3

Ngày 2026-11-15. Nhánh `feature/admin-3-catalog`, nền `8e34108` giai đoạn 2. Đã triển khai và kiểm chứng phần danh mục được chốt; hạn mức gói cũ chưa có dữ liệu, biến thể và nhận nuôi dừng ở thiết kế. Không push/merge/rebase, không áp migration vào database chính, không thêm dependency. `.worktrees/` có sẵn giữ nguyên ngoài commit.

## Tính năng và commit

| Nhóm | Trạng thái | File chính | Commit |
|---|---|---|---|
| N3-P0: CRUD sản phẩm, danh mục cha-con, tắt/xóa an toàn, không sửa tồn qua form | ĐÃ XONG | AdminCatalogService, CatalogPolicy, CatalogPage, CatalogForms | 8415b2e |
| N3-P1: ngưỡng tồn, tổng hợp SQL, bulk preview/apply nguyên transaction | ĐÃ XONG | AdminProductBatch, CatalogBatch, Inventory.reorderPoint | 8415b2e |
| N3-P2: CSV kiểm từng dòng/preview/atomic apply | ĐÃ XONG | AdminProductBatch, CatalogBatch | 8415b2e |
| N3-P2: biến thể | NEEDS DECISION — thiết kế | admin-phase-3-design-decisions.md | Commit tích hợp |
| N5-P0/P1: thú cưng bán, loài/giống, nhả toàn bộ đơn, hỗ trợ pet chỉ đọc có audit | ĐÃ XONG | AdminCatalogService, CatalogPage | 8415b2e + tích hợp |
| N5-P2: nhận nuôi | NEEDS DECISION — thiết kế | admin-phase-3-design-decisions.md | Commit tích hợp |
| N6-P0: dịch vụ, bật/tắt và xóa có kiểm tham chiếu | ĐÃ XONG | AdminCatalogService, CatalogConfig | 8415b2e |
| N6-P1: giờ mở cửa, ngày đóng cửa, ca/nghỉ, giờ trống và chọn hủy lịch ảnh hưởng | ĐÃ XONG | BusinessSchedule, AdminScheduleService, AppointmentServiceImpl, CatalogSchedule | e5cfae4 |
| N6-P2: hiệu suất theo kỳ bằng SQL | ĐÃ XONG | AdminScheduleService, StaffPerformance | e5cfae4 |
| N7-P0: mã khuyến mãi, bất biến khi đã dùng, danh sách sử dụng | ĐÃ XONG | AdminCatalogService, PromotionRepository, CatalogPage | 8415b2e + c23fcfe |
| N7-P1: tỷ lệ tích/đổi, ngưỡng, điều chỉnh điểm có khóa/ledger/audit | ĐÃ XONG | LoyaltyPolicy, AdminMarketingService, CheckoutPricing, LoyaltyPage | c23fcfe |
| N7-P1: gói chăm sóc, danh sách đăng ký, hủy/gia hạn | MỘT PHẦN — CRUD/đăng ký xong; chưa có quota/lượt dùng lịch sử | AdminMarketingService, CatalogMarketing | c23fcfe |
| N7-P2: chiến dịch theo nhóm, preview, hạn tần suất, code riêng, thông báo web | ĐÃ XONG trong trần 500 khách | AdminMarketingService, PromotionAudience/Campaign, CatalogMarketing | c23fcfe |
| Guards, route/menu, kiểm thử ma trận và audit, migration, tài liệu | ĐÃ XONG | SecurityConfig, AdminCatalogExtrasController, AdminPortal, AdminCatalogTest, EndpointSecurityMatrixTest, FlywayMigrationTest | Commit tích hợp kèm báo cáo |

Migration V19 và CRUD dùng chung nằm ở commit đầu, endpoints phụ/menu/kiểm thử ở commit tích hợp. Triển khai toàn bộ nhánh; không cherry-pick một màn hình tách khỏi backend/migration.

## Ma trận quyền sau triển khai

Tiền tố `/api/v1/admin`; ADMIN có toàn bộ. Anonymous **401**, CUSTOMER **403** cho mọi endpoint admin. Ma trận giai đoạn 1/2 giữ nguyên, xem [giai đoạn 1](admin-phase-1-report.md), [giai đoạn 2](admin-phase-2-report.md). Đây là 27 mapping mới (11 đọc, 16 ghi); `{resource}` được kiểm riêng cho cả 8 loại, `{kind}` gồm hours/holidays/shifts/time-off.

| Endpoint | SALES_STAFF | WAREHOUSE_STAFF | VETERINARIAN |
|---|---|---|---|
| GET /catalog/{resource}, /catalog/{resource}/{id} | Có | Chỉ products/categories | 403 |
| POST /catalog/{resource}, PUT/DELETE /catalog/{resource}/{id} | Có | 403 | 403 |
| GET /catalog/products/low-stock | Có | Có | 403 |
| POST /catalog/products/bulk-preview, bulk-apply, csv-preview, csv-apply | Có | 403 | 403 |
| POST /catalog/pets/{id}/release-reservation | Có | 403 | 403 |
| GET /pet-support/{id} | 403 | 403 | Chỉ pet có lịch được giao; ngoài phạm vi 404 |
| GET/POST /schedules/{kind}, PUT/DELETE /schedules/{kind}/{id} | 403 | 403 | 403 |
| GET /schedule-affected; POST /schedule-cancel | 403 | 403 | 403 |
| GET /staff-performance | 403 | 403 | 403 |
| GET/PUT /loyalty-rules; POST /loyalty/{userId}/adjust | 403 | 403 | 403 |
| GET /promotion-usages/{id}, /campaign-audience; POST /campaigns | Có | 403 | 403 |
| GET /subscriptions; POST /subscriptions/{id} | Có | 403 | 403 |

`GET /admin/me/permissions` vẫn từ **PermissionService**, không thêm role:

| Role | Permissions |
|---|---|
| ADMIN | PORTAL_ACCESS, USER_MANAGE, REPORT_VIEW, AUDIT_VIEW, SYSTEM_MANAGE, ORDER_MANAGE, CATALOG_MANAGE, CATALOG_READ, INVENTORY_MANAGE, PROMOTION_MANAGE, SALE_PET_MANAGE, REVIEW_MANAGE, CONTENT_MANAGE, PACKAGE_MANAGE, APPOINTMENT_MANAGE, ASSIGNED_APPOINTMENT_MANAGE, ASSIGNED_MEDICAL_MANAGE |
| SALES_STAFF | PORTAL_ACCESS, ORDER_MANAGE, CATALOG_MANAGE, CATALOG_READ, INVENTORY_MANAGE, PROMOTION_MANAGE, SALE_PET_MANAGE, REVIEW_MANAGE, CONTENT_MANAGE, PACKAGE_MANAGE, ASSIGNED_APPOINTMENT_MANAGE |
| WAREHOUSE_STAFF | PORTAL_ACCESS, CATALOG_READ, INVENTORY_MANAGE |
| VETERINARIAN | PORTAL_ACCESS, ASSIGNED_APPOINTMENT_MANAGE, ASSIGNED_MEDICAL_MANAGE |
| CUSTOMER | Không truy cập endpoint admin |

API legacy giữ guard cũ để tương thích; portal mới dùng API admin có audit. Không khẳng định đã hợp nhất mọi validation/audit legacy.

## Audit và xác nhận

Mỗi thao tác ghi admin mới thành công tạo đúng **một** SUCCESS cùng transaction, kể cả POST preview và replay campaign. Không có ngoại lệ bỏ audit. CUSTOMER_PET_READ ghi mỗi lần đọc hồ sơ khách; DENIED/FAILED dùng filter hiện có.

Action mới: CATALOG_CREATED, CATALOG_UPDATED, CATALOG_DELETED; PRODUCT_BULK_PREVIEW/APPLIED, PRODUCT_CSV_PREVIEW/APPLIED; PET_RESERVATION_RELEASED, CUSTOMER_PET_READ; SCHEDULE_SAVED/DELETED/APPOINTMENTS_CANCELLED; LOYALTY_RULES_UPDATED, LOYALTY_ADJUSTED; SUBSCRIPTION_UPDATED; CAMPAIGN_SENT. Bulk/cancel có số lượng và IDs, replay có cờ replay. Không ghi mã bí mật/token/điện thoại/email/nội dung tự do vào audit.

ConfirmDialog trước lưu/xóa/bulk/import/gửi. Lý do bắt buộc: xóa, nhả giữ chỗ, sửa/xóa lịch làm việc, hủy lịch, điều chỉnh điểm, hủy/gia hạn đăng ký. Không thêm yêu cầu nhập lại mật khẩu cho catalog; quy tắc mật khẩu xác nhận ở quản trị tài khoản giai đoạn 1 giữ nguyên.

## Migration, API và môi trường

- **V19__admin_catalog.sql**: cây danh mục/thứ tự, singleton khóa, snapshot tỷ lệ điểm, loyalty_rules, bốn bảng lịch, code normalized unique, audience và campaign. Chi tiết và cách áp: [database](database.md#admin-catalog-v19-2026-11-15), [runbook](migrations.md).
- Biến optional mới: `APP_CATALOG_IMAGE_HOSTS`, mặc định images.unsplash.com,images.pexels.com. Chỉ HTTPS đúng host; backend không tải URL. Không thêm secret.
- DTO loyalty khách thêm earnEvery/pointValue; thresholds/backend cùng nguồn. Đơn tạo trước khi đổi tỷ lệ vẫn giữ tỷ lệ tích đã snapshot; dữ liệu lịch sử NULL dùng mặc định lịch sử.
- Nhả pet gọi đúng luồng hủy toàn bộ đơn: hoàn sản phẩm, pet, voucher và điểm theo nghiệp vụ hiện có, có order note/thông báo. Không tách đơn hỗn hợp.
- Thay đổi lịch ảnh hưởng ngay giờ trống/đặt lịch/phân công. Lịch đã có không tự bị hủy. Danh sách bị ảnh hưởng theo ngày/nhân viên; nghỉ một phần ngày cần chọn chính xác các lịch trùng giờ.
- [API/payload/giới hạn](api.md#admin-catalog--giai-đoạn-3-2026-11-15); [hướng dẫn vận hành](admin-guide.md#danh-mục--giai-đoạn-3).

## Kiểm chứng

- `tools/test-sqlserver.ps1` chạy `mvn test`: **285 tests, 0 failures, 0 errors, 0 skipped**. Database test tên ngẫu nhiên `petcarehub_test_*`, được dọn bởi lifecycle; không Docker, không truy cập database ứng dụng để sửa dữ liệu. V19 qua schema mới/nâng cấp legacy/checksum/Hibernate validate.
- AdminCatalogTest: 7 bài kiểm thử tích hợp, thực thi nhiều HTTP request; ma trận mọi role và 8 resource, đủ 27 mappings, đủ 16 write mappings có đúng một audit. CRUD/giá/loài-giống/ngày sinh/vòng danh mục/tham chiếu; CSV lỗi không nhập một phần; stale preview; bulk trần; nhả pet hủy cả đơn/hoàn kho; lịch/ca/nghỉ và API slots; loyalty số dư và DTO; campaign membership tại checkout, retry và rate; snapshot earnEvery qua giao đơn.
- EndpointSecurityMatrixTest cập nhật inventory toàn ứng dụng và anonymous/customer/role, private-resource regression references. Thêm endpoint chưa khai báo sẽ fail. Các kiểm thử cạnh tranh checkout/loyalty/kho và SQL Server hiện có vẫn đạt; không tuyên bố đã load-test 500 khách hoặc chứng minh mọi interleaving của lịch/campaign.
- `npx tsc --noEmit`: exit 0. `npm run lint`: exit 0. `npm run build`: exit 0 (tsc -b + Vite). Không thêm thư viện.
- **Chrome thật, headless**: backend H2 tạm + build frontend; đăng nhập, tạo/sửa sản phẩm, bulk giá preview/apply, CSV preview/apply, thêm ngày đóng cửa, cập nhật tỷ lệ điểm thành công. Kiểm 17 màn ở 320/360/390/1366px, không tràn ngang; Tab nằm trong ConfirmDialog, Escape đóng; không pageerror. Đây là kiểm thử môi trường cách ly, không phải xác nhận bản production đã triển khai.
- Bằng chứng: [desktop sản phẩm](admin-phase-3-evidence/admin3-catalog-products.png), [form thú cưng 320px](admin-phase-3-evidence/admin3-catalog-pets-new-320.png). Log local không commit: backend/target/admin3-all-tests.log, admin3-proof-server.log; script proof local admin3-proof.cjs.
- Quét `findAll()` trong admin: không có; danh sách SQL projection/page, tổng hợp SQL. Public categories giữ findAll(Sort) hiện hữu về phạm vi danh mục nhỏ. Không dangerouslySetInnerHTML trong frontend. “mock” còn là tên module ảnh trang công khai, không response giả trong portal. LocalDateTime.now() không Clock còn ở health/error handlers cũ, code nghiệp vụ mới dùng Clock. NEEDS DECISION cũ là báo cáo lịch sử; các mục hiện tại ở tài liệu quyết định giai đoạn 3. `git diff --check` đạt.

## Phần còn lại và việc triển khai

**NEEDS DECISION:** biến thể sản phẩm, quy trình nhận nuôi, hạn mức/lượt dùng gói cũ — [đề xuất cụ thể](admin-phase-3-design-decisions.md). CRUD gói và quản lý đăng ký đã hoạt động, nhưng không có căn cứ để suy ra số lượt còn lại. Không ghi toàn bộ N7-P1 là hoàn tất.

Campaign đồng bộ trần 500; danh sách lịch/tuần phân trang, hiển thị rõ trang hiện tại. Có một khóa catalog chung để bảo đảm tính nhất quán; cần đo tải trước khi mở rộng quy mô. Không tự tăng trần hoặc thêm email/SMS.

Trước triển khai thật: sao lưu và kiểm khôi phục, thử V19 trên bản sao, rà trùng code hoa/thường, dừng backend cũ và chạy phiên bản mới với Flyway/validate; rà quyền, host ảnh, tài khoản ADMIN và nội dung pháp lý. Các truy vấn rà tài khoản nhân viên đáng nghi, ADMIN hoạt động, sản phẩm tắt trong giỏ, pet PENDING không có đơn, voucher vượt lượt ở [admin guide](admin-guide.md#kiểm-tra-dữ-liệu-trước-vận-hành-chỉ-đọc-tự-chạy-khi-cần), **không tự chạy trên DB chính**. Giai đoạn 4/5 chưa triển khai trong nhánh này.
