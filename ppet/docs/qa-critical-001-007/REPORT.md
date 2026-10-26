# Sửa lỗi QA Critical 001–007

## Bước 0 — nhánh và phạm vi

- Workspace thực tế: `D:/visual studio/PetCare-Hub/ppet` (đường dẫn trong yêu cầu là `D:/PetCare-Hub/ppet`).
- Trước khi bắt đầu: `main cfd890d`, `git status -s` chỉ có `?? ppet/docs/qa/` (báo cáo QA trước đó). Không có thay đổi Đợt G chưa commit; bốn file được bảo vệ sẽ so với base `44027f8` sau khi chuyển nhánh.
- Đã chạy `git branch -a`, `git log --oneline --graph --all -30`. `fix/audit-25-58` ở `44027f8`; `feature/pet-purchase-quick-add` ở `27fb305` có bốn commit tính năng riêng.
- `git merge-base --is-ancestor cfd890d HEAD`: exit 0 trên main ban đầu; exit 1 sau khi tạo `fix/qa-critical-001-007` từ `fix/audit-25-58`.
- `git diff --stat <merge-base HEAD main>..main`: trước chuyển nhánh rỗng; sau chuyển nhánh: xóa `ppet/frontend/write_components.py` (204 dòng), `write_home.py` (419 dòng), `write_productlist.py` (159 dòng). `git log HEAD..main` chỉ có `cfd890d`.
- **NEEDS DECISION — hợp nhất:** chưa merge/rebase main hoặc feature. Ba script trên là phần main khác base; source backend/frontend tính năng trên feature cũng cần review khi tích hợp, đặc biệt `OrderServiceImpl`, `PetServiceImpl`, DTO pet/order và checkout. Phương án đã chọn: sửa trên base được chỉ định; hợp nhất bằng PR sau, chạy lại toàn bộ test trên kết quả hợp nhất.
- Không chạy ứng dụng với profile dev, không thao tác DB dev hoặc SQL reset. Test tự động dùng profile `test`, H2. SQL Server Testcontainers hiện có chỉ chạy khi bật biến opt-in riêng.
- Đã đọc SecurityConfig, RoleType, toàn bộ controller, Ownership, UserDetailsImpl, JWT/filter/properties, GlobalExceptionHandler, DataSeeder/AdminBootstrap, application profiles và các test audit/security. Method security đã bật; JWT xác minh chữ ký rồi tải quyền hiện tại từ DB.

## Nhật ký xác minh từng lỗi

### BUG-001

- Base đã ép CUSTOMER trong UserFactory; public DTO vẫn chứa enum `role`, dùng chung với tạo nhân viên. Endpoint đổi role có thể hạ quyền admin cuối cùng.
- Test mới `QaRegistrationSecurityTest`: 5 ca đỏ trước sửa (payload đặc quyền thừa bị 400; admin bị hạ quyền). Sau sửa, public DTO bỏ role và bỏ qua field không được khai báo; tạo nhân viên dùng StaffCreateRequest riêng. ADMIN role change được kiểm cả controller/service; không hạ quyền admin qua endpoint chung.
- Giữ các test `StaffRegistrationTest` về mọi RoleType và quyền tạo nhân viên. Login/token test kiểm DB role CUSTOMER và truy cập admin 403.
- Quét mass assignment: ProfileUpdateRequest chỉ tên/email/phone; UserController gán từng field. Product/service/promotion active/price chỉ qua endpoint nhân viên; createMine pet ghi đè owner/price/health/availability. Không có endpoint người dùng tự sửa enabled/tier/points qua DTO profile.
- **NEEDS DECISION:** muốn gỡ một admin phải có luồng quản trị riêng được tuần tự hóa và bảo vệ admin cuối cùng. Hiện chặn mọi lần hạ quyền một admin qua endpoint generic; tạo/cấp quyền ADMIN từ admin vẫn theo API sẵn có.

BUG-001: full Maven PASS, 193 test / 0 lỗi / 3 skip SQL Server; commit `4cced7d`.

### BUG-002

- **ĐÃ ĐƯỢC SỬA TRƯỚC ĐÓ** đối với phân quyền CRUD ba controller: ADMIN/SALES_STAFF; mọi role khác bị từ chối. GET storefront đã ép active=true; `/products/manage` có guard riêng.
- Bổ sung lớp URL theo HTTP method cho catalogue/service, clinical, inventory, pet quản trị, review approve, loyalty earn và order transitions; giữ guard method hiện có. Auth public thu hẹp còn POST login/register.
- Red: `CatalogAuthorizationTest.unauthorizedMalformedWritesAreRejectedBeforeDeserialization` trả 400 thay vì 403. Green: full Maven PASS. Anonymous POST/PUT/DELETE cả ba catalogue đều 401; matrix tất cả RoleType vẫn PASS.
- **NEEDS DECISION:** giữ quyền SALES_STAFF quản lý promotion theo ma trận đã có; đổi thành ADMIN-only cần sửa đồng thời SecurityConfig, PromotionController và test matrix.
- `EndpointSecurityMatrixTest` lập inventory tường minh mọi endpoint từ RequestMappingHandlerMapping; endpoint mới chưa khai báo làm test fail. Kiểm anonymous cho mọi route, CUSTOMER không gọi được write ngoài allowlist. Những write không có role annotation nhưng dành cho chủ tài khoản được giữ: cart, profile, pet cá nhân, notifications, quiz, booking/cancel, order/cancel, review, loyalty spend (xử lý tiếp ở BUG-007).
- Không phát hiện controller quản trị nào ngoài danh sách trên còn cho CUSTOMER ghi; API order transition đã có guard service. Lớp URL bổ sung giúp từ chối trước validation.

