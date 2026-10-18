-- SQLCMD mode only. No admin account is created by this demo script.
:On Error exit
IF N'$(APP_PROFILE)' <> N'dev'
    THROW 50000, 'Demo seed requires explicit APP_PROFILE=dev', 1;
IF LEN(N'$(DEMO_PASSWORD_HASH)') <> 60 OR LEFT(N'$(DEMO_PASSWORD_HASH)', 4) NOT IN (N'$2a$', N'$2b$', N'$2y$')
    THROW 50000, 'Provide a BCrypt DEMO_PASSWORD_HASH through the environment', 1;
-- Run only on a fresh demo database, before starting application/bootstrap.
-- Apply schema and migrations 001, 002, 003 first: inventory uses filtered unique indexes.
IF EXISTS (SELECT 1 FROM dbo.users) OR EXISTS (SELECT 1 FROM dbo.roles)
    THROW 50000, 'Demo seed requires empty users and roles tables', 1;
GO

-- ==========================================================================================
-- PetCare Hub - Seed Data Script for Microsoft SQL Server
-- Target: SQL Server 2016+ / Azure SQL Database
-- Notes:
-- - Uses SET IDENTITY_INSERT where explicit IDs are inserted to ensure deterministic foreign keys
-- ==========================================================================================

-- ------------------------------------------------------------------------------------------
-- 1. SEED ROLES
-- ------------------------------------------------------------------------------------------
SET IDENTITY_INSERT dbo.roles ON;

INSERT INTO dbo.roles (id, name, description) VALUES
(1, 'ADMIN', N'Quản trị viên toàn quyền hệ thống'),
(2, 'CUSTOMER', N'Khách hàng mua sắm và sử dụng dịch vụ'),
(3, 'SALES_STAFF', N'Nhân viên bán hàng và tư vấn khách hàng'),
(4, 'WAREHOUSE_STAFF', N'Nhân viên kho quản lý xuất nhập tồn'),
(5, 'VETERINARIAN', N'Bác sĩ thú y khám chữa bệnh và tiêm chủng');

SET IDENTITY_INSERT dbo.roles OFF;
GO

-- ------------------------------------------------------------------------------------------
-- 2. SEED DEMO USERS (credentials supplied by operator)
-- ------------------------------------------------------------------------------------------
SET IDENTITY_INSERT dbo.users ON;

