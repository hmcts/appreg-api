package uk.gov.hmcts.appregister.csds.ingress.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
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
    private static final String ID = "rc_id";
    private static final String CODE = "resolution_code";
    private static final String TITLE = "resolution_code_title";
    private static final String WORDING = "resolution_code_wording";
    private static final String LEGISLATION = "resolution_legislation";
    private static final String EMAIL_1 = "rc_destination_email_address_1";
    private static final String EMAIL_2 = "rc_destination_email_address_2";
    private static final String START_DATE = "resolution_code_start_date";
    private static final String END_DATE = "resolution_code_end_date";
    private static final String VERSION = "version";
    private static final String CHANGED_BY = "changed_by";
    private static final String CHANGED_DATE = "changed_date";
    private static final String USER_NAME = "user_name";
    private static final List<String> COLUMNS =
            List.of(
                    ID,
                    CODE,
                    TITLE,
                    WORDING,
                    LEGISLATION,
                    EMAIL_1,
                    EMAIL_2,
                    START_DATE,
                    END_DATE,
                    VERSION,
                    CHANGED_BY,
                    CHANGED_DATE,
                    USER_NAME);
    private static final List<String> UPDATABLE_COLUMNS =
            List.of(
                    CODE,
                    TITLE,
                    WORDING,
                    LEGISLATION,
                    EMAIL_1,
                    EMAIL_2,
                    START_DATE,
                    END_DATE,
                    VERSION,
                    CHANGED_BY,
                    CHANGED_DATE,
                    USER_NAME);

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
    public Map<String, Object> toRow(ResolutionCodeIngressRecord item) {
        var row = new LinkedHashMap<String, Object>();
        row.put(ID, item.id());
        row.put(CODE, item.code());
        row.put(TITLE, item.title());
        row.put(WORDING, item.wording());
        row.put(LEGISLATION, item.legislation());
        row.put(EMAIL_1, item.recipient1Email());
        row.put(EMAIL_2, item.recipient2Email());
        row.put(START_DATE, item.startDate());
        row.put(END_DATE, item.endDate());
        row.put(VERSION, item.version());
        row.put(CHANGED_BY, 0L);
        row.put(CHANGED_DATE, null); // ...updated upon handling.
        row.put(USER_NAME, TECHNICAL_USERNAME);
        return row;
    }

    @Override
    public ResolutionCodeIngressRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ResolutionCodeIngressRecord(
                rs.getLong(ID),
                rs.getString(CODE),
                rs.getString(TITLE),
                rs.getString(WORDING),
                rs.getString(LEGISLATION),
                rs.getString(EMAIL_1),
                rs.getString(EMAIL_2),
                rs.getObject(START_DATE, LocalDate.class),
                rs.getObject(END_DATE, LocalDate.class),
                rs.getLong(VERSION));
    }
}
