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
