-- ==========================================================================================
-- PetCare Hub - Database Schema Script for Microsoft SQL Server
-- Target: SQL Server 2016+ / Azure SQL Database
-- Naming Convention: snake_case, plural table names
-- Character Set: NVARCHAR for international and Vietnamese characters
-- Standard Audit Columns: created_at, updated_at, deleted_at (Soft Delete)
-- ==========================================================================================

-- Đảm bảo sử dụng database petcarehub (nếu đã tạo)
--USE petcarehub;
--GO

-- ------------------------------------------------------------------------------------------
-- 1. ROLES & USERS & SECURITY
-- ------------------------------------------------------------------------------------------

GO

-- Bảng roles: Danh mục vai trò trong hệ thống
IF OBJECT_ID(N'dbo.roles', N'U') IS NULL
CREATE TABLE dbo.roles (
    id BIGINT IDENTITY(1,1) NOT NULL,
    name VARCHAR(50) NOT NULL,
    description NVARCHAR(255) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_roles_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_roles_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_roles PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_roles_name UNIQUE (name),
    CONSTRAINT CK_roles_name CHECK (name IN ('CUSTOMER', 'SALES_STAFF', 'WAREHOUSE_STAFF', 'VETERINARIAN', 'ADMIN'))
);
GO

-- Bảng users: Tài khoản người dùng (khách hàng, nhân viên, bác sĩ, quản trị)
IF OBJECT_ID(N'dbo.users', N'U') IS NULL
CREATE TABLE dbo.users (
    id BIGINT IDENTITY(1,1) NOT NULL,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name NVARCHAR(100) NOT NULL,
    last_name NVARCHAR(100) NOT NULL,
    phone VARCHAR(20) NULL,
    avatar_url VARCHAR(500) NULL,
    enabled BIT NOT NULL CONSTRAINT DF_users_enabled DEFAULT 1,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_users_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_users_updated_at DEFAULT SYSUTCDATETIME(),
    deleted_at DATETIME2(0) NULL,
    CONSTRAINT PK_users PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_users_username UNIQUE (username),
    CONSTRAINT UQ_users_email UNIQUE (email)
);
GO

-- Bảng user_roles: Bảng liên kết N-N giữa người dùng và vai trò
IF OBJECT_ID(N'dbo.user_roles', N'U') IS NULL
CREATE TABLE dbo.user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at DATETIME2(0) NOT NULL CONSTRAINT DF_user_roles_assigned_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_user_roles PRIMARY KEY CLUSTERED (user_id, role_id),
    CONSTRAINT FK_user_roles_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE,
    CONSTRAINT FK_user_roles_role FOREIGN KEY (role_id) REFERENCES dbo.roles(id) ON DELETE CASCADE
);
GO

-- [ĐỀ XUẤT THÊM] Bảng user_addresses: Sổ địa chỉ giao hàng của khách hàng
IF OBJECT_ID(N'dbo.user_addresses', N'U') IS NULL
CREATE TABLE dbo.user_addresses (
    id BIGINT IDENTITY(1,1) NOT NULL,
    user_id BIGINT NOT NULL,
    recipient_name NVARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    street_address NVARCHAR(255) NOT NULL,
    ward NVARCHAR(100) NULL,
    district NVARCHAR(100) NOT NULL,
    city NVARCHAR(100) NOT NULL,
    is_default BIT NOT NULL CONSTRAINT DF_user_addresses_is_default DEFAULT 0,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_user_addresses_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_user_addresses_updated_at DEFAULT SYSUTCDATETIME(),
    deleted_at DATETIME2(0) NULL,
    CONSTRAINT PK_user_addresses PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_user_addresses_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE
);
GO

-- ------------------------------------------------------------------------------------------
-- 2. SPECIES, BREEDS & PETS
-- ------------------------------------------------------------------------------------------

-- Bảng species: Danh mục Loài thú cưng (Chó, Mèo, Chim, Cá...)
IF OBJECT_ID(N'dbo.species', N'U') IS NULL
CREATE TABLE dbo.species (
    id BIGINT IDENTITY(1,1) NOT NULL,
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(MAX) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_species_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_species_updated_at DEFAULT SYSUTCDATETIME(),
    deleted_at DATETIME2(0) NULL,
    CONSTRAINT PK_species PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_species_name UNIQUE (name)
);
GO

-- Bảng breeds: Danh mục Giống loài thú cưng
IF OBJECT_ID(N'dbo.breeds', N'U') IS NULL
CREATE TABLE dbo.breeds (
    id BIGINT IDENTITY(1,1) NOT NULL,
    species_id BIGINT NOT NULL,
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(MAX) NULL,
    avg_lifespan NVARCHAR(50) NULL,
    size_category NVARCHAR(50) NULL,
    energy_level NVARCHAR(50) NULL,
    grooming_needs NVARCHAR(50) NULL,
    good_with_children BIT NULL,
    noise_level NVARCHAR(50) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_breeds_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_breeds_updated_at DEFAULT SYSUTCDATETIME(),
    deleted_at DATETIME2(0) NULL,
    CONSTRAINT PK_breeds PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_breeds_species FOREIGN KEY (species_id) REFERENCES dbo.species(id) ON DELETE NO ACTION
);
GO

