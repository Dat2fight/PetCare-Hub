# Cổng quản trị — giai đoạn 1–3

Giai đoạn 3: `feature/admin-3-catalog`, nền `8e34108` (giai đoạn 2). Không push/merge/rebase.

## Vận hành

- Đăng nhập, chọn **Quản trị**, hoặc mở `/PetCare-Hub/admin`. Menu lấy từ `GET /api/v1/admin/me/permissions`.
- **Người dùng**: tìm tên/email, lọc vai trò/trạng thái, sắp xếp trên tiêu đề cột, phân trang. Bấm tên để xem hồ sơ; mỗi lần đọc hồ sơ ghi audit.
- **Tạo nhân viên**: chọn bán hàng/kho/bác sĩ. Mật khẩu ngẫu nhiên hiển thị đúng một lần, không lưu trong trình duyệt hoặc nhật ký. Chuyển riêng cho nhân viên; nếu đóng mất thì không thể xem lại. Chưa có chức năng đặt lại mật khẩu theo yêu cầu không làm quên mật khẩu.
- Nhân viên dùng mật khẩu tạm phải đổi tại `/change-password`; backend chặn chức năng khác, kể cả WebSocket. Đổi xong phải đăng nhập lại.
- Khóa, đổi vai trò, xóa mềm, thu hồi phiên đều tăng phiên bản token. Token cũ không sống lại khi mở khóa. Luồng thông báo kiểm tra lại phiên bản trước mỗi lần gửi/nhận dữ liệu.
- Không tự khóa/xóa/đổi quyền; không loại admin hoạt động cuối cùng. Thay đổi từ/sang ADMIN, khóa ADMIN, xóa nhân viên/admin yêu cầu mật khẩu người thực hiện.
- Xóa mềm giữ username/email, đơn hàng và lịch sử. Lịch tương lai SCHEDULED/CONFIRMED của nhân viên bị xóa được bỏ phân công và thông báo khách; lịch đang thực hiện giữ nguyên. Dùng Lịch hẹn → Chưa phân công để giao lại ở giai đoạn 2.
- **Nhật ký thao tác**: lọc người thực hiện, mã hành động, đối tượng, khoảng ngày; mở chi tiết để xem thay đổi an toàn và mã đối chiếu lỗi. Không có API sửa/xóa.

## Quyền

Nguồn duy nhất của portal: `PermissionService`. ADMIN có tất cả quyền; SALES_STAFF có quyền vận hành/catalog/kho/khuyến mãi/review/nội dung/gói và lịch được giao; WAREHOUSE_STAFF có kho/đọc catalog; VETERINARIAN có lịch và y tế được giao. CUSTOMER không vào portal.

Giai đoạn 1 có tổng quan cơ bản, người dùng, hồ sơ, nhật ký; giai đoạn 2 thêm đơn hàng, kho, lịch hẹn, đánh giá. Giai đoạn 3 thêm catalog/khuyến mãi/lịch làm việc; CMS/y tế nâng cao theo kế hoạch sau. API nghiệp vụ cũ giữ chính sách cũ cho tới khi được đưa vào từng giai đoạn.

## Audit và bảo mật

Ghi tường minh trong service, cùng transaction với thay đổi; rollback thay đổi thì rollback SUCCESS. Filter ghi DENIED/FAILED bằng transaction riêng sau khi HTTP bị từ chối. Không ghi request body, mật khẩu, token, email, điện thoại hoặc địa chỉ. Lý do được yêu cầu tại API/UI nhưng văn bản tự do không đưa vào audit để tránh lưu dữ liệu nhạy cảm; audit ghi hành động và trường trạng thái/quyền đã đổi. IP lấy từ socket, không tin header forwarding của client.

Mã hành động: STAFF_CREATED, USER_STATUS_CHANGED, USER_ROLE_CHANGED, USER_DELETED, USER_SESSIONS_REVOKED, USER_OVERVIEW_READ, USER_PASSWORD_CHANGED, ADMIN_REQUEST_REJECTED. SQL Server có trigger ngăn UPDATE/DELETE audit. Đây không phải cơ chế ngăn DBA thay đổi schema/trigger.

Các thay đổi quản trị khóa hàng role ADMIN trước khi đọc lại danh tính người thao tác và khóa đối tượng. Quy tắc này chống hai admin đồng thời vô hiệu hóa lẫn nhau.

## Migration và triển khai

V16 thêm token_version/must_change_password, dùng deleted_at có sẵn, cho phép staff_id lịch hẹn rỗng. V17 tạo bảng/index/trigger audit. Không sửa migration trước đó. Không thêm thư viện hay biến môi trường bắt buộc.

