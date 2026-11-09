SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID('dbo.wishlist_items','U') IS NULL
BEGIN
 CREATE TABLE dbo.wishlist_items (
   id bigint IDENTITY PRIMARY KEY, user_id bigint NOT NULL REFERENCES dbo.users(id),
   type varchar(12) NOT NULL, product_id bigint REFERENCES dbo.products(id), pet_id bigint REFERENCES dbo.pets(id),
   created_at datetime2 NOT NULL, updated_at datetime2 NULL,
   CONSTRAINT CK_wishlist_target CHECK ((type='PRODUCT' AND product_id IS NOT NULL AND pet_id IS NULL)
     OR (type='PET' AND pet_id IS NOT NULL AND product_id IS NULL))
 );
 CREATE UNIQUE INDEX UX_wishlist_product ON dbo.wishlist_items(user_id,product_id) WHERE product_id IS NOT NULL;
 CREATE UNIQUE INDEX UX_wishlist_pet ON dbo.wishlist_items(user_id,pet_id) WHERE pet_id IS NOT NULL;
 CREATE INDEX IX_wishlist_user ON dbo.wishlist_items(user_id,id DESC);
END;
COMMIT;
