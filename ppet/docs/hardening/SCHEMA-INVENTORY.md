# Đối chiếu schema trước Flyway

Nguồn là metadata Hibernate 6.5.3 với SQLServerDialect từ entity hiện tại, so với schema-sqlserver.sql và chuỗi migration thủ công đến 012. Phạm vi là source trong repo. Chưa đọc schema database thật và chưa thực thi SQL Server container.

Metadata có 30 bảng, SQL có 33. Phân tích cột trong source không phát hiện cột mapped nào thiếu sau đủ migration. Cần xác nhận bằng migrate + validate trên container ở giai đoạn 3. Bảng dưới liệt kê các khác biệt kiểu/độ dài/precision, không tự kết luận tất cả đều làm Hibernate validate thất bại. NUMERIC và DECIMAL cùng precision được coi tương đương.

| Entity/Hibernate | SQL source | Cột |
|---|---|---|
| datetime2(6) | datetime2(0) | appointments.created_at, appointments.updated_at, care_package_subscriptions.created_at, care_packages.created_at, carts.created_at, carts.updated_at, inventory.updated_at, inventory_transactions.created_at, loyalty_accounts.created_at, loyalty_accounts.updated_at, loyalty_transactions.created_at, medical_records.created_at, notifications.created_at, orders.created_at, orders.updated_at, pet_matching_quizzes.created_at, pets.created_at, pets.updated_at, products.created_at, products.updated_at, promotion_usages.created_at, promotions.created_at, promotions.end_date, promotions.start_date, reviews.created_at, reviews.updated_at, services.created_at, users.created_at, users.updated_at, vaccinations.created_at |
| time | time(0) | appointments.end_time, appointments.start_time |
| TEXT | nvarchar(max) | appointments.notes, breeds.description, care_packages.description, inventory_transactions.notes, medical_records.diagnosis, medical_records.findings, medical_records.recommendations, notifications.data_json, notifications.message, orders.notes, orders.shipping_address, pet_matching_results.explanation, pet_matching_results.score_breakdown_json, pets.description, product_categories.description, products.description, promotions.description, reviews.comment, services.description, species.description, treatments.notes, vaccinations.notes |
| varchar(50) | nvarchar(50) | breeds.avg_lifespan, breeds.energy_level, breeds.grooming_needs, breeds.noise_level, breeds.size_category |
| varchar(100) | nvarchar(100) | breeds.name, inventory.location, orders.recipient_name, orders.province, orders.district, orders.ward, pet_matching_quizzes.activity_level, pet_matching_quizzes.children_age_range, pet_matching_quizzes.energy_preference, pet_matching_quizzes.grooming_tolerance, pet_matching_quizzes.house_size, pet_matching_quizzes.living_environment, pet_matching_quizzes.noise_tolerance, pet_matching_quizzes.pet_experience, pet_matching_quizzes.size_preference, pets.color, pets.name, product_categories.name, products.dimensions, services.name, species.name, treatments.dosage, treatments.frequency, treatments.medication, users.first_name, users.last_name, vaccinations.vaccine_name |
| varchar(255) | nvarchar(255) | care_packages.name, notifications.title, order_items.package_name, orders.promotion_code, products.name, reviews.moderation_state |
| numeric(10,2) | decimal(18,2) | care_packages.price, order_items.subtotal, order_items.unit_price, orders.discount_amount, orders.subtotal, orders.tax_amount, orders.total_amount, pets.price, products.price, promotions.discount_value, promotions.min_order_amount, services.price |
| TEXT | nvarchar(255) | loyalty_transactions.description, roles.description |
| datetime2(6) | datetime2 | orders.cancelled_at, orders.confirmed_at, orders.delivered_at, orders.expired_at, orders.expires_at, orders.paid_at, orders.processing_at, orders.shipped_at, wishlist_items.created_at, wishlist_items.updated_at |
| varchar(255) | varchar(64) | orders.checkout_fingerprint |
| varchar(255) | varchar(36) | orders.checkout_key |
| varchar(10) | nvarchar(10) | orders.delivery_phone |
| varchar(200) | nvarchar(200) | orders.address_line |
| varchar(500) | nvarchar(500) | orders.delivery_note, reviews.moderation_reason |
| varchar(20) | nvarchar(20) | pets.gender |
| varchar(255) | varchar(500) | pets.image_url, products.image_url, users.avatar_url |

