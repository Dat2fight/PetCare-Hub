# Bằng chứng kiểm chứng giai đoạn 4

Ngày 2026-11-15, Chrome headless thật trên Windows. Backend Spring chạy profile test với H2 trong bộ nhớ, tài khoản bootstrap/mật khẩu ngẫu nhiên; frontend dùng Vite preview từ production build. Database tạm bị hủy khi dừng tiến trình. Không truy cập database ứng dụng, không gửi ra email/SMS.

Đã thực thi: đăng nhập → dashboard/chart/bảng số liệu → tải CSV BOM UTF-8 → cảnh báo mở kho low=true → tạo mẫu tiếng Việt → preview một người → xác nhận xếp hàng → đợi batch COMPLETED với 1 đã gửi/0 bỏ qua → trạng thái CONNECTED và worker SUCCESS. Kiểm cả 5 route dashboard/compose/templates/history/system ở 320, 360, 390, 1366px; document.scrollWidth không vượt viewport; không pageerror; Tab nằm trong ConfirmDialog và Escape đóng được.

Ảnh mẫu lưu trong repository:

- [Dashboard desktop](admin4-dashboard-1366.png)
- [Dashboard mobile](admin4-dashboard-320.png)
- [Soạn thông báo mobile](admin4-notifications-320.png)
- [Lịch sử thông báo đã hoàn tất](admin4-notifications-history-320.png)
- [Hệ thống mobile](admin4-system-320.png)

DISABLED ở migration là đúng cho H2/test; NEVER_RUN ở job nhắc là đúng vì đã tắt trong test, expiry có thể chưa đến lần chạy đầu. Phiên bản lấy từ Maven build-info. Mẫu dữ liệu trong ảnh chỉ dùng cho kiểm thử; không phải dữ liệu hoặc xác nhận triển khai production.

Kết quả kiểm tra mã:

```text
tools/test-sqlserver.ps1 → mvn test
Tests run: 295, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
npx tsc --noEmit: exit 0
npm run lint: exit 0
npm run build: exit 0
Chrome: PASS (CSV, template, async batch, telemetry, responsive, keyboard)
```

Các bài kiểm thử hồi quy lưu ở backend/src/test/java/com/petcarehub: AdminInsightsTest, AdminCsvHttpTest, NotificationConcurrencyTest, NotificationJobFailureTest; inventory toàn app ở EndpointSecurityMatrixTest, migration V20 ở FlywayMigrationTest. Script Chrome và log đầy đủ của lần chạy local nằm trong backend/target (không commit file sinh tự động). Không khẳng định đây là kiểm thử tải, khả năng chịu lỗi hạ tầng hoặc mọi interleaving SQL Server.