INSERT INTO dbo.users (id, username, email, password_hash, first_name, last_name, phone, avatar_url, enabled) VALUES
(2, 'sales01', 'sales@petcarehub.com', '$(DEMO_PASSWORD_HASH)', N'Thị Mai', N'Nguyễn', '0900000002', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150', 1),
(3, 'warehouse01', 'warehouse@petcarehub.com', '$(DEMO_PASSWORD_HASH)', N'Văn Khoa', N'Trần', '0900000003', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150', 1),
(4, 'vet01', 'vet@petcarehub.com', '$(DEMO_PASSWORD_HASH)', N'Đức Huy', N'Bác sĩ Lê', '0900000004', 'https://images.unsplash.com/photo-1622253692010-333f2da6031d?w=150', 1),
(5, 'customer01', 'customer@gmail.com', '$(DEMO_PASSWORD_HASH)', N'Văn An', N'Hoàng', '0988776655', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150', 1);

SET IDENTITY_INSERT dbo.users OFF;
GO

-- ------------------------------------------------------------------------------------------
-- 3. SEED USER ROLES
-- ------------------------------------------------------------------------------------------
INSERT INTO dbo.user_roles (user_id, role_id) VALUES
(2, 3), -- sales01 -> SALES_STAFF
(3, 4), -- warehouse01 -> WAREHOUSE_STAFF
(4, 5), -- vet01 -> VETERINARIAN
(5, 2); -- customer01 -> CUSTOMER
GO

-- ------------------------------------------------------------------------------------------
-- 4. SEED SPECIES & BREEDS
-- ------------------------------------------------------------------------------------------
SET IDENTITY_INSERT dbo.species ON;

INSERT INTO dbo.species (id, name, description) VALUES
(1, N'Chó (Dog)', N'Các giống chó cảnh và chó nuôi trong gia đình'),
(2, N'Mèo (Cat)', N'Các giống mèo cảnh thuần chủng và lai'),
(3, N'Chim (Bird)', N'Chim cảnh hót và chim cảnh màu sắc'),
(4, N'Cá (Fish)', N'Cá thủy sinh và cá cảnh nước ngọt'),
(5, N'Bò sát (Reptile)', N'Thằn lằn, trăn, rùa cảnh'),
(6, N'Thú nhỏ (Small Animal)', N'Chuột Hamster, bọ ú, thỏ cảnh');

SET IDENTITY_INSERT dbo.species OFF;
GO

SET IDENTITY_INSERT dbo.breeds ON;

INSERT INTO dbo.breeds (id, species_id, name, description, avg_lifespan, size_category, energy_level, grooming_needs, good_with_children, noise_level) VALUES
(1, 1, N'Chó Corgi', N'Chó chân ngắn mông trái tim, thân thiện và lanh lợi', N'12-15 năm', N'Medium', N'High', N'High', 1, N'Medium'),
(2, 1, N'Chó Poodle', N'Thông minh, không rụng lông, dễ huấn luyện', N'12-15 năm', N'Small', N'Medium', N'High', 1, N'Low'),
(3, 1, N'Chó Golden Retriever', N'Cực kỳ trung thành, tình cảm, thích chạy nhảy', N'10-12 năm', N'Large', N'High', N'High', 1, N'Medium'),
(4, 2, N'Mèo Anh Lông Ngắn', N'Mặt bánh bao, tính cách điềm đạm, thích vuốt ve', N'12-17 năm', N'Medium', N'Low', N'Medium', 1, N'Low'),
(5, 2, N'Mèo Ba Tư', N'Mặt tịt, lông dài quý phái, yên tĩnh', N'12-15 năm', N'Medium', N'Low', N'High', 1, N'Low'),
(6, 3, N'Vẹt Cockatiel', N'Vẹt má hồng thông minh biết huýt sáo', N'10-14 năm', N'Small', N'Medium', N'Low', 1, N'Medium'),
(7, 6, N'Chuột Hamster Bear', N'Dễ thương, năng động ban đêm, ăn hạt', N'2-3 năm', N'Small', N'Medium', N'Low', 0, N'Low');

SET IDENTITY_INSERT dbo.breeds OFF;
GO

-- ------------------------------------------------------------------------------------------
-- 5. SEED PETS
-- ------------------------------------------------------------------------------------------
SET IDENTITY_INSERT dbo.pets ON;

INSERT INTO dbo.pets (id, name, species_id, breed_id, owner_id, date_of_birth, gender, color, weight, price, description, image_url, health_status, availability_status) VALUES
(1, N'Bé Corgi Vàng Trắng', 1, 1, NULL, '2026-02-15', N'Đực', N'Vàng Trắng', 3.50, 9500000.00, N'Bé Corgi chân ngắn siêu đáng yêu, đã biết ăn hạt và đi vệ sinh đúng chỗ.', 'https://images.unsplash.com/photo-1552053831-71594a27632d?w=500', 'HEALTHY', 'AVAILABLE'),
(2, N'Bé Poodle Nâu Đỏ', 1, 2, NULL, '2026-01-10', N'Cái', N'Nâu Đỏ', 2.20, 6000000.00, N'Bé Poodle size tiny, màu nâu đỏ cực xinh xắn, ngoan ngoãn.', 'https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=500', 'HEALTHY', 'AVAILABLE'),
(3, N'Mèo ALN Xám Xanh', 2, 4, NULL, '2026-01-20', N'Đực', N'Xám Xanh', 1.80, 5500000.00, N'Bé ALN màu xám xanh, mặt bánh bao, cực kỳ quấn người.', 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=500', 'HEALTHY', 'AVAILABLE'),
(4, N'Golden Retriever Thuần Chủng', 1, 3, NULL, '2026-02-01', N'Đực', N'Vàng Kim', 6.00, 12000000.00, N'Bé Golden khung to, mặt vuông, form chuẩn chó đi show.', 'https://images.unsplash.com/photo-1552053831-71594a27632d?w=500', 'HEALTHY', 'AVAILABLE'),
(5, N'Mèo Ba Tư Trắng Tuyết', 2, 5, NULL, '2025-12-05', N'Cái', N'Trắng', 2.50, 8000000.00, N'Bé mèo Ba Tư mặt tịt, lông dài óng mượt.', 'https://images.unsplash.com/photo-1518791841217-8f162f1e1131?w=500', 'DUE_FOR_VACCINATION', 'AVAILABLE'),
(6, N'Milo (Cún cưng của An)', 1, 1, 5, '2025-06-10', N'Đực', N'Vàng Trắng', 11.50, 0.00, N'Thú cưng thuộc sở hữu của khách hàng Hoàng Văn An.', 'https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=500', 'HEALTHY', 'SOLD');

SET IDENTITY_INSERT dbo.pets OFF;
GO

-- ------------------------------------------------------------------------------------------
-- 6. SEED PRODUCT CATEGORIES & PRODUCTS
-- ------------------------------------------------------------------------------------------
SET IDENTITY_INSERT dbo.product_categories ON;

INSERT INTO dbo.product_categories (id, name, description, parent_category_id) VALUES
(1, N'Thức ăn thú cưng', N'Thức ăn hạt, pate, súp thưởng và snack dinh dưỡng', NULL),
(2, N'Đồ chơi', N'Đồ chơi gặm cắn, bóng, cần câu mèo và bàn cào móng', NULL),
(3, N'Phụ kiện & Chuồng', N'Vòng cổ, dây dắt, lồng vận chuyển, nệm nằm', NULL),
(4, N'Vệ sinh & Chăm sóc', N'Cát vệ sinh, sữa tắm, lược chải lông, kéo cắt móng', NULL);

SET IDENTITY_INSERT dbo.product_categories OFF;
GO

SET IDENTITY_INSERT dbo.products ON;

INSERT INTO dbo.products (id, name, description, price, image_url, category_id, sku, weight, dimensions, active) VALUES
(1, N'Thức ăn Royal Canin Adult Cat 10kg', N'Thức ăn hạt khô dành cho mèo trưởng thành. Giúp duy trì vóc dáng, làm mượt lông.', 1300000.00, 'https://images.unsplash.com/photo-1589924691995-400dc9ecc119?w=500', 1, 'RC-CAT-10KG', 10.00, N'40x30x15 cm', 1),
(2, N'Vòng cổ da cao cấp có chuông', N'Vòng cổ da thật an toàn, không kích ứng da cổ thú cưng, kèm chuông nhỏ.', 250000.00, 'https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=500', 3, 'ACC-COLLAR-01', 0.15, N'30x2 cm', 1),
(3, N'Đồ chơi bóng cao su nhai gặm', N'Bóng cao su thiên nhiên giúp làm sạch răng miệng và xả stress cho cún.', 120000.00, 'https://images.unsplash.com/photo-1576201836106-db1758fd1c97?w=500', 2, 'TOY-BALL-RUB', 0.20, N'8x8x8 cm', 1),
(4, N'Lồng vận chuyển hàng không size M', N'Lồng đạt tiêu chuẩn hàng không quốc tế IATA, nhựa ABS bền bỉ chống va đập.', 800000.00, 'https://images.unsplash.com/photo-1541781774459-bb2af2f05b55?w=500', 3, 'CRATE-IATA-M', 3.50, N'60x40x40 cm', 1);

SET IDENTITY_INSERT dbo.products OFF;
GO

-- ------------------------------------------------------------------------------------------
-- 7. SEED INVENTORY
-- ------------------------------------------------------------------------------------------
SET IDENTITY_INSERT dbo.inventory ON;

INSERT INTO dbo.inventory (id, pet_id, product_id, quantity, reorder_point, reorder_quantity, location) VALUES
(1, 1, NULL, 1, 1, 1, N'Khu Thú Cưng A1'),
(2, 2, NULL, 1, 1, 1, N'Khu Thú Cưng A2'),
(3, 3, NULL, 1, 1, 1, N'Khu Thú Cưng B1'),
(4, 4, NULL, 1, 1, 1, N'Khu Thú Cưng A3'),
(5, 5, NULL, 1, 1, 1, N'Khu Thú Cưng B2'),
(6, NULL, 1, 45, 10, 50, N'Kho 1 - Kệ F1'),
(7, NULL, 2, 80, 15, 50, N'Kho 1 - Kệ A2'),
(8, NULL, 3, 120, 20, 100, N'Kho 1 - Kệ T1'),
(9, NULL, 4, 15, 5, 20, N'Kho 2 - Khu C');

SET IDENTITY_INSERT dbo.inventory OFF;
GO

-- ------------------------------------------------------------------------------------------
-- 8. SEED SERVICES & CARE PACKAGES
-- ------------------------------------------------------------------------------------------
SET IDENTITY_INSERT dbo.services ON;

INSERT INTO dbo.services (id, name, description, service_type, duration_minutes, price, active) VALUES
(1, N'Tắm và vệ sinh cơ bản (Basic Grooming)', N'Tắm sấy khử mùi, vệ sinh tai, vắt tuyến hôi, cắt mài móng', 'GROOMING', 60, 200000.00, 1),
(2, N'Cắt tỉa tạo kiểu trọn gói (Full Grooming)', N'Gói tắm sấy + cắt tỉa nghệ thuật theo yêu cầu', 'GROOMING', 120, 450000.00, 1),
(3, N'Khám sức khỏe tổng quát', N'Kiểm tra cân nặng, mắt, tai, mũi, họng, nghe tim phổi', 'VETERINARY', 30, 150000.00, 1),
(4, N'Tiêm phòng vaccine phòng bệnh', N'Tiêm phòng vaccine 5 bệnh, 7 bệnh hoặc dại có sổ theo dõi', 'VETERINARY', 30, 250000.00, 1);

SET IDENTITY_INSERT dbo.services OFF;
GO

SET IDENTITY_INSERT dbo.care_packages ON;

INSERT INTO dbo.care_packages (id, name, description, price, duration_months, includes_grooming, includes_checkup, includes_vaccination, active) VALUES
(1, N'Gói Chăm Sóc Cơ Bản (Basic Care)', N'Bao gồm 1 lần khám tổng quát mỗi tháng', 299000.00, 1, 0, 1, 0, 1),
(2, N'Gói Chăm Sóc Tiêu Chuẩn (Standard Care)', N'Bao gồm 1 lần khám tổng quát + 1 lần tắm tỉa vệ sinh', 499000.00, 1, 1, 1, 0, 1),
(3, N'Gói Chăm Sóc Toàn Diện (Premium Care)', N'Bao gồm khám định kỳ + tắm tỉa không giới hạn + nhắc tiêm miễn phí', 899000.00, 1, 1, 1, 1, 1);

SET IDENTITY_INSERT dbo.care_packages OFF;
GO

-- ------------------------------------------------------------------------------------------
-- 9. SEED PROMOTIONS
-- ------------------------------------------------------------------------------------------
SET IDENTITY_INSERT dbo.promotions ON;

INSERT INTO dbo.promotions (id, code, description, discount_type, discount_value, min_order_amount, max_uses, current_uses, start_date, end_date, active) VALUES
(1, 'SUMMER20', N'Giảm 20% cho đơn hàng mùa hè từ 500k', 'PERCENTAGE', 20.00, 500000.00, 500, 12, '2026-05-01', '2026-08-31', 1),
(2, 'WELCOME100K', N'Giảm trực tiếp 100.000đ cho khách hàng mới', 'FIXED_AMOUNT', 100000.00, 1000000.00, 1000, 45, '2026-01-01', '2026-12-31', 1);

SET IDENTITY_INSERT dbo.promotions OFF;
GO

-- ------------------------------------------------------------------------------------------
-- 10. SEED LOYALTY ACCOUNT CHO CUSTOMER01
-- ------------------------------------------------------------------------------------------
SET IDENTITY_INSERT dbo.loyalty_accounts ON;

INSERT INTO dbo.loyalty_accounts (id, customer_id, total_points, available_points, membership_level) VALUES
(1, 5, 1250, 1250, 'GOLD');

SET IDENTITY_INSERT dbo.loyalty_accounts OFF;
GO

INSERT INTO dbo.loyalty_transactions (loyalty_account_id, transaction_type, points, source_type, source_id, description) VALUES
(1, 'EARN', 500, 'PURCHASE', NULL, N'Tích điểm mua thú cưng Milo'),
(1, 'EARN', 250, 'SERVICE_BOOKING', NULL, N'Tích điểm dịch vụ Full Grooming'),
(1, 'EARN', 500, 'CARE_PACKAGE', NULL, N'Đăng ký gói Standard Care');
GO

-- ------------------------------------------------------------------------------------------
-- 11. SEED SAMPLE MEDICAL RECORD & VACCINATION CHO PET #6 (MILO)
-- ------------------------------------------------------------------------------------------
SET IDENTITY_INSERT dbo.medical_records ON;

INSERT INTO dbo.medical_records (id, pet_id, veterinarian_id, appointment_id, examination_date, findings, diagnosis, recommendations) VALUES
(1, 6, 4, NULL, '2026-04-01', N'Thể trạng tốt, răng sạch, tai khô ráo không ve rận.', N'Sức khỏe bình thường, phát triển cân đối.', N'Duy trì chế độ ăn giàu đạm, dắt đi bộ 30 phút mỗi ngày.');

SET IDENTITY_INSERT dbo.medical_records OFF;
GO

SET IDENTITY_INSERT dbo.vaccinations ON;

INSERT INTO dbo.vaccinations (id, pet_id, veterinarian_id, vaccine_name, vaccine_batch_number, date_administered, next_due_date, notes) VALUES
(1, 6, 4, N'Vaccine 7 bệnh Vanguard Plus 5/L', 'VG-2026-889', '2026-04-01', '2027-04-01', N'Tiêm nhắc định kỳ hàng năm. Không có phản ứng phụ.');

SET IDENTITY_INSERT dbo.vaccinations OFF;
GO

