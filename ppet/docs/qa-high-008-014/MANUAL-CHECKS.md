# Kiểm thử thủ công sau BUG-008–014

Chỉ tạo dữ liệu `qa_` trên H2/test hoặc DB test riêng; không thử mutation trên DB dev thật. Các test tự động dùng profile test. SQL Server cần kiểm riêng trên container và duyệt migration004 trước khi triển khai.

1. Không token: `curl -i http://localhost:8081/api/v1/auth/me` và `curl -i http://localhost:8081/api/v1/pets/my-pets` phải401 JSON với path/message/correlationId; token hỏng/hết hạn cũng401. Login sai không reload/reset form. Mở my-pets/profile/orders khi guest phải sang login, đăng nhập thành công quay về trang trước.
2. Khách `qa_` chưa có pet mở `/PetCare-Hub/services/booking?serviceId=<dịch vụ active thật>`. Loading/lỗi/rỗng phải khác nhau. Tắt API thú cưng → lỗi và Thử lại, không nói chưa có pet.
3. Rỗng thật: bấm Thêm nhanh, nhập tên/loài tải từ API/tuổi0–30. Thử tên trắng/quá100, tuổi âm/31/số lẻ, species không tồn tại; phải báo lỗi. Lưu đúng → chọn pet mới, không rời trang, giữ dịch vụ/ngày/giờ. Esc đóng modal và trả focus về nút mở.
4. Tắt API giờ trống hoặc đổi ngày liên tục: lỗi có Thử lại, không có giờ giả, response ngày cũ không ghi đè ngày mới. Ngày hết slot có thông báo hết chỗ riêng.
5. Dùng token khách A POST appointments với petB, pet không tồn tại hoặc pet đang bán:404; thiếu petId400; guest401; kiểm DB test không thêm appointment. PetA thật thành công, customerId/ownerId giả bị bỏ qua. Không dán token vào báo cáo.
6. `/pets/not-a-number`, `/pets/0`, `/pets/999999` hiện404; ID không hợp lệ không phát request. Pet thật hiển thị đúng response; owned/SOLD404, RESERVED đang đặt. Nút mua hiện Sắp ra mắt và disabled vì chưa hợp nhất feature mua pet. Lỗi mạng/5xx có retry.
7. Chỉ ADMIN được gọi dashboard/revenue-report. Cùng from/to phải cùng revenue. Tạo dữ liệu test paidAt ở đầu/cuối kỳ, paid/unpaid/cancelled/refunded/COD-chưa-giao; tổng chỉ gồm COMPLETED, không CANCELLED, COD phải DELIVERED, paidAt trong kỳ. Kỳ rỗng0; đảo ngày/quá366 ngày400. Đơn cũ thiếu paidAt không tính, cần đối soát riêng.
8. DevTools kiểm viewport320/360/375/390/412/768/1366/1920, guest và tất cả5 role. Console: `document.documentElement.scrollWidth <= window.innerWidth`. Kiểm home/products/product detail/pets/pet detail/login/register/booking/checkout/cart/orders. Mở menu bằng bàn phím, Esc, chọn link, resize; đăng nhập/đăng ký/cart bấm được. Tên dài/badge lớn không tràn.
9. Quên mật khẩu và Google/Facebook ghi Sắp ra mắt, không nhảy đầu trang/giả báo thành công. Wishlist cũng rõ chưa hỗ trợ. Không có địa chỉ hỗ trợ thật trong frontend nên không bịa thông tin liên hệ.

## Chạy lại tự động

Backend (thư mục `ppet/backend`): `./mvnw.cmd clean test`.

Frontend (thư mục `ppet/frontend`), chạy tuần tự:

```powershell
npm ci
npm run lint
node tools/check-lint.cjs
node tools/check-actions.cjs
npx tsc --noEmit
npm run build
```

UI harness không thêm package dự án. Cài Playwright tạm ngoài repo theo chính sách máy, đặt `QA_PLAYWRIGHT` tới module đó. Chạy Vite cổng5175; các script dùng Chrome tại đường dẫn Windows ghi trong file và chặn toàn bộ API bằng fixture:

```powershell
$env:QA_PLAYWRIGHT = "$env:TEMP/petcare-qa-browser/node_modules/playwright"
node tools/check-qa-ui.cjs
node tools/check-responsive.cjs ../docs/qa-high-008-014/responsive-after.json
```

Browser harness kiểm UI với fixture, không thay thế kiểm thử tích hợp SQL Server. Dữ liệu thanh toán thật/hoàn một phần/hợp nhất pet purchase cần quyết định riêng.
