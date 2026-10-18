# High lượt 2 — tiến độ

## Bước 0 (2026-10-18)

- Git sạch tại `5c6dff2`, nhánh `fix/critical-jwt-secret`; đã kiểm tra status, branch và log 60 commit. Không tạo nhánh, không thao tác stash.
- Đã đọc báo cáo Medium, High lượt 1, Critical #1, mã hiện hành và diff từ `b4286c6` của các service/repository/schema liên quan.
- Baseline: Maven 127 test = 126 pass, 0 failure/error, 1 skip (`SqlServerConstraintTest`, Docker opt-in chưa bật); frontend build pass. Đã restore tsconfig.app.tsbuildinfo.
- Lệnh Maven trên Java 25 host: `cmd.exe /c ".\mvnw.cmd -q -Dnet.bytebuddy.experimental=true test"`; release target 17, profile test/H2. Log baseline: `backend/high-2-baseline.log` (ignored).

## Bản đồ hiện trạng trước sửa

| File/nhóm | Hành vi đã có cần giữ |
|---|---|
| `security/Ownership`, controller Appointment/Loyalty/Cart | Typed enabled principal; userId/authorizedUserId/requireUser; ownedPet chỉ chủ thật, requirePet cho ADMIN/VET; không thêm helper/role/handler |
| `AppointmentServiceImpl`, `UserRepository`, `AppointmentRepository` | #28 BookingProperties tương lai/giờ làm/thời lượng; #29 staff enabled + role, sắp ID; #30 khóa PESSIMISTIC_WRITE hàng nhân viên trong transaction rồi kiểm tra overlap; hết người trả IllegalStateException ->409. #8/#9 quyền và pet owner; #32 canTransitionTo và khóa lịch khi đổi trạng thái |
| `InventoryServiceImpl`, `InventoryRepository` | #25 add/reduce/adjust đã khóa hàng kho, validate dương/nonnegative, chống overflow, giữ nhật ký cùng transaction. #43 findByProductIdIn tải kho theo trang |
| `LoyaltyServiceImpl` | #5 controller ownership, #6 points 1..1.000.000, chống overflow; #47 tier/lifetime/threshold DTO. getOrCreate/add/spend còn không khóa |
| `CartServiceImpl`, `CartItem` | Kiểm tra active và tổng lượng so với kho; add còn không khóa, thiếu unique dòng sản phẩm. update/remove/clear là TODO #15, không sửa |
| `ProductServiceImpl` | #22 thực tế đã tạo Inventory=0 rồi gọi addStock nếu >0 trong cùng transaction từ Medium. #40 update cấm stockQuantity, kiểm tra category; #43 batch-load kho |
| `Inventory` và SQL | #55 schema IF NOT EXISTS, reset riêng có guard; migration 001 đã đổi UNIQUE nullable sang filtered index + CHECK nonnegative. Schema tạo mới và OneToOne/unique mapping vẫn có UNIQUE nullable. Seed đã có nhiều dòng inventory; giữ dev opt-in và không sinh dữ liệu che lỗi |
| JWT/profile | Giữ nguyên Critical #1, cấu hình test riêng, JWT_SECRET bắt buộc, không default dev |

## Kế hoạch còn lại

1. #20: XONG, giữ code đã đúng; thêm AppointmentCapacityTest (4 test): HTTP 409 hết người/không ai eligible, 3 yêu cầu tự phân công trên 2 nhân viên chỉ 2 thành công, lịch liền kề và hủy giải phóng chỗ. Full Maven 131 = 130 pass, 0 fail/error, 1 skip; frontend build pass, đã restore buildinfo.
2. #21: XONG, khóa hàng user trước get/create và cập nhật loyalty/cart; GET tạo account cũng transactional. Giữ khóa kho hiện có. Migration 002 + schema tạo filtered unique cart/product, dừng nếu có duplicate; không thêm version. 7 test race mới pass (tiêu 80/100, cộng/đọc lần đầu, giỏ mới/cũ, nhập kho và nhập–xuất). Full Maven 138 = 137 pass, 0 fail/error, 1 skip; frontend build pass, đã restore buildinfo. Test cũ chỉ thêm stub khóa user, giữ assertion. Đọc snapshot tồn kho trước khóa user để tránh đảo thứ tự kho -> audit-user; giỏ không giữ chỗ hàng.
3. #12: đồng bộ schema khởi tạo + mapping inventory với filtered indexes của migration 001, kiểm tra seed/migration và mở rộng test SQL Server opt-in (không chạy SQL thật).
4. #22: giữ code đã đúng, kiểm thử persistence/rollback/khởi tạo stock và nhập kho tiếp theo.
5. Sau từng nhóm chạy full Maven + frontend build, restore buildinfo; ghi commit và kết quả trong REPORT.md.
