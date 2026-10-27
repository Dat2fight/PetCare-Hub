# Sửa lỗi QA Critical 001–007

## Bước 0 — nhánh và phạm vi

- Workspace thực tế: `D:/visual studio/PetCare-Hub/ppet` (đường dẫn trong yêu cầu là `D:/PetCare-Hub/ppet`).
- Trước khi bắt đầu: `main cfd890d`, `git status -s` chỉ có `?? ppet/docs/qa/` (báo cáo QA trước đó). Không có thay đổi Đợt G chưa commit; bốn file được bảo vệ sẽ so với base `44027f8` sau khi chuyển nhánh.
- Đã chạy `git branch -a`, `git log --oneline --graph --all -30`. `fix/audit-25-58` ở `44027f8`; `feature/pet-purchase-quick-add` ở `27fb305` có bốn commit tính năng riêng.
- `git merge-base --is-ancestor cfd890d HEAD`: exit 0 trên main ban đầu; exit 1 sau khi tạo `fix/qa-critical-001-007` từ `fix/audit-25-58`.
- `git diff --stat <merge-base HEAD main>..main`: trước chuyển nhánh rỗng; sau chuyển nhánh: xóa `ppet/frontend/write_components.py` (204 dòng), `write_home.py` (419 dòng), `write_productlist.py` (159 dòng). `git log HEAD..main` chỉ có `cfd890d`.
- **NEEDS DECISION — hợp nhất:** chưa merge/rebase main hoặc feature. Ba script trên là phần main khác base; source backend/frontend tính năng trên feature cũng cần review khi tích hợp, đặc biệt `OrderServiceImpl`, `PetServiceImpl`, DTO pet/order và checkout. Phương án đã chọn: sửa trên base được chỉ định; hợp nhất bằng PR sau, chạy lại toàn bộ test trên kết quả hợp nhất.
- Không chạy ứng dụng với profile dev, không ghi DB dev hoặc SQL reset. Chỉ hai SELECT đọc ID pet để đối soát; test tự động dùng profile `test`, H2. SQL Server Testcontainers hiện có chỉ chạy khi bật biến opt-in riêng.
- Đã đọc SecurityConfig, RoleType, toàn bộ controller, Ownership, UserDetailsImpl, JWT/filter/properties, GlobalExceptionHandler, DataSeeder/AdminBootstrap, application profiles và các test audit/security. Method security đã bật; JWT xác minh chữ ký rồi tải quyền hiện tại từ DB.

## Nhật ký xác minh từng lỗi

### BUG-001

- Base đã ép CUSTOMER trong UserFactory; public DTO vẫn chứa enum `role`, dùng chung với tạo nhân viên. Endpoint đổi role có thể hạ quyền admin cuối cùng.
- Test mới `QaRegistrationSecurityTest`: 5 ca đỏ trước sửa (payload đặc quyền thừa bị 400; admin bị hạ quyền). Sau sửa, public DTO bỏ role và bỏ qua field không được khai báo; tạo nhân viên dùng StaffCreateRequest riêng. ADMIN role change được kiểm cả controller/service; không hạ quyền admin qua endpoint chung.
- Giữ các test `StaffRegistrationTest` về mọi RoleType và quyền tạo nhân viên. Login/token test kiểm DB role CUSTOMER và truy cập admin 403.
- Quét mass assignment: ProfileUpdateRequest chỉ tên/email/phone; UserController gán từng field. Product/service/promotion active/price chỉ qua endpoint nhân viên; createMine pet ghi đè owner/price/health/availability. Không có endpoint người dùng tự sửa enabled/tier/points qua DTO profile.
- **NEEDS DECISION:** muốn gỡ một admin phải có luồng quản trị riêng được tuần tự hóa và bảo vệ admin cuối cùng. Hiện chặn mọi lần hạ quyền một admin qua endpoint generic; tạo/cấp quyền ADMIN từ admin vẫn theo API sẵn có.