## Khác biệt nullable

SQL đặt NOT NULL trong khi entity cho nullable ở appointments.updated_at, carts.updated_at, inventory.updated_at, loyalty_accounts.updated_at, orders.updated_at, pets.updated_at, products.updated_at, reviews.updated_at, users.updated_at và orders.payment_status. Timestamp được JPA audit điền trong luồng ứng dụng. Cần đồng bộ mapping phù hợp dữ liệu hiện hữu.

## Cột và bảng SQL không được ORM ánh xạ

Giữ nguyên các bảng/cột bổ sung dưới đây trong kế hoạch nâng cấp. Chúng có thể chứa dữ liệu hoặc phục vụ caller ngoài JPA.

| Bảng | Cột ngoài mapping |
|---|---|
| roles | created_at, updated_at |
| users | deleted_at |
| user_roles | assigned_at |
| user_addresses | id, user_id, recipient_name, phone, street_address, ward, district, city, is_default, created_at, updated_at, deleted_at |
| species | created_at, updated_at, deleted_at |
| breeds | created_at, updated_at, deleted_at |
| pets | deleted_at |
| pet_images | id, pet_id, image_url, is_primary, display_order, created_at |
| product_categories | created_at, updated_at, deleted_at |
| products | deleted_at |
| product_images | id, product_id, image_url, is_primary, display_order, created_at |
| cart_items | created_at, updated_at |
| orders | deleted_at |
| services | updated_at, deleted_at |
| appointments | deleted_at |
| medical_records | updated_at, deleted_at |
| vaccinations | updated_at, deleted_at |
| treatments | created_at, updated_at, deleted_at |
| reviews | deleted_at |
| promotions | updated_at, deleted_at |
| care_packages | updated_at, deleted_at |
| care_package_subscriptions | updated_at, deleted_at |
| pet_matching_results | created_at |

Ba bảng ngoài ORM là user_addresses, pet_images và product_images. Ngoài ra còn các cột audit/deleted_at liệt kê ở trên. Việc không mapped không đủ cơ sở để xóa dữ liệu.

## Chỉ mục, ràng buộc và thời gian

- Unique username/email, role.name, species.name, product.sku, promotion.code, order_number và customer_id của carts/loyalty_accounts có tương ứng SQL.
- Filtered unique inventory.pet_id/product_id, cart_items(cart_id,product_id), orders(customer_id,checkout_key), wishlist(user,target) và subscription.order_item_id do SQL quản lý. Metadata JPA không khai báo filtered index, phải kiểm riêng sys.indexes, filter_definition và phép INSERT bị trùng.
- Inventory và CartItem đã có comment tránh ORM tạo UNIQUE không lọc trên nullable. Migration 001/003 và 002 giải quyết việc này. Schema từng chạy ddl-auto update có thể còn index tự sinh, cần snapshot sys.indexes trên bản sao.
- FK pets.reserved_order_id, package/beneficiary trên order_items và subscription.order_item_id đến từ migration 004/009. Chỉ mục tham chiếu inventory_transactions đến từ 010.
- CHECK enum và phạm vi gồm pet availability, order item reference/quantity, appointment status, review rating/type, loyalty types, notification types/JSON. Đây là ràng buộc script vượt metadata, cần kiểm bằng truy vấn và thử dữ liệu sai trong container.
- Schema có IX_appointments_pet chỉ theo pet_id. Query overlap mới có thể cần composite index pet_id,appointment_date,status,start_time,end_time sau khi kiểm execution plan. Đây là đề xuất hiệu năng, chưa có số đo để khẳng định cần.
- Nhiều SQL default dùng SYSUTCDATETIME(), trong khi business Clock hiện ghi giờ Việt Nam. Giữ nguyên lịch sử đến khi biết nguồn ghi và có quy tắc đối soát.
- Có index tra cứu email/username/sku cùng cột đã UNIQUE. Có thể dư về hiệu năng, chưa đo chi phí và chưa đề xuất xóa.
- SQL có deleted_at nhưng phần lớn entity chưa triển khai soft delete. Baseline phải giữ cột và ghi nhận nghiệp vụ hiện tại, không tự thay semantics.
