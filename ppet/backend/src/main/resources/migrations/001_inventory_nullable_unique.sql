-- Review and apply explicitly after schema-sqlserver.sql. Never resets data.
-- SQL Server UNIQUE constraints permit only one NULL; polymorphic inventory
-- needs uniqueness only for non-NULL keys. Existing bad quantities abort safely.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF EXISTS (SELECT 1 FROM dbo.inventory WHERE quantity < 0)
    THROW 50001, N'Tồn kho âm: cần đối soát trước khi migration.', 1;
IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_inventory_pet' AND parent_object_id = OBJECT_ID(N'dbo.inventory'))
    ALTER TABLE dbo.inventory DROP CONSTRAINT UQ_inventory_pet;
IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_inventory_product' AND parent_object_id = OBJECT_ID(N'dbo.inventory'))
    ALTER TABLE dbo.inventory DROP CONSTRAINT UQ_inventory_product;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_inventory_pet_present' AND object_id = OBJECT_ID(N'dbo.inventory'))
    CREATE UNIQUE INDEX UX_inventory_pet_present ON dbo.inventory(pet_id) WHERE pet_id IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_inventory_product_present' AND object_id = OBJECT_ID(N'dbo.inventory'))
    CREATE UNIQUE INDEX UX_inventory_product_present ON dbo.inventory(product_id) WHERE product_id IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_inventory_nonnegative' AND parent_object_id = OBJECT_ID(N'dbo.inventory'))
    ALTER TABLE dbo.inventory WITH CHECK ADD CONSTRAINT CK_inventory_nonnegative CHECK (quantity >= 0);
COMMIT;