### BUG-003

- Red: 5/6 ca trong `QaAppointmentScopeTest,PrivatePetDataTest` thất bại: staff xem hai lịch thay vì chỉ lịch được giao; truy cập pet/clinical/notification của người khác trả 403 thay vì 404. Base đã chặn đọc chéo CUSTOMER, chưa đúng phạm vi staff và hợp đồng che giấu tài nguyên.
- Thêm truy vấn pet theo id+owner, notification theo id+user, order theo id+customer; khóa appointment/order được giới hạn người được phép trong query. Clinical dùng Ownership.requirePet để truy vấn chủ hoặc cho VETERINARIAN/ADMIN. Nhân viên chỉ liệt kê lịch của mình, ADMIN xem toàn bộ; bác sĩ xem lịch theo pet cũng bị giới hạn phân công.
- Thêm `/appointments/me`, frontend bookingService có `getMyAppointments`; alias `/user/{userId}` còn tương thích, kiểm userId. Legacy quiz URL có ID khác principal nay 403 trước persistence.
- `QaAppointmentScopeTest` dùng JWT thật cho hai CUSTOMER có lịch/pet riêng và hai bác sĩ; `PrivatePetDataTest` có hồ sơ/tiêm chủng/thông báo thật trong H2. Các test OrderFlow/TreatmentFlow/CartMutation bao phủ tài nguyên riêng tương ứng.
- Cập nhật test cũ từ 403 sang 404 chỉ cho tài nguyên không thuộc người gọi; userId chéo vẫn 403. Mock repository được đổi theo query có ownership; không bỏ assertion nghiệp vụ.
- Full Maven PASS sau sửa; frontend `tsc --noEmit`, lint, build PASS. Dependency được cài đúng lockfile audit qua npm ci, không sửa manifest. npm ci báo 1 advisory high có sẵn, ngoài phạm vi không cập nhật dependency.
- **NEEDS DECISION:** lịch hẹn là dữ liệu theo người được phân công. Nếu sales cần toàn bộ lịch, cần quyền nghiệp vụ riêng; hiện giữ ADMIN là vai trò duy nhất xem toàn bộ.

### BUG-007

- Base đã kiểm điểm dương/trần 1.000.000, chống tràn int, khóa `User` ổn định trong transaction trước đọc/tạo tài khoản điểm, ghi ledger cùng transaction. Hai lần trừ 80 từ 100 chỉ một thành công; dư 20, một EARN và một REDEEM. Đây là phần **ĐÃ ĐƯỢC SỬA TRƯỚC ĐÓ**.
- Frontend LoyaltyPage chỉ GET account/history; checkout không gọi earn/spend. Không có redemption UI cần giữ. Các alias HTTP spend nay ADMIN-only giống add; guard cả filter/controller/service. CUSTOMER chỉ đọc tài khoản/lịch sử của mình. Giao dịch vẫn dùng số điểm dương, loại REDEEM biểu diễn chiều trừ.
- Red `QaLoyaltyBoundaryTest`: 3/4 ca fail (CUSTOMER tự tiêu điểm, service bypass, history resource trả 403). Green full Maven PASS, cả concurrency. DTO request param thêm Positive/Max; service vẫn kiểm lại, số dư/ledger không đổi khi invalid hoặc thiếu điểm. History lọc accountId+customerId, trả 404 cho tài khoản điểm khác.
- Test cũ tiêu điểm trực tiếp bằng CUSTOMER được cập nhật theo quyền ADMIN; test race dùng actor ADMIN để kiểm cơ chế khóa, không làm yếu kiểm dư/ledger.
- **NEEDS DECISION:** endpoint spend cũ chỉ còn phục vụ điều chỉnh ADMIN, không tích hợp checkout vì chưa có chính sách tỷ giá/giảm giá/hủy đơn. Khi có chính sách, tạo workflow nội bộ checkout và cùng transaction, không mở lại tùy ý endpoint số dư cho khách.
- **NEEDS DECISION dữ liệu:** không tự sửa lịch sử điểm cũ; cần đối soát REDEEM âm, số dư bất thường bằng truy vấn trong phụ lục trước khi điều chỉnh.

### BUG-005

- Base đã lọc owner=NULL + AVAILABLE tại query storefront và dùng PublicPetDTO; test thêm query ownerId/availability giả và size quá lớn đều giữ giới hạn. Chủ xem DTO riêng qua my-pets; tài khoản khác 404.
- Phát hiện đường rò còn lại: quiz chọn mọi pet AVAILABLE dù có owner và trả PetDTO chứa healthStatus. Red: 3 ca fail (quiz vẫn xếp hạng pet có chủ, JSON có healthStatus, detail RESERVED bị 404).
- Green: query quiz cũng ép owner NULL, MatchResultDTO.pet dùng PublicPetDTO; cập nhật TypeScript tương ứng. Không lưu xếp hạng pet đã có chủ. Chi tiết unowned RESERVED được xem với nhãn “Thú cưng đang được đặt”; danh sách vẫn chỉ AVAILABLE. SOLD/UNAVAILABLE/owned luôn 404 ở public detail.
- **NEEDS DECISION:** enum thật là RESERVED, không có PENDING. Dùng RESERVED cho pet đang được đặt, không thêm enum/schema. Nhánh pet-purchase riêng phải giữ cùng quy tắc khi tích hợp; pet cá nhân owner khác null không bao giờ được public dù availability sai.
- Full Maven và frontend tsc/lint/build PASS. Không sửa seed/schema. Chỉ đọc ID pet cần đối soát bằng SELECT, không thay đổi DB.
