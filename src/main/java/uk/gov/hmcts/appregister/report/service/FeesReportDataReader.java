package uk.gov.hmcts.appregister.report.service;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.reader.DataReader;
import uk.gov.hmcts.appregister.common.async.reader.PageReader;
import uk.gov.hmcts.appregister.common.async.reader.ReadPagePosition;
import uk.gov.hmcts.appregister.generated.model.FeesReportFilterDto;
import uk.gov.hmcts.appregister.generated.model.Location;
import uk.gov.hmcts.appregister.report.model.FeesReportRow;

class FeesReportDataReader implements DataReader<FeesReportRow> {
    private static final String REPORT_QUERY =
            """
            WITH candidate_apps AS (
                SELECT
                    al.application_list_date,
                    CASE
                        WHEN al.courthouse_code IS NOT NULL
                        THEN al.courthouse_code || ' - ' || al.courthouse_name
                        ELSE NULL
                    END AS courthouse_name,
                    al.other_courthouse,
                    al.courthouse_code,
                    cja.cja_code,
                    NULL AS standard_applicant_code,
                    na.name,
                    na.forename_1,
                    na.surname,
                    ac.application_code,
                    ac.application_code_title,
                    ale.ale_id
                FROM application_lists al
                JOIN application_list_entries ale
                    ON ale.al_al_id = al.al_id
                JOIN application_codes ac
                    ON ale.ac_ac_id = ac.ac_id
                JOIN name_address na
                    ON ale.a_na_id = na.na_id
                LEFT JOIN criminal_justice_area cja
                    ON al.cja_cja_id = cja.cja_id
                WHERE ac.fee_due = 'Y'
                    AND al.application_list_date >= :dateFrom
                    AND al.application_list_date < (:dateTo + INTERVAL '1 day')
                    AND (al.is_deleted IS NULL OR al.is_deleted <> 'Y')
                    AND (ale.is_deleted IS NULL OR ale.is_deleted <> 'Y')
                    AND :standardApplicantCode IS NULL
                GROUP BY
                    al.application_list_date,
                    courthouse_name,
                    al.other_courthouse,
                    al.courthouse_code,
                    cja.cja_code,
                    na.name,
                    na.forename_1,
                    na.surname,
                    ac.application_code,
                    ac.application_code_title,
                    ale.ale_id

                UNION ALL

                SELECT
                    al.application_list_date,
                    CASE
                        WHEN al.courthouse_code IS NOT NULL
                        THEN al.courthouse_code || ' - ' || al.courthouse_name
                        ELSE NULL
                    END AS courthouse_name,
                    al.other_courthouse,
                    al.courthouse_code,
                    cja.cja_code,
                    sa.standard_applicant_code,
                    sa.name,
                    sa.forename_1,
                    sa.surname,
                    ac.application_code,
                    ac.application_code_title,
                    ale.ale_id
                FROM application_lists al
                JOIN application_list_entries ale
                    ON ale.al_al_id = al.al_id
                JOIN application_codes ac
                    ON ale.ac_ac_id = ac.ac_id
                JOIN standard_applicants sa
                    ON ale.sa_sa_id = sa.sa_id
                LEFT JOIN criminal_justice_area cja
                    ON al.cja_cja_id = cja.cja_id
                WHERE ac.fee_due = 'Y'
                    AND al.application_list_date >= :dateFrom
                    AND al.application_list_date < (:dateTo + INTERVAL '1 day')
                    AND (al.is_deleted IS NULL OR al.is_deleted <> 'Y')
                    AND (ale.is_deleted IS NULL OR ale.is_deleted <> 'Y')
                    AND (
                        :standardApplicantCode IS NULL
                        OR UPPER(sa.standard_applicant_code) = UPPER(:standardApplicantCode)
                    )
                GROUP BY
                    al.application_list_date,
                    courthouse_name,
                    al.other_courthouse,
                    al.courthouse_code,
                    cja.cja_code,
                    sa.standard_applicant_code,
                    sa.name,
                    sa.forename_1,
                    sa.surname,
                    ac.application_code,
                    ac.application_code_title,
                    ale.ale_id
            ),
            applicant_names AS (
                SELECT
                    b.*,
                    COALESCE(
                        NULLIF(TRIM(b.name), ''),
                        NULLIF(
                            TRIM(
                                COALESCE(b.forename_1, '')
                                || ' '
                                || COALESCE(b.surname, '')
                            ),
                            ''
                        )
                    ) AS applicant_display_name
                FROM candidate_apps b
            ),
            filtered_apps AS (
                SELECT b.*
                FROM applicant_names b
                WHERE (
                        :applicantName IS NULL
                        OR UPPER(b.applicant_display_name)
                            LIKE '%' || UPPER(:applicantName) || '%'
                    )
                    AND (
                        :applicantOrganisation IS NULL
                        OR UPPER(b.name) LIKE '%' || UPPER(:applicantOrganisation) || '%'
                    )
                    AND (
                        :cjaCode IS NULL
                        OR b.cja_code = :cjaCode
                        OR SUBSTRING(b.courthouse_code FROM 2 FOR 2) = :cjaCode
                    )
                    AND (
                        :otherCourthouse IS NULL
                        OR UPPER(b.other_courthouse)
                            LIKE '%' || UPPER(:otherCourthouse) || '%'
                    )
                    AND (
                        :courthouseCode IS NULL
                        OR UPPER(b.courthouse_code) = UPPER(:courthouseCode)
                    )
            ),
            latest_fee_status AS (
                SELECT
                    alefs_ale_id,
                    alefs_fee_status_date,
                    alefs_payment_reference,
                    CASE alefs_fee_status
                        WHEN 'D' THEN 'Due'
                        WHEN 'P' THEN 'Paid'
                        WHEN 'R' THEN 'Remitted'
                        WHEN 'U' THEN 'Undertaking'
                        ELSE alefs_fee_status
                    END AS fee_status,
                    ROW_NUMBER() OVER (
                        PARTITION BY alefs_ale_id
                        ORDER BY alefs_status_creation_date DESC NULLS LAST, alefs_id DESC
                    ) AS rn
                FROM app_list_entry_fee_status alefs
                JOIN filtered_apps fa
                    ON fa.ale_id = alefs.alefs_ale_id
            ),
            entry_fee_values AS (
                SELECT
                    alefi.ale_ale_id,
                    MAX(
                        CASE
                            WHEN curr_fee.is_offsite IS NOT TRUE
                            THEN curr_fee.fee_value
                        END
                    )::numeric(9, 2) AS fee_value,
                    MAX(
                        CASE
                            WHEN curr_fee.is_offsite IS TRUE
                            THEN curr_fee.fee_value
                        END
                    )::numeric(9, 2) AS off_site_fee_value
                FROM app_list_entry_fee_id alefi
                JOIN filtered_apps fa
                    ON fa.ale_id = alefi.ale_ale_id
                JOIN fee curr_fee
                    ON curr_fee.fee_id = alefi.fee_fee_id
                GROUP BY alefi.ale_ale_id
            )
            SELECT
                fa.application_list_date AS list_date,
                fa.courthouse_name,
                fa.other_courthouse,
                fa.cja_code,
                fa.standard_applicant_code,
                fa.applicant_display_name AS applicant_full_name,
                fa.application_code,
                fa.application_code_title,
                efv.fee_value,
                efv.off_site_fee_value,
                (
                    COALESCE(efv.fee_value, 0::numeric)
                    + COALESCE(efv.off_site_fee_value, 0::numeric)
                )::numeric(9, 2)
                    AS total_fee_value,
                lfs.fee_status,
                lfs.alefs_fee_status_date AS fee_status_date,
                lfs.alefs_payment_reference AS payment_reference
            FROM filtered_apps fa
            JOIN entry_fee_values efv
                ON efv.ale_ale_id = fa.ale_id
            LEFT JOIN latest_fee_status lfs
                ON lfs.alefs_ale_id = fa.ale_id
                AND lfs.rn = 1
            ORDER BY fa.application_list_date DESC, fa.ale_id DESC
            LIMIT :limit OFFSET :offset
            """;

