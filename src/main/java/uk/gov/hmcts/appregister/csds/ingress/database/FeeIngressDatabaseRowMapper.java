package uk.gov.hmcts.appregister.csds.ingress.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.csds.ingress.processor.fee.FeeIngressRecord;

@Component
public class FeeIngressDatabaseRowMapper
        implements IngressDatabaseRowMapper<FeeIngressRecord>, RowMapper<FeeIngressRecord> {
    private static final String TECHNICAL_USERNAME = "CSDS_INGRESS";
    private static final String OFFSITE_FEE_REFERENCE = "CO1.1";
    private static final String ID = "fee_id";
    private static final String REFERENCE = "fee_reference";
    private static final String DESCRIPTION = "fee_description";
    private static final String VALUE = "fee_value";
    private static final String START_DATE = "fee_start_date";
    private static final String END_DATE = "fee_end_date";
    private static final String VERSION = "fee_version";
    private static final String CHANGED_BY = "fee_changed_by";
    private static final String CHANGED_DATE = "fee_changed_date";
    private static final String USER_NAME = "fee_user_name";
    private static final String IS_OFFSITE = "is_offsite";
    private static final List<String> COLUMNS =
            List.of(
                    ID,
                    REFERENCE,
                    DESCRIPTION,
                    VALUE,
                    START_DATE,
                    END_DATE,
                    VERSION,
                    CHANGED_BY,
                    CHANGED_DATE,
                    USER_NAME,
                    IS_OFFSITE);
    private static final List<String> UPDATABLE_COLUMNS =
            List.of(
                    REFERENCE,
                    DESCRIPTION,
                    VALUE,
                    START_DATE,
                    END_DATE,
                    VERSION,
                    CHANGED_BY,
                    CHANGED_DATE,
                    USER_NAME,
                    IS_OFFSITE);

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
    public Map<String, Object> toRow(FeeIngressRecord item) {
        var row = new LinkedHashMap<String, Object>();
        row.put(ID, item.id());
        row.put(REFERENCE, item.reference());
        row.put(DESCRIPTION, item.description());
        row.put(VALUE, item.amount());
        row.put(START_DATE, item.startDate());
        row.put(END_DATE, item.endDate());
        row.put(VERSION, item.version());
        row.put(CHANGED_BY, 0L);
        row.put(CHANGED_DATE, null); // ...updated upon handling.
        row.put(USER_NAME, TECHNICAL_USERNAME);
        row.put(IS_OFFSITE, OFFSITE_FEE_REFERENCE.equals(item.reference()));
        return row;
    }

    @Override
    public FeeIngressRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new FeeIngressRecord(
                rs.getLong(ID),
                rs.getString(REFERENCE),
                rs.getString(DESCRIPTION),
                rs.getBigDecimal(VALUE),
                rs.getObject(START_DATE, LocalDate.class),
                rs.getObject(END_DATE, LocalDate.class),
                rs.getLong(VERSION));
    }
}
