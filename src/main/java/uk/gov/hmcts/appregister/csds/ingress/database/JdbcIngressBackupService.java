package uk.gov.hmcts.appregister.csds.ingress.database;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.csds.ingress.service.CsdsIngressTransactionRunner;

@Component
@RequiredArgsConstructor
@SuppressWarnings("java:S2077") // Schema and table names are restricted to lowercase letters,
// digits and underscores.
public class JdbcIngressBackupService {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CsdsIngressTransactionRunner csdsIngressTransactionRunner;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    public BackupResult backup(String sourceTable, String targetTable) {
        val validatedSchema = CsdsSqlIdentifierValidator.requireValid(schema, "schema");
        val validatedSourceTable =
                CsdsSqlIdentifierValidator.requireValid(sourceTable, "sourceTable");
        val validatedTargetTable =
                CsdsSqlIdentifierValidator.requireValid(targetTable, "targetTable");

        return csdsIngressTransactionRunner.execute(
                () -> {
                    // Schema and table names are regex-validated SQL identifiers.
                    val deleteSql = "DELETE FROM " + validatedSchema + "." + validatedTargetTable;
                    val insertSql =
                            """
                            INSERT INTO %s.%s
                            SELECT * FROM %s.%s
                            """
                                    .formatted(
                                            validatedSchema,
                                            validatedTargetTable,
                                            validatedSchema,
                                            validatedSourceTable);
                    var deletedCount = jdbcTemplate.update(deleteSql, Map.of());
                    var insertedCount = jdbcTemplate.update(insertSql, Map.of());
                    return new BackupResult(deletedCount, insertedCount);
                });
    }

    public record BackupResult(int deletedCount, int insertedCount) {}
}
