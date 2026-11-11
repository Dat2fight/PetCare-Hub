# Báo cáo hardening giai đoạn 1 và 2

Kiểm chứng 11/11/2026. Nền 43ce3ca. Giai đoạn 1 ở feature/hardening-1-core, giai đoạn 2 kế thừa ở feature/hardening-2-experience.

| Mục | Kết quả | Nguyên nhân và thay đổi | Kiểm chứng | Commit |
|---|---|---|---|---|
| A7 | ĐÃ XONG | Nhiều ngày nghiệp vụ theo máy. Business Clock chung cho booking, audit JPA, ưu đãi, tuổi pet, y tế, báo cáo, JWT/WebSocket, hạn đơn/gói | Clock 17:30 UTC với ba múi giờ JVM, slot sang ngày hôm sau | bddebfa |
| A5 | ĐÃ XONG | Fallback ID 1 ĐÃ ĐƯỢC SỬA TRƯỚC ĐÓ. Bổ sung Ownership, nguồn USER/SYSTEM, tham chiếu ORDER sau flush | Thiếu actor từ chối, đúng actor, đặt/hủy cùng order ID, rollback | 46189e8 |
| A6 | ĐÃ XONG | Public từng trả inactive. Thêm manage riêng ADMIN/SALES_STAFF | List/type/detail, customer 403, staff thấy inactive, booking từ chối inactive | cec85ea |
| A4 | ĐÃ XONG | Chuỗi địa chỉ không đủ kiểm tra. ShippingInfo chuẩn hóa, ShippingForm và lỗi theo field | Emoji, số sai, abc, trống/quá dài nhận 400; đơn tiếng Việt tải lại đúng | 91fcece |
| A1 | ĐÃ XONG | Key/index đã có nhưng key body tùy chọn, reload mất khóa. Header UUID bắt buộc, replay 200, nội dung khác 422, sessionStorage/retry | 3 yêu cầu cùng key một đơn, phạm vi khách, điểm/mã một lần, mất phản hồi sau commit | 44ac967 |
| A2 | ĐÃ XONG | Chỉ khóa staff, pet có thể chiếm hai staff. Khóa pet rồi staff, overlap SQL và slot theo pet | 3 luồng cùng pet/2 staff chỉ 1 thành công; kề nhau, trùng một phần, pet khác, hủy nhả slot | 409313c |
| A3 | ĐÃ ĐƯỢC SỬA TRƯỚC ĐÓ, hoàn thiện | Home đã dùng API. Thêm sort mới nhất, ẩn rỗng, kích thước/fallback ảnh | API unowned AVAILABLE, ID/giá thật, mới nhất đứng trước | c469b6a |
| B1 | ĐÃ XONG | Giá dịch vụ/review/số liệu tĩnh. Thay API, bán chạy từ DELIVERED, siteConfig | Approved, tên viết tắt; fallback mới nhất và bán chạy sau giao hàng trên trình duyệt | 029a85d |
| B2 | ĐÃ XONG | Đặt xong quay về danh sách. Thêm xác nhận, lịch cá nhân, hủy trước 2h, thông báo, ICS | Refresh, 404 sai chủ, hủy lại một notification, sát giờ 409 ở route mới/cũ, tải ICS | 7f12968 |
| B3 | ĐÃ XONG | Quote/phí/điểm đã có. Thêm xem lại, phiên bản báo giá, xác nhận, timeline thực | Giá đổi 409 không tạo đơn/trừ kho, mất phản hồi chỉ một đơn chung, refresh, lưu mốc trạng thái | 188eae1 |

Các test hồi quy đã chạy green. Chưa lưu log red riêng từng mục trước khi sửa. Test cũ đổi fixture/kỳ vọng do header bắt buộc, ShippingInfo, actor kho thật và quy tắc một pet chỉ có một lịch đồng thời.

## Kiểm chứng

- Maven có 251 test, 0 failure, 0 error, 3 skipped. Ba test SQL Server cần Docker, hiện chưa có Docker.
- TypeScript, ESLint, build qua. Build 224 module, chunk ứng dụng khoảng 451 kB, gzip khoảng 144 kB. Chưa có số đo hiệu năng C3.
- EndpointSecurityMatrixTest kiểm public/private, mọi role thật và ownership endpoint mới.
- Chrome headless, Spring Boot test, H2 in-memory riêng cổng 18191. Seed test riêng, tài khoản ngẫu nhiên, đóng tiến trình sau kiểm.
- Mua chung pet/sản phẩm, đổi giá sau quote, mất phản hồi sau lưu, refresh confirmation, đặt lịch, ICS, hủy và nhả slot đều qua. Không lỗi JavaScript.
- Kiểm scrollWidth ở 320/360/390px cho home, checkout review, xác nhận đơn, booking, xác nhận lịch, lịch cá nhân. Không tràn ngang.
- Tệp kiểm chứng/ảnh tạm ở backend/target/hardening-*, gitignore. .worktrees/ có trước nhiệm vụ.

