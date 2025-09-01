-- Create an app-specific schema
CREATE SCHEMA IF NOT EXISTS app_schema AUTHORIZATION app_reg_user;

-- (Optional) tighten search_path so unqualified names hit your schema
ALTER ROLE app_user IN DATABASE app_db SET search_path = app_schema, public;