BUG-001: full Maven PASS, 177 test hiện hành / 0 lỗi / 3 skip SQL Server; commit `4cced7d`. Số 193 ban đầu đếm cả 16 kết quả XML cũ của nhánh feature; đã loại báo cáo cũ khi đối soát và chạy clean test cuối nhiệm vụ.

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

### BUG-004

- Base đã có persistence Order/OrderItem, giá từ DB, transaction/khóa tồn kho, COD/PENDING, ProtectedRoute state.from, đơn thật/list/detail. Không còn DH123456 hoặc setTimeout giả trong các trang order/checkout. Không viết lại các phần đã sửa.
- Khoảng trống còn lại: chưa dọn cart cùng transaction; mọi authenticated role có thể tạo đơn; thiếu kiểm ID response trước chuyển trang; tồn kho thiếu trả 400; field tổng tiền lạ gây 400 thay vì bỏ qua.
- Red `QaCheckoutAtomicTest`: 5 ca thất bại/lỗi theo hợp đồng mới. Green full Maven PASS: giá giả/userId giả không ảnh hưởng đơn, PENDING trung thực; trừ đúng số lượng cart đã đặt, giữ sản phẩm khác/phần còn lại; item thứ hai lỗi rollback tồn/đơn/cart; hai đơn tranh một đơn vị hàng chỉ một commit. Thiếu hàng 409; hàng ngừng bán 404; invalid body 400; guest 401; staff tạo đơn 403; owner xem đơn, người khác 404 (BUG-003).
- Tạo đơn bảo vệ ở filter/controller/service; khóa customer sau inventory giữ cùng thứ tự với cart/stock. Request shippingAddress hiện là trường tổng hợp người nhận/SĐT/địa chỉ, bắt buộc không rỗng và <=2000 ký tự. Phí/discount/tax giữ 0 theo logic base; chưa thêm promotion/loyalty checkout.
- Frontend chỉ điều hướng khi id là số nguyên dương hợp lệ; API lỗi giữ form và thông báo tiếng Việt. tsc/lint/build PASS. Chưa có framework test UI; bước mô phỏng response lỗi nằm trong hướng dẫn thủ công, không tuyên bố đã chạy E2E browser.
- **NEEDS DECISION — nhánh mua pet:** base được chỉ định chưa chứa bốn commit feature. Không tự merge hoặc chép lại implementation mua pet. Request petId (đơn pet hoặc trộn product+pet) bị từ chối 400 rõ ràng; nhánh feature cần tích hợp riêng và kiểm lại validation XOR, khóa pet, order DTO, stock/cart cleanup. Product checkout đã hoàn tất trong base này.
- **NEEDS DECISION — idempotency:** base chưa có khóa idempotency lưu DB. Ref/loading ngăn bấm đúp trong một form; timeout sau commit yêu cầu kiểm tra danh sách đơn trước khi thử lại. Chưa cam kết exactly-once qua nhiều request sau mất kết nối.

### BUG-006

- Base đã bỏ JWT default, fail-fast ở JwtTokenProvider, reject fingerprint khóa từng lộ, tách AdminBootstrap opt-in khỏi DataSeeder dev-only. DB password fallback vẫn còn ở hai application runtime; thiếu marker test-only và chính sách độ mạnh admin mới chỉ kiểm độ dài.
- Red `QaSecretConfigurationTest`: 3/4 ca fail (runtime DB fallback, marker test key, mật khẩu bootstrap yếu vượt qua bước kiểm). Green các context JWT thiếu/rỗng/placeholder/ngắn/đã lộ không start; khóa hợp lệ start; test BCrypt và captured log không thấy password/hash. `@Validated`/`@NotBlank` ở JwtProperties; kiểm byte length/non-empty secret ở provider để Spring không echo secret bị từ chối.
- Cả default/dev dùng DB_URL/DB_USERNAME/DB_PASSWORD; không fallback credential. H2 config ở src/test/resources dùng key `test-only-` + random, không có runtime application-test. Bootstrap mặc định không tạo admin, log hướng dẫn không có giá trị; khi bật, yêu cầu đủ biến và mật khẩu >=12 ký tự, <=72 byte, có hoa/thường/số. Không sửa admin đang tồn tại.
- `.env.example`, `.gitignore`, README và hướng dẫn Windows/Bash đã cập nhật. Không thêm thư viện. Workflow frontend giữ GitHub Secrets cho token deploy, không nhận secret backend.
- Chỉ loại secret khỏi cây mã hiện tại; lịch sử vẫn chứa secret cũ. **Bắt buộc ROTATE ngoài nhiệm vụ**, xem [SECRET-OPERATIONS.md](SECRET-OPERATIONS.md). Không reset mật khẩu hoặc thay credential DB thật.

