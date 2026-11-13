# Giai đoạn 2 — vận hành quản trị

Ngày 2026-11-13. Nhánh `feature/admin-2-operations`, nền `e42caab` của giai đoạn 1. Hoàn tất phạm vi vận hành; không triển khai giai đoạn 3–5, không push/merge/rebase hoặc áp migration vào database thật. Không thêm dependency. `.worktrees/` có sẵn được giữ nguyên ngoài commit.

## Kết quả theo nhóm

| Nhóm | Trạng thái | Mã nguồn chính | Commit |
|---|---|---|---|
| N4-P0: hàng đợi/chi tiết, chuyển trạng thái, hủy hỗn hợp, hoàn tiền thủ công | ĐÃ XONG | AdminOrderOperations, OrderServiceImpl, OperationOrders | 958b101 |
| N4-P1: ghi chú nội bộ, in snapshot, lịch sử/đối soát tiền mặt | ĐÃ XONG trong phạm vi tiền mặt | OrderNote, OrderPaymentEvent, V18, OperationOrders | 958b101 |
| N3-kho: nhập/xuất/kiểm kê, khóa tồn, lịch sử có nguồn/tham chiếu | ĐÃ XONG | AdminInventoryOperations, InventoryServiceImpl, OperationInventory | b733e22 |
| N6-P0-lịch: bảng/tuần, lọc, phân công/đổi, phạm vi nhân viên, hủy và thông báo | ĐÃ XONG | AdminAppointmentOperations, OperationAppointments | 3b44e6b |
| N8-P0: hàng đợi/duyệt/từ chối/ẩn/hiện, số chờ duyệt | ĐÃ XONG | AdminReviewOperations, OperationReviews, AdminPortal | ea37121 + commit tích hợp |
| URL/method guards, menu/routes, kiểm thử API và audit | ĐÃ XONG | AdminOperationsController, SecurityConfig, PermissionService, AdminOperationsTest | Commit tích hợp kèm báo cáo này |
| Gateway/email/social login/quên mật khẩu | KHÔNG LÀM THEO QUYẾT ĐỊNH | Chỉ COD/tiền mặt và thông báo web | — |
| CRUD catalog, giờ kinh doanh DB, CMS, trả hàng, cấu hình vận hành | CHƯA TRIỂN KHAI trong giai đoạn này | Theo giai đoạn 3–5 | — |

N4 chứa cả helper phân trang/form và migration dùng chung; các nhóm N3/N6/N8 được nối route/guard trong commit tích hợp. Phải triển khai toàn bộ nhánh, không cherry-pick riêng màn hình thiếu API.

## Ma trận endpoint sau triển khai

Tiền tố `/api/v1/admin`. ADMIN được toàn bộ. Anonymous 401; CUSTOMER 403 toàn bộ. Các vai trò còn lại:

| Endpoint | SALES_STAFF | WAREHOUSE_STAFF | VETERINARIAN |
|---|---|---|---|
| GET /orders, /orders/{id}, /orders/{id}/print, /orders/{id}/notes, /orders/{id}/payments | Có | 403 | 403 |
| POST /orders/{id}/transition, /request-refund, /mark-refunded, /notes | Có | 403 | 403 |
| GET /inventory, /inventory/transactions; POST /inventory/movements | Có | Có | 403 |
| GET /appointments, /appointments/{id}; POST /appointments/{id}/status | Chỉ được giao | 403 | Chỉ được giao |
| GET /appointments/{id}/eligible-staff; POST /appointments/{id}/assign | 403 | 403 | 403 |
| GET /reviews, /reviews/count; POST /reviews/{id}/moderate | Có | 403 | 403 |

