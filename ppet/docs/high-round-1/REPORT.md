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

Các file phụ thuộc đã đối chiếu trước khi sửa:

| File | Trạng thái Medium và cách giữ tương thích |
|---|---|
| `config/SecurityConfig.java` | Giữ matcher authenticated cho `auth/me` và `pets/my-pets`; mở rộng matcher riêng tư trước wildcard public khi thêm GET chi tiết my-pets |
| `repository/AppointmentRepository.java` | Giữ các truy vấn overlap và pessimistic lock #28–32; chỉ thêm truy vấn pet + customer cho lịch sử |
| `security/JwtTokenProvider.java` | Giữ `JwtProperties` #35, khóa ký, định dạng subject; chỉ thêm đọc expiration cho STOMP |
| `frontend/src/pages/customer/LoyaltyPage.tsx` | Giữ API thật, loading/error/empty/retry, phân trang và tier thresholds #47; dùng endpoint tự xác định principal. MembershipPage vẫn là alias |
| `frontend/src/services/petManagementService.ts` | Giữ DTO cá nhân và API my-pets #49, phân trang bệnh án/vaccine #44; thêm type công khai và đổi riêng GET chi tiết cá nhân |
| `frontend/src/pages/pets/PetListPage.tsx`, `PetDetailPage.tsx` | Giữ age/birthDate, species/breed và giá null #39–40; dùng PublicPetDTO và bỏ hiển thị healthStatus trên trang công khai |
| `frontend/src/pages/customer/NotificationsPage.tsx` | Giữ list Page, read/read-all, retry, phân trang và polling #48; chỉ thêm hook nhận sự kiện để tải lại |
| `service/impl/AppointmentServiceImplTest.java` | Giữ nguyên assertion Medium; chỉ truyền dependency Ownership mới vào constructor test |

Các class mới (`AdminBootstrap`, `StaffAccountService`, `PublicPetDTO`, `StompAuthorizationInterceptor`) không thay thế validator/exception/role/DTO hiện có; PublicPetDTO là hợp đồng công khai thu hẹp có chủ đích. Mỗi lỗi có commit riêng; file test mới tương ứng nằm trong `backend/src/test/java/com/petcarehub/`.

## 2. Kết quả từng lỗi

Đường dẫn Java dưới `backend/src/main/java/com/petcarehub/`; tên test là tên class, không phải tổng số assertion.