## Tổng kết trạng thái và commit

Các trạng thái dưới đây nói về nhánh sửa lỗi từ base audit, không phải `main cfd890d` hoặc feature branch chưa hợp nhất.

| Bug | Trạng thái trên nhánh này | Phần đã có / phần bổ sung | Red → green | Commit |
| --- | --- | --- | --- | --- |
| BUG-001 | ĐÃ SỬA | Ép CUSTOMER đã có; bỏ role DTO công khai, staff DTO riêng, bảo vệ admin | 5 fail → PASS | `4cced7d` |
| BUG-002 | ĐÃ SỬA; CRUD chính ĐÃ ĐƯỢC SỬA TRƯỚC ĐÓ | Guard controller đã có; thêm URL-before-binding và inventory/matrix test | 1 fail → PASS | `1aa2530` |
| BUG-003 | ĐÃ SỬA | CUSTOMER IDOR đã chặn; thêm 404, query owner, scope staff, /me và test JWT A/B | 5 fail → PASS | `b787cdc` |
| BUG-007 | ĐÃ SỬA; số âm/khóa ĐÃ ĐƯỢC SỬA TRƯỚC ĐÓ | Giới hạn mutation ADMIN cả service; history resource 404 | 3 fail → PASS | `40b5ce5` |
| BUG-005 | ĐÃ SỬA; storefront chính ĐÃ ĐƯỢC SỬA TRƯỚC ĐÓ | Quiz owner filter/PublicPetDTO; RESERVED detail | 3 fail → PASS | `e89bebf` |
| BUG-004 | ĐÃ SỬA cho product checkout; NEEDS DECISION tích hợp feature pet | Persistence/tracking đã có; cart atomic, CUSTOMER-only, 409, ID response | 4 fail + 1 error → PASS | `538b6df` |
| BUG-006 | ĐÃ SỬA trong source; NEEDS DECISION rotate môi trường | JWT/admin opt-in đã có; bỏ DB fallback, strength, test config, tài liệu | 3 fail → PASS | `HEAD` — `fix(BUG-006): remove credential fallbacks and verify security configuration` |

## Quyền trước/sau và kiểm sở hữu

[docs/api.md](../api.md) là inventory **102 endpoint** hiện hành, thay bản mô tả cũ có route/role không tồn tại. Method security luôn bật. Base đã có guard cho product/category/promotion/service/pet quản trị, clinical, inventory, moderation và admin. Thay đổi chính:

