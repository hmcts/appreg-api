package uk.gov.hmcts.appregister.report.fee.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.appregister.report.fee.dto.FeeReportRowDto;

/** Repository to run custom SQL queries for generating fee reports. */
@RequiredArgsConstructor
@Repository
public class FeeReportJdbcRepository {

    private static final Logger log = LoggerFactory.getLogger(FeeReportJdbcRepository.class);
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<FeeReportRowDto> generateFeeReport(
            LocalDate startDate,
            LocalDate endDate,
            String standardApplicantCode,
            String applicantSurname,
            String courthouseCode) {

        log.info("Searching fee report with the following criteria:");
        log.info("startDate = {}", startDate);
        log.info("endDate = {}", endDate);
        log.info("standardApplicantCode = {}", standardApplicantCode);
        log.info("applicantSurname = {}", applicantSurname);
        log.info("courthouseCode = {}", courthouseCode);

        // TODO: Fine for POC, should probably externalise the query rather than hardcoding it as a
        // String.
        StringBuilder sql =
                new StringBuilder(
                        """
                    -- Standard Applicants
                    SELECT
                        application_list.date AS listDate,
                        courthouse.name AS courthouseName,
                        NULL AS otherCourthouse,
                        standard_applicant.code AS standardApplicantCode,
                        standard_applicant.surname AS applicantNameOrSurname,
                        application_code.application_code AS applicationCode,
                        application_code.title AS applicationCodeTitle
                    FROM application
                    JOIN application_list ON application.application_list_id = application_list.id
                    JOIN courthouse ON application_list.courthouse_id = courthouse.id
                    JOIN application_code ON application.application_code_id = application_code.id
                    JOIN standard_applicant ON application.standard_applicant_id = standard_applicant.id
                    WHERE application_code.fee_due = true
                      AND application_list.date BETWEEN :startDate AND :endDate
                      AND (:standardApplicantCode IS NULL OR standard_applicant.code
                      ILIKE :standardApplicantCode)
                      AND (:applicantSurname IS NULL OR standard_applicant.surname ILIKE :applicantSurname)
                      AND (:courthouseCode IS NULL OR courthouse.location_code ILIKE :courthouseCode)
                    """);

        boolean includeNonStandard =
                (standardApplicantCode == null || standardApplicantCode.isBlank());

        if (includeNonStandard) {
            sql.append(
                    """
                    UNION ALL

                    -- Non-Standard Applicants
                    SELECT
                        application_list.date AS listDate,
                        courthouse.name AS courthouseName,
                        NULL AS otherCourthouse,
                        NULL AS standardApplicantCode,
                        COALESCE(identity_details.surname, identity_details.name)
                        AS applicantNameOrSurname,
                        application_code.application_code AS applicationCode,
                        application_code.title AS applicationCodeTitle
                    FROM application
                    JOIN application_list ON application.application_list_id = application_list.id
                    JOIN courthouse ON application_list.courthouse_id = courthouse.id
                    JOIN application_code ON application.application_code_id = application_code.id
                    JOIN identity_details ON application.applicant_id = identity_details.id
                    WHERE application_code.fee_due = true
                      AND application.standard_applicant_id IS NULL
                      AND application_list.date BETWEEN :startDate AND :endDate
                      AND (:applicantSurname IS NULL OR identity_details.surname ILIKE :applicantSurname
                          OR identity_details.name ILIKE :applicantSurname)
                      AND (:courthouseCode IS NULL OR courthouse.location_code ILIKE :courthouseCode)
                    """);
        }

        sql.append(" ORDER BY listDate DESC");

        var params =
                new MapSqlParameterSource()
                        .addValue("startDate", startDate)
                        .addValue("endDate", endDate)
                        .addValue(
                                "standardApplicantCode",
                                standardApplicantCode == null
                                        ? null
                                        : "%" + standardApplicantCode + "%",
                                java.sql.Types.VARCHAR)
                        .addValue(
                                "applicantSurname",
                                applicantSurname == null ? null : "%" + applicantSurname + "%",
                                java.sql.Types.VARCHAR)
                        .addValue(
                                "courthouseCode",
                                courthouseCode == null ? null : "%" + courthouseCode + "%",
                                java.sql.Types.VARCHAR);

        return jdbcTemplate.query(sql.toString(), params, this::mapRow);
    }

    private FeeReportRowDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new FeeReportRowDto(
                rs.getDate("listDate").toLocalDate(),
                rs.getString("courthouseName"),
                rs.getString("otherCourthouse"),
                rs.getString("standardApplicantCode"),
                rs.getString("applicantNameOrSurname"),
                rs.getString("applicationCode"),
                rs.getString("applicationCodeTitle"));
    }
}
