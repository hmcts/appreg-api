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
import uk.gov.hmcts.appregister.generated.model.WorkloadFilterDto;
import uk.gov.hmcts.appregister.report.model.WorkloadReportRow;

public class WorkloadReportDataReader implements DataReader<WorkloadReportRow> {
    private static final String REPORT_QUERY =
            """
            WITH applicants AS (
                SELECT
                    na_id AS id,
                    code,
                    name,
                    title,
                    first_name as forename_1,
                    middle_name as forename_2,
                    null as forename_3,
                    last_name as surname,
                    FALSE AS is_standard_applicant
                FROM
                    name_address na
                UNION ALL
                SELECT
                    sa_id AS id,
                    standard_applicant_code,
                    name,
                    title,
                    forename_1,
                    forename_2,
                    forename_3,
                    surname,
                    TRUE AS is_standard_applicant
                FROM
                    standard_applicants sa),
                    officials_agg AS (
                          SELECT
                              ale_ale_id,
                              MAX(CASE WHEN official_type = 'M' AND rn = 1
                                  THEN CONCAT(title, ' ', forename, ' ', surname) END) AS JP1,
                              MAX(CASE WHEN official_type = 'M' AND rn = 2
                                  THEN CONCAT(title, ' ', forename, ' ', surname) END) AS JP2,
                              MAX(CASE WHEN official_type = 'M' AND rn = 3
                                  THEN CONCAT(title, ' ', forename, ' ', surname) END) AS JP3,
                              MAX(CASE WHEN official_type = 'C'
                                  THEN CONCAT(title, ' ', forename, ' ', surname) END) AS official
                          FROM (
                              SELECT *,
                                     ROW_NUMBER() OVER
                                     (PARTITION BY ale_ale_id, official_type ORDER BY aleo_id) AS rn
                              FROM app_list_entry_official
                          ) ranked
                          GROUP BY ale_ale_id
                    )
                SELECT
                    ale.ale_id,
                    al.application_list_date AS list_date,
                    CASE
                           WHEN al.courthouse_code IS NOT NULL
                                                       THEN al.courthouse_code || ' - ' || al.courthouse_name
                    END AS courthouse_name,
                    al.other_courthouse AS list_other_location,
                    cja.cja_code AS cja_code,
                    al.list_description AS list_description,
                    CASE
                           WHEN a.is_standard_applicant = TRUE THEN
                                                   a.code
                    END AS standard_applicant_code,
                    CASE
                           WHEN a.name IS NOT NULL THEN
                                a.name
                           ELSE
                                CONCAT(a.forename_1, ' ', a.forename_2, ' ', a.forename_3, ' ', a.surname)
                    END AS applicant_name,
                    ac.application_code AS application_code,
                    ac.application_code_title AS application_code_title,
                    string_agg(DISTINCT rc.resolution_code, ',') AS resolution_codes,
                    oa.JP1,
                    oa.JP2,
                    oa.JP3,
                    oa.official
                FROM
                    applicants a
                JOIN application_list_entries ale ON
                    (a.is_standard_applicant = FALSE
                           AND ale.a_na_id = a.id)
                    OR (a.is_standard_applicant = TRUE
                           AND ale.sa_sa_id = a.id)
                JOIN application_lists al ON
                    al.al_id = ale.al_al_id
                JOIN application_codes ac ON
                    ac.ac_id = ale.ac_ac_id
                LEFT OUTER JOIN criminal_justice_area cja ON
                    cja.cja_id = al.cja_cja_id
                LEFT OUTER JOIN app_list_entry_resolutions aler ON
                    aler.ale_ale_id = ale.ale_id
                LEFT OUTER JOIN resolution_codes rc ON
                    rc.rc_id = aler.rc_rc_id
                LEFT OUTER JOIN officials_agg oa ON
                    oa.ale_ale_id = ale.ale_id
                WHERE
                    al.application_list_status = 'CLOSED'
                    AND (al.is_deleted = 'N' OR al.is_deleted IS NULL)
                    AND (ale.is_deleted = 'N' OR ale.is_deleted IS NULL)
                    AND al.application_list_date >= :dateFrom
                    AND al.application_list_date < (:dateTo + INTERVAL '1 day')
                    -- Maintains legacy MIS Workload report AR5-7 location semantics.
                    AND (
                        {{LEGACY_LOCATION_PREDICATE}}
                    )
                    AND(:hasCursor IS FALSE
                           OR :lastListDate IS NULL
                           OR al.application_list_date < :lastListDate
                           OR (al.application_list_date = :lastListDate
                                   AND ale.ale_id < :lastApplicationListEntryId))
                GROUP BY
                    ale.ale_id,
                    al.application_list_date,
                    al.courthouse_name,
                    al.courthouse_code,
                    al.other_courthouse,
                    cja.cja_code,
                    al.list_description,
                    a.is_standard_applicant,
                    a.code,
                    a.name,
                    a.forename_1,
                    a.forename_2,
                    a.forename_3,
                    a.surname,
                    ac.application_code,
                    ac.application_code_title,
                    oa.JP1,
                    oa.JP2,
                    oa.JP3,
                    oa.official
                ORDER BY
                    list_date DESC,
                    ale.ale_id DESC
                LIMIT :limit
            """
                    .replace(
                            "{{LEGACY_LOCATION_PREDICATE}}",
                            LegacyMisReportLocationSql.predicate(
                                    "al.courthouse_code",
                                    "al.other_courthouse",
                                    "cja.cja_code",
                                    "otherLocation"));