| Endpoint/nhóm | Trước (44027f8) | Sau | Test |
| --- | --- | --- | --- |
| POST auth/register | DTO nhận RoleType, factory ép CUSTOMER | DTO bỏ role, unknown ignored; login quyền DB | QaRegistrationSecurityTest, StaffRegistrationTest |
| POST admin/staff; PATCH admin/users/{id}/role | ADMIN, DTO dùng chung; có thể hạ admin cuối | StaffCreateRequest; ADMIN service guard; không hạ admin qua API chung | StaffRegistrationTest, QaRegistrationSecurityTest |
| Write products/categories/promotions/services | ADMIN/SALES_STAFF ở method | Thêm filter trước JSON + method guard | CatalogAuthorizationTest, EndpointSecurityMatrixTest |
| Write medical-records/vaccinations/treatments | VETERINARIAN/ADMIN ở method | Thêm filter trước JSON | AuditSecurityTest, TreatmentFlowTest, EndpointSecurityMatrixTest |
| Write inventory | WAREHOUSE_STAFF/ADMIN ở method | Thêm filter trước JSON | InventoryServiceImplTest, matrix |
| PUT reviews/{id}/approve; order confirm/process/ship/deliver | ADMIN moderation; ADMIN/SALES_STAFF order service | Filter phù hợp + guard hiện có | matrix, OrderFlowTest |
| GET appointments | Staff được xem tất cả | ADMIN tất cả; SALES_STAFF/VETERINARIAN chỉ assigned | QaAppointmentScopeTest |
| GET appointments/user/{userId} | Chính chủ/ADMIN | Giữ guard, thêm /me; legacy deprecated | QaAppointmentScopeTest |
| GET appointments/pet/{petId}; PUT {id}/status | Chủ/clinical; từ chối 403 | Query scoped, bác sĩ lịch assigned, tài nguyên ngoài phạm vi 404 | QaAppointmentScopeTest, AppointmentAuthorizationTest |
| GET clinical id/pet/record; GET/PUT my-pets/{id} | Chủ/clinical hoặc owner; người khác 403 | Ownership query owner; người khác 404; ghi clinical chỉ V/ADMIN | PrivatePetDataTest, TreatmentFlowTest |
| GET notifications/{userId}; PUT {id}/read | UserId chéo 403; id chéo 403 | UserId chéo 403; id query theo chủ và 404; ADMIN không override | PrivatePetDataTest |
| GET orders/{id}; POST cancel | Chủ/ADMIN (+SALES_STAFF đọc); chéo 403 | Query owner, chéo 404; cancel lock scoped | OrderFlowTest |
| POST orders | Mọi authenticated | CUSTOMER filter/controller/service | QaCheckoutAtomicTest |
| POST loyalty add/spend | Add ADMIN; spend chủ/ADMIN, service không guard | Mọi mutation ADMIN cả service; số âm/tràn/thiếu dư vẫn 400 | QaLoyaltyBoundaryTest, LoyaltyLimitsTest |
| GET loyalty accountId/transactions | requireUser, chéo 403 | Query accountId+customerId, chéo 404; ADMIN override | QaLoyaltyBoundaryTest |
| POST pet-matching/submit/{userId} | Bỏ qua path ID | Alias chéo 403; /submit lấy principal | MatchingTransactionTest |

`EndpointSecurityMatrixTest` kiểm tất cả endpoint anonymous, allowlist write CUSTOMER, tất cả năm RoleType cho 37 write nhân viên, và registry ca ownership cho mọi ID riêng tư không thuộc nhóm staff-only. Các API write dành cho chủ (cart, profile, pet cá nhân, notifications, booking/cancel, order/cancel, review, quiz) không cần role nhân viên; không gán chúng thành thiếu guard. Public catalog IDs không có owner. Các write clinical/admin chỉ kiểm role vì policy cho phép truy cập toàn miền. GET inventory/{productId} giữ quyền authenticated hiện có, không coi là tài nguyên cá nhân. Không còn endpoint riêng tư theo path ID thiếu test trong registry này; đây không phải chứng minh mọi payload/combination nghiệp vụ đã được kiểm.

## Inventory kiểm secret (không ghi giá trị)

