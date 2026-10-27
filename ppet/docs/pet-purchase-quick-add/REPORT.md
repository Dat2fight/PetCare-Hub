# Hoàn thiện mua thú cưng và thêm nhanh khi đặt lịch

Ngày kiểm tra: 20/10/2026. Workspace thực tế: `D:/visual studio/PetCare-Hub/ppet`.
Nhánh: `feature/pet-purchase-quick-add`, tạo từ `44027f8`.

## 1. Khảo sát trước khi sửa

| Hạng mục | Kết quả đọc code thực tế |
| --- | --- |
| Git | Working tree sạch lúc bắt đầu. Các thay đổi Đợt G không còn ở trạng thái chưa commit như giả định trong yêu cầu; HEAD đã chứa chúng. Không sửa hay đưa các file bị cấm vào commit nhiệm vụ này. |
| Order request | `OrderCreateRequest` có `userId`, items, shippingAddress, paymentMethod; checkout chỉ hỗ trợ COD. Không có field SĐT riêng, không có mã giảm giá hay điểm thưởng trong createOrder. Entity Order đã có notes. Bỏ userId khỏi request; thêm notes vào DTO theo field entity sẵn có. |
| OrderItem | Entity đã map LAZY product và pet, cả hai nullable; itemType lưu STRING. Schema có CHECK bảo đảm đúng loại và đúng một tham chiếu; product_id đã nullable, không cần đổi cột này. Giá snapshot là unitPrice/subtotal BigDecimal. |
| Trạng thái pet | Enum thật là `AvailabilityStatus`: AVAILABLE, SOLD, RESERVED, UNAVAILABLE; thiếu PENDING. SQL có `CK_pets_avail_status`. Chưa có liên kết pet với đơn giữ chỗ. |
| Pet | PetDTO có ownerId, price, availabilityStatus, birthDate và age tính từ ngày sinh. Breed nullable ở entity/schema. PublicPetDTO riêng không chứa chủ hay tình trạng bệnh. Danh sách bán đã lọc AVAILABLE và owner null; chi tiết trước đây chỉ cho AVAILABLE chưa có chủ. |
| Endpoint cá nhân | POST `/api/v1/pets/my-pets` đã tồn tại nhưng dùng PetCreateRequest của quản trị; chưa có giới hạn số lượng và ràng buộc role CUSTOMER ở method. Matcher my-pets authenticated đã đứng trước wildcard public. |
| Identity | `Ownership.userId()`, `hasRole(String)`, `requireUser(Long)`, `requirePet(Long)` đã tồn tại. requireUser cho chủ hoặc ADMIN. Tái sử dụng, không tạo cơ chế xác thực mới. |
| Loài/tuổi | Species là bảng, không có mã dog/cat ổn định. Java seed dùng Dog/Cat, SQL seed dùng Chó (Dog)/Mèo (Cat). Không hardcode ID. Theo cách xử lý tuổi hiện có: ngày sinh = hôm nay trừ số năm, tuổi suy ra từ ngày sinh. |
| Order/inventory | createOrder trước đây chỉ xử lý sản phẩm; cancel hoàn kho; mapper không hiển thị pet. Tồn kho sản phẩm dùng service trong cùng transaction. Chưa có scheduler hết hạn đơn/giữ chỗ. |
| Các consumer khác | AdminServiceImpl tính doanh thu từ tổng đơn. ReviewServiceImpl/repository đã hỗ trợ điều kiện mua pet. Không tìm thấy mapper email/hóa đơn/loyalty đang dereference product của OrderItem. |
| Frontend | Chưa có orderService hay Modal chung. Checkout gọi API trực tiếp. Service pet hiện có là petManagementService. Button đã có size. Input thực tế chưa nối label với input: bổ sung id/htmlFor và aria-describedby vào component đó. |
| Booking | Route đã nằm trong ProtectedRoute. Giờ trống có loading/rỗng/lỗi; giữ nguyên. Danh sách myPets trước đây chưa phân biệt lỗi tải với rỗng. MyPetsPage cũng gọi POST cũ nên cần đổi sang cùng form thêm nhanh. |
| Hồ sơ người dùng | Có firstName/lastName/phone, không có địa chỉ để tự điền. Dùng shippingAddress chứa tên, SĐT, địa chỉ như checkout sản phẩm hiện có. |
| Tài liệu API | docs/api.md chưa đồng bộ #60: còn PUT status/cancel và apply-promotion không khớp controller thật. Không sửa file này theo điều kiện của yêu cầu; hợp đồng mới ghi bên dưới. |