-- Bảng pets: Hồ sơ thú cưng trong cửa hàng hoặc thú cưng của khách
IF OBJECT_ID(N'dbo.pets', N'U') IS NULL
CREATE TABLE dbo.pets (
    id BIGINT IDENTITY(1,1) NOT NULL,
    name NVARCHAR(100) NULL,
    species_id BIGINT NOT NULL,
    breed_id BIGINT NULL,
    owner_id BIGINT NULL,
    date_of_birth DATE NULL,
    gender NVARCHAR(20) NULL,
    color NVARCHAR(100) NULL,
    weight DECIMAL(5,2) NULL,
    price DECIMAL(18,2) NOT NULL CONSTRAINT DF_pets_price DEFAULT 0.00,
    description NVARCHAR(MAX) NULL,
    image_url VARCHAR(500) NULL,
    health_status VARCHAR(50) NOT NULL CONSTRAINT DF_pets_health_status DEFAULT 'HEALTHY',
    availability_status VARCHAR(50) NOT NULL CONSTRAINT DF_pets_avail_status DEFAULT 'AVAILABLE',
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_pets_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_pets_updated_at DEFAULT SYSUTCDATETIME(),
    deleted_at DATETIME2(0) NULL,
    CONSTRAINT PK_pets PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_pets_species FOREIGN KEY (species_id) REFERENCES dbo.species(id) ON DELETE NO ACTION,
    CONSTRAINT FK_pets_breed FOREIGN KEY (breed_id) REFERENCES dbo.breeds(id) ON DELETE NO ACTION,
    CONSTRAINT FK_pets_owner FOREIGN KEY (owner_id) REFERENCES dbo.users(id) ON DELETE NO ACTION,
    CONSTRAINT CK_pets_health_status CHECK (health_status IN ('HEALTHY', 'DUE_FOR_VACCINATION', 'UNDER_TREATMENT', 'RECOVERING', 'HEALTH_WARNING')),
    CONSTRAINT CK_pets_avail_status CHECK (availability_status IN ('AVAILABLE', 'SOLD', 'RESERVED', 'UNAVAILABLE'))
);
GO

-- [ĐỀ XUẤT THÊM] Bảng pet_images: Thư viện nhiều hình ảnh cho thú cưng
IF OBJECT_ID(N'dbo.pet_images', N'U') IS NULL
CREATE TABLE dbo.pet_images (
    id BIGINT IDENTITY(1,1) NOT NULL,
    pet_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    is_primary BIT NOT NULL CONSTRAINT DF_pet_images_is_primary DEFAULT 0,
    display_order INT NOT NULL CONSTRAINT DF_pet_images_display_order DEFAULT 0,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_pet_images_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_pet_images PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_pet_images_pet FOREIGN KEY (pet_id) REFERENCES dbo.pets(id) ON DELETE CASCADE
);
GO

-- ------------------------------------------------------------------------------------------
-- 3. PRODUCT & CATEGORIES
-- ------------------------------------------------------------------------------------------

-- Bảng product_categories: Phân cấp danh mục sản phẩm (hỗ trợ cha-con)
IF OBJECT_ID(N'dbo.product_categories', N'U') IS NULL
CREATE TABLE dbo.product_categories (
    id BIGINT IDENTITY(1,1) NOT NULL,
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(MAX) NULL,
    parent_category_id BIGINT NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_prod_cat_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_prod_cat_updated_at DEFAULT SYSUTCDATETIME(),
    deleted_at DATETIME2(0) NULL,
    CONSTRAINT PK_product_categories PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_product_categories_parent FOREIGN KEY (parent_category_id) REFERENCES dbo.product_categories(id) ON DELETE NO ACTION
);
GO

-- Bảng products: Sản phẩm (thức ăn, đồ chơi, vật dụng, phụ kiện)
IF OBJECT_ID(N'dbo.products', N'U') IS NULL
CREATE TABLE dbo.products (
    id BIGINT IDENTITY(1,1) NOT NULL,
    name NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX) NULL,
    price DECIMAL(18,2) NOT NULL CONSTRAINT DF_products_price DEFAULT 0.00,
    image_url VARCHAR(500) NULL,
    category_id BIGINT NOT NULL,
    sku VARCHAR(100) NOT NULL,
    weight DECIMAL(10,2) NULL,
    dimensions NVARCHAR(100) NULL,
    active BIT NOT NULL CONSTRAINT DF_products_active DEFAULT 1,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_products_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_products_updated_at DEFAULT SYSUTCDATETIME(),
    deleted_at DATETIME2(0) NULL,
    CONSTRAINT PK_products PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_products_sku UNIQUE (sku),
    CONSTRAINT FK_products_category FOREIGN KEY (category_id) REFERENCES dbo.product_categories(id) ON DELETE NO ACTION
);
GO

-- [ĐỀ XUẤT THÊM] Bảng product_images: Bộ sưu tập hình ảnh chi tiết của sản phẩm
IF OBJECT_ID(N'dbo.product_images', N'U') IS NULL
CREATE TABLE dbo.product_images (
    id BIGINT IDENTITY(1,1) NOT NULL,
    product_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    is_primary BIT NOT NULL CONSTRAINT DF_prod_images_is_primary DEFAULT 0,
    display_order INT NOT NULL CONSTRAINT DF_prod_images_display_order DEFAULT 0,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_prod_images_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_product_images PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_product_images_product FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE CASCADE
);
GO

