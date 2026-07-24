package uk.gov.hmcts.appregister.csds.ingress.database;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JdbcBulkUpsertService {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcBatchFailureIsolationService jdbcBatchFailureIsolationService;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    public <T> int[] upsertBatch(
            String tableName,
            String primaryKey,
            List<T> records,
            IngressDatabaseRowMapper<T> rowMapper) {
        return upsertBatch(tableName, primaryKey, records, rowMapper, item -> null);
    }

    public <T> int[] upsertBatch(
            String tableName,
            String primaryKey,
            List<T> records,
            IngressDatabaseRowMapper<T> rowMapper,
            Function<T, Long> keyExtractor) {
        if (records.isEmpty()) {
            return new int[0];
        }

        CsdsSqlIdentifierValidator.requireValid(tableName, "tableName");
        CsdsSqlIdentifierValidator.requireValid(primaryKey, "primaryKey");
        rowMapper
                .columns()
                .forEach(column -> CsdsSqlIdentifierValidator.requireValid(column, "column"));
        rowMapper
                .updatableColumns()
                .forEach(
                        column ->
                                CsdsSqlIdentifierValidator.requireValid(column, "updatableColumn"));
        rowMapper
                .insertExpressions()
                .keySet()
                .forEach(
                        column ->
                                CsdsSqlIdentifierValidator.requireValid(
                                        column, "insertExpression"));
        rowMapper
                .updateExpressions()
                .keySet()
                .forEach(
                        column ->
                                CsdsSqlIdentifierValidator.requireValid(
                                        column, "updateExpression"));

        val sql = buildUpsertSql(tableName, primaryKey, rowMapper);
        val parameters =
                records.stream()
                        .map(rowMapper::toRow)
                        .map(MapSqlParameterSource::new)
                        .toArray(MapSqlParameterSource[]::new);

        try {
            return jdbcTemplate.batchUpdate(sql, parameters);
        } catch (DataAccessException ex) {
            var failures =
                    jdbcBatchFailureIsolationService.identifyFailures(
                            sql, records, rowMapper::toRow, keyExtractor, ex);
            throw new CsdsBatchUpsertException(
                    "CSDS batch upsert failed for " + tableName + "." + primaryKey,
                    ex,
                    new ArrayList<>(failures));
        }
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
}
