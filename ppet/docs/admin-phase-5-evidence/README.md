# Bằng chứng giao diện giai đoạn 5

2026-11-15, Chrome headless thật, frontend production build (Vite preview), backend Java 17 và H2 tạm, không dùng database chính. Script kiểm chứng cục bộ `backend/target/admin5-proof.cjs` và log Maven trong target (ignored, không chứa trong bản phát hành). Fixture/tài khoản test cách ly; không chụp mật khẩu/token.

Đã chạy: login admin → tạo nháp (public404) → publish → trang công khai render `<script>` như văn bản → sửa cấu hình → enqueue CSV → READY → tải BOM UTF-8 → CONSUMED. Context khách riêng: đăng ký/login, đặt đơn sản phẩm thật và xác nhận giao/thu tiền → gửi yêu cầu trả từ UI → context admin duyệt, nhận kho, xác nhận đã hoàn tiền mặt → API khách phản ánh returnedAmount. Không có pageerror.

Năm route admin content, content/{id}, settings, data-exports, returns kiểm 320/360/390/1366px không tràn ngang. Public articles/article/FAQ/terms kiểm 320px; khách gửi yêu cầu/đọc lịch sử trên desktop. Tab ở dialog settings và Escape đóng hoạt động. Không coi kiểm tự động này là kiểm toàn diện screen reader hoặc test tải.

Ảnh tiêu biểu:

- [CMS desktop và xem trước HTML dưới dạng text](content-editor-1366.png)
- [CMS 320px](content-editor-320.png)
- [Cấu hình 320px](settings-320.png)
- [Lịch sử xuất dữ liệu 320px](data-exports-320.png)
- [Trả hàng 320px](returns-320.png)
- [Khách gửi yêu cầu trả hàng](customer-return-1366.png)

Kết quả backend: 303 tests, 0 failures/errors/skipped; migration V21 có kiểm SQL Server trên DB test riêng, nghiệp vụ và Chrome dùng H2. Frontend tsc/lint/build exit0. Không áp migration thật hoặc push repo trong giai đoạn này.
