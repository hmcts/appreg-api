package uk.gov.hmcts.appregister.report.service;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.reader.DataReader;
import uk.gov.hmcts.appregister.common.async.reader.PageReader;
import uk.gov.hmcts.appregister.common.async.reader.ReadPagePosition;
import uk.gov.hmcts.appregister.generated.model.DurationFilterDto;
import uk.gov.hmcts.appregister.generated.model.LegacyReportLocation;
import uk.gov.hmcts.appregister.report.model.DurationReportRow;

class DurationReportDataReader implements DataReader<DurationReportRow> {
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
                    OR al.application_list_date < :lastListDate
                    OR (
                        al.application_list_date = :lastListDate
                        AND al.al_id < :lastApplicationListId
                    )
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

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DurationFilterDto filter;
    private final String schema;

    DurationReportDataReader(
            NamedParameterJdbcTemplate jdbcTemplate, DurationFilterDto filter, String schema) {
        this.jdbcTemplate = jdbcTemplate;
        this.filter = filter;
        this.schema = schema;
    }

    @Override
    public void readData(
            ReadPagePosition position,
            PageReader<DurationReportRow> pageReader,
            JobContext jobContext)
            throws IOException {
        jdbcTemplate
                .getJdbcTemplate()
                .execute("SET LOCAL search_path TO \"" + schema + "\""); // NOSONAR
        // S2077: schema is trusted Spring config; report filter values are bound query parameters.

        DurationReportReadCursor cursor = new DurationReportReadCursor(position.getPageSize());
        List<DurationReportRow> rows = readPage(cursor);

        while (!rows.isEmpty()) {
            pageReader.readData(rows, jobContext);
            if (rows.size() < cursor.pageSize()) {
                return;
            }
            cursor.advance(rows);
            rows = readPage(cursor);
        }
    }

    @Override
    public void close() throws IOException {
        // No stream to close.
    }

    private List<DurationReportRow> readPage(DurationReportReadCursor cursor) {
        MapSqlParameterSource parameters =
                new MapSqlParameterSource()
                        .addValue("dateFrom", filter.getDateFrom(), Types.DATE)
                        .addValue("dateTo", filter.getDateTo(), Types.DATE)
                        .addValue(
                                "cjaCode",
                                getLocationValue(LegacyReportLocation::getCjaCode),
                                Types.VARCHAR)
                        .addValue(
                                "otherCourthouse",
                                getLocationValue(LegacyReportLocation::getOtherLocationDescription),
                                Types.VARCHAR)
                        .addValue(
                                "courthouseCode",
                                getLocationValue(LegacyReportLocation::getCourtLocationCode),
                                Types.VARCHAR)
                        .addValue("hasCursor", cursor.hasLastRow(), Types.BOOLEAN)
                        .addValue("lastListDate", cursor.lastListDate(), Types.DATE)
                        .addValue(
                                "lastApplicationListId",
                                cursor.lastApplicationListId(),
                                Types.BIGINT)
                        .addValue("limit", cursor.pageSize(), Types.INTEGER);

        return jdbcTemplate.query(REPORT_QUERY, parameters, ROW_MAPPER);
    }

    private String getLocationValue(
            java.util.function.Function<LegacyReportLocation, String> getter) {
        if (filter.getLocation() == null) {
            return null;
        }

        return getter.apply(filter.getLocation());
    }

    private static class DurationReportReadCursor {
        private final int pageSize;
        private DurationReportRow lastRow;

        DurationReportReadCursor(int pageSize) {
            this.pageSize = pageSize;
        }

        void advance(List<DurationReportRow> rows) {
            lastRow = rows.getLast();
        }

        boolean hasLastRow() {
            return lastRow != null;
        }

        LocalDate lastListDate() {
            return hasLastRow() ? lastRow.getListDate() : null;
        }

        Long lastApplicationListId() {
            return hasLastRow() ? lastRow.getApplicationListId() : null;
        }

        int pageSize() {
            return pageSize;
        }
    }

    private static class DurationReportRowMapper implements RowMapper<DurationReportRow> {
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