-- ------------------------------------------------------------------------------------------
-- 4. INVENTORY & TRANSACTIONS
-- ------------------------------------------------------------------------------------------

-- Bảng inventory: Tồn kho hiện tại của sản phẩm hoặc thú cưng (Polymorphic tracking)
IF OBJECT_ID(N'dbo.inventory', N'U') IS NULL
CREATE TABLE dbo.inventory (
    id BIGINT IDENTITY(1,1) NOT NULL,
    pet_id BIGINT NULL,
    product_id BIGINT NULL,
    quantity INT NOT NULL CONSTRAINT DF_inventory_quantity DEFAULT 0,
    reorder_point INT NULL CONSTRAINT DF_inventory_reorder_point DEFAULT 5,
    reorder_quantity INT NULL CONSTRAINT DF_inventory_reorder_qty DEFAULT 20,
    location NVARCHAR(100) NULL,
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_inventory_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_inventory PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_inventory_pet UNIQUE (pet_id),
    CONSTRAINT UQ_inventory_product UNIQUE (product_id),
    CONSTRAINT FK_inventory_pet FOREIGN KEY (pet_id) REFERENCES dbo.pets(id) ON DELETE CASCADE,
    CONSTRAINT FK_inventory_product FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE CASCADE,
    CONSTRAINT CK_inventory_polymorphic CHECK (
        (pet_id IS NOT NULL AND product_id IS NULL) OR 
        (pet_id IS NULL AND product_id IS NOT NULL)
    )
);
GO

-- Bảng inventory_transactions: Nhật ký kiểm toán xuất/nhập/điều chỉnh tồn kho
IF OBJECT_ID(N'dbo.inventory_transactions', N'U') IS NULL
CREATE TABLE dbo.inventory_transactions (
    id BIGINT IDENTITY(1,1) NOT NULL,
    inventory_id BIGINT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    reference_type VARCHAR(50) NULL,
    reference_id BIGINT NULL,
    notes NVARCHAR(MAX) NULL,
    performed_by BIGINT NOT NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_inv_tx_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_inventory_transactions PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_inv_tx_inventory FOREIGN KEY (inventory_id) REFERENCES dbo.inventory(id) ON DELETE CASCADE,
    CONSTRAINT FK_inv_tx_user FOREIGN KEY (performed_by) REFERENCES dbo.users(id) ON DELETE NO ACTION,
    CONSTRAINT CK_inv_tx_type CHECK (transaction_type IN ('STOCK_IN', 'STOCK_OUT', 'ADJUSTMENT', 'RETURN'))
);
GO

-- ------------------------------------------------------------------------------------------
-- 5. SHOPPING CART & ITEMS
-- ------------------------------------------------------------------------------------------

-- Bảng carts: Giỏ hàng trực tuyến của khách hàng (1-1 với User)
IF OBJECT_ID(N'dbo.carts', N'U') IS NULL
CREATE TABLE dbo.carts (
    id BIGINT IDENTITY(1,1) NOT NULL,
    customer_id BIGINT NOT NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_carts_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_carts_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_carts PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_carts_customer UNIQUE (customer_id),
    CONSTRAINT FK_carts_customer FOREIGN KEY (customer_id) REFERENCES dbo.users(id) ON DELETE CASCADE
);
GO

-- Bảng cart_items: Mặt hàng trong giỏ (hỗ trợ cả Product và Pet)
IF OBJECT_ID(N'dbo.cart_items', N'U') IS NULL
CREATE TABLE dbo.cart_items (
    id BIGINT IDENTITY(1,1) NOT NULL,
    cart_id BIGINT NOT NULL,
    item_type VARCHAR(50) NOT NULL,
    pet_id BIGINT NULL,
    product_id BIGINT NULL,
    quantity INT NOT NULL CONSTRAINT DF_cart_items_qty DEFAULT 1,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_cart_items_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_cart_items_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_cart_items PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_cart_items_cart FOREIGN KEY (cart_id) REFERENCES dbo.carts(id) ON DELETE CASCADE,
    CONSTRAINT FK_cart_items_pet FOREIGN KEY (pet_id) REFERENCES dbo.pets(id) ON DELETE NO ACTION,
    CONSTRAINT FK_cart_items_product FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE NO ACTION,
    CONSTRAINT CK_cart_items_type CHECK (item_type IN ('PET', 'PRODUCT')),
    CONSTRAINT CK_cart_items_ref CHECK (
        (item_type = 'PET' AND pet_id IS NOT NULL AND product_id IS NULL) OR
        (item_type = 'PRODUCT' AND product_id IS NOT NULL AND pet_id IS NULL)
    )
);
GO

-- ------------------------------------------------------------------------------------------
-- 6. ORDERS & ORDER ITEMS
-- ------------------------------------------------------------------------------------------