| File / key | Kết quả |
| --- | --- |
| application.yml, application-dev.yml / spring.datasource.password | Fallback cũ đã loại; `${DB_PASSWORD}` |
| application*.yml / spring.datasource.username, url | Env DB_USERNAME/DB_URL; host/port/database default không nhạy cảm |
| application.yml / app.jwt.secret | `${JWT_SECRET}`, không default; đã đúng ở base |
| JwtProperties.secret / JwtTokenProvider | NotBlank + kiểm >=32 byte, placeholder/fingerprint; không echo giá trị |
| AdminBootstrap / app.bootstrap.* | Env opt-in, BCrypt; không literal credential, không reset existing admin |
| DataSeeder | Chỉ dev, không seed admin/user password |
| backend/.env.example | Khóa credential rỗng, ví dụ URL không chứa credential |
| src/test/resources/application-test.yml | H2, password rỗng, JWT test-only + random; không đóng gói runtime |
| seed_pets.js / password | Biến môi trường, không credential cố định; script không được chạy |
| frontend/.env.example, vite.config, frontend types | API URL/types, không secret backend; trường password trong interface là type |
| .github/workflows/deploy.yml / github_token | GitHub Secrets đã có; API URL là repo variable; không thêm secret backend vào bundle |
| schema-sqlserver.sql / password_hash | Khai báo cột, không sửa |
| data-sqlserver.sql / password_hash | Placeholder seed cần operator cấp, không literal usable secret; không sửa/chạy |
| README/docs | Hướng dẫn env/rotate; đã cập nhật đoạn fallback lỗi thời, không hướng dẫn nhập credential vào YAML |
| docker-compose, runtime application.properties, frontend .env thật, SQL CREATE LOGIN | Không thấy file/statement tracked tương ứng chứa credential cố định |

Quét `git grep` theo tên key chỉ xuất file/key hoặc phân loại; không ghi giá trị secret/hash/token. Sau sửa không còn password fallback ở runtime source. Fingerprint SHA-256 của khóa từng lộ không phải signing key và được giữ để ngăn dùng lại. Không quét/ghi nội dung file môi trường cá nhân không tracked.

## Rà soát dữ liệu và rotate — chưa thực hiện mutation

**Pet:** SELECT chỉ ID trên DB dev hiện tại tìm thấy pet **6** có owner; không có hàng vừa owner khác null vừa AVAILABLE ở thời điểm đối soát. QA cũ trên main trả mọi pet nên vẫn có thể làm lộ pet 6 dù không AVAILABLE. Không suy ra dữ liệu đã bị sửa bởi nhiệm vụ này. Đề xuất giữ owner đúng, kiểm availability theo nghiệp vụ; query/DTO mới loại mọi pet có chủ khỏi storefront/quiz.

Các truy vấn sau chỉ là gợi ý người vận hành chạy trong môi trường được phép; không tự thu hồi quyền/sửa số dư/dữ liệu. Không chọn email/SĐT/hash/password:

```sql
-- Nhân viên/admin cần đối chiếu phê duyệt tạo tài khoản; role tự nó không chứng minh bất hợp lệ.
SELECT u.id, r.name AS role_name, u.created_at
FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id
WHERE r.name IN ('SALES_STAFF','WAREHOUSE_STAFF','VETERINARIAN','ADMIN')
ORDER BY u.created_at DESC;

-- Giao dịch điểm sai dấu và số dư bất thường cần đối soát, không tự sửa.
SELECT id, loyalty_account_id, transaction_type, points, created_at
FROM loyalty_transactions WHERE transaction_type='REDEEM' AND points<=0;
SELECT id, customer_id, available_points, total_points
FROM loyalty_accounts WHERE available_points<0 OR total_points<0 OR available_points>total_points;
WITH ledger AS (
  SELECT loyalty_account_id,
    SUM(CASE WHEN transaction_type='EARN' THEN CAST(points AS bigint)
             WHEN transaction_type='REDEEM' THEN -CAST(points AS bigint) ELSE 0 END) AS expected_balance,
    SUM(CASE WHEN transaction_type NOT IN ('EARN','REDEEM') THEN 1 ELSE 0 END) AS other_types
  FROM loyalty_transactions GROUP BY loyalty_account_id
)
SELECT a.id, a.available_points, l.expected_balance
FROM loyalty_accounts a JOIN ledger l ON l.loyalty_account_id=a.id
WHERE l.other_types=0 AND a.available_points<>l.expected_balance;
-- Tài khoản có ADJUST/EXPIRE, số dư mở đầu hoặc dữ liệu seed phải đối soát theo chính sách riêng.

SELECT id, availability_status FROM pets WHERE owner_id IS NOT NULL;

-- Phát hiện đơn tồn tại nhưng rỗng hoặc tổng dòng không khớp subtotal.
SELECT o.id, o.order_number, o.status, o.subtotal, COUNT(i.id) AS item_count,
       COALESCE(SUM(i.subtotal),0) AS item_subtotal
FROM orders o LEFT JOIN order_items i ON i.order_id=o.id
GROUP BY o.id,o.order_number,o.status,o.subtotal
HAVING COUNT(i.id)=0 OR o.subtotal<>COALESCE(SUM(i.subtotal),0);
```

