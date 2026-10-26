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

Các kết quả test, commit và ma trận cuối được ghi sau từng bước.
