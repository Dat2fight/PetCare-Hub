SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.orders','confirmed_at') IS NULL ALTER TABLE dbo.orders ADD confirmed_at datetime2 NULL;
IF COL_LENGTH('dbo.orders','processing_at') IS NULL ALTER TABLE dbo.orders ADD processing_at datetime2 NULL;
IF COL_LENGTH('dbo.orders','shipped_at') IS NULL ALTER TABLE dbo.orders ADD shipped_at datetime2 NULL;
IF COL_LENGTH('dbo.orders','delivered_at') IS NULL ALTER TABLE dbo.orders ADD delivered_at datetime2 NULL;
IF COL_LENGTH('dbo.orders','cancelled_at') IS NULL ALTER TABLE dbo.orders ADD cancelled_at datetime2 NULL;
COMMIT;