-- Bảng orders: Đơn đặt hàng của khách
IF OBJECT_ID(N'dbo.orders', N'U') IS NULL
CREATE TABLE dbo.orders (
    id BIGINT IDENTITY(1,1) NOT NULL,
    customer_id BIGINT NOT NULL,
    order_number VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL CONSTRAINT DF_orders_status DEFAULT 'PENDING',
    subtotal DECIMAL(18,2) NOT NULL CONSTRAINT DF_orders_subtotal DEFAULT 0.00,
    discount_amount DECIMAL(18,2) NOT NULL CONSTRAINT DF_orders_discount DEFAULT 0.00,
    tax_amount DECIMAL(18,2) NOT NULL CONSTRAINT DF_orders_tax DEFAULT 0.00,
    total_amount DECIMAL(18,2) NOT NULL CONSTRAINT DF_orders_total DEFAULT 0.00,
    shipping_address NVARCHAR(MAX) NULL,
    payment_method VARCHAR(50) NULL,
    payment_status VARCHAR(50) NOT NULL CONSTRAINT DF_orders_pay_status DEFAULT 'PENDING',
    notes NVARCHAR(MAX) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_orders_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_orders_updated_at DEFAULT SYSUTCDATETIME(),
    deleted_at DATETIME2(0) NULL,
    CONSTRAINT PK_orders PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_orders_order_number UNIQUE (order_number),
    CONSTRAINT FK_orders_customer FOREIGN KEY (customer_id) REFERENCES dbo.users(id) ON DELETE NO ACTION,
    CONSTRAINT CK_orders_status CHECK (status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPING', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT CK_orders_pay_status CHECK (payment_status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED'))
);
GO

-- Bảng order_items: Chi tiết món trong đơn hàng (lưu lại snapshot giá bán lúc mua)
IF OBJECT_ID(N'dbo.order_items', N'U') IS NULL
CREATE TABLE dbo.order_items (
    id BIGINT IDENTITY(1,1) NOT NULL,
    order_id BIGINT NOT NULL,
    item_type VARCHAR(50) NOT NULL,
    pet_id BIGINT NULL,
    product_id BIGINT NULL,
    quantity INT NOT NULL CONSTRAINT DF_order_items_qty DEFAULT 1,
    unit_price DECIMAL(18,2) NOT NULL,
    subtotal DECIMAL(18,2) NOT NULL,
    CONSTRAINT PK_order_items PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_order_items_order FOREIGN KEY (order_id) REFERENCES dbo.orders(id) ON DELETE CASCADE,
    CONSTRAINT FK_order_items_pet FOREIGN KEY (pet_id) REFERENCES dbo.pets(id) ON DELETE NO ACTION,
    CONSTRAINT FK_order_items_product FOREIGN KEY (product_id) REFERENCES dbo.products(id) ON DELETE NO ACTION,
    CONSTRAINT CK_order_items_type CHECK (item_type IN ('PET', 'PRODUCT')),
    CONSTRAINT CK_order_items_ref CHECK (
        (item_type = 'PET' AND pet_id IS NOT NULL AND product_id IS NULL) OR
        (item_type = 'PRODUCT' AND product_id IS NOT NULL AND pet_id IS NULL)
    )
);
GO

-- ------------------------------------------------------------------------------------------
-- 7. SERVICES & APPOINTMENTS
-- ------------------------------------------------------------------------------------------

-- Bảng services: Danh mục dịch vụ spa, grooming và thú y
IF OBJECT_ID(N'dbo.services', N'U') IS NULL
CREATE TABLE dbo.services (
    id BIGINT IDENTITY(1,1) NOT NULL,
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(MAX) NULL,
    service_type VARCHAR(50) NOT NULL,
    duration_minutes INT NOT NULL,
    price DECIMAL(18,2) NOT NULL,
    active BIT NOT NULL CONSTRAINT DF_services_active DEFAULT 1,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_services_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_services_updated_at DEFAULT SYSUTCDATETIME(),
    deleted_at DATETIME2(0) NULL,
    CONSTRAINT PK_services PRIMARY KEY CLUSTERED (id),
    CONSTRAINT CK_services_type CHECK (service_type IN ('GROOMING', 'VETERINARY'))
);
GO

-- Bảng appointments: Lịch hẹn dịch vụ chăm sóc / khám bệnh
IF OBJECT_ID(N'dbo.appointments', N'U') IS NULL
CREATE TABLE dbo.appointments (
    id BIGINT IDENTITY(1,1) NOT NULL,
    customer_id BIGINT NOT NULL,
    pet_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    appointment_date DATE NOT NULL,
    start_time TIME(0) NOT NULL,
    end_time TIME(0) NOT NULL,
    status VARCHAR(50) NOT NULL CONSTRAINT DF_appointments_status DEFAULT 'SCHEDULED',
    notes NVARCHAR(MAX) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_appointments_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_appointments_updated_at DEFAULT SYSUTCDATETIME(),
    deleted_at DATETIME2(0) NULL,
    CONSTRAINT PK_appointments PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_appointments_customer FOREIGN KEY (customer_id) REFERENCES dbo.users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_appointments_pet FOREIGN KEY (pet_id) REFERENCES dbo.pets(id) ON DELETE NO ACTION,
    CONSTRAINT FK_appointments_service FOREIGN KEY (service_id) REFERENCES dbo.services(id) ON DELETE NO ACTION,
    CONSTRAINT FK_appointments_staff FOREIGN KEY (staff_id) REFERENCES dbo.users(id) ON DELETE NO ACTION,
    CONSTRAINT CK_appointments_status CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW'))
);
GO

