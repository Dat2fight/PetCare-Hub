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
| `GET /admin/users` | ADMIN; không được hạ quyền admin hiện hữu |
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

- Register nhận username/email/password/firstName/lastName/phone; login trả `{token,user}`. Không có endpoint refresh/logout server trong inventory này.
- `GET /pets`: query `healthStatus`, `availabilityStatus`, `speciesId`, `breedId`, page/size/sort. Điều kiện owner=NULL và AVAILABLE luôn áp dụng. PublicPetDTO chỉ có id/name/speciesId/speciesName/breedId/breedName/age/birthDate/price/availabilityStatus/description/imageUrl. Detail nhận thêm RESERVED chưa có chủ; SOLD hoặc có chủ 404.
- `/appointments/me` thay cho URL có userId khi xem lịch cá nhân. Alias cũ giữ kiểm quyền. `/notifications` và `/loyalty/account`, `/loyalty/transactions` cũng lấy principal.
- Product checkout nhận `{shippingAddress,paymentMethod:"COD",items:[{productId,quantity}]}`. ShippingAddress hiện gộp người nhận, SĐT và địa chỉ. Price/userId/total từ client không quyết định đơn; petId bị từ chối vì tính năng mua pet ở nhánh riêng chưa tích hợp.
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

