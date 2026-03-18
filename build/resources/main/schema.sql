CREATE SCHEMA IF NOT EXISTS goldentime;
CREATE SCHEMA IF NOT EXISTS user_management;

-- is_active 컬럼이 존재할 경우 삭제 (엔티티에서 삭제됨)
ALTER TABLE user_management.tb_user_vehicle DROP COLUMN IF EXISTS is_active;
