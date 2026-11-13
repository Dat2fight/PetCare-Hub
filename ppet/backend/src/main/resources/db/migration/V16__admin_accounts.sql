-- Existing accounts retain their credentials and must log in again (JWT version claim required).
IF COL_LENGTH('dbo.users', 'token_version') IS NULL
    ALTER TABLE dbo.users ADD token_version BIGINT NOT NULL CONSTRAINT DF_users_token_version DEFAULT 0;
IF COL_LENGTH('dbo.users', 'must_change_password') IS NULL
    ALTER TABLE dbo.users ADD must_change_password BIT NOT NULL CONSTRAINT DF_users_must_change_password DEFAULT 0;
IF COL_LENGTH('dbo.users', 'deleted_at') IS NULL
    ALTER TABLE dbo.users ADD deleted_at DATETIME2(0) NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.users') AND name='IX_users_status_id')
    CREATE INDEX IX_users_status_id ON dbo.users(deleted_at, enabled, id);
-- Keep appointments when an employee leaves, ready for explicit reassignment.
ALTER TABLE dbo.appointments ALTER COLUMN staff_id BIGINT NULL;