    private static final RowMapper<WorkloadReportRow> ROW_MAPPER = new WorkloadReportRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final WorkloadFilterDto filterDto;
    private final String schema;

    public WorkloadReportDataReader(
            NamedParameterJdbcTemplate jdbcTemplate, WorkloadFilterDto filterDto, String schema) {
        this.jdbcTemplate = jdbcTemplate;
        this.filterDto = filterDto;
        this.schema = schema;
    }

    @Override
    public void readData(
            ReadPagePosition position,
            PageReader<WorkloadReportRow> pageReader,
            JobContext jobContext)
            throws IOException {
        jdbcTemplate
                .getJdbcTemplate()
                .execute("SET LOCAL search_path TO \"" + schema + "\""); // NOSONAR
        WorkloadReportReadCursor cursor = new WorkloadReportReadCursor(position.getPageSize());
        List<WorkloadReportRow> rows = readPage(cursor);

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

    private List<WorkloadReportRow> readPage(WorkloadReportReadCursor cursor) {
        MapSqlParameterSource parameters =
                new MapSqlParameterSource()
                        .addValue("dateFrom", filterDto.getDateFrom(), Types.DATE)
                        .addValue("dateTo", filterDto.getDateTo(), Types.DATE)
                        .addValue(
                                "courthouseCode",
                                filterDto.getLocation() != null
                                        ? filterDto.getLocation().getCourtLocationCode()
                                        : null,
                                Types.VARCHAR)
                        .addValue(
                                "otherLocation",
                                filterDto.getLocation() != null
                                        ? filterDto.getLocation().getOtherLocationDescription()
                                        : null,
                                Types.VARCHAR)
                        .addValue(
                                "cjaCode",
                                filterDto.getLocation() != null
                                        ? filterDto.getLocation().getCjaCode()
                                        : null,
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

    private static class WorkloadReportReadCursor {
        private final int pageSize;
        private WorkloadReportRow lastRow;

        WorkloadReportReadCursor(int pageSize) {
            this.pageSize = pageSize;
        }

        void advance(List<WorkloadReportRow> rows) {
            lastRow = rows.getLast();
        }

        boolean hasLastRow() {
            return lastRow != null;
        }

        int pageSize() {
            return pageSize;
        }

        LocalDate lastListDate() {
            return hasLastRow() ? lastRow.getListDate() : null;
        }

        Long lastApplicationListEntryId() {
            return hasLastRow() ? lastRow.getLastApplicationListEntryId() : null;
        }
    }

    private static class WorkloadReportRowMapper implements RowMapper<WorkloadReportRow> {
        @Override
        public WorkloadReportRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return WorkloadReportRow.builder()
                    .lastApplicationListEntryId(rs.getLong("ale_id"))
                    .applicationCode(rs.getString("application_code"))
                    .applicationCodeTitle(rs.getString("application_code_title"))
                    .applicantNameSurname(rs.getString("applicant_name"))
                    .cjaCode(rs.getString("cja_code"))
                    .listCourtHouseName(rs.getString("courthouse_name"))
                    .listDate(rs.getDate("list_date").toLocalDate())
                    .listOtherLocation(rs.getString("list_other_location"))
                    .listDescription(rs.getString("list_description"))
                    .standardApplicantCode(rs.getString("standard_applicant_code"))
                    .official(rs.getString("official"))
                    .jp1(rs.getString("jp1"))
                    .jp2(rs.getString("jp2"))
                    .jp3(rs.getString("jp3"))
                    .results(rs.getString("resolution_codes"))
                    .build();
        }
    }
}
