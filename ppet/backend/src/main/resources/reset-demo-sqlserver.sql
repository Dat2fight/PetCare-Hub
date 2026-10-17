-- CẢNH BÁO: XÓA TOÀN BỘ DỮ LIỆU DEMO. KHÔNG DÙNG CHO DATABASE THẬT.
-- Chỉ chạy thủ công trên database có hậu tố _demo, sau khi đặt:
-- EXEC sys.sp_set_session_context @key=N'allow_petcare_reset', @value=1;
IF RIGHT(DB_NAME(), 5) <> '_demo' OR COALESCE(TRY_CONVERT(int, SESSION_CONTEXT(N'allow_petcare_reset')), 0) <> 1
    THROW 50001, N'Từ chối reset: cần database *_demo và cờ allow_petcare_reset=1.', 1;
ELSE
BEGIN
IF OBJECT_ID('dbo.user_roles', 'U') IS NOT NULL DROP TABLE dbo.user_roles;
IF OBJECT_ID('dbo.user_addresses', 'U') IS NOT NULL DROP TABLE dbo.user_addresses;
IF OBJECT_ID('dbo.notifications', 'U') IS NOT NULL DROP TABLE dbo.notifications;
IF OBJECT_ID('dbo.loyalty_transactions', 'U') IS NOT NULL DROP TABLE dbo.loyalty_transactions;
IF OBJECT_ID('dbo.loyalty_accounts', 'U') IS NOT NULL DROP TABLE dbo.loyalty_accounts;
IF OBJECT_ID('dbo.promotion_usages', 'U') IS NOT NULL DROP TABLE dbo.promotion_usages;
IF OBJECT_ID('dbo.promotions', 'U') IS NOT NULL DROP TABLE dbo.promotions;
IF OBJECT_ID('dbo.reviews', 'U') IS NOT NULL DROP TABLE dbo.reviews;
IF OBJECT_ID('dbo.treatments', 'U') IS NOT NULL DROP TABLE dbo.treatments;
IF OBJECT_ID('dbo.vaccinations', 'U') IS NOT NULL DROP TABLE dbo.vaccinations;
IF OBJECT_ID('dbo.medical_records', 'U') IS NOT NULL DROP TABLE dbo.medical_records;
IF OBJECT_ID('dbo.appointments', 'U') IS NOT NULL DROP TABLE dbo.appointments;
IF OBJECT_ID('dbo.services', 'U') IS NOT NULL DROP TABLE dbo.services;
IF OBJECT_ID('dbo.care_package_subscriptions', 'U') IS NOT NULL DROP TABLE dbo.care_package_subscriptions;
IF OBJECT_ID('dbo.care_packages', 'U') IS NOT NULL DROP TABLE dbo.care_packages;
IF OBJECT_ID('dbo.pet_matching_results', 'U') IS NOT NULL DROP TABLE dbo.pet_matching_results;
IF OBJECT_ID('dbo.pet_matching_quizzes', 'U') IS NOT NULL DROP TABLE dbo.pet_matching_quizzes;
IF OBJECT_ID('dbo.order_items', 'U') IS NOT NULL DROP TABLE dbo.order_items;
IF OBJECT_ID('dbo.orders', 'U') IS NOT NULL DROP TABLE dbo.orders;
IF OBJECT_ID('dbo.cart_items', 'U') IS NOT NULL DROP TABLE dbo.cart_items;
IF OBJECT_ID('dbo.carts', 'U') IS NOT NULL DROP TABLE dbo.carts;
IF OBJECT_ID('dbo.inventory_transactions', 'U') IS NOT NULL DROP TABLE dbo.inventory_transactions;
IF OBJECT_ID('dbo.inventory', 'U') IS NOT NULL DROP TABLE dbo.inventory;
IF OBJECT_ID('dbo.product_images', 'U') IS NOT NULL DROP TABLE dbo.product_images;
IF OBJECT_ID('dbo.products', 'U') IS NOT NULL DROP TABLE dbo.products;
IF OBJECT_ID('dbo.product_categories', 'U') IS NOT NULL DROP TABLE dbo.product_categories;
IF OBJECT_ID('dbo.pet_images', 'U') IS NOT NULL DROP TABLE dbo.pet_images;
IF OBJECT_ID('dbo.pets', 'U') IS NOT NULL DROP TABLE dbo.pets;
IF OBJECT_ID('dbo.breeds', 'U') IS NOT NULL DROP TABLE dbo.breeds;
IF OBJECT_ID('dbo.species', 'U') IS NOT NULL DROP TABLE dbo.species;
IF OBJECT_ID('dbo.users', 'U') IS NOT NULL DROP TABLE dbo.users;
IF OBJECT_ID('dbo.roles', 'U') IS NOT NULL DROP TABLE dbo.roles;
END;
GO
-- Sau reset, chạy schema-sqlserver.sql rồi seed demo nếu cần.
