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
    private static final String ID = "sa_id";
    private static final String CODE = "standard_applicant_code";
    private static final String START_DATE = "standard_applicant_start_date";
    private static final String END_DATE = "standard_applicant_end_date";
    private static final String VERSION = "version";
    private static final String CHANGED_BY = "changed_by";
    private static final String CHANGED_DATE = "changed_date";
    private static final String USER_NAME = "user_name";
    private static final String NAME = "name";
    private static final String TITLE = "title";
    private static final String FORENAME_1 = "forename_1";
    private static final String FORENAME_2 = "forename_2";
    private static final String FORENAME_3 = "forename_3";
    private static final String SURNAME = "surname";
    private static final String ADDRESS_1 = "address_l1";
    private static final String ADDRESS_2 = "address_l2";
    private static final String ADDRESS_3 = "address_l3";
    private static final String ADDRESS_4 = "address_l4";
    private static final String ADDRESS_5 = "address_l5";
    private static final String POSTCODE = "postcode";
    private static final String EMAIL_ADDRESS = "email_address";
    private static final String TELEPHONE_NUMBER = "telephone_number";
    private static final String MOBILE_NUMBER = "mobile_number";
    private static final List<String> COLUMNS =
            List.of(
                    ID,
                    CODE,
                    START_DATE,
                    END_DATE,
                    VERSION,
                    CHANGED_BY,
                    CHANGED_DATE,
                    USER_NAME,
                    NAME,
                    TITLE,
                    FORENAME_1,
                    FORENAME_2,
                    FORENAME_3,
                    SURNAME,
                    ADDRESS_1,
                    ADDRESS_2,
                    ADDRESS_3,
                    ADDRESS_4,
                    ADDRESS_5,
                    POSTCODE,
                    EMAIL_ADDRESS,
                    TELEPHONE_NUMBER,
                    MOBILE_NUMBER);
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
        return Map.of(CHANGED_DATE, "current_timestamp");
    }

    @Override
    public Map<String, String> updateExpressions() {
        return Map.of(CHANGED_DATE, "current_timestamp");
    }

    @Override
    public Map<String, Object> toRow(StandardApplicantIngressRecord item) {
        var row = new LinkedHashMap<String, Object>();
        row.put(ID, item.id());
        row.put(CODE, item.code());
        row.put(START_DATE, item.startDate());
        row.put(END_DATE, item.endDate());
        row.put(VERSION, item.version());
        row.put(CHANGED_BY, 0L);
        row.put(CHANGED_DATE, null);
        row.put(USER_NAME, TECHNICAL_USERNAME);
        row.put(NAME, item.name());
        row.put(TITLE, null);
        row.put(FORENAME_1, null);
        row.put(FORENAME_2, null);
        row.put(FORENAME_3, null);
        row.put(SURNAME, null);
        row.put(ADDRESS_1, item.addressLine1());
        row.put(ADDRESS_2, item.addressLine2());
        row.put(ADDRESS_3, item.addressLine3());
        row.put(ADDRESS_4, item.addressLine4());
        row.put(ADDRESS_5, item.addressLine5());
        row.put(POSTCODE, item.postcode());
        row.put(EMAIL_ADDRESS, item.emailAddress());
        row.put(TELEPHONE_NUMBER, item.telephoneNumber());
        row.put(MOBILE_NUMBER, null);
        return row;
    }

    @Override
    public StandardApplicantIngressRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new StandardApplicantIngressRecord(
                rs.getLong(ID),
                rs.getString(CODE),
                rs.getObject(START_DATE, LocalDate.class),
                rs.getObject(END_DATE, LocalDate.class),
                rs.getLong(VERSION),
                rs.getString(NAME),
                rs.getString(ADDRESS_1),
                rs.getString(ADDRESS_2),
                rs.getString(ADDRESS_3),
                rs.getString(ADDRESS_4),
                rs.getString(ADDRESS_5),
                rs.getString(POSTCODE),
                rs.getString(EMAIL_ADDRESS),
                rs.getString(TELEPHONE_NUMBER));
    }
}
