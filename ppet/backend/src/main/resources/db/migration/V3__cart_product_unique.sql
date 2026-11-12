-- Apply after migration 001, with application writers stopped and a verified backup.
-- Keep distinct PET lines (product_id NULL); never merge/delete existing customer data.
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
    IF EXISTS (
        SELECT cart_id, product_id FROM dbo.cart_items WITH (TABLOCKX, HOLDLOCK)
        WHERE product_id IS NOT NULL
        GROUP BY cart_id, product_id HAVING COUNT_BIG(*) > 1
    )
        THROW 50002, N'Duplicate cart/product rows: reconcile manually before migration 002.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_cart_items_cart_product_present'
                   AND object_id = OBJECT_ID(N'dbo.cart_items'))
        CREATE UNIQUE INDEX UX_cart_items_cart_product_present
            ON dbo.cart_items(cart_id, product_id) WHERE product_id IS NOT NULL;
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