Trước áp dụng thật: sao lưu và xác minh khôi phục; chạy thử trên bản sao; dừng backend cũ; khởi động backend mới với Flyway và `ddl-auto=validate`. Không chạy reset/seed. Người dùng phải đăng nhập lại vì JWT cũ thiếu claim `tv`. Không rollback bằng xóa cột hoặc xóa audit. Bản triển khai này chưa tự áp dụng vào database thật.

## Kiểm tra dữ liệu trước vận hành (chỉ đọc, tự chạy khi cần)

```sql
-- Rà soát thủ công tài khoản nội bộ; không đủ dữ liệu để kết luận tài khoản được cấp trái phép.
SELECT u.id,u.username,u.enabled,u.deleted_at,r.name
FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id
WHERE r.name <> 'CUSTOMER';
SELECT COUNT(*) AS active_admins FROM users u JOIN user_roles ur ON ur.user_id=u.id
JOIN roles r ON r.id=ur.role_id WHERE r.name='ADMIN' AND u.enabled=1 AND u.deleted_at IS NULL;
SELECT ci.id,ci.product_id FROM cart_items ci JOIN products p ON p.id=ci.product_id WHERE p.active=0;
SELECT id FROM pets WHERE availability_status='PENDING' AND reserved_order_id IS NULL;
SELECT id,code,current_uses,max_uses FROM promotions WHERE max_uses IS NOT NULL AND current_uses>max_uses;
```

## Các quyết định giữ nguyên

COD/tiền mặt; thông báo web; không email/social/quên mật khẩu. Giữ role kho hiện hữu. Không tự áp dụng migration lên DB thật. Nhả pet sẽ hủy cả đơn ở giai đoạn vận hành. Nhận nuôi/biến thể dừng ở thiết kế khi chưa chốt. Gói cũ chưa có hạn mức không được tự suy thành vô hạn. CMS giữ 20 phiên bản; trả hàng sản phẩm mặc định 7 ngày trong giai đoạn 5.

## Vận hành giai đoạn 2

1. **Đơn hàng**: lọc mã/tên khách, trạng thái đơn/thanh toán/ngày; mở đơn để xác nhận → chuẩn bị → giao → xác nhận đã giao và thu tiền. Chỉ xác nhận thu khi thực tế đã nhận tiền mặt. Sai thứ tự bị từ chối 409. Nút hủy chỉ xuất hiện trước khi giao, yêu cầu lý do và hủy toàn bộ đơn hỗn hợp.
2. **Hoàn tiền**: với đơn đã giao/thu tiền, lập yêu cầu cùng lý do. Lọc “Chờ hoàn tiền”, đối chiếu thực tế, rồi “Đánh dấu đã hoàn” kèm biên nhận và lý do. Gửi lại cùng biên nhận không ghi tiền hai lần. Đây là hoàn toàn bộ tiền; trả hàng, hoàn kho, điểm đã kiếm, chuyển chủ và hủy gói không được tự xử lý. Quy trình trả/đổi hàng thuộc giai đoạn 5.
3. **Ghi chú / phiếu / đối soát**: ghi chú nội bộ nằm riêng và không in cho khách. Phiếu in HTML lấy giá/số lượng/địa chỉ đơn, tên snapshot từ đơn mới; dùng In/Lưu PDF. Đơn cũ chưa có snapshot tên sẽ dùng tên catalog hiện tại. Bảng lịch sử chỉ là thu/hoàn tiền mặt nội bộ; đơn cũ có thể có paidAt nhưng không có event, không suy diễn thành chưa thu.
4. **Kho**: nhập/xuất số lượng tăng/giảm; kiểm kê nhập số tồn thực tế mới. Mọi thao tác cần lý do; không xuất vượt tồn. Ngưỡng thấp dùng reorderPoint hiện có, thiếu thì 10. Lịch sử lọc theo ID sản phẩm, nhân viên, loại, ngày; ORDER/<id> + SYSTEM là giao dịch từ đơn hàng.
5. **Lịch hẹn**: bảng/lịch tuần có bộ lọc. Lịch tuần hiển thị trang hiện tại (20 dòng), dùng phân trang để xem hết. ADMIN phân công/đổi nhân viên; hộp chọn có phân trang, máy chủ kiểm tra vai trò, khóa tài khoản và trùng lịch. SALES_STAFF/VETERINARIAN chỉ thấy lịch của mình. Hủy cần lý do và gửi thông báo web cho khách.
6. **Đánh giá**: mặc định chờ duyệt; duyệt/từ chối/ẩn/hiện lại bằng lựa chọn “Đã duyệt”. Từ chối cần lý do; form cũng đề nghị ghi lý do cho thao tác khác. Nội dung HTML hiển thị nguyên văn, không chạy script. Tổng quan có số đánh giá chờ duyệt.

## Audit giai đoạn 2

