SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.orders','promotion_code') IS NULL ALTER TABLE dbo.orders ADD promotion_code nvarchar(255) NULL;
IF COL_LENGTH('dbo.orders','points_used') IS NULL ALTER TABLE dbo.orders ADD points_used int NULL;
IF COL_LENGTH('dbo.orders','points_discount') IS NULL ALTER TABLE dbo.orders ADD points_discount decimal(38,2) NULL;
IF COL_LENGTH('dbo.orders','shipping_fee') IS NULL ALTER TABLE dbo.orders ADD shipping_fee decimal(38,2) NULL;
IF COL_LENGTH('dbo.orders','checkout_key') IS NULL ALTER TABLE dbo.orders ADD checkout_key varchar(36) NULL;
IF COL_LENGTH('dbo.orders','checkout_fingerprint') IS NULL ALTER TABLE dbo.orders ADD checkout_fingerprint varchar(64) NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.orders') AND name = 'UX_orders_checkout_key')
    EXEC('CREATE UNIQUE INDEX UX_orders_checkout_key ON dbo.orders(customer_id,checkout_key) WHERE checkout_key IS NOT NULL');
IF COL_LENGTH('dbo.promotions','max_uses_per_customer') IS NULL ALTER TABLE dbo.promotions ADD max_uses_per_customer int NULL;
COMMIT;
