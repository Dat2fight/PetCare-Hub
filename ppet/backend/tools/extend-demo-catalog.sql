-- Optional additions to the Vietnamese demo catalog. Run with sqlcmd -I -b -f 65001.
SET XACT_ABORT ON;
SET NOCOUNT ON;
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
BEGIN TRANSACTION;

IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE sku = 'GROOM-BRUSH-01')
BEGIN
    DECLARE @category_id bigint = (SELECT MIN(id) FROM dbo.product_categories WHERE name = N'Vệ sinh & Chăm sóc');
    IF @category_id IS NULL THROW 50010, N'Không tìm thấy danh mục Vệ sinh & Chăm sóc.', 1;
    INSERT INTO dbo.products (name, description, price, image_url, category_id, sku, active)
    VALUES (N'Lược chải lông cho chó mèo', N'Lược cầm tay dùng để gỡ rối và gom lông rụng khi chải.', 85000,
            N'/PetCare-Hub/images/brush.png', @category_id, 'GROOM-BRUSH-01', 1);
    INSERT INTO dbo.inventory (product_id, quantity, location) VALUES (SCOPE_IDENTITY(), 20, N'Kho chăm sóc');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.services WITH (UPDLOCK, HOLDLOCK) WHERE name = N'Cắt và mài móng')
    INSERT INTO dbo.services (name, description, service_type, duration_minutes, price, active)
    VALUES (N'Cắt và mài móng', N'Cắt ngắn và mài nhẵn các đầu móng cho chó hoặc mèo.', 'GROOMING', 20, 80000, 1);

INSERT INTO dbo.care_packages (name, description, price, duration_months, includes_grooming, includes_checkup, includes_vaccination, active)
SELECT p.name, p.description, p.price, p.months, p.grooming, p.checkup, p.vaccination, 1
FROM (VALUES
    (N'Gói chăm sóc lông 3 tháng', N'Chăm sóc lông và vệ sinh cho thú cưng trong ba tháng.', 750000, 3, 1, 0, 0),
    (N'Gói sức khỏe 6 tháng', N'Khám định kỳ và tiêm phòng cho thú cưng trong sáu tháng.', 1250000, 6, 0, 1, 1)
) AS p(name, description, price, months, grooming, checkup, vaccination)
WHERE NOT EXISTS (SELECT 1 FROM dbo.care_packages c WITH (UPDLOCK, HOLDLOCK) WHERE c.name = p.name);

COMMIT;
