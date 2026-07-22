-- V69__extend_csds_audit_error.sql

ALTER TABLE csds_audit
    ADD COLUMN IF NOT EXISTS error TEXT;
