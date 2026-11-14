# Giai đoạn 3 — thiết kế cần quyết định

Các mặc định Q1–Q15 giữ nguyên: tiền mặt, thông báo web, tài khoản nội bộ, không email/social/quên mật khẩu. Không có thay đổi DB thật hoặc publish trong giai đoạn này.

## N3-P2: biến thể sản phẩm — NEEDS DECISION

Chưa bật biến thể. Product hiện là đơn vị SKU/tồn/giỏ/đơn; thêm size/màu mà chỉ đổi form sẽ làm đặt sai giá/tồn. Đề xuất product_variants(id,product_id,sku,size,color,price,active), inventory theo variant, cart_item/order_item chứa variant_id và snapshot tên/thuộc tính. Backfill một biến thể mặc định cho product cũ trên bản sao, version API và kiểm cạnh tranh checkout/hoàn kho trước khi bật. Cần chọn có triển khai và cách di chuyển SKU hiện hữu; mặc định giữ model hiện tại.

## N5-P2: nhận nuôi — NEEDS DECISION

Chưa triển khai hồ sơ nhận nuôi khi chưa có phê duyệt riêng. Đề xuất adoption_applications(customer_id,pet_id,status,submitted_at,reviewed_by,reviewed_at,reason), APPLIED→REVIEWING→APPROVED/REJECTED, audit và thông báo. Không tạo đơn mua/giảm giá; cần chốt tiêu chí xét duyệt, quyền reviewer, quyền sở hữu sau duyệt và chính sách thông tin hồ sơ.

## N7-P1: hạn mức gói — MỘT PHẦN / NEEDS DECISION

Đã có CRUD gói, danh sách đăng ký, tên snapshot, hủy/gia hạn. Dữ liệu hiện hữu chỉ có ba cờ includesGrooming/includesCheckup/includesVaccination; không có số lượt được cấp hay lịch sử sử dụng. UI ghi “Chưa có dữ liệu hạn mức”; không hiển thị 0 hoặc vô hạn như thể biết chắc.

Đề xuất care_package_benefits(package_id,service_type,quota), snapshot subscription_benefits(subscription_id,service_type,granted), ledger subscription_usage(benefit_id,appointment_id,delta,idempotency_key,reason). Dùng lượt khi COMPLETED, hoàn lượt qua reversal có audit, khóa benefit khi dùng. Gói cũ granted=NULL nghĩa UNKNOWN, không cấp tự động; cần nhập hạn mức và đối soát số đã dùng từ bằng chứng. Cần chốt số lượt mỗi loại, kỳ cấp theo tháng/toàn gói, hạn mức khi gia hạn, và dữ liệu backfill. Không thể suy ra những số này từ các cờ boolean.

## Giới hạn vận hành đã chọn

- Preview/batch/chiến dịch xử lý đồng bộ, có giới hạn cứng 100/200/500. Khi tăng quy mô cần outbox/job và theo dõi trạng thái; không tăng trần đơn thuần.
- Lịch tuần phân trang như bảng, hiển thị rõ “trang hiện tại”; chọn ngày bắt đầu và nhân viên để thu hẹp. Danh sách bị ảnh hưởng là các lịch còn hoạt động trong ngày/nhân viên, ADMIN đối chiếu giờ nghỉ một phần ngày trước khi chọn hủy.
- URL nghiệp vụ legacy vẫn tương thích, guard cũ; portal mới dùng URL admin. Không khẳng định đã hợp nhất tất cả validation/audit của legacy vào portal.
- Migration chỉ kiểm chứng ở SQL Server thử nghiệm. Triển khai thật vẫn cần sao lưu và thử bản sao theo runbook.
