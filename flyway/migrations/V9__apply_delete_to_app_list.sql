ALTER TABLE application_lists
ADD COLUMN delete_by VARCHAR(73),
ADD COLUMN delete_date TIMESTAMP,
ADD COLUMN is_deleted CHAR(1) DEFAULT 'N' CHECK (is_deleted IN ('Y', 'N'));