## Lựa chọn kỹ thuật

Khóa hàng users có sẵn serialize checkout theo khách trước tra key. Retry chờ transaction trước commit rồi đọc đơn cũ. Unique index lọc SQL Server vẫn bảo vệ DB. SQL Server chưa chạy nên đây là kết quả transaction H2 cùng phân tích code.

A2 dùng EntityManager PESSIMISTIC_WRITE với timeout cấu hình để khóa pet trước staff. State machine không cho hồi sinh lịch kết thúc hoặc đổi giờ.

PetPurchaseModal chỉ thêm giỏ theo Q4. ShippingForm ở checkout chung. Gói chăm sóc miễn địa chỉ giao. quoteVersion tùy chọn cho caller cũ, frontend luôn gửi. Tiền luôn tính trên server.

Đơn lịch sử dùng shippingAddress và chỉ hiển thị mốc đã có dữ liệu. Dịch vụ trong lịch cũ vẫn đọc tên khi inactive.

## Catalog trang chủ

| Khối trước | Cách xử lý |
|---|---|
| Hero ảnh và giới thiệu | Giữ hình minh họa, điều hướng |
| Pet nổi bật | API /pets, AVAILABLE, mới nhất, tối đa 5 |
| Danh mục | API /categories |
| Giá dịch vụ 150k/200k/300k, Pet Sitting tĩnh | API /services active |
| Bán chạy | /home/products từ DELIVERED, fallback mới nhất |
| Review tên/ngày/lời bình tĩnh, 4.8/5, 2500+ | /home/reviews approved, tên viết tắt, rỗng thì ẩn |
| Giao toàn quốc, khỏe mạnh, hỗ trợ 24/7 | Gỡ lời hứa chưa xác thực |
| Liên hệ | siteConfig từ VITE_SITE_* |
| Nút tải app chưa tồn tại | Gỡ khỏi footer |
| Gợi ý pet | Giữ liên kết tính năng hiện có |

## Migration và cấu hình

Migration mới 010_inventory_actor.sql, 011_order_shipping.sql, 012_order_timeline.sql. Idempotent, cột nullable giữ dữ liệu cũ. Thứ tự và truy vấn đối soát ở docs/database.md. Chưa áp dụng lên SQL Server đang dùng. Triển khai cần sao lưu, thử bản sao rồi cập nhật backend/frontend tương thích.

Biến mới gồm APP_BOOKING_TIMEZONE, APP_BOOKING_LOCKTIMEOUTMS, APP_BOOKING_CANCELLATIONNOTICEHOURS, VITE_BUSINESS_TIMEZONE, VITE_SITE_HOTLINE/ADDRESS/EMAIL/HOURS. Xem .env.example. API chi tiết ở docs/api.md.

## Quét và phần còn lại

- LocalDateTime.now() ở timestamp lỗi/health dùng chẩn đoán.
- mockImages là nguồn ảnh minh họa ở auth, dịch vụ, gói chăm sóc. Home không còn nhập mockImages. Đã xóa mockProducts.ts sau khi xác nhận không có caller.
- Sắp cập nhật ở chính sách, Sắp ra mắt ở khôi phục mật khẩu/social login là phần cũ, thuộc giai đoạn 5.
- Không có href="#" trong frontend source.
- NEEDS DECISION về timezone lịch sử vì default SQL dùng UTC còn LocalDateTime cũ theo máy. Mặc định đọc giờ Việt Nam theo Q2.
- NEEDS DECISION cũ về 20 pet, grooming SALES_STAFF, giá public, ngày sinh ước tính, xóa admin giữ theo quyết định trước.
- Liên hệ doanh nghiệp đang hiển thị Chưa cập nhật.
- Chưa xác minh SQL Server container, Flyway, CI mới, pháp lý, khôi phục mật khẩu hoặc Lighthouse.

Giai đoạn 3 cần duyệt kế hoạch văn bản trước thay công cụ migration. Kế hoạch và đối chiếu metadata gửi riêng để duyệt.