    private static final RowMapper<FeesReportRow> ROW_MAPPER = new FeesReportRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final FeesReportFilterDto filter;
    private final String schema;

    FeesReportDataReader(
            NamedParameterJdbcTemplate jdbcTemplate, FeesReportFilterDto filter, String schema) {
        this.jdbcTemplate = jdbcTemplate;
        this.filter = filter;
        this.schema = schema;
    }

    @Override
    public void readData(
            ReadPagePosition position, PageReader<FeesReportRow> pageReader, JobContext jobContext)
            throws IOException {
        jdbcTemplate
                .getJdbcTemplate()
                .execute("SET LOCAL search_path TO \"" + schema + "\""); // NOSONAR
        // S2077: schema is trusted Spring config; report filter values are bound query parameters.

        List<FeesReportRow> rows = readPage(position);

        while (!rows.isEmpty()) {
            pageReader.readData(rows, jobContext);
            position.setStartOffset(position.getStartOffset() + position.getPageSize());
            rows = readPage(position);
        }
    }

    @Override
    public void close() throws IOException {
        // No stream to close.
    }

    private List<FeesReportRow> readPage(ReadPagePosition position) {
        MapSqlParameterSource parameters =
                new MapSqlParameterSource()
                        .addValue("dateFrom", filter.getDateFrom(), Types.DATE)
                        .addValue("dateTo", filter.getDateTo(), Types.DATE)
                        .addValue(
                                "standardApplicantCode",
                                filter.getStandardApplicantCode(),
                                Types.VARCHAR)
                        .addValue("applicantName", filter.getApplicantName(), Types.VARCHAR)
                        .addValue(
                                "applicantOrganisation",
                                filter.getApplicantOrganisation(),
                                Types.VARCHAR)
                        .addValue("cjaCode", getLocationValue(Location::getCjaCode), Types.VARCHAR)
                        .addValue(
                                "otherCourthouse",
                                getLocationValue(Location::getOtherLocationDescription),
                                Types.VARCHAR)
                        .addValue(
                                "courthouseCode",
                                getLocationValue(Location::getCourtLocationCode),
                                Types.VARCHAR)
                        .addValue("limit", position.getPageSize(), Types.INTEGER)
                        .addValue("offset", position.getStartOffset(), Types.INTEGER);

        return jdbcTemplate.query(REPORT_QUERY, parameters, ROW_MAPPER);
    }

