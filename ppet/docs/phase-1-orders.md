# Giai đoạn 1 · Đơn COD

| Tính năng | Thay đổi | Kiểm chứng |
|---|---|---|
| F6 | Job quét ứng viên theo trang, khóa từng đơn, kiểm tra lại trạng thái, nhả tài nguyên và lưu thông báo trong một transaction. | Test đơn chưa tới hạn, đã thu tiền, quá hạn và chạy lại. |
| F5 | POST /orders/quote và POST /orders dùng chung quy tắc tính tiền. Snapshot lưu trên đơn. Khóa theo khách và mã, mỗi lần hủy trả điểm khả dụng và lượt dùng. Đơn COD giao thành công tích điểm theo tiền sản phẩm thực trả. | Test quote không tiêu điểm, giá phía server, gửi lại cùng khóa, hoàn điểm và lượt mã. Ba test giá đơn sản phẩm cũ đã cộng thêm phí 30.000đ theo quyết định Q6. |
| F1 | Thêm pet qua POST /cart/items, đặt một đơn COD chung với sản phẩm. Mỗi pet là một dòng số lượng 1. Thêm vào giỏ chưa giữ hàng. Checkout kiểm tra quyền, giá và khóa pet. | Test gửi thêm pet hai lần, 2 khách cạnh tranh, hủy/giao, rollback, tình trạng công khai và hết hạn đơn hỗn hợp. Các test chặn mọi pet trước đây được thay bằng kiểm tra hành vi mua thực tế. |

**Vận hành.** Chạy các script có tiền tố 004 rồi lần lượt 005_order_expiry.sql và 006_checkout_pricing.sql bằng sqlcmd -I -b -f 65001 trên database đích đã chọn. Script 005 thêm hạn và lý do nhận biết hết hạn; script 006 thêm snapshot, giới hạn voucher và khóa chống tạo đơn trùng. Hai script phải được áp trước khi khởi động bản mới. Xem README và docs/database.md. Mặc định có thể chạy mà không khai báo thêm secret.

**Phạm vi phí.** Với đơn hỗn hợp, phần thú cưng được tính vào tổng tiền hàng để xét miễn phí giao hàng, còn voucher, đổi điểm và tích điểm chỉ dựa vào tiền sản phẩm. Mua riêng thú cưng có phí giao hàng bằng 0. Không xác định hạn hồi tố cho đơn cũ chưa có expires_at.

**Kiểm tra frontend.** TypeScript, ESLint và Vite build. Kiểm tra Chrome với fixture khách hàng trên trang pet, giỏ hỗn hợp và checkout ở chiều rộng 320, 360, 390px. Đây là kiểm tra bố cục, không sử dụng dữ liệu dev. Luồng ghi dữ liệu được kiểm tra qua MockMvc và H2.

**Quét mã nguồn.** Các nhãn sắp ra mắt còn ở wishlist, footer và xác thực thuộc các giai đoạn sau. Các tham chiếu mockImages và mockProducts thuộc nội dung minh họa, một số thẻ trang chủ còn có ID mẫu cần xử lý ở F2. Không tìm thấy href="#" trong src frontend. Các ghi chú NEEDS DECISION còn lại liên quan giới hạn số pet cá nhân, phân công dịch vụ, tuổi ước tính, giá công khai, kiểm duyệt review và quản trị tài khoản. Chưa kiểm chứng trên SQL Server thật.
