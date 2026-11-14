# PetCare Hub — API và ma trận quyền hiện hành

Đối chiếu code trên `fix/qa-critical-001-007`, base `44027f8`, ngày 27/10/2026. Các endpoint bên dưới dùng tiền tố `/api/v1`. Role thật: `CUSTOMER`, `SALES_STAFF`, `WAREHOUSE_STAFF`, `VETERINARIAN`, `ADMIN`. “Đã đăng nhập” bao gồm mọi role thật nhưng thao tác cá nhân luôn bị giới hạn theo principal.

- 401: thiếu đăng nhập; 403: sai vai trò hoặc userId URL khác principal; 404: tài nguyên riêng không thuộc người gọi; 400: input sai; 409: trạng thái/tồn kho/xung đột dữ liệu.
- Trang mặc định 20, tối đa 100. `GET /orders`, pets/products và notifications trả Page; các danh sách clinical/appointment nhận Pageable nhưng trả mảng giới hạn theo trang.
- JWT chỉ mang subject/thời gian; filter tải tài khoản và quyền hiện tại từ DB. Public register bỏ qua field lạ (kể cả role), luôn CUSTOMER. Admin tạo nhân viên dùng DTO riêng, chỉ SALES_STAFF/WAREHOUSE_STAFF/VETERINARIAN; endpoint role riêng không hạ quyền admin hiện hữu.
- Inventory được lấy trực tiếp từ `EndpointSecurityMatrixTest`; thêm/xóa route mà chưa khai báo chính sách làm test fail. Test registry chỉ rõ ca ownership cho các ID riêng tư. Public catalog IDs không có chủ; các write nhân viên được kiểm ma trận role.

## Endpoint × quyền / sở hữu