Đơn “thành công giả” không được persist ở main cũ không thể khôi phục chỉ bằng SELECT; cần đối chiếu log/audit/khách hàng theo quy trình vận hành. Không tự tạo bù đơn hoặc tự trừ kho.

Checklist rotate: JWT mới trên mọi instance (token cũ mất hiệu lực), mật khẩu DB mới và quyền tối thiểu, đổi admin từng seed qua quy trình được xác thực, kiểm tài khoản nhân viên trái phép, tắt bootstrap sau dùng. Xem hướng dẫn chi tiết [SECRET-OPERATIONS.md](SECRET-OPERATIONS.md). Không rewrite lịch sử git/force push.

## Gate cuối, giới hạn và file bảo vệ

- Backend `./mvnw.cmd clean test` đã loại class/report cũ của branch feature, PASS 200 test (0 failure/error, 3 skip SQL Server); sau bổ sung matrix đủ năm role chạy lại full `test`, kết quả cuối ghi bên dưới.
- Test lỗi update pet trong lần rà cuối là assertion giá JSON không đúng contract: private DTO chủ động ẩn price. Đã sửa test để xác minh giá lưu DB không bị đổi và DTO vẫn ẩn giá; không nới logic ứng dụng để làm test qua.
- Frontend `npx tsc --noEmit`, `npm run lint`, `npm run build` PASS sau BUG-003/005/004. Cache tsbuildinfo được restore; không commit dist/target/node_modules.
- Không chạy mutation DB dev/production, không chạy SQL reset hoặc migration, không chạy browser E2E trong phiên sửa này. Concurrency đã chạy thật bằng transaction H2; SQL Server isolation/deadlock khác H2 nên ba test opt-in vẫn cần container riêng trước release. Manual checks nằm trong [MANUAL-CHECKS.md](MANUAL-CHECKS.md).
- Không có thay đổi Đợt G chưa commit tại baseline. `data-sqlserver.sql`, `schema-sqlserver.sql`, `reset-demo-sqlserver.sql`, `package.json`, `package-lock.json` được giữ nguyên so với base audit `44027f8`. Không thêm dependency/migration. Báo cáo QA cũ ở `docs/qa/` vẫn untracked, không được đưa vào các commit này.
- Chỉ stage đường dẫn tường minh theo từng bug. Không merge/rebase main/feature, không push/force hoặc sửa lịch sử.

**Kết quả gate cuối:** 45 suite, **201 test: 198 pass, 0 failure, 0 error, 3 skip** (SQL Server opt-in). Full test chạy sau khi thêm matrix đủ năm role; `git diff --check` PASS. Không cần đổi thư viện hoặc schema. Nhật ký cuối cục bộ ở `ppet/backend/qa-final-tests.log` (ignored, không commit). Các log red/green tạm trong target đã được xóa bởi clean; kết quả đỏ được ghi ở từng mục và có thể tái hiện với test so với parent commit tương ứng.

**Giới hạn để release:** review/hợp nhất main và feature theo NEEDS DECISION; rotate credential thực tế; kiểm SQL Server container, trình duyệt và các bước trong MANUAL-CHECKS. Đây là các bước vận hành/tích hợp chưa được thực hiện, không phải kết quả đã PASS.

