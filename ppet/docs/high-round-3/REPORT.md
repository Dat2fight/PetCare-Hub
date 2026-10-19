# Báo cáo High lượt 3/3 — PetCare-Hub

Ngày kiểm tra: 2026-10-19. Nhánh `fix/critical-jwt-secret`. Phạm vi: #13, #15, #14, #16, #18, #17, #19, #23, #24.

## 1. Hiện trạng và tránh xung đột

Bước 0 đã chạy `git status`, `git branch --show-current`, `git log --oneline -n 80`: working tree sạch, HEAD `0c8560f`, đủ commit Medium, High1, Critical1, High2. Không tạo nhánh, push, force, rebase/reset hoặc thao tác stash. Stash cũ giữ nguyên. Đọc các báo cáo audit-25-58, high-round-1, high-round-2, critical-jwt-secret và diff từ `b4286c6` ở file liên quan trước khi sửa.

| Nhóm file | Bản sửa trước được giữ | Điều chỉnh lượt này |
|---|---|---|
| security/Ownership, GlobalExceptionHandler | Một helper principal/ownership, ADMIN override user; ADMIN/VET override clinical; 400/401/403/404/409 và correlationId | CartController dùng helper có sẵn; order detail và treatment dùng lại |
| OrderServiceImpl, state/order, OrderRepository | Medium đã repository, transaction, giá server, inventory lock, khóa đơn, chuyển trạng thái, hoàn kho; #45 doanh thu payment COMPLETED và status khác CANCELLED | Bổ sung GET detail, thông tin giao hàng/thanh toán, COD validation; không viết lại state/revenue |
| InventoryServiceImpl, ProductServiceImpl | #21 khóa Inventory và ledger cùng transaction; #22 tạo product+inventory cùng transaction; #43 batch-load | Không sửa |
| CartServiceImpl/CartRepository | #21 khóa user ổn định trước cart/items, đọc snapshot tồn trước user lock; unique cart/product migration002 | Update/remove/clear dùng cùng transaction/lock order; item phải thuộc cart của user |
| DTO/controller validation | #26 validation và ma trận #27; role chỉ CUSTOMER/SALES_STAFF/WAREHOUSE_STAFF/VETERINARIAN/ADMIN | Promotion dùng validation thật; mật khẩu thêm giới hạn byte, không bỏ min8 |
| TreatmentServiceImpl | CRUD, mapper medicine→medication, frequency/startDate, ngày, ownership và VET/ADMIN write đã có | Check owner hồ sơ cũ trước khi đổi medicalRecordId; thêm hồi quy DB/HTTP |
| PetMatchingServiceImpl | Mapper quiz #38, strategies và userId từ principal | Tính/sort/rank trước, lưu quiz+results chung transaction |
| Checkout/Tracking/Cart/Orders/App | #52 /orders, protected routes, catch-all, basename /PetCare-Hub | API thật, route detail, giữ legacy tracking |
| client/useWebSocket/CorsConfig/deploy.yml | #7 CONNECT Bearer, user queue, shared origin property; #52 npm ci và copy404 | URL build-time; trim CORS giống WebSocket; giữ proxy dev8081 |
| pom/SecurityConfig/JWT | Boot 3.3.5, BCrypt, JWT_SECRET bắt buộc; không default dev | Chỉ nâng bản vá và chính sách password; JwtTokenProvider, application*.yml và application-test.yml không đổi |

Chi tiết tiến độ và mốc kiểm thử từng nhóm: [PROGRESS.md](PROGRESS.md).

## 2. Kết quả từng lỗi

Đường dẫn Java bên dưới tính từ `backend/src/main/java/com/petcarehub/`; các test ở `backend/src/test/java/com/petcarehub/`.

