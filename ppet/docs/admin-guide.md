# Cổng quản trị — giai đoạn 1

Nhánh: `feature/admin-1-foundation`, nền `0fd70f1`. Không push/merge/rebase.

## Vận hành

- Đăng nhập, chọn **Quản trị**, hoặc mở `/PetCare-Hub/admin`. Menu lấy từ `GET /api/v1/admin/me/permissions`.
- **Người dùng**: tìm tên/email, lọc vai trò/trạng thái, sắp xếp trên tiêu đề cột, phân trang. Bấm tên để xem hồ sơ; mỗi lần đọc hồ sơ ghi audit.
- **Tạo nhân viên**: chọn bán hàng/kho/bác sĩ. Mật khẩu ngẫu nhiên hiển thị đúng một lần, không lưu trong trình duyệt hoặc nhật ký. Chuyển riêng cho nhân viên; nếu đóng mất thì không thể xem lại. Chưa có chức năng đặt lại mật khẩu theo yêu cầu không làm quên mật khẩu.
- Nhân viên dùng mật khẩu tạm phải đổi tại `/change-password`; backend chặn chức năng khác, kể cả WebSocket. Đổi xong phải đăng nhập lại.
- Khóa, đổi vai trò, xóa mềm, thu hồi phiên đều tăng phiên bản token. Token cũ không sống lại khi mở khóa. Luồng thông báo kiểm tra lại phiên bản trước mỗi lần gửi/nhận dữ liệu.
- Không tự khóa/xóa/đổi quyền; không loại admin hoạt động cuối cùng. Thay đổi từ/sang ADMIN, khóa ADMIN, xóa nhân viên/admin yêu cầu mật khẩu người thực hiện.
- Xóa mềm giữ username/email, đơn hàng và lịch sử. Lịch tương lai SCHEDULED/CONFIRMED của nhân viên bị xóa được bỏ phân công và thông báo khách; lịch đang thực hiện giữ nguyên. Chức năng phân công lại thuộc giai đoạn 2.
- **Nhật ký thao tác**: lọc người thực hiện, mã hành động, đối tượng, khoảng ngày; mở chi tiết để xem thay đổi an toàn và mã đối chiếu lỗi. Không có API sửa/xóa.

## Quyền

Nguồn duy nhất của portal: `PermissionService`. ADMIN có tất cả quyền; SALES_STAFF có quyền vận hành/catalog/kho/khuyến mãi/review/nội dung/gói và lịch được giao; WAREHOUSE_STAFF có kho/đọc catalog; VETERINARIAN có lịch và y tế được giao. CUSTOMER không vào portal.

Giai đoạn 1 chỉ có các màn tổng quan cơ bản, người dùng, hồ sơ, nhật ký. Các quyền vận hành còn lại là nền cho các giai đoạn sau, chưa phải màn hình đã hoàn tất. API nghiệp vụ cũ giữ chính sách cũ cho tới khi được đưa vào từng giai đoạn.

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