Rủi ro chính đã xử lý: giữ chỗ đồng thời, mất liên kết giữa pet và đơn, hoàn kho nhầm với pet, giả mạo danh tính/giá, lộ dữ liệu pet cá nhân và vượt giới hạn bằng nhiều request đồng thời.

## 2. Tiến độ và commit

Đường dẫn trong bảng tính từ `ppet/`; tên Java trong package `com.petcarehub`.

| Mục | Trạng thái | File chính | Commit | Kiểm chứng |
| --- | --- | --- | --- | --- |
| 1A | ĐÃ XONG; migration chờ áp dụng | backend: enums/AvailabilityStatus.java, entity/Pet.java, resources/migrations/004_pet_purchase.sql | `4fca5f1` | H2 persistence; thêm migration vào SqlServerConstraintTest tùy chọn |
| 1B | ĐÃ XONG | dto/order/OrderItemDTO.java, OrderCreateRequest.java, OrderDTO.java | `4fca5f1` | PetPurchaseTest: XOR, qty, duplicate, null item, forged identity/price |
| 1C | ĐÃ XONG | service/impl/OrderServiceImpl.java, PetServiceImpl.java, repository/PetRepository.java | `4fca5f1` | Mixed order, stock isolation, rollback, delivery/cancel, reservation mismatch, concurrent purchase |
| 1D | ĐÃ XONG | controller/OrderController.java, service/OrderService.java, dto/pet/PublicPetDTO.java, mapper/PetMapper.java | `4fca5f1` | HTTP 401/403/404/409, list/detail DTO, privacy tests |
| 1E | ĐÃ XONG | frontend: components/common/{Input,Modal}.tsx, components/pets/PetPurchaseModal.tsx, components/orders/OrderItems.tsx, services/orderService.ts, pages/pets/PetDetailPage.tsx, pages/orders/{CheckoutPage,OrdersPage,OrderTrackingPage}.tsx, types/index.ts | `e609a39` | TypeScript, ESLint, build; checklist UI bên dưới |
| 1F | ĐÃ XONG với giới hạn kiểm thử nêu dưới | backend tests: PetPurchaseTest.java (9), service/impl/OrderPetValidationTest.java (1); cập nhật CheckoutPersistenceTest, OrderFlowTest, PetAuditTest, SqlServerConstraintTest | `4fca5f1` | Full Maven suite; frontend không có framework test, không thêm dependency |
| 2A | ĐÃ XONG | backend: config/PersonalPetProperties.java, dto/pet/QuickPetCreateRequest.java, controller/PetController.java, repository/PetRepository.java, service/PetService.java, service/impl/PetServiceImpl.java, QuickPetCreateTest.java, PetAuditTest.java | `7fe4d3c` | 6 test: owner/privacy, fields/species, role, cap, concurrent cap, create→book |
| 2B | ĐÃ XONG | frontend: components/pets/QuickAddPetModal.tsx, services/petManagementService.ts, pages/services/BookingPage.tsx, pages/pets/MyPetsPage.tsx; báo cáo này | Commit `feat(booking): quick add pet UI` chứa báo cáo | TypeScript, ESLint, build; checklist UI bên dưới |

Các test cũ được chỉnh vì request không còn setter userId và PetServiceImpl có thêm dependency/khóa pet. CheckoutPersistenceTest dùng principal CUSTOMER thật; việc giả mạo body được kiểm tra qua HTTP trong PetPurchaseTest. Không xóa assertion của luồng sản phẩm. Hủy lặp đơn chỉ chứa sản phẩm vẫn giữ hành vi cũ; hủy lặp đơn có pet thành công và không hoàn kho lần hai.

## 3. Hành vi đã triển khai