Sai phạm vi lịch hẹn trả 404, kể cả filter staff khác. Quyền `GET /admin/me/permissions` không đổi so với [giai đoạn 1](admin-phase-1-report.md#quyền-endpoint-sau-triển-khai): ADMIN toàn bộ; SALES_STAFF có ORDER_MANAGE/INVENTORY_MANAGE/REVIEW_MANAGE/ASSIGNED_APPOINTMENT_MANAGE; WAREHOUSE_STAFF có INVENTORY_MANAGE; VETERINARIAN có ASSIGNED_APPOINTMENT_MANAGE. Chỉ ADMIN có APPOINTMENT_MANAGE. Những quyền catalog khác đã có sẵn không đồng nghĩa chức năng giai đoạn sau đã hoàn tất.

[Hợp đồng API](api.md#admin-operations--giai-đoạn-2-2026-11-13) liệt kê đủ 20 endpoint, payload, bộ lọc và whitelist sort. Mọi hàng đợi được phân trang tại SQL, trần 100; không tải toàn bộ rồi lọc trong Java.

## Dữ liệu, audit và quyết định

- V18__admin_operations.sql thêm metadata yêu cầu/biên nhận hoàn tiền, order_notes, order_payment_events, item_name_snapshot và index hàng đợi. Hibernate validate đã qua trên SQL Server. Không sửa migration cũ. Áp dụng Flyway sau sao lưu và xác minh bản sao theo [runbook](migrations.md).
- Tên sản phẩm/thú cưng snapshot từ checkout mới; đơn cũ thiếu snapshot giữ tên catalog hiện tại. Gói dùng snapshot đã có. Phiếu đọc giá, số lượng, thuế, phí, giảm giá, địa chỉ và mốc đã lưu. Không backfill tên/biên nhận/mốc lịch sử thiếu bằng chứng.
- Hoàn tiền toàn bộ đơn, thao tác thủ công sau khi thực trả tiền; khóa đơn + unique event chặn trùng. Cùng biên nhận có thể retry; khác biên nhận sau khi hoàn trả 409. Doanh thu loại REFUNDED bằng predicate hiện có. Không tự hoàn kho/chuyển chủ/thu hồi điểm đã kiếm/hủy gói; trả hàng thuộc giai đoạn 5.
- Mỗi POST admin thành công có đúng một SUCCESS cùng transaction; retry hoàn tiền có audit riêng nhưng không có giao dịch tiền trùng. Đọc chi tiết/phiếu/ghi chú/sổ tiền/lịch hẹn có audit. FAILED/DENIED riêng như giai đoạn 1.
- Action: ORDER_CONFIRM/PROCESS/SHIP/DELIVER/CANCEL, ORDER_REFUND_REQUESTED, ORDER_REFUNDED, ORDER_NOTE_ADDED; INVENTORY_STOCK_IN/STOCK_OUT/ADJUSTMENT; APPOINTMENT_ASSIGNED, APPOINTMENT_STATUS_CHANGED; REVIEW_APPROVED/REJECTED/HIDDEN. Đọc: ORDER_DETAIL_READ, ORDER_PRINT_READ, ORDER_NOTES_READ, ORDER_PAYMENTS_READ, APPOINTMENT_READ.
- Lý do bắt buộc: hủy đơn/lịch, nhập/xuất/kiểm kê, phân công, yêu cầu/xác nhận hoàn, từ chối review. Không bổ sung yêu cầu nhập lại mật khẩu cho vận hành. Audit không chứa nội dung ghi chú/review, biên nhận, mật khẩu/token hoặc thông tin liên hệ.
- Không có biến môi trường mới. `.env.example` backend/frontend nhắc V18, API base và timezone hiện có. API khách không nhận internal notes. Admin detail có wrapper mới; endpoint nghiệp vụ cũ còn giữ để tương thích, portal dùng URL admin mới để ghi audit.
- Không có NEEDS DECISION mới trong phạm vi này. Các mặc định Q1–Q15 giữ nguyên; các nội dung cần quyết định ở giai đoạn sau vẫn theo [admin guide](admin-guide.md).

## Kiểm chứng

- `tools/test-sqlserver.ps1` chạy `mvn test`: **278 tests, 0 failures, 0 errors, 0 skipped**. Database `petcarehub_test_<UUID>` riêng trên SQL Server cài sẵn, được guard tạo/dọn; không Docker, không dùng `petcarehub` thật.
- AdminOperationsTest kiểm inventory đủ 20 endpoint, URL/method guards với 5 role, audit mỗi write; state/refund lặp, mixed cancel, ghi chú không lộ; xuất âm/0/vượt tồn; nhân viên sai vai trò/bị khóa/trùng lịch/sai phạm vi; cap/LIKE escape/sort/date; pending/hidden không lộ qua API công khai và summary chỉ đếm approved.
- FlywayMigrationTest nâng cấp schema trống/legacy tới V18, validate, giữ dữ liệu và chạy lại không đổi; test mới kiểm Unicode ghi chú/biên nhận và unique cash event. Các test cạnh tranh tồn kho, pet/booking và state machine hiện có vẫn qua.
- Test cũ được sửa có chủ đích: PhaseThreeOwnershipTest dùng UserDetailsImpl có ID nhân viên đã lưu để ghi actor thu tiền; OrderPetValidationTest cập nhật dependency EntityManager; AdminFoundationTest chỉ kiểm controller nền tảng, AdminOperationsTest kiểm controller vận hành, EndpointSecurityMatrixTest vẫn kiểm inventory toàn cục. Không bỏ test để vượt lỗi.
- `npx tsc --noEmit`: PASS. `npm run lint`: PASS. `npm run build` (gồm `tsc -b`): PASS. Không sửa package.json/package-lock.json.
- Chrome headless, backend H2 tạm độc lập, không mock response: đăng nhập thật; chuyển đơn qua đủ 4 bước; yêu cầu/ghi nhận hoàn; thêm ghi chú; phiếu in không chứa ghi chú nội bộ; nhập kho và xem history; chọn nhân viên từ trang 2/2, đổi trạng thái; duyệt review chứa script hiển thị dưới dạng văn bản.
- 8 trang admin tại 320/360/390/1366px và lịch tuần tại 320/390/1366px: không tràn ngang, không lỗi JavaScript. Đã xem ảnh phiếu in, chi tiết đơn di động, kho desktop, lịch tuần. Ngày giờ được định dạng giờ nghiệp vụ, tiếng Việt hiển thị đúng.
- Script, log và ảnh thử nghiệm nằm ở `backend/target/admin2-*`, không commit dữ liệu thử nghiệm. Chưa kiểm screen reader hoặc toàn bộ thao tác bàn phím; đây không phải kiểm thử tải hay chứng nhận production. Chưa mô phỏng riêng hai lệnh phân công đồng thời; khóa pet/staff được tái dùng theo thứ tự booking và đã kiểm xung đột trực tiếp.
- Flyway vẫn cảnh báo SQL Server 17 mới hơn phạm vi hỗ trợ chính thức của thư viện hiện tại; không tự nâng dependency. Test tích hợp thực tế đã qua.

## Trước khi đưa vào sử dụng

Sao lưu và thử phục hồi; chạy Flyway trên bản sao trước khi áp V18; triển khai backend/frontend cùng phiên bản. Rà tài khoản/quyền và các truy vấn chỉ đọc trong [admin guide](admin-guide.md#kiểm-tra-dữ-liệu-trước-vận-hành-chỉ-đọc-tự-chạy-khi-cần), thử nghiệp vụ với nhân viên thực tế. Đối soát tiền mặt theo biên nhận thật, không dùng dữ liệu demo làm giao dịch thật. Lịch tuần chỉ trình bày trang hiện tại (20 dòng), cần phân trang hoặc lọc hẹp để xem đầy đủ.
