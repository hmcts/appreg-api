package uk.gov.hmcts.appregister.csds.ingress.database;

import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JdbcBulkUpsertService {
    private static final Pattern SQL_IDENTIFIER = Pattern.compile("[a-z][a-z0-9_]*");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    public <T> int[] upsertBatch(
            String tableName,
            String primaryKey,
            List<T> records,
            IngressDatabaseRowMapper<T> rowMapper) {
        if (records.isEmpty()) {
            return new int[0];
        }

        validateIdentifier(tableName, "tableName");
        validateIdentifier(primaryKey, "primaryKey");
        rowMapper.columns().forEach(column -> validateIdentifier(column, "column"));
        rowMapper
                .updatableColumns()
                .forEach(column -> validateIdentifier(column, "updatableColumn"));
        rowMapper
                .insertExpressions()
                .keySet()
                .forEach(column -> validateIdentifier(column, "insertExpression"));
        rowMapper
                .updateExpressions()
                .keySet()
                .forEach(column -> validateIdentifier(column, "updateExpression"));

        val sql = buildUpsertSql(tableName, primaryKey, rowMapper);
        val parameters =
                records.stream()
                        .map(rowMapper::toRow)
                        .map(MapSqlParameterSource::new)
                        .toArray(MapSqlParameterSource[]::new);

        return jdbcTemplate.batchUpdate(sql, parameters);
    }

    String buildUpsertSql(
            String tableName, String primaryKey, IngressDatabaseRowMapper<?> rowMapper) {
        val insertColumns = String.join(", ", rowMapper.columns());
        val insertValues =
                rowMapper.columns().stream()
                        .map(
                                column ->
                                        rowMapper
                                                .insertExpressions()
                                                .getOrDefault(column, ":" + column))
                        .toList();
        val updateAssignments =
                rowMapper.updatableColumns().stream()
                        .map(
                                column ->
                                        column
                                                + " = "
                                                + rowMapper
                                                        .updateExpressions()
                                                        .getOrDefault(column, "EXCLUDED." + column))
                        .toList();

        return """
               INSERT INTO %s.%s (%s)
               VALUES (%s)
               ON CONFLICT (%s) DO UPDATE
               SET %s
               """
                .formatted(
                        schema,
                        tableName,
                        insertColumns,
                        String.join(", ", insertValues),
                        primaryKey,
                        String.join(", ", updateAssignments));
    }

    private void validateIdentifier(String value, String description) {
        if (!StringUtils.hasText(value) || !SQL_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid SQL " + description + ": " + value);
        }
    }
}
