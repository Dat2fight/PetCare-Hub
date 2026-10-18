# High lượt 2 — #20, #21, #12, #22

Ngày thực hiện: 2026-10-18. Nhánh: `fix/critical-jwt-secret`. Mốc đầu: `5c6dff2`.

## 1. Bản đồ hiện trạng và tránh xung đột

Đã chạy `git status --short`, `git branch --show-current`, `git log --oneline -n 60`: working tree sạch, đúng nhánh, có các commit High lượt 1 và Critical #1. Tiếp tục trên nhánh này; không tạo nhánh, push, rebase, sửa lịch sử hay pop/drop stash. Stash cũ vẫn giữ nguyên.

Đã đọc `docs/audit-25-58/REPORT.md`, `docs/high-round-1/REPORT.md` (gồm ma trận quyền), `docs/critical-jwt-secret/REPORT.md`, mã hiện hành và diff từ `b4286c6` tới HEAD ở các file liên quan. Các mốc đối chiếu chính: `06015d0` (#25/#40–43), `11cb032` (#28–32), `cfe10c5` (#55), `94088d6`/`1f3304f` (#5/#6), `30a352f`/`cbc7cad` (#8/#9). Đọc lại file hiện hành trước khi sửa.

Đường dẫn Java trong bảng dưới tính từ `backend/src/main/java/com/petcarehub/`.

| File/nhóm | Hiện trạng trước lượt này | Cách giữ hành vi cũ |
|---|---|---|
| `security/Ownership` | `userId`, `authorizedUserId`, `requireUser`, `ownedPet`, `requirePet`, `hasRole`; typed enabled principal; ownedPet không có ADMIN override | Không sửa; giữ quyền controller/service đã có, không tạo helper hoặc role mới |
| `service/impl/AppointmentServiceImpl` | #28 dùng BookingProperties, thời gian tương lai, giờ mở/đóng, duration 5..480; #29 enabled/chuyên môn; #30 chọn staff theo ID và khóa trước overlap; #8/#9 ownership; #32 state machine/409 | Giữ nguyên production code; thêm test tích hợp tải đồng thời, HTTP 409 và slot/hủy |
| `repository/UserRepository`, `AppointmentRepository` | `findLockedById` PESSIMISTIC_WRITE; eligible staff sắp ID; query overlap chỉ SCHEDULED/CONFIRMED/IN_PROGRESS; lịch theo pet+customer | Tái sử dụng khóa user cho loyalty/cart; không đổi truy vấn lịch hoặc quyền |
| `service/impl/InventoryServiceImpl`, `repository/InventoryRepository` | #25 add/reduce/adjust đã khóa Inventory trong transaction; validate, overflow, nhật ký; findByProductIdIn phục vụ #43 | Không sửa; thêm test nhập/nhập và nhập/xuất đồng thời, giữ test oversell cũ |
| `service/impl/LoyaltyServiceImpl` | #5 controller ownership; #6 điểm 1..1.000.000, overflow; #47 tier/threshold/history. Còn read-modify-write không khóa | Khóa user trước get/create account; thêm transaction cho GET có thể tạo account. Không đổi validation, tier, DTO, controller |
| `service/impl/CartServiceImpl`, `entity/CartItem` | Add đã validate active/tổng lượng, nhưng chưa khóa và thiếu unique cart/product. Update/remove/clear là TODO #15 | Chỉ sửa add: khóa user trước đọc/tạo cart, migration unique; không triển khai các TODO |
| `service/impl/ProductServiceImpl` | Medium đã tạo Inventory=0 cùng transaction, gọi InventoryService.addStock khi có tồn đầu; #40 update cấm stockQuantity, validate category; #43 batch-load kho | Không sửa production code; kiểm thử persistence/rollback/nhập tiếp và quyền/validation HTTP |
| `entity/Inventory` | OneToOne + unique trên hai khóa nullable có thể tái tạo UNIQUE không lọc | ManyToOne về mặt ORM, vẫn duy nhất về nghiệp vụ bằng filtered indexes do SQL quản lý; giữ field, FK và DTO |
| SQL schema/data/migrations | #55 schema idempotent, reset tách riêng có guard; 001 đã sửa UNIQUE nullable tên cố định. Schema gốc vẫn tạo UNIQUE nullable, chưa có unique giỏ | Giữ 001/reset và dữ liệu seed; sửa schema tạo mới, thêm 002/003; seed chỉ thêm comment thứ tự chạy |
| JWT/profile | Critical #1 bắt buộc JWT_SECRET; test config ở test resources; không default dev | Không sửa JwtTokenProvider, security, application*.yml hoặc cấu hình test |

GlobalExceptionHandler 400/401/403/404/409, RoleType và contract frontend được giữ nguyên. Ba test cũ chỉ cập nhật stub `findLockedById`, không xóa assertion hoặc hạ yêu cầu kiểm thử.

## 2. Kết quả từng lỗi

| ID | Trước khi sửa | Kết quả | Commit | File chính | Test thêm/mở rộng |
|---|---|---|---|---|---|
| #20 | Đã sửa sẵn từ Medium: hết người trả 409, khóa nhân viên rồi kiểm tra overlap trong transaction | ✅ Giữ implementation đúng, bổ sung hồi quy | `fdd5a2b` | `backend/src/test/java/com/petcarehub/AppointmentCapacityTest.java` | 4 test: 3 yêu cầu/2 nhân viên chỉ 2 thành công; tất cả bận 409; không eligible 409; staff chỉ định trùng bị từ chối, slot liền kề và hủy vẫn đúng |
| #21 | Kho đã khóa; loyalty/cart còn mất cập nhật/tạo trùng khi đồng thời, thiếu unique dòng giỏ | ✅ Đã sửa | `317cb20` | `LoyaltyServiceImpl`, `CartServiceImpl`, `CartItem`, schema, migration 002 | `BalanceAndCartConcurrencyTest` 7 test; mở rộng SQL Server test unique cart; giữ tests limits/ownership/stock cũ |
| #12 | Migration 001 đã có; schema khởi tạo/JPA chưa đồng bộ, chưa xử lý UNIQUE có tên do Hibernate sinh | ✅ Đã sửa mã/script; SQL Server chưa chạy | `bcbd494` | `Inventory`, schema/data SQL, migration 003, README | `InventoryMappingTest` 1 test DDL Hibernate; mở rộng `SqlServerConstraintTest` thành 3 test opt-in (đều skip) |
| #22 | Đã sửa sẵn từ Medium: Product + Inventory + nhập tồn đầu cùng transaction | ✅ Giữ implementation đúng, bổ sung hồi quy | `a6d32f8` | `backend/src/test/java/com/petcarehub/ProductInventoryCreationTest.java` | 5 case: null/0/5, mỗi case kiểm tra nhập tiếp; lỗi ledger rollback Product/Inventory; CUSTOMER 403 và stock âm 400 |

`PROGRESS.md` được cập nhật cùng từng commit. Các commit #20/#22 chủ yếu là test có chủ đích: không viết lại phần Medium đã đáp ứng yêu cầu để tạo diff.

## 3. Việc còn lại và giới hạn kiểm chứng

Không còn phần triển khai bỏ dở trong bốn ID. **Chưa áp dụng migration lên database của nhóm và chưa kiểm chứng trực tiếp SQL Server.** Đây là bước vận hành còn lại trước triển khai, không được tính như đã chạy thành công.

- H2 kiểm chứng transaction, khóa trong môi trường H2, rollback và DDL do Hibernate sinh. H2 không chứng minh UNIQUE/NULL, filtered index, locking/deadlock thực tế trên SQL Server.
- Ba test `SqlServerConstraintTest` bị skip vì không bật `RUN_SQLSERVER_TESTS=true`; không coi skip là pass. Đã compile nhưng chưa thực thi script trong container hay SQL Server thật.
- Các ID #13–19/#23/#24 và #15 giỏ hàng không được sửa. Lượt 3 cần sử dụng cùng quy ước khóa khi triển khai mutation giỏ còn thiếu.

## 4. Schema và áp dụng cho database có dữ liệu

Không thêm cột/version, không DROP bảng, không xóa/gộp dữ liệu tự động.

| Đối tượng | Thay đổi |
|---|---|
| `inventory.pet_id` | `UX_inventory_pet_present`: UNIQUE WHERE pet_id IS NOT NULL |
| `inventory.product_id` | `UX_inventory_product_present`: UNIQUE WHERE product_id IS NOT NULL |
| `inventory.quantity` | Schema mới có CHECK >=0, đồng nhất migration 001 sẵn có |
| `cart_items(cart_id, product_id)` | `UX_cart_items_cart_product_present`: UNIQUE WHERE product_id IS NOT NULL; nhiều dòng PET với product_id NULL vẫn được phép |
| Migration 001 | Giữ nguyên, thay hai constraint tên cũ và thêm CHECK nonnegative |
| Migration 002 | Kiểm tra duplicate cart/product dưới khóa bảng; duplicate thì THROW/ROLLBACK. Sau đó tạo filtered unique index nếu chưa có |
| Migration 003 | Tạo/giữ filtered inventory indexes trước; thay UNIQUE constraint/index không lọc, một cột pet_id hoặc product_id, kể cả tên Hibernate sinh. Giữ PK, FK, composite indexes và filtered indexes; tên đối tượng được QUOTENAME. Có transaction/rollback |

Trình tự nhóm thực hiện thủ công:

1. Review scripts trên nhánh mới; sao lưu DB, kiểm thử restore và thử trước trên bản sao. Dừng writer/instance cũ trong lúc nâng cấp để ORM cũ không tái sinh UNIQUE nullable.
2. Đối soát tồn âm, inventory trùng pet/product non-null và cart_items trùng `(cart_id, product_id)` non-null. Nếu có, quyết định xử lý nghiệp vụ trước; scripts không tự chọn hàng giữ/xóa.
3. Database đã tồn tại: chạy **001 → 002 → 003** trong `backend/src/main/resources/migrations/`. Dùng đúng database và SQL session có ANSI_NULLS, QUOTED_IDENTIFIER, ANSI_PADDING, ANSI_WARNINGS, CONCAT_NULL_YIELDS_NULL, ARITHABORT ON; NUMERIC_ROUNDABORT OFF (các settings được ghi ở đầu schema/002/003). Nếu 001 dừng do dữ liệu xấu, rollback transaction còn mở trước khi đối soát/chạy lại. Không chạy reset/schema để nâng cấp DB đang có dữ liệu.
4. Database trống: chạy `schema-sqlserver.sql` rồi **001 → 002 → 003**. Demo seed nếu cần chạy sau cùng, vẫn theo cơ chế dev opt-in hiện có. `data-sqlserver.sql` không đổi các hàng seed.
5. Kiểm tra `sys.indexes` có đúng ba unique indexes và filter; `inventory` không còn unique không lọc một cột pet_id/product_id; dữ liệu/tổng lượng không thay đổi ngoài thao tác đối soát đã duyệt. Chạy scripts lặp trên bản sao để kiểm tra idempotence.
6. Khởi động bản mới với cơ chế JWT hiện hành; sau khi migration hoàn tất dùng `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`. Hibernate không tạo được filtered indexes qua mapping JPA này: **không dùng riêng ddl-auto=create/update thay cho schema/migrations**. Hibernate validate cũng không thay thế việc kiểm tra index/filter bằng SQL.

SQL đọc để tìm dòng giỏ trùng:

```sql
SELECT cart_id, product_id, COUNT_BIG(*) AS row_count
FROM dbo.cart_items
WHERE product_id IS NOT NULL
GROUP BY cart_id, product_id
HAVING COUNT_BIG(*) > 1;
```

Test SQL Server opt-in sẵn có nay bao gồm: schema trên DB trống; nhiều NULL ở cả hai phía inventory; duplicate non-null bị từ chối; quantity âm; cart product unique và nhiều dòng PET; migration chạy lặp giữ dữ liệu; thay UNIQUE tên sinh tự động; duplicate cart làm migration dừng, giữ dữ liệu và đóng transaction. Các test chỉ nhắm container riêng nếu nhóm bật chạy, không dùng SQL Server localhost.

## 5. Cơ chế chống đồng thời

| Luồng | Cơ chế và lý do |
|---|---|
| Đặt lịch | Giữ PESSIMISTIC_WRITE trên hàng nhân viên, sau đó query overlap và insert trong cùng transaction. Khóa tồn tại ngay cả khi chưa có lịch. Auto assignment theo ID ổn định. Hết người -> IllegalStateException ->409, không fallback |
| Nhập/xuất/kiểm kê kho | Giữ khóa PESSIMISTIC_WRITE của Inventory; kiểm tra và thay đổi quantity + nhật ký cùng transaction; hai lần xuất vượt tổng tồn chỉ một thành công |
| Loyalty | Khóa hàng user bằng `UserRepository.findLockedById` trước khi đọc/tạo account; giữ đến commit cả balance/tier/ledger. GET có thể tạo account cũng transactional, tránh race tạo đầu. Giữ limits/overflow/ownership cũ |
| Add giỏ | Khóa user trước khi đọc/tạo cart và item; mọi add cùng user được tuần tự hóa, không mất lượng hay tạo hai cart. Filtered unique index chặn duplicate product line ở DB |
| Tạo sản phẩm | Giữ transaction Product + Inventory=0 + nhập tồn đầu qua InventoryService để có nhật ký. Lỗi ledger làm rollback cả hai hàng mới |

Không thêm @Version vì khóa đã đáp ứng yêu cầu và không cần đổi contract/xử lý optimistic retry. Khóa nhân viên hiện rộng hơn một ngày (cả hàng nhân viên); giữ lựa chọn Medium an toàn, chấp nhận giảm song song. Loyalty/cart cùng user dùng chung khóa nên cũng có thể chờ nhau. Các test race dùng thread/transaction riêng, barrier bắt đầu và timeout; không mô phỏng race chỉ bằng Mockito.

Snapshot tồn kho cho add giỏ được đọc trước khóa user để tránh đảo thứ tự kho -> user của nhật ký kho. Giỏ **không giữ chỗ hàng**; checkout vẫn kiểm tra/khóa kho như trước. Đây không phải cam kết hàng trong giỏ còn đủ khi thanh toán.

Quy ước cho lượt sau: mutation cart mới phải khóa user trước khi load cart/items, dùng cùng transaction đến commit. Không chỉ khóa dòng cart vì lần tạo đầu chưa có dòng để khóa. Không mở thêm đường ghi loyalty/cart bỏ qua quy ước này.

## 6. Contract API và frontend

Không đổi endpoint, field, DTO, role, claim JWT hoặc status code công khai. Frontend không cần sửa; build/typecheck được chạy sau mỗi nhóm. Không cài framework test frontend. Appointment giữ 409; loyalty/stock/cart validation giữ 400 và ownership giữ 401/403/404 theo handler hiện có.

Mapping Inventory thay đổi cách Hibernate sinh DDL, không thay đổi JSON hoặc tên cột. Unique index cart là bảo vệ dữ liệu, không thêm API update/remove/clear.

## 7. Giả định nghiệp vụ

- Mỗi sản phẩm xuất hiện tối đa một dòng trong một giỏ. Dòng PET không bị giới hạn bởi product_id NULL; không mở rộng nghiệp vụ thêm pet vào giỏ trong lượt này.
- Giỏ không giữ tồn kho; số dư loyalty và tồn kho chính thức chỉ cập nhật trong transaction.
- Không tự gộp quantity của các dòng giỏ cũ trùng nhau vì có thể vượt tồn hoặc khác ý định khách; nhóm đối soát trước migration.
- Giữ giờ làm việc, duration, chuyên môn, state machine, giới hạn điểm và tier hiện có; không chọn giả định mới thay thế các quyết định Medium.
- Tồn đầu null/0 tạo Inventory=0, không ghi một STOCK_IN giả bằng 0; tồn đầu >0 dùng nhật ký nhập kho hiện có.

## 8. Kiểm thử so với baseline

| Mốc | Tổng Maven | Pass | Failure/error | Skip | npm run build |
|---|---:|---:|---:|---:|---|
| Baseline `5c6dff2` | 127 | 126 | 0 | 1 | PASS |
| Sau #20 | 131 | 130 | 0 | 1 | PASS |
| Sau #21 | 138 | 137 | 0 | 1 | PASS |
| Sau #12 | 141 | 138 | 0 | 3 | PASS |
| Sau #22 / code cuối | 146 | 143 | 0 | 3 | PASS |

Thêm 19 case tổng cộng: 17 chạy pass, 2 case SQL Server mới skip cùng case cũ. Không có test baseline pass bị fail, không xóa test. Những test JWT, enabled, authorization, ownership, state transitions và batch-query cũ vẫn pass. Không có lệnh treo/không chạy được trong lượt này.

Lệnh backend (trong `ppet/backend`): `cmd.exe /c ".\mvnw.cmd -q -Dnet.bytebuddy.experimental=true test"`. Host Java 25, target Java 17; flag Byte Buddy tương thích Mockito như các lượt trước. Test dùng profile test/H2, không dev seed hay DB nghiệp vụ. Logs ignored: `high-2-baseline.log`, `high-2-20.log`, `high-2-21.log`, `high-2-12.log`, `high-2-22.log`; chi tiết Surefire ở `target/surefire-reports`.

Frontend: `npm.cmd run build` chạy `tsc -b && vite build`; sau **mọi** lần build đều `git restore ppet/frontend/tsconfig.app.tsbuildinfo`. Không đổi package/lockfile, không commit buildinfo. `git diff --check` pass. Lint không chạy lại trong lượt này (không có thay đổi frontend; yêu cầu baseline của lượt 2 là test/build).

## 9. Bản sửa trước còn hở và xung đột

- Không gặp xung đột phải hoàn tác hành vi Medium/High/Critical. #20 và #22 đã đúng nên chỉ tăng kiểm thử. Không sửa JWT/config profile; diff xác nhận các file đó không đổi.
- Khoảng hở mapping/schema nullable UNIQUE thuộc đúng #12 đã được xử lý. Migration 001 được giữ, migration 003 bổ sung cho tên UNIQUE do Hibernate tạo; không viết lại cơ chế #55.
- `InventoryServiceImpl.getCurrentUser()` còn fallback user ID 1 nếu không resolve được người thao tác; đây là vấn đề gán người ghi nhật ký đã tồn tại, chỉ ghi nhận, không sửa quyền/principal ngoài phạm vi. Test mới cung cấp principal thật hợp lệ.
- `ProductCreateRequest` chặn stock âm ở HTTP; gọi trực tiếp service với số âm hiện vẫn đi nhánh tồn 0. Không thay validation Medium ngoài yêu cầu #22; caller nội bộ phải tuân thủ DTO validation.
- Các TODO update/remove/clear giỏ #15 giữ nguyên. Không coi chúng đã hoàn thành nhờ bản sửa add đồng thời.
- Migration 001 cũ chưa có TRY/CATCH rollback tường minh; hướng dẫn vận hành nêu rollback nếu gặp lỗi, không refactor lại script cũ. Migration 002/003 mới có TRY/CATCH.

## 10. Kiểm tra tay nên làm

1. Hai phiên khách hợp lệ đặt cùng giờ, một nhân viên: một 201, một 409; ba yêu cầu tự phân công khi có hai nhân viên: đúng hai lịch. Thử slot liền kề, sai giờ, sai chuyên môn, hủy rồi đặt lại. Pet người khác vẫn 403; CUSTOMER không đổi trạng thái nghiệp vụ.
2. Tồn kho 100, gửi đồng thời hai lần xuất 80: đúng một thành công, tồn 20, một log xuất. Hai lần nhập hoặc nhập/xuất đồng thời giữ đúng tổng lượng và đủ nhật ký. Thử 0/âm/overflow vẫn bị từ chối như trước.
3. Loyalty 100 điểm, hai lần tiêu 80: một thành công, còn 20, một log REDEEM; hai lần cộng vào account chưa tồn tại tạo đúng một account. Khách không truy cập/tiêu điểm tài khoản khác.
4. Hai add sản phẩm vào giỏ mới tạo một cart, một dòng và cộng đủ lượng; giỏ cũ gần chạm stock chỉ nhận lượng hợp lệ. Không dùng kết quả này để suy ra update/remove/clear #15 đã chạy.
5. Tạo sản phẩm với null/0/số dương; GET trả đúng stock; nhập kho tiếp hoạt động ngay. Giả lập lỗi ghi kho/log trên môi trường thử để xác nhận không lưu product mồ côi. CUSTOMER vẫn không tạo sản phẩm.
6. Trên DB trống/bản sao SQL Server: chạy schema + 001/002/003, kiểm tra nhiều NULL hợp lệ và duplicate non-null bị chặn; chạy lặp không mất dữ liệu. Trên bản sao cũ, kiểm tra migration với tên UNIQUE do Hibernate sinh và cart trùng; migration phải dừng mà không tự xóa hàng.
7. Smoke test đăng nhập/JWT và booking/frontend sau triển khai để bảo đảm secret môi trường hiện hành, quyền và routing cũ vẫn đúng. Không đổi hoặc đưa secret vào báo cáo/log.
