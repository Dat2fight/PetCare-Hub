# Kế hoạch Flyway đã duyệt

Ngày 11/11/2026. Người dùng đã duyệt bằng yêu cầu tiếp tục triển khai giai đoạn 3; triển khai ngày 11–12/11/2026. Nhánh feature/hardening-3-migrations chứa kế hoạch, kiểm kê và phần dọn fixture không dùng còn lại từ B1. Flyway đã được thêm; chưa chạy trên database thật. Kết quả và giới hạn kiểm chứng ở REPORT.md.

Mặc định và dev hiện đều dùng ddl-auto validate. Test H2 dùng create-drop. Vì vậy bước C1 tập trung vào lịch sử migration, baseline và đồng bộ mapping/schema. Testcontainers SQL Server đã có dependency, chưa có Docker tại máy kiểm chứng.

## Đánh phiên bản

Đề xuất thư mục backend/src/main/resources/db/migration. V1 là bản sao nguyên nội dung schema-sqlserver.sql. Các file thủ công được sao chép byte-for-byte, kiểm SHA-256, không chỉnh bản cũ. Có hai file 004 nên ánh xạ theo bảng này để phiên bản Flyway duy nhất.

| Phiên bản | Nguồn hiện có | Đích đề xuất |
|---|---|---|
| V1 | schema-sqlserver.sql | V1__legacy_schema.sql |
| V2 | migrations/001_inventory_nullable_unique.sql | V2__inventory_nullable_unique.sql |
| V3 | migrations/002_cart_product_unique.sql | V3__cart_product_unique.sql |
| V4 | migrations/003_inventory_generated_unique.sql | V4__inventory_generated_unique.sql |
| V5 | migrations/004_orders_paid_at.sql | V5__orders_paid_at.sql |
| V6 | migrations/004_pet_purchase.sql | V6__pet_purchase.sql |
| V7 | migrations/005_order_expiry.sql | V7__order_expiry.sql |
| V8 | migrations/006_checkout_pricing.sql | V8__checkout_pricing.sql |
| V9 | migrations/007_reviews.sql | V9__reviews.sql |
| V10 | migrations/008_wishlist.sql | V10__wishlist.sql |
| V11 | migrations/009_package_orders.sql | V11__package_orders.sql |
| V12 | migrations/010_inventory_actor.sql | V12__inventory_actor.sql |
| V13 | migrations/011_order_shipping.sql | V13__order_shipping.sql |
| V14 | migrations/012_order_timeline.sql | V14__order_timeline.sql |

V15 dự kiến bổ sung các ràng buộc/mapping cần thống nhất sau kiểm thử, không sửa nội dung V1–V14. Nếu cần tách thay đổi thì tăng V16 trở đi.

## Database mới và database có dữ liệu

Database trống chạy V1 đến V14 rồi migration hiệu chỉnh và Hibernate validate. SQL khởi tạo không gồm reset-demo-sqlserver.sql hay seed dữ liệu bán hàng.

Database đã có dữ liệu cần sao lưu và restore ra bản sao để kiểm kê sys.tables/sys.columns/sys.indexes/sys.foreign_keys/sys.check_constraints. Đối chiếu schema trước khi chọn baseline. Đề xuất baseline-version=1 cho DB có đủ cấu trúc V1, rồi cho V2–V14 chạy lại theo tính idempotent của các script. Cách này xử lý được DB đã áp một phần migration thủ công, tránh đánh dấu bỏ qua những migration còn thiếu. Những bản DB bị drift hoặc schema chưa đủ V1 cần script bổ sung riêng và duyệt trước khi baseline.

Nếu xác minh được DB khớp toàn bộ V1–V14, có thể baseline 14 để bỏ qua các script đã áp dụng. Cần bằng chứng schema và dữ liệu cho lựa chọn đó.

