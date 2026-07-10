package uk.gov.hmcts.appregister.csds.ingress.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.csds.ingress.processor.standardapplicant.StandardApplicantIngressRecord;

@Component
public class StandardApplicantIngressDatabaseRowMapper
        implements IngressDatabaseRowMapper<StandardApplicantIngressRecord>,
                RowMapper<StandardApplicantIngressRecord> {
    private static final String TECHNICAL_USERNAME = "CSDS_INGRESS";
    private static final List<String> COLUMNS =
            List.of(
                    "sa_id",
                    "standard_applicant_code",
                    "standard_applicant_start_date",
                    "standard_applicant_end_date",
                    "version",
                    "changed_by",
                    "changed_date",
                    "user_name",
                    "name",
                    "title",
                    "forename_1",
                    "forename_2",
                    "forename_3",
                    "surname",
                    "address_l1",
                    "address_l2",
                    "address_l3",
                    "address_l4",
                    "address_l5",
                    "postcode",
                    "email_address",
                    "telephone_number",
                    "mobile_number");
    private static final List<String> UPDATABLE_COLUMNS = COLUMNS.subList(1, COLUMNS.size());

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
    public Map<String, Object> toRow(StandardApplicantIngressRecord item) {
        var row = new LinkedHashMap<String, Object>();
        row.put("sa_id", item.id());
        row.put("standard_applicant_code", item.code());
        row.put("standard_applicant_start_date", item.startDate());
        row.put("standard_applicant_end_date", item.endDate());
        row.put("version", item.version());
        row.put("changed_by", 0L);
        row.put("changed_date", null);
        row.put("user_name", TECHNICAL_USERNAME);
        row.put("name", item.name());
        row.put("title", null);
        row.put("forename_1", null);
        row.put("forename_2", null);
        row.put("forename_3", null);
        row.put("surname", null);
        row.put("address_l1", item.addressLine1());
        row.put("address_l2", item.addressLine2());
        row.put("address_l3", item.addressLine3());
        row.put("address_l4", item.addressLine4());
        row.put("address_l5", item.addressLine5());
        row.put("postcode", item.postcode());
        row.put("email_address", item.emailAddress());
        row.put("telephone_number", item.telephoneNumber());
        row.put("mobile_number", null);
        return row;
    }

    @Override
    public StandardApplicantIngressRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new StandardApplicantIngressRecord(
                rs.getLong("sa_id"),
                rs.getString("standard_applicant_code"),
                rs.getObject("standard_applicant_start_date", LocalDate.class),
                rs.getObject("standard_applicant_end_date", LocalDate.class),
                rs.getLong("version"),
                rs.getString("name"),
                rs.getString("address_l1"),
                rs.getString("address_l2"),
                rs.getString("address_l3"),
                rs.getString("address_l4"),
                rs.getString("address_l5"),
                rs.getString("postcode"),
                rs.getString("email_address"),
                rs.getString("telephone_number"));
    }
}