| Method và endpoint | Quyền và phạm vi thực tế |
| --- | --- |
| `DELETE /cart` | Đã đăng nhập; chỉ giỏ/item của principal, item lạ 404 |
| `DELETE /cart/items/{itemId}` | Đã đăng nhập; chỉ giỏ/item của principal, item lạ 404 |
| `DELETE /categories/{id}` | ADMIN, SALES_STAFF |
| `DELETE /medical-records/{id}` | VETERINARIAN, ADMIN |
| `DELETE /pets/{id}` | ADMIN, SALES_STAFF |
| `DELETE /products/{id}` | ADMIN, SALES_STAFF |
| `DELETE /promotions/{id}` | ADMIN, SALES_STAFF |
| `DELETE /services/{id}` | ADMIN, SALES_STAFF |
| `DELETE /treatments/{id}` | VETERINARIAN, ADMIN |
| `DELETE /vaccinations/{id}` | VETERINARIAN, ADMIN |
| `GET /admin/dashboard` | ADMIN; không được hạ quyền admin hiện hữu |
| `GET /admin/revenue-report` | ADMIN; không được hạ quyền admin hiện hữu |
| `GET /admin/users` | ADMIN; phân trang tại DB, mặc định 20/tối đa 100 |
| `GET /appointments` | ADMIN xem toàn bộ; SALES_STAFF/VETERINARIAN chỉ lịch được phân công |
| `GET /appointments/me` | Đã đăng nhập; dữ liệu theo principal |
| `GET /appointments/pet/{petId}` | Chủ pet chỉ lịch của mình; VETERINARIAN chỉ lịch được giao; ADMIN toàn bộ |
| `GET /appointments/slots` | Đã đăng nhập; dữ liệu theo principal |
| `GET /appointments/user/{userId}` | Chính userId hoặc ADMIN; alias cũ, userId khác 403 |
| `GET /auth/me` | Đã đăng nhập; dữ liệu theo principal |
| `GET /cart` | Đã đăng nhập; chỉ giỏ/item của principal, item lạ 404 |
| `GET /categories` | Public (GET storefront chỉ dữ liệu công khai) |
| `GET /categories/{id}` | Public (GET storefront chỉ dữ liệu công khai) |
| `GET /health` | Đã đăng nhập; dữ liệu theo principal |
| `GET /inventory/{inventoryId}/transactions` | ADMIN, WAREHOUSE_STAFF |
| `GET /inventory/{productId}` | Đã đăng nhập (quyền đọc tồn kho hiện có) |
| `GET /inventory/low-stock` | ADMIN, WAREHOUSE_STAFF |
| `GET /loyalty/account` | Đã đăng nhập; chỉ điểm/lịch sử của principal |
| `GET /loyalty/account/{accountId}/transactions` | Chủ tài khoản điểm hoặc ADMIN; người khác 404 |
| `GET /loyalty/account/{userId}` | Chính userId hoặc ADMIN; userId khác trả 403 |
| `GET /loyalty/transactions` | Đã đăng nhập; chỉ điểm/lịch sử của principal |
| `GET /medical-records/{id}` | Chủ pet, VETERINARIAN hoặc ADMIN; người khác 404 |
| `GET /medical-records/pet/{petId}` | Chủ pet, VETERINARIAN hoặc ADMIN; người khác 404 |
| `GET /notifications` | Chỉ chủ thông báo, kể cả ADMIN; userId chéo 403, notificationId chéo 404 |
| `GET /notifications/{userId}` | Chỉ chủ thông báo, kể cả ADMIN; userId chéo 403, notificationId chéo 404 |
| `GET /notifications/unread-count` | Chỉ chủ thông báo, kể cả ADMIN; userId chéo 403, notificationId chéo 404 |
| `GET /orders` | Đã đăng nhập; chỉ đơn của principal |
| `GET /orders/{id}` | Chủ đơn, SALES_STAFF hoặc ADMIN; người khác 404 |
| `GET /pets` | Public (GET storefront chỉ dữ liệu công khai) |
| `GET /pets/{id}` | Public (GET storefront chỉ dữ liệu công khai) |
| `GET /pets/my-pets` | Đã đăng nhập; chỉ pet của principal, server giữ owner/price/health/availability |
| `GET /pets/my-pets/{id}` | Chủ pet, VETERINARIAN hoặc ADMIN; người khác 404 |
| `GET /products` | Public (GET storefront chỉ dữ liệu công khai) |
| `GET /products/{id}` | Public (GET storefront chỉ dữ liệu công khai) |
| `GET /products/manage` | ADMIN, SALES_STAFF, WAREHOUSE_STAFF |
| `GET /promotions` | Public (GET storefront chỉ dữ liệu công khai) |
| `GET /promotions/{id}` | Public (GET storefront chỉ dữ liệu công khai) |
| `GET /promotions/validate/{code}` | Public (GET storefront chỉ dữ liệu công khai) |
| `GET /reviews` | Public (GET storefront chỉ dữ liệu công khai) |
| `GET /reviews/{id}` | Public (GET storefront chỉ dữ liệu công khai) |
| `GET /services` | Public (GET storefront chỉ dữ liệu công khai) |
| `GET /services/{id}` | Public (GET storefront chỉ dữ liệu công khai) |
| `GET /species` | Public (GET storefront chỉ dữ liệu công khai) |
| `GET /species/{speciesId}/breeds` | Public (GET storefront chỉ dữ liệu công khai) |
| `GET /treatments/{id}` | Chủ pet, VETERINARIAN hoặc ADMIN; người khác 404 |
| `GET /treatments/record/{recordId}` | Chủ pet, VETERINARIAN hoặc ADMIN; người khác 404 |
| `GET /vaccinations/{id}` | Chủ pet, VETERINARIAN hoặc ADMIN; người khác 404 |
| `GET /vaccinations/pet/{petId}` | Chủ pet, VETERINARIAN hoặc ADMIN; người khác 404 |
| `PATCH /admin/users/{userId}/role` | ADMIN; không được hạ quyền admin hiện hữu |
| `POST /admin/staff` | ADMIN; không được hạ quyền admin hiện hữu |
| `POST /appointments` | Đã đăng nhập; pet phải thuộc principal, không fallback pet khác |
| `POST /auth/login` | Public (GET storefront chỉ dữ liệu công khai) |
| `POST /auth/register` | Public (GET storefront chỉ dữ liệu công khai) |
| `POST /cart/items` | Đã đăng nhập; chỉ giỏ/item của principal, item lạ 404 |
| `POST /categories` | ADMIN, SALES_STAFF |
| `POST /inventory/add` | ADMIN, WAREHOUSE_STAFF |
| `POST /inventory/adjust` | ADMIN, WAREHOUSE_STAFF |
| `POST /inventory/reduce` | ADMIN, WAREHOUSE_STAFF |
| `POST /loyalty/account/{userId}/add` | ADMIN; points 1..1.000.000, khóa số dư và ghi ledger cùng transaction |
| `POST /loyalty/account/{userId}/spend` | ADMIN; points 1..1.000.000, khóa số dư và ghi ledger cùng transaction |
| `POST /loyalty/account/spend` | ADMIN; points 1..1.000.000, khóa số dư và ghi ledger cùng transaction |
| `POST /medical-records` | VETERINARIAN, ADMIN |
| `POST /orders` | CUSTOMER; khách lấy từ principal, giá/tổng do server tính |
| `POST /orders/{id}/cancel` | Chủ đơn hoặc ADMIN; state machine kiểm trạng thái, người khác 404 |
| `POST /orders/{id}/confirm` | ADMIN, SALES_STAFF; state machine kiểm trạng thái |
| `POST /orders/{id}/deliver` | ADMIN, SALES_STAFF; state machine kiểm trạng thái |
| `POST /orders/{id}/process` | ADMIN, SALES_STAFF; state machine kiểm trạng thái |
| `POST /orders/{id}/ship` | ADMIN, SALES_STAFF; state machine kiểm trạng thái |
| `POST /pet-matching/submit` | Đã đăng nhập; quiz của principal, alias userId chéo 403; kết quả PublicPetDTO |
| `POST /pet-matching/submit/{userId}` | Đã đăng nhập; quiz của principal, alias userId chéo 403; kết quả PublicPetDTO |
| `POST /pets` | ADMIN, SALES_STAFF |
| `POST /pets/my-pets` | Đã đăng nhập; chỉ pet của principal, server giữ owner/price/health/availability |
| `POST /products` | ADMIN, SALES_STAFF |
| `POST /promotions` | ADMIN, SALES_STAFF |
| `POST /reviews` | Đã đăng nhập; tác giả từ principal, cần mua/hoàn thành dịch vụ; approved do server |
| `POST /services` | ADMIN, SALES_STAFF |
| `POST /treatments` | VETERINARIAN, ADMIN |
| `POST /vaccinations` | VETERINARIAN, ADMIN |
| `PUT /appointments/{id}/status` | ADMIN hoặc SALES_STAFF/VETERINARIAN được phân công; chủ chỉ hủy lịch tương lai; ngoài phạm vi 404 |
| `PUT /cart/items/{itemId}` | Đã đăng nhập; chỉ giỏ/item của principal, item lạ 404 |
| `PUT /categories/{id}` | ADMIN, SALES_STAFF |
| `PUT /medical-records/{id}` | VETERINARIAN, ADMIN |
| `PUT /notifications/{id}/read` | Chỉ chủ thông báo, kể cả ADMIN; userId chéo 403, notificationId chéo 404 |
| `PUT /notifications/read-all` | Chỉ chủ thông báo, kể cả ADMIN; userId chéo 403, notificationId chéo 404 |
| `PUT /pets/{id}` | ADMIN, SALES_STAFF |
| `PUT /pets/my-pets/{id}` | Đã đăng nhập; chỉ pet của principal, server giữ owner/price/health/availability |
| `PUT /products/{id}` | ADMIN, SALES_STAFF |
| `PUT /promotions/{id}` | ADMIN, SALES_STAFF |
| `PUT /reviews/{id}/approve` | ADMIN |
| `PUT /services/{id}` | ADMIN, SALES_STAFF |
| `PUT /treatments/{id}` | VETERINARIAN, ADMIN |
| `PUT /users/me` | Đã đăng nhập; dữ liệu theo principal |
| `PUT /vaccinations/{id}` | VETERINARIAN, ADMIN |

