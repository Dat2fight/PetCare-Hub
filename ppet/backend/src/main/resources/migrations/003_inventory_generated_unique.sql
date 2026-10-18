-- Apply after 001/002 with application writers stopped and a verified backup.
-- Migration 001 handles the original named constraints. Old Hibernate ddl-auto=update
-- may also have created differently named UNIQUE constraints/indexes on these keys.
-- Only single-key, unfiltered, non-primary uniqueness on inventory pet/product is replaced.
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF EXISTS (SELECT 1 FROM dbo.inventory WITH (TABLOCKX, HOLDLOCK) WHERE quantity < 0)
        THROW 50003, N'Negative inventory: reconcile before migration 003.', 1;

    -- Establish replacement protection before removing any legacy uniqueness.
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_inventory_pet_present' AND object_id = OBJECT_ID(N'dbo.inventory'))
        CREATE UNIQUE INDEX UX_inventory_pet_present ON dbo.inventory(pet_id) WHERE pet_id IS NOT NULL;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_inventory_product_present' AND object_id = OBJECT_ID(N'dbo.inventory'))
        CREATE UNIQUE INDEX UX_inventory_product_present ON dbo.inventory(product_id) WHERE product_id IS NOT NULL;

    DECLARE @drop NVARCHAR(MAX) = N'';
    SELECT @drop = (SELECT CASE WHEN i.is_unique_constraint = 1
        THEN N'ALTER TABLE dbo.inventory DROP CONSTRAINT ' + QUOTENAME(i.name) + N';'
        ELSE N'DROP INDEX ' + QUOTENAME(i.name) + N' ON dbo.inventory;' END
    FROM sys.indexes i
    WHERE i.object_id = OBJECT_ID(N'dbo.inventory')
      AND i.is_unique = 1 AND i.has_filter = 0 AND i.is_primary_key = 0
      AND (SELECT COUNT(*) FROM sys.index_columns k WHERE k.object_id = i.object_id AND k.index_id = i.index_id AND k.key_ordinal > 0) = 1
      AND EXISTS (SELECT 1 FROM sys.index_columns k
                  JOIN sys.columns c ON c.object_id = k.object_id AND c.column_id = k.column_id
                  WHERE k.object_id = i.object_id AND k.index_id = i.index_id AND k.key_ordinal = 1
                    AND c.name IN (N'pet_id', N'product_id'))
    ORDER BY i.index_id
    FOR XML PATH(''), TYPE).value('.', 'nvarchar(max)');
    IF LEN(@drop) > 0 EXEC sys.sp_executesql @drop;
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
