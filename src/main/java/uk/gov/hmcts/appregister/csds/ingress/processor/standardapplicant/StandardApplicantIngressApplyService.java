package uk.gov.hmcts.appregister.csds.ingress.processor.standardapplicant;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.appregister.common.service.BusinessDateProvider;
import uk.gov.hmcts.appregister.csds.ingress.database.JdbcBulkUpsertService;
import uk.gov.hmcts.appregister.csds.ingress.database.StandardApplicantIngressDatabaseRowMapper;
import uk.gov.hmcts.appregister.csds.ingress.diff.IngressDiffRecord;

@Service
@RequiredArgsConstructor
public class StandardApplicantIngressApplyService {
    private static final Pattern SQL_IDENTIFIER = Pattern.compile("[a-z][a-z0-9_]*");
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcBulkUpsertService bulkUpsertService;
    private final StandardApplicantIngressDatabaseRowMapper rowMapper;
    private final BusinessDateProvider businessDateProvider;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    @Transactional
    public void reconcileAndUpsert(
            String targetTable, String targetKeyField, StandardApplicantDiffResult diff) {
        validateIdentifier(targetTable, "tableName");
        validateIdentifier(targetKeyField, "primaryKey");
        endDateMissingApplicants(targetTable, targetKeyField, diff.incomingById());
        val rows = diff.diffRecords().stream().map(IngressDiffRecord::intended).toList();
        bulkUpsertService.upsertBatch(targetTable, targetKeyField, rows, rowMapper);
    }

    private void endDateMissingApplicants(
            String targetTable,
            String targetKeyField,
            Map<Long, StandardApplicantIngressRecord> incomingById) {
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
                        .formatted(schema, targetTable);
        var parameters = new HashMap<String, Object>();
        parameters.put("today", businessDateProvider.currentUkDate());
        if (!incomingById.isEmpty()) {
            sql += " AND " + targetKeyField + " NOT IN (:incomingIds)";
            parameters.put("incomingIds", incomingById.keySet());
        }
        jdbcTemplate.update(sql, parameters);
    }

    private void validateIdentifier(String value, String description) {
        if (!StringUtils.hasText(value) || !SQL_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid SQL " + description + ": " + value);
        }
    }
}
