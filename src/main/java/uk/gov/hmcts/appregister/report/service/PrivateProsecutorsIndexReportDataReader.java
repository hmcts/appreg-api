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
import uk.gov.hmcts.appregister.generated.model.PrivateProsecutorsIndexFilterDto;
import uk.gov.hmcts.appregister.report.model.PrivateProsecutorsIndexReportRow;

class PrivateProsecutorsIndexReportDataReader
        implements DataReader<PrivateProsecutorsIndexReportRow> {
    private static final String REPORT_QUERY =
            """
            WITH standard_applicant_names AS (
                SELECT
                    sa.sa_id,
                    COALESCE(
                        NULLIF(TRIM(sa.name), ''),
                        NULLIF(
                            TRIM(
                                COALESCE(sa.forename_1, '')
                                || ' '
                                || COALESCE(sa.surname, '')
                            ),
                            ''
                        ),
                        sa.standard_applicant_code
                    ) AS standard_applicant_name
                FROM standard_applicants sa
            ),
            candidate_apps AS (
                SELECT
                    ale.ale_id,
                    al.application_list_date,
                    CASE
                        WHEN al.courthouse_code IS NOT NULL
                        THEN al.courthouse_code || ' - ' || al.courthouse_name
                        ELSE NULL
                    END AS courthouse_name,
                    al.other_courthouse,
                    al.courthouse_code,
                    cja.cja_code,
                    COALESCE(app_na.last_name, app_na.name) AS applicant_name_or_surname,
                    app_na.first_name AS applicant_first_name,
                    NULL AS standard_applicant_name,
                    resp_na.first_name AS respondent_first_name,
                    resp_na.last_name AS respondent_surname,
                    resp_na.name AS respondent_organisation_name,
                    ale.application_list_entry_wording,
                    ale.notes
                FROM application_lists al
                JOIN application_list_entries ale
                    ON ale.al_al_id = al.al_id
                JOIN application_codes ac
                    ON ale.ac_ac_id = ac.ac_id
                JOIN name_address app_na
                    ON ale.a_na_id = app_na.na_id
                LEFT JOIN name_address resp_na
                    ON ale.r_na_id = resp_na.na_id
                LEFT JOIN criminal_justice_area cja
                    ON al.cja_cja_id = cja.cja_id
                WHERE al.application_list_status = 'CLOSED'
                    AND ac.application_code = 'MX99010'
                    AND al.application_list_date >= :dateFrom
                    AND al.application_list_date < (:dateTo + INTERVAL '1 day')
                    AND (al.is_deleted IS NULL OR al.is_deleted <> 'Y')
                    AND (ale.is_deleted IS NULL OR ale.is_deleted <> 'Y')
                    AND ale.sa_sa_id IS NULL
                    AND :standardApplicantName IS NULL
                    AND (
                        :applicantFirstName IS NULL
                        OR UPPER(app_na.first_name)
                            LIKE '%' || UPPER(:applicantFirstName) || '%'
                    )
                    AND (
                        :applicantSurname IS NULL
                        OR UPPER(app_na.last_name) LIKE '%' || UPPER(:applicantSurname) || '%'
                        OR UPPER(app_na.name) LIKE '%' || UPPER(:applicantSurname) || '%'
                    )
                    AND (
                        :applicantOrganisationName IS NULL
                        OR UPPER(app_na.name) LIKE '%' || UPPER(:applicantOrganisationName) || '%'
                    )
                    AND (
                        :respondentFirstName IS NULL
                        OR UPPER(resp_na.first_name)
                            LIKE '%' || UPPER(:respondentFirstName) || '%'
                    )
                    AND (
                        :respondentSurname IS NULL
                        OR UPPER(resp_na.last_name) LIKE '%' || UPPER(:respondentSurname) || '%'
                    )
                    AND (
                        :respondentOrganisationName IS NULL
                        OR UPPER(resp_na.name)
                            LIKE '%' || UPPER(:respondentOrganisationName) || '%'
                    )
                    AND (
                        {{LEGACY_LOCATION_PREDICATE}}
                    )
                UNION ALL
                SELECT
                    ale.ale_id,
                    al.application_list_date,
                    CASE
                        WHEN al.courthouse_code IS NOT NULL
                        THEN al.courthouse_code || ' - ' || al.courthouse_name
                        ELSE NULL
                    END AS courthouse_name,
                    al.other_courthouse,
                    al.courthouse_code,
                    cja.cja_code,
                    NULL AS applicant_name_or_surname,
                    NULL AS applicant_first_name,
                    sa.standard_applicant_name,
                    resp_na.first_name AS respondent_first_name,
                    resp_na.last_name AS respondent_surname,
                    resp_na.name AS respondent_organisation_name,
                    ale.application_list_entry_wording,
                    ale.notes
                FROM application_lists al
                JOIN application_list_entries ale
                    ON ale.al_al_id = al.al_id
                JOIN application_codes ac
                    ON ale.ac_ac_id = ac.ac_id
                JOIN standard_applicant_names sa
                    ON ale.sa_sa_id = sa.sa_id
                LEFT JOIN name_address resp_na
                    ON ale.r_na_id = resp_na.na_id
                LEFT JOIN criminal_justice_area cja
                    ON al.cja_cja_id = cja.cja_id
                WHERE al.application_list_status = 'CLOSED'
                    AND ac.application_code = 'MX99010'
                    AND al.application_list_date >= :dateFrom
                    AND al.application_list_date < (:dateTo + INTERVAL '1 day')
                    AND (al.is_deleted IS NULL OR al.is_deleted <> 'Y')
                    AND (ale.is_deleted IS NULL OR ale.is_deleted <> 'Y')
                    AND :applicantFirstName IS NULL
                    AND :applicantSurname IS NULL
                    AND :applicantOrganisationName IS NULL
                    AND (
                        :standardApplicantName IS NULL
                        OR UPPER(sa.standard_applicant_name)
                            LIKE '%' || UPPER(:standardApplicantName) || '%'
                    )
                    AND (
                        :respondentFirstName IS NULL
                        OR UPPER(resp_na.first_name)
                            LIKE '%' || UPPER(:respondentFirstName) || '%'
                    )
                    AND (
                        :respondentSurname IS NULL
                        OR UPPER(resp_na.last_name) LIKE '%' || UPPER(:respondentSurname) || '%'
                    )
                    AND (
                        :respondentOrganisationName IS NULL
                        OR UPPER(resp_na.name)
                            LIKE '%' || UPPER(:respondentOrganisationName) || '%'
                    )
                    AND (
                        {{LEGACY_LOCATION_PREDICATE}}
                    )
            ),
            filtered_apps AS (
                SELECT *
                FROM candidate_apps ca
                WHERE :hasCursor IS FALSE
                    OR ca.application_list_date < :lastListDate
                    OR (
                        ca.application_list_date = :lastListDate
                        AND ca.ale_id < :lastApplicationListEntryId
                    )
                ORDER BY ca.application_list_date DESC, ca.ale_id DESC
                LIMIT :limit
            ),
            ordered_results AS (
                SELECT
                    aler.ale_ale_id,
                    rc.resolution_code,
                    ROW_NUMBER() OVER (
                        PARTITION BY aler.ale_ale_id
                        ORDER BY rc.resolution_code DESC
                    ) AS result_order
                FROM app_list_entry_resolutions aler
                JOIN resolution_codes rc
                    ON rc.rc_id = aler.rc_rc_id
                JOIN filtered_apps fa
                    ON fa.ale_id = aler.ale_ale_id
            ),
            result_pivot AS (
                SELECT
                    ale_ale_id,
                    MAX(CASE WHEN result_order = 1 THEN resolution_code END) AS result1,
                    MAX(CASE WHEN result_order = 2 THEN resolution_code END) AS result2,
                    MAX(CASE WHEN result_order = 3 THEN resolution_code END) AS result3,
                    MAX(CASE WHEN result_order = 4 THEN resolution_code END) AS result4
                FROM ordered_results
                GROUP BY ale_ale_id
            )
            SELECT
                fa.ale_id,
                fa.application_list_date AS list_date,
                fa.courthouse_name,
                fa.other_courthouse,
                fa.cja_code,
                fa.applicant_name_or_surname,
                fa.applicant_first_name,
                fa.standard_applicant_name,
                fa.respondent_first_name,
                fa.respondent_surname,
                fa.respondent_organisation_name,
                REPLACE(REPLACE(fa.application_list_entry_wording, '{', ''), '}', '')
                    AS application_wording,
                rp.result1,
                rp.result2,
                rp.result3,
                rp.result4,
                fa.notes
            FROM filtered_apps fa
            LEFT JOIN result_pivot rp
                ON rp.ale_ale_id = fa.ale_id
            ORDER BY fa.application_list_date DESC, fa.ale_id DESC
            """
                    .replace(
                            "{{LEGACY_LOCATION_PREDICATE}}",
                            LegacyMisReportLocationSql.predicate(
                                    "al.courthouse_code",
                                    "al.other_courthouse",
                                    "cja.cja_code",
                                    "otherCourthouse"));

    private static final RowMapper<PrivateProsecutorsIndexReportRow> ROW_MAPPER =
            new PrivateProsecutorsIndexReportRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PrivateProsecutorsIndexFilterDto filter;
    private final String schema;

    PrivateProsecutorsIndexFilterDto filter() {
        return filter;
    }

    PrivateProsecutorsIndexReportDataReader(
            NamedParameterJdbcTemplate jdbcTemplate,
            PrivateProsecutorsIndexFilterDto filter,
            String schema) {
        this.jdbcTemplate = jdbcTemplate;
        this.filter = filter;
        this.schema = schema;
    }

    @Override
    public void readData(
            ReadPagePosition position,
            PageReader<PrivateProsecutorsIndexReportRow> pageReader,
            JobContext jobContext)
            throws IOException {
        jdbcTemplate
                .getJdbcTemplate()
                .execute("SET LOCAL search_path TO \"" + schema + "\""); // NOSONAR
        // S2077: schema is trusted Spring config; report filter values are bound query parameters.

        PrivateProsecutorsIndexReportReadCursor cursor =
                new PrivateProsecutorsIndexReportReadCursor(position.getPageSize());
        List<PrivateProsecutorsIndexReportRow> rows = readPage(cursor);

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

    private List<PrivateProsecutorsIndexReportRow> readPage(
            PrivateProsecutorsIndexReportReadCursor cursor) {
        MapSqlParameterSource parameters =
                new MapSqlParameterSource()
                        .addValue("dateFrom", filter.getDateFrom(), Types.DATE)
                        .addValue("dateTo", filter.getDateTo(), Types.DATE)
                        .addValue("applicantSurname", filter.getApplicantSurname(), Types.VARCHAR)
                        .addValue(
                                "applicantFirstName", filter.getApplicantFirstName(), Types.VARCHAR)
                        .addValue(
                                "applicantOrganisationName",
                                filter.getApplicantOrganisationName(),
                                Types.VARCHAR)
                        .addValue(
                                "standardApplicantName",
                                filter.getStandardApplicantName(),
                                Types.VARCHAR)
                        .addValue("respondentSurname", filter.getRespondentSurname(), Types.VARCHAR)
                        .addValue(
                                "respondentFirstName",
                                filter.getRespondentFirstName(),
                                Types.VARCHAR)
                        .addValue(
                                "respondentOrganisationName",
                                filter.getRespondentOrganisationName(),
                                Types.VARCHAR)
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

    private static class PrivateProsecutorsIndexReportReadCursor {
        private final int pageSize;
        private PrivateProsecutorsIndexReportRow lastRow;

        PrivateProsecutorsIndexReportReadCursor(int pageSize) {
            this.pageSize = pageSize;
        }

        void advance(List<PrivateProsecutorsIndexReportRow> rows) {
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

    private static class PrivateProsecutorsIndexReportRowMapper
            implements RowMapper<PrivateProsecutorsIndexReportRow> {
        @Override
        public PrivateProsecutorsIndexReportRow mapRow(ResultSet rs, int rowNum)
                throws SQLException {
            return PrivateProsecutorsIndexReportRow.builder()
                    .applicationListEntryId(rs.getLong("ale_id"))
                    .listDate(rs.getObject("list_date", LocalDate.class))
                    .courthouseName(rs.getString("courthouse_name"))
                    .otherCourthouse(rs.getString("other_courthouse"))
                    .cjaCode(rs.getString("cja_code"))
                    .applicantNameOrSurname(rs.getString("applicant_name_or_surname"))
                    .applicantFirstName(rs.getString("applicant_first_name"))
                    .standardApplicantName(rs.getString("standard_applicant_name"))
                    .respondentFirstName(rs.getString("respondent_first_name"))
                    .respondentSurname(rs.getString("respondent_surname"))
                    .respondentOrganisationName(rs.getString("respondent_organisation_name"))
                    .applicationWording(rs.getString("application_wording"))
                    .result1(rs.getString("result1"))
                    .result2(rs.getString("result2"))
                    .result3(rs.getString("result3"))
                    .result4(rs.getString("result4"))
                    .notes(rs.getString("notes"))
                    .build();
        }
    }
}
