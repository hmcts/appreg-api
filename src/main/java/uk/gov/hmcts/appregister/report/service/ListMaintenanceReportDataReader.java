package uk.gov.hmcts.appregister.report.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.gov.hmcts.appregister.generated.model.LegacyReportLocation;
import uk.gov.hmcts.appregister.generated.model.ListMaintenanceFilterDto;
import uk.gov.hmcts.appregister.report.model.ListMaintenanceReportRow;

class ListMaintenanceReportDataReader
        extends AbstractReportDataReader<
                ListMaintenanceReportRow,
                ListMaintenanceReportDataReader.ListMaintenanceReportReadCursor> {
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

    private static final RowMapper<ListMaintenanceReportRow> ROW_MAPPER =
            new ListMaintenanceReportRowMapper();

    private final ListMaintenanceFilterDto filter;

    ListMaintenanceReportDataReader(
            NamedParameterJdbcTemplate jdbcTemplate,
            ListMaintenanceFilterDto filter,
            String schema,
            int maxRows) {
        super(jdbcTemplate, schema, maxRows);
        this.filter = filter;
    }

    ListMaintenanceFilterDto filter() {
        return filter;
    }

    @Override
    protected ListMaintenanceReportReadCursor createCursor(int pageSize) {
        return new ListMaintenanceReportReadCursor(pageSize);
    }

    @Override
    protected List<ListMaintenanceReportRow> readPage(
            ListMaintenanceReportReadCursor cursor, int pageLimit) {
        MapSqlParameterSource parameters =
                new MapSqlParameterSource()
                        .addValue("dateFrom", filter.getDateFrom(), Types.DATE)
                        .addValue("dateTo", filter.getDateTo(), Types.DATE)
                        .addValue("listDescription", filter.getListDescription(), Types.VARCHAR)
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

    static class ListMaintenanceReportReadCursor
            extends ReportReadCursor<ListMaintenanceReportRow> {
        ListMaintenanceReportReadCursor(int pageSize) {
            super(pageSize);
        }

        LocalDate lastListDate() {
            return hasLastRow() ? lastRow().getListDate() : null;
        }

        Long lastApplicationListId() {
            return hasLastRow() ? lastRow().getApplicationListId() : null;
        }
    }

    static class ListMaintenanceReportRowMapper implements RowMapper<ListMaintenanceReportRow> {
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
