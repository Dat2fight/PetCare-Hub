# Báo cáo hardening giai đoạn 1 và 2

**Cập nhật mới nhất 12/11/2026:** C1 đã kiểm chứng thành công bằng SQL Server cục bộ tách database test theo lựa chọn mới của người dùng. Toàn bộ 262 test pass, không skip; các phần ghi thiếu Docker/MỘT PHẦN bên dưới là lịch sử trước lần kiểm chứng này. Xem mục cuối và docs/migrations.md.


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

Kế hoạch giai đoạn 3 đã được duyệt bằng yêu cầu tiếp tục triển khai. Kết quả thực hiện được ghi bên dưới.

## Giai đoạn 3 — C1, ngày 12/11/2026

| Mục | Trạng thái | Kết quả và nguyên nhân | Kiểm chứng | Commit |
|---|---|---|---|---|
| C1 cấu hình/history | ĐÃ XONG phần triển khai | Chưa có Flyway; đăng ký V1–V14 không đổi SQL cũ, V15 mở rộng description; baseline mặc định tắt, clean bị cấm, sql.init never | MigrationResourcesTest kiểm toàn bộ bản sao/checksum/sidecar và mặc định an toàn | feat(C1): version SQL Server migrations and align entity mappings |
| C1 mapping | ĐÃ XONG phần triển khai | Unicode TEXT/VARCHAR, tiền precision 10, URL 255, key 255 khác schema; sửa annotation, không cắt dữ liệu | SchemaMappingTest RED trước sửa, GREEN sau sửa; đối chiếu metadata ngoại tuyến các sai lệch không thời gian khớp script sau V15 | cùng commit C1 |
| C1 ddl-auto | ĐÃ ĐƯỢC SỬA TRƯỚC ĐÓ | Default/dev đã validate, báo cáo gốc nói update là lỗi thời. Giữ validate, test create-drop và tắt Flyway | Bộ test H2 qua; không thêm profile update | cùng commit C1 |
| C1 SQL Server | MỘT PHẦN | Đã viết 5 test Flyway integration; máy không có Docker hợp lệ | Lượt opt-in FAIL trước khi khởi động container: Could not find a valid Docker environment; chưa chạy migrate hoặc Hibernate validate trên SQL Server | cùng commit C1 |

Nhánh feature/hardening-3-migrations. Không push/merge/rebase. SQL schema/reset/seed và các migration thủ công cũ không đổi. Không truy cập hoặc mutation database đang dùng. .worktrees/ có trước nhiệm vụ không nằm trong commit.

### Kết quả kiểm thử

- Maven với JDK 17.0.12: 259 test, 251 pass, 0 failure, 0 error, 8 skipped (3 SQL Server cũ + 5 Flyway mới). Log lượt đầy đủ: backend/target/migration-tests.log; số tổng được lưu trong migration-test-counts.txt trước lượt opt-in.
- Lượt riêng RUN_SQLSERVER_TESTS=true, FlywayMigrationTest: 1 lỗi khởi tạo do thiếu Docker, 0 test migration nghiệp vụ đã chạy. Log backend/target/migration-docker.log. Không coi skip hoặc lỗi môi trường này là bằng chứng migration thành công.
- npx tsc --noEmit, npm run lint, npm run build: PASS. 224 module, JS 450.93 kB/gzip 143.68 kB. Không có thay đổi UI, không lặp lại kiểm responsive giai đoạn 2 và chưa đo C3.
- SchemaMappingTest ban đầu fail với các kiểu/độ dài cũ; log red tại backend/target/migration-red.log. Sau sửa green trong bộ đầy đủ. Các artifact target không commit.
- EndpointSecurityMatrixTest cùng toàn bộ kiểm thử bảo mật/ownership đã chạy trong bộ Maven; không thêm endpoint nên allowlist giữ nguyên.

### Phạm vi và cách áp dụng

Dependency Flyway core + sqlserver 10.10.0 theo BOM Spring Boot 3.3.13. V1–V14 sao chép byte-for-byte trong checkout; SHA-256 trong manifest chuẩn hóa LF để không phụ thuộc Git CRLF. Sidecar tắt transaction ngoài của Flyway với script tự BEGIN/COMMIT. V15 chỉ mở rộng roles.description và loyalty_transactions.description sang NVARCHAR(MAX), idempotent.

Mapping: Nationalized cho Unicode và Length.LONG32 cho text dài; precision tiền 18; URL 500; key/fingerprint 36/64; nullable audit/payment theo SQL. Product.weight giữ precision 10 đúng schema. Giữ timestamp/time precision/default/timezone cũ và bảng ngoài ORM; chi tiết tại SCHEMA-INVENTORY.md. Không tự sửa DB do ORM tạo bị drift mà chưa được kiểm kê.

Biến mới ở backend/.env.example: FLYWAY_ENABLED=true, FLYWAY_BASELINE_ON_MIGRATE=false, FLYWAY_BASELINE_VERSION=1. Không đổi API/DTO/role/frontend. Default/dev bật Flyway trước validate; H2 test tắt Flyway. Database có sẵn chưa có history sẽ bị chặn đến khi onboarding có kiểm soát. README, docs/api.md, docs/database.md và docs/migrations.md đã cập nhật; phần giới thiệu database ghi nhầm PostgreSQL được sửa thành SQL Server.

### Việc còn lại và điều kiện hoàn tất

