package uk.gov.hmcts.appregister.report.service;

import org.springframework.jdbc.core.RowMapper;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import uk.gov.hmcts.appregister.common.async.JobContext;
import uk.gov.hmcts.appregister.common.async.reader.PageReader;
import uk.gov.hmcts.appregister.common.async.reader.ReadPagePosition;
import uk.gov.hmcts.appregister.generated.model.WorkloadFilterDto;
import uk.gov.hmcts.appregister.report.model.FeesReportRow;
import uk.gov.hmcts.appregister.report.model.WorkloadReportRow;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class WorkloadReportDataReader implements DataReader<{
    private static final String REPORT_QUERY =
        """
            WITH applicants AS (
            SELECT
            	na_id AS id,
            	   code,
            	   name,
            	   title,
            	   forename_1,
            	   forename_2,
            	   forename_3,
            	   surname,
            	   address_l1,
            	   address_l2,
            	   address_l3,
            	   address_l4,
            	   address_l5,
            	   postcode,
            	   email_address,
            	   telephone_number,
            	   mobile_number,
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
            	   address_l1,
            	   address_l2,
            	   address_l3,
            	   address_l4,
            	   address_l5,
            	   postcode,
            	   email_address,
            	   telephone_number,
            	   mobile_number,
            	   TRUE AS is_standard_applicant
            FROM
            	standard_applicants sa)

            SELECT
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
            		ELSE
            	    NULL
            	END AS standard_applicant_code,
            	   CASE
            		WHEN a.name IS NOT NULL THEN
            	   	a.name
            		ELSE
            	   	CONCAT(a.forename_1, ' ', a.forename_2, ' ', a.forename_3, ' ', a.surname)
            	END AS applicant_name,
            	   ac.application_code AS application_code,
            	   ac.application_code_title AS appplication_code_title,
            	   string_agg(rc.resolution_code, ',') AS resolution_codes,
            	   CASE
            		WHEN ale.sequence_number = 1
            		AND aleo.official_type = 'M' THEN
            	   	CONCAT(aleo.title, ' ', aleo.forename, ' ', aleo.surname)
            	END AS JP1,
            	   CASE
            		WHEN ale.sequence_number = 2
            		AND aleo.official_type = 'M' THEN
            	   	CONCAT(aleo.title, ' ', aleo.forename, ' ', aleo.surname)
            	END AS JP2,
            	   CASE
            		WHEN ale.sequence_number = 3
            		AND aleo.official_type = 'M' THEN
            	   	CONCAT(aleo.title, ' ', aleo.forename, ' ', aleo.surname)
            	END AS JP3,
            	   CASE
            		WHEN aleo.official_type = 'C' THEN
            	    CONCAT(aleo.title, ' ', aleo.forename, ' ', aleo.surname)
            	END AS official
            FROM
            	applicants a
            JOIN application_list_entries ale
            JOIN application_lists al ON
            	al.al_id = ale.al_al_id
            JOIN application_codes ac ON
            	ac.ac_id = ale.ac_ac_id
            LEFT OUTER JOIN criminal_justice_area cja ON
            	cja.cja_id = al.cja_cja_id
            ON
            	(a.is_standard_applicant = FALSE
            		AND ale.a_na_id = a.id)
            	OR (a.is_standard_applicant = FALSE
            		AND ale.sa_sa_id = a.id)
            LEFT OUTER JOIN app_list_entry_resolutions aler ON
            	aler.ale_ale_id = ale.ale_id
            LEFT OUTER JOIN resolution_codes rc ON
            	rc.rc_id = aler.rc_rc_id
            LEFT OUTER JOIN app_list_entry_official aleo ON
            	aleo.ale_ale_id = ale.ale_id
            WHERE
            	al.application_list_status = 'CLOSED'
            GROUP BY
            	rc.resolution_code,
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
            	ale.sequence_number,
            	aleo.official_type,
            	aleo.title,
            	aleo.forename,
            	aleo.surname
            WHERE
            	AND al.application_list_date >= :dateFrom
            	AND al.application_list_date < (:dateTo + INTERVAL '1 day')
            	AND :courthouseName IS NULL
            	OR ((courthouse_name LIKE '%' || :courthouseName || '%')
            		AND :otherLocation IS NULL
            		AND :cjaCode IS NULL)
            	AND :otherLocation IS NULL
            	OR ((list_other_location LIKE '%' || :otherLocation || '%')
            		AND (cja_code LIKE '%' || :cjaCode || '%')
            			AND courthouse_name IS NULL)
            	AND :cjaCode IS NULL
            	OR ((cja_code LIKE '%' || :cjaCode || '%')
            		AND :otherLocation IS NULL
            		AND :courthouseName IS NULL)
            ORDER BY
            	list_date DESC
        """;

    private static final RowMapper<WorkloadReportRow> ROW_MAPPER = new WorkloadReportRowMapper();

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final WorkloadFilterDto filterDto;
    private final String schema;

    public WorkloadReportDataReader(NamedParameterJdbcTemplate jdbcTemplate, WorkloadFilterDto filterDto, String schema) {
        this.jdbcTemplate = jdbcTemplate;
        this.filterDto = filterDto;
        this.schema = schema;
    }

    @Override
    public void readData(ReadPagePosition position, PageReader<WorkloadReportRow> pageReader, JobContext jobContext)
        throws IOException {
        jdbcTemplate.getJdbcTemplate().execute("SET LOCAL search_path TO " + schema);
        WorkloadReportReadCursor cursor = new WorkloadReportReadCursor(position.getPageSize());
        List<WorkloadReportRow> rows = readPage(cursor);

        while(!rows.isEmpty()) {
            pageReader.readData(rows, jobContext);
            if(rows.size() < cursor.pageSize()) {
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
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("dateFrom", filterDto.getDateFrom())
            .addValue("dateTo", filterDto.getDateTo())
            .addValue("courthouseName", filterDto.getLocation().getCourtLocationCode())
            .addValue("otherLocation", filterDto.getLocation().getOtherLocationDescription())
            .addValue("cjaCode", filterDto.getLocation().getCjaCode());
    }

    private static class WorkloadReportReadCursor {
        private final int pageSize;
        private FeesReportRow lastRow;

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
    }

    private static class WorkloadReportRowMapper implements RowMapper<WorkloadReportRow> {
        @Override
        public WorkloadReportRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return WorkloadReportRow.builder()
                .applicationCode(rs.getString("application_code"))
                .applicationCodeTitle(rs.getString("appplication_code_title"))
                .applicantNameSurname(rs.getString("applicant_name"))
                .cjaCode(rs.getString("cja_code"))
                .listCourtHouseName(rs.getString("courthouse_name"))
                .listDate(rs.getDate("list_date").toLocalDate())
                .listOtherLocation(rs.getString("list_other_location"))
                .listDescription(rs.getString("list_description"))
                .official(rs.getString("official"))
                .jp1(rs.getString("jp1"))
                .jp2(rs.getString("jp2"))
                .jp3(rs.getString("jp3"))
                .results(rs.getString("resolution_codes"))
                .build();
        }
    }
}
