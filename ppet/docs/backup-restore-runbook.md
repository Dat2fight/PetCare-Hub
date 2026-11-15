# Sao lưu và khôi phục SQL Server ngoài ứng dụng

Không có nút/API sao lưu hoặc restore DB trong PetCare Hub. Xuất CSV của admin chỉ gồm tập dữ liệu cho phép, không thay thế backup đầy đủ. Tài liệu này **chưa chạy trên database chính**.

## Quyết định vận hành — NEEDS DECISION

Chủ hệ thống/DBA xác định nơi lưu ngoài máy chủ, tài khoản dịch vụ có quyền thư mục, mã hóa và quản lý khóa, lịch full/differential/log backup, thời gian giữ, RPO/RTO và người có quyền restore. Mặc định giai đoạn này: người vận hành tạo full backup trước migration, lưu bản sao an toàn ngoài máy chạy, thử khôi phục trên database riêng; không suy diễn rằng một file trên cùng ổ đĩa đã đủ chống mất dữ liệu. Chọn lịch tự động và giám sát bằng công cụ SQL Server/hạ tầng sau khi chốt các mục trên.

## Trước triển khai V21

1. Xác nhận đúng server/instance/database trong cấu hình triển khai. Lưu phiên bản backend/frontend và danh sách migration hiện tại. Không dùng script reset/seed để nâng cấp.
2. Trong SSMS, người có quyền chọn database ứng dụng → Tasks → Back Up → Full; dùng **Copy-only**, bật backup checksum, chọn file mới có timestamp trong thư mục SQL Server được phép ghi. Không ghi đè bản backup cuối cùng. Không chép password/connection string vào ticket/log.
3. Dùng RESTORE VERIFYONLY với CHECKSUM hoặc tác vụ tương đương để kiểm tính đọc được. VERIFYONLY không thay thế việc khôi phục thử.
4. Khôi phục file vào **database mới có tên riêng** trên môi trường thử nghiệm; dùng tên file data/log khác, kiểm tra danh sách đích trước khi xác nhận. Không chọn WITH REPLACE lên database chính. Giới hạn quyền truy cập bản sao vì có dữ liệu cá nhân.
5. Trên bản sao, kiểm DBCC CHECKDB theo quy trình DBA, bảng/khóa/ràng buộc, số tài khoản/đơn/chi tiết/kho, đơn mẫu và nội dung Unicode. Ghi lại thời gian restore, kết quả và người kiểm. Kiểm app cùng Flyway validate trước khi nâng cấp thử đến V21.
6. Duyệt kết quả thử, dừng hoặc drain backend cũ để không ghi đồng thời trong lúc triển khai; chạy backend mới với Flyway và `ddl-auto=validate`, rồi frontend tương thích. Xác nhận migration 21, đăng nhập/permissions, mẫu đơn, báo cáo, CMS nháp/công khai, job DATA_EXPORT.

## Khi cần phục hồi

- Dừng thao tác ghi, lưu thông tin lỗi/migration và bảo toàn database hiện tại để điều tra. Không sửa checksum, xóa bảng/cột mới hoặc tự đánh dấu migration đã thành công.
- Người chịu trách nhiệm quyết định restore toàn DB hay sửa tiến bằng migration mới; đánh giá các giao dịch phát sinh sau bản backup, đặc biệt thu/hoàn tiền mặt và tồn kho thực tế. Restore không tự đảo ngược tiền mặt hoặc hàng hóa đã giao nhận.
- Kiểm bản backup và đích restore riêng như trên, đối soát dữ liệu rồi đổi cấu hình ứng dụng sang bản đã xác minh theo quy trình chuyển đổi. Triển khai đúng cặp phiên bản code/schema. Chỉ mở ghi khi người vận hành duyệt số liệu và quyền.
- Dữ liệu xuất dùng một lần sau restore có thể quay về trạng thái trước lần tải. Sau phục hồi, DBA rà soát/vô hiệu các job xuất cũ theo quy trình đã duyệt để không tái cung cấp file nhạy cảm; không giả định idempotency xuyên qua việc quay lùi toàn database.

Các câu SQL rà soát tài khoản nội bộ/ADMIN hoạt động, giỏ chứa sản phẩm tắt, pet PENDING không có đơn, voucher vượt lượt có ở [admin guide](admin-guide.md#kiểm-tra-dữ-liệu-trước-vận-hành-chỉ-đọc-tự-chạy-khi-cần). Chúng là kiểm tra chỉ đọc; việc sửa dữ liệu cần phương án riêng đã rà soát.
