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
- Quét ngoài phạm vi: findAll còn ở danh mục, species, services, promotions, admin users; matching vẫn đánh giá toàn bộ pet available. Đợt G đã thêm periods theo tháng bằng GROUP BY year/month, dùng chung bộ lọc doanh thu.

## Đợt F
- #51: ProtectedRoute chờ restore phiên, state.from và role; các trang cá nhân không mount trước đăng nhập. Profile null không spinner vô hạn.
- #52: /orders lấy Page theo principal, catch-all 404, giữ BrowserRouter basename Vite; CI copy index.html thành 404.html. Chưa deploy nên chưa xác minh refresh trên host thật.
- #47: loyalty/membership dùng dữ liệu thật; backend là nguồn ngưỡng 1000/5000/10000, trả tier/nextTier/pointsToNextTier/thresholds. NEEDS DECISION về ngưỡng.
- #48: notifications nối API đầy đủ, polling 30s + cleanup theo phiên. NEEDS DECISION: WebSocket chưa có STOMP authentication, không kích hoạt hook chưa an toàn.
- #49: PUT users/me validated; thêm/sửa pets/my-pets gán owner từ principal, không cho khách đổi trạng thái y tế/giá/owner; OAuth/quên mật khẩu ghi Sắp ra mắt và vô hiệu hóa.
- #50: lịch sử y tế/tiêm phòng/my-pets phân biệt error, empty, loading; cleanup chống response cũ, retry, phân trang lịch sử.
- Tests: HTTP customer/admin và ownership notification + profile validation; frontend build PASS, lint lỗi baseline. Chưa có browser E2E.
- Phát hiện thêm: OrderServiceImpl và checkout trước đó là demo/stub; xử lý trong G để kiểm thử persistence có ý nghĩa.


## Đợt G và kiểm tra cuối (2026-10-18)