-- ------------------------------------------------------------------------------------------
-- 8. MEDICAL RECORDS, VACCINATIONS & TREATMENTS
-- ------------------------------------------------------------------------------------------

-- Bảng medical_records: Hồ sơ bệnh án của thú cưng
IF OBJECT_ID(N'dbo.medical_records', N'U') IS NULL
CREATE TABLE dbo.medical_records (
    id BIGINT IDENTITY(1,1) NOT NULL,
    pet_id BIGINT NOT NULL,
    veterinarian_id BIGINT NOT NULL,
    appointment_id BIGINT NULL,
    examination_date DATE NOT NULL,
    findings NVARCHAR(MAX) NOT NULL,
    diagnosis NVARCHAR(MAX) NULL,
    recommendations NVARCHAR(MAX) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_med_records_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_med_records_updated_at DEFAULT SYSUTCDATETIME(),
    deleted_at DATETIME2(0) NULL,
    CONSTRAINT PK_medical_records PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_med_records_pet FOREIGN KEY (pet_id) REFERENCES dbo.pets(id) ON DELETE NO ACTION,
    CONSTRAINT FK_med_records_vet FOREIGN KEY (veterinarian_id) REFERENCES dbo.users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_med_records_appt FOREIGN KEY (appointment_id) REFERENCES dbo.appointments(id) ON DELETE NO ACTION
);
GO

-- Bảng vaccinations: Lịch sử tiêm phòng vaccine của thú cưng
IF OBJECT_ID(N'dbo.vaccinations', N'U') IS NULL
CREATE TABLE dbo.vaccinations (
    id BIGINT IDENTITY(1,1) NOT NULL,
    pet_id BIGINT NOT NULL,
    veterinarian_id BIGINT NOT NULL,
    vaccine_name NVARCHAR(100) NOT NULL,
    vaccine_batch_number VARCHAR(100) NULL,
    date_administered DATE NOT NULL,
    next_due_date DATE NULL,
    notes NVARCHAR(MAX) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_vaccinations_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_vaccinations_updated_at DEFAULT SYSUTCDATETIME(),
    deleted_at DATETIME2(0) NULL,
    CONSTRAINT PK_vaccinations PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_vaccinations_pet FOREIGN KEY (pet_id) REFERENCES dbo.pets(id) ON DELETE NO ACTION,
    CONSTRAINT FK_vaccinations_vet FOREIGN KEY (veterinarian_id) REFERENCES dbo.users(id) ON DELETE NO ACTION
);
GO

-- Bảng treatments: Đơn thuốc và tiến trình điều trị bệnh
IF OBJECT_ID(N'dbo.treatments', N'U') IS NULL
CREATE TABLE dbo.treatments (
    id BIGINT IDENTITY(1,1) NOT NULL,
    medical_record_id BIGINT NOT NULL,
    medication NVARCHAR(100) NOT NULL,
    dosage NVARCHAR(100) NOT NULL,
    frequency NVARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    follow_up_date DATE NULL,
    status VARCHAR(50) NOT NULL CONSTRAINT DF_treatments_status DEFAULT 'ACTIVE',
    notes NVARCHAR(MAX) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_treatments_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_treatments_updated_at DEFAULT SYSUTCDATETIME(),
    deleted_at DATETIME2(0) NULL,
    CONSTRAINT PK_treatments PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_treatments_record FOREIGN KEY (medical_record_id) REFERENCES dbo.medical_records(id) ON DELETE CASCADE,
    CONSTRAINT CK_treatments_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'DISCONTINUED'))
);
GO

-- ------------------------------------------------------------------------------------------
-- 9. REVIEWS & PROMOTIONS
-- ------------------------------------------------------------------------------------------

-- Bảng reviews: Đánh giá và bình luận (Polymorphic cho PET, PRODUCT, SERVICE)
IF OBJECT_ID(N'dbo.reviews', N'U') IS NULL
CREATE TABLE dbo.reviews (
    id BIGINT IDENTITY(1,1) NOT NULL,
    customer_id BIGINT NOT NULL,
    reviewable_type VARCHAR(50) NOT NULL,
    reviewable_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment NVARCHAR(MAX) NULL,
    approved BIT NOT NULL CONSTRAINT DF_reviews_approved DEFAULT 0,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_reviews_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_reviews_updated_at DEFAULT SYSUTCDATETIME(),
    deleted_at DATETIME2(0) NULL,
    CONSTRAINT PK_reviews PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_reviews_customer FOREIGN KEY (customer_id) REFERENCES dbo.users(id) ON DELETE CASCADE,
    CONSTRAINT CK_reviews_type CHECK (reviewable_type IN ('PET', 'PRODUCT', 'SERVICE')),
    CONSTRAINT CK_reviews_rating CHECK (rating >= 1 AND rating <= 5)
);
GO

