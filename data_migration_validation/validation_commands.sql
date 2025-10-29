"c:\Program Files\PostgreSQL\16\bin\psql.exe" --set=ON_ERROR_STOP=1 -c "\copy data_validation.oracle_column_metadata(owner, table_name, column_name, data_type, char_length, nullable, suggested_pg_type) FROM 'oracle_metadata.csv' CSV QUOTE ''''" "postgresql://hmcts:<pwd>@appreg-dev.postgres.database.azure.com:5432/appreg-db"

"c:\Program Files\PostgreSQL\16\bin\psql.exe" --set=ON_ERROR_STOP=1 -c "\copy data_validation.oracle_rowcounts(owner, table_name, row_count) FROM 'oracle_rowcounts.csv' CSV QUOTE ''''" "postgresql://hmcts:<pwd>@appreg-dev.postgres.database.azure.com:5432/appreg-db"

"c:\Program Files\PostgreSQL\16\bin\psql.exe" --set=ON_ERROR_STOP=1 -c "\copy data_validation.oracle_counts_by_date(owner, table_name, bucket_label, row_count) FROM 'oracle_counts_by_date.csv' CSV QUOTE ''''" "postgresql://hmcts:<pwd>@appreg-dev.postgres.database.azure.com:5432/appreg-db"

"c:\Program Files\PostgreSQL\16\bin\psql.exe" --set=ON_ERROR_STOP=1 -c "\copy data_validation.oracle_column_analysis(owner, table_name, column_name, metric, metric_value) FROM 'oracle_column_analysis.csv' CSV QUOTE ''''" "postgresql://hmcts:<pwd>@appreg-dev.postgres.database.azure.com:5432/appreg-db"

-- populate the postgres side
select data_validation.refresh_pg_counts();
select data_validation.refresh_pg_buckets();
select data_validation.refresh_pg_column_analysis();
select data_validation.refresh_summary();

-- comparison queries
select * from data_validation.v_metadata;

select * from data_validation.v_count_diff where count_diff <> 0;

select * from data_validation.v_bucket_diff where bucket_diff <> 0;

select * from data_validation.v_minmax_diff
where (NULLIF(btrim(o_min),' ')
IS DISTINCT FROM NULLIF(btrim(p_min),' ')
OR (NULLIF(btrim(o_max),' ')
IS DISTINCT FROM NULLIF(btrim(p_max),' ')));