- Order identity lấy từ Ownership, chỉ CUSTOMER và không đồng thời mang role nhân viên/quản trị được tạo đơn. Các endpoint xử lý đơn giữ quyền ADMIN/SALES_STAFF; hủy cho chủ đơn hoặc ADMIN.
- Item là đúng một productId hoặc petId. Pet quantity phải bằng 1, không trùng petId. Validation ở DTO và kiểm tra phòng vệ tại service. Giá pet/product lấy DB, không dùng giá client.
- Khóa pet tăng dần bằng PESSIMISTIC_WRITE, hint timeout 5000 ms; tồn kho sản phẩm khóa sau pet. Khi thành công pet PENDING và reservedOrder trỏ đúng đơn. Toàn bộ giao dịch rollback nếu một item sai.
- Giao đơn khóa pet, kiểm tra đúng reservation và state machine trước khi chuyển SOLD/owner. Hủy trả AVAILABLE, xóa reservation; retry hủy đơn có pet không chạm reservation của đơn sau. Update/delete quản trị cũng khóa pet và chặn pet đang giữ chỗ.
- Danh sách công khai chỉ pet chưa có chủ, AVAILABLE. Chi tiết PENDING chưa có chủ hiển thị trạng thái; SOLD trả bản ghi chỉ có id, tên chung “Thú cưng đã bán”, trạng thái, không lộ thông tin thú cưng cá nhân/chủ. Các pet cá nhân không bán trả 404 ở public detail.
- Quick add chỉ nhận name/speciesId/age; owner principal, giá 0, UNAVAILABLE, giống null, ngày sinh ước lượng. Khóa tài khoản trước khi đếm và tạo để ngăn hai request vượt cap.
- Modal dùng native dialog: focus trap, Esc, aria-modal/label và trả focus. Có chặn submit kép. Quick add có loading/lỗi/retry cho danh mục loài, hiển thị field error theo ErrorResponse hiện có. Booking phân biệt lỗi tải pet với empty, thêm xong tự chọn pet mà không đổi dịch vụ/ngày/giờ.

Các vị trí sửa vì giả định product không null:

1. OrderServiceImpl.createOrder: tách PET/PRODUCT, pet không gọi inventory.
2. OrderServiceImpl.cancelOrder: chỉ filter product để hoàn kho; pet dùng nhả reservation.
3. OrderServiceImpl.dto và OrderController.mine: một mapper chung cho cả loại item, tránh DTO list thiếu pet.
4. Frontend OrdersPage/OrderTrackingPage: OrderItems dùng tên/ảnh pet hoặc product và giá snapshot.

Kết quả `rg 'getProduct\(\)' backend/src/main`: CartServiceImpl và InventoryServiceImpl đã có null guard; ProductServiceImpl sử dụng kết quả query chỉ dành cho product inventory. Admin revenue/review không cần sửa cho null product.

## 4. Hợp đồng API

Tạo đơn, khách hàng đã đăng nhập, `POST /api/v1/orders`:

```json
{
  "items": [{"petId": 123, "quantity": 1}],
  "shippingAddress": "Người nhận, SĐT, địa chỉ",
  "paymentMethod": "COD",
  "notes": "Ghi chú tùy chọn"
}
```

ID 123 là ví dụ; lấy ID thật từ danh mục. Có thể thêm item `{ "productId": 456, "quantity": 2 }` vào cùng đơn. Không gửi giá/danh tính. Các field giả mạo userId/customerId/ownerId/price/totalAmount/availability ở request bị bỏ qua; item.price là read-only. Response giữ totalPrice, items[].price/quantity/productId/productName, bổ sung petId/petName/imageUrl và notes. GET list trả page OrderDTO.

Thêm nhanh, `POST /api/v1/pets/my-pets`, trả 201 PetDTO:

```json
{"name":"Bông","speciesId":123,"age":2}
```

speciesId phải lấy từ GET `/api/v1/species`, lọc Chó/Mèo hợp lệ. name 1–100 ký tự không toàn khoảng trắng; age số nguyên 0–30. Body ownerId/price/availability không được sử dụng. Lỗi validation 400 theo field trong message hiện có; JSON sai kiểu trả 400 chung. Vượt cap 409. Không đăng nhập 401, sai role 403.

## 5. Migration — chưa chạy trên database thật

Script: `backend/src/main/resources/migrations/004_pet_purchase.sql`.

Script thêm cột nullable `pets.reserved_order_id`, FK tới orders không cascade delete, mở CHECK availability cho PENDING. Giữ các giá trị cũ AVAILABLE/SOLD/RESERVED/UNAVAILABLE; không sửa dữ liệu hiện có. Có transaction, XACT_ABORT, TRY/CATCH rollback; có thể chạy lặp. Không cần sửa order_items.product_id vì đã nullable.

Thao tác triển khai:

