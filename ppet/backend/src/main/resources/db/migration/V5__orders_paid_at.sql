-- Manual deployment migration; never run by application startup.
-- NEEDS DECISION: reconcile historical payment timestamps before backfilling.
-- Do not infer a payment date from created_at or updated_at.
IF COL_LENGTH('dbo.orders', 'paid_at') IS NULL
BEGIN
    ALTER TABLE dbo.orders ADD paid_at datetime2 NULL;
END;
