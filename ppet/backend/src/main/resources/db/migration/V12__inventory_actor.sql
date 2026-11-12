SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.inventory_transactions', 'source') IS NULL
    ALTER TABLE dbo.inventory_transactions ADD source varchar(10) NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.inventory_transactions') AND name = 'IX_inventory_transactions_reference')
    CREATE INDEX IX_inventory_transactions_reference ON dbo.inventory_transactions(reference_type, reference_id);
COMMIT;