## File thay đổi theo commit

### BUG-001 — 4cced7d

- `ppet/backend/src/main/java/com/petcarehub/controller/AdminController.java`
- `ppet/backend/src/main/java/com/petcarehub/controller/AuthController.java`
- `ppet/backend/src/main/java/com/petcarehub/dto/auth/RegisterRequest.java`
- `ppet/backend/src/main/java/com/petcarehub/dto/auth/StaffCreateRequest.java`
- `ppet/backend/src/main/java/com/petcarehub/service/StaffAccountService.java`
- `ppet/backend/src/main/java/com/petcarehub/service/impl/AdminServiceImpl.java`
- `ppet/backend/src/test/java/com/petcarehub/QaRegistrationSecurityTest.java`
- `ppet/docs/qa-critical-001-007/REPORT.md`

### BUG-002 — 1aa2530

- `ppet/backend/src/main/java/com/petcarehub/config/SecurityConfig.java`
- `ppet/backend/src/test/java/com/petcarehub/CatalogAuthorizationTest.java`
- `ppet/backend/src/test/java/com/petcarehub/EndpointSecurityMatrixTest.java`
- `ppet/docs/qa-critical-001-007/REPORT.md`

### BUG-003 — b787cdc

- `ppet/backend/src/main/java/com/petcarehub/controller/AppointmentController.java`
- `ppet/backend/src/main/java/com/petcarehub/controller/NotificationController.java`
- `ppet/backend/src/main/java/com/petcarehub/controller/PetController.java`
- `ppet/backend/src/main/java/com/petcarehub/controller/PetMatchingController.java`
- `ppet/backend/src/main/java/com/petcarehub/repository/AppointmentRepository.java`
- `ppet/backend/src/main/java/com/petcarehub/repository/NotificationRepository.java`
- `ppet/backend/src/main/java/com/petcarehub/repository/OrderRepository.java`
- `ppet/backend/src/main/java/com/petcarehub/repository/PetRepository.java`
- `ppet/backend/src/main/java/com/petcarehub/security/Ownership.java`
- `ppet/backend/src/main/java/com/petcarehub/service/impl/AppointmentServiceImpl.java`
- `ppet/backend/src/main/java/com/petcarehub/service/impl/OrderServiceImpl.java`
- `ppet/backend/src/test/java/com/petcarehub/AppointmentAuthorizationTest.java`
- `ppet/backend/src/test/java/com/petcarehub/AuditSecurityTest.java`
- `ppet/backend/src/test/java/com/petcarehub/BookingOwnershipTest.java`
- `ppet/backend/src/test/java/com/petcarehub/EndpointSecurityMatrixTest.java`
- `ppet/backend/src/test/java/com/petcarehub/MatchingTransactionTest.java`
- `ppet/backend/src/test/java/com/petcarehub/OrderFlowTest.java`
- `ppet/backend/src/test/java/com/petcarehub/PrivatePetDataTest.java`
- `ppet/backend/src/test/java/com/petcarehub/QaAppointmentScopeTest.java`
- `ppet/backend/src/test/java/com/petcarehub/TreatmentFlowTest.java`
- `ppet/backend/src/test/java/com/petcarehub/service/impl/AppointmentServiceImplTest.java`
- `ppet/docs/qa-critical-001-007/REPORT.md`
- `ppet/frontend/src/services/bookingService.ts`

### BUG-007 — 40b5ce5