-- Bảng promotions: Mã phiếu giảm giá khuyến mãi
IF OBJECT_ID(N'dbo.promotions', N'U') IS NULL
CREATE TABLE dbo.promotions (
    id BIGINT IDENTITY(1,1) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description NVARCHAR(MAX) NULL,
    discount_type VARCHAR(50) NOT NULL,
    discount_value DECIMAL(18,2) NOT NULL,
    min_order_amount DECIMAL(18,2) NULL,
    max_uses INT NULL,
    current_uses INT NOT NULL CONSTRAINT DF_promotions_current_uses DEFAULT 0,
    start_date DATETIME2(0) NULL,
    end_date DATETIME2(0) NULL,
    active BIT NOT NULL CONSTRAINT DF_promotions_active DEFAULT 1,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_promotions_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_promotions_updated_at DEFAULT SYSUTCDATETIME(),
    deleted_at DATETIME2(0) NULL,
    CONSTRAINT PK_promotions PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_promotions_code UNIQUE (code),
    CONSTRAINT CK_promotions_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT'))
);
GO

-- Bảng promotion_usages: Lịch sử áp dụng khuyến mãi theo đơn hàng
IF OBJECT_ID(N'dbo.promotion_usages', N'U') IS NULL
CREATE TABLE dbo.promotion_usages (
    id BIGINT IDENTITY(1,1) NOT NULL,
    promotion_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_promo_usage_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_promotion_usages PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_promo_usage_promo FOREIGN KEY (promotion_id) REFERENCES dbo.promotions(id) ON DELETE NO ACTION,
    CONSTRAINT FK_promo_usage_order FOREIGN KEY (order_id) REFERENCES dbo.orders(id) ON DELETE CASCADE,
    CONSTRAINT FK_promo_usage_customer FOREIGN KEY (customer_id) REFERENCES dbo.users(id) ON DELETE NO ACTION
);
GO

-- ------------------------------------------------------------------------------------------
-- 10. LOYALTY PROGRAM (KHÁCH HÀNG THÂN THIẾT)
-- ------------------------------------------------------------------------------------------

-- Bảng loyalty_accounts: Điểm tích lũy và thứ hạng thành viên
IF OBJECT_ID(N'dbo.loyalty_accounts', N'U') IS NULL
CREATE TABLE dbo.loyalty_accounts (
    id BIGINT IDENTITY(1,1) NOT NULL,
    customer_id BIGINT NOT NULL,
    total_points INT NOT NULL CONSTRAINT DF_loyalty_total DEFAULT 0,
    available_points INT NOT NULL CONSTRAINT DF_loyalty_avail DEFAULT 0,
    membership_level VARCHAR(50) NOT NULL CONSTRAINT DF_loyalty_level DEFAULT 'BRONZE',
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_loyalty_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_loyalty_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_loyalty_accounts PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_loyalty_customer UNIQUE (customer_id),
    CONSTRAINT FK_loyalty_customer FOREIGN KEY (customer_id) REFERENCES dbo.users(id) ON DELETE CASCADE,
    CONSTRAINT CK_loyalty_level CHECK (membership_level IN ('BRONZE', 'SILVER', 'GOLD', 'PLATINUM'))
);
GO

-- Bảng loyalty_transactions: Lịch sử cộng/trừ điểm thưởng
IF OBJECT_ID(N'dbo.loyalty_transactions', N'U') IS NULL
CREATE TABLE dbo.loyalty_transactions (
    id BIGINT IDENTITY(1,1) NOT NULL,
    loyalty_account_id BIGINT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    points INT NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_id BIGINT NULL,
    description NVARCHAR(255) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_loyalty_tx_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_loyalty_transactions PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_loyalty_tx_account FOREIGN KEY (loyalty_account_id) REFERENCES dbo.loyalty_accounts(id) ON DELETE CASCADE,
    CONSTRAINT CK_loyalty_tx_type CHECK (transaction_type IN ('EARN', 'REDEEM', 'EXPIRE', 'ADJUST')),
    CONSTRAINT CK_loyalty_source_type CHECK (source_type IN ('PURCHASE', 'SERVICE_BOOKING', 'REVIEW', 'CARE_PACKAGE', 'REDEMPTION'))
);
GO

-- ------------------------------------------------------------------------------------------
-- 11. CARE PACKAGES (GÓI CHĂM SÓC ĐỊNH KỲ)
-- ------------------------------------------------------------------------------------------

-- Bảng care_packages: Danh mục gói dịch vụ định kỳ
IF OBJECT_ID(N'dbo.care_packages', N'U') IS NULL
CREATE TABLE dbo.care_packages (
    id BIGINT IDENTITY(1,1) NOT NULL,
    name NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX) NULL,
    price DECIMAL(18,2) NOT NULL,
    duration_months INT NOT NULL CONSTRAINT DF_care_pkg_duration DEFAULT 1,
    includes_grooming BIT NOT NULL CONSTRAINT DF_care_pkg_grooming DEFAULT 0,
    includes_checkup BIT NOT NULL CONSTRAINT DF_care_pkg_checkup DEFAULT 0,
    includes_vaccination BIT NOT NULL CONSTRAINT DF_care_pkg_vaccination DEFAULT 0,
    active BIT NOT NULL CONSTRAINT DF_care_pkg_active DEFAULT 1,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_care_pkg_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_care_pkg_updated_at DEFAULT SYSUTCDATETIME(),
    deleted_at DATETIME2(0) NULL,
    CONSTRAINT PK_care_packages PRIMARY KEY CLUSTERED (id)
);
GO

