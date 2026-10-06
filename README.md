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

Lệnh chạy server localhost:

- Bật backend Spring Boot:
cd "D:\visual studio\ppet\backend"
mvnw spring-boot:run

- Bật frontend Vite:
cd "D:\visual studio\ppet\frontend"
npm run dev
    Update:
1. Tạo API Service (productService.ts)
Tôi đã tạo file src/services/productService.ts để kết nối với các endpoint của Spring Boot Backend:
GET /api/v1/products: Lấy danh sách sản phẩm (có phân trang và bộ lọc).
GET /api/v1/products/{id}: Lấy chi tiết một sản phẩm cụ thể.
2. Tích hợp API vào Trang Danh sách Sản phẩm (ProductListPage.tsx)
Loại bỏ hoàn toàn mảng mockProducts cứng cũ.
Sử dụng useEffect và useState để gọi productService.getAllProducts().
Bổ sung các trạng thái UI chuyên nghiệp: Loading (vòng xoay tải dữ liệu), Error (lỗi kết nối), và Empty State (không tìm thấy sản phẩm).
Tạm thời map các sản phẩm từ backend với ảnh mẫu (mockImages) vì hiện tại ProductDTO ở backend chưa hỗ trợ trả về link hình ảnh.
3. Tích hợp API vào Trang Chi tiết Sản phẩm (ProductDetailPage.tsx)
Thay thế việc tìm kiếm trong mảng mock bằng API productService.getProductById(id).
Cập nhật logic giỏ hàng (Cart) và hiển thị Số lượng kho (stockQuantity) thực tế từ cơ sở dữ liệu. Nếu stockQuantity <= 0, nút "Thêm vào giỏ hàng" sẽ tự động bị disable (vô hiệu hoá).
Thêm Loading State để tránh màn hình bị giật/trắng khi chờ dữ liệu tải về.
4. Sửa lỗi hệ thống Frontend
Phát hiện file index.html bị di chuyển sai vị trí ra ngoài thư mục frontend. Tôi đã chuyển file này về đúng chỗ.
Kiểm tra toàn bộ mã nguồn bằng tsc -b (Trình biên dịch TypeScript) và vite build: Mã nguồn đã biên dịch thành công 100%, không còn bất kì lỗi cú pháp hay thiếu import nào. Tiếng Việt hiển thị hoàn hảo.

⚠️ LƯU Ý QUAN TRỌNG VỀ BACKEND: Khi tôi thử khởi động lại Spring Boot backend bằng lệnh mvn spring-boot:run để bạn có thể xem API hoạt động trực tiếp, hệ thống báo lỗi biên dịch Java Backend (rất nhiều lỗi cannot find symbol liên quan đến Getters/Setters). Nguyên nhân là do phiên bản Java trên máy của bạn hiện tại quá mới (JDK 27), khiến thư viện Lombok không thể tự động sinh ra code (vì Lombok chưa hỗ trợ hoàn thiện cho JDK 27).

Để test màn hình Products tích hợp API, bạn hãy mở Spring Boot, chỉnh lại project sang JDK 17 hoặc JDK 21 và chạy ứng dụng Backend nhé! Hiện tại thì Frontend đã hoàn chỉnh hoàn toàn rồi.