| ID | Trước khi sửa | Kết quả | Commit | File chính | Test thêm |
|---|---|---|---|---|---|
| #13 | Core đã sửa từ Medium, thiếu API detail và contract thanh toán rõ | ✅ | `8885ef0` | OrderController, OrderService/Impl, dto/order | OrderFlowTest 5 case: giá server, ownership, quyền staff, state/revenue, hoàn kho một lần, rollback thiếu kho, 404/CARD400 |
| #15 | update/remove/clear TODO | ✅ | `4798d0f` | CartServiceImpl, CartRepository, CartController | CartMutationTest 4: persistence, foreign404, quantity/stock400, update/add đồng thời |
| #14 | Checkout đã POST nhưng chưa gửi paymentMethod/đi detail; tracking alias | ✅ | `d3c25ce` | CheckoutPage, OrderTrackingPage, OrdersPage, App | OrderFlowTest thêm1: POST→GET cùng ID và shipping/COD; địa chỉ trống400 |
| #16 | Cart mock và giảm giá cố định gây âm tổng | ✅ | `b49e4e7` | CartPage | CartMutationTest thêm1: giá thật và quantity sau mutation |
| #18 | BeanUtils không map tên DTO/entity khác nhau | ✅ | `b5bdf54` | Promotion DTO/ServiceImpl/Controller | PromotionMappingTest 3; fixture hợp lệ của CatalogAuthorizationTest |
| #17 | CRUD phần lớn đã đúng; update chưa check hồ sơ cũ trước reassignment | ✅ | `fac3662` | TreatmentServiceImpl | TreatmentFlowTest 3: CRUD DB, foreign read/reassign403, CUSTOMER write403, ngày400/missing404 |
| #19 | Save result thiếu rank trước sort | ✅ | `80d4358` | PetMatchingServiceImpl | MatchingTransactionTest 3: rank/top10/tie, rollback sau flush, principal chống spoof URL |
| #23 | URL HTTP/WS cố định path, Pages không có proxy | ✅ | `bf4288a` | client.ts, useWebSocket.ts, deploy.yml, CorsConfig, .env.example, README | DeploymentOriginsTest 3: HTTP/WS origin allow/deny; kiểm tra bundle URL deploy mẫu |
| #24 | Boot 3.3.5/Security 6.3.4; password chỉ giới hạn ký tự | ✅ | `dfedead` | pom, SecurityConfig, RegisterRequest/LoginRequest, validation, RegisterPage | PasswordBoundaryTest 3: ASCII/Unicode72byte, suffix, DTO và HTTP |

## 3. Phần còn lại và giới hạn

Đã hoàn tất phần triển khai của cả 9 ID, không còn mục ⚠️/❌ trong phạm vi lượt này. Không chạy SQL Server nghiệp vụ, không deploy, chưa kiểm thử trình duyệt end-to-end với DB thật. Ba test SQL Server opt-in tiếp tục skip, không tính là pass. Không đổi schema ở lượt này; migration của lượt 2 vẫn là điều kiện vận hành. Frontend chưa có test framework nên không cài mới; dùng build/typecheck và checklist tay.

## 4. Contract API và frontend

- `GET /api/v1/orders/{id}` mới: 200 cho owner/ADMIN/SALES_STAFF, 403 người khác, 404 không tồn tại. GET danh sách `/orders` vẫn Page đơn của chính principal, không thành endpoint danh sách toàn hệ thống.
- DTO order thêm `orderNumber`, `shippingAddress`, `paymentMethod`, `paymentStatus`, `createdAt`; item thêm `productName`. Giữ `id`, `userId`, `status`, `totalPrice`, `items`, `price` cũ.
- POST `/orders` nhận `paymentMethod=COD`; bỏ trường vẫn COD cho caller cũ; phương thức khác400. `userId`/giá client không quyết định chủ đơn hay giá. Checkout gửi chỉ productId/quantity, thông tin người nhận/điện thoại/địa chỉ gộp trong shippingAddress, paymentMethod. Chỉ navigate sau response thành công; lỗi ở lại trang, báo rõ và có link danh sách đơn.
- Route frontend mới `/orders/:id`; `/orders`, `/orders/tracking` và `?orderId=` vẫn dùng được. Tracking tải detail thật, có loading/error/retry/cancel.
- Cart endpoints giữ nguyên: GET `/cart`, PUT `/cart/items/{id}` trả CartDTO, DELETE item/DELETE `/cart` trả204. Item ngoài giỏ404, quantity≤0/null/vượt snapshot tồn400. CartPage lấy giá/quantity thật, đồng bộ mutation rồi đọc lại; nếu response không chắc chắn buộc tải lại trước checkout.
- Promotion **đổi tên contract**: dùng `discountType` (PERCENTAGE/FIXED_AMOUNT), `discountValue`, `minOrderAmount`, `maxUses`, `active`; response có `currentUses`. Bỏ tên sai cũ `discountPercentage`, `maxDiscountAmount`, `usageLimit`, `isActive`, `timesUsed`. Không có caller frontend sử dụng các tên cũ qua tìm kiếm repo. Caller ngoài repo phải cập nhật. POST/PUT validation400; currentUses do server quản lý, PUT không reset.
- Treatment vẫn `medicine`, `dosage`, `frequency`, `startDate`, `endDate`, `medicalRecordId`, `description`; không đổi API. Matching vẫn trả top10 MatchResultDTO; rank lưu DB, không thêm field response.
- `VITE_API_BASE_URL` và `VITE_WS_URL` mới, công khai lúc build; mặc định `/api/v1`, `/ws`. SockJS cần HTTPS URL, không phải wss://. Backend tiếp tục `APP_CORS_ALLOWED_ORIGINS` dùng chung HTTP/WS. Không truyền JWT_SECRET vào frontend.
- Register/staff creation giữ min8, max72 ký tự và thêm max72 byte UTF-8; login cũng max72 byte, không áp min8 lên tài khoản legacy. Vi phạm400 trước xác thực. RegisterPage kiểm tra tương ứng bằng TextEncoder.

