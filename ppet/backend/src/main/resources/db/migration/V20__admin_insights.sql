-- Durable web notifications and safe scheduled-job telemetry. No changes to existing content.
IF OBJECT_ID('dbo.notification_templates','U') IS NULL
BEGIN
 CREATE TABLE dbo.notification_templates(id BIGINT IDENTITY PRIMARY KEY,code VARCHAR(60) NOT NULL UNIQUE,purpose VARCHAR(20) NOT NULL,title NVARCHAR(200) NOT NULL,body NVARCHAR(2000) NOT NULL,active BIT NOT NULL);
 INSERT INTO dbo.notification_templates(code,purpose,title,body,active) VALUES
 ('GENERAL','GENERAL',N'Thông báo từ PetCare Hub',N'Xin chào {firstName}, cảm ơn bạn đã đồng hành cùng PetCare Hub.',1),
 ('APPOINTMENT_REMINDER','APPOINTMENT',N'Nhắc lịch hẹn',N'{petName} có lịch hẹn lúc {appointmentTime}. Vui lòng đến đúng giờ.',1),
 ('VACCINATION_REMINDER','VACCINATION',N'Nhắc lịch tiêm',N'{petName} đến lịch tiêm tiếp theo ngày {dueDate}.',1);
END;
IF OBJECT_ID('dbo.notification_batches','U') IS NULL
BEGIN
 CREATE TABLE dbo.notification_batches(id BIGINT IDENTITY PRIMARY KEY,request_key VARCHAR(36) NOT NULL UNIQUE,actor_id BIGINT NOT NULL REFERENCES dbo.users(id),fingerprint VARCHAR(64) NOT NULL,status VARCHAR(20) NOT NULL,total INT NOT NULL,sent INT NOT NULL,skipped INT NOT NULL,attempts INT NOT NULL,created_at DATETIME2 NOT NULL,finished_at DATETIME2 NULL,error_code VARCHAR(40) NULL);
 CREATE INDEX IX_notification_batch_queue ON dbo.notification_batches(status,id);
 CREATE INDEX IX_notification_batch_actor ON dbo.notification_batches(actor_id,created_at);
END;
IF OBJECT_ID('dbo.notification_deliveries','U') IS NULL
BEGIN
 CREATE TABLE dbo.notification_deliveries(id BIGINT IDENTITY PRIMARY KEY,batch_id BIGINT NOT NULL REFERENCES dbo.notification_batches(id),user_id BIGINT NOT NULL REFERENCES dbo.users(id),title NVARCHAR(200) NOT NULL,body NVARCHAR(2000) NOT NULL,status VARCHAR(20) NOT NULL,CONSTRAINT UX_notification_delivery UNIQUE(batch_id,user_id));
 CREATE INDEX IX_notification_delivery_queue ON dbo.notification_deliveries(batch_id,status,id);
END;
IF OBJECT_ID('dbo.reminder_receipts','U') IS NULL
BEGIN
 CREATE TABLE dbo.reminder_receipts(id BIGINT IDENTITY PRIMARY KEY,kind VARCHAR(20) NOT NULL,source_id BIGINT NOT NULL,source_date DATE NOT NULL,source_time TIME(6) NOT NULL,created_at DATETIME2 NOT NULL,CONSTRAINT UX_reminder_receipt UNIQUE(kind,source_id,source_date,source_time));
END;
IF OBJECT_ID('dbo.job_runs','U') IS NULL
BEGIN
 CREATE TABLE dbo.job_runs(id BIGINT IDENTITY PRIMARY KEY,name VARCHAR(40) NOT NULL,started_at DATETIME2 NOT NULL,finished_at DATETIME2 NULL,processed INT NOT NULL,status VARCHAR(20) NOT NULL,error_code VARCHAR(40) NULL);
 CREATE INDEX IX_job_runs_latest ON dbo.job_runs(name,id DESC);
END;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.vaccinations') AND name='IX_vaccinations_due') CREATE INDEX IX_vaccinations_due ON dbo.vaccinations(next_due_date,id);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.orders') AND name='IX_orders_paid_report') CREATE INDEX IX_orders_paid_report ON dbo.orders(paid_at,payment_status,status) INCLUDE(total_amount,payment_method);