baseline-on-migrate mặc định false. Baseline thực hiện một lần bằng lệnh explicit hoặc cấu hình true chỉ trong lượt onboarding đã xác định đúng database, sau đó tắt. Flyway chỉ chạy phiên bản lớn hơn baselineVersion. [Tài liệu baseline](https://documentation.red-gate.com/flyway/reference/commands/baseline), [baseline-on-migrate](https://documentation.red-gate.com/flyway/reference/configuration/flyway-namespace/flyway-baseline-on-migrate-setting).

## Xử lý khác biệt đã ghi nhận

Danh sách đầy đủ các nhóm cột, bảng bổ sung và ràng buộc tại [SCHEMA-INVENTORY.md](SCHEMA-INVENTORY.md).

- Chuỗi Unicode của SQL là NVARCHAR nhưng mapping nhiều chỗ là VARCHAR hoặc TEXT. Ưu tiên mapping phù hợp schema Unicode đã có, kiểm round-trip tiếng Việt trên SQL Server. Nếu database do ORM cũ tạo có VARCHAR/TEXT, bổ sung migration mới để mở rộng sang NVARCHAR sau kiểm dung lượng và index phụ thuộc.
- Giá tiền SQL DECIMAL(18,2) trong khi entity nhiều chỗ precision 10. Ưu tiên mở rộng mapping lên 18 để giữ miền dữ liệu của SQL. Không thu hẹp dữ liệu hiện có.
- URL ảnh SQL dài 500 nhưng mapping 255. Ưu tiên mapping 500. Checkout key/fingerprint SQL 36/64 nhưng mapping 255, sửa annotation theo định dạng thực và validate. Cần kiểm các dữ liệu ngoài định dạng trước khi ràng buộc.
- TEXT ở roles.description và loyalty_transactions.description so với NVARCHAR(255) cần quyết định độ dài. Mặc định mở rộng SQL mới nếu có dữ liệu dài cần giữ, đưa giới hạn thích hợp vào API và mapping.
- DATETIME2(0)/(6)/(7), TIME(0) và nullable audit có khác biệt metadata. Kiểm kiểu JDBC mà Hibernate validate nhận trước, thống nhất độ chính xác theo cột hiện hữu, không chuyển timezone hoặc tự ghi lại lịch sử.
- Giữ user_addresses, pet_images, product_images, deleted_at và các cột SQL chưa mapped.
- Giữ filtered unique, CHECK và FK theo script. Đối chiếu tên và định nghĩa, vì hibernate validate không thay thế kiểm index/constraint.
- Giữ NULL cho mốc paid_at và timeline lịch sử chưa có chứng cứ. Các dòng kho nghi gán ID 1 và các đơn/lịch nghi trùng chỉ được liệt kê bằng truy vấn ở docs/database.md.

## Cấu hình sau duyệt

Thêm flyway-core và flyway-sqlserver dùng phiên bản tương thích BOM Spring Boot hiện tại. SQL Server cần module riêng. [Tài liệu SQL Server](https://documentation.red-gate.com/fd/sql-server-database-277579330.html).

Cấu hình spring.flyway.locations=classpath:db/migration, schema dbo, enabled theo profile, clean-disabled=true, baseline-on-migrate=false. JDBC, username và password đọc từ môi trường hiện có. Runtime giữ Hibernate validate. Test H2 tắt Flyway SQL Server và dùng create-drop. Nếu cần profile local schema tự cập nhật, phải đặt tên tường minh và cảnh báo, chỉ bổ sung khi có nhu cầu thật.

Các script chứa BEGIN TRANSACTION/COMMIT và GO, nên kiểm transaction ownership bằng container. Nếu Flyway bọc transaction gây lệch @@TRANCOUNT, dùng sidecar script config executeInTransaction=false cho các script tự quản lý transaction, giữ nguyên nội dung SQL và checksum.

## Kiểm chứng bắt buộc sau duyệt

1. Container SQL Server mới, Flyway migrate từ trống, ứng dụng validate khởi động.
2. Container thứ hai dựng từ schema cũ và một phần migration, seed qa_ cho user/pet/order/inventory với dữ liệu Unicode. Baseline 1, migrate, so số dòng/khóa/giá trị quan trọng trước sau.
3. Một fixture đã áp đủ migration thủ công, baseline và chạy lại script idempotent. Kiểm @@TRANCOUNT khi thành công và khi cố ý vi phạm constraint.
4. Kiểm unique có NULL, same-customer checkout key, same key khác khách, rollback kho, khóa đồng thời booking, enum/check constraints.
5. Migrate lần nữa không thay dữ liệu, validate checksum thành công, sửa checksum bản sao thử phải thất bại.
6. Tạo integration test tự động cho đường database trống. Test mặc định có thể skip khi thiếu Docker, CI db-checks giai đoạn 4 sẽ bắt buộc chạy.

Hiện Docker không có trong PATH, nên chưa thể gửi kết quả hai đường nâng cấp SQL Server. Các kiểm chứng H2 của giai đoạn 1–2 đã qua, phạm vi khác với kiểm chứng migration.

## Triển khai và hồi phục

Chuẩn bị backup và kiểm restore, lưu snapshot schema/checksum/row counts. Thử bản sao trước, chốt phiên bản baseline, chạy migrate và validate, kiểm catalog, đặt/hủy đơn và lịch, rồi mới mở ghi bình thường. Cần cửa sổ triển khai thống nhất frontend/backend vì DTO mới.

Ưu tiên forward-only khi phát hiện lỗi migration đã commit. Nếu lỗi chưa commit và script có transaction, kiểm rollback cùng log Flyway. Nếu có bước đã commit nhưng không thể khắc phục ngay, dừng ghi, phục hồi backup đã kiểm thử và dùng phiên bản ứng dụng tương thích. Không dùng clean, reset demo hoặc repair checksum để che thay đổi ngoài kiểm soát. Việc phục hồi có thể làm mất ghi sau backup, vì vậy runbook cần giới hạn cửa sổ ghi phù hợp.

Bản gốc schema-sqlserver.sql và migration thủ công được giữ làm tài liệu lịch sử. Sau chuyển đổi, Flyway là đường cập nhật chính, có hướng dẫn trong docs/migrations.md. Reset demo chỉ dùng database demo tách biệt.

## Phạm vi đã duyệt

Duyệt cấu trúc V1–V14, baseline 1 trên DB hiện hữu đủ điều kiện, giữ toàn bộ dữ liệu/bảng ngoài mapping và hiệu chỉnh Unicode/precision theo hướng mở rộng. Sau duyệt sẽ triển khai trên container/bản sao, gửi kết quả trước mọi thao tác trên DB đang dùng.