| ID | Trước khi sửa | Kết quả | Commit | File chính | Test đã thêm |
|---|---|---|---|---|---|
| #11 | Còn lỗi: principal luôn enabled; token chưa kiểm tra khóa tài khoản | ✅ Đã sửa | `46b7cb0` | `security/UserDetailsImpl`, `JwtAuthenticationFilter` | `EnabledAccountTest` (3): entity false/null, token cũ sau khóa, login đúng mật khẩu nhưng disabled |
| #3 | Public signup đã ép CUSTOMER; thiếu API tạo nhân viên riêng | ✅ Đã sửa phần còn thiếu, giữ signup Medium | `4c37f7a` | `controller/AdminController`, `service/StaffAccountService` | `StaffRegistrationTest` (13): mọi role giả mạo signup vẫn CUSTOMER; non-admin 403; ADMIN tạo 3 loại nhân viên, không trả hash |
| #2 | Còn admin hardcode; dev mặc định và SQL demo có tài khoản admin | ✅ Đã sửa | `e44094e` | `config/AdminBootstrap`, `DataSeeder`, `application.yml`, `data-sqlserver.sql`, README | `AdminBootstrapTest` (5): cấu hình bắt buộc, mã hóa password, không reset admin, không nâng CUSTOMER, profile dev |
| #4 | Product đã có quyền Medium; category/promotion còn thiếu role write | ✅ Đã sửa | `1951a99` | `controller/ProductCategoryController`, `PromotionController` | `CatalogAuthorizationTest` (6): 5 role × 3 nhóm × các thao tác write và GET công khai |
| #5 | Legacy loyalty đã kiểm tra ownership; frontend còn truyền ID | ✅ Đã bổ sung endpoint tự xác định principal, giữ legacy | `94088d6` | `security/Ownership`, `controller/LoyaltyController`, `LoyaltyPage.tsx` | `LoyaltyOwnershipTest` (4): query ID giả không đổi chủ thể, CUSTOMER chéo bị 403, history/ADMIN, anonymous 401 |
| #6 | Đã chặn points ≤ 0; thiếu giới hạn trên/tràn số | ✅ Đã sửa | `1f3304f` | `service/impl/LoyaltyServiceImpl` | `LoyaltyLimitsTest` (6): null/âm/0/quá mức, HTTP 400, biên hợp lệ, tràn integer |
| #8 | Đã có owner cancel và role controller; staff chỉ dựa ID, lịch theo pet có thể lộ chủ cũ | ✅ Đã sửa | `30a352f` | `service/impl/AppointmentServiceImpl`, `repository/AppointmentRepository` | `AppointmentAuthorizationTest` (5): CUSTOMER/foreign deny, nhân viên bị hạ role, nhân viên được phân công, query pet+customer |
| #9 | Medium đã kiểm tra pet owner, không fallback; BookingPage đã dùng getMyPets | ✅ Giữ hành vi đúng, gom kiểm tra vào helper chung | `cbc7cad` | `security/Ownership`, `service/impl/AppointmentServiceImpl` | `BookingOwnershipTest` (3): pet người khác + customerId giả, pet thiếu 404, pet vô chủ 403; không tự thay pet |
| #10 | Bệnh án/vaccine/notification đã bảo vệ; public pets còn DTO riêng tư và pet có chủ | ✅ Đã sửa phần còn thiếu | `a971c2b` | `controller/PetController`, `dto/pet/PublicPetDTO`, `service/impl/PetServiceImpl`, `config/SecurityConfig`, frontend pet pages/service | `PrivatePetDataTest` (4): public projection/filter, private owner/clinical, foreign bệnh án/vaccine, notification chéo kể cả ADMIN |
| #7 | Socket chưa auth, topic theo userId và origins wildcard; hook chưa dùng | ✅ Đã sửa | `8917238` | `security/StompAuthorizationInterceptor`, `JwtTokenProvider`, `config/WebSocketConfig`, `service/WebSocketNotificationService`, hook/trang thông báo, cấu hình/README | `StompAuthorizationTest` (17), `WebSocketDeliveryTest` (1) |

## 3. Phần còn lại và giới hạn kiểm chứng

Không để lại hạng mục triển khai ⚠️/❌ trong phạm vi endpoint của 10 ID được giao. Các phần đã đúng từ Medium được giữ nguyên và thêm kiểm thử hồi quy, không sửa lại để tạo diff. Điểm còn hở ở luồng matching ngoài phạm vi được ghi riêng tại mục 9.

- Chưa chạy trình duyệt end-to-end với SQL Server thật; checklist ở mục 10 cần chạy trên môi trường của nhóm.
- `SqlServerConstraintTest` tiếp tục skip vì không bật test Docker opt-in. Không tính skip là pass.
- SQL seed chỉ được đọc/kiểm tra tĩnh; không thực thi lên SQL Server, không thay schema/migration #55 hay seed_pets.js #58.
- Critical #1 và High #12–24 không được xử lý trong lượt này. Các giới hạn đó không bị che bằng seed dữ liệu giả.

## 4. Tiện ích ownership dùng lại cho lượt sau

Class **`com.petcarehub.security.Ownership`**, file `backend/src/main/java/com/petcarehub/security/Ownership.java`, là bean có sẵn từ Medium, được mở rộng thay vì tạo helper mới. Inject qua constructor.

