SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.order_items','care_package_id') IS NULL
  ALTER TABLE dbo.order_items ADD care_package_id bigint NULL REFERENCES dbo.care_packages(id),
    beneficiary_pet_id bigint NULL REFERENCES dbo.pets(id), package_name nvarchar(255) NULL, package_months int NULL,
    package_grooming bit NULL, package_checkup bit NULL, package_vaccination bit NULL;
IF COL_LENGTH('dbo.care_package_subscriptions','order_item_id') IS NULL
  ALTER TABLE dbo.care_package_subscriptions ADD order_item_id bigint NULL REFERENCES dbo.order_items(id);
GO
IF OBJECT_ID('dbo.CK_order_items_package_ref','C') IS NULL
BEGIN
 ALTER TABLE dbo.order_items DROP CONSTRAINT IF EXISTS CK_order_items_type, CK_order_items_ref;
 ALTER TABLE dbo.order_items WITH CHECK ADD CONSTRAINT CK_order_items_package_ref CHECK (
   (item_type='PET' AND pet_id IS NOT NULL AND product_id IS NULL AND care_package_id IS NULL AND beneficiary_pet_id IS NULL)
   OR (item_type='PRODUCT' AND product_id IS NOT NULL AND pet_id IS NULL AND care_package_id IS NULL AND beneficiary_pet_id IS NULL)
   OR (item_type='CARE_PACKAGE' AND pet_id IS NULL AND product_id IS NULL AND care_package_id IS NOT NULL
       AND beneficiary_pet_id IS NOT NULL AND quantity=1 AND package_months IS NOT NULL AND package_months>0
       AND package_name IS NOT NULL AND package_grooming IS NOT NULL AND package_checkup IS NOT NULL AND package_vaccination IS NOT NULL)
 );
END;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.care_package_subscriptions') AND name='UX_subscription_item')
 CREATE UNIQUE INDEX UX_subscription_item ON dbo.care_package_subscriptions(order_item_id) WHERE order_item_id IS NOT NULL;
COMMIT;