C1 tổng thể là MỘT PHẦN vì chưa có bằng chứng SQL Server. Cần Docker Linux containers hoạt động và chạy lệnh trong docs/migrations.md để kiểm database trống + startup validate, baseline một phần/toàn bộ giữ dữ liệu, Unicode/giá lớn/URL dài, constraints/rollback, checksum và retry sau đối soát. CI bắt buộc DB checks thuộc giai đoạn 4, chưa triển khai trong đợt này.

Trước nâng cấp DB thật: người vận hành sao lưu, kiểm restore, kiểm kê schema và chọn baseline theo runbook, thử bản sao thành công rồi mới chạy lên đích trong cửa sổ dừng ghi. Baseline 1 chỉ dành cho DB đủ V1; baseline 14 chỉ khi đã chứng minh đủ V1–V14. Giữ toàn bộ dữ liệu và đối soát các nghi vấn bằng truy vấn ở docs/database.md; không tự backfill hoặc xóa.

Quét cuối: các LocalDateTime.now() chẩn đoán, mockImages minh họa, nhãn Sắp ra mắt/Sắp cập nhật và NEEDS DECISION lịch sử còn theo phần Quét và phần còn lại ở trên. Không có href="#" trong frontend source. Các bản sao Flyway giữ nguyên comment lịch sử/manual/NEEDS DECISION của nguồn để bảo toàn nội dung; đó không phải chỉ dẫn triển khai hiện hành. Quy trình hiện hành ở docs/migrations.md. Khôi phục mật khẩu, chính sách và SEO vẫn thuộc giai đoạn sau.

Git diff --cached --check báo hai whitespace có sẵn trong V1 (dòng 252 và dòng trống cuối file); giữ nguyên để bảo toàn byte của schema nguồn. Cả 14 blob migration đã stage khớp byte với blob nguồn và SHA-256 trong manifest.

## C1 bổ sung — SQL Server cục bộ thay Docker (12/11/2026)

| Mục | Trạng thái | Thực hiện và kiểm chứng |
|---|---|---|
| Thay môi trường test | ĐÃ XONG | Theo yêu cầu người dùng, dùng SQL Server cài sẵn localhost:1433. Không dùng DB_URL hoặc petcarehub. Container giữ tùy chọn cho CI. |
| Cách ly/dọn DB | ĐÃ XONG | SqlServerTestDatabase tạo DB mới petcarehub_test_<UUID>; guard tên, ownership lượt chạy, database_id và create_date trước DROP; không force-disconnect. 3 test guard pass. |
| Migration và validate | ĐÃ XONG | 5 Flyway test pass: fresh V1–V15 + ứng dụng Hibernate validate; legacy một phần/toàn bộ baseline 1 lên V15 giữ dữ liệu; checksum lỗi chặn; duplicate cart chặn rồi retry sau đối soát. |
| Constraint SQL Server | ĐÃ XONG | 3 test filtered uniqueness/NULL, stock không âm, inventory uniqueness cũ, duplicate cart/transaction pass trên engine SQL Server. |
| Test độc lập | ĐÃ XONG | Lượt đầy đủ phát hiện RevenueRepositoryTest thiếu AuditConfig trong DataJpaTest, phụ thuộc listener của context khác. Bổ sung import AuditConfig và Clock test riêng; không đổi code doanh thu runtime. |
| Kiểm tra cuối | ĐÃ XONG | Script tools/test-sqlserver.ps1 chạy Maven JDK 17.0.12: 262 test, 0 failure, 0 error, 0 skipped. TypeScript, lint, frontend build pass. |

Commit triển khai trước: cd15f74. Phần thay môi trường test, guard, script, fixture auditing và tài liệu nằm trong commit test(C1): verify migrations on isolated local SQL Server databases, cùng nhánh feature/hardening-3-migrations. Không push, merge hay sửa các migration đã commit.

Cấu hình test mới: RUN_SQLSERVER_TESTS, SQLSERVER_TEST_MODE=local, SQLSERVER_TEST_ALLOW_CREATE=true, SQLSERVER_TEST_PORT=1433; tùy chọn SQLSERVER_TEST_USERNAME/PASSWORD (riêng test). Script đặt và khôi phục các biến cùng PATH. Dùng Windows Authentication với Microsoft JDBC auth DLL 12.6.4 x64 đã có trên máy; DLL không commit. Đường dẫn có dấu cách được xử lý qua PATH, không nối vào argLine của Java. Thông tin cụ thể trong .env.example và docs/migrations.md.

Bằng chứng: target/migration-local.log (11 test gồm 8 integration + 3 guard), target/migration-local-full.log (262 test). Các file XML Surefire ghi 5/3 integration test đều không skip. Kiểm metadata master sau cả lượt đầu và lượt đầy đủ đều còn 0 DB test. RevenueRepositoryTest chạy riêng sau đó cũng pass (target/revenue-isolated.log). Dữ liệu thật không được dùng làm fixture hoặc thay đổi. API/DTO, quyền, frontend và datasource runtime không đổi.

Engine kiểm chứng là SQL Server 17.0.1000.7. Flyway 10.10.0 theo BOM cảnh báo phiên bản này mới hơn SQL Server 16 được nhà cung cấp kiểm thử; các luồng nêu trên đã chạy thành công tại đây. Việc nâng cấp dependency để mở rộng phạm vi hỗ trợ cần đợt riêng. Không suy ra database petcarehub thật đã đủ điều kiện baseline: vẫn cần backup/restore bản sao và kiểm kê trước triển khai.

Giai đoạn 3 hoàn tất phần triển khai và kiểm chứng theo phương án SQL Server riêng đã được người dùng chọn. Các test concurrency SQL Server mở rộng và CI bắt buộc còn thuộc giai đoạn 4.