SUCCESS cùng transaction: ORDER_CONFIRM, ORDER_PROCESS, ORDER_SHIP, ORDER_DELIVER, ORDER_CANCEL, ORDER_REFUND_REQUESTED, ORDER_REFUNDED, ORDER_NOTE_ADDED; INVENTORY_STOCK_IN, INVENTORY_STOCK_OUT, INVENTORY_ADJUSTMENT; APPOINTMENT_ASSIGNED, APPOINTMENT_STATUS_CHANGED; REVIEW_APPROVED, REVIEW_REJECTED, REVIEW_HIDDEN.

Đọc nhạy cảm: ORDER_DETAIL_READ, ORDER_PRINT_READ, ORDER_NOTES_READ, ORDER_PAYMENTS_READ, APPOINTMENT_READ. POST lỗi dùng ADMIN_REQUEST_REJECTED riêng như giai đoạn 1. Mỗi request ghi thành công có một audit, kể cả replay hoàn tiền; replay không tạo thêm giao dịch tiền. Không lưu nội dung ghi chú/review/biên nhận/PII vào details audit.

Lý do bắt buộc: hủy đơn/lịch, nhập/xuất/kiểm kê, phân công, yêu cầu/xác nhận hoàn, từ chối review. Không bổ sung nhập lại mật khẩu cho thao tác vận hành trong giai đoạn 2; các điều kiện quản trị tài khoản của giai đoạn 1 giữ nguyên.

Trước triển khai: sao lưu và kiểm tra nâng cấp V18 trên bản sao, sau đó áp Flyway và triển khai backend/frontend cùng phiên bản; rà quyền nhân viên và thử giao dịch tiền mặt. Không dùng migration ngược để xóa sổ tiền/audit. Không thêm biến môi trường hoặc dependency.

## Danh mục — giai đoạn 3

1. **Sản phẩm/danh mục:** chọn Tạo mới hoặc tên dòng để sửa; form không sửa số tồn. Có thể chọn danh mục bằng bảng tìm kiếm/phân trang. Xóa cần lý do; đã tham chiếu thì tắt hoạt động. Danh mục cha không được tạo vòng; thứ tự hiển thị áp dụng danh sách công khai.
2. **Bulk/CSV:** chọn ID hoặc tải file, bấm Kiểm tra/xem trước, đọc báo cáo rồi xác nhận áp dụng. Khi dữ liệu đổi phải preview lại. CSV giữ tồn kho, không nhập riêng các dòng đúng nếu file có dòng sai. Ngưỡng tồn trống dùng 10.
3. **Thú cưng/loài/giống:** giống phải thuộc loài, ngày sinh không tương lai. Pet của khách chỉ ở Hỗ trợ thú cưng, đọc có audit. Pet giữ chỗ: xem đơn và hạn giữ, nút hủy sẽ hủy cả đơn sản phẩm+pet, không tách đơn. Pet từng thuộc đơn không được sửa giá hoặc xóa trực tiếp.
4. **Dịch vụ/lịch:** tắt dịch vụ chặn đặt mới, giữ lịch cũ. Chỉ ADMIN cấu hình Lịch làm việc. Ca lặp theo thứ; nghỉ theo ngày. Xem tuần theo ngày bắt đầu, lọc ID nhân viên, phân trang; bảng và lịch hiển thị cùng trang hiện tại. Khi đóng cửa, dùng phần lịch bị ảnh hưởng, chọn rõ các lịch cần hủy và nhập lý do; không hủy ngầm.
5. **Khuyến mãi/điểm:** mã dùng cho sản phẩm, đã dùng chỉ gia hạn/tắt. Quy tắc điểm chỉ ADMIN; thay đổi tỷ lệ tích không đổi snapshot đơn trước đó. Điều chỉnh điểm thay số dư, không thay lifetime. UI khách đọc ngưỡng/tỷ lệ cùng nguồn backend.
6. **Gói chăm sóc:** CRUD gói; danh sách đăng ký dùng tên snapshot nếu có; hủy/gia hạn có lý do. Chưa có dữ liệu hạn mức/lượt dùng của gói cũ: không suy thành vô hạn, cần chốt theo thiết kế trước khi triển khai phần này.
7. **Chiến dịch:** chọn nhóm, xem số người nhận, nhập mã/thời hạn/thông báo rồi xác nhận. Tối đa 500 khách, một chiến dịch mỗi 10 phút/người gửi. Mã chỉ dùng được cho danh sách đã chốt. Thông báo lưu trên web; không email/SMS.

Ảnh: URL HTTPS thuộc danh sách cấu hình `APP_CATALOG_IMAGE_HOSTS`. Migration V19 chưa áp database thật. [Báo cáo và ma trận quyền](admin-phase-3-report.md), [các quyết định còn lại](admin-phase-3-design-decisions.md).