## 5. Phiên bản và tương thích #24

Chọn Boot **3.3.13** thay **3.3.5**, Security BOM **6.3.10** thay **6.3.4**, giữ Java target17, JJWT0.12.6 và springdoc2.6.0. Phiên bản thực tế kiểm chứng bằng `java.class.path` trong Surefire XML. Lệnh `dependency:tree` ban đầu không resolve được plugin prefix, nên không dùng nó làm bằng chứng. Maven vẫn tải và chạy thành công dependencies bản vá.

Nguồn chính thức: [CVE-2025-22228](https://spring.io/security/cve-2025-22228/) ghi nhánh6.3 được vá từ6.3.8; [BOM Boot 3.3.13](https://docs.spring.io/spring-boot/3.3/appendix/dependency-versions/coordinates.html) dùng Security 6.3.10. [Thông báo Boot 3.3.13](https://spring.io/blog/2025/06/19/spring-boot-3-3-13-available-now/) xác nhận kết thúc OSS support dòng3.3. Nâng sang dòng còn hỗ trợ cần kế hoạch riêng; không khẳng định bản này xử lý mọi CVE hiện tại.

Test phát hiện BCrypt6.3.10 vẫn cho phép verify password dài để tương thích legacy, dù encode mới từ chối >72byte; xem [mã nguồn BCrypt6.3.10](https://github.com/spring-projects/spring-security/blob/6.3.10/crypto/src/main/java/org/springframework/security/crypto/bcrypt/BCrypt.java) và [CVE-2025-22234 về timing](https://spring.io/security/cve-2025-22234/). Không hạ kỳ vọng test. Bean PasswordEncoder giữ BCrypt mặc định, chạy `super.matches` trước rồi áp giới hạn72byte; không đổi định dạng hash/strength, không cắt mật khẩu, không bỏ qua dummy-hash computation. Cùng giới hạn được áp tại validation register/login. Không sửa logic JWT, matcher, role hoặc thêm secret fallback.

## 6. Ma trận quyền cuối ở các nhóm được giao

“Mình” là chính principal, áp dụng cả tài khoản nhân viên khi họ là chủ dữ liệu. “—” là không được. Mọi yêu cầu riêng tư cần tài khoản enabled và xác thực hợp lệ.

| Nhóm thao tác | Khách | CUSTOMER | SALES_STAFF | WAREHOUSE_STAFF | VETERINARIAN | ADMIN |
|---|---|---|---|---|---|---|
| Public register | CUSTOMER | CUSTOMER | CUSTOMER | CUSTOMER | CUSTOMER | CUSTOMER |
| Tạo nhân viên / revenue admin | — | — | — | — | — | Có |
| Public catalog/product/category/promotion/pets bán | Có | Có | Có | Có | Có | Có |
| Product manage | — | — | Có | Có | — | Có |
| Ghi product/category/promotion | — | — | Có | — | — | Có |
| Inventory GET product | — | Có | Có | Có | Có | Có |
| Inventory nhập/xuất/adjust/history/low-stock | — | — | — | Có | — | Có |
| Cart đọc/add/update/remove/clear | — | Mình | Mình | Mình | Mình | Mình |
| Tạo đơn / list đơn | — | Mình | Mình | Mình | Mình | Mình |
| Đọc detail đơn | — | Mình | Mọi đơn | Mình | Mình | Mọi đơn |
| Confirm/process/ship/deliver đơn | — | — | Có | — | — | Có |
| Hủy đơn hợp lệ | — | Mình | Mình | Mình | Mình | Mọi đơn |
| Loyalty read/spend/history | — | Mình | Mình | Mình | Mình | Mọi user |
| Loyalty add | — | — | — | — | — | Có |
| Appointment list mọi lịch | — | — | Có | — | Có | Có |
| Appointment theo user | — | Mình | Mình | Mình | Mình | Mọi user |
| Appointment theo pet | — | Chủ pet + lịch mình | Như CUSTOMER | Như CUSTOMER | Mọi pet | Mọi pet |
| Đặt lịch | — | Pet mình | Pet mình | Pet mình | Pet mình | Pet mình |
| Hủy lịch tư cách khách | — | Mình, trước giờ bắt đầu | Như CUSTOMER | Như CUSTOMER | Như CUSTOMER | Theo state |
| Đổi trạng thái lịch nghiệp vụ | — | — | Được phân công | — | Được phân công | Có |
| Pet riêng tư / đọc bệnh án, vaccine, treatment | — | Pet mình | Pet mình | Pet mình | Mọi pet | Mọi pet |
| Ghi/xóa bệnh án, vaccine, treatment | — | — | — | — | Có | Có |
| Quiz matching submit | — | Mình | Mình | Mình | Mình | Mình |
| Notification HTTP và STOMP queue | — | Mình | Mình | Mình | Mình | Mình |
| STOMP SEND từ client | — | — | — | — | — | — |

## 7. Giả định nghiệp vụ

- Checkout chỉ bán product và COD theo luồng đang có; không giả lập thanh toán online. Giá lấy từ Product DB lúc tạo đơn; giá dòng đơn được lưu snapshot. Đơn mới PENDING/paymentPENDING, trừ kho ngay trong transaction.
- State giữ nguyên PENDING→CONFIRMED→PROCESSING→SHIPPING→DELIVERED; hủy chỉ PENDING/CONFIRMED/PROCESSING, hoàn kho một lần; SHIPPING/DELIVERED/CANCELLED không hủy lại. COD giao thành công → paymentCOMPLETED; doanh thu vẫn theo #45.
- Cart là ý định mua, không giữ tồn kho. Checkout kiểm tra lại dưới khóa Inventory. Giữ hành vi cũ: tạo đơn không tự clear cart; người dùng có thể xóa bằng nút mới. Không tự xóa item người dùng vừa thêm ở tab khác. Không có idempotency key liên request; UI chỉ chặn double-submit trong tab và hướng dẫn kiểm tra đơn sau lỗi mạng.
- Không có promotion được áp dụng bởi API cart/order hiện tại; bỏ giảm giá giả, không trình bày discount chưa được server tính. CRUD/validate promotion không đồng nghĩa tích hợp mã giảm giá vào checkout.
- Promotion percentage trong (0,100], fixed amount>0, maxUses nếu có phải>0, minOrderAmount≥0, ngày cuối≥ngày đầu. Thiếu active mặc định true; không tự sửa currentUses. maxUses thấp hơn currentUses làm mã hết lượt theo validator hiện hữu.
- Treatment cho phép ADMIN/VET đổi medical record sau kiểm tra cả hồ sơ cũ/mới. Không cho CUSTOMER ghi điều trị, dù sở hữu pet.
- Matching lưu toàn bộ kết quả được chấm với rank1..n, trả top10 như cũ; hòa điểm dùng pet ID tăng dần. Giữ nguyên strategies và mapper.

## 8. Kiểm thử so với baseline

| Mốc | Tổng test | Pass | Fail/error | Skip | Frontend build |
|---|---:|---:|---:|---:|---|
| Baseline | 146 | 143 | 0 | 3 | PASS |
| #13 | 151 | 148 | 0 | 3 | PASS |
| #15 | 155 | 152 | 0 | 3 | PASS |
| #14 | 156 | 153 | 0 | 3 | PASS |
| #16 | 157 | 154 | 0 | 3 | PASS |
| #18 | 160 | 157 | 0 | 3 | PASS |
| #17 | 163 | 160 | 0 | 3 | PASS |
| #19 recheck, gồm test #23 | 169 | 166 | 0 | 3 | PASS |
| #23 | 169 | 166 | 0 | 3 | PASS default + deploy URL mẫu |
| #24 cuối | 172 | 169 | 0 | 3 | PASS |

Thêm 26 test so với baseline; tất cả test mới pass. Tổng cuối 172 gồm 169 pass và 3 skip.

Lệnh backend: `cmd.exe /c ".\mvnw.cmd -q -Dnet.bytebuddy.experimental=true test"` trong `ppet/backend`; host Java25 cần flag Mockito/ByteBuddy, target17. Logs ignored `backend/high-3-*.log`, Surefire XML/TXT ở target. Nên vận hành bằng JDK17 theo project; chưa chạy lại bằng JDK17 trong phiên này. Frontend `npm run build` gồm `tsc -b` + Vite; mỗi lần build đều restore `tsconfig.app.tsbuildinfo`. Không chạy lint trong lượt3 vì không thuộc baseline yêu cầu hiện tại; không khẳng định lint pass.

Lỗi trong quá trình thực hiện: import default apiClient ở CartPage đã sửa; test rollback matching điều chỉnh đúng exception Spring translation; test password đã bắt hành vi verify legacy và dẫn tới guard encoder. Không xóa test, không có baseline pass bị bỏ qua để báo thành công. Ba SQL Server skips có từ baseline.

## 9. Bản sửa trước còn hở / xung đột

- Matching query vẫn lọc AVAILABLE mà chưa lọc owner=null, và PetMapper.toDTO có ownerId/healthStatus. Khoảng hở privacy đã được báo ở High1, giữ nguyên theo quy tắc không sửa lại #10/Medium trong lượt này. Cần xử lý riêng trước khi coi toàn bộ bề mặt công khai an toàn.
- InventoryServiceImpl.getCurrentUser còn fallback user ID1 khi không resolve được actor; đã được báo ở High2. Không sửa ngoài phạm vi; tests luồng mới cung cấp principal hợp lệ.
- SQL migration001 chưa có TRY/CATCH rollback tường minh; migration002/003 đã có. Không refactor schema/reset hoặc chạy SQL thật.
- CatalogAuthorizationTest cũ gửi `{}` cho promotion để kiểm tra quyền. Khi bật validation phải dùng payload hợp lệ, giữ nguyên toàn bộ kỳ vọng ma trận quyền; không nới quyền cho test pass.
- Không có xung đột cần hoàn tác bản sửa cũ. JWT/provider/profile, inventory locks, product creation, appointment/time/staff/state, schema và seed giữ nguyên.

## 10. Kiểm tra tay và vận hành

1. Trên DB test đã có migration001→002→003: đăng nhập A/B, thêm giỏ, đổi số lượng/xóa/clear, refresh; B sửa item của A phải404. Hai tab cùng add/update không tạo dòng trùng; vượt tồn bị từ chối.
2. Giỏ→checkout COD→detail theo ID thực→refresh `/orders/{id}`; địa chỉ trống/stock thay đổi/server lỗi phải giữ trang và báo lỗi. B xem/hủy đơn A phải403. Kiểm tra danh sách đơn trước thử lại khi mất mạng sau POST.
3. Staff/ADMIN confirm/process/ship/deliver; CUSTOMER bị403. Hủy trước ship hoàn kho đúng một lần; hủy lại hoặc sau ship bị409. Hai checkout cùng tồn giới hạn không làm âm kho. COD delivered cập nhật doanh thu theo #45.
4. ADMIN/SALES tạo/sửa promotion PERCENTAGE và FIXED_AMOUNT, active=false/maxUses/ngày, tải lại xác nhận trường giữ đúng; giá trị0/âm/%>100/maxUses0 phải400. CUSTOMER ghi403.
5. VET/ADMIN tạo/sửa/xóa treatment, reload thấy medication/frequency/startDate; owner đọc được, người khác403, CUSTOMER ghi403. Đổi medicalRecordId không cho chiếm treatment khác.
6. Quiz: kết quả giảm dần điểm, DB rank không null, top10 khớp rank; lỗi persistence không để quiz/results dở. Kiểm tra riêng privacy gap mục9 trước mở rộng sử dụng.
7. Cấp VITE_API_BASE_URL/VITE_WS_URL qua build hoặc GitHub repository Variables; backend cấp APP_CORS_ALLOWED_ORIGINS chính xác. Build/deploy rồi login, cart/order, refresh route, nhận notification hai tài khoản riêng. Origins khác bị từ chối. Không chạy URL ví dụ `.invalid` như endpoint thật.
8. Password8..72byte đăng ký/đăng nhập được; >72byte ASCII/Unicode phải400, password đúng72byte thêm suffix không xác thực; tài khoản disabled vẫn bị từ chối. JWT_SECRET thiếu/không an toàn vẫn fail-fast theo test Critical1; không cấp secret backend vào VITE_*.

## 11. Việc còn lại sau ba lượt High

Hoàn thiện kiểm chứng SQL Server opt-in, migration trên bản sao dữ liệu và smoke test trình duyệt/deploy theo checklist; chưa coi H2 là bằng chứng về filtered UNIQUE/NULL của SQL Server. Theo dõi privacy matching, audit actor fallback và kế hoạch nâng Boot sang dòng còn hỗ trợ bằng công việc riêng. Không push hoặc deploy từ lượt sửa này. Khóa JWT cũ vẫn phải coi đã lộ vĩnh viễn, không tái sử dụng; các nghĩa vụ vận hành Critical1 giữ nguyên.

Kiểm tra Git cuối: provider/config JWT và test config không có diff so với `0c8560f`; stash cũ nguyên hash; buildinfo đã restore. Các commit sửa lỗi độc lập được liệt kê ở mục2; báo cáo là commit tài liệu riêng.
