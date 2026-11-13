IF OBJECT_ID('dbo.admin_audit_log', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.admin_audit_log (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        occurred_at DATETIME2(7) NOT NULL,
        actor_user_id BIGINT NOT NULL,
        actor_role VARCHAR(255) NOT NULL,
        action VARCHAR(80) NOT NULL,
        target_type VARCHAR(80) NOT NULL,
        target_id BIGINT NULL,
        summary NVARCHAR(500) NOT NULL,
        details NVARCHAR(2000) NOT NULL,
        ip VARCHAR(64) NULL,
        correlation_id VARCHAR(36) NOT NULL,
        result VARCHAR(20) NOT NULL,
        CONSTRAINT CK_admin_audit_result CHECK(result IN ('SUCCESS','DENIED','FAILED'))
    );
    CREATE INDEX IX_admin_audit_time ON dbo.admin_audit_log(occurred_at DESC,id DESC);
    CREATE INDEX IX_admin_audit_actor ON dbo.admin_audit_log(actor_user_id,occurred_at);
    CREATE INDEX IX_admin_audit_target ON dbo.admin_audit_log(target_type,target_id,occurred_at);
    CREATE INDEX IX_admin_audit_action ON dbo.admin_audit_log(action,occurred_at);
END;
GO
CREATE OR ALTER TRIGGER dbo.TR_admin_audit_append_only ON dbo.admin_audit_log
INSTEAD OF UPDATE, DELETE AS
BEGIN
    THROW 51001, 'Administrative audit records are append-only', 1;
END;