## Hợp đồng cần lưu ý

- `GET /admin/users?page=0&size=20&sort=id,asc` trả đối tượng Page (`content`, `number`, `size`, `totalElements`, `totalPages`, `first`, `last`), không còn mảng entity. Trang tính từ 0; mặc định 20, tối đa 100. Mỗi dòng chỉ có `id`, `username`, `email`, `firstName`, `lastName`, `enabled`, `roles` (tên vai trò). Sort cho phép các trường đơn này trừ `roles`, thêm `id` để thứ tự ổn định. Không tìm thấy consumer frontend quản trị trong repo; client bên ngoài cần đọc `content` và gửi tham số trang.

- Register nhận username/email/password/firstName/lastName/phone; login trả `{token,user}`. Không có endpoint refresh/logout server trong inventory này.
- `GET /pets`: query `healthStatus`, `availabilityStatus`, `speciesId`, `breedId`, page/size/sort. Điều kiện owner=NULL và AVAILABLE luôn áp dụng. PublicPetDTO chỉ có id/name/speciesId/speciesName/breedId/breedName/age/birthDate/price/availabilityStatus/description/imageUrl. Detail chỉ nhận AVAILABLE/RESERVED chưa có chủ; PENDING/SOLD/UNAVAILABLE, trạng thái khác hoặc có chủ luôn 404. RESERVED không xuất hiện trong danh sách bán.
- `/appointments/me` thay cho URL có userId khi xem lịch cá nhân. Alias cũ giữ kiểm quyền. `/notifications` và `/loyalty/account`, `/loyalty/transactions` cũng lấy principal.
- Product checkout nhận `{shippingAddress,paymentMethod:"COD",items:[{productId,quantity}]}`. ShippingAddress hiện gộp người nhận, SĐT và địa chỉ. Price/userId/total từ client không quyết định đơn. Theo quyết định H-01 phương án A, mua pet trực tuyến đóng: frontend không mở modal mua, DTO/service từ chối petId (kể cả đơn hỗn hợp) mà không thay đổi đơn, kho, giỏ hoặc giữ chỗ. Các đơn pet đã tồn tại vẫn giữ luồng xử lý/hủy/giao hiện có.
- Thành công checkout trả id/status/items/totalPrice/orderNumber/paymentStatus; status và paymentStatus đều PENDING ban đầu. Cập nhật tồn kho, đơn và phần giỏ đã mua cùng transaction. Thanh toán online/giảm giá/đổi điểm/idempotency chưa được thêm. Giao COD chỉ đánh dấu COMPLETED theo state machine hiện có.
- Public GET products luôn active; muốn xem inactive dùng `/products/manage`. Khuyến mãi giữ quyền SALES_STAFF theo base, cần quyết định nếu đổi ADMIN-only.
- API/DTO khác giữ nguyên từ base. Các route `/pets/{id}/medical-records`, `/product-categories`, auth refresh/logout từng ghi trong bản tài liệu cũ không tồn tại trong mapping hiện hành.
- Ngoài controller inventory: Swagger/OpenAPI public; WebSocket handshake public nhưng STOMP CONNECT xác thực JWT và destination theo người dùng. Không cấp secret cho frontend bundle.

