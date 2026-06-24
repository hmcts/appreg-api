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
import uk.gov.hmcts.appregister.generated.model.SearchWarrantsReportFilterDto;
import uk.gov.hmcts.appregister.report.model.SearchWarrantsReportRow;

class SearchWarrantsReportDataReader implements DataReader<SearchWarrantsReportRow> {
    private static final String REPORT_QUERY =
            """
            WITH candidate_apps AS (
                SELECT
                    ale.ale_id,
                    al.application_list_date AS list_date,
                    al.courthouse_code,
                    CASE
                        WHEN al.courthouse_code IS NOT NULL
                        THEN al.courthouse_code || ' - ' || al.courthouse_name
                        ELSE NULL
                    END AS courthouse_name,
                    al.other_courthouse,
                    cja.cja_code,
                    NULL AS standard_applicant_code,
                    COALESCE(
                        NULLIF(TRIM(na.name), ''),
                        NULLIF(TRIM(COALESCE(na.first_name, '') || ' ' ||COALESCE(na.last_name, '')), '')
                    ) AS applicant_full_name,
                    ac.application_code,
                    REPLACE(REPLACE(ale.application_list_entry_wording,'{',''),'}','') AS application_list_entry_wording
                FROM application_lists al
                JOIN application_list_entries ale
                    ON ale.al_al_id = al.al_id
                JOIN application_codes ac
                    ON ale.ac_ac_id = ac.ac_id
                JOIN name_address na
                    ON ale.a_na_id = na.na_id
                LEFT JOIN criminal_justice_area cja
                    ON al.cja_cja_id = cja.cja_id
                WHERE UPPER(ac.application_code) LIKE 'SW%'
                    AND al.application_list_date >= :dateFrom
                    AND al.application_list_date < (:dateTo + INTERVAL '1 day')
                    AND (al.is_deleted IS NULL OR al.is_deleted <> 'Y')
                    AND (ale.is_deleted IS NULL OR ale.is_deleted <> 'Y')

                UNION ALL

                SELECT
                    ale.ale_id,
                    al.application_list_date AS list_date,
                    al.courthouse_code,
                    CASE
                        WHEN al.courthouse_code IS NOT NULL
                        THEN al.courthouse_code || ' - ' || al.courthouse_name
                        ELSE NULL
                    END AS courthouse_name,
                    al.other_courthouse,
                    cja.cja_code,
                    sa.standard_applicant_code,
                    COALESCE(
                        NULLIF(TRIM(sa.name), ''),
                        NULLIF(TRIM(COALESCE(sa.forename_1, '') || ' ' ||COALESCE(sa.surname, '')), '')
                    ) AS applicant_full_name,
                    ac.application_code,
                    REPLACE(REPLACE(ale.application_list_entry_wording,'{',''),'}','') AS application_list_entry_wording
                FROM application_lists al
                JOIN application_list_entries ale
                    ON ale.al_al_id = al.al_id
                JOIN application_codes ac
                    ON ale.ac_ac_id = ac.ac_id
                JOIN standard_applicants sa
                    ON ale.sa_sa_id = sa.sa_id
                LEFT JOIN criminal_justice_area cja
                    ON al.cja_cja_id = cja.cja_id
                WHERE UPPER(ac.application_code) LIKE 'SW%'
                    AND al.application_list_date >= :dateFrom
                    AND al.application_list_date < (:dateTo + INTERVAL '1 day')
                    AND (al.is_deleted IS NULL OR al.is_deleted <> 'Y')
                    AND (ale.is_deleted IS NULL OR ale.is_deleted <> 'Y')
            )
            SELECT
                c.ale_id,
                c.list_date,
                c.courthouse_name,
                c.other_courthouse,
                c.cja_code,
                c.standard_applicant_code,
                c.applicant_full_name,
                c.application_code,
                c.application_list_entry_wording
            FROM candidate_apps c
            WHERE (
                    {{LEGACY_LOCATION_PREDICATE}}
                )
                AND (
                    :hasCursor IS FALSE
                    OR c.list_date < :lastListDate
                    OR (c.list_date = :lastListDate
                        AND c.ale_id < :lastApplicationListEntryId)
                )
            ORDER BY c.list_date DESC, c.ale_id DESC
            LIMIT :limit
            """
                    .replace(
                            "{{LEGACY_LOCATION_PREDICATE}}",
                            LegacyMisReportLocationSql.predicate(
                                    "c.courthouse_code",
                                    "c.other_courthouse",
                                    "c.cja_code",
                                    "otherCourthouse"));

    private static final RowMapper<SearchWarrantsReportRow> ROW_MAPPER =
            new SearchWarrantsReportRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SearchWarrantsReportFilterDto filter;
    private final String schema;

    SearchWarrantsReportDataReader(
            NamedParameterJdbcTemplate jdbcTemplate,
            SearchWarrantsReportFilterDto filter,
            String schema) {
        this.jdbcTemplate = jdbcTemplate;
        this.filter = filter;
        this.schema = schema;
    }

    SearchWarrantsReportFilterDto filter() {
        return filter;
    }

    @Override
    public void readData(
            ReadPagePosition position,
            PageReader<SearchWarrantsReportRow> pageReader,
            JobContext jobContext)
            throws IOException {
        jdbcTemplate
                .getJdbcTemplate()
                .execute("SET LOCAL search_path TO \"" + schema + "\""); // NOSONAR
        // S2077: schema is trusted Spring config; report filter values are bound query parameters.

        SearchWarrantsReportReadCursor cursor =
                new SearchWarrantsReportReadCursor(position.getPageSize());
        List<SearchWarrantsReportRow> rows = readPage(cursor);
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

    private List<SearchWarrantsReportRow> readPage(SearchWarrantsReportReadCursor cursor) {
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
                                "lastApplicationListEntryId",
                                cursor.lastApplicationListEntryId(),
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

    private static class SearchWarrantsReportReadCursor {
        private final int pageSize;
        private SearchWarrantsReportRow lastRow;

        SearchWarrantsReportReadCursor(int pageSize) {
            this.pageSize = pageSize;
        }

        void advance(List<SearchWarrantsReportRow> rows) {
            lastRow = rows.getLast();
        }

        boolean hasLastRow() {
            return lastRow != null;
        }

        LocalDate lastListDate() {
            return hasLastRow() ? lastRow.getListDate() : null;
        }

        Long lastApplicationListEntryId() {
            return hasLastRow() ? lastRow.getApplicationListEntryId() : null;
        }

        int pageSize() {
            return pageSize;
        }
    }

    private static class SearchWarrantsReportRowMapper
            implements RowMapper<SearchWarrantsReportRow> {
        @Override
        public SearchWarrantsReportRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return SearchWarrantsReportRow.builder()
                    .applicationListEntryId(rs.getLong("ale_id"))
                    .listDate(rs.getObject("list_date", LocalDate.class))
                    .courthouseName(rs.getString("courthouse_name"))
                    .otherCourthouse(rs.getString("other_courthouse"))
                    .cjaCode(rs.getString("cja_code"))
                    .standardApplicantCode(rs.getString("standard_applicant_code"))
                    .applicantFullName(rs.getString("applicant_full_name"))
                    .applicationCode(rs.getString("application_code"))
                    .applicationCodeWording(rs.getString("application_list_entry_wording"))
                    .build();
        }
    }
}
