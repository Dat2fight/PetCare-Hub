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
