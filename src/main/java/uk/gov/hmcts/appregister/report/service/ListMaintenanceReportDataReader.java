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
import uk.gov.hmcts.appregister.generated.model.LegacyReportLocation;
import uk.gov.hmcts.appregister.generated.model.ListMaintenanceFilterDto;
import uk.gov.hmcts.appregister.report.model.ListMaintenanceReportRow;

class ListMaintenanceReportDataReader implements DataReader<ListMaintenanceReportRow> {
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
                al.application_list_status,
                (
                    SELECT COUNT(*)
                    FROM application_list_entries ale
                    WHERE ale.al_al_id = al.al_id
                        AND (ale.is_deleted IS NULL OR ale.is_deleted <> 'Y')
                ) AS application_entry_count
            FROM application_lists al
            LEFT JOIN criminal_justice_area cja
                ON al.cja_cja_id = cja.cja_id
            WHERE al.application_list_status = 'OPEN'
                AND al.application_list_date >= :dateFrom
                AND al.application_list_date < (:dateTo + INTERVAL '1 day')
                AND (al.is_deleted IS NULL OR al.is_deleted <> 'Y')
                AND (
                    :listDescription IS NULL
                    OR UPPER(al.list_description) LIKE '%' || UPPER(:listDescription) || '%'
                )
                -- Maintains legacy MIS List Maintenance report AR5-7 location semantics.
                AND (
                    (
                        :cjaCode IS NOT NULL
                        AND UPPER(cja.cja_code) = UPPER(:cjaCode)
                        AND UPPER(al.other_courthouse)
                            LIKE '%' || UPPER(:otherCourthouse) || '%'
                        AND :courthouseCode IS NULL
                    )
                    OR (
                        :cjaCode IS NULL
                        AND (
                            UPPER(al.other_courthouse)
                                LIKE '%' || UPPER(:otherCourthouse) || '%'
                            OR :otherCourthouse IS NULL
                        )
                        AND (
                            UPPER(al.courthouse_code)
                                LIKE '%' || UPPER(:courthouseCode) || '%'
                            OR :courthouseCode IS NULL
                        )
                    )
                    OR (
                        :cjaCode IS NOT NULL
                        AND (
                            UPPER(SUBSTRING(al.courthouse_code FROM 2 FOR 2)) = UPPER(:cjaCode)
                            OR UPPER(cja.cja_code) = UPPER(:cjaCode)
                        )
                        AND :otherCourthouse IS NULL
                        AND :courthouseCode IS NULL
                    )
                    OR (
                        :cjaCode IS NULL
                        AND :otherCourthouse IS NULL
                        AND :courthouseCode IS NULL
                    )
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
            """;

    private static final RowMapper<ListMaintenanceReportRow> ROW_MAPPER =
            new ListMaintenanceReportRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ListMaintenanceFilterDto filter;
    private final String schema;

    ListMaintenanceReportDataReader(
            NamedParameterJdbcTemplate jdbcTemplate,
            ListMaintenanceFilterDto filter,
            String schema) {
        this.jdbcTemplate = jdbcTemplate;
        this.filter = filter;
        this.schema = schema;
    }

    @Override
    public void readData(
            ReadPagePosition position,
            PageReader<ListMaintenanceReportRow> pageReader,
            JobContext jobContext)
            throws IOException {
        jdbcTemplate
                .getJdbcTemplate()
                .execute("SET LOCAL search_path TO \"" + schema + "\""); // NOSONAR
        // S2077: schema is trusted Spring config; report filter values are bound query parameters.

        ListMaintenanceReportReadCursor cursor =
                new ListMaintenanceReportReadCursor(position.getPageSize());
        List<ListMaintenanceReportRow> rows = readPage(cursor);

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

    private List<ListMaintenanceReportRow> readPage(ListMaintenanceReportReadCursor cursor) {
        MapSqlParameterSource parameters =
                new MapSqlParameterSource()
                        .addValue("dateFrom", filter.getDateFrom(), Types.DATE)
                        .addValue("dateTo", filter.getDateTo(), Types.DATE)
                        .addValue("listDescription", filter.getListDescription(), Types.VARCHAR)
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

    private static class ListMaintenanceReportReadCursor {
        private final int pageSize;
        private ListMaintenanceReportRow lastRow;

        ListMaintenanceReportReadCursor(int pageSize) {
            this.pageSize = pageSize;
        }

        void advance(List<ListMaintenanceReportRow> rows) {
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

    private static class ListMaintenanceReportRowMapper
            implements RowMapper<ListMaintenanceReportRow> {
        @Override
        public ListMaintenanceReportRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return ListMaintenanceReportRow.builder()
                    .applicationListId(rs.getLong("al_id"))
                    .listDate(rs.getObject("list_date", LocalDate.class))
                    .courthouseName(rs.getString("courthouse_name"))
                    .otherCourthouse(rs.getString("other_courthouse"))
                    .cjaCode(rs.getString("cja_code"))
                    .listDescription(rs.getString("list_description"))
                    .listStatus(rs.getString("application_list_status"))
                    .applicationEntryCount(rs.getLong("application_entry_count"))
                    .build();
        }
    }
}