| Hàm | Cách dùng / quyền |
|---|---|
| `userId()` | Lấy ID từ SecurityContext với principal UserDetailsImpl authenticated và enabled; không hợp lệ thì 401 |
| `authorizedUserId(requestedId)` | Trả ID hiện tại nếu trùng; ADMIN được chỉ định ID khác; còn lại 403. Dùng giá trị trả về để gọi service |
| `requireUser(id)` | Kiểm tra tương đương, không trả ID; ADMIN override được giữ từ Medium |
| `ownedPet(petId)` | Trả entity pet nếu đúng chủ hiện tại; không có pet 404, không có chủ/sai chủ 403. **Không** override ADMIN/VET; dùng cho đặt lịch cá nhân |
| `requirePet(petId)` | Giữ kiểm tra chủ hoặc ADMIN/VETERINARIAN; dùng đọc bệnh án/vaccine/chi tiết riêng tư |
| `hasRole(role)` | Kiểm tra authority `ROLE_...` hiện tại; không tự cấp role |

Không dùng `requirePet` cho nghiệp vụ chỉ chủ sở hữu được phép; chọn `ownedPet` khi không muốn override chuyên môn. Notification giữ kiểm tra chủ nghiêm ngặt của Medium, không đổi sang helper có ADMIN override.

## 5. Contract API và frontend

Prefix `/api/v1` giữ nguyên; không đổi tên property JWT, trường đăng nhập, token/accessToken hay status mapping handler.

| Contract | Thay đổi và nơi cập nhật |
|---|---|
| `POST /admin/staff` | Mới, ADMIN, RegisterRequest hiện có; role SALES_STAFF/WAREHOUSE_STAFF/VETERINARIAN; 201 UserDTO, không password/hash. Endpoint tối thiểu, chưa có UI tạo nhân viên riêng |
| `GET /loyalty/account`, `GET /loyalty/transactions`, `POST /loyalty/account/spend` | Mới, tự xác định user từ principal. Giữ nguyên các route legacy có ID với ownership check. LoyaltyPage chuyển sang self endpoints; MembershipPage dùng lại cùng trang |
| Loyalty add/spend | points phải 1..1.000.000 mỗi thao tác; vượt hoặc tràn balance/total trả 400; DTO tier/thresholds và dạng lịch sử List không đổi |
| `GET /pets`, `GET /pets/{id}` | Chỉ pet owner=null **và** AVAILABLE; PublicPetDTO bỏ ownerId/healthStatus. ID riêng tư/unavailable trả 404. PetListPage/PetDetailPage đổi type, bỏ hiển thị health công khai |
| `GET /pets/my-pets/{id}` | Mới, trả PetDTO cá nhân cho chủ hoặc ADMIN/VET. petManagementService.getPetById chuyển route; HealthHistoryPage/VaccinationPage dùng service nên được cập nhật theo. GET danh sách my-pets cho BookingPage giữ nguyên |
| Appointment | Không đổi route/DTO; customerId request không được dùng để chọn chủ thể; foreign pet 403, missing 404. Giữ 409 chuyển trạng thái và slot/time validations |
| STOMP | CONNECT header `Authorization: Bearer <JWT>`; SUBSCRIBE `/user/queue/notifications`; topic cũ, raw queue, queue người khác và mọi SEND client bị từ chối. Hook đọc accessToken/token và reconnect bằng token hiện tại; cleanup khi đổi user/unmount |
| Thông báo frontend | NotificationsPage dùng hook để refresh API, giữ polling 30s/read/read-all/pagination của #48. HTTP notification endpoints/DTO giữ nguyên |
| Cấu hình | Không mặc định dev; bootstrap admin opt-in qua biến môi trường. `APP_CORS_ALLOWED_ORIGINS` là danh sách origin chính xác cho HTTP và socket; mặc định localhost:5173, wildcard/empty socket fail startup. README đã cập nhật |

