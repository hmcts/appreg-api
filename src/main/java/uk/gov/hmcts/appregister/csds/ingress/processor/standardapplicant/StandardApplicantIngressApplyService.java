package uk.gov.hmcts.appregister.csds.ingress.processor.standardapplicant;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.csds.ingress.database.CsdsSqlIdentifierValidator;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcBulkUpsertService;
import uk.gov.hmcts.appregister.csds.ingress.database.StandardApplicantIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;

@Service
@RequiredArgsConstructor
@SuppressWarnings("java:S2077") // SQL identifiers are restricted to lowercase letters, digits and
// underscores.
public class StandardApplicantIngressApplyService {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcBulkUpsertService bulkUpsertService;
    private final StandardApplicantIngressDatabaseRowMapper rowMapper;
    private final BusinessDateProvider businessDateProvider;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    @Transactional
    public void reconcileAndUpsert(
            String targetTable, String targetKeyField, StandardApplicantDiffResult diff) {
        val validatedTableName = CsdsSqlIdentifierValidator.requireValid(targetTable, "tableName");
        val validatedPrimaryKey =
                CsdsSqlIdentifierValidator.requireValid(targetKeyField, "primaryKey");
        endDateMissingApplicants(validatedTableName, diff.incomingById());
        val rows = diff.diffRecords().stream().map(IngressDiffRecord::intended).toList();
        bulkUpsertService.upsertBatch(
                validatedTableName,
                validatedPrimaryKey,
                rows,
                rowMapper,
                StandardApplicantIngressRecord::id);
    }

    private void endDateMissingApplicants(
            String targetTable, Map<Long, StandardApplicantIngressRecord> incomingById) {
        val validatedSchema = CsdsSqlIdentifierValidator.requireValid(schema, "schema");
        var sql =
                """
                UPDATE %s.%s
                SET standard_applicant_end_date = :today,
                    version = version + 1,
                    changed_by = 0,
                    changed_date = current_timestamp,
                    user_name = 'CSDS_INGRESS'
                WHERE standard_applicant_end_date IS NULL
                """
                        // Schema and table name are regex-validated SQL identifiers.
                        .formatted(validatedSchema, targetTable); // NOSONAR
        var parameters = new HashMap<String, Object>();
        parameters.put("today", businessDateProvider.currentUkDate());
        if (!incomingById.isEmpty()) {
            // A new version must not end-date older versions of a code still in the feed.
            sql += " AND standard_applicant_code NOT IN (:incomingCodes)";
            parameters.put(
                    "incomingCodes",
                    incomingById.values().stream()
                            .map(StandardApplicantIngressRecord::code)
                            .distinct()
                            .toList());
        }
        jdbcTemplate.update(sql, parameters);
    }
}
