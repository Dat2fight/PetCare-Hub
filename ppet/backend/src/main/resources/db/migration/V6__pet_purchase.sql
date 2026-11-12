-- Apply explicitly after reviewing a backup, before deploying pet-purchase code.
-- Adds reservation ownership and PENDING without changing existing rows.
SET XACT_ABORT ON;
BEGIN TRY
BEGIN TRANSACTION;
IF COL_LENGTH(N'dbo.pets', N'reserved_order_id') IS NULL
    ALTER TABLE dbo.pets ADD reserved_order_id BIGINT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_pets_reserved_order' AND parent_object_id = OBJECT_ID(N'dbo.pets'))
    EXEC(N'ALTER TABLE dbo.pets WITH CHECK ADD CONSTRAINT FK_pets_reserved_order
        FOREIGN KEY (reserved_order_id) REFERENCES dbo.orders(id) ON DELETE NO ACTION');
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_pets_avail_status' AND parent_object_id = OBJECT_ID(N'dbo.pets'))
    ALTER TABLE dbo.pets DROP CONSTRAINT CK_pets_avail_status;
ALTER TABLE dbo.pets WITH CHECK ADD CONSTRAINT CK_pets_avail_status
    CHECK (availability_status IN ('AVAILABLE', 'PENDING', 'SOLD', 'RESERVED', 'UNAVAILABLE'));
COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
