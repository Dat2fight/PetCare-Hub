-- Additive phase 5 schema. Apply with Flyway after backup/test-copy verification.
IF COL_LENGTH('dbo.orders','return_window_days') IS NULL ALTER TABLE dbo.orders ADD return_window_days INT NULL;
IF COL_LENGTH('dbo.orders','returned_amount') IS NULL ALTER TABLE dbo.orders ADD returned_amount DECIMAL(18,2) NOT NULL CONSTRAINT DF_order_returned_amount DEFAULT 0;
IF OBJECT_ID('dbo.system_settings','U') IS NULL
 CREATE TABLE dbo.system_settings(setting_key VARCHAR(50) PRIMARY KEY,setting_value DECIMAL(18,2) NOT NULL);
IF OBJECT_ID('dbo.site_content','U') IS NULL
BEGIN
 CREATE TABLE dbo.site_content(id BIGINT IDENTITY PRIMARY KEY,slug VARCHAR(100) NOT NULL UNIQUE,kind VARCHAR(20) NOT NULL,title NVARCHAR(200) NOT NULL,body NVARCHAR(MAX) NOT NULL,configuration VARCHAR(2000) NOT NULL,published_snapshot NVARCHAR(MAX) NULL,updated_by BIGINT NULL REFERENCES dbo.users(id),updated_at DATETIME2 NULL,published_at DATETIME2 NULL);
 CREATE INDEX IX_site_content_public ON dbo.site_content(kind,published_at,id);
END;
IF OBJECT_ID('dbo.content_revisions','U') IS NULL
BEGIN
 CREATE TABLE dbo.content_revisions(id BIGINT IDENTITY PRIMARY KEY,content_id BIGINT NOT NULL REFERENCES dbo.site_content(id),snapshot NVARCHAR(MAX) NOT NULL,actor_id BIGINT NOT NULL REFERENCES dbo.users(id),created_at DATETIME2 NOT NULL);
 CREATE INDEX IX_content_revision ON dbo.content_revisions(content_id,id DESC);
END;
IF OBJECT_ID('dbo.return_requests','U') IS NULL
BEGIN
 CREATE TABLE dbo.return_requests(id BIGINT IDENTITY PRIMARY KEY,order_id BIGINT NOT NULL REFERENCES dbo.orders(id),item_id BIGINT NOT NULL REFERENCES dbo.order_items(id),customer_id BIGINT NOT NULL REFERENCES dbo.users(id),quantity INT NOT NULL CHECK(quantity>0),status VARCHAR(20) NOT NULL CHECK(status IN ('REQUESTED','APPROVED','REJECTED','RECEIVED','REFUNDED')),reason NVARCHAR(500) NOT NULL,decision_reason NVARCHAR(500) NULL,request_key VARCHAR(36) NOT NULL,fingerprint VARCHAR(64) NOT NULL,maximum_refund DECIMAL(18,2) NOT NULL,refund_amount DECIMAL(18,2) NULL,refund_reference NVARCHAR(100) NULL,created_at DATETIME2 NOT NULL,updated_at DATETIME2 NULL,handled_by BIGINT NULL REFERENCES dbo.users(id),CONSTRAINT UX_return_request UNIQUE(customer_id,request_key));
 CREATE INDEX IX_return_item ON dbo.return_requests(item_id,status);
 CREATE INDEX IX_return_queue ON dbo.return_requests(status,id);
 CREATE INDEX IX_return_order ON dbo.return_requests(order_id,status);
END;
IF OBJECT_ID('dbo.data_export_jobs','U') IS NULL
BEGIN
 CREATE TABLE dbo.data_export_jobs(id BIGINT IDENTITY PRIMARY KEY,actor_id BIGINT NOT NULL REFERENCES dbo.users(id),request_key VARCHAR(36) NOT NULL,fingerprint VARCHAR(64) NOT NULL,kind VARCHAR(20) NOT NULL,status VARCHAR(20) NOT NULL,criteria NVARCHAR(1000) NOT NULL,payload NVARCHAR(MAX) NULL,row_count INT NULL,error_code VARCHAR(40) NULL,created_at DATETIME2 NOT NULL,expires_at DATETIME2 NOT NULL,consumed_at DATETIME2 NULL,CONSTRAINT UX_data_export_request UNIQUE(actor_id,request_key));
 CREATE INDEX IX_data_export_queue ON dbo.data_export_jobs(status,id);
 CREATE INDEX IX_data_export_expiry ON dbo.data_export_jobs(expires_at,status);
 CREATE INDEX IX_data_export_owner ON dbo.data_export_jobs(actor_id,created_at);
END;
