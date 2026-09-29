# PetCare-Hub
java project

Các công cụ bắt buộc để chạy toàn bộ ứng dụng:
1. Java JDK 17 trở lên (backend dùng Java 17).
2. Node.js 18 trở lên và npm (frontend dùng React/Vite; npm đi kèm bộ cài Node.js).
3. PostgreSQL 15 trở lên. Tạo database tên `petcarehub` trước khi chạy backend.

Không cần cài riêng:
- Apache Maven: backend có Maven Wrapper (`mvnw.cmd`), tự tải Maven 3.9.9 khi chạy lần đầu. Cần kết nối Internet lần đầu.
- Spring Boot: Maven tự tải framework và các thư viện từ `pom.xml`.

Tùy chọn:
- Git, nếu cần clone hoặc cập nhật mã nguồn.
- Visual Studio Code hoặc IntelliJ IDEA để chỉnh sửa code.
- Postman để kiểm tra API.

Lưu ý: cấu hình phát triển mặc định kết nối PostgreSQL trên `localhost:5432`. Tài khoản/mật khẩu phải khớp với cấu hình trong `ppet/backend/src/main/resources/application-dev.yml`.
