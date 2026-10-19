# PetCare-Hub

Hệ thống quản lý cửa hàng và dịch vụ chăm sóc thú cưng: React/Vite, Spring Boot và SQL Server.

**[Hướng dẫn chạy chi tiết bằng PowerShell](ppet/README.md)** là tài liệu vận hành chính: chuẩn bị database, cấp secret, chạy backend/frontend, test/build và xử lý sự cố.

## Mục lục

- [Kiến trúc](#kiến-trúc)
- [Bắt đầu](#bắt-đầu)
- [Kết quả kiểm chứng](#kết-quả-kiểm-chứng)
- [Tài liệu liên quan](#tài-liệu-liên-quan)

## Kiến trúc

| Thành phần | Công nghệ | Địa chỉ local |
|---|---|---|
| Frontend | React 18, TypeScript, Vite 7 | http://localhost:5173/PetCare-Hub/ |
| Backend | Java target 17, Spring Boot 3.3.13 | http://localhost:8081/api/v1 |
| Database | Microsoft SQL Server | localhost:1433, database petcarehub |

Vite chuyển tiếp /api và /ws tới backend; backend có context path /.

## Bắt đầu

Theo [hướng dẫn đầy đủ](ppet/README.md): database → môi trường backend → backend → frontend. Spring Boot **không tự đọc .env**. Phải cấp JWT_SECRET mới, ngẫu nhiên, cho chính tiến trình backend; không có khóa mặc định.

Không chạy script reset lên dữ liệu đang dùng. Không dùng mật khẩu/khóa từng xuất hiện trong tài liệu hay lịch sử Git.

## Kết quả kiểm chứng

Ngày 19/10/2026, nhánh `fix/critical-jwt-secret`: backend và frontend chạy được; đăng ký/đăng nhập/me có token trả 200, me không token trả 401; trang sản phẩm hiển thị dữ liệu SQL Server. Maven: 172 test, 169 pass, 3 skip. Frontend build và lint pass.

Có một thay đổi cấu hình Vite tối thiểu để SockJS chạy trong dev. Database hiện có chưa áp dụng migration 001–003; chưa kiểm chứng luồng đặt lịch qua UI. Chi tiết ở [báo cáo chạy local](ppet/docs/local-run-20261019.md).

## Tài liệu liên quan

- [Hướng dẫn vận hành](ppet/README.md)
- [Kiến trúc](ppet/docs/architecture.md), [database](ppet/docs/database.md), [API](ppet/docs/api.md)
- [JWT bắt buộc và xoay khóa](ppet/docs/critical-jwt-secret/REPORT.md)
- [Báo cáo High lượt 3 và việc còn lại](ppet/docs/high-round-3/REPORT.md)
