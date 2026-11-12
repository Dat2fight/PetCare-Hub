-- Preserve the unbounded descriptions accepted by the application.
-- All other Unicode/price/URL differences are aligned in entity mappings to V1-V14.
-- Databases with unrelated ORM-created schema drift require an audited migration
-- before onboarding; baseline is not a schema repair operation.
SET XACT_ABORT ON;

IF EXISTS (SELECT 1 FROM sys.columns
           WHERE object_id = OBJECT_ID(N'dbo.roles') AND name = N'description'
             AND (TYPE_NAME(system_type_id) <> N'nvarchar' OR max_length <> -1))
    ALTER TABLE dbo.roles ALTER COLUMN description NVARCHAR(MAX) NULL;

IF EXISTS (SELECT 1 FROM sys.columns
           WHERE object_id = OBJECT_ID(N'dbo.loyalty_transactions') AND name = N'description'
             AND (TYPE_NAME(system_type_id) <> N'nvarchar' OR max_length <> -1))
    ALTER TABLE dbo.loyalty_transactions ALTER COLUMN description NVARCHAR(MAX) NULL;
