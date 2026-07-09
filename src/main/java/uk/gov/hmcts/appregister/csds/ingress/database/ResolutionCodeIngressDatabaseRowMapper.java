package uk.gov.hmcts.appregister.csds.ingress.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.csds.ingress.processor.resolutioncode.ResolutionCodeIngressRecord;

@Component
public class ResolutionCodeIngressDatabaseRowMapper
        implements IngressDatabaseRowMapper<ResolutionCodeIngressRecord>,
                RowMapper<ResolutionCodeIngressRecord> {
    private static final String TECHNICAL_USERNAME = "CSDS_INGRESS";
    private static final List<String> COLUMNS =
            List.of(
                    "rc_id",
                    "resolution_code",
                    "resolution_code_title",
                    "resolution_code_wording",
                    "resolution_legislation",
                    "rc_destination_email_address_1",
                    "rc_destination_email_address_2",
                    "resolution_code_start_date",
                    "resolution_code_end_date",
                    "version",
                    "changed_by",
                    "changed_date",
                    "user_name");
    private static final List<String> UPDATABLE_COLUMNS =
            List.of(
                    "resolution_code",
                    "resolution_code_title",
                    "resolution_code_wording",
                    "resolution_legislation",
                    "rc_destination_email_address_1",
                    "rc_destination_email_address_2",
                    "resolution_code_start_date",
                    "resolution_code_end_date",
                    "version",
                    "changed_by",
                    "changed_date",
                    "user_name");

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
    public Map<String, Object> toRow(ResolutionCodeIngressRecord item) {
        var row = new LinkedHashMap<String, Object>();
        row.put("rc_id", item.id());
        row.put("resolution_code", item.code());
        row.put("resolution_code_title", item.title());
        row.put("resolution_code_wording", item.wording());
        row.put("resolution_legislation", item.legislation());
        row.put("rc_destination_email_address_1", item.recipient1Email());
        row.put("rc_destination_email_address_2", item.recipient2Email());
        row.put("resolution_code_start_date", item.startDate());
        row.put("resolution_code_end_date", item.endDate());
        row.put("version", item.version());
        row.put("changed_by", 0L);
        row.put("changed_date", null); // ...updated upon handling.
        row.put("user_name", TECHNICAL_USERNAME);
        return row;
    }

    @Override
    public ResolutionCodeIngressRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ResolutionCodeIngressRecord(
                rs.getLong("rc_id"),
                rs.getString("resolution_code"),
                rs.getString("resolution_code_title"),
                rs.getString("resolution_code_wording"),
                rs.getString("resolution_legislation"),
                rs.getString("rc_destination_email_address_1"),
                rs.getString("rc_destination_email_address_2"),
                rs.getObject("resolution_code_start_date", java.time.LocalDate.class),
                rs.getObject("resolution_code_end_date", java.time.LocalDate.class),
                rs.getLong("version"));
    }
}
