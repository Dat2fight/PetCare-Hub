# Audit 25–58 — nhật ký thực hiện

Nhánh: fix/audit-25-58. Không push; không chạy schema/data lên SQL Server.

## Baseline
- TypeScript và frontend build: PASS (Vite 5.4.21).
- Lint: FAIL có sẵn, ESLint 9 thiếu eslint.config.*.
- Backend baseline trong sandbox bị treo sau khởi tạo context; chưa xác nhận baseline đầy đủ. Sau sửa A, chạy ngoài sandbox: 9 test cũ PASS.
- npm audit: frontend 14 (9 high, 5 moderate); package gốc 9 (6 high, 3 moderate).

## Đợt A
- #34: matcher cá nhân trước wildcard; principal typed và 401.
- #27: STAFF → SALES_STAFF; treatment thêm ADMIN. Đăng ký công khai chỉ CUSTOMER để tránh tự cấp quyền staff.
- #35: app.jwt.expiration-ms typed, Positive, toàn bộ profile; cùng thời điểm iat/exp.
- #36: correlationId + log stack trace, thông báo 500 chung; mapping 400/401/403/404/409.
- #26: validation đăng ký/sản phẩm/y tế, kiểm tra ngày tiêm. Update sản phẩm giữ semantics partial: null giữ nguyên.
- #58: seed đọc ADMIN_EMAIL/ADMIN_PASSWORD/API_BASE_URL; không in token. ADMIN_EMAIL hiện được gửi dưới field username vì backend chỉ đăng nhập bằng username; đặt biến này bằng tên đăng nhập hiện có.
- Tests: AuditSecurityTest, AuditValidationTest. TypeScript/build PASS; lint vẫn lỗi baseline.
- NEEDS DECISION: TreatmentServiceImpl là stub, chưa có persistence; DTO cũ thiếu frequency/startDate/endDate của entity. Không tuyên bố hoàn thiện luồng điều trị.
- NEEDS DECISION: cho giá sản phẩm bằng 0; public signup chỉ CUSTOMER. Role nhân viên do quản trị cấp.

## Ma trận các endpoint sửa ở A
| Endpoint | CUSTOMER | SALES_STAFF | WAREHOUSE_STAFF | VETERINARIAN | ADMIN |
|---|---|---|---|---|---|
| GET auth/me, pets/my-pets | có | có | có | có | có |
| POST/PUT/DELETE pets, services | không | có | không | không | có |
| POST/PUT/DELETE treatments | không | không | không | có | có |

Seed: đặt ba biến môi trường rồi chạy `node seed_pets.js` từ ppet; không đưa giá trị mật khẩu vào Git hoặc log.

## Đợt B
- #25: số lượng dương ở DTO/service; khóa pessimistic hàng kho; overflow bị chặn; adjustment riêng với lý do, dùng enum/schema ADJUSTMENT có sẵn.
- #40: cập nhật category có kiểm tra tồn tại; stockQuantity gửi vào update bị từ chối rõ (giữ field legacy với @Null).
- #41/#42/#43: storefront ép active và 404 detail inactive; /products/manage có role và lọc Boolean ba trạng thái; tìm kiếm SQL, phân trang URL, debounce, abort; một truy vấn kho mỗi trang và EntityGraph category.
- #56: script chỉ thay exact string literal qua TypeScript AST, một file chỉ định, dry-run mặc định, Git sạch trước write, chạy tsc và rollback khi lỗi. Không chạy --write. Quét Idễ/disabledễ/ễ: không còn trong src từ trước lượt này; không sửa trùng.
- Tests: InventoryServiceImplTest, ProductServiceImplTest (test cũ chuyển sang specification do cách truy vấn thay đổi). Frontend chưa có hạ tầng test: kiểm tra tìm từ khóa ở trang 2+, đổi danh mục reset trang, thử inactive và hết hàng.
- Kiểm tra: backend PASS; tsc/build PASS; lint vẫn lỗi cấu hình baseline.

## Đợt C
- #28–32: kiểm tra dịch vụ active, thời gian tương lai và toàn bộ duration trong 09:00–17:00 (BookingProperties); chuyên môn theo ServiceType; khóa hàng nhân viên trong transaction trước truy vấn overlap SQL; không fallback tùy ý.
- Availability dùng thời gian thực và xung đột của nhân viên; duration 0/âm/1440/vượt giờ không lặp vô hạn. State machine enum đầy đủ, khách chỉ hủy lịch của mình trước giờ bắt đầu.
- BookingPage bỏ slot/pet mẫu, tải pet của tài khoản, tách loading/error/empty và abort request slots.
- #44 phần pet: truy vấn findByPetId có giới hạn 100, sắp xếp theo ngày. Cần hoàn thiện API phân trang bên ngoài ở E.
- Tests: AppointmentServiceImplTest (duration, availability, toàn bộ bảng transition). Backend PASS, frontend build PASS sau sửa null pet, lint lỗi baseline.
- NEEDS DECISION: GROOMING → SALES_STAFF; VETERINARY → VETERINARIAN. Không có lịch nghỉ/chuyên môn riêng trong schema. Khách được hủy trước giờ bắt đầu (chưa có khoảng phạt hủy).