-- Bảng care_package_subscriptions: Hợp đồng đăng ký gói chăm sóc cho thú cưng
IF OBJECT_ID(N'dbo.care_package_subscriptions', N'U') IS NULL
CREATE TABLE dbo.care_package_subscriptions (
    id BIGINT IDENTITY(1,1) NOT NULL,
    customer_id BIGINT NOT NULL,
    pet_id BIGINT NOT NULL,
    package_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL CONSTRAINT DF_care_sub_status DEFAULT 'ACTIVE',
    auto_renew BIT NOT NULL CONSTRAINT DF_care_sub_auto_renew DEFAULT 1,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_care_sub_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_care_sub_updated_at DEFAULT SYSUTCDATETIME(),
    deleted_at DATETIME2(0) NULL,
    CONSTRAINT PK_care_package_subscriptions PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_care_sub_customer FOREIGN KEY (customer_id) REFERENCES dbo.users(id) ON DELETE NO ACTION,
    CONSTRAINT FK_care_sub_pet FOREIGN KEY (pet_id) REFERENCES dbo.pets(id) ON DELETE NO ACTION,
    CONSTRAINT FK_care_sub_package FOREIGN KEY (package_id) REFERENCES dbo.care_packages(id) ON DELETE NO ACTION,
    CONSTRAINT CK_care_sub_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED'))
);
GO

-- ------------------------------------------------------------------------------------------
-- 12. PET MATCHING QUIZ (TRẮC NGHIỆM TÌM THÚ CƯNG)
-- ------------------------------------------------------------------------------------------

-- Bảng pet_matching_quizzes: Câu trả lời khảo sát thói quen sinh hoạt của khách
IF OBJECT_ID(N'dbo.pet_matching_quizzes', N'U') IS NULL
CREATE TABLE dbo.pet_matching_quizzes (
    id BIGINT IDENTITY(1,1) NOT NULL,
    customer_id BIGINT NOT NULL,
    living_environment NVARCHAR(100) NULL,
    house_size NVARCHAR(100) NULL,
    has_children BIT NULL,
    children_age_range NVARCHAR(100) NULL,
    activity_level NVARCHAR(100) NULL,
    daily_care_hours DECIMAL(4,2) NULL,
    pet_experience NVARCHAR(100) NULL,
    size_preference NVARCHAR(100) NULL,
    energy_preference NVARCHAR(100) NULL,
    grooming_tolerance NVARCHAR(100) NULL,
    noise_tolerance NVARCHAR(100) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_pet_quiz_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_pet_matching_quizzes PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_pet_quiz_customer FOREIGN KEY (customer_id) REFERENCES dbo.users(id) ON DELETE CASCADE
);
GO

-- Bảng pet_matching_results: Kết quả chấm điểm xếp hạng thú cưng phù hợp
IF OBJECT_ID(N'dbo.pet_matching_results', N'U') IS NULL
CREATE TABLE dbo.pet_matching_results (
    id BIGINT IDENTITY(1,1) NOT NULL,
    quiz_id BIGINT NOT NULL,
    pet_id BIGINT NOT NULL,
    overall_score INT NOT NULL,
    score_breakdown_json NVARCHAR(MAX) NULL,
    explanation NVARCHAR(MAX) NULL,
    rank INT NOT NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_pet_match_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_pet_matching_results PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_pet_match_quiz FOREIGN KEY (quiz_id) REFERENCES dbo.pet_matching_quizzes(id) ON DELETE CASCADE,
    CONSTRAINT FK_pet_match_pet FOREIGN KEY (pet_id) REFERENCES dbo.pets(id) ON DELETE CASCADE,
    CONSTRAINT CK_pet_match_json CHECK (score_breakdown_json IS NULL OR ISJSON(score_breakdown_json) = 1)
);
GO

-- ------------------------------------------------------------------------------------------
-- 13. NOTIFICATIONS (THÔNG BÁO)
-- ------------------------------------------------------------------------------------------

-- Bảng notifications: Hộp thư thông báo người dùng
IF OBJECT_ID(N'dbo.notifications', N'U') IS NULL
CREATE TABLE dbo.notifications (
    id BIGINT IDENTITY(1,1) NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title NVARCHAR(255) NOT NULL,
    message NVARCHAR(MAX) NOT NULL,
    is_read BIT NOT NULL CONSTRAINT DF_notif_is_read DEFAULT 0,
    data_json NVARCHAR(MAX) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_notif_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_notifications PRIMARY KEY CLUSTERED (id),
    CONSTRAINT FK_notifications_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE,
    CONSTRAINT CK_notif_type CHECK (type IN ('ORDER_UPDATE', 'APPOINTMENT_REMINDER', 'APPOINTMENT_CONFIRMED', 'VACCINATION_DUE', 'LOW_STOCK', 'GROOMING_COMPLETE', 'HEALTH_ALERT', 'SYSTEM')),
    CONSTRAINT CK_notif_json CHECK (data_json IS NULL OR ISJSON(data_json) = 1)
);
GO