1. Sao lưu database và kiểm tra trên bản sao staging. Xác nhận các migration 001–003 theo hướng dẫn triển khai hiện có.
2. Tạm dừng thao tác ghi khi triển khai. Mở SQL Server Management Studio, chọn đúng database PetCare Hub, chạy nội dung 004; hoặc dùng lệnh mẫu bên dưới sau khi thay server/database.
3. Kiểm tra cột reserved_order_id, FK_pets_reserved_order và CK_pets_avail_status có PENDING; khởi động phiên bản backend mới và smoke test mua/hủy/giao.

```powershell
# Chạy từ ppet/backend. Đây là mẫu, chưa được thực thi trong nhiệm vụ này.
sqlcmd -S <server> -d <database> -E -b -i src/main/resources/migrations/004_pet_purchase.sql
```

Không tự gỡ cột/CHECK khi đã có đơn PENDING; cần giải quyết reservation trước khi rollback phiên bản. Mã mới phụ thuộc migration này, kể cả luồng thêm nhanh vì entity Pet có cột mới.

## 6. NEEDS DECISION

| Điểm cần chốt | Phương án đang dùng | Cách thay đổi |
| --- | --- | --- |
| Áp dụng migration | Chỉ cung cấp 004, không chạy database thật | Chủ hệ thống kiểm tra/áp dụng trước deploy |
| Hết hạn PENDING | Chưa có job, giữ chỗ tới khi hủy/giao; không tự thêm scheduler | Đề xuất chốt TTL 24 giờ; job sau này phải khóa đơn/pet và gọi cùng logic nhả pet/hoàn kho đúng một lần |
| Hủy sau giao | Cấm qua cancel; trả 409 | Luồng hoàn trả riêng cần quy tắc nhận lại pet, owner và thanh toán |
| Hủy lặp | Idempotent cho đơn có pet, giữ hành vi cũ cho product-only | Nếu muốn thống nhất toàn hệ thống, thay trong nhiệm vụ riêng và cập nhật contract/tests |
| Khuyến mãi/loyalty/phí | CreateOrder hiện không áp dụng; không thêm ưu đãi hay phí riêng cho pet | Cần quy tắc theo loại item trước khi tích hợp |
| Thanh toán | COD hiện có, ghi COMPLETED khi giao như luồng cũ | Online payment cần tích hợp trạng thái thanh toán/timeout riêng |
| Giới hạn cá nhân | Mặc định 20, đếm mọi pet hiện đang sở hữu; giới hạn này chặn đăng ký nhanh, không chặn nhận pet từ đơn đã mua | Đặt `app.pets.max-personal-pets` hoặc env `APP_PETS_MAX_PERSONAL_PETS` số dương; cap toàn bộ sở hữu cần phối hợp cả giao đơn |
| Loài | So khớp tên Dog/Cat/Chó/Mèo/Chó (Dog)/Mèo (Cat), không phân biệt hoa thường, trim, không ID cố định | Nên thêm species code ổn định trong migration riêng nếu muốn đổi tên tự do |
| Ngày sinh/giống | Ước lượng hôm nay trừ age năm, giống null; không bịa giống | Muốn chính xác ngày sinh cần mở rộng DTO/form; giữ dateOfBirth là nguồn tính tuổi |
| Public SOLD | Chỉ trả tombstone trạng thái để trang cũ hiện Đã bán; không trả tên/ảnh/profile thật | Có thể đổi thành 404 nếu chính sách không muốn lộ cả trạng thái tồn tại |
| Liên hệ giao hàng | Ghép tên/SĐT/địa chỉ vào shippingAddress như checkout cũ; không có address trong hồ sơ | Tách field cần contract/migration riêng |
| Khóa trên SQL Server | H2 concurrent tests đã qua; timeout JPA hint phụ thuộc driver/database | Chạy staging SQL Server và đo hành vi lock/timeout trước production |

## 7. Kiểm tra đã chạy

