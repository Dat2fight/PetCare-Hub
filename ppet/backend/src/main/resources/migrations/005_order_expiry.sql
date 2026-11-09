SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.orders', 'expires_at') IS NULL
    ALTER TABLE dbo.orders ADD expires_at datetime2 NULL;
IF COL_LENGTH('dbo.orders', 'expired_at') IS NULL
    ALTER TABLE dbo.orders ADD expired_at datetime2 NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.orders') AND name = 'IX_orders_expiry')
    EXEC('CREATE INDEX IX_orders_expiry ON dbo.orders(status, payment_status, expires_at) INCLUDE (id)');
-- Automated stock releases have no human operator.
ALTER TABLE dbo.inventory_transactions ALTER COLUMN performed_by bigint NULL;
COMMIT;