Xem [báo cáo sửa lỗi](qa-critical-001-007/REPORT.md), [kiểm tra thủ công](qa-critical-001-007/MANUAL-CHECKS.md) và [cấu hình/rotate](qa-critical-001-007/SECRET-OPERATIONS.md).
# QA high follow-up: personal pet creation and authentication

`GET /admin/dashboard` and `/admin/revenue-report` require ADMIN and accept ISO dates `from`, `to` (inclusive, at most 366 days). Default: first day of the ending month through today in Asia/Ho_Chi_Minh; if `to` is supplied it becomes the ending date. Revenue uses the half-open payment timestamp interval `[from 00:00, to+1 day 00:00)`. Only COMPLETED payment, non-CANCELLED order counts; COD additionally requires DELIVERED. REFUNDED payments and unknown `paid_at` never count. Partial-refund records do not exist in this schema. Dashboard `totalOrders` is lifetime order count; revenue-report `totalOrders` is the count of revenue-qualified orders in the requested period, not lifetime orders or all delivered orders. Monthly periods use the same predicate.

Deployment prerequisite: manually review/apply `migrations/004_orders_paid_at.sql` before deploying the backend. No automatic migration or historical backfill is performed. COD delivery records `paid_at` in business time. Existing COMPLETED records need verified payment timestamps before inclusion; do not copy created_at/updated_at as payment evidence. There is no frontend admin dashboard consumer in this branch.

`POST /api/v1/pets/my-pets` requires CUSTOMER. Send `name` (1–100), `speciesId` from `/species`, and integer `age` (0–30) for quick creation. The existing `birthDate`, `breedId`, `description`, `imageUrl` form remains supported; unknown age/date may be omitted. Identity, price and internal statuses are never accepted from the client. Result is an owned UNAVAILABLE pet; registration count is serialized under the owner lock and capped by `app.pets.max-personal-pets` (default 20), returning 409 at the cap. Invalid species/breed is rejected.

Personal routes including `/auth/me` and `/pets/my-pets/**` require authentication. Filter errors use the same ErrorResponse as controllers, including `correlationId`; 401 message is `Vui lòng đăng nhập`. Invalid bearer tokens receive 401.

Booking requires `petId`, `serviceId`, `appointmentTime`; customer identity is taken from the principal. Foreign, unowned and missing pets return 404. `GET /appointments/pet/{petId}` retains owner/assigned-vet/admin scope. There is no customer rescheduling endpoint or staff-on-behalf booking flow.


Các phản hồi đơn hàng có thêm expiresAt và expiredAt (giờ Việt Nam). Đơn COD chứa thú cưng chờ xác nhận có hạn 24 giờ. Khi hết hạn, trạng thái là CANCELLED và expiredAt có giá trị, khách nhận thông báo trong hộp thư trên web.

POST /api/v1/orders/quote (CUSTOMER) nhận items, promotionCode, pointsToUse. Kết quả gồm subtotal, discount, pointsApplied, pointsDiscount, shippingFee, total, availablePoints, maxRedeemablePoints. POST /orders hỗ trợ checkoutKey UUID cùng phạm vi khách, dùng lại khi gửi lại cùng payload.

POST /cart/items (CUSTOMER) nhận đúng một trong hai trường productId hoặc petId. Với pet, quantity = 1. Lặp lại thao tác thêm cùng pet giữ một dòng trong giỏ. POST /orders nhận cả hai loại dòng trong cùng đơn COD. Cart chỉ xóa những mặt hàng vừa đặt.

### Đánh giá
`GET /reviews` và `/reviews/summary` nhận `targetType` và `targetId`. Khách đã mua hoặc đã dùng dịch vụ kiểm tra `/reviews/eligibility`, đăng đánh giá và quản lý qua `/reviews/me`. Sửa nội dung sẽ chuyển về chờ duyệt. Nhân viên bán hàng và quản trị xem `/reviews/pending`, dùng PUT `/{id}/approve`, `/{id}/reject` (gửi `reason`) hoặc `/{id}/hide` dưới `/reviews`.

`GET /wishlist?page=0&size=12` trả mục yêu thích của khách hiện tại. `POST /wishlist` nhận `{type,targetId}`, gọi lại trả cùng ID. `GET /wishlist/status?type=PET&ids=1,2` tra cứu theo lô. `DELETE /wishlist/{id}` dùng ID mục đã lưu.

### Gói chăm sóc

