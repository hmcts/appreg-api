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
    private static final String AC_ID = "ac_id";
    private static final String APPLICATION_CODE = "application_code";
    private static final String APPLICATION_CODE_TITLE = "application_code_title";
    private static final String APPLICATION_CODE_WORDING = "application_code_wording";
    private static final String APPLICATION_LEGISLATION = "application_legislation";
    private static final String FEE_DUE = "fee_due";
    private static final String APPLICATION_CODE_RESPONDENT = "application_code_respondent";
    private static final String DESTINATION_EMAIL_ADDRESS_1 = "ac_destination_email_address_1";
    private static final String DESTINATION_EMAIL_ADDRESS_2 = "ac_destination_email_address_2";
    private static final String APPLICATION_CODE_START_DATE = "application_code_start_date";
    private static final String APPLICATION_CODE_END_DATE = "application_code_end_date";
    private static final String BULK_RESPONDENT_ALLOWED = "bulk_respondent_allowed";
    private static final String VERSION = "version";
    private static final String CHANGED_BY = "changed_by";
    private static final String CHANGED_DATE = "changed_date";
    private static final String USER_NAME = "user_name";
    private static final String AC_FEE_REFERENCE = "ac_fee_reference";
    private static final List<String> COLUMNS =
            List.of(
                    AC_ID,
                    APPLICATION_CODE,
                    APPLICATION_CODE_TITLE,
                    APPLICATION_CODE_WORDING,
                    APPLICATION_LEGISLATION,
                    FEE_DUE,
                    APPLICATION_CODE_RESPONDENT,
                    DESTINATION_EMAIL_ADDRESS_1,
                    DESTINATION_EMAIL_ADDRESS_2,
                    APPLICATION_CODE_START_DATE,
                    APPLICATION_CODE_END_DATE,
                    BULK_RESPONDENT_ALLOWED,
                    VERSION,
                    CHANGED_BY,
                    CHANGED_DATE,
                    USER_NAME,
                    AC_FEE_REFERENCE);
    private static final List<String> UPDATABLE_COLUMNS =
            List.of(
                    APPLICATION_CODE,
                    APPLICATION_CODE_TITLE,
                    APPLICATION_CODE_WORDING,
                    APPLICATION_LEGISLATION,
                    FEE_DUE,
                    APPLICATION_CODE_RESPONDENT,
                    DESTINATION_EMAIL_ADDRESS_1,
                    DESTINATION_EMAIL_ADDRESS_2,
                    APPLICATION_CODE_START_DATE,
                    APPLICATION_CODE_END_DATE,
                    BULK_RESPONDENT_ALLOWED,
                    VERSION,
                    CHANGED_BY,
                    CHANGED_DATE,
                    USER_NAME,
                    AC_FEE_REFERENCE);

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
    public Map<String, Object> toRow(ApplicationCodeIngressRecord item) {
        var row = new LinkedHashMap<String, Object>();
        row.put(AC_ID, item.id());
        row.put(APPLICATION_CODE, item.code());
        row.put(APPLICATION_CODE_TITLE, item.title());
        row.put(APPLICATION_CODE_WORDING, item.wording());
        row.put(APPLICATION_LEGISLATION, item.legislation());
        row.put(FEE_DUE, item.feeDue().getValue());
        row.put(APPLICATION_CODE_RESPONDENT, item.requiresRespondent().getValue());
        row.put(DESTINATION_EMAIL_ADDRESS_1, null);
        row.put(DESTINATION_EMAIL_ADDRESS_2, null);
        row.put(APPLICATION_CODE_START_DATE, item.startDate());
        row.put(APPLICATION_CODE_END_DATE, item.endDate());
        row.put(BULK_RESPONDENT_ALLOWED, item.bulkRespondentAllowed().getValue());
        row.put(VERSION, item.version());
        row.put(CHANGED_BY, 0L);
        row.put(CHANGED_DATE, null); // ...updated upon handling.
        row.put(USER_NAME, TECHNICAL_USERNAME);
        row.put(AC_FEE_REFERENCE, item.feeReference());
        return row;
    }

    @Override
    public ApplicationCodeIngressRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ApplicationCodeIngressRecord(
                rs.getLong(AC_ID),
                rs.getString(APPLICATION_CODE),
                rs.getString(APPLICATION_CODE_TITLE),
                rs.getString(APPLICATION_CODE_WORDING),
                rs.getString(APPLICATION_LEGISLATION),
                YesOrNo.fromValue(rs.getString(FEE_DUE)),
                YesOrNo.fromValue(rs.getString(APPLICATION_CODE_RESPONDENT)),
                rs.getObject(APPLICATION_CODE_START_DATE, LocalDate.class),
                rs.getObject(APPLICATION_CODE_END_DATE, LocalDate.class),
                YesOrNo.fromValue(rs.getString(BULK_RESPONDENT_ALLOWED)),
                rs.getLong(VERSION),
                rs.getString(AC_FEE_REFERENCE));
    }
}