-- ==========================================================================================
-- INDEXING STRATEGY (CÁC CHỈ MỤC TỐI ƯU TRUY VẤN)
-- ==========================================================================================

-- Index cho Authentication & User
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_users_email' AND object_id = OBJECT_ID(N'dbo.users'))
CREATE NONCLUSTERED INDEX IX_users_email ON dbo.users(email);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_users_username' AND object_id = OBJECT_ID(N'dbo.users'))
CREATE NONCLUSTERED INDEX IX_users_username ON dbo.users(username);

-- Index cho Pet search & filter
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_pets_species_breed' AND object_id = OBJECT_ID(N'dbo.pets'))
CREATE NONCLUSTERED INDEX IX_pets_species_breed ON dbo.pets(species_id, breed_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_pets_availability_status' AND object_id = OBJECT_ID(N'dbo.pets'))
CREATE NONCLUSTERED INDEX IX_pets_availability_status ON dbo.pets(availability_status) INCLUDE (price, health_status);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_pets_owner_id' AND object_id = OBJECT_ID(N'dbo.pets'))
CREATE NONCLUSTERED INDEX IX_pets_owner_id ON dbo.pets(owner_id) WHERE owner_id IS NOT NULL;

-- Index cho Product search & filter
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_products_category_active' AND object_id = OBJECT_ID(N'dbo.products'))
CREATE NONCLUSTERED INDEX IX_products_category_active ON dbo.products(category_id, active) INCLUDE (price, name);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_products_sku' AND object_id = OBJECT_ID(N'dbo.products'))
CREATE NONCLUSTERED INDEX IX_products_sku ON dbo.products(sku);

-- Index cho Inventory
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_inventory_quantity' AND object_id = OBJECT_ID(N'dbo.inventory'))
CREATE NONCLUSTERED INDEX IX_inventory_quantity ON dbo.inventory(quantity) INCLUDE (reorder_point);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_inv_tx_inventory_created' AND object_id = OBJECT_ID(N'dbo.inventory_transactions'))
CREATE NONCLUSTERED INDEX IX_inv_tx_inventory_created ON dbo.inventory_transactions(inventory_id, created_at DESC);

-- Index cho Orders
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_orders_customer_created' AND object_id = OBJECT_ID(N'dbo.orders'))
CREATE NONCLUSTERED INDEX IX_orders_customer_created ON dbo.orders(customer_id, created_at DESC);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_orders_status' AND object_id = OBJECT_ID(N'dbo.orders'))
CREATE NONCLUSTERED INDEX IX_orders_status ON dbo.orders(status) INCLUDE (total_amount, created_at);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_order_items_order_id' AND object_id = OBJECT_ID(N'dbo.order_items'))
CREATE NONCLUSTERED INDEX IX_order_items_order_id ON dbo.order_items(order_id);

-- Index cho Appointments (chống trùng lịch nhân viên/bác sĩ)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_appointments_staff_schedule' AND object_id = OBJECT_ID(N'dbo.appointments'))
CREATE NONCLUSTERED INDEX IX_appointments_staff_schedule ON dbo.appointments(staff_id, appointment_date, start_time, end_time);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_appointments_customer' AND object_id = OBJECT_ID(N'dbo.appointments'))
CREATE NONCLUSTERED INDEX IX_appointments_customer ON dbo.appointments(customer_id, appointment_date);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_appointments_pet' AND object_id = OBJECT_ID(N'dbo.appointments'))
CREATE NONCLUSTERED INDEX IX_appointments_pet ON dbo.appointments(pet_id);

-- Index cho Y tế & Tiêm phòng
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_medical_records_pet' AND object_id = OBJECT_ID(N'dbo.medical_records'))
CREATE NONCLUSTERED INDEX IX_medical_records_pet ON dbo.medical_records(pet_id, examination_date DESC);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_vaccinations_pet_next_due' AND object_id = OBJECT_ID(N'dbo.vaccinations'))
CREATE NONCLUSTERED INDEX IX_vaccinations_pet_next_due ON dbo.vaccinations(pet_id, next_due_date);

-- Index cho Reviews & Promotions & Loyalty
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_reviews_target' AND object_id = OBJECT_ID(N'dbo.reviews'))
CREATE NONCLUSTERED INDEX IX_reviews_target ON dbo.reviews(reviewable_type, reviewable_id, approved);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_promotions_code_active' AND object_id = OBJECT_ID(N'dbo.promotions'))
CREATE NONCLUSTERED INDEX IX_promotions_code_active ON dbo.promotions(code, active, start_date, end_date);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_loyalty_tx_account' AND object_id = OBJECT_ID(N'dbo.loyalty_transactions'))
CREATE NONCLUSTERED INDEX IX_loyalty_tx_account ON dbo.loyalty_transactions(loyalty_account_id, created_at DESC);

-- Index cho Notifications
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_notifications_user_unread' AND object_id = OBJECT_ID(N'dbo.notifications'))
CREATE NONCLUSTERED INDEX IX_notifications_user_unread ON dbo.notifications(user_id, is_read, created_at DESC);
GO