| Quyền | API dưới /api/v1 |
|---|---|
| Công khai | `GET /care-packages`, `GET /care-packages/{id}` trả gói đang bán |
| CUSTOMER | `POST /orders/quote`, `POST /orders` nhận đúng một dòng `{carePackageId, beneficiaryPetId, quantity: 1}`. Giá lấy từ DB. Giao diện gửi UUID `checkoutKey` để có thể thử lại. Đơn gói dùng COD, phí vận chuyển và ưu đãi bằng 0. |
| CUSTOMER | `GET /care-packages/subscriptions/me` trả quyền lợi, tên thú cưng, ngày hiệu lực, ngày cuối được sử dụng và mã đơn nếu có |
| ADMIN, SALES_STAFF | `GET /care-packages/manage` xem cả gói đã tắt. `POST /care-packages`, `PUT /care-packages/{id}` nhận name, description, price, durationMonths, includesGrooming, includesCheckup, includesVaccination, active. `DELETE /care-packages/{id}` ngừng bán. |

Khi chuyển đơn COD sang DELIVERED, backend ghi nhận thu tiền và kích hoạt trong cùng giao dịch. Gửi lại lệnh giao đơn gói đã thu tiền trả về đơn hiện tại. Đơn đã hủy không thể kích hoạt. Dòng đơn trả thêm `carePackageId`, `beneficiaryPetId`, `packageName`.

## Đặt hàng và lịch hẹn

`POST /api/v1/orders` yêu cầu header `Idempotency-Key` dạng UUID. Gửi lại cùng nội dung và tài khoản trả đơn cũ với `Idempotent-Replay: true`. Dùng lại khóa cho nội dung khác nhận 422. Checkout sản phẩm và thú cưng nhận đối tượng `shipping` gồm `recipientName`, `phone`, `province`, `district`, `ward`, `addressLine` và `note` tùy chọn. Lỗi dữ liệu trả 400 và `fieldErrors`. Gói chăm sóc được miễn địa chỉ giao. Đơn cũ tiếp tục trả `shippingAddress`.

`GET /api/v1/services` và chi tiết công khai chỉ hiển thị dịch vụ đang hoạt động. ADMIN và SALES_STAFF dùng `GET /api/v1/services/manage?active=false` để tra cứu dịch vụ ngừng.

`GET /api/v1/appointments/slots?date=YYYY-MM-DD&serviceId=…&petId=…` loại thời gian trùng của thú cưng thuộc người gọi. Khi đặt, hệ thống khóa thú cưng trước nhân viên. Hai lịch kề nhau được phép. Lịch đã kết thúc, đã hủy hoặc vắng mặt giải phóng khung giờ.

## Hành trình sau mua

- POST /orders/quote trả lines và quoteVersion. Checkout gửi lại quoteVersion để phát hiện giá hoặc ưu đãi/phí giao thay đổi, nhận 409 và xem báo giá mới. Backend luôn tự tính tiền. Trường phiên bản tùy chọn để giữ tương thích caller cũ, giao diện checkout luôn gửi.
- GET /orders/{id} và danh sách đơn thêm confirmedAt, processingAt, shippedAt, deliveredAt, cancelledAt. Đơn lịch sử có thể để trống mốc chưa từng ghi. Trang /orders/:id/confirmation tải đơn thật, refresh được.
- GET /home/products công khai, tối đa 6 sản phẩm active, xếp theo lượng hàng trong đơn DELIVERED. bestSelling=false khi dùng mới nhất.
- GET /home/reviews công khai, tối đa 3 đánh giá approved, tên viết tắt, bỏ ID người viết và email.
- GET /appointments/{id} chỉ chủ lịch, sai chủ trả 404. DTO thêm tên dịch vụ/thú cưng/nhân viên, giá, mã LH, timezone, startsAt/endsAt dạng instant, cancelUntil, cancellable.
- GET /appointments/me?filter=upcoming|past|cancelled&page=0&size=10 trả Page. Thiếu filter giữ response mảng cũ.
- POST /appointments/{id}/cancel nhận reason tùy chọn. Chủ lịch được hủy trước hạn server tính. Hủy lại trả cùng kết quả. Chỉ tạo một thông báo. PUT status cũ dùng cùng chính sách cho khách.
- APP_BOOKING_CANCELLATIONNOTICEHOURS=2 cấu hình hạn hủy. Mốc server được dùng để tạo ICS trên trình duyệt.

## Giai đoạn 3 — migration có phiên bản

Không thêm endpoint, đổi DTO hoặc quyền truy cập. Backend bật Flyway trước Hibernate validate; database cũ chưa baseline sẽ bị chặn khởi động. Cần hoàn thành [onboarding database](migrations.md) trước khi triển khai backend. Chuỗi Unicode, tiền decimal(18,2), URL 500 ký tự và idempotency key được ánh xạ theo schema SQL Server; các giới hạn API hiện có vẫn áp dụng.

Kiểm chứng C1 bổ sung dùng SQL Server cục bộ tách database test, không thay endpoint/DTO/quyền hay datasource runtime. Hướng dẫn tại [migration](migrations.md).


## Admin foundation (2026-11-13)
All paths below have prefix `/api/v1/admin`. Anonymous: 401; unauthorized roles: 403. URL and method guards use PermissionService.

