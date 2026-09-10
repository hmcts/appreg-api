-- Support activity-audit filtering and keyset pagination without rescanning data_audit.
CREATE INDEX IF NOT EXISTS data_audit_activity_report_idx
    ON data_audit (event_name, created_date, data_id)
    WHERE POSITION('_ID' IN UPPER(column_name)) = 0;
