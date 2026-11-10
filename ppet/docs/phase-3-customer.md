# Giai đoạn 3 · Tính năng khách hàng

Nhánh `feature/phase-3-customer` tiếp nối luồng đặt đơn COD tại `41081ca`.

| Tính năng | Trạng thái | Điểm vào mã nguồn | Migration | Kiểm thử chính | Commit |
|---|---|---|---|---|---|
| F9 · Đánh giá | ĐÃ XONG | ReviewController, ReviewServiceImpl, ReviewRepository, Reviews.tsx | 007_reviews.sql | Điều kiện giao dịch, đánh giá trùng, sửa/xóa khác chủ, trạng thái sau chỉnh sửa, ẩn nội dung chờ duyệt | 55d0116 |
| F2 · Yêu thích | ĐÃ XONG | WishlistController, WishlistRepository, Wishlist.tsx, HomePage và các trang danh mục/chi tiết | 008_wishlist.sql | Thêm hai lần, cô lập hai tài khoản, sản phẩm tắt, thú cưng đã đổi chủ, phân trang và truy vấn trạng thái theo lô | 33ad3df |
| F8 · Gói chăm sóc | ĐÃ XONG | CarePackageController, PackageCheckout, OrderServiceImpl, CarePackages.tsx, orderService | 009_package_orders.sql | Đặt cho thú cưng khác chủ, đơn lặp, gói tắt, hủy trước khi thu tiền, giao lặp, quyền lợi đã chốt, thời hạn và quyền đọc | feat(F8) trên cùng nhánh |

Danh sách file đầy đủ nằm trong `git show --stat <commit>`. Tên và quyền của các endpoint trong [api.md](api.md). Thứ tự migration, yêu cầu database và lệnh PowerShell trong [README](../README.md). Cấu hình môi trường và thư viện phụ thuộc dùng lại từ giai đoạn trước.

## Kết quả kiểm tra

- Maven với profile test có 239 test, 236 đạt và 3 test SQL Server Testcontainers bỏ qua. Máy không có Docker CLI. Đã bổ sung các migration mới vào vòng chạy lặp của bộ SQL Server. Cần chạy trên SQL Server cô lập trước khi triển khai. Database dev chưa chạy migration.
- Ma trận endpoint kiểm tra truy cập anonymous, các thao tác ghi của CUSTOMER và từng vai trò nhân viên. Các ca nghiệp vụ mới nằm trong PhaseThreeOwnershipTest.
- Frontend đạt `npx tsc --noEmit`, `npm run lint` và `npm run build` (gồm `tsc -b`).
- Chrome trên bản dựng production với phản hồi API kiểm thử đã kiểm tra 7 trang ở 320/360/390 px. Danh sách, chi tiết, checkout gói, gói của tôi, wishlist, đánh giá của tôi và chi tiết dịch vụ đều vừa chiều ngang. Chuỗi chứa thẻ HTML hiện dưới dạng chữ. Đã thử hoàn tác nút tim khi máy chủ từ chối, thêm lại thành công và gửi đơn gói với thú cưng đã chọn. Checkout dùng được trên cả origin HTTPS và HTTP phát triển.

## Quy tắc nghiệp vụ

Khách đặt từng gói cho một thú cưng thuộc tài khoản của mình. Đơn gói thanh toán riêng bằng tiền mặt, phí giao hàng bằng 0. Nhân viên xác nhận thu tiền ở bước giao thành công. Một dòng đơn kích hoạt tối đa một hợp đồng, kể cả khi yêu cầu được gửi lại. Tắt gói sau khi đặt vẫn giữ quyền lợi đã chốt. Hủy đơn trước khi thu tiền sẽ không sinh hợp đồng. Ngày cuối cùng còn hiệu lực được hiển thị tại Gói của tôi.

Mô hình gói hiện lưu ba quyền lợi theo loại dịch vụ. Việc đếm lượt và lịch sử sử dụng cần mô hình định mức và giao dịch sử dụng. Theo lựa chọn Q10, màn hình hiện hiển thị quyền lợi, đơn mua và thời hạn thực tế. Hàng đợi kiểm duyệt và màn hình nhân viên quản lý danh mục thuộc F10. API cho cả hai nghiệp vụ đã có trong giai đoạn này.

## Kết quả rà soát chuỗi trong kho mã

- Nhãn tính năng sắp ra mắt còn ở chân trang, trang đăng nhập và biên bản QA cũ. Nút tim trong phạm vi giai đoạn này đã dùng API.
- Các chuỗi mock thuộc test backend, công cụ QA, báo cáo, dữ liệu mẫu cũ và ảnh minh họa ở trang chủ, đăng nhập, đăng ký, dịch vụ. Danh sách thú cưng nổi bật và các tính năng khách hàng mới dùng dữ liệu API.
- Liên kết href trỏ dấu thăng còn được nhắc trong tài liệu và bộ kiểm tra UI. Các trang mới dùng route hoặc button.
- Ghi chú quyết định cũ liên quan giá thú cưng công khai, vai trò grooming, giới hạn thú cưng cá nhân, quy đổi tuổi, quyền gỡ quản trị và thời gian thu tiền lịch sử. Các ghi chú còn lại nằm trong báo cáo trước đây. Giai đoạn này áp dụng lựa chọn Q8, Q10 và Q11 đã chốt.
