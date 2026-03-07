-- V9: Remove legacy ownership_user_id from product_assets (ownership tracked in passport_ownership)
ALTER TABLE product_assets DROP CONSTRAINT IF EXISTS fk_product_assets_owner;
ALTER TABLE product_assets DROP COLUMN IF EXISTS ownership_user_id;