- `ppet/backend/src/main/java/com/petcarehub/config/SecurityConfig.java`
- `ppet/backend/src/main/java/com/petcarehub/controller/LoyaltyController.java`
- `ppet/backend/src/main/java/com/petcarehub/repository/LoyaltyAccountRepository.java`
- `ppet/backend/src/main/java/com/petcarehub/service/impl/LoyaltyServiceImpl.java`
- `ppet/backend/src/test/java/com/petcarehub/BalanceAndCartConcurrencyTest.java`
- `ppet/backend/src/test/java/com/petcarehub/EndpointSecurityMatrixTest.java`
- `ppet/backend/src/test/java/com/petcarehub/LoyaltyLimitsTest.java`
- `ppet/backend/src/test/java/com/petcarehub/LoyaltyOwnershipTest.java`
- `ppet/backend/src/test/java/com/petcarehub/QaLoyaltyBoundaryTest.java`
- `ppet/docs/qa-critical-001-007/REPORT.md`

### BUG-005 — e89bebf

- `ppet/backend/src/main/java/com/petcarehub/dto/matching/MatchResultDTO.java`
- `ppet/backend/src/main/java/com/petcarehub/dto/pet/PublicPetDTO.java`
- `ppet/backend/src/main/java/com/petcarehub/repository/PetRepository.java`
- `ppet/backend/src/main/java/com/petcarehub/service/impl/PetMatchingServiceImpl.java`
- `ppet/backend/src/test/java/com/petcarehub/MatchingTransactionTest.java`
- `ppet/backend/src/test/java/com/petcarehub/PrivatePetDataTest.java`
- `ppet/backend/src/test/java/com/petcarehub/service/impl/PetAuditTest.java`
- `ppet/docs/qa-critical-001-007/REPORT.md`
- `ppet/frontend/src/pages/pets/PetDetailPage.tsx`
- `ppet/frontend/src/pages/pets/PetMatchingPage.tsx`

### BUG-004 — 538b6df

- `ppet/backend/src/main/java/com/petcarehub/config/SecurityConfig.java`
- `ppet/backend/src/main/java/com/petcarehub/controller/OrderController.java`
- `ppet/backend/src/main/java/com/petcarehub/dto/order/OrderCreateRequest.java`
- `ppet/backend/src/main/java/com/petcarehub/dto/order/OrderItemDTO.java`
- `ppet/backend/src/main/java/com/petcarehub/service/impl/InventoryServiceImpl.java`
- `ppet/backend/src/main/java/com/petcarehub/service/impl/OrderServiceImpl.java`
- `ppet/backend/src/test/java/com/petcarehub/CheckoutPersistenceTest.java`
- `ppet/backend/src/test/java/com/petcarehub/OrderFlowTest.java`
- `ppet/backend/src/test/java/com/petcarehub/QaCheckoutAtomicTest.java`
- `ppet/backend/src/test/java/com/petcarehub/service/impl/InventoryServiceImplTest.java`
- `ppet/docs/qa-critical-001-007/REPORT.md`
- `ppet/frontend/src/pages/orders/CheckoutPage.tsx`

### BUG-006 — HEAD và gate cuối

- `.gitignore`
- `ppet/backend/.env.example`
- `ppet/backend/src/main/java/com/petcarehub/config/AdminBootstrap.java`
- `ppet/backend/src/main/java/com/petcarehub/config/AdminBootstrapNotice.java`
- `ppet/backend/src/main/java/com/petcarehub/config/JwtProperties.java`
- `ppet/backend/src/main/resources/application-dev.yml`
- `ppet/backend/src/main/resources/application.yml`
- `ppet/backend/src/test/java/com/petcarehub/AdminBootstrapTest.java`
- `ppet/backend/src/test/java/com/petcarehub/EndpointSecurityMatrixTest.java`
- `ppet/backend/src/test/java/com/petcarehub/PrivatePetDataTest.java`
- `ppet/backend/src/test/java/com/petcarehub/QaSecretConfigurationTest.java`
- `ppet/backend/src/test/resources/application-test.yml`
- `ppet/docs/api.md`
- `ppet/docs/qa-critical-001-007/MANUAL-CHECKS.md`
- `ppet/docs/qa-critical-001-007/REPORT.md`
- `ppet/docs/qa-critical-001-007/SECRET-OPERATIONS.md`
- `ppet/README.md`
