-- Catalog additions; no legacy data is deleted or silently reconciled.
IF OBJECT_ID('dbo.admin_catalog_state','U') IS NULL CREATE TABLE dbo.admin_catalog_state(id INT PRIMARY KEY);
IF NOT EXISTS(SELECT 1 FROM dbo.admin_catalog_state WHERE id=1) INSERT INTO dbo.admin_catalog_state(id) VALUES(1);
IF COL_LENGTH('dbo.product_categories','display_order') IS NULL ALTER TABLE dbo.product_categories ADD display_order INT NOT NULL CONSTRAINT DF_category_display_order DEFAULT 0;
IF COL_LENGTH('dbo.orders','reward_earn_every') IS NULL ALTER TABLE dbo.orders ADD reward_earn_every DECIMAL(18,2) NULL;
IF COL_LENGTH('dbo.promotions','restricted_audience') IS NULL ALTER TABLE dbo.promotions ADD restricted_audience BIT NOT NULL CONSTRAINT DF_promotion_audience DEFAULT 0;
IF EXISTS(SELECT UPPER(code) FROM dbo.promotions GROUP BY UPPER(code) HAVING COUNT(*)>1) THROW 50019, 'Duplicate promotion codes ignoring case: reconcile manually before migration', 1;
IF COL_LENGTH('dbo.promotions','normalized_code') IS NULL ALTER TABLE dbo.promotions ADD normalized_code AS UPPER(code) PERSISTED;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.promotions') AND name='UX_promotion_code_normalized') CREATE UNIQUE INDEX UX_promotion_code_normalized ON dbo.promotions(normalized_code);
IF OBJECT_ID('dbo.loyalty_rules','U') IS NULL
BEGIN
 CREATE TABLE dbo.loyalty_rules(id INT PRIMARY KEY,earn_every DECIMAL(18,2) NOT NULL,point_value DECIMAL(18,2) NOT NULL,silver INT NOT NULL,gold INT NOT NULL,platinum INT NOT NULL,
 CONSTRAINT CK_loyalty_rules CHECK(id=1 AND earn_every>0 AND point_value>0 AND silver>0 AND gold>silver AND platinum>gold));
 INSERT INTO dbo.loyalty_rules VALUES(1,10000,100,1000,5000,10000);
END;
IF OBJECT_ID('dbo.CK_loyalty_source_type','C') IS NOT NULL ALTER TABLE dbo.loyalty_transactions DROP CONSTRAINT CK_loyalty_source_type;
ALTER TABLE dbo.loyalty_transactions ADD CONSTRAINT CK_loyalty_source_type CHECK (source_type IN ('PURCHASE','SERVICE_BOOKING','REVIEW','CARE_PACKAGE','REDEMPTION','MANUAL_ADJUSTMENT'));
IF OBJECT_ID('dbo.business_hours','U') IS NULL
BEGIN
 CREATE TABLE dbo.business_hours(id BIGINT IDENTITY PRIMARY KEY,weekday INT NULL,work_date DATE NULL,staff_id BIGINT NULL REFERENCES dbo.users(id),start_time TIME(6) NULL,end_time TIME(6) NULL,closed BIT NOT NULL,reason NVARCHAR(500) NULL,
 CONSTRAINT CK_business_hours_weekday CHECK(weekday IS NULL OR weekday BETWEEN 1 AND 7), CONSTRAINT CK_business_hours_range CHECK(start_time<end_time));
 CREATE INDEX IX_business_hours_lookup ON dbo.business_hours(staff_id,work_date,weekday);
END;
IF OBJECT_ID('dbo.holidays','U') IS NULL
BEGIN
 CREATE TABLE dbo.holidays(id BIGINT IDENTITY PRIMARY KEY,weekday INT NULL,work_date DATE NULL,staff_id BIGINT NULL REFERENCES dbo.users(id),start_time TIME(6) NULL,end_time TIME(6) NULL,closed BIT NOT NULL,reason NVARCHAR(500) NULL,
 CONSTRAINT CK_holidays_weekday CHECK(weekday IS NULL OR weekday BETWEEN 1 AND 7), CONSTRAINT CK_holidays_range CHECK(start_time<end_time));
 CREATE INDEX IX_holidays_lookup ON dbo.holidays(staff_id,work_date,weekday);
END;
IF OBJECT_ID('dbo.staff_shifts','U') IS NULL
BEGIN
 CREATE TABLE dbo.staff_shifts(id BIGINT IDENTITY PRIMARY KEY,weekday INT NULL,work_date DATE NULL,staff_id BIGINT NULL REFERENCES dbo.users(id),start_time TIME(6) NULL,end_time TIME(6) NULL,closed BIT NOT NULL,reason NVARCHAR(500) NULL,
 CONSTRAINT CK_staff_shifts_weekday CHECK(weekday IS NULL OR weekday BETWEEN 1 AND 7), CONSTRAINT CK_staff_shifts_range CHECK(start_time<end_time));
 CREATE INDEX IX_staff_shifts_lookup ON dbo.staff_shifts(staff_id,work_date,weekday);
END;
IF OBJECT_ID('dbo.staff_time_off','U') IS NULL
BEGIN
 CREATE TABLE dbo.staff_time_off(id BIGINT IDENTITY PRIMARY KEY,weekday INT NULL,work_date DATE NULL,staff_id BIGINT NULL REFERENCES dbo.users(id),start_time TIME(6) NULL,end_time TIME(6) NULL,closed BIT NOT NULL,reason NVARCHAR(500) NULL,
 CONSTRAINT CK_staff_time_off_weekday CHECK(weekday IS NULL OR weekday BETWEEN 1 AND 7), CONSTRAINT CK_staff_time_off_range CHECK(start_time<end_time));
 CREATE INDEX IX_staff_time_off_lookup ON dbo.staff_time_off(staff_id,work_date,weekday);
END;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.business_hours') AND name='UX_business_hours_day') CREATE UNIQUE INDEX UX_business_hours_day ON dbo.business_hours(weekday);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.holidays') AND name='UX_holiday_date') CREATE UNIQUE INDEX UX_holiday_date ON dbo.holidays(work_date);
IF OBJECT_ID('dbo.promotion_audience','U') IS NULL
BEGIN
 CREATE TABLE dbo.promotion_audience(id BIGINT IDENTITY PRIMARY KEY,promotion_id BIGINT NOT NULL REFERENCES dbo.promotions(id),user_id BIGINT NOT NULL REFERENCES dbo.users(id));
 CREATE UNIQUE INDEX UX_promotion_audience ON dbo.promotion_audience(promotion_id,user_id);
END;
IF OBJECT_ID('dbo.promotion_campaigns','U') IS NULL
BEGIN
 CREATE TABLE dbo.promotion_campaigns(id BIGINT IDENTITY PRIMARY KEY,request_key VARCHAR(36) NOT NULL UNIQUE,actor_id BIGINT NOT NULL REFERENCES dbo.users(id),promotion_id BIGINT NOT NULL REFERENCES dbo.promotions(id),created_at DATETIME2 NOT NULL,recipient_count INT NOT NULL,fingerprint VARCHAR(64) NOT NULL);
 CREATE INDEX IX_campaign_actor_time ON dbo.promotion_campaigns(actor_id,created_at);
END;
