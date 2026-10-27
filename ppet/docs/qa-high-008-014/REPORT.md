# QA BUG-008–014 — 2026-10-27

## Bước 0

- Workspace thực tế: `D:/visual studio/PetCare-Hub`; đường dẫn trong yêu cầu là tham chiếu cũ.
- Ban đầu `fix/critical-jwt-secret` tại `48770a2`, worktree sạch; không có thay đổi Đợt G chưa commit. `cfd890d` không là tổ tiên (exit 1).
- Nhánh mới `fix/qa-high-008-014` từ `fix/qa-critical-001-007` (`f8b2192`). Chọn theo nội dung: nhánh critical có 7 commit bảo mật cần thiết, dù commit `48770a2` có thời gian muộn hơn. Không merge/rebase.
- Main-only `cfd890d` xóa 3 script Python frontend (782 dòng); `48770a2` còn thêm báo cáo QA cũ. NEEDS DECISION: hợp nhất các thay đổi này riêng.
- Audit: `e468c47` bảo vệ endpoint cá nhân (#34/#36); `cbc7cad` booking ownership; `233053e` dữ liệu pet thật; `5d9de2c` doanh thu SQL; `53eb80d` protected routes, tài khoản, link giả; `cfe10c5` ESLint config.
- Critical: `b787cdc` truy vấn ownership/appointment scope; `e89bebf` PublicPetDTO; `1aa2530` filter quyền; `538b6df` checkout; `f8b2192` secrets/test matrix.
- Feature riêng: `4fca5f1` mua pet backend, `e609a39` UI mua, `7fe4d3c` thêm nhanh backend, `27fb305` UI booking. NEEDS DECISION: các file giao nhau PetController, PetService/Impl, PetRepository, SecurityConfig, BookingPage, PetDetailPage, OrderService/DTO/tests cần đối chiếu khi hợp nhất; không tự merge.
- Hiện trạng: booking bỏ fallback và kiểm owner; trang pet đã gọi API; doanh thu đã SUM theo COMPLETED nhưng dùng createdAt và chưa chặn khoảng quá dài; auth matcher đúng nhưng lỗi filter thiếu schema/correlationId; Header chưa mobile; quên mật khẩu đã disabled. Không có hạ tầng frontend test runner; dùng probe Node không thêm thư viện và hướng dẫn kiểm thử tay.

## BUG-014

- ĐÃ ĐƯỢC SỬA TRƯỚC ĐÓ: flat config đã có từ cfe10c5. Không đổi package/lock.
- Lần lint đầu không dùng làm baseline: npm ci đã khởi động khi lint chưa kết thúc, khiến module bị thay trong lúc đọc. Lỗi nguyên văn: `Error: Cannot find module '../package.json'`, require stack `node_modules/eslint/bin/eslint.js`. Đây là lỗi trình tự kiểm tra, không phải thiếu flat config. Chạy lại tuần tự sau npm ci sạch.
- Thêm `tools/check-lint.cjs`: tạo vi phạm unused variable trong src, đòi exit 1 đúng rule rồi xóa trong finally.
- Cấu hình kế thừa tắt no-explicit-any, chưa bật hooks/refresh; không thêm suppression. Không có rule warn mới. NEEDS DECISION: nâng chính sách lint riêng để tránh đổi hành vi ngoài phạm vi trường hợp config đã tồn tại.
- CI chưa có lint. NEEDS DECISION: thêm npm run lint vào CI; chưa sửa workflow.
- Sau npm ci: lint exit 0, 0 lỗi/0 cảnh báo; probe exit 1 đúng no-unused-vars, đã xóa; TypeScript/build PASS. npm ci báo 1 advisory high và deprecation có sẵn, không sửa dependency ngoài phạm vi.

## BUG-011

- ĐÃ SỬA phần còn thiếu. 401 trước đó đã đúng; 2 test mới FAIL vì message khác và thiếu correlationId; sau sửa full backend, TypeScript/lint/build PASS.
- Filter entry point và access denied dùng GlobalExceptionHandler qua resolver; ErrorResponse sinh correlationId cho cả lỗi dự kiến. JWT sai trả 401 JSON, không log token; disabled account cũng cùng schema.
- GET `/auth/me`, `/pets/my-pets`, `/pets/my-pets/{id}`, `/appointments/me`, `/orders`, `/cart`, `/notifications`, `/notifications/unread-count`, `/loyalty/account`, `/loyalty/transactions`: anonymous trước 401 đơn giản, sau 401 JSON đầy đủ. PUT `/users/me` được bảo vệ; EndpointSecurityMatrixTest bao phủ toàn bộ 102 route. AuditSecurityTest kiểm token hợp lệ 200 trên auth/me và my-pets.
- AuthController và JwtTokenProvider bỏ cast principal không kiểm kiểu. Ownership đã instanceof; ReviewServiceImpl đã kiểm instanceof; hai cast STOMP lấy từ UserDetailsServiceImpl (kiểu trả về nội bộ xác định), không từ anonymous principal.
- ProtectedRoute đã có state.from; MyPetsPage đã tách loading/error/retry/empty. Interceptor bỏ qua login/register, tránh redirect loop tại login, lưu đường dẫn quay lại trong sessionStorage; LoginPage đọc và kiểm đường dẫn nội bộ; bỏ console error khi khôi phục phiên thất bại.
- BUG-014 commit `d20ceb1`; backend baseline PASS (201 tests, 3 SQL Server opt-in skipped).

## BUG-008

- Ownership đã sửa trước; BookingOwnershipTest 3/3 PASS ngay. Hai test QaQuickPetTest FAIL trước (201→400, staff 403→400). Tái sử dụng UI modal từ `27fb305`, native dialog từ `e609a39`, cấu hình cap từ `7fe4d3c`; không merge nhánh.
- POST my-pets dùng DTO cá nhân, CUSTOMER-only ở filter/controller/service, identity từ Ownership, bỏ qua trường đặc quyền, giá 0/UNAVAILABLE; cap 20 cấu hình được và khóa owner trước COUNT. Giữ tương thích form cũ birthDate/ghi chú/giống; tuổi có thể chưa biết. Loài backend chấp nhận danh mục DB hợp lệ; modal lọc tên Chó/Mèo theo dữ liệu seed, không hardcode ID. NEEDS DECISION: đổi catalog sang mã loài ổn định nếu đổi tên đa ngôn ngữ.
- Booking pets có loading/error/retry/empty; chỉ auto-select nếu 1 pet; modal lưu chọn pet mới, giữ nguyên dịch vụ/ngày/giờ. Slot đã có AbortController và tách lỗi/rỗng, không mock. Booking nằm trong ProtectedRoute.
- POST appointments dùng ownedPet; GET appointments/pet/{petId} dùng requirePet + lọc customer/assigned staff; PUT appointments/{id}/status không nhận petId, truy vấn scoped locked. Không có reschedule API. NEEDS DECISION: chưa thêm đặt hộ khách.
- Test H2 thật QaAppointmentScopeTest thêm: foreign/missing/store pet 404, thiếu petId 400, guest401, không ghi lịch khi thất bại; pet đúng chủ201, bỏ qua owner/customer giả. Test quick pet kiểm private storefront và cap.
- API/DTO tài liệu cập nhật. Frontend TypeScript/lint/build PASS.
- BUG-011 commit `f308893`.

## BUG-009

- ĐÃ SỬA phần còn thiếu; dữ liệu API thật và bỏ mock đã có. Test mới FAIL vì message 404 tiếng Anh; sửa tiếng Việt, full backend/frontend checks PASS.
- ID không phải số nguyên dương an toàn trả màn 404, không request; reset theo route ID và AbortController chặn response cũ. API qua petManagementService (service hiện có, không tạo bản trùng petService). 404 không có retry; lỗi mạng/5xx có retry. Chỉ dùng PublicPetDTO.
- Không có mockPets trong src. Public detail owned/SOLD vẫn404 theo BUG-005; RESERVED hiển thị đang đặt. NEEDS DECISION: mua pet chưa tích hợp, nút disabled aria-disabled và Sắp ra mắt; không xây order pet trong bug này.
- BUG-008 commit `c2110b6`; backend 206 tests, 3 skipped, không lỗi. Một test bổ sung đã phát hiện ownerId chưa ignored (400), sửa DTO rồi toàn bộ PASS.