- Checkout/OrderService trước đó là stub: hiện lưu đơn + dòng hàng, lấy principal/giá từ server, giữ kho trong transaction, khóa đơn khi đổi trạng thái, hoàn kho khi hủy đúng một lần. Trang tracking legacy dùng danh sách đơn thật. TreatmentService cũng đã thay stub bằng persistence và validation ngày.
- Bổ sung concurrency H2 cho hai yêu cầu xuất cùng tồn cuối / cùng nhân viên-khung giờ; chỉ một yêu cầu thành công. Thêm test truy vấn sản phẩm với 25 hàng: active true/false/null, keyword ở hàng cuối, trang 20 tối đa 3 câu SQL (page/count/batch inventory).
- Thêm GROUP BY tháng và test dữ liệu nhiều kỳ; test mapping quiz đủ câu trả lời; test validation lịch quá khứ/ngoài giờ/nhân viên sai role/bị khóa; test treatment persistence.
- SQL schema idempotent, reset tách riêng có hai guard; migration 001 khắc phục UNIQUE nullable của SQL Server và CHECK tồn kho. Testcontainer chạy chính schema + migration, lặp lại để kiểm tra idempotence, rồi kiểm tra hai sản phẩm cùng pet_id NULL, trùng product và tồn âm. Chưa chạy testcontainer do không có Docker.
- Dependency: Vite 7.3.7, React Router 7.18.4, gh-pages 6.1.1; plugin-react 4.7 và Tailwind/PostCSS 4.3.3. Có nâng major, không dùng --force; cập nhật import CSS, PostCSS và utility thay đổi theo [hướng dẫn Tailwind](https://tailwindcss.com/docs/upgrade-guide), kiểm tra lint/tsc/build/dev. Đã sửa cấu hình ESLint flat và các biến không dùng. Không bật no-explicit-any cho toàn codebase legacy.
- Hai npm audit đều **0 vulnerabilities**. Advisory dev server phụ thuộc điều kiện expose server; advisory SSR không phải bằng chứng SPA đã bị khai thác.
- Browser localhost: /profile và /orders → /login khi anonymous; URL không tồn tại → 404; screenshot xác nhận CSS render. Google/Facebook/App Store/Google Play disabled rõ ràng; footer bỏ href="#". Chưa kiểm chứng đăng nhập và nghiệp vụ E2E với database thật hoặc host deploy.

## Bảng từng lỗi

Đường dẫn backend viết gọn từ `backend/src/main/java/com/petcarehub`; tên test nằm trong `backend/src/test/java/com/petcarehub` (hoặc `service/impl`). Với frontend chưa có hạ tầng automated test, dùng checklist bên dưới. ĐÃ SỬA chỉ xác nhận mã và kiểm thử đã nêu, không thay cho kiểm chứng production.

| Lỗi | Trạng thái | Nguyên nhân đã xác nhận → thay đổi / file chính | Kiểm chứng |
|---|---|---|---|
| #25 | ĐÃ SỬA | Quantity không bảo vệ/race → InventoryServiceImpl, StockUpdateRequest, InventoryRepository; khóa kho, adjustment riêng | InventoryServiceImplTest, ConcurrencyTest |
| #26 | ĐÃ SỬA | DTO thiếu validation/ngày; treatment stub → dto/auth, product, health; controllers; MedicalRecord/Vaccination/TreatmentServiceImpl | AuditValidationTest, AuditSecurityTest, TreatmentServiceImplTest |
| #27 | ĐÃ SỬA | STAFF không tồn tại → Pet/Service/TreatmentController, role thật và ADMIN | AuditSecurityTest chạy cả 5 RoleType |
| #28 | ĐÃ SỬA | Không kiểm tra giờ/active → AppointmentServiceImpl, BookingProperties | AppointmentServiceImplTest |
| #29 | ĐÃ SỬA | Chọn staff tùy ý → ServiceType.staffRole, eligible enabled + role | AppointmentServiceImplTest (sai role/disabled), ConcurrencyTest |
| #30 | ĐÃ SỬA | Giờ mẫu/không xét nguồn lực → overlap query, khóa staff, BookingPage | AppointmentServiceImplTest, ConcurrencyTest; thủ công M4 |
| #31 | ĐÃ SỬA | LocalTime quay vòng → LocalDateTime và duration 5–480 | AppointmentServiceImplTest timeout 0/-1/481/1440 |
| #32 | ĐÃ SỬA | Gán status tự do → AppointmentStatus transition, ownership, khóa lịch | AppointmentServiceImplTest toàn bộ ma trận; M5 |
| #33 | ĐÃ SỬA | Gán health trực tiếp → PetServiceImpl gọi PetHealthService | PetAuditTest |
| #34 | ĐÃ SỬA | Wildcard public đứng trước cá nhân → SecurityConfig/AuthController/PetController | AuditSecurityTest anonymous401/JWT200 |
| #35 | ĐÃ SỬA | Tên property lệch → JwtProperties, JwtTokenProvider, application*.yml | AuditSecurityTest exp-iat |
| #36 | ĐÃ SỬA | Catch-all lộ message/status sai → GlobalExceptionHandler, ErrorResponse correlationId | AuditValidationTest mappings, AuditSecurityTest field400 |
| #37 | ĐÃ SỬA | Không kiểm giao dịch/duyệt → ReviewServiceImpl/Repository/Controller | ReviewAuditTest target/mua/trùng/pending |
| #38 | ĐÃ SỬA | BeanUtils bỏ tên field khác → PetMatchingServiceImpl, PetMapper, principal controller | PetAuditTest 3 strategy + capture mọi câu trả lời |
| #39 | ĐÃ SỬA | Mapper thiếu age/price → PetDTO/PetMapper và requests, frontend Pet type | PetAuditTest tuổi/ẩn giá pet cá nhân |
| #40 | ĐÃ SỬA | Category bị bỏ qua, stock cập nhật im lặng → ProductServiceImpl/ProductUpdateRequest | ProductServiceImplTest category/404/stock bị từ chối |
| #41 | ĐÃ SỬA | false/null lẫn lộn, public lộ inactive → ProductServiceImpl/Controller, ProductListPage | ProductQueryTest, AuditSecurityTest ép active/detail404 |
| #42 | ĐÃ SỬA | Lọc trên trang đầu → SQL keyword/category/page, ProductListPage URL/debounce/abort | ProductQueryTest keyword hàng25; M6 |
| #43 | ĐÃ SỬA | Query kho từng hàng → batch findByProductIdIn + EntityGraph category | ProductQueryTest <=3 SQL cho 20 hàng |
| #44 | ĐÃ SỬA | findAll/unpaged ở lịch sử → repo Pageable thật, PaginationConfig, frontend history | Repository/context tests; M8/M11; danh sách ngoài phạm vi bên dưới |
| #45 | ĐÃ SỬA | Cộng mọi đơn → OrderRepository shared filter + SUM/COUNT/GROUP BY tháng, AdminServiceImpl | RevenueRepositoryTest paid/unpaid/refund/cancel/date/nhiều kỳ |
| #46 | ĐÃ SỬA | Pet và điểm quiz mẫu → PetList/Detail/MatchingPage gọi API; xóa mockPets không còn import | tsc/build, M12 |
| #47 | ĐÃ SỬA | Điểm/hạng mẫu/ngưỡng lệch → LoyaltyPage/MembershipPage/LoyaltyServiceImpl | LoyaltyPromotionTest; M8 |
| #48 | NEEDS DECISION | Thông báo mẫu/contract lệch → API list/read/read-all/count + polling30s, badge | AuditSecurityTest ownership; M8; chưa bật WebSocket thiếu auth |
| #49 | ĐÃ SỬA | Nút không handler → users/me, pets/my-pets CRUD, Profile/MyPetsPage, OAuth disabled | AuditSecurityTest profile; M11/M13 |
| #50 | ĐÃ SỬA | catch coi lỗi là empty → health/vaccination/my-pets error/retry/cleanup | tsc/build; M11 |
| #51 | ĐÃ SỬA | Trang riêng mount anonymous → ProtectedRoute/isLoading/state.from | Browser /profile,/orders → /login; M7 |
| #52 | ĐÃ SỬA | Thiếu /orders/404, fallback host → App/OrdersPage/OrderController/deploy.yml | Browser route404 và bảo vệ; M7 deploy chưa chạy |
| #53 | SỬA MỘT PHẦN | Thiếu coverage rủi ro → security, checkout, loyalty/promotion, concurrency, SQL query, container tests | Suite H2/unit chạy; SQL Server container SKIP vì không Docker |
| #54 | ĐÃ SỬA | Package advisory → hai package/lock, Vite/Tailwind config | npm audit cả hai=0; lint/tsc/build/dev PASS |
| #55 | ĐÃ SỬA | DROP khi init → schema IF NOT EXISTS, reset-demo guard, migration riêng | Review script; container test đã viết nhưng SKIP; không chạy SQL thật |
| #56 | ĐÃ SỬA | Regex toàn cục phá cú pháp → normalize-encoding.cjs exact literal AST, dry-run, git clean, rollback tsc | tsc/build; không chạy --write; M14 |
| #57 | ĐÃ SỬA | Null price về0, breed/owner sai → PetServiceImpl validation và giữ giá | PetAuditTest |
| #58 | ĐÃ SỬA | Seed hardcode/log token → seed_pets.js env, README hướng dẫn | Review source và thiếu env; không seed vào DB thật |

## Ma trận quyền sau sửa

Mọi endpoint dưới `/api/v1`. ✓ là được qua role check, vẫn phải thỏa validation/ownership. “mình” là tài nguyên của principal. Role thật: CUSTOMER, SALES_STAFF, WAREHOUSE_STAFF, VETERINARIAN, ADMIN; không còn role STAFF trong annotation phân quyền.

| Endpoint / hành động | Anonymous | CUSTOMER | SALES_STAFF | WAREHOUSE_STAFF | VETERINARIAN | ADMIN |
|---|---|---|---|---|---|---|
| GET auth/me, pets/my-pets | 401 | mình | mình | mình | mình | mình |
| GET products, products/{id} | active | active | active | active | active | active |
| GET products/manage | không | không | ✓ | ✓ | không | ✓ |
| Ghi products, pets chung, services | không | không | ✓ | không | không | ✓ |
| Ghi treatments, medical-records, vaccinations | không | không | không | không | ✓ | ✓ |
| Đọc medical/vaccination/treatment | không | pet mình | pet mình | pet mình | ✓ | ✓ |
| POST inventory add/reduce/adjust, GET low-stock/transactions | không | không | không | ✓ | không | ✓ |
| GET inventory/{productId} | không | ✓ | ✓ | ✓ | ✓ | ✓ |
| GET appointments tất cả | không | không | ✓ | không | ✓ | ✓ |
| Tạo appointment | không | pet mình | pet mình | pet mình | pet mình | pet mình |
| Đổi appointment | không | hủy lịch mình trước giờ | được giao hoặc hủy lịch mình | hủy lịch mình | được giao hoặc hủy lịch mình | ✓ |
| GET orders, POST orders | không | mình | mình | mình | mình | mình |
| Confirm/process/ship/deliver order | không | không | ✓ | không | không | ✓ |
| Cancel order | không | mình | mình | mình | mình | ✓ |
| PUT users/me; ghi pets/my-pets | không | mình | mình | mình | mình | mình |
| GET/PUT notifications | không | mình | mình | mình | mình | mình |
| GET loyalty/account, history; spend | không | mình | mình | mình | mình | ✓ |
| Add loyalty; approve review; /admin/** | không | không | không | không | không | ✓ |
| GET reviews | chỉ approved | approved + detail pending mình | tương tự | tương tự | tương tự | detail pending ✓ |

## NEEDS DECISION và giới hạn

1. Grooming → SALES_STAFF, thú y → VETERINARIAN vì schema không có lịch nghỉ/chuyên khoa. Đổi tại ServiceType.staffRole; giờ tại BookingProperties. Khách được hủy trước thời điểm bắt đầu; chưa áp dụng phí/khoảng cấm hủy.
2. Legacy age là năm tròn, suy ra birthDate nếu chưa có ngày sinh; có thể đổi sang bắt buộc birthDate. Giá chỉ hiện khi pet chưa có owner và AVAILABLE.
3. Review pending mặc định, ADMIN duyệt, một review/user/target. Muốn một review mỗi lần mua phải bổ sung liên kết giao dịch.
4. Doanh thu chỉ payment COMPLETED và không CANCELLED, theo createdAt; không có paidAt/partial-refund nên chưa có sổ doanh thu hoàn tiền từng phần. Đổi cần schema/nguồn thanh toán thật.
5. Loyalty chọn ngưỡng backend 1000/5000/10000; UI chỉ đọc response. Đổi một nguồn THRESHOLDS.
6. Polling 30 giây cho notifications. Không nối hook WebSocket chưa xác thực STOMP. Muốn realtime cần handshake/token và quyền subscription trước khi bật.
7. Checkout chỉ COD sản phẩm, server price; ghi nhận thanh toán khi staff giao thành công. Chưa có thanh toán trực tuyến. Không tự cộng điểm hay áp promotion vào checkout khi quy tắc chưa rõ.
8. Không thêm Flyway/Liquibase. Migration001 cần review/backup và chạy có chủ đích bởi người vận hành. Chưa xác nhận runtime SQL Server hoặc lock semantics SQL Server; H2 concurrency không thay thế kiểm thử đó.
9. Giữ package gốc phục vụ seed/deploy. Không push/deploy hay sửa data-sqlserver.sql.

## Contract thay đổi

- JWT property duy nhất `app.jwt.expiration-ms`; lỗi500 thêm correlationId, các field lỗi validation giữ format hiện có.
- Product update stockQuantity bị từ chối, dùng inventory API. Public ép active; manage nhận Boolean ba trạng thái. Search hỗ trợ keyword/categoryId/page/size/sort.
- Pet DTO thêm birthDate/age/price và mapping quan hệ; update price null giữ nguyên. Personal CRUD bỏ quyền khách đổi owner/giá/sức khỏe tùy ý.
- Notification list trả Page và field `read`; endpoint GET `/notifications`, `/unread-count`, PUT `/{id}/read`, `/read-all`; legacy GET `/{userId}` kiểm ownership.
- Orders list Page theo principal; tạo đơn dùng items productId/quantity + shippingAddress, không tin userId/price. Treatment request thêm frequency/startDate/endDate theo entity thực.
- Lịch sử medical/vaccination/appointment/loyalty nhận page/size nhưng giữ array response để tương thích; UI dùng next khi đầy trang. Default20, max100.
- Loyalty trả tier/nextTier/pointsToNextTier/thresholds. Revenue report thêm periods theo tháng.
- Schema init không DROP; migration001 đổi ràng buộc/index kho, không thay/xóa dữ liệu seed.

## Kiểm tra thủ công

Dùng tài khoản test và database riêng; không chạy trên dữ liệu thật.

1. **M1:** GET auth/me và pets/my-pets không Bearer →401; cùng request với token hợp lệ →200. Thử CUSTOMER DELETE pets/services/treatments →403; đối chiếu từng role với bảng.
2. **M2:** POST auth/register email sai/mật khẩu dưới8/khoảng trắng →400 và field errors. Product price âm; medical ngày tương lai; vaccination nextDueDate trước ngày tiêm; treatment endDate trước startDate →400.
3. **M3:** Với kho có2, nhập/xuất -1 hoặc0 bị từ chối, xuất3 bị từ chối, xuất1 còn1; journal chỉ có thao tác thành công. Adjust cần reason và quantity>=0.
4. **M4:** Chọn pet mình, thử đặt hôm qua/08:00/kết thúc sau17:00/dịch vụ tắt/staff sai role →bị từ chối. Đặt cùng slot hai phiên →chỉ một thành công. Hết chỗ hiện rỗng, tắt API hiện lỗi và thử lại, đổi ngày nhanh không ghi đè slot cũ.
5. **M5:** COMPLETED → SCHEDULED trả409; khách không được confirm; khách chỉ hủy lịch mình trong tương lai. Pet UNDER_TREATMENT → HEALTHY bị409; qua RECOVERING mới hợp lệ.
6. **M6:** Tạo hơn20 sản phẩm test, tìm từ khóa chỉ ở trang2 vẫn thấy; category/keyword reset page0, URL giữ bộ lọc; inactive không hiện public và detail404, hết hàng không thêm giỏ. Manage active true/false/bỏ param cho ba tập kết quả.
7. **M7:** Xóa phiên rồi mở /profile,/orders,/booking tương ứng →login; đăng nhập quay lại URL cũ. URL sai404; refresh /PetCare-Hub/orders trên bản deploy. Local browser mới chỉ xác nhận /profile,/orders và404.
8. **M8:** Đăng nhập A rồi B: điểm/hạng/thông báo đổi theo user; read/read-all cập nhật badge, logout dừng polling. A gọi notification/loyalty của B →403. Phân trang lịch sử/thông báo không lặp.
9. **M9:** Dataset paid/unpaid/refunded/cancelled ở nhiều tháng: chỉ completed-payment và không cancelled góp doanh thu, kiểm from/to bao gồm cả ngày cuối và periods.
10. **M10:** `npm audit` từ ppet rồi frontend phải0; chạy lint/tsc/build, mở dev localhost.
11. **M11:** MyPets thêm/sửa thật, kiểm tra owner là mình. History loading riêng; chặn network phải báo lỗi + retry; API200[] mới báo rỗng; đổi pet nhanh không hiện dữ liệu pet trước.
12. **M12:** Pets list/detail lấy API; id không tồn tại không hiện pet mẫu. Quiz gửi cả chăm sóc/trẻ em/môi trường/mức hoạt động và hiển thị điểm/giải thích server.
13. **M13:** Profile lưu/refresh giữ giá trị, lỗi validation hiển thị. OAuth/quên mật khẩu hiện Sắp ra mắt; không giả đăng nhập. Checkout đổi price/userId phía client vẫn dùng giá/identity server, order được lưu, hủy hoàn kho một lần.
14. **M14:** `node normalize-encoding.cjs --file=src/services/productService.ts --dry-run` không đổi file. Chỉ --write trên file chỉ định khi git sạch; không chạy ghi toàn repo. Seed thiếu env dừng và không log token; không chạy seed production.

## Mojibake, quét phạm vi và kiểm thử

- Quét src không còn `Idễ`, `disabledễ`, `ễ:` trước lần sửa này: không có vị trí mới để khôi phục; các dấu `?` đã đúng, không sửa trùng. Xóa mockPets.ts vì không có import.
- `findAll(pageable/spec,pageable)` ở products/pets/appointments là có giới hạn. Ngoài phạm vi còn findAll ở admin users, species, category, promotions, service catalogue; matching đánh giá toàn bộ AVAILABLE với unpaged trước top10 (đã ghi nhận, chưa tối ưu thuật toán).
- Test cũ ProductServiceImplTest đổi mock sang specification do truy vấn chuyển sang SQL filter; test Medical/Vaccination constructor cập nhật dependency ownership. Test storefront mới dùng Page.empty(PageRequest) vì Page.empty() unpaged không serialize offset; đây là sửa fixture, không đổi API để chiều test.
- Chưa có frontend test runner từ baseline; không thêm framework mới, dùng build/lint và checklist. Không giả báo PASS cho các bước E2E còn chưa chạy.


## Kết quả cuối so với baseline

| Kiểm tra | Baseline | Cuối đợt G |
|---|---|---|
| Backend | Sandbox treo, chưa xác nhận full baseline; sau A 9 test cũ PASS | 43 test: 42 PASS, 1 SQL Server SKIP, 0 failure/error |
| TypeScript | PASS | PASS |
| Frontend build | PASS Vite5 | PASS Vite7.3.7 |
| ESLint | FAIL thiếu flat config | PASS |
| npm audit frontend | 14 (9 high,5 moderate) | 0 |
| npm audit package gốc | 9 (6 high,3 moderate) | 0 |
| Dev/browser | Chưa kiểm | Local dev chạy; anonymous redirects và404 đúng |
| SQL Server/deploy E2E | Chưa chạy | Chưa chạy: không Docker, không deploy, không tác động DB thật |

Log backend: `backend/target/audit-G.log`; XML ở target/surefire-reports (artifact local, không commit). Cảnh báo sẵn có về AuthenticationProvider, open-in-view và JVM agent vẫn còn; không tuyên bố log hoàn toàn sạch. Frontend lint/build không có warning mới. Số test bao gồm parameterized roles. Seed thiếu env đã kiểm tra exit1; không gọi API.