| Method/path | Permission | Role |
|---|---|---|
| GET /me/permissions | PORTAL_ACCESS | All four internal roles |
| GET /dashboard, /revenue-report | REPORT_VIEW | ADMIN |
| GET /users | USER_MANAGE | ADMIN |
| POST /staff | USER_MANAGE | ADMIN |
| PATCH /users/{userId}/role, /users/{userId}/status | USER_MANAGE | ADMIN |
| DELETE /users/{userId} | USER_MANAGE | ADMIN |
| POST /users/{userId}/revoke-sessions | USER_MANAGE | ADMIN |
| GET /users/{userId}/overview | USER_MANAGE | ADMIN |
| GET /audit-logs | AUDIT_VIEW | ADMIN |

Users: keyword, role, status=ACTIVE/LOCKED/DELETED/ALL, page/size (max 100), sort whitelist. Role writes now accept JSON {role,reason,currentPassword}; status {enabled,reason,currentPassword}; delete/revoke {reason,currentPassword}. Staff accepts username/email/firstName/lastName/phone/role; returns {user,temporaryPassword} once with Cache-Control:no-store. Client password is ignored. User DTO adds deletedAt/mustChangePassword. Overview independently pages ordersPage/appointmentsPage/petsPage, size max100. Audit filters actor/action/targetType/target/from/to.

POST /api/v1/auth/password accepts {currentPassword,newPassword}; returns 204 and revokes tokens. Auth user DTO adds mustChangePassword. JWT requires tv claim. See admin-guide.md for permission grants, safeguards and audit. Legacy business URLs retain previous policies until their phase is migrated.

## Admin operations — giai đoạn 2 (2026-11-13)

Tiền tố `/api/v1/admin`. ADMIN được tất cả; anonymous 401, CUSTOMER 403. URL guard và method guard dùng cùng PermissionService. Staff phải hoàn thành đổi mật khẩu tạm.

| Endpoint | SALES_STAFF | WAREHOUSE_STAFF | VETERINARIAN | Permission |
|---|---|---|---|---|
| GET /orders; GET /orders/{id}; GET /orders/{id}/print | Có | 403 | 403 | ORDER_MANAGE |
| GET /orders/{id}/notes; GET /orders/{id}/payments | Có | 403 | 403 | ORDER_MANAGE |
| POST /orders/{id}/transition; /request-refund; /mark-refunded; /notes | Có | 403 | 403 | ORDER_MANAGE |
| GET /inventory; GET /inventory/transactions; POST /inventory/movements | Có | Có | 403 | INVENTORY_MANAGE |
| GET /appointments; GET /appointments/{id}; POST /appointments/{id}/status | Chỉ được giao | 403 | Chỉ được giao | APPOINTMENT_MANAGE hoặc ASSIGNED_APPOINTMENT_MANAGE |
| GET /appointments/{id}/eligible-staff; POST /appointments/{id}/assign | 403 | 403 | 403 | APPOINTMENT_MANAGE (ADMIN) |
| GET /reviews; GET /reviews/count; POST /reviews/{id}/moderate | Có | 403 | 403 | REVIEW_MANAGE |

