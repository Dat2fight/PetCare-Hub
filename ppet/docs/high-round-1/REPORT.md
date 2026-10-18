# High lượt 1 — nhật ký và báo cáo

## 1. Bước 0 / bản đồ hiện trạng

- Git ban đầu sạch, chỉ có local `main`, remote main/gh-pages. `main` = `86d1dd1`, merge PR #2 từ `fix/audit-25-58`; tạo `fix/high-bugs` từ chính HEAD này, không từ main cũ. Không push/rebase/reset/stash.
- Mốc trước Medium: `b4286c6`; diff tới `86d1dd1`: 143 file, 3900 dòng thêm / 3470 dòng xóa. Đã đọc REPORT Medium, diff và mã hiện hành; không dùng số dòng review cũ.
- Baseline Maven (Java 25 host, compile release 17; `-Dnet.bytebuddy.experimental=true` cho Mockito): 43 test, 42 pass, 0 fail/error, 1 skip (`SqlServerConstraintTest`, không bật container). Chỉ H2/unit, không SQL Server thật.
- Baseline lint: pass. Build lần đầu fail vì node_modules cũ (Vite 5, thiếu @tailwindcss/postcss) không khớp lockfile Medium (Vite 7/Tailwind 4). Đồng bộ bằng npm ci, không đổi package/lock. tsbuildinfo thay đổi do build sẽ không đưa vào commit.

| File dự kiến tác động (backend dưới src/main/java/com/petcarehub) | Medium đã thay đổi / phần phải giữ |
|---|---|
| security/UserDetailsImpl, JwtAuthenticationFilter | Medium không sửa hai file; giữ constructor cũ cho các test/caller, JwtProperties `app.jwt.expiration-ms` #35 và typed principal #34 |
| factory/UserFactory | #27 đã ép CUSTOMER bất kể request.role; giữ nguyên public createUser |
| controller/AdminController, dto/auth/RegisterRequest | ADMIN class annotation; revenue from/to; validation đăng ký #26. Endpoint nhân viên mới phải dùng lại RegisterRequest/UserDTO/RoleType |
| config/DataSeeder, resources/application*.yml, data-sqlserver.sql, README | Seeder dev còn admin hardcode; default dev. #35 expiry typed. #58 seed_pets.js đọc env, không log token; #55 schema idempotent, reset guarded, migration riêng: không sửa các script đó |
| controller/ProductController | #41 public ép active, /manage ADMIN/SALES_STAFF/WAREHOUSE_STAFF; write ADMIN/SALES_STAFF, validated #26. Giữ toàn bộ |
| controller/ProductCategoryController, PromotionController | Chưa có role write; theo role commerce ADMIN/SALES_STAFF của #27 |
| security/Ownership | Medium đã tạo userId, requireUser (ADMIN override), hasRole, requirePet (ADMIN/VETERINARIAN override). Tái sử dụng một class, không tạo helper cạnh tranh |
| controller/LoyaltyController, service/impl/LoyaltyServiceImpl | requireUser/account lookup history, add ADMIN; points >0 đã có; Pageable history; DTO tier/nextTier/pointsToNextTier/thresholds #47. Giữ endpoints legacy và DTO |
| controller/AppointmentController, service/impl/AppointmentServiceImpl | all-list staff role; user/pet ownership ở controller; book typed principal và pet owner; BookingProperties, future/opening/duration, eligible staff, pessimistic locks, overlap SQL, canTransitionTo 409, customer cancel-before-start #28–32; Pageable #44 |
| frontend/pages/services/BookingPage.tsx | Medium đã dùng getMyPets, bỏ fallback mockPets/slots, abort slots, disable submit khi thiếu pet/slot. Không viết lại |
| controller/MedicalRecordController, VaccinationController; service/impl tương ứng | Write VETERINARIAN/ADMIN, validated dates, requirePet ở cả detail/history, Pageable giữ List response #44 |
| controller/NotificationController | #48 list Page theo principal, legacy list check user, read ownership/read-all/unread-count. Giữ nguyên contract và chặt hơn ADMIN override |
| controller/PetController, service/impl/PetServiceImpl, mapper/PetMapper, dto/pet/PetDTO | #49 my-pets create/update ép owner, giữ health/availability; #33 health state service; #57 breed/species/owner validation, null price giữ nguyên; #39 age/birthDate/price; public hiện vẫn dùng DTO cá nhân |
| config/WebSocketConfig, service/WebSocketNotificationService, frontend/hooks/useWebSocket.ts | Chưa được Medium sửa auth; topic userId public, origins *. #48 cố ý giữ polling, chưa bật hook. Giữ polling khi đổi hook an toàn |

GlobalExceptionHandler #36 đã có 400/401/403/404/409 và correlationId: tái sử dụng, không thêm handler. RoleType chỉ CUSTOMER/SALES_STAFF/WAREHOUSE_STAFF/VETERINARIAN/ADMIN.

## Nhật ký kiểm chứng

Các commit, kết quả cuối và ma trận quyền được bổ sung sau từng nhóm.