- Baseline Maven suite trước sửa: PASS.
- Sau backend mua pet: full Maven PASS; sau bổ sung case giá null/0/âm, targeted OrderPetValidationTest PASS.
- Sau UI mua pet: `npx tsc --noEmit`, `npm run lint`, `npm run build`: PASS.
- Sau quick add: full `./mvnw.cmd -q test`: **188 tests, 0 failures, 0 errors, 3 skipped (185 chạy thành công)**. Trong đó 16 test mới gồm PetPurchaseTest 9, OrderPetValidationTest 1, QuickPetCreateTest 6.
- Sau UI quick add: `npx tsc --noEmit`, `npm run lint`, `npm run build`: PASS; 215 modules, build thành công, không warning frontend.
- Các warning backend giống baseline: Open-in-view, cấu hình AuthenticationProvider, JVM agent; test JWT cấu hình sai chủ động phát warning khởi tạo context. Không có loại WARN mới trong log final so với baseline.
- 3 skipped là SqlServerConstraintTest, chỉ bật bằng RUN_SQLSERVER_TESTS=true và Docker/Testcontainers. Chưa chạy integration/lock test trên SQL Server thật; không dùng kết quả H2 để khẳng định tương đương SQL Server.
- Không có frontend test framework; không thêm framework. Chưa chạy thao tác trình duyệt end-to-end, các bước bên dưới cần kiểm tra thủ công.
- `git diff --check`: PASS. So với 44027f8, không thay đổi bất kỳ schema-sqlserver.sql/reset-demo-sqlserver.sql/data-sqlserver.sql/package.json/package-lock.json. tsconfig.app.tsbuildinfo sinh bởi build được khôi phục, không commit.
- Log local (không commit): backend/target/pet-feature-baseline.log, pet-purchase-backend.log, quick-pet-backend.log và target/surefire-reports.
- Commit tách theo yêu cầu, chỉ stage file cụ thể, không thêm dependency, không push/force push.

## 8. Checklist thủ công sau khi áp dụng migration

1. Guest mở pet AVAILABLE giá dương; bấm Đặt mua → login → quay lại chi tiết. Staff/admin không thấy nút.
2. Khách A mở modal mua: tên/SĐT được điền nếu hồ sơ có, địa chỉ nhập tay, COD, ghi chú, checkbox chăm sóc. Kiểm tra Tab ở trong modal, Esc đóng và focus trở về nút mở. Nhấn nhanh hai lần chỉ gửi một request.
3. Tạo đơn thành công → /orders hiển thị pet với tên/ảnh/giá. Mở lại chi tiết pet: Đang được đặt và nút disabled. Khách B gọi trực tiếp POST cùng pet → 409. Với trang B đã tải trước khi A mua, submit → toast xung đột và reload trạng thái.
4. ADMIN/SALES_STAFF lần lượt POST `/orders/{id}/confirm`, `/process`, `/ship`, `/deliver`: pet thuộc A, public detail chỉ hiện Đã bán. Giao lại/hủy đơn đã giao → 409. Trong ví dụ các hậu tố process/ship/deliver đều gắn vào `/api/v1/orders/{id}`.
5. Với đơn pet khác: chủ đơn hoặc ADMIN POST `/api/v1/orders/{id}/cancel`; pet AVAILABLE. Khách khác mua pet đó rồi gọi lại cancel đơn cũ: reservation mới không bị nhả. Mixed order trả tồn kho sản phẩm đúng một lần.
6. Gửi body mua có customerId/userId giả và item.price giả: order vẫn thuộc principal và dùng giá DB. Kiểm tra cả hai ID/không có ID/qty pet khác 1/duplicate pet đều 400; pet không tồn tại 404; unavailable/owned/unpriced 409.
7. Khách mới mở `/services/booking?serviceId=<id thật>`. Chọn ngày/giờ, bấm Thêm nhanh: danh mục Chó/Mèo từ API, nhập tên/tuổi. Lưu → pet được chọn, dịch vụ/ngày/giờ giữ nguyên, đặt lịch thành công. Pet không xuất hiện danh mục bán và public detail trả 404.
8. Dùng DevTools chặn GET my-pets: hiện lỗi + Thử lại, không hiện Bạn chưa có thú cưng. Bỏ chặn rồi retry. Kiểm tra lỗi tải species có retry riêng.
9. Modal quick add: tên trống, tuổi âm/31/thập phân bị chặn; API trả field error hiển thị đúng trường. Vượt giới hạn cấu hình → 409 và modal vẫn cho sửa/đóng. Kiểm tra thêm tại trang Thú cưng của tôi dùng cùng form mới.
10. Đặt một đơn chỉ có sản phẩm qua giỏ hàng/checkout rồi xử lý/hủy như trước; xem list/detail vẫn đúng giá, số lượng và tồn kho.

Lệnh kiểm tra lại:

```powershell
# Trong ppet/backend
.\mvnw.cmd test
# Trong ppet/frontend
npx tsc --noEmit
npm run lint
npm run build
```
