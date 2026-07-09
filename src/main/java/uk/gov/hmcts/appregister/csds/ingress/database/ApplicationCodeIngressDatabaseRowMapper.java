package uk.gov.hmcts.appregister.csds.ingress.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.common.enumeration.YesOrNo;
import uk.gov.hmcts.appregister.csds.ingress.processor.applicationcode.ApplicationCodeIngressRecord;

@Component
public class ApplicationCodeIngressDatabaseRowMapper
        implements IngressDatabaseRowMapper<ApplicationCodeIngressRecord>,
                RowMapper<ApplicationCodeIngressRecord> {
    private static final String TECHNICAL_USERNAME = "CSDS_INGRESS";
    private static final List<String> COLUMNS =
            List.of(
                    "ac_id",
                    "application_code",
                    "application_code_title",
                    "application_code_wording",
                    "application_legislation",
                    "fee_due",
                    "application_code_respondent",
                    "ac_destination_email_address_1",
                    "ac_destination_email_address_2",
                    "application_code_start_date",
                    "application_code_end_date",
                    "bulk_respondent_allowed",
                    "version",
                    "changed_by",
                    "changed_date",
                    "user_name",
                    "ac_fee_reference");
    private static final List<String> UPDATABLE_COLUMNS =
            List.of(
                    "application_code",
                    "application_code_title",
                    "application_code_wording",
                    "application_legislation",
                    "fee_due",
                    "application_code_respondent",
                    "ac_destination_email_address_1",
                    "ac_destination_email_address_2",
                    "application_code_start_date",
                    "application_code_end_date",
                    "bulk_respondent_allowed",
                    "version",
                    "changed_by",
                    "changed_date",
                    "user_name",
                    "ac_fee_reference");

    @Override
    public List<String> columns() {
        return COLUMNS;
    }

    @Override
    public List<String> updatableColumns() {
        return UPDATABLE_COLUMNS;
    }

    @Override
    public Map<String, String> insertExpressions() {
        return Map.of("changed_date", "current_timestamp");
    }

    @Override
    public Map<String, String> updateExpressions() {
        return Map.of("changed_date", "current_timestamp");
    }

    @Override
    public Map<String, Object> toRow(ApplicationCodeIngressRecord item) {
        var row = new LinkedHashMap<String, Object>();
        row.put("ac_id", item.id());
        row.put("application_code", item.code());
        row.put("application_code_title", item.title());
        row.put("application_code_wording", item.wording());
        row.put("application_legislation", item.legislation());
        row.put("fee_due", item.feeDue().getValue());
        row.put("application_code_respondent", item.requiresRespondent().getValue());
        row.put("ac_destination_email_address_1", null);
        row.put("ac_destination_email_address_2", null);
        row.put("application_code_start_date", item.startDate());
        row.put("application_code_end_date", item.endDate());
        row.put("bulk_respondent_allowed", item.bulkRespondentAllowed().getValue());
        row.put("version", item.version());
        row.put("changed_by", 0L);
        row.put("changed_date", null); // ...updated upon handling.
        row.put("user_name", TECHNICAL_USERNAME);
        row.put("ac_fee_reference", item.feeReference());
        return row;
    }

    @Override
    public ApplicationCodeIngressRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ApplicationCodeIngressRecord(
                rs.getLong("ac_id"),
                rs.getString("application_code"),
                rs.getString("application_code_title"),
                rs.getString("application_code_wording"),
                rs.getString("application_legislation"),
                YesOrNo.fromValue(rs.getString("fee_due")),
                YesOrNo.fromValue(rs.getString("application_code_respondent")),
                rs.getObject("application_code_start_date", LocalDate.class),
                rs.getObject("application_code_end_date", LocalDate.class),
                YesOrNo.fromValue(rs.getString("bulk_respondent_allowed")),
                rs.getLong("version"),
                rs.getString("ac_fee_reference"));
    }
}
