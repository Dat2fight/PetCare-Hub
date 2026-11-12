SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.reviews','moderation_state') IS NULL ALTER TABLE dbo.reviews ADD moderation_state nvarchar(255) NULL;
IF COL_LENGTH('dbo.reviews','moderation_reason') IS NULL ALTER TABLE dbo.reviews ADD moderation_reason nvarchar(500) NULL;
COMMIT;