Mọi danh sách trả Page (`content,totalElements,number,size,…`), mặc định 20, tối đa 100. `sort` ngoài whitelist trả 400; ID giảm dần là khóa phụ ổn định. Khoảng ngày đóng hai đầu theo giờ nghiệp vụ. Keyword tối đa 100 ký tự, LIKE escape %, _, [, !; giá trị client dùng bind parameters.

| Danh sách | Bộ lọc | Sort cho phép |
|---|---|---|
| orders | status, payment, keyword (mã/tên khách), from, to, refunds=true | id, createdAt, total, status |
| inventory | keyword (tên/SKU), low=true | id, quantity, name |
| inventory/transactions | productId, type, actor, from, to | id, createdAt |
| appointments | staff, serviceId, status, from, to, unassigned=true | id, date, status |
| reviews | status=PENDING mặc định (hoặc APPROVED/REJECTED/HIDDEN/ALL), type=PRODUCT/PET/SERVICE, target | id, createdAt, rating |
| notes, payments, eligible-staff | page, size | id |

- `transition`: `{action:"confirm|process|ship|deliver|cancel",reason?}`. Cancel cần reason; dùng state machine hiện tại, sai trạng thái 409. Chỉ hủy PENDING/CONFIRMED/PROCESSING; hủy đơn hỗn hợp nhả toàn bộ pet, kho, lượt mã và điểm đã dùng.
- `request-refund`: `{body:"lý do"}` (1–2000 ký tự), chỉ COMPLETED + DELIVERED/CANCELLED. Tạo yêu cầu đối soát toàn bộ đơn.
- `mark-refunded`: `{reference:"biên nhận",reason:"ghi chú"}` (tối đa 100/500 ký tự), chỉ đã thu tiền và đã yêu cầu hoàn hoặc đã hủy. Retry cùng biên nhận không tạo giao dịch tiền trùng; khác biên nhận sau khi hoàn trả 409. Không nhận số tiền từ client, không hoàn một phần. Không gọi cổng, hoàn kho, chuyển chủ, thay đổi điểm đã kiếm hoặc hủy gói khi đánh dấu hoàn.
- `notes`: `{body:"nội dung"}`. Không có ghi chú nội bộ trong DTO khách hoặc phiếu in. Order detail/print trả `{order,total,taxAmount,paidAt,refundRequestedAt,refundedAt,refundReference}`; `order` là DTO an toàn hiện có. `/payments` là sổ thu/hoàn tiền mặt, không phải dữ liệu cổng.
- `movements`: `{productId,type:"STOCK_IN|STOCK_OUT|ADJUSTMENT",quantity,reason}`. Nhập/xuất >0; kiểm kê là số tồn mới >=0; lý do bắt buộc. Xuất vượt tồn 409. Actor lấy từ phiên đăng nhập; lịch sử có source, referenceType/referenceId, actorId.
- `assign`: `{staffId,reason}`. Chỉ lịch tương lai SCHEDULED/CONFIRMED, nhân viên active/chưa xóa/đúng role dịch vụ. Khóa pet → nhân viên → lịch, kiểm trùng nhân viên và pet. Sai role/khóa 400, trùng 409.
- `status`: `{status,reason?}`. Tái dùng state machine, sai 409, hủy cần lý do và tạo thông báo web. Nhân viên sai phạm vi ID hoặc lọc staff khác trả 404. Không thông báo email.
- `moderate`: `{action:"APPROVED|REJECTED|HIDDEN",reason?}`. Reject cần reason. APPROVED cũng dùng hiện lại review đã ẩn. GET /reviews/count trả `{pending:n}`. API review/summary công khai chỉ tính approved.

Các POST thành công trả 200; transition trả OrderDTO, POST còn lại body rỗng. Chi tiết audit và vận hành ở [admin-guide](admin-guide.md). URL nghiệp vụ cũ vẫn tồn tại với chính sách cũ; portal mới dùng toàn bộ URL admin kể trên để ghi audit.

## Admin catalog — giai đoạn 3 (2026-11-15)

Tiền tố `/api/v1/admin`. JSON thuần; lỗi 400/403/404/409 có correlationId như giai đoạn 1. Danh sách trả Page `{content,number,size,totalElements}`, tối đa 100 dòng, sort theo whitelist; SQL bind mọi giá trị. Anonymous 401, CUSTOMER 403. Ma trận đầy đủ ở [báo cáo giai đoạn 3](admin-phase-3-report.md).

### Danh mục dùng chung

`GET /catalog/{resource}`, `GET /catalog/{resource}/{id}`, `POST /catalog/{resource}`, `PUT /catalog/{resource}/{id}`, `DELETE /catalog/{resource}/{id}`. Resource: products/categories/pets/species/breeds/services/promotions/packages. POST/PUT là form đầy đủ, không bind entity; field ngoài whitelist bị 400. DELETE body `{reason}` bắt buộc; có tham chiếu thì 409, dùng active=false khi phù hợp. Filter `keyword` (tên, riêng promotions tìm code), `active` cho loại có active. Sort id/name, price nếu có; categories thêm displayOrder; promotions dùng code.

| Resource | Các trường form |
|---|---|
| products | name, description, price>=0, sku, categoryId, imageUrl, active, reorderPoint nullable (mặc định 10); không có quantity |
| categories | name, description, parentId nullable, displayOrder>=0; chống vòng cha-con |
| pets | name, description, price>0, speciesId, breedId nullable, birthDate không tương lai, imageUrl, healthStatus, availabilityStatus AVAILABLE/UNAVAILABLE |
| species | name, description |
| breeds | name, description, speciesId |
| services | name, description, price>=0, durationMinutes 5..480, serviceType VETERINARY/GROOMING, active |
| promotions | code, description, discountType PERCENTAGE/FIXED_AMOUNT, discountValue, minOrderAmount, maxUses nullable, maxUsesPerCustomer, startDate, endDate, active |
| packages | name, description, price>0, durationMonths 1..120, includesGrooming/includesCheckup/includesVaccination, active |

Ảnh optional HTTPS thuộc APP_CATALOG_IMAGE_HOSTS, tối đa 500 ký tự; không download qua backend. Thời gian mã theo giờ cửa hàng, ISO local datetime; from<to, % từ 1..100. Mã uppercase, duy nhất không phân biệt hoa thường, áp dụng tiền **sản phẩm**. Mã đã dùng chỉ gia hạn/tắt, không bật lại hoặc sửa giá trị/phạm vi. Pet có chủ/từng gắn đơn bị chặn chỉnh sửa/xóa trực tiếp; danh mục bán chỉ owner IS NULL.

### Kho thấp, bulk và CSV

- `GET /catalog/products/low-stock`: `{count,quantity}` tổng hợp SQL các sản phẩm active có quantity<=COALESCE(reorderPoint,10).
- `POST /catalog/products/bulk-preview`, `/bulk-apply`: `{ids:[...],action:ACTIVE|PRICE_AMOUNT|PRICE_PERCENT,active,value,previewToken?}`. 1..100 ID duy nhất; % trong -100..100; preview trả rows before/after và previewToken. Apply bắt buộc token khớp dữ liệu hiện tại; một transaction, một audit có IDs. Không sửa tồn kho.
- `POST /catalog/products/csv-preview`, `/csv-apply`: `{csv,previewToken?}`. UTF-8 tối đa 200000 byte, 1..200 dòng dữ liệu. Header `sku,name,price,categoryId,imageUrl,description`; hỗ trợ quoted comma/newline/double quote và BOM. Báo lỗi từng dòng; chặn ô công thức; SKU hiện có cập nhật, SKU mới tạo inventory=0. SKU không phân biệt hoa thường; giữ active/ngưỡng của SKU cũ. Apply kiểm lại token/toàn bộ dữ liệu, lỗi thì không nhập dòng nào.

### Thú cưng và lịch

- `POST /catalog/pets/{id}/release-reservation` `{reason}`: khóa đơn rồi pet, gọi luồng hủy hiện có cho **toàn bộ đơn hỗn hợp**, hoàn tài nguyên, ghi order note, thông báo web và một audit.
- `GET /pet-support/{id}`: chỉ đọc, mỗi lần xem audit; ADMIN hoặc bác sĩ có lịch với pet; ngoài phạm vi 404.
- `GET/POST /schedules/{kind}`, `PUT/DELETE /schedules/{kind}/{id}`; kind hours/holidays/shifts/time-off. GET filter staff, from/to (ngày nghỉ), sort id/weekday/workDate. POST/PUT `{weekday?,workDate?,staffId?,startTime?,endTime?,closed?,reason}`. Weekday 1=Monday..7=Sunday; hours/shifts lặp tuần; holidays/time-off ngày cụ thể; holidays nghỉ cả ngày, time-off có khoảng giờ. DELETE `{reason}`. Không sửa ngày quá khứ.
- `GET /schedule-affected?date=YYYY-MM-DD&staff=` liệt kê lịch còn hoạt động của ngày/nhân viên để admin lựa chọn; không tự hủy khi lưu cấu hình. Với nghỉ một phần ngày, admin đối chiếu giờ và chọn đúng lịch.
- `POST /schedule-cancel` `{date,ids,reason}`: 1..100 lịch cùng ngày, kiểm state machine, hủy nguyên transaction và thông báo từng khách.
- `GET /staff-performance?from=&to=`: kỳ tối đa 366 ngày; SQL group by nhân viên; total/completed/cancelled/noShow, phân trang. UI tính tỷ lệ từ số đếm, không tải toàn bộ lịch.

### Loyalty, đăng ký gói và chiến dịch

- `GET/PUT /loyalty-rules`: `{earnEvery,pointValue,silver,gold,platinum}`; tỷ lệ dương, ngưỡng tăng dần. DTO khách `/loyalty/account` thêm earnEvery/pointValue, thresholds cũng từ cùng bảng. Đơn mới snapshot earnEvery; đổi cấu hình không đổi điểm tích của đơn đã tạo.
- `POST /loyalty/{userId}/adjust` `{points,reason}`: points khác 0, -1000000..1000000, không âm số dư; khóa User chung với checkout/loyalty. Ledger ADJUST/MANUAL_ADJUSTMENT đúng dấu; không tăng lifetime/rank do điều chỉnh.
- `GET /promotion-usages/{id}`: các đơn dùng mã, phân trang.
- `GET /subscriptions?packageId=`: danh sách khách/pet/ngày/trạng thái; dùng packageName snapshot khi có. ACTIVE đã qua endDate hiển thị EXPIRED; hạn mức chưa có dữ liệu không suy thành unlimited.
- `POST /subscriptions/{id}` `{action:cancel|extend,endDate?,reason}`: không gia hạn gói đã hủy, ngày mới phải dài hơn hạn cũ, không quá khứ hoặc quá 10 năm.
- `GET /campaign-audience?group=BIRTHDAY|INACTIVE&days=90`: count,limit=500,overLimit,audienceToken. BIRTHDAY theo tháng sinh pet có chủ; INACTIVE là khách có tài khoản cũ hơn days và không tạo đơn trong khoảng đó.
- `POST /campaigns` `{group,days,requestKey:UUID,audienceToken,promotion:{...},message}`: kiểm lại danh sách, tối đa 500 khách, 1 lần/10 phút/người gửi; tạo code riêng + membership audience + thông báo web cùng transaction. Retry cùng key/người/nội dung trả kết quả cũ, không gửi lại; khác nội dung hoặc quá tần suất 409. Checkout kiểm membership cả quote và đặt đơn. Không gửi email/SMS.

API nghiệp vụ cũ giữ tương thích và guard cũ. Portal dùng các endpoint admin trên để ghi audit. Không coi URL legacy là đường thay thế có cùng validation/audit mới.