STOMP dùng principal là ID người dùng dạng chuỗi để `convertAndSendToUser` khớp đúng recipient. JWT chỉ đọc ở CONNECT; không lưu token trong registry, xóa Authorization khỏi native headers. Kiểm tra lại enabled/expiry trên inbound và trước khi gửi MESSAGE ra socket, gồm tài khoản bị khóa sau kết nối.

## 6. Ma trận quyền sau sửa

“Của mình” áp dụng cho mọi tài khoản authenticated nếu chúng đóng vai trò chủ dữ liệu. Role override được ghi riêng; không có role mới.

| Nhóm endpoint/thao tác | Khách | CUSTOMER | SALES_STAFF | WAREHOUSE_STAFF | VETERINARIAN | ADMIN |
|---|---|---|---|---|---|---|
| Public register | Tạo CUSTOMER | Tạo CUSTOMER | Tạo CUSTOMER | Tạo CUSTOMER | Tạo CUSTOMER | Tạo CUSTOMER |
| Tạo nhân viên | — | — | — | — | — | Có |
| GET product/category/promotion công khai | Có | Có | Có | Có | Có | Có |
| Product `/manage` | — | — | Có | Có | — | Có |
| POST/PUT/DELETE product/category/promotion | — | — | Có | — | — | Có |
| Loyalty đọc/spend/history | — | Của mình | Của mình | Của mình | Của mình | Mọi tài khoản |
| Loyalty add | — | — | — | — | — | Có |
| Appointment list toàn bộ | — | — | Có | — | Có | Có |
| Appointment theo user | — | Của mình | Của mình | Của mình | Của mình | Mọi user |
| Appointment theo pet | — | Chủ pet + lịch của mình | Chủ pet + lịch của mình | Chủ pet + lịch của mình | Mọi pet | Mọi pet |
| Đặt lịch | — | Pet của mình | Pet của mình | Pet của mình | Pet của mình | Pet của mình |
| Hủy lịch với tư cách khách | — | Lịch của mình, trước giờ bắt đầu | Như CUSTOMER | Như CUSTOMER | Như CUSTOMER | Có, theo bảng chuyển trạng thái |
| Đổi trạng thái nghiệp vụ | — | — | Được phân công | — | Được phân công | Mọi lịch |
| Public pets (DTO bán) | Có | Có | Có | Có | Có | Có |
| Chi tiết pet riêng tư, đọc bệnh án/vaccine | — | Pet của mình | Pet của mình | Pet của mình | Mọi pet | Mọi pet |
| Ghi/xóa bệnh án/vaccine | — | — | — | — | Có | Có |
| HTTP notification đọc/đánh dấu | — | Của mình | Của mình | Của mình | Của mình | Của mình |
| STOMP CONNECT/SUBSCRIBE | — | JWT hợp lệ, queue mình | Như CUSTOMER | Như CUSTOMER | Như CUSTOMER | Như CUSTOMER |
| STOMP SEND từ client | — | — | — | — | — | — |

Enabled=false bị từ chối login/JWT/STOMP dù có role nào. Role của nhân viên đổi trạng thái lịch phải còn hiệu lực hiện tại, không chỉ trùng staffId.

## 7. Giả định nghiệp vụ

- Giới hạn loyalty là **1.000.000 điểm/thao tác**; balance và total không được tràn Integer. Comment ở validatePoints ghi đây là mức mặc định nghiệp vụ.
- API tạo nhân viên chỉ tạo ba role nhân viên; không tạo ADMIN. Nâng quyền admin vẫn thuộc nghiệp vụ quản trị hiện có, ngoài endpoint mới.
- Pet có thể đổi chủ: người chủ mới không được thấy lịch của khách cũ. Đặt lịch cá nhân luôn cần đúng chủ, kể cả tài khoản ADMIN/VET.
- Quyền đọc chuyên môn ADMIN/VET và quyền nhân viên được phân công thay đổi trạng thái giữ theo Medium; không mở quyền cho WAREHOUSE_STAFF.
- Socket hiện chỉ phát thông báo từ server; không có nhu cầu client SEND nên chặn toàn bộ. Origin cấu hình cần chính xác theo frontend được triển khai.
- Không tự sửa/tái kích hoạt admin cũ khi bootstrap; môi trường vận hành phải chủ động bật bootstrap và cấp mật khẩu riêng. Demo chỉ là opt-in dev.

