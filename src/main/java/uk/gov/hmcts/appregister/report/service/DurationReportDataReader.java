package uk.gov.hmcts.appregister.report.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.LegacyReportLocation;
import uk.gov.hmcts.appregister.report.model.DurationReportRow;

class DurationReportDataReader
        extends AbstractReportDataReader<
                DurationReportRow, DurationReportDataReader.DurationReportReadCursor> {
    private static final String REPORT_QUERY =
            """
            SELECT
                al.al_id,
                al.application_list_date AS list_date,
                CASE
                    WHEN al.courthouse_code IS NOT NULL
                    THEN al.courthouse_code || ' - ' || al.courthouse_name
                    ELSE NULL
                END AS courthouse_name,
                al.other_courthouse,
                cja.cja_code,
                al.list_description,
                al.duration_hour,
                al.duration_minute
            FROM application_lists al
            LEFT JOIN criminal_justice_area cja
                ON al.cja_cja_id = cja.cja_id
            WHERE al.application_list_status = 'CLOSED'
                AND al.application_list_date >= :dateFrom
                AND al.application_list_date < (:dateTo + INTERVAL '1 day')
                AND (al.is_deleted IS NULL OR al.is_deleted <> 'Y')
                -- Maintains legacy MIS Duration report AR5-7 location semantics.
                AND (
                    {{LEGACY_LOCATION_PREDICATE}}
                )
                AND (
                    :hasCursor IS FALSE
                    OR (al.application_list_date, al.al_id) < (:lastListDate, :lastApplicationListId)
                )
            ORDER BY al.application_list_date DESC, al.al_id DESC
            LIMIT :limit
            """
                    .replace(
                            "{{LEGACY_LOCATION_PREDICATE}}",
                            LegacyMisReportLocationSql.predicate(
                                    "al.courthouse_code",
                                    "al.other_courthouse",
                                    "cja.cja_code",
                                    "otherCourthouse"));

    private static final RowMapper<DurationReportRow> ROW_MAPPER = new DurationReportRowMapper();

    private final DurationFilterDto filter;

    DurationReportDataReader(
            NamedParameterJdbcTemplate jdbcTemplate,
            DurationFilterDto filter,
            String schema,
            int maxRows) {
        super(jdbcTemplate, schema, maxRows);
        this.filter = filter;
    }

    DurationFilterDto filter() {
        return filter;
    }

    @Override
    protected DurationReportReadCursor createCursor(int pageSize) {
        return new DurationReportReadCursor(pageSize);
    }

    @Override
    protected List<DurationReportRow> readPage(DurationReportReadCursor cursor, int pageLimit) {
        MapSqlParameterSource parameters =
                new MapSqlParameterSource()
                        .addValue("dateFrom", filter.getDateFrom(), Types.DATE)
                        .addValue("dateTo", filter.getDateTo(), Types.DATE)
                        .addValue(
                                "cjaCode",
                                legacyLocationValue(
                                        filter.getLocation(), LegacyReportLocation::getCjaCode),
                                Types.VARCHAR)
                        .addValue(
                                "otherCourthouse",
                                legacyLocationValue(
                                        filter.getLocation(),
                                        LegacyReportLocation::getOtherLocationDescription),
                                Types.VARCHAR)
                        .addValue(
                                "courthouseCode",
                                legacyLocationValue(
                                        filter.getLocation(),
                                        LegacyReportLocation::getCourtLocationCode),
                                Types.VARCHAR)
                        .addValue("hasCursor", cursor.hasLastRow(), Types.BOOLEAN)
                        .addValue("lastListDate", cursor.lastListDate(), Types.DATE)
                        .addValue(
                                "lastApplicationListId",
                                cursor.lastApplicationListId(),
                                Types.BIGINT)
                        .addValue("limit", pageLimit, Types.INTEGER);

        return jdbcTemplate.query(REPORT_QUERY, parameters, ROW_MAPPER);
    }

    static class DurationReportReadCursor extends ReportReadCursor<DurationReportRow> {
        DurationReportReadCursor(int pageSize) {
            super(pageSize);
        }

        LocalDate lastListDate() {
            return hasLastRow() ? lastRow().getListDate() : null;
        }

        Long lastApplicationListId() {
            return hasLastRow() ? lastRow().getApplicationListId() : null;
        }
    }

    static class DurationReportRowMapper implements RowMapper<DurationReportRow> {
        @Override
        public DurationReportRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return DurationReportRow.builder()
                    .applicationListId(rs.getLong("al_id"))
                    .listDate(rs.getObject("list_date", LocalDate.class))
                    .courthouseName(rs.getString("courthouse_name"))
                    .otherCourthouse(rs.getString("other_courthouse"))
                    .cjaCode(rs.getString("cja_code"))
                    .listDescription(rs.getString("list_description"))
                    .durationHours(toInteger(rs.getObject("duration_hour")))
                    .durationMinutes(toInteger(rs.getObject("duration_minute")))
                    .build();
        }

        private Integer toInteger(Object value) {
            return value == null ? null : ((Number) value).intValue();
        }
    }
}