    private String getLocationValue(java.util.function.Function<Location, String> getter) {
        if (filter.getLocation() == null) {
            return null;
        }

        return getter.apply(filter.getLocation());
    }

    private static class FeesReportRowMapper implements RowMapper<FeesReportRow> {
        @Override
        public FeesReportRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return FeesReportRow.builder()
                    .listDate(rs.getObject("list_date", java.time.LocalDate.class))
                    .courthouseName(rs.getString("courthouse_name"))
                    .otherCourthouse(rs.getString("other_courthouse"))
                    .cjaCode(rs.getString("cja_code"))
                    .standardApplicantCode(rs.getString("standard_applicant_code"))
                    .applicantFullName(rs.getString("applicant_full_name"))
                    .applicationCode(rs.getString("application_code"))
                    .applicationCodeTitle(rs.getString("application_code_title"))
                    .feeValue(rs.getBigDecimal("fee_value"))
                    .offSiteFeeValue(rs.getBigDecimal("off_site_fee_value"))
                    .totalFeeValue(rs.getBigDecimal("total_fee_value"))
                    .feeStatus(rs.getString("fee_status"))
                    .feeStatusDate(rs.getObject("fee_status_date", java.time.LocalDate.class))
                    .paymentReference(rs.getString("payment_reference"))
                    .build();
        }
    }
}