## 8. Kiểm thử so với baseline

| Mốc | Maven | Frontend build | Lint |
|---|---|---|---|
| Baseline Medium | 43 tổng: 42 pass, 0 fail/error, 1 skip | Lần đầu fail do dependency local lệch lockfile; sau npm ci: pass | Pass |
| Sau nhóm A | 70 tổng: 69 pass, 0 fail/error, 1 skip | Pass | Pass |
| Sau nhóm B | 110 tổng: **109 pass, 0 fail/error, 1 skip** (thêm 67 test so với baseline) | Pass | Pass |

- Lệnh Maven: `cmd.exe /c ".\mvnw.cmd -q -Dnet.bytebuddy.experimental=true test"` trong backend; test profile H2, không dev seed/SQL Server thật. Host Java 25 cần flag ByteBuddy; không đổi Java target 17 hay thư viện Medium. Java/Maven trong sandbox từng không hoàn tất; chạy lại ngoài sandbox thành công, log vào target.
- Lệnh frontend: `npm.cmd run build`, `npm.cmd run lint`; build chạy `tsc -b` và Vite 7. Dependency được khôi phục đúng lockfile bằng npm ci; không thay package/lock.
- Đã chạy riêng `cmd.exe /c "npx tsc --noEmit"` và `cmd.exe /c ".\mvnw.cmd -q compile"`: đều exit 0. Báo cáo Surefire cuối gồm 27 test class; log local tại `backend/target/high-group-b-final.log`, không đưa log/build output vào Git.
- Một lượt test mới #4 ban đầu dùng payload category chứa field không thuộc DTO nên 5 case fail; đã sửa fixture test, giữ nguyên DTO/validation Medium, toàn bộ nhóm A pass sau đó. Không phải lỗi baseline hay hồi quy production.
- Test tích hợp SockJS mới ban đầu timeout do client test dùng ByteArrayMessageConverter chỉ nhận octet-stream trong khi server phát JSON; đổi sang SimpleMessageConverter và giữ mọi frame để kiểm tra chéo. Cả 17 unit case STOMP và kết nối thật hai người dùng đã pass sau sửa fixture, không thay production để che lỗi test.
- Các test Medium đã pass vẫn pass; chỉ cập nhật constructor test Appointment để inject Ownership, không bỏ assertion/test. Không cài framework frontend mới.
- Build output/tsbuildinfo không đưa vào commit. SQL Server opt-in skip có chủ đích; cảnh báo JVM native access/agent trên Java 25 không phải lỗi test.

## 9. Xung đột và điểm Medium cần lưu ý

