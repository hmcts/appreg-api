package uk.gov.hmcts.appregister.csds.ingress.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.appregister.csds.ingress.processor.nationalcourthouse.NationalCourtHouseIngressRecord;

@Component
public class NationalCourtHouseIngressDatabaseRowMapper
        implements IngressDatabaseRowMapper<NationalCourtHouseIngressRecord>,
                RowMapper<NationalCourtHouseIngressRecord> {
    private static final String ID = "nch_id";
    private static final String NAME = "courthouse_name";
    private static final String VERSION = "version_number";
    private static final String CHANGED_BY = "changed_by";
    private static final String CHANGED_DATE = "changed_date";
    private static final String COURT_TYPE = "court_type";
    private static final String START_DATE = "start_date";
    private static final String END_DATE = "end_date";
    private static final String LOCATION_ID = "loc_loc_id";
    private static final String PETTY_SESSIONAL_AREA_ID = "psa_psa_id";
    private static final String COURT_LOCATION_CODE = "court_location_code";
    private static final String WELSH_NAME = "sl_courthouse_name";
    private static final String ORGANISATION_ID = "norg_id";
    private static final List<String> COLUMNS =
            List.of(
                    ID,
                    NAME,
                    VERSION,
                    CHANGED_BY,
                    CHANGED_DATE,
                    COURT_TYPE,
                    START_DATE,
                    END_DATE,
                    LOCATION_ID,
                    PETTY_SESSIONAL_AREA_ID,
                    COURT_LOCATION_CODE,
                    WELSH_NAME,
                    ORGANISATION_ID);
    private static final Map<String, String> TIMESTAMP_EXPRESSION =
            Map.of(CHANGED_DATE, "current_timestamp");

    @Override
    public List<String> columns() {
        return COLUMNS;
    }

    @Override
    public Map<String, String> insertExpressions() {
        return TIMESTAMP_EXPRESSION;
    }

    @Override
    public Map<String, Object> toRow(NationalCourtHouseIngressRecord item) {
        var row = new LinkedHashMap<String, Object>();
        row.put(ID, item.id());
        row.put(NAME, item.name());
        row.put(VERSION, item.version());
        row.put(CHANGED_BY, 0L);
        row.put(CHANGED_DATE, null);
        row.put(COURT_TYPE, "CHOA");
        row.put(START_DATE, item.startDate());
        row.put(END_DATE, item.endDate());
        // CSDS relationship identifiers are intentionally left unmapped until their legacy
        // equivalents are confirmed.
        row.put(LOCATION_ID, null);
        row.put(PETTY_SESSIONAL_AREA_ID, null);
        row.put(COURT_LOCATION_CODE, item.courtLocationCode());
        row.put(WELSH_NAME, item.welshName());
        row.put(ORGANISATION_ID, null);
        return row;
    }

    @Override
    public NationalCourtHouseIngressRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new NationalCourtHouseIngressRecord(
                rs.getLong(ID),
                rs.getString(NAME),
                rs.getLong(VERSION),
                rs.getObject(START_DATE, LocalDate.class),
                rs.getObject(END_DATE, LocalDate.class),
                rs.getString(COURT_LOCATION_CODE),
                rs.getString(WELSH_NAME));
    }
}
