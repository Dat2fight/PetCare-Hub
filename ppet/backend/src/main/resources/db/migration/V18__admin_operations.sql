IF COL_LENGTH('dbo.orders','refund_reference') IS NULL
    ALTER TABLE dbo.orders ADD refund_reference NVARCHAR(100) NULL, refunded_at DATETIME2(7) NULL, refund_requested_at DATETIME2(7) NULL;
IF OBJECT_ID('dbo.order_notes','U') IS NULL
BEGIN
    CREATE TABLE dbo.order_notes(id BIGINT IDENTITY PRIMARY KEY,order_id BIGINT NOT NULL REFERENCES dbo.orders(id),
        actor_id BIGINT NOT NULL REFERENCES dbo.users(id),body NVARCHAR(2000) NOT NULL,created_at DATETIME2(7) NOT NULL);
    CREATE INDEX IX_order_notes_order ON dbo.order_notes(order_id,id);
END;
IF OBJECT_ID('dbo.order_payment_events','U') IS NULL
BEGIN
    CREATE TABLE dbo.order_payment_events(id BIGINT IDENTITY PRIMARY KEY,order_id BIGINT NOT NULL REFERENCES dbo.orders(id),
        event_type VARCHAR(40) NOT NULL,amount DECIMAL(18,2) NOT NULL,reference NVARCHAR(100) NULL,
        actor_id BIGINT NOT NULL REFERENCES dbo.users(id),occurred_at DATETIME2(7) NOT NULL);
    CREATE UNIQUE INDEX UX_order_payment_event ON dbo.order_payment_events(order_id,event_type);
END;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.orders') AND name='IX_orders_admin_queue')
    CREATE INDEX IX_orders_admin_queue ON dbo.orders(status,payment_status,created_at,id);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.reviews') AND name='IX_reviews_admin_queue')
    CREATE INDEX IX_reviews_admin_queue ON dbo.reviews(moderation_state,reviewable_type,id);

IF COL_LENGTH('dbo.order_items','item_name_snapshot') IS NULL
    ALTER TABLE dbo.order_items ADD item_name_snapshot NVARCHAR(255) NULL;
