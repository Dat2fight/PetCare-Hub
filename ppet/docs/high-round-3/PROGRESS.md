# High lượt 3 — tiến độ (2026-10-19)

## Bước 0
- Git sạch, nhánh `fix/critical-jwt-secret`, HEAD `0c8560f`; đã đọc status/branch/log80, đủ commit Medium/High1/Critical1/High2. Không tạo nhánh hoặc thao tác stash/push/history.
- Baseline Maven: 146 tổng, 143 pass, 0 fail/error, 3 skip SQL Server opt-in. `npm run build` pass; đã restore buildinfo. Java 25 host dùng `-Dnet.bytebuddy.experimental=true`, target Java 17, test profile H2. Log `backend/high-3-baseline.log` ignored.
- Đã đọc báo cáo các lượt và diff những file liên quan từ b4286c6, tìm theo class/hàm hiện tại.

## Bản đồ hiện trạng
| File/nhóm | Hành vi giữ nguyên |
|---|---|
| Ownership | userId typed enabled principal, requireUser ADMIN override, ownedPet strict owner, requirePet ADMIN/VET. Không thêm helper/role trùng |
| OrderServiceImpl/OrderController/DTO/state/order/OrderRepository | Medium đã persistence/transaction, giá server, kho atomic, khóa đơn, state transitions, hoàn kho một lần, Page theo principal. Thiếu GET detail. Doanh thu #45 payment COMPLETED và không CANCELLED theo createdAt; COD hoàn tất khi giao |
| InventoryServiceImpl/ProductServiceImpl | #21 khóa Inventory + audit cùng transaction; #22 tạo product/inventory cùng transaction. Không sửa |
| CartServiceImpl | #21 khóa user trước đọc/tạo cart; snapshot stock đọc trước khóa user. Unique cart/product qua migration002. Update/remove/clear vẫn TODO |
| Checkout/Tracking/Orders/Cart pages, App | Checkout đã gọi API nhưng chưa gửi paymentMethod/đi chi tiết; tracking alias OrdersPage; Cart còn mock và giảm cố định. #52 /orders, protected routes, catch-all, BrowserRouter basename giữ nguyên |
| Promotion DTO/service/controller | DTO tên khác entity; BeanUtils làm mất discountType/value/active/maxUses. Write ADMIN/SALES_STAFF #4 giữ nguyên; thêm validation theo #26 |
| Treatment DTO/service/controller | Đã CRUD transaction, medicine->medication, frequency/startDate/endDate, validate ngày, ownership đọc/xóa; write VET/ADMIN. Giữ contract và bổ sung kiểm tra dữ liệu cũ trước update |
| Matching service/controller | @Transactional và mapper quiz #38 đã có; controller chọn userId principal. Save result thiếu rank trước sort. Giữ mapper/strategies |
| client.ts/useWebSocket/WebConfig/WebSocketConfig/deploy.yml | HTTP /api/v1, SockJS /ws; CONNECT Bearer, user queue và origins property #7. #52 npm ci + copy404 giữ nguyên; dev proxy8081 |
| pom/RegisterRequest | Boot3.3.5, BCrypt, password8..72 ký tự; #24 nâng sau cùng, bổ sung giới hạn UTF-8 bytes để khớp BCrypt. Không đổi JWT logic/default secret |

## Còn lại
Triển khai từng ID #13, #15, #14, #16, #18, #17, #19, #23, #24; commit riêng và test/build sau từng nhóm. Báo cáo cuối ghi contract, ma trận quyền và phần SQL/deploy chưa kiểm chứng.

## Đã hoàn tất
- #13: GET /orders/{id} với owner/ADMIN/SALES_STAFF; DTO thêm chi tiết giao hàng/thanh toán/dòng hàng. paymentMethod COD (legacy thiếu vẫn COD), phương thức khác 400. Giữ transaction/giá server/kho/state/revenue Medium. OrderFlowTest 5 test pass. Full Maven 151 = 148 pass, 3 skip, 0 lỗi; build pass, đã restore buildinfo.
- #15: update/remove/clear transactional, khóa user dùng lại #21, item ngoài giỏ trả 404, quantity/tồn kho sai trả 400. CartMutationTest 4 test gồm cạnh tranh update/add. Full Maven 155 = 152 pass, 3 skip; build pass, đã restore buildinfo.
- #14: checkout gửi COD + địa chỉ, chặn double submit, chỉ chuyển /orders/{id} sau thành công; tracking tải GET thật, lỗi/404/403/retry và hủy đơn. JUnit checkout POST→GET và địa chỉ trống. Maven 156 = 153 pass, 3 skip; build pass, restore buildinfo.