## Đợt D
- #33/#57: update sức khỏe qua PetHealthService; enum sai trả 400; breed/species kiểm tra; owner không tồn tại bị từ chối; giá null giữ nguyên.
- #39: thêm birthDate và price; age tính từ dateOfBirth. Legacy age quy đổi năm chỉ nếu chưa có ngày sinh. Giá chỉ trả cho pet chưa có chủ đang AVAILABLE.
- #38: ánh xạ quiz tường minh; availableCareTime đơn vị giờ/ngày; principal quyết định user; mapper Pet dùng chung đầy đủ quan hệ.
- #46: ba trang pet list/detail/quiz dùng API thật, không fallback pet đầu tiên hoặc điểm 98%; có loading/error/empty. Các thông tin mẫu không có nguồn API đã bỏ khỏi trang.
- Tests: PetAuditTest (state machine, breed/owner, giữ giá, mapper/tuổi/quyền riêng tư, cả ba matching strategy). Backend/TypeScript/build PASS; lint lỗi baseline.
- NEEDS DECISION: age là năm tròn, ngày sinh suy ra chỉ là ước lượng; có thể thay bằng từ chối age và bắt buộc birthDate. Chưa xác minh E2E bằng trình duyệt.

## Đợt E
- #37: target tồn tại, giao dịch DELIVERED hoặc lịch COMPLETED của principal; khóa user chống gửi trùng, mỗi target một review; mặc định pending; public list chỉ approved; detail pending chỉ chủ/admin; thêm endpoint duyệt ADMIN.
- #45: hai báo cáo dùng chung SUM/COUNT tại SQL; payment COMPLETED, loại CANCELLED, lọc from/to (mặc định tháng hiện tại). Không có dữ liệu partial refund/paidAt nên dùng createdAt; NEEDS DECISION.
- #44/#48 backend: thông báo trả Page, ownership, read/read-all/unread-count; pageable toàn hệ thống mặc định 20, max 100. Lịch hẹn/hồ sơ/loyalty giữ response list nhưng nhận page/size thật để tương thích.
- Tests: ReviewAuditTest và RevenueRepositoryTest (H2, các trạng thái thanh toán/hủy/khoảng thời gian); toàn bộ backend PASS. tsc/build PASS; lint lỗi baseline.
- Quét ngoài phạm vi: findAll còn ở danh mục, species, services, promotions, admin users; matching vẫn đánh giá toàn bộ pet available. Chưa có GROUP BY theo kỳ vì DTO báo cáo hiện chỉ tổng; cần bổ sung nếu muốn breakdown theo ngày/tháng.

## Đợt F
- #51: ProtectedRoute chờ restore phiên, state.from và role; các trang cá nhân không mount trước đăng nhập. Profile null không spinner vô hạn.
- #52: /orders lấy Page theo principal, catch-all 404, giữ BrowserRouter basename Vite; CI copy index.html thành 404.html. Chưa deploy nên chưa xác minh refresh trên host thật.
- #47: loyalty/membership dùng dữ liệu thật; backend là nguồn ngưỡng 1000/5000/10000, trả tier/nextTier/pointsToNextTier/thresholds. NEEDS DECISION về ngưỡng.
- #48: notifications nối API đầy đủ, polling 30s + cleanup theo phiên. NEEDS DECISION: WebSocket chưa có STOMP authentication, không kích hoạt hook chưa an toàn.
- #49: PUT users/me validated; thêm/sửa pets/my-pets gán owner từ principal, không cho khách đổi trạng thái y tế/giá/owner; OAuth/quên mật khẩu ghi Sắp ra mắt và vô hiệu hóa.
- #50: lịch sử y tế/tiêm phòng/my-pets phân biệt error, empty, loading; cleanup chống response cũ, retry, phân trang lịch sử.
- Tests: HTTP customer/admin và ownership notification + profile validation; frontend build PASS, lint lỗi baseline. Chưa có browser E2E.
- Phát hiện thêm: OrderServiceImpl và checkout trước đó là demo/stub; xử lý trong G để kiểm thử persistence có ý nghĩa.