- #27 đã chốt public signup CUSTOMER, product write ADMIN/SALES_STAFF; giữ nguyên, không thêm role “manager”. #35 vẫn dùng `app.jwt.expiration-ms`; #36 giữ duy nhất GlobalExceptionHandler.
- #28–32, #34 đã sửa nhiều phần của #8/#9. Chỉ thêm kiểm tra và query hạn chế hơn; giữ lock, lịch làm việc, slot, phân công, 409 state machine. BookingPage đã tải thú cưng thật nên không chỉnh lại.
- #47/48 giữ DTO tier/pagination và HTTP notification ownership. ADMIN vẫn không đọc thông báo người khác. Hook #48 trước đây chưa được dùng; nay thêm realtime nhưng giữ polling.
- Bỏ default dev (#2) làm lộ phụ thuộc `CorsConfig` vào property chỉ có trong dev/test. #7 bổ sung default origin local ở cấu hình gốc và cùng env override trong dev để ứng dụng có cấu hình hợp lệ khi không bật dev.
- Môi trường node_modules cũ không phản ánh lockfile Medium; npm ci giải quyết, không sửa dependency của nhóm.
- Chưa đánh giá lại toàn bộ 62 nhóm. Một điểm UI hiện hữu: BookingPage khi tải my-pets lỗi dùng toast, chưa có khu vực retry/empty riêng như mô tả rộng trong báo cáo Medium; chỉ ghi nhận, không sửa ngoài yêu cầu lượt này.
- **Cần review riêng Medium #38:** `PetMatchingServiceImpl.matchPets` (POST `/pet-matching/submit`, yêu cầu đăng nhập) vẫn chọn pet chỉ theo AVAILABLE và trả PetDTO có ownerId/healthStatus. Nếu dữ liệu có pet vừa có chủ vừa AVAILABLE, endpoint matching có thể lộ dữ liệu đó. Đây là luồng matching ngoài các endpoint public `/pets` và clinical được giao ở #10; theo yêu cầu không sửa lại Medium, chỉ ghi nhận để nhóm xử lý riêng. Kết quả #10 không đồng nghĩa đã audit mọi nơi sử dụng PetDTO trong toàn hệ thống.

## 10. Checklist kiểm tra tay

1. Đăng nhập tài khoản active; khóa tài khoản bằng quản trị rồi thử lại login, REST với token cũ và socket đang kết nối: phải bị từ chối/không nhận dữ liệu mới.
2. Public register gửi role ADMIN/VET vẫn tạo CUSTOMER. CUSTOMER gọi admin/staff 403; ADMIN tạo từng loại nhân viên 201, response không có password/hash.
3. Trên DB test riêng: bật bootstrap với cấu hình môi trường; chạy lại không reset admin. Không bật dev thì không seed demo. Không dùng SQL seed trên DB nghiệp vụ.
4. CUSTOMER không ghi product/category/promotion; SALES_STAFF và ADMIN được ghi. WAREHOUSE_STAFF vẫn đọc product manage nhưng không có write thương mại.
5. Mở Loyalty/Membership; xác nhận số điểm và phân trang thật. Đổi userId/accountId trên URL legacy sang người khác → 403; points 0/âm/quá mức → 400; ADMIN thao tác tài khoản khác đúng quyền.
6. BookingPage chỉ liệt kê pet hiện tại. Sửa petId sang pet người khác → 403, ID không tồn tại → 404, customerId giả không có tác dụng. Giờ/slot/nhân viên/chuyển trạng thái sai vẫn bị từ chối; chủ lịch chỉ hủy trước giờ bắt đầu.
7. CUSTOMER không liệt kê mọi lịch, không đổi trạng thái nghiệp vụ hay xem lịch người khác. Nhân viên được phân công + role hợp lệ mới cập nhật; hạ role thành CUSTOMER phải mất quyền nghiệp vụ.
8. URL public pet có chủ/unavailable → 404; danh sách chỉ pet bán, không có ownerId/healthStatus. Trang thú cưng cá nhân, lịch sử khám và vaccine vẫn dùng được; tài khoản khác 403; ADMIN/VET đọc được theo quyền chuyên môn.
9. Hai trình duyệt đăng nhập hai user: notification HTTP và socket chỉ đúng chủ; đọc-all của A không đổi B. CONNECT thiếu JWT, SUBSCRIBE topic cũ/user khác/raw queue, SEND giả notification đều bị từ chối.
10. Đổi user/đăng xuất khi ở trang thông báo; xác nhận cleanup socket và giữ polling khi mất kết nối. Kiểm tra origin đúng được nhận, origin khác không được handshake; cấu hình reverse proxy `/ws` khi triển khai.